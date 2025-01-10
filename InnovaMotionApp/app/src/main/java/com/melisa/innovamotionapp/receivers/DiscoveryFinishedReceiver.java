package com.melisa.innovamotionapp.receivers;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class DiscoveryFinishedReceiver extends BroadcastReceiver {
    private final DiscoveryFinishedListener listener;

    public interface DiscoveryFinishedListener {
        void onDiscoveryFinished();
    }

    public DiscoveryFinishedReceiver(DiscoveryFinishedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction())) {
            if (listener != null) {
                listener.onDiscoveryFinished();
            }
        }
    }
}
