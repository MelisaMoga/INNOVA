package com.melisa.innovamotionapp.data.posture;

import com.melisa.innovamotionapp.R;

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
}
