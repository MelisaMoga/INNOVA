package com.melisa.pedonovation.PedometricSole;

/**
 * This class contains the data of pressure measurements, having 3 sensors.
 */
public class SensorTypeData {
    public SensorDataComponent sensor1;
    public SensorDataComponent sensor2;
    public SensorDataComponent sensor3;

    public SensorTypeData(SensorDataComponent sensor1, SensorDataComponent sensor2, SensorDataComponent sensor3) {
        this.sensor1 = sensor1;
        this.sensor2 = sensor2;
        this.sensor3 = sensor3;
    }

    public SensorState calculateMajorityState() {
        SensorState[] states = {
                sensor1.sensorState,
                sensor2.sensorState,
                sensor3.sensorState
        };

        int sum = 0;
        for (SensorState state : states) {
            sum += state.ordinal();
        }

        // Calculate average and round to nearest integer (0/1) -> (LOW=0/ HIGH=1)
        int majorityValue = (int) Math.round((double) sum / states.length);
        SensorState majorityState = SensorState.values()[majorityValue];

        return majorityState;
    }

    public boolean ifAtLeastOne(SensorState sensorState) {
        SensorState[] states = {
                sensor1.sensorState,
                sensor2.sensorState,
                sensor3.sensorState
        };
        for (SensorState state : states) {
            if (state == sensorState) {
                return true;
            }
        }
        return false;
    }
}