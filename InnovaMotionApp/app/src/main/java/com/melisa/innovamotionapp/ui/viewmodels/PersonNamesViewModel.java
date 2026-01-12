package com.melisa.innovamotionapp.ui.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.melisa.innovamotionapp.data.database.MonitoredPerson;
import com.melisa.innovamotionapp.utils.PersonNameManager;

import java.util.List;

/**
 * ViewModel for the Person Names management UI.
 * 
 * Provides access to all monitored persons and methods to update their display names.
 * Uses PersonNameManager for data operations.
 */
public class PersonNamesViewModel extends AndroidViewModel {

    private final PersonNameManager personNameManager;
    private final LiveData<List<MonitoredPerson>> allPersons;

    public PersonNamesViewModel(@NonNull Application application) {
        super(application);
        personNameManager = PersonNameManager.getInstance(application);
        allPersons = personNameManager.getAllPersonsLive();
    }

    /**
     * Get all monitored persons as LiveData.
     * Updates automatically when persons are added or names are changed.
     */
    public LiveData<List<MonitoredPerson>> getAllPersons() {
        return allPersons;
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
     * Check if there are any monitored persons.
     * 
     * @return true if the list is empty or null
     */
    public boolean isEmpty() {
        List<MonitoredPerson> persons = allPersons.getValue();
        return persons == null || persons.isEmpty();
    }
}
