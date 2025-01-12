package com.melisa.innovamotionapp.data.posture.types;

import com.melisa.innovamotionapp.R;
import com.melisa.innovamotionapp.data.posture.Posture;

public class FallingPosture extends Posture {
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

    @Override
    public int getPictureCode() {
        return R.raw.cadere;
    }

    @Override
    public int getCalories() {
        return 10;
    }
}
