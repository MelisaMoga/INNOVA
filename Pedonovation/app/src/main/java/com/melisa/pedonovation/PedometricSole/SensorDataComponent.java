package com.melisa.pedonovation.PedometricSole;

public class SensorDataComponent {
    public int value;
    public double normalisedValue;
    public SensorState sensorState;

    public SensorDataComponent(int value, double normalisedValue, SensorState sensorState) {
        this.value = value;
        this.normalisedValue = normalisedValue;
        this.sensorState = sensorState;
    }

    public char getStateAsLetter() {
        return sensorState.toString().charAt(0);
    }
}