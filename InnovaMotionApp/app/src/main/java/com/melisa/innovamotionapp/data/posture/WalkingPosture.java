package com.melisa.innovamotionapp.data.posture;

import com.melisa.innovamotionapp.R;

public class WalkingPosture extends Posture{
    @Override
    public int getRisc() {
        return R.string.posture_walking_risk;
    }

    @Override
    public int getTextCode() {
        return R.string.posture_walking_msg;
    }

    @Override
    public int getVideoCode() {
        return R.raw.mers_movie;
    }
}
