package com.melisa.pedonovation.PedometricSole;


import androidx.annotation.NonNull;

public class PedometricData {
    public final SensorTypeData accelerometerData;
    public final SensorTypeData capacitiveData;


    public PedometricData(SensorTypeData accelerometerData, SensorTypeData capacitiveData) {
        this.accelerometerData = accelerometerData;
        this.capacitiveData = capacitiveData;
    }


    @NonNull
    @Override
    public String toString() {
        // Formatted to be saved in dataFile
        return accelerometerData.sensor1.value + " " + accelerometerData.sensor2.value + " " + accelerometerData.sensor3.value + " " +
                capacitiveData.sensor1.normalisedValue + " " + capacitiveData.sensor2.normalisedValue + " " + capacitiveData.sensor3.normalisedValue;
    }

}
