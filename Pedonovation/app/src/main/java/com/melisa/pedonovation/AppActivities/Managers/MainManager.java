package com.melisa.pedonovation.AppActivities.Managers;

import static android.content.ContentValues.TAG;

import static com.melisa.pedonovation.Utilities.FILE_NAME_FORMAT;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.melisa.pedonovation.AppActivities.MainActivity;
import com.melisa.pedonovation.BluetoothCore.BluetoothDeviceReceivedData;
import com.melisa.pedonovation.GlobalData;
import com.melisa.pedonovation.Interfaces.ICurrentActivityManager;
import com.melisa.pedonovation.Interfaces.UILogger;
import com.melisa.pedonovation.PedometricSole.BodyPosition;
import com.melisa.pedonovation.PedometricSole.PedometricData;
import com.melisa.pedonovation.PedometricSole.SensorState;
import com.melisa.pedonovation.R;
import com.melisa.pedonovation.Utilities;

import java.io.File;
import java.io.FileOutputStream;

public class MainManager implements UILogger, ICurrentActivityManager {
    public final MainActivity activity;
    private final Button startBt;
    private final EditText editText1;
    private final TextView feedbackDevice1, feedbackDevice2;
    private GlobalData globalData;
    private final Object lock = new Object();

    // Constants
    private final String FEEDBACK_DEVICE_TV = "Acc: %s %s %s | LineCount: %d\n" + "Cap: %s %s %s | diff: %d";
    private int lineCount1 = 0;
    private int lineCount2 = 0;

    // TEMP
    double rollRotation1, pitchRotation1, rollRotation2, pitchRotation2;
    public boolean shouldWriteToFile = false;

    // string constants
    String CONST_STRING_IN_PICIOARE = "în picioare";

    public MainManager(MainActivity activity, GlobalData globalData) {
        this.activity = activity;
        startBt = activity.binding.startBt;
        editText1 = activity.binding.editTextTextMultiLine;
        feedbackDevice1 = activity.binding.feedbackDevice1;
        feedbackDevice2 = activity.binding.feedbackDevice2;

        this.globalData = globalData;
    }

    public void updateUI() {
        // Make start_bt available only when both bt connections are valid
        boolean areBothConnected = globalData.isConnection1Alive() && globalData.isConnection2Alive();
        startBt.setEnabled(areBothConnected);
    }

    public void writeToFile(String fileName, String content) {

        File path = activity.getExternalMediaDirs()[0];

        try {
            FileOutputStream writer = new FileOutputStream(new File(path, fileName), true);
            writer.write(content.getBytes());
            writer.close();
//            log("Write to file:" + fileName);

        } catch (Exception e) {
            Log.d(TAG, String.format("[LOG] %s", e));
        }
    }

    public void deleteFile(String path_of_file) {
        // if file exists, then delete
        File file = new File(path_of_file);
        if(file.exists()){
            file.delete();
        }
    }

    // Add methods to add messages to queues
    public void addMessageToQueue1(BluetoothDeviceReceivedData bluetoothDeviceReceivedData) {
        globalData.connectionData1.msgQueue.add(bluetoothDeviceReceivedData);
        syncMessages();
    }

    public void addMessageToQueue2(BluetoothDeviceReceivedData bluetoothDeviceReceivedData) {
        globalData.connectionData2.msgQueue.add(bluetoothDeviceReceivedData);
        syncMessages();
    }

    public void deleteDataFiles() {
        File path = activity.getExternalMediaDirs()[0];
        String fileName = String.format(FILE_NAME_FORMAT, "1");
        String path_of_file = new File(path, fileName).getPath();
        deleteFile(path_of_file);

        fileName = String.format(FILE_NAME_FORMAT, "2");
        path_of_file = new File(path, fileName).getPath();
        deleteFile(path_of_file);
    }


