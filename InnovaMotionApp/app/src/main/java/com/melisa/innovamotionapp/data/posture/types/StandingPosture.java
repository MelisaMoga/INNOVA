package com.melisa.innovamotionapp.data.posture.types;

import com.melisa.innovamotionapp.R;
import com.melisa.innovamotionapp.data.posture.Posture;

public class StandingPosture extends Posture {
    @Override
    public int getRisc() {
        return R.string.posture_standing_risk;
    }

    @Override
    public int getTextCode() {
        return R.string.posture_standing_msg;
    }

    @Override
    public int getVideoCode() {
        return R.raw.in_picioare_movie;
    }

    @Override
    public int getPictureCode() {
        return R.raw.in_picioare;
    }
}
