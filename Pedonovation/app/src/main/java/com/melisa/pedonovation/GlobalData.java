package com.melisa.pedonovation;

import android.app.Application;
import android.os.Handler;

import com.melisa.pedonovation.Interfaces.ICurrentActivityManager;
import com.melisa.pedonovation.BluetoothCore.ConnectionData;
import com.melisa.pedonovation.MessageHandlers.MessageHandler;

public class GlobalData extends Application {

    public ConnectionData connectionData1;
    public ConnectionData connectionData2;
    public Handler messageHandler;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public void setActivityManagerForHandler(ICurrentActivityManager activityManager) {
        messageHandler = new Handler(new MessageHandler(activityManager));

        if (isConnection1Alive()) {
            connectionData1.connectionThread.setHandler(messageHandler);
        }

        if (isConnection2Alive()) {
            connectionData2.connectionThread.setHandler(messageHandler);
        }
    }

    public boolean isConnection1Alive() {
        return connectionData1 != null && connectionData1.connectionThread.isAlive();
    }

    public boolean isConnection2Alive() {
        return connectionData2 != null && connectionData2.connectionThread.isAlive();
    }

}