package com.melisa.pedonovation;

import static android.content.ContentValues.TAG;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;

import com.melisa.pedonovation.AppActivities.MainActivity;
import com.melisa.pedonovation.BluetoothCore.BluetoothHelper;
import com.melisa.pedonovation.BluetoothCore.ConnectionData;
import com.melisa.pedonovation.BluetoothCore.ConnectionThread;
import com.melisa.pedonovation.MessageHandlers.MessageHandler;
import com.melisa.pedonovation.PedometricSole.PedometricData;
import com.melisa.pedonovation.PedometricSole.SensorTypeData;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


public class DEBUG_CLASS {


    public class TestDataThread extends ConnectionThread {

        private final String receivedData;

        public TestDataThread(BluetoothDevice device, Handler handler, String receivedData) {
            super(device, handler);
            this.receivedData = receivedData;
        }

        private void aux() {


            List<String> lines = pedometricHelper.textToArrayList(receivedData);
            for (String line : lines) {
                Pair<SensorTypeData, SensorTypeData> result = pedometricHelper.processPedometricDataLine(line);
                SensorTypeData accData = result.first;
                SensorTypeData capData = result.second;

                pedometricData = new PedometricData(accData, capData);
                sendMessageToMainThread(MessageHandler.STATE_MESSAGE_RECEIVED, line + '\n');

                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }


        }

        @Override
        public void run() {
            super.run();
            // Process TestData line by line
            // then send it
            aux();

            this.cancel();

        }
    }

    public String readFromFile(File file) {
        StringBuilder content = new StringBuilder();

        try (FileInputStream reader = new FileInputStream(file);
             InputStreamReader inputStreamReader = new InputStreamReader(reader);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                content.append(line).append('\n');
            }
        } catch (Exception e) {
            Log.d(TAG, String.format("[LOG] %s", e));
        }

        return content.toString();
    }

    private void deleteFile(File file) {
        // if file exists, then delete
        if(file.exists()){
            file.delete();
        }
    }

    public DEBUG_CLASS(MainActivity activity, GlobalData globalData) {

        BluetoothHelper bluetoothHelper = new BluetoothHelper(activity);


        Set<BluetoothDevice> bondedDevices = bluetoothHelper.getBondedDevices();
        Iterator iterator = bondedDevices.iterator();

        BluetoothDevice bluetoothDevice1 = (BluetoothDevice) iterator.next();
        BluetoothDevice bluetoothDevice2 = (BluetoothDevice) iterator.next();


        File externalAppStoragePath = activity.getExternalMediaDirs()[0];

        // Delete TestData_1/2.txt
        deleteFile(new File(externalAppStoragePath, "TestData_1.txt"));
        deleteFile(new File(externalAppStoragePath, "TestData_2.txt"));

        // Read from file in com.melisa.app
        String fileName1 = "AD4_Melisa_mers2_stang.txt";
        File file1 = new File(externalAppStoragePath, fileName1);
        String data1 = readFromFile(file1);

        String fileName2 = "AD4_Melisa_mers2_drept.txt";
        File file2 = new File(externalAppStoragePath, fileName2);
        String data2 = readFromFile(file2);



        TestDataThread testDataThread1 = new TestDataThread(bluetoothDevice1, globalData.messageHandler, data1);
        globalData.connectionData1 = new ConnectionData(bluetoothDevice1, testDataThread1);

        TestDataThread testDataThread2 = new TestDataThread(bluetoothDevice2, globalData.messageHandler, data2);
        globalData.connectionData2 = new ConnectionData(bluetoothDevice2, testDataThread2);

        testDataThread1.start();
        testDataThread2.start();
    }


}
