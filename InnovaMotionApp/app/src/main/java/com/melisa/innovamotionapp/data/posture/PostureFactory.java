package com.melisa.innovamotionapp.data.posture;

import android.util.Log;

import com.melisa.innovamotionapp.data.posture.types.FallingPosture;
import com.melisa.innovamotionapp.data.posture.types.SittingPosture;
import com.melisa.innovamotionapp.data.posture.types.StandingPosture;
import com.melisa.innovamotionapp.data.posture.types.UnknownPosture;
import com.melisa.innovamotionapp.data.posture.types.UnusedFootwearPosture;
import com.melisa.innovamotionapp.data.posture.types.WalkingPosture;

public class PostureFactory {

    private static final String TAG = "PostureFactory"; // Tag for logging

    /**
     * Creates a Posture object based on a received data string.
     * The input string is expected to be a hexadecimal representation (e.g., "0xAB3311").
     * Comparisons are case-insensitive for the input string.
     *
     * @param receivedData The input string representing the posture code.
     * @return A concrete Posture object corresponding to the received data,
     * or an UnknownPosture if the data is null, empty, or doesn't match any known posture.
     */
    public static Posture createPosture(String receivedData) {
        // Basic input validation: Check for null or empty string
        if (receivedData == null || receivedData.isEmpty()) {
            Log.w(TAG, "Received data is null or empty. Returning UnknownPosture.");
            return new UnknownPosture();
        }

        // Convert the received data to lowercase for case-insensitive comparison.
        // This handles "0xAB3311", "0XAB3311", "0xab3311", etc.
        String normalizedData = receivedData.toLowerCase();

        // Use a switch statement for string comparison.
        switch (normalizedData) {
            case "0x793248":
                Log.d(TAG, "Matched " + normalizedData + ": Returning UnusedFootwearPosture.");
                return new UnusedFootwearPosture();
            case "0xab3311": // Note: case is lowercase due to normalization
                Log.d(TAG, "Matched " + normalizedData + ": Returning StandingPosture.");
                return new StandingPosture();
            case "0xac4312": // Note: case is lowercase due to normalization
                Log.d(TAG, "Matched " + normalizedData + ": Returning SittingPosture.");
                return new SittingPosture();
            case "0xba3311": // Note: case is lowercase due to normalization
                Log.d(TAG, "Matched " + normalizedData + ": Returning WalkingPosture.");
                return new WalkingPosture();
            case "0xef0112": // Note: case is lowercase due to normalization
                Log.d(TAG, "Matched " + normalizedData + ": Returning FallingPosture.");
                return new FallingPosture();
            default:
                // Log when no match is found
                Log.i(TAG, "No matching posture found for received data: \"" + receivedData + "\". Returning UnknownPosture.");
                return new UnknownPosture();
        }
    }
}