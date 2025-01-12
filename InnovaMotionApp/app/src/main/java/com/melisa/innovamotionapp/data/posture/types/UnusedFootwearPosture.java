package com.melisa.innovamotionapp.data.posture.types;

import com.melisa.innovamotionapp.R;
import com.melisa.innovamotionapp.data.posture.Posture;

public class UnusedFootwearPosture extends Posture {
    @Override
    public int getRisc() {
        return R.string.posture_unused_footwear_risk;
    }

    @Override
    public int getTextCode() {
        return R.string.posture_unused_footwear_msg;
    }

    @Override
    public int getVideoCode() {
        return R.raw.neutilizat_video;
    }

    @Override
    public int getPictureCode() {
        return R.raw.neutilizat;
    }
    @Override
    public int getCalories() {
        return 0;
    }
}