    // Method to process messages in sync
    private void syncMessages() {
        synchronized (lock) {
            while (!globalData.connectionData1.msgQueue.isEmpty() &&
                    !globalData.connectionData2.msgQueue.isEmpty()) {
                BluetoothDeviceReceivedData msg1 = globalData.connectionData1.msgQueue.poll();
                BluetoothDeviceReceivedData msg2 = globalData.connectionData2.msgQueue.poll();

                PedometricData pedometricData1 = msg1.pedometricData;
                PedometricData pedometricData2 = msg2.pedometricData;

                // Calculate Roll and Pitch for accelerometer data of device 1

                rollRotation1 = computeRollRotation(
                        pedometricData1.accelerometerData.sensor1.value,
                        pedometricData1.accelerometerData.sensor2.value,
                        pedometricData1.accelerometerData.sensor3.value
                );
                pitchRotation1 = computePitchRotation(
                        pedometricData1.accelerometerData.sensor1.value,
                        pedometricData1.accelerometerData.sensor2.value,
                        pedometricData1.accelerometerData.sensor3.value
                );
                // Calculate Roll and Pitch for accelerometer data of device 2
                rollRotation2 = computeRollRotation(
                        pedometricData2.accelerometerData.sensor1.value,
                        pedometricData2.accelerometerData.sensor2.value,
                        pedometricData2.accelerometerData.sensor3.value
                );
                pitchRotation2 = computePitchRotation(
                        pedometricData2.accelerometerData.sensor1.value,
                        pedometricData2.accelerometerData.sensor2.value,
                        pedometricData2.accelerometerData.sensor3.value
                );

                // Process messages here
                processMessage1(msg1);
                processMessage2(msg2);

                interpretData(msg1, msg2);
            }
        }
    }

    private double computeRollRotation(double x_value, double y_value, double z_value) {
        return Math.atan(y_value / Math.sqrt(Math.pow(x_value, 2) + Math.pow(z_value, 2))) * 180 / Math.PI;
    }

    private double computePitchRotation(double x_value, double y_value, double z_value) {
        return Math.atan(-1 * x_value / Math.sqrt(Math.pow(y_value, 2) + Math.pow(z_value, 2))) * 180 / Math.PI;
    }

    // AICI VIN LA INTERPRETARE DATELE <-------------------
    private void interpretData(BluetoothDeviceReceivedData msg1, BluetoothDeviceReceivedData msg2) {
        // msg1 este pachetul obiect de tipul BluetoothDeviceReceivedData primit de la device1
        // msg2 este pachetul obiect de tipul BluetoothDeviceReceivedData primit de la device2
        // Extragem datele de la fiecare dispozitiv
        PedometricData pedometricData1 = msg1.pedometricData;
        PedometricData pedometricData2 = msg2.pedometricData;

        BodyPosition position = determinePosition(pedometricData1, pedometricData2,
                rollRotation1, pitchRotation1, rollRotation2, pitchRotation2);

        switch (position) {
            case IN_PICIOARE:
                activity.binding.imageView4.setImageResource(R.drawable.position_standing_up);
                break;
            case IN_SEZUT:
                activity.binding.imageView4.setImageResource(R.drawable.position_sitting_on_chair);
                break;
            case MERGE:
                activity.binding.imageView4.setImageResource(R.drawable.position_walking);
                break;
            case CAZUT:
                activity.binding.imageView4.setImageResource(R.drawable.falling);
                break;
            case INVALID:
                // Pastreaza ultima poza
                break;
            default:
                // Pastreaza ultima poza
                log_and_toast("UNKNOWN position");
                break;
        }

    }

