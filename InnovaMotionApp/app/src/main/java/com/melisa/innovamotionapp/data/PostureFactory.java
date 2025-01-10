package com.melisa.innovamotionapp.data;

import com.melisa.innovamotionapp.data.posture.FallingPosture;
import com.melisa.innovamotionapp.data.posture.Posture;
import com.melisa.innovamotionapp.data.posture.SittingPosture;
import com.melisa.innovamotionapp.data.posture.StandingPosture;
import com.melisa.innovamotionapp.data.posture.UnknownPosture;
import com.melisa.innovamotionapp.data.posture.UnusedFootwearPosture;
import com.melisa.innovamotionapp.data.posture.WalkingPosture;

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