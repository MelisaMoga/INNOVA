package com.melisa.innovamotionapp.ui.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.melisa.innovamotionapp.R;
import com.melisa.innovamotionapp.data.database.InnovaDatabase;
import com.melisa.innovamotionapp.data.database.MonitoredPerson;
import com.melisa.innovamotionapp.data.database.ReceivedBtDataDao;
import com.melisa.innovamotionapp.data.database.ReceivedBtDataEntity;
import com.melisa.innovamotionapp.ui.models.MessageLogItem;
import com.melisa.innovamotionapp.utils.Constants;
import com.melisa.innovamotionapp.utils.PersonNameManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ViewModel for the Message Log UI.
 * 
 * Transforms raw database entities into UI-ready MessageLogItem objects,
 * resolving sensor IDs to display names and determining posture icons.
 */
public class MessageLogViewModel extends AndroidViewModel {

    private static final int DEFAULT_MESSAGE_LIMIT = Constants.MESSAGE_LOG_MAX_ITEMS;
    
    // Posture hex codes (lowercase for comparison)
    private static final String HEX_STANDING = "0xab3311";
    private static final String HEX_SITTING = "0xac4312";
    private static final String HEX_WALKING = "0xba3311";
    private static final String HEX_FALLING = "0xef0112";
    private static final String HEX_UNUSED = "0x793248";

    private final ReceivedBtDataDao dao;
    private final PersonNameManager personNameManager;
    
    // Filter state
    private final MutableLiveData<String> filterSensor = new MutableLiveData<>(null);
    
    // In-memory name cache, updated reactively from LiveData to avoid main-thread DB access
    private volatile Map<String, String> nameCache = Collections.emptyMap();
    
    // Transformed data
    private final LiveData<List<MessageLogItem>> messages;
    private final LiveData<List<String>> availableSensors;
    private final LiveData<Map<String, Integer>> messageCountsPerSensor;

    public MessageLogViewModel(@NonNull Application application) {
        super(application);
        dao = InnovaDatabase.getInstance(application).receivedBtDataDao();
        personNameManager = PersonNameManager.getInstance(application);
        
        // Observe all persons to keep nameCache updated (async, off main thread)
        LiveData<List<MonitoredPerson>> allPersons = personNameManager.getAllPersonsLive();
        
        // Get recent messages (limit to last 500)
        LiveData<List<ReceivedBtDataEntity>> rawMessages = dao.getRecentMessages(DEFAULT_MESSAGE_LIMIT);
        
        // #region agent log
        // H5: Log ViewModel initialization and observe message count
        rawMessages.observeForever(entities -> {
            int count = entities != null ? entities.size() : 0;
            android.util.Log.w("DBG_H5", "rawMessages LiveData update: messageCount=" + count);
        });
        // #endregion

        // Use MediatorLiveData to combine rawMessages + allPersons
        // so that transformation only runs when we have data, and uses cached names safely
        MediatorLiveData<List<MessageLogItem>> combinedMessages = new MediatorLiveData<>();
        
        // Track latest data
        final List<ReceivedBtDataEntity>[] latestEntities = new List[]{null};
        
        combinedMessages.addSource(allPersons, persons -> {
            // Update name cache from persons list (this runs on main thread but no DB access)
            Map<String, String> newCache = new HashMap<>();
            if (persons != null) {
                for (MonitoredPerson p : persons) {
                    String name = p.getDisplayName();
                    newCache.put(p.getSensorId(), (name != null && !name.isEmpty()) ? name : p.getSensorId());
                }
            }
            nameCache = newCache;
            
            // Re-transform if we have entities
            if (latestEntities[0] != null) {
                combinedMessages.setValue(transformToMessageLogItems(latestEntities[0]));
            }
        });
        
        // Wrap messages transformation with filter support
        LiveData<List<ReceivedBtDataEntity>> filteredMessages = Transformations.switchMap(filterSensor, sensor -> {
            if (sensor == null || sensor.isEmpty()) {
                return rawMessages;
            } else {
                return dao.getMessagesForSensor(sensor, DEFAULT_MESSAGE_LIMIT);
            }
        });
        
        combinedMessages.addSource(filteredMessages, entities -> {
            latestEntities[0] = entities;
            // Only transform if we have attempted to load persons (cache may be empty but that's ok)
            combinedMessages.setValue(transformToMessageLogItems(entities));
        });
        
        messages = combinedMessages;
        
        // Available sensors for filter dropdown
        availableSensors = dao.getDistinctSensorIds();
        
        // Message counts per sensor (for summary header)
        messageCountsPerSensor = Transformations.map(rawMessages, entities -> {
            Map<String, Integer> counts = new HashMap<>();
            if (entities != null) {
                for (ReceivedBtDataEntity entity : entities) {
                    String sensor = entity.getSensorId();
                    counts.put(sensor, counts.getOrDefault(sensor, 0) + 1);
                }
            }
            return counts;
        });
    }