    private BodyPosition determinePosition(PedometricData pedometricData1, PedometricData pedometricData2,
                                           double rollRotation1, double pitchRotation1,
                                           double rollRotation2, double pitchRotation2) {


        // Obținem stările senzorilor pentru fiecare dispozitiv
        char accS0Char1 = pedometricData1.accelerometerData.sensor1.getStateAsLetter();
        char accS1Char1 = pedometricData1.accelerometerData.sensor2.getStateAsLetter();
        char accS2Char1 = pedometricData1.accelerometerData.sensor3.getStateAsLetter();
        char capS0Char1 = pedometricData1.capacitiveData.sensor1.getStateAsLetter();
        char capS1Char1 = pedometricData1.capacitiveData.sensor2.getStateAsLetter();
        char capS2Char1 = pedometricData1.capacitiveData.sensor3.getStateAsLetter();

        char accS0Char2 = pedometricData2.accelerometerData.sensor1.getStateAsLetter();
        char accS1Char2 = pedometricData2.accelerometerData.sensor2.getStateAsLetter();
        char accS2Char2 = pedometricData2.accelerometerData.sensor3.getStateAsLetter();
        char capS0Char2 = pedometricData2.capacitiveData.sensor1.getStateAsLetter();
        char capS1Char2 = pedometricData2.capacitiveData.sensor2.getStateAsLetter();
        char capS2Char2 = pedometricData2.capacitiveData.sensor3.getStateAsLetter();

        // Daca majoritatea senzorilor capacitivi al unui picior sunt HIGH atunci sta in picioare
        if (pedometricData1.capacitiveData.calculateMajorityState() == SensorState.HIGH ||
                pedometricData2.capacitiveData.calculateMajorityState() == SensorState.HIGH) {
            return BodyPosition.IN_PICIOARE;

        // Daca macar un senzor capacitiv de la un picior este high si celalalt majoritar pe low
            // atunci subiectul merge
        } else if (((pedometricData1.capacitiveData.ifAtLeastOne(SensorState.HIGH) &&
                    pedometricData2.capacitiveData.calculateMajorityState() == SensorState.LOW) ||
                    (pedometricData2.capacitiveData.ifAtLeastOne(SensorState.HIGH) &&
                    pedometricData1.capacitiveData.calculateMajorityState() == SensorState.LOW))) {
            return BodyPosition.MERGE;

        // Daca senzorii capacitivi de la ambele picioare sunt majoritar LOW si unghiul de rotatie
            // al picioarelor nu depaseste 15% in vreo inclinatie atunci este pe scaun
        } else if (pedometricData1.capacitiveData.calculateMajorityState() == SensorState.LOW &&
                (pedometricData1.capacitiveData.calculateMajorityState() == SensorState.LOW) &&
                (rollRotation1 <= 15 && rollRotation1 >= -15) && (rollRotation2 <= 15 && rollRotation2 >= -15) &&
                (pitchRotation1 <= 15 && pitchRotation1 >= -15) && (pitchRotation2 <= 15 && pitchRotation2 >= -15)) {
            return BodyPosition.IN_SEZUT;

        // Daca senzorii capacitivi de la ambele picioare sunt majoritar LOW si unghiul de rotatie
            // al picioarelor depaseste 15% in vreo inclinatie atunci este cazut
            // SI ambele pitchRotations sunt in aceeasi directie
        } else if (pedometricData1.capacitiveData.calculateMajorityState() == SensorState.LOW &&
                pedometricData1.capacitiveData.calculateMajorityState() == SensorState.LOW &&
                ((pitchRotation1 >= 0 && pitchRotation2 >= 0) || (pitchRotation1 <= 0 && pitchRotation2 <= 0))){
            return BodyPosition.CAZUT;
        } else {
            return BodyPosition.INVALID;
        }
    }

    @SuppressLint("MissingPermission")
    private void processMessage1(BluetoothDeviceReceivedData msg) {
        BluetoothDevice bluetoothDevice = msg.device;
        PedometricData pedometricData = msg.pedometricData;
        String receivedData = msg.receivedData;

        // bluetoothHelper.showToast(String.format("[mainRead][%s] %s", bluetoothDevice.getName(), data));
        lineCount1 += 1;
//            feedbackDevice1.setText(String.format("%d (diff: %d)", lineCount1, lineCount1-lineCount2));

        char accS0Char = pedometricData.accelerometerData.sensor1.sensorState.toString().charAt(0);
        char accS1Char = pedometricData.accelerometerData.sensor2.sensorState.toString().charAt(0);
        char accS2Char = pedometricData.accelerometerData.sensor3.sensorState.toString().charAt(0);

        char capS0Char = pedometricData.capacitiveData.sensor1.sensorState.toString().charAt(0);
        char capS1Char = pedometricData.capacitiveData.sensor2.sensorState.toString().charAt(0);
        char capS2Char = pedometricData.capacitiveData.sensor3.sensorState.toString().charAt(0);
        feedbackDevice1.setText(String.format(FEEDBACK_DEVICE_TV,
                accS0Char, accS1Char, accS2Char, lineCount1,
                capS0Char, capS1Char, capS2Char, lineCount1 - lineCount2));
        feedbackDevice1.append(String.format(" Pitch1: %,.2f; Roll1: %,.2f", pitchRotation1, rollRotation1));
//            editText1.append(receivedData);

        String fileName = String.format(Utilities.FILE_NAME_FORMAT, "1");
        if (shouldWriteToFile) {
            writeToFile(fileName, pedometricData.toString() + '\n');
        }
    }

