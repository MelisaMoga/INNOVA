package com.melisa.innovamotionapp.ui.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.melisa.innovamotionapp.data.database.InnovaDatabase;
import com.melisa.innovamotionapp.data.database.ReceivedBtDataDao;
import com.melisa.innovamotionapp.data.database.ReceivedBtDataEntity;
import com.melisa.innovamotionapp.data.posture.Posture;
import com.melisa.innovamotionapp.data.posture.PostureFactory;
import com.melisa.innovamotionapp.data.posture.types.UnknownPosture;
import com.melisa.innovamotionapp.utils.GlobalData;

import java.util.List;

/**
 * ViewModel for supervisor feed functionality.
 * Converts the latest database row to Posture and pushes it into GlobalData
 * so that PersonDetailActivity can observe it without changes.
 */
public class SupervisorFeedViewModel extends AndroidViewModel {
    private final ReceivedBtDataDao dao;
    private final GlobalData global = GlobalData.getInstance();

    public SupervisorFeedViewModel(@NonNull Application application) {
        super(application);
        dao = InnovaDatabase.getInstance(application).receivedBtDataDao();
    }

    /**
     * Live posture derived from the latest DB row for supervised users.
     * Also updates GlobalData so PersonDetailActivity stays unchanged.
     * 
     * @return LiveData<Posture> that emits the latest posture from Room database
     */
    public LiveData<Posture> getLatestPosture() {
        // Get supervised sensor IDs from GlobalData
        List<String> supervisedSensorIds = global.supervisedSensorIds;
        
        if (supervisedSensorIds == null || supervisedSensorIds.isEmpty()) {
            // Fallback to all data if no supervised sensors configured
            Log.i("UI/Stats", "Subscribing for owner=all (no supervised sensors configured)");
            LiveData<ReceivedBtDataEntity> latest = dao.getLatestMessage();
            return Transformations.map(latest, entity -> {
                if (entity == null) {
                    return new UnknownPosture();
                }
                
                // Convert the latest database entity to a Posture
                Posture posture = PostureFactory.createPosture(entity.getReceivedMsg());
                
                // Update GlobalData so existing observers (like PersonDetailActivity) 
                // continue to work without any changes
                global.setReceivedPosture(posture);
                
                return posture;
            });
        }
        
        // Filter by supervised sensor IDs
        Log.i("UI/Stats", "Subscribing for owner=" + supervisedSensorIds);
        LiveData<ReceivedBtDataEntity> latest = dao.getLatestForOwners(supervisedSensorIds);
        return Transformations.map(latest, entity -> {
            if (entity == null) {
                return new UnknownPosture();
            }
            
            // Convert the latest database entity to a Posture
            Posture posture = PostureFactory.createPosture(entity.getReceivedMsg());
            
            // Update GlobalData so existing observers (like PersonDetailActivity) 
            // continue to work without any changes
            global.setReceivedPosture(posture);
            
            return posture;
        });
    }
    
    /**
     * Get the latest posture for a specific device (extended feature).
     * Useful when supervisors want to monitor a specific supervised user.
     * 
     * @param deviceAddress The device address to filter by
     * @return LiveData<Posture> that emits the latest posture from the specified device
     */
    public LiveData<Posture> getLatestPostureForDevice(String deviceAddress) {
        LiveData<ReceivedBtDataEntity> latestForDevice = dao.getLatestForDevice(deviceAddress);
        return Transformations.map(latestForDevice, entity -> {
            if (entity == null) {
                return new UnknownPosture();
            }
            
            // Convert the device-specific database entity to a Posture
            Posture posture = PostureFactory.createPosture(entity.getReceivedMsg());
            
            // Update GlobalData so existing observers continue to work
            global.setReceivedPosture(posture);
            
            return posture;
        });
    }
}
