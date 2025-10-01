package com.melisa.innovamotionapp.sync;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Monitors network connectivity changes and notifies listeners.
 * Uses modern Android ConnectivityManager API for reliable connectivity detection.
 */
public class NetworkConnectivityMonitor {
    private static final String TAG = "NetworkConnectivityMonitor";
    
    private final Context context;
    private final ConnectivityManager connectivityManager;
    private final CopyOnWriteArrayList<ConnectivityListener> listeners;
    private final Handler mainHandler;
    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean isConnected = false;

    public interface ConnectivityListener {
        void onConnectivityChanged(boolean isConnected);
    }

    public NetworkConnectivityMonitor(Context context) {
        this.context = context.getApplicationContext();
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.listeners = new CopyOnWriteArrayList<>();
        this.mainHandler = new Handler(Looper.getMainLooper());
        
        // Initialize current connectivity state
        this.isConnected = isCurrentlyConnected();
    }

    /**
     * Start monitoring network connectivity changes
     */
    public void startMonitoring() {
        if (networkCallback != null) {
            Log.w(TAG, "Already monitoring network connectivity");
            return;
        }

        NetworkRequest.Builder builder = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                Log.d(TAG, "Network available: " + network);
                updateConnectivityState(true);
            }

            @Override
            public void onLost(@NonNull Network network) {
                Log.d(TAG, "Network lost: " + network);
                updateConnectivityState(false);
            }

            @Override
            public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
                boolean hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                Log.d(TAG, "Network capabilities changed. Has internet: " + hasInternet);
                updateConnectivityState(hasInternet);
            }
        };

        try {
            connectivityManager.registerNetworkCallback(builder.build(), networkCallback);
            Log.d(TAG, "Network monitoring started");
        } catch (Exception e) {
            Log.e(TAG, "Failed to register network callback", e);
        }
    }

    /**
     * Stop monitoring network connectivity changes
     */
    public void stopMonitoring() {
        if (networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
                Log.d(TAG, "Network monitoring stopped");
            } catch (Exception e) {
                Log.e(TAG, "Failed to unregister network callback", e);
            }
            networkCallback = null;
        }
    }

    /**
     * Check current connectivity state
     */
    public boolean isCurrentlyConnected() {
        try {
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork == null) return false;

            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
            return capabilities != null &&
                   capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                   capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        } catch (Exception e) {
            Log.e(TAG, "Error checking connectivity", e);
            return false;
        }
    }

    /**
     * Add a connectivity listener
     */
    public void addListener(ConnectivityListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            // Immediately notify the new listener of current state
            mainHandler.post(() -> listener.onConnectivityChanged(isConnected));
        }
    }

    /**
     * Remove a connectivity listener
     */
    public void removeListener(ConnectivityListener listener) {
        listeners.remove(listener);
    }

    /**
     * Get current connectivity state
     */
    public boolean isConnected() {
        return isConnected;
    }

    /**
     * Update connectivity state and notify listeners
     */
    private void updateConnectivityState(boolean connected) {
        if (this.isConnected != connected) {
            this.isConnected = connected;
            Log.i(TAG, "Connectivity state changed: " + (connected ? "CONNECTED" : "DISCONNECTED"));
            
            // Notify all listeners on main thread
            mainHandler.post(() -> {
                for (ConnectivityListener listener : listeners) {
                    try {
                        listener.onConnectivityChanged(connected);
                    } catch (Exception e) {
                        Log.e(TAG, "Error notifying connectivity listener", e);
                    }
                }
            });
        }
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        stopMonitoring();
        listeners.clear();
    }
}