    @SuppressLint("MissingPermission")
    private void processMessage2(BluetoothDeviceReceivedData msg) {
        BluetoothDevice bluetoothDevice = msg.device;
        PedometricData pedometricData = msg.pedometricData;
        String receivedData = msg.receivedData;


        lineCount2 += 1;
//            feedbackDevice2.setText(String.format("%d (diff: %d)", lineCount2, lineCount2-lineCount1));

        char accS0Char = pedometricData.accelerometerData.sensor1.sensorState.toString().charAt(0);
        char accS1Char = pedometricData.accelerometerData.sensor2.sensorState.toString().charAt(0);
        char accS2Char = pedometricData.accelerometerData.sensor3.sensorState.toString().charAt(0);

        char capS0Char = pedometricData.capacitiveData.sensor1.sensorState.toString().charAt(0);
        char capS1Char = pedometricData.capacitiveData.sensor2.sensorState.toString().charAt(0);
        char capS2Char = pedometricData.capacitiveData.sensor3.sensorState.toString().charAt(0);
        feedbackDevice2.setText(String.format(FEEDBACK_DEVICE_TV,
                accS0Char, accS1Char, accS2Char, lineCount2,
                capS0Char, capS1Char, capS2Char, lineCount2 - lineCount1));
        feedbackDevice2.append(String.format(" Pitch2: %,.2f; Roll2: %,.2f", pitchRotation2, rollRotation2));

//            editText2.append(receivedData);
        // Update the EditText with the received data

        String fileName = String.format(Utilities.FILE_NAME_FORMAT, "2");
        if (shouldWriteToFile) {
            writeToFile(fileName, pedometricData.toString() + '\n');
        }
    }


    public void showToast(String msg) {
        Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void log_and_toast(String stringToLog) {
        Log.d(TAG, String.format("[LOG] %s", stringToLog));
        showToast(stringToLog);
    }

    @Override
    public void handleAnyState(BluetoothDeviceReceivedData bluetoothDeviceReceivedData) {
        updateUI();
    }


    @SuppressLint("MissingPermission")
    @Override
    public void handleConnecting(BluetoothDeviceReceivedData bluetoothDeviceReceivedData) {
        log_and_toast(String.format("Trying to connect to... %s", bluetoothDeviceReceivedData.device.getName()));
    }

    @Override
    public void handleConnected(BluetoothDeviceReceivedData bluetoothDeviceReceivedData) {

    }

    @SuppressLint("MissingPermission")
    @Override
    public void handleConnectionFailed(BluetoothDeviceReceivedData bluetoothDeviceReceivedData) {
        BluetoothDevice bluetoothDevice = bluetoothDeviceReceivedData.device;
        log_and_toast(String.format("Connection Failed %s", bluetoothDevice.getName()));
    }

    @Override
    public void handleMsgReceived(BluetoothDeviceReceivedData bluetoothDeviceReceivedData) {
        BluetoothDevice bluetoothDevice = bluetoothDeviceReceivedData.device;

        if (globalData.connectionData1 != null && bluetoothDevice == globalData.connectionData1.device) {
            if (globalData.isConnection1Alive() && globalData.isConnection2Alive()) {
                addMessageToQueue1(bluetoothDeviceReceivedData);
            } else {
                processMessage1(bluetoothDeviceReceivedData);
            }
        }
        if (globalData.connectionData2 != null && bluetoothDevice == globalData.connectionData2.device) {
            if (globalData.isConnection1Alive() && globalData.isConnection2Alive()) {
                addMessageToQueue2(bluetoothDeviceReceivedData);
            } else {
                processMessage2(bluetoothDeviceReceivedData);
            }
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void handleDisconnected(BluetoothDeviceReceivedData bluetoothDeviceReceivedData) {
        BluetoothDevice bluetoothDevice = bluetoothDeviceReceivedData.device;
        log_and_toast(String.format("Device Disconnected %s", bluetoothDevice.getName()));
    }
}
