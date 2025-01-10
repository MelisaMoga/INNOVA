package com.melisa.innovamotionapp.data.posture;

import com.melisa.innovamotionapp.R;

public class StandingPosture extends Posture{
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
}
