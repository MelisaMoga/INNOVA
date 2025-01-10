package com.melisa.innovamotionapp.data.posture;

import com.melisa.innovamotionapp.R;

public class UnusedFootwearPosture extends Posture{
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
}
