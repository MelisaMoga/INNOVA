package com.melisa.innovamotionapp.ui.adapters;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.melisa.innovamotionapp.R;
import com.melisa.innovamotionapp.uistuff.OnDeviceClickListener;

import java.util.ArrayList;

public class NearbyDeviceDataAdapter extends ArrayAdapter<BluetoothDevice> {
    private LayoutInflater mLayoutInflater;
    private ArrayList<BluetoothDevice> mDevices;
    private int mViewResourceId;
    private OnDeviceClickListener onDeviceClickListener;

    public NearbyDeviceDataAdapter(Context context, int tvResourceId, ArrayList<BluetoothDevice> devices) {
        super(context, tvResourceId, devices);
        this.mDevices = devices;
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mViewResourceId = tvResourceId;
    }

    public void setOnDeviceClickListener(OnDeviceClickListener onDeviceClickListener) {
        this.onDeviceClickListener = onDeviceClickListener;
    }

    @SuppressLint("MissingPermission")
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = mLayoutInflater.inflate(mViewResourceId, null);

        BluetoothDevice device = mDevices.get(position);

        if (device != null) {
            TextView deviceName = (TextView) convertView.findViewById(R.id.tvDeviceName);
            TextView deviceAddress = (TextView) convertView.findViewById(R.id.tvDeviceAddress);
            LinearLayout scanDeviceContainer = (LinearLayout) convertView.findViewById(R.id.scanDeviceContainer);

            if (deviceName != null) {
                deviceName.setText(device.getName());
            }
            if (deviceAddress != null) {
                deviceAddress.setText(device.getAddress());
            }

            scanDeviceContainer.setOnClickListener(v -> {
                if (onDeviceClickListener != null) {
                    onDeviceClickListener.onDeviceClick(device);
                }
            });
        }

        return convertView;
    }

}
