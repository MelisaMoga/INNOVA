package com.melisa.innovamotionapp.ui.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.melisa.innovamotionapp.data.database.InnovaDatabase;
import com.melisa.innovamotionapp.data.database.MonitoredPerson;
import com.melisa.innovamotionapp.data.database.MonitoredPersonDao;
import com.melisa.innovamotionapp.data.database.ReceivedBtDataDao;
import com.melisa.innovamotionapp.data.database.ReceivedBtDataEntity;
import com.melisa.innovamotionapp.data.posture.Posture;
import com.melisa.innovamotionapp.data.posture.PostureFactory;
import com.melisa.innovamotionapp.utils.PersonNameManager;

import java.util.List;

/**
 * ViewModel for the Live Posture Viewer tab.
 * 
 * Allows selecting a monitored person and observing their current posture
 * in real-time as new data arrives from Bluetooth.
 */
public class LivePostureViewModel extends AndroidViewModel {

    private final ReceivedBtDataDao dao;
    private final MonitoredPersonDao personDao;
    private final PersonNameManager personNameManager;

    // Selected person's sensor ID
    private final MutableLiveData<String> selectedSensorId = new MutableLiveData<>();

    // Derived LiveData
    private final LiveData<Posture> currentPosture;
    private final LiveData<Long> lastUpdateTime;
    private final LiveData<String> selectedPersonName;
    private final LiveData<List<MonitoredPerson>> availablePersons;
    private final LiveData<ReceivedBtDataEntity> latestReading;

    public LivePostureViewModel(@NonNull Application application) {
        super(application);

        InnovaDatabase db = InnovaDatabase.getInstance(application);
        dao = db.receivedBtDataDao();
        personDao = db.monitoredPersonDao();
        personNameManager = PersonNameManager.getInstance(application);

        // Available persons for dropdown
        availablePersons = personDao.getAllMonitoredPersons();

        // Latest reading for selected sensor (reactive)
        latestReading = Transformations.switchMap(selectedSensorId, sensorId -> {
            if (sensorId == null || sensorId.isEmpty()) {
                return new MutableLiveData<>(null);
            }
            return dao.getLatestForSensor(sensorId);
        });

        // Transform reading to Posture
        currentPosture = Transformations.map(latestReading, entity -> {
            if (entity == null) return null;
            return PostureFactory.createPosture(entity.getReceivedMsg());
        });

        // Extract timestamp from latest reading
        lastUpdateTime = Transformations.map(latestReading, entity -> {
            if (entity == null) return null;
            return entity.getTimestamp();
        });

        // Selected person's display name (reactive to selection changes)
        selectedPersonName = Transformations.switchMap(selectedSensorId, sensorId -> {
            if (sensorId == null || sensorId.isEmpty()) {
                return new MutableLiveData<>("");
            }
            // Use LiveData from PersonNameManager for reactive updates
            return personNameManager.getPersonBySensorIdLive(sensorId) != null
                    ? Transformations.map(personNameManager.getPersonBySensorIdLive(sensorId), person -> {
                        if (person == null) return sensorId;
                        return person.getDisplayName();
                    })
                    : new MutableLiveData<>(sensorId);
        });
    }

    /**
     * Select a person to view their posture.
     * 
     * @param sensorId The sensor ID of the person to select, or null to clear selection
     */
    public void selectPerson(String sensorId) {
        selectedSensorId.setValue(sensorId);
    }

    /**
     * Get the currently selected sensor ID.
     */
    public String getSelectedSensorId() {
        return selectedSensorId.getValue();
    }

    /**
     * Get the list of all monitored persons for the dropdown.
     */
    public LiveData<List<MonitoredPerson>> getAvailablePersons() {
        return availablePersons;
    }

    /**
     * Get the current posture of the selected person.
     * Updates automatically when new data arrives.
     */
    public LiveData<Posture> getCurrentPosture() {
        return currentPosture;
    }

    /**
     * Get the timestamp of the last reading for the selected person.
     */
    public LiveData<Long> getLastUpdateTime() {
        return lastUpdateTime;
    }

    /**
     * Get the display name of the selected person.
     */
    public LiveData<String> getSelectedPersonName() {
        return selectedPersonName;
    }

    /**
     * Check if a person is currently selected.
     */
    public boolean hasSelection() {
        String sensorId = selectedSensorId.getValue();
        return sensorId != null && !sensorId.isEmpty();
    }
}
