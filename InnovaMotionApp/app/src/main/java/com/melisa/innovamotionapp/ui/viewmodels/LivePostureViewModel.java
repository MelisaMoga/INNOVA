package com.melisa.innovamotionapp.ui.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.melisa.innovamotionapp.data.database.InnovaDatabase;
import com.melisa.innovamotionapp.data.database.MonitoredPerson;
import com.melisa.innovamotionapp.data.database.ReceivedBtDataDao;
import com.melisa.innovamotionapp.data.database.ReceivedBtDataEntity;
import com.melisa.innovamotionapp.data.posture.Posture;
import com.melisa.innovamotionapp.data.posture.PostureFactory;
import com.melisa.innovamotionapp.data.posture.types.FallingPosture;
import com.melisa.innovamotionapp.ui.models.PersonStatus;
import com.melisa.innovamotionapp.utils.GlobalData;
import com.melisa.innovamotionapp.utils.PersonNameManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ViewModel for the Aggregator's Live Posture tab.
 * 
 * Refactored to show a grid of sensors OWNED by this aggregator.
 * 
 * IMPORTANT: Only shows sensors where owner_user_id matches the current user.
 * This prevents data leakage when user has both aggregator and supervisor roles.
 * 
 * REACTIVE: Observes currentUserUid LiveData so dashboard updates when async auth completes.
 * 
 * Features:
 * - Display only sensors owned by this aggregator
 * - Reactive to user UID changes (fixes race condition on first login)
 * - Real-time updates from Room database
 * - Click to navigate to PersonDetailActivity
 * - Alerts sorted to top
 */
public class LivePostureViewModel extends AndroidViewModel {

    private static final String TAG = "LivePostureViewModel";
    
    private final ReceivedBtDataDao dao;
    private final PersonNameManager personNameManager;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MediatorLiveData<List<PersonStatus>> personStatuses;
    
    // In-memory cache for name lookups (avoids main-thread DB access)
    private final Map<String, String> nameCache = new HashMap<>();
    
    // Raw data source cache
    private List<ReceivedBtDataEntity> latestEntities = null;
    
    // Track current data source to allow cleanup when swapping
    private LiveData<List<ReceivedBtDataEntity>> currentDataSource = null;

    public LivePostureViewModel(@NonNull Application application) {
        super(application);

        dao = InnovaDatabase.getInstance(application).receivedBtDataDao();
        personNameManager = PersonNameManager.getInstance(application);
        personStatuses = new MediatorLiveData<>();

        // Observe person names to update the cache
        LiveData<List<MonitoredPerson>> allPersons = personNameManager.getAllPersonsLive();
        personStatuses.addSource(allPersons, persons -> {
            updateNameCache(persons);
            transformToPersonStatuses();
        });

        // Observe current user UID LiveData (reactive - updates when async auth completes)
        LiveData<String> userUidLive = GlobalData.getInstance().getCurrentUserUidLive();
        personStatuses.addSource(userUidLive, uid -> {
            Log.d(TAG, "User UID updated: " + uid);
            updateDataSource(uid);
        });
    }
    
    /**
     * Update the data source when user UID changes.
     * Removes old source and adds new one based on the updated UID.
     */
    private void updateDataSource(String userUid) {
        // Remove old data source if exists
        if (currentDataSource != null) {
            personStatuses.removeSource(currentDataSource);
        }
        
        // Create new data source based on user UID
        if (userUid != null && !userUid.isEmpty()) {
            Log.d(TAG, "Setting up data source for owner: " + userUid);
            currentDataSource = dao.getLatestForEachSensorByOwner(userUid);
        } else {
            Log.w(TAG, "No user UID available, dashboard will be empty");
            currentDataSource = new MutableLiveData<>(Collections.emptyList());
        }
        
        // Add new data source
        personStatuses.addSource(currentDataSource, entities -> {
            latestEntities = entities;
            transformToPersonStatuses();
        });
    }

    /**
     * Update the in-memory name cache from the person list.
     */
    private void updateNameCache(List<MonitoredPerson> persons) {
        nameCache.clear();
        if (persons != null) {
            for (MonitoredPerson person : persons) {
                nameCache.put(person.getSensorId(), person.getDisplayName());
            }
        }
    }

    /**
     * Get display name from cache, falling back to sensorId if not found.
     */
    private String getDisplayNameFromCache(String sensorId) {
        return nameCache.getOrDefault(sensorId, sensorId);
    }

    /**
     * Transform raw entities into PersonStatus list using cached names.
     */
    private void transformToPersonStatuses() {
        if (latestEntities == null) {
            personStatuses.setValue(Collections.emptyList());
            return;
        }
        
        List<PersonStatus> statuses = new ArrayList<>();
        for (ReceivedBtDataEntity entity : latestEntities) {
            String sensorId = entity.getSensorId();
            if (sensorId == null || sensorId.isEmpty()) continue;

            String displayName = getDisplayNameFromCache(sensorId);
            Posture posture = PostureFactory.createPosture(entity.getReceivedMsg());
            boolean isAlert = posture instanceof FallingPosture;

            statuses.add(new PersonStatus(
                    sensorId,
                    displayName,
                    posture,
                    entity.getTimestamp(),
                    isAlert
            ));
        }

        // Sort: alerts first, then by name alphabetically
        Collections.sort(statuses, (a, b) -> {
            if (a.isAlert() != b.isAlert()) {
                return a.isAlert() ? -1 : 1;
            }
            return a.getDisplayName().compareToIgnoreCase(b.getDisplayName());
        });

        personStatuses.setValue(statuses);
    }

    /**
     * Get all person statuses as LiveData.
     * Updates automatically when new sensor data arrives.
     */
    public LiveData<List<PersonStatus>> getPersonStatuses() {
        return personStatuses;
    }

    /**
     * Get loading state for UI progress indicator.
     */
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    /**
     * Trigger a manual refresh of data.
     * This can be used with swipe-to-refresh.
     */
    public void refreshData() {
        isLoading.setValue(true);
        // The LiveData from Room will automatically update when data changes.
        // For now, just reset the loading state after a short delay.
        isLoading.postValue(false);
    }
}
