package com.melisa.innovamotionapp.utils;

import androidx.annotation.StringRes;

import com.melisa.innovamotionapp.R;

/**
 * Pre-defined test scenarios for developer testing mode.
 * 
 * Each scenario generates specific mock data to test different aspects
 * of the multi-user posture monitoring system.
 */
public enum TestScenario {
    
    /**
     * Basic multi-person scenario with 3 sensors showing varied postures.
     * Generates 10 readings over simulated 30 seconds.
     */
    BASIC_MULTI_PERSON(
            "basic_multi_person",
            R.string.scenario_basic_multi_person_title,
            R.string.scenario_basic_multi_person_desc,
            3,  // sensor count
            10, // readings per sensor
            false, // include fall
            false  // include stale
    ),
    
    /**
     * Fall detection scenario with a single sensor showing fall posture.
     * Tests alert triggering and notification system.
     */
    FALL_DETECTION(
            "fall_detection",
            R.string.scenario_fall_detection_title,
            R.string.scenario_fall_detection_desc,
            1,
            5,
            true,  // include fall
            false
    ),
    
    /**
     * Stale data scenario with sensors having old timestamps.
     * Tests the yellow status indicator in dashboard.
     */
    STALE_DATA(
            "stale_data",
            R.string.scenario_stale_data_title,
            R.string.scenario_stale_data_desc,
            3,
            5,
            false,
            true  // include stale
    ),
    
    /**
     * High volume scenario with 10 sensors and 50 readings each.
     * Stress tests UI performance and database operations.
     */
    HIGH_VOLUME(
            "high_volume",
            R.string.scenario_high_volume_title,
            R.string.scenario_high_volume_desc,
            10,
            50,
            false,
            false
    ),
    
    /**
     * Name resolution scenario with sensors having pre-assigned display names.
     * Tests person name display throughout the UI.
     */
    NAME_RESOLUTION(
            "name_resolution",
            R.string.scenario_name_resolution_title,
            R.string.scenario_name_resolution_desc,
            5,
            10,
            false,
            false
    ),
    
    /**
     * Mixed states scenario with active, stale, and alert sensors.
     * Tests dashboard sorting and color-coded status indicators.
     */
    MIXED_STATES(
            "mixed_states",
            R.string.scenario_mixed_states_title,
            R.string.scenario_mixed_states_desc,
            6,
            8,
            true,  // include fall
            true   // include stale
    );
    
    private final String id;
    @StringRes private final int titleResId;
    @StringRes private final int descriptionResId;
    private final int sensorCount;
    private final int readingsPerSensor;
    private final boolean includeFall;
    private final boolean includeStale;
    
    TestScenario(String id, @StringRes int titleResId, @StringRes int descriptionResId,
                 int sensorCount, int readingsPerSensor, boolean includeFall, boolean includeStale) {
        this.id = id;
        this.titleResId = titleResId;
        this.descriptionResId = descriptionResId;
        this.sensorCount = sensorCount;
        this.readingsPerSensor = readingsPerSensor;
        this.includeFall = includeFall;
        this.includeStale = includeStale;
    }
    
    public String getId() {
        return id;
    }
    
    @StringRes
    public int getTitleResId() {
        return titleResId;
    }
    
    @StringRes
    public int getDescriptionResId() {
        return descriptionResId;
    }
    
    public int getSensorCount() {
        return sensorCount;
    }
    
    public int getReadingsPerSensor() {
        return readingsPerSensor;
    }
    
    public boolean shouldIncludeFall() {
        return includeFall;
    }
    
    public boolean shouldIncludeStale() {
        return includeStale;
    }
    
    /**
     * Get total number of readings this scenario will generate.
     */
    public int getTotalReadings() {
        return sensorCount * readingsPerSensor;
    }
}
