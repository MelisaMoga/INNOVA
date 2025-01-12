package com.melisa.innovamotionapp.data.posture;

import com.melisa.innovamotionapp.data.posture.types.FallingPosture;
import com.melisa.innovamotionapp.data.posture.types.SittingPosture;
import com.melisa.innovamotionapp.data.posture.types.StandingPosture;
import com.melisa.innovamotionapp.data.posture.types.UnknownPosture;
import com.melisa.innovamotionapp.data.posture.types.UnusedFootwearPosture;
import com.melisa.innovamotionapp.data.posture.types.WalkingPosture;

public class PostureFactory {

    public static Posture createPosture(String receivedData) {
        switch (receivedData) {
            case "0x793248":
                return new UnusedFootwearPosture();  // Example, customize based on your actual posture class
            case "0xAB3311":
                return new StandingPosture();  // Stând în picioare
            case "0xAC4312":
                return new SittingPosture();  // Stând în șezut
            case "0xBA3311":
                return new WalkingPosture();  // Pășit
            case "0xEF0112":
                return new FallingPosture();  // Cădere
            default:
                return new UnknownPosture();  // Default case if no match found
        }
    }
}