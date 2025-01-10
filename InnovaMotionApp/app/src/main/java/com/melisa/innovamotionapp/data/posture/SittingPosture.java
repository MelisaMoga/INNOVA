package com.melisa.innovamotionapp.data.posture;

import com.melisa.innovamotionapp.R;

public class SittingPosture extends Posture{
    @Override
    public int getRisc() {
        return R.string.posture_sitting_risk;
    }

    @Override
    public int getTextCode() {
        return R.string.posture_sitting_msg;
    }

    @Override
    public int getVideoCode() {
        return R.raw.pe_scaun_movie;
    }
}
