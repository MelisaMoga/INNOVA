package com.melisa.innovamotionapp.utils;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Centralized logging utility for InnovaMotionApp.
 * Provides consistent logging across the application with different log levels,
 * automatic tag generation, and production-safe logging.
 * 
 * Usage:
 * Logger.d("MainActivity", "User logged in successfully");
 * Logger.e("BluetoothService", "Connection failed", exception);
 * Logger.i("StatisticsActivity", "Chart data updated");
 */
public class Logger {
    
    private static final String APP_TAG = "InnovaMotion";
    private static final boolean ENABLE_DEBUG_LOGS = true; // Set to false in production
    private static final boolean ENABLE_VERBOSE_LOGS = false; // Usually false in production
    
    /**
     * Debug log - for detailed debugging information
     * @param tag The tag to identify the source (usually class name)
     * @param message The message to log
     */
    public static void d(@NonNull String tag, @NonNull String message) {
        if (ENABLE_DEBUG_LOGS) {
            Log.d(formatTag(tag), message);
        }
    }
    
    /**
     * Debug log with exception
     * @param tag The tag to identify the source
     * @param message The message to log
     * @param throwable The exception to log
     */
    public static void d(@NonNull String tag, @NonNull String message, @Nullable Throwable throwable) {
        if (ENABLE_DEBUG_LOGS) {
            Log.d(formatTag(tag), message, throwable);
        }
    }
    
    /**
     * Info log - for general information
     * @param tag The tag to identify the source
     * @param message The message to log
     */
    public static void i(@NonNull String tag, @NonNull String message) {
        Log.i(formatTag(tag), message);
    }
    
    /**
     * Info log with exception
     * @param tag The tag to identify the source
     * @param message The message to log
     * @param throwable The exception to log
     */
    public static void i(@NonNull String tag, @NonNull String message, @Nullable Throwable throwable) {
        Log.i(formatTag(tag), message, throwable);
    }
    
    /**
     * Warning log - for potential issues
     * @param tag The tag to identify the source
     * @param message The message to log
     */
    public static void w(@NonNull String tag, @NonNull String message) {
        Log.w(formatTag(tag), message);
    }
    
    /**
     * Warning log with exception
     * @param tag The tag to identify the source
     * @param message The message to log
     * @param throwable The exception to log
     */
    public static void w(@NonNull String tag, @NonNull String message, @Nullable Throwable throwable) {
        Log.w(formatTag(tag), message, throwable);
    }
    
    /**
     * Error log - for errors and exceptions
     * @param tag The tag to identify the source
     * @param message The message to log
     */
    public static void e(@NonNull String tag, @NonNull String message) {
        Log.e(formatTag(tag), message);
    }
    
    /**
     * Error log with exception
     * @param tag The tag to identify the source
     * @param message The message to log
     * @param throwable The exception to log
     */
    public static void e(@NonNull String tag, @NonNull String message, @Nullable Throwable throwable) {
        Log.e(formatTag(tag), message, throwable);
    }
    
    /**
     * Verbose log - for very detailed debugging (usually disabled in production)
     * @param tag The tag to identify the source
     * @param message The message to log
     */
    public static void v(@NonNull String tag, @NonNull String message) {
        if (ENABLE_VERBOSE_LOGS) {
            Log.v(formatTag(tag), message);
        }
    }
    
    /**
     * Verbose log with exception
     * @param tag The tag to identify the source
     * @param message The message to log
     * @param throwable The exception to log
     */
    public static void v(@NonNull String tag, @NonNull String message, @Nullable Throwable throwable) {
        if (ENABLE_VERBOSE_LOGS) {
            Log.v(formatTag(tag), message, throwable);
        }
    }
    
    /**
     * Logs Bluetooth-specific events with consistent formatting
     * @param tag The source tag
     * @param deviceAddress The Bluetooth device address
     * @param event The event description
     */
    public static void bluetooth(@NonNull String tag, @NonNull String deviceAddress, @NonNull String event) {
        i(tag, String.format("BT[%s]: %s", deviceAddress, event));
    }
    
    /**
     * Logs user interaction events
     * @param tag The source tag
     * @param action The user action performed
     */
    public static void userAction(@NonNull String tag, @NonNull String action) {
        i(tag, String.format("USER_ACTION: %s", action));
    }
    
    /**
     * Logs performance-related information
     * @param tag The source tag
     * @param operation The operation being measured
     * @param durationMs The duration in milliseconds
     */
    public static void performance(@NonNull String tag, @NonNull String operation, long durationMs) {
        d(tag, String.format("PERF: %s took %dms", operation, durationMs));
    }
    
    /**
     * Formats the tag with app prefix for easier filtering in logs
     * @param tag The original tag
     * @return Formatted tag with app prefix
     */
    private static String formatTag(@NonNull String tag) {
        return APP_TAG + "_" + tag;
    }
    
    /**
     * Utility method to get the calling class name automatically
     * Useful when you don't want to specify the tag manually
     * @return The simple name of the calling class
     */
    public static String getCallingClassName() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        // Index 3 because: 0=getStackTrace, 1=getCallingClassName, 2=calling method, 3=actual caller
        if (stackTrace.length > 3) {
            String fullClassName = stackTrace[3].getClassName();
            return fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
        }
        return "Unknown";
    }
}
