package com.melisa.innovamotionapp.bluetooth;
import android.content.Intent;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class BluetoothHelper {
    public interface BluetoothEnableCallback {
        void onBluetoothEnabled();
        void onBluetoothEnableFailed();
    }

    public static ActivityResultLauncher<Intent> createLauncher(AppCompatActivity activity, BluetoothEnableCallback callback) {
        return activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                        callback.onBluetoothEnabled();
                    } else {
                        callback.onBluetoothEnableFailed();
                    }
                }
        );
    }
}