package com.melisa.innovamotionapp.data.posture.types;

import com.melisa.innovamotionapp.R;
import com.melisa.innovamotionapp.data.posture.Posture;

public class WalkingPosture extends Posture {
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

    @Override
    public int getPictureCode() {
        return R.raw.mers;
    }
}
