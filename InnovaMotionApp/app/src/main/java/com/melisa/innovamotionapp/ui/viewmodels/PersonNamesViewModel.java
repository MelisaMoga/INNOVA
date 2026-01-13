package com.melisa.innovamotionapp.ui.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.melisa.innovamotionapp.data.database.MonitoredPerson;
import com.melisa.innovamotionapp.sync.SensorAssignmentService;
import com.melisa.innovamotionapp.utils.PersonNameManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ViewModel for the Person Names management UI.
 * 
 * Provides access to all monitored persons and methods to update their display names.
 * Also handles supervisor assignment operations.
 * Uses PersonNameManager for name data operations.
 * Uses SensorAssignmentService for supervisor assignment operations.
 */
public class PersonNamesViewModel extends AndroidViewModel {

    private final PersonNameManager personNameManager;
    private final SensorAssignmentService assignmentService;
    private final LiveData<List<MonitoredPerson>> allPersons;
    
    // Map of sensorId -> supervisorEmail for UI binding
    private final MutableLiveData<Map<String, String>> sensorSupervisorMap;

    /**
     * Callback interface for assignment operations.
     */
    public interface AssignmentResultCallback {
        void onSuccess();
        void onError(String error);
    }

    public PersonNamesViewModel(@NonNull Application application) {
        super(application);
        personNameManager = PersonNameManager.getInstance(application);
        assignmentService = SensorAssignmentService.getInstance(application);
        allPersons = personNameManager.getAllPersonsLive();
        sensorSupervisorMap = new MutableLiveData<>(new HashMap<>());
        
        // Load initial assignment map
        loadAssignmentMap();
    }

    /**
     * Get all monitored persons as LiveData.
     * Updates automatically when persons are added or names are changed.
     */
    public LiveData<List<MonitoredPerson>> getAllPersons() {
        return allPersons;
    }

    /**
     * Get the supervisor assignment map.
     * Maps sensorId -> supervisorEmail.
     */
    public LiveData<Map<String, String>> getSensorSupervisorMap() {
        return sensorSupervisorMap;
    }

    /**
     * Update the display name for a sensor ID.
     * 
     * @param sensorId The sensor ID to update
     * @param newName  The new display name
     */
    public void updateDisplayName(@NonNull String sensorId, @NonNull String newName) {
        personNameManager.setDisplayName(sensorId, newName);
    }

    /**
     * Assign a supervisor to a sensor.
     * 
     * @param sensorId The sensor ID to assign
     * @param supervisorEmail The supervisor's email
     * @param callback Result callback
     */
    public void assignSupervisor(@NonNull String sensorId, @NonNull String supervisorEmail,
                                  @NonNull AssignmentResultCallback callback) {
        assignmentService.assignSupervisor(sensorId, supervisorEmail, 
                new SensorAssignmentService.AssignmentCallback() {
            @Override
            public void onSuccess() {
                // Update local map
                updateMapEntry(sensorId, supervisorEmail);
                callback.onSuccess();
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    /**
     * Unassign a supervisor from a sensor.
     * 
     * @param sensorId The sensor ID to unassign
     * @param callback Result callback
     */
    public void unassignSupervisor(@NonNull String sensorId, @NonNull AssignmentResultCallback callback) {
        assignmentService.unassignSupervisor(sensorId, new SensorAssignmentService.AssignmentCallback() {
            @Override
            public void onSuccess() {
                // Remove from local map
                removeMapEntry(sensorId);
                callback.onSuccess();
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    /**
     * Get the supervisor email for a specific sensor.
     * 
     * @param sensorId The sensor ID to look up
     * @return The supervisor email, or null if not assigned
     */
    @Nullable
    public String getSupervisorForSensor(@NonNull String sensorId) {
        Map<String, String> map = sensorSupervisorMap.getValue();
        if (map != null) {
            return map.get(sensorId);
        }
        return null;
    }

    /**
     * Refresh the assignment map from Firestore.
     */
    public void refreshAssignments() {
        loadAssignmentMap();
    }

    /**
     * Load all assignments for the current aggregator.
     */
    private void loadAssignmentMap() {
        assignmentService.getAssignmentMap(new SensorAssignmentService.AssignmentMapCallback() {
            @Override
            public void onResult(Map<String, String> map) {
                sensorSupervisorMap.postValue(map);
            }

            @Override
            public void onError(String error) {
                // Keep existing map on error
            }
        });
    }

    /**
     * Update a single entry in the map.
     */
    private void updateMapEntry(String sensorId, String supervisorEmail) {
        Map<String, String> currentMap = sensorSupervisorMap.getValue();
        if (currentMap == null) {
            currentMap = new HashMap<>();
        } else {
            currentMap = new HashMap<>(currentMap); // Create mutable copy
        }
        currentMap.put(sensorId, supervisorEmail);
        sensorSupervisorMap.postValue(currentMap);
    }

    /**
     * Remove a single entry from the map.
     */
    private void removeMapEntry(String sensorId) {
        Map<String, String> currentMap = sensorSupervisorMap.getValue();
        if (currentMap != null && currentMap.containsKey(sensorId)) {
            currentMap = new HashMap<>(currentMap); // Create mutable copy
            currentMap.remove(sensorId);
            sensorSupervisorMap.postValue(currentMap);
        }
    }

    /**
     * Check if there are any monitored persons.
     * 
     * @return true if the list is empty or null
     */
    public boolean isEmpty() {
        List<MonitoredPerson> persons = allPersons.getValue();
        return persons == null || persons.isEmpty();
    }
}
