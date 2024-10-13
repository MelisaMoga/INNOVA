package com.melisa.pedonovation.PedometricSole;

import android.util.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class PedometricHelper {//face normalizarea datelor pedometrice si pune sensor state "H" sau "L", si procesarea a nu stiu ce paaaaaannggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggg
    private static final double SENSOR_STATE_THRESHOLD = 0.5;
    private final int[] minAccelerometerValues;
    private final int[] maxAccelerometerValues;
    private final int[] minCapacitiveValues;
    private final int[] maxCapacitiveValues;

    public PedometricHelper() {

        this.minAccelerometerValues = new int[3];
        this.maxAccelerometerValues = new int[3];
        this.minCapacitiveValues = new int[3];
        this.maxCapacitiveValues = new int[3];
        Arrays.fill(minAccelerometerValues, Integer.MAX_VALUE);
        Arrays.fill(maxAccelerometerValues, Integer.MIN_VALUE);
        Arrays.fill(minCapacitiveValues, Integer.MAX_VALUE);
        Arrays.fill(maxCapacitiveValues, Integer.MIN_VALUE);
//        processPedometricStringData();

//        pedometricData.NormaliseAccelerometerData();
//        pedometricData.NormaliseCapacitiveData();

//        GenerateGraphData();
    }


    /**
     * Converts a multi-line string into a list of strings, where each element is a line from the input text.
     *
     * @param text The input multi-line string.
     * @return A list of strings, each containing a line from the input text.
     */
    public List<String> textToArrayList(String text) {
        // Create an ArrayList to hold the lines of text.
        List<String> list = new ArrayList<>();
        try {
            // Create a BufferedReader to read the input text line by line.
            final BufferedReader reader = new BufferedReader(new StringReader(text));
            String line;
            // Read each line from the BufferedReader until no more lines are available.
            while ((line = reader.readLine()) != null) {
                // Add the line to the list.
                list.add(line);
            }
            // Close the BufferedReader to release resources.
            reader.close();
        } catch (final IOException e) {
            // Print the stack trace if an IOException occurs.
            e.printStackTrace();
        }
        // Return the list of lines.
        return list;
    }


    private void updateMinMaxValues(int[] values, int[] minAccelerometerValues, int[] maxAccelerometerValues) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] < minAccelerometerValues[i]) {
                minAccelerometerValues[i] = values[i];
            }
            if (values[i] > maxAccelerometerValues[i]) {
                maxAccelerometerValues[i] = values[i];
            }
        }
    }

    private SensorState getSensorState(double sensorValue) {
        SensorState sensorState;
        if (sensorValue < SENSOR_STATE_THRESHOLD) {
            sensorState = SensorState.LOW;
        } else {
            sensorState = SensorState.HIGH;
        }
        return sensorState;
    }

    private SensorDataComponent[] createSensorComponents(int[] values, int[] minAccelerometerValues, int[] maxAccelerometerValues) {

        // Normalise values AND create the sensor components putting in each one,
        //  the value and the normalised value
        double[] normalisedValues = new double[3];
        SensorDataComponent[] sensorDataComponents = new SensorDataComponent[3];

        for (int i = 0; i < values.length; i++) {
            // Normalise value
            normalisedValues[i] = (double) (values[i] - minAccelerometerValues[i]) /
                    (maxAccelerometerValues[i] - minAccelerometerValues[i]);

            // If divided by 0 (the values is undefined), just put 0
            if (Double.isNaN(normalisedValues[i])) {
                normalisedValues[i] = 0;
            }

            // Get SensorState using the normalised value
            SensorState sensorState = getSensorState(normalisedValues[i]);

            // Now we have the value and the normalisedValue so we can create sensorsComponents
            sensorDataComponents[i] = new SensorDataComponent(values[i], normalisedValues[i], sensorState);
        }
        return sensorDataComponents;
    }

    private SensorTypeData processAccTypeData(int decValue1, int decValue2, int decValue3) {
        int[] values = {decValue1, decValue2, decValue3};

        // Update min/max values
        updateMinMaxValues(values, minAccelerometerValues, maxAccelerometerValues);

        // Normalise values AND create the sensor components putting in each one,
        //  the value and the normalised value
        SensorDataComponent[] sensorDataComponents = createSensorComponents(values, minAccelerometerValues, maxAccelerometerValues);

        // We use the earlier created sensorsComponents to store them in the capacitiveData
        SensorTypeData sensorTypeData = new SensorTypeData(
                sensorDataComponents[0],
                sensorDataComponents[1],
                sensorDataComponents[2]
        );

        return sensorTypeData;
    }

    private SensorTypeData processCapTypeData(int decValue1, int decValue2, int decValue3) {
        int[] values = {decValue1, decValue2, decValue3};

        // Update min/max values
        updateMinMaxValues(values, minCapacitiveValues, maxCapacitiveValues);

        // Normalise values AND create the sensor components putting in each one,
        //  the value and the normalised value
        SensorDataComponent[] sensorDataComponents = createSensorComponents(values, minCapacitiveValues, maxCapacitiveValues);

        // We use the earlier created sensorsComponents to store them in the capacitiveData
        SensorTypeData sensorTypeData = new SensorTypeData(
                sensorDataComponents[0],
                sensorDataComponents[1],
                sensorDataComponents[2]
        );

        return sensorTypeData;
    }


    public Pair<SensorTypeData, SensorTypeData> processPedometricDataLine(String dataLine) {

        String[] hexValues = dataLine.split(" ");

        int convertedDecValue0 = Integer.valueOf(hexValues[0], 16).shortValue();
        int convertedDecValue1 = Integer.valueOf(hexValues[1], 16).shortValue();
        int convertedDecValue2 = Integer.valueOf(hexValues[2], 16).shortValue();

        int convertedDecValue3 = Integer.parseInt(hexValues[3], 16);
        int convertedDecValue4 = Integer.parseInt(hexValues[4], 16);
        int convertedDecValue5 = Integer.parseInt(hexValues[5], 16);

        SensorTypeData accData = processAccTypeData(
                convertedDecValue0,
                convertedDecValue1,
                convertedDecValue2
        );
        SensorTypeData capData = processCapTypeData(
                convertedDecValue3,
                convertedDecValue4,
                convertedDecValue5
        );

        return new Pair<>(accData, capData);
    }

}
