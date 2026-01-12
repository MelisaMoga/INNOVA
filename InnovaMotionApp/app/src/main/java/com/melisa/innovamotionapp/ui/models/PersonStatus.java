package com.melisa.innovamotionapp.ui.models;

import androidx.annotation.NonNull;

import com.melisa.innovamotionapp.data.posture.Posture;

import java.util.Objects;

/**
 * Model representing the current status of a monitored person.
 * Used in the Supervisor Dashboard to display all persons in a grid.
 */
public class PersonStatus {
    
    @NonNull
    private final String sensorId;
    
    @NonNull
    private final String displayName;
    
    @NonNull
    private final Posture currentPosture;
    
    private final long lastUpdateTime;
    
    private final boolean isAlert;

    public PersonStatus(@NonNull String sensorId, 
                        @NonNull String displayName, 
                        @NonNull Posture currentPosture, 
                        long lastUpdateTime, 
                        boolean isAlert) {
        this.sensorId = sensorId;
        this.displayName = displayName;
        this.currentPosture = currentPosture;
        this.lastUpdateTime = lastUpdateTime;
        this.isAlert = isAlert;
    }

    @NonNull
    public String getSensorId() {
        return sensorId;
    }

    @NonNull
    public String getDisplayName() {
        return displayName;
    }

    @NonNull
    public Posture getCurrentPosture() {
        return currentPosture;
    }

    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    public boolean isAlert() {
        return isAlert;
    }

    /**
     * Get the resource ID for the posture icon.
     * Delegates to the current posture's getPictureCode().
     */
    public int getPostureIconRes() {
        return currentPosture.getPictureCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PersonStatus that = (PersonStatus) o;
        return lastUpdateTime == that.lastUpdateTime &&
                isAlert == that.isAlert &&
                sensorId.equals(that.sensorId) &&
                displayName.equals(that.displayName) &&
                currentPosture.getClass().equals(that.currentPosture.getClass());
    }

    @Override
    public int hashCode() {
        return Objects.hash(sensorId, displayName, currentPosture.getClass().getName(), 
                lastUpdateTime, isAlert);
    }

    @NonNull
    @Override
    public String toString() {
        return "PersonStatus{" +
                "sensorId='" + sensorId + '\'' +
                ", displayName='" + displayName + '\'' +
                ", posture=" + currentPosture.getClass().getSimpleName() +
                ", lastUpdate=" + lastUpdateTime +
                ", isAlert=" + isAlert +
                '}';
    }
}
