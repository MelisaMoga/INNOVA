package com.melisa.innovamotionapp.ui.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.melisa.innovamotionapp.data.database.InnovaDatabase;
import com.melisa.innovamotionapp.data.database.ReceivedBtDataDao;
import com.melisa.innovamotionapp.data.database.ReceivedBtDataEntity;
import com.melisa.innovamotionapp.utils.GlobalData;
import com.melisa.innovamotionapp.utils.OwnerSource;

import java.util.Collections;
import java.util.List;

/**
 * ViewModel for Statistics Activity.
 * 
 * Supports two modes:
 * 1. User-based filtering: Shows data for a specific user (targetUserId)
 * 2. Sensor-based filtering: Shows data for a specific sensor (sensorId)
 * 
 * Sensor-based filtering takes precedence when sensorId is set.
 */
public class StatisticsViewModel extends AndroidViewModel {
    private static final String TAG = "UI/StatsVM";
    
    private final ReceivedBtDataDao dao;
    private final GlobalData global = GlobalData.getInstance();
    private final MutableLiveData<String> targetUserId = new MutableLiveData<>();
    private final MutableLiveData<String> sensorId = new MutableLiveData<>();

    public StatisticsViewModel(@NonNull Application app) {
        super(app);
        dao = InnovaDatabase.getInstance(app).receivedBtDataDao();
        // Default: keep null until Activity sets it when session is ready
        targetUserId.setValue(null);
        sensorId.setValue(null);
    }

    public void setTargetUserId(String userId) { 
        targetUserId.setValue(userId); 
    }

    /**
     * Set sensor ID for sensor-specific filtering.
     * When set, data will be filtered to this specific sensor.
     */
    public void setSensorId(@Nullable String sensorId) {
        this.sensorId.setValue(sensorId);
    }

    /**
     * Check if sensor-based filtering is active.
     */
    public boolean isSensorMode() {
        String id = sensorId.getValue();
        return id != null && !id.isEmpty();
    }

    /**
     * Get all data for user OR sensor, depending on which mode is active.
     */
    public LiveData<List<ReceivedBtDataEntity>> getAllForUser() {
        // If sensorId is set, use sensor-based filtering
        return Transformations.switchMap(sensorId, sid -> {
            if (sid != null && !sid.isEmpty()) {
                Log.i(TAG, "subscribe sensorId=" + sid);
                return dao.getAllForSensor(sid);
            }
            // Otherwise, use user-based filtering
            return Transformations.switchMap(targetUserId, uid -> {
                Log.i(TAG, "subscribe targetUser=" + uid);
                if (uid == null) return new MutableLiveData<>(Collections.emptyList());
                return dao.getAllForUserLive(uid);
            });
        });
    }

    /**
     * Get range data for user OR sensor, depending on which mode is active.
     */
    public LiveData<List<ReceivedBtDataEntity>> getRangeForUser(long start, long end) {
        // If sensorId is set, use sensor-based filtering
        return Transformations.switchMap(sensorId, sid -> {
            if (sid != null && !sid.isEmpty()) {
                Log.i(TAG, "subscribe sensorId=" + sid + " range=[" + start + "," + end + "]");
                return dao.getRangeForSensor(sid, start, end);
            }
            // Otherwise, use user-based filtering
            return Transformations.switchMap(targetUserId, uid -> {
                Log.i(TAG, "subscribe targetUser=" + uid + " range=[" + start + "," + end + "]");
                if (uid == null) return new MutableLiveData<>(Collections.emptyList());
                return dao.getRangeForUserLive(uid, start, end);
            });
        });
    }

    /**
     * Get all data for a specific sensor.
     * Use this when you explicitly want sensor-based data.
     */
    public LiveData<List<ReceivedBtDataEntity>> getAllForSensor() {
        return Transformations.switchMap(sensorId, sid -> {
            Log.i(TAG, "getAllForSensor sensorId=" + sid);
            if (sid == null || sid.isEmpty()) {
                return new MutableLiveData<>(Collections.emptyList());
            }
            return dao.getAllForSensor(sid);
        });
    }

    /**
     * Get range data for a specific sensor.
     * Use this when you explicitly want sensor-based data.
     */
    public LiveData<List<ReceivedBtDataEntity>> getRangeForSensor(long start, long end) {
        return Transformations.switchMap(sensorId, sid -> {
            Log.i(TAG, "getRangeForSensor sensorId=" + sid + " range=[" + start + "," + end + "]");
            if (sid == null || sid.isEmpty()) {
                return new MutableLiveData<>(Collections.emptyList());
            }
            return dao.getRangeForSensor(sid, start, end);
        });
    }
}
