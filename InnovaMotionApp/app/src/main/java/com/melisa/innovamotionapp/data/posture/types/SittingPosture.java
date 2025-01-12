package com.melisa.innovamotionapp.data.posture.types;

import com.melisa.innovamotionapp.R;
import com.melisa.innovamotionapp.data.posture.Posture;

public class SittingPosture extends Posture {
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

    @Override
    public int getPictureCode() {
        return R.raw.pe_scaun;
    }
    @Override
    public int getCalories() {
        return 2;
    }
}