    /**
     * Transform database entities to UI items.
     * Uses the in-memory nameCache instead of DB lookups.
     */
    private List<MessageLogItem> transformToMessageLogItems(List<ReceivedBtDataEntity> entities) {
        List<MessageLogItem> items = new ArrayList<>();
        if (entities == null) return items;
        
        for (ReceivedBtDataEntity entity : entities) {
            String sensorId = entity.getSensorId();
            // Use cached name instead of blocking DB lookup
            String displayName = nameCache.getOrDefault(sensorId, sensorId);
            
            int iconRes = getPostureIcon(entity.getReceivedMsg());
            boolean isFall = isFallPosture(entity.getReceivedMsg());
            
            items.add(new MessageLogItem(
                    entity.getId(),
                    entity.getTimestamp(),
                    sensorId,
                    displayName,
                    entity.getReceivedMsg(),
                    iconRes,
                    isFall
            ));
        }
        return items;
    }

    /**
     * Get drawable resource for a posture hex code.
     */
    private int getPostureIcon(String hexCode) {
        if (hexCode == null) return R.drawable.ic_posture_unknown;
        
        String normalized = hexCode.toLowerCase();
        switch (normalized) {
            case HEX_STANDING:
                return R.drawable.ic_posture_standing;
            case HEX_SITTING:
                return R.drawable.ic_posture_sitting;
            case HEX_WALKING:
                return R.drawable.ic_posture_walking;
            case HEX_FALLING:
                return R.drawable.ic_posture_falling;
            case HEX_UNUSED:
                return R.drawable.ic_posture_unknown;
            default:
                return R.drawable.ic_posture_unknown;
        }
    }

    /**
     * Check if a hex code represents a fall posture.
     */
    private boolean isFallPosture(String hexCode) {
        if (hexCode == null) return false;
        return hexCode.toLowerCase().equals(HEX_FALLING);
    }

    /**
     * Get display name for a sensor ID from the local cache.
     * Safe to call from main thread.
     */
    public String getDisplayName(String sensorId) {
        return nameCache.getOrDefault(sensorId, sensorId);
    }

    // ========== Public API ==========

    /**
     * Set the sensor filter. Pass null or empty string to show all.
     */
    public void setFilterSensor(String sensor) {
        filterSensor.setValue(sensor);
    }

    /**
     * Get current filter value.
     */
    public String getCurrentFilter() {
        return filterSensor.getValue();
    }

    /**
     * Get transformed messages for UI display.
     */
    public LiveData<List<MessageLogItem>> getMessages() {
        return messages;
    }

    /**
     * Get list of available sensor IDs for filter dropdown.
     */
    public LiveData<List<String>> getAvailableSensors() {
        return availableSensors;
    }

    /**
     * Get message counts per sensor for summary header.
     */
    public LiveData<Map<String, Integer>> getMessageCountsPerSensor() {
        return messageCountsPerSensor;
    }
}
