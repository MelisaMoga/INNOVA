package com.melisa.innovamotionapp.data.posture.types;

import com.melisa.innovamotionapp.R;
import com.melisa.innovamotionapp.data.posture.Posture;

public class UnknownPosture extends Posture {
    @Override
    public int getRisc() {
        return R.string.posture_unknown_risk;
    }

    @Override
    public int getTextCode() {
        return R.string.posture_unknown_msg;
    }

    @Override
    public int getVideoCode() {
        return -1;
    }

    @Override
    public int getPictureCode() {
        return R.raw.unknown;
    }
    @Override
    public int getCalories() {
        return 0;
    }
}
