package com.melisa.innovamotionapp.receivers;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BluetoothStateReceiver extends BroadcastReceiver {
    private final BluetoothStateListener listener;

    public interface BluetoothStateListener {
        void onStateChanged(int state);
    }

    public BluetoothStateReceiver(BluetoothStateListener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            if (listener != null) {
                listener.onStateChanged(state);
            }
        }
    }
}
