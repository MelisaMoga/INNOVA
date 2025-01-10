package com.melisa.innovamotionapp.data.posture;

import com.melisa.innovamotionapp.R;

public class FallingPosture extends Posture{
    @Override
    public int getRisc() {
        return R.string.posture_falling_risk;
    }

    @Override
    public int getTextCode() {
        return R.string.posture_falling_msg;
    }

    @Override
    public int getVideoCode() {
        return R.raw.cadere_video;
    }
}
