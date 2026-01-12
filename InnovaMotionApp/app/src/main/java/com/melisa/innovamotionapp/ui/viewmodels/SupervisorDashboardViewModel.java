package com.melisa.innovamotionapp.ui.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.melisa.innovamotionapp.data.database.InnovaDatabase;
import com.melisa.innovamotionapp.data.database.ReceivedBtDataDao;
import com.melisa.innovamotionapp.data.database.ReceivedBtDataEntity;
import com.melisa.innovamotionapp.data.posture.Posture;
import com.melisa.innovamotionapp.data.posture.PostureFactory;
import com.melisa.innovamotionapp.data.posture.types.FallingPosture;
import com.melisa.innovamotionapp.ui.models.PersonStatus;
import com.melisa.innovamotionapp.utils.PersonNameManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ViewModel for the Supervisor Dashboard.
 * 
 * Observes the latest reading for each sensor and transforms them into
 * PersonStatus objects with display names and alert flags.
 * 
 * Features:
 * - Real-time updates as new data arrives
 * - Alerts sorted to top
 * - Alphabetical sorting by display name
 */
public class SupervisorDashboardViewModel extends AndroidViewModel {

    private final ReceivedBtDataDao dao;
    private final PersonNameManager personNameManager;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final LiveData<List<PersonStatus>> personStatuses;

    public SupervisorDashboardViewModel(@NonNull Application application) {
        super(application);

        dao = InnovaDatabase.getInstance(application).receivedBtDataDao();
        personNameManager = PersonNameManager.getInstance(application);

        // Get latest reading for each sensor
        LiveData<List<ReceivedBtDataEntity>> latestPerSensor = dao.getLatestForEachSensor();

        // Transform to PersonStatus with display names
        personStatuses = Transformations.map(latestPerSensor, entities -> {
            if (entities == null) {
                return Collections.emptyList();
            }
            
            List<PersonStatus> statuses = new ArrayList<>();
            for (ReceivedBtDataEntity entity : entities) {
                String sensorId = entity.getSensorId();
                if (sensorId == null || sensorId.isEmpty()) continue;

                String displayName = personNameManager.getDisplayName(sensorId);
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

            return statuses;
        });
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
        // In a real implementation, this would trigger a Firestore sync.
        isLoading.postValue(false);
    }
}
