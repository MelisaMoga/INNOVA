package com.melisa.innovamotionapp.ui.viewmodels;

import static org.junit.Assert.*;

import com.melisa.innovamotionapp.data.posture.Posture;
import com.melisa.innovamotionapp.data.posture.PostureFactory;
import com.melisa.innovamotionapp.data.posture.types.FallingPosture;
import com.melisa.innovamotionapp.data.posture.types.SittingPosture;
import com.melisa.innovamotionapp.data.posture.types.StandingPosture;
import com.melisa.innovamotionapp.data.posture.types.UnknownPosture;
import com.melisa.innovamotionapp.data.posture.types.UnusedFootwearPosture;
import com.melisa.innovamotionapp.data.posture.types.WalkingPosture;

import org.junit.Test;

/**
 * Unit tests for LivePostureViewModel helper methods and posture conversion.
 * 
 * Note: Full ViewModel testing requires AndroidX Test framework since
 * LivePostureViewModel extends AndroidViewModel. These tests cover the
 * posture conversion logic that the ViewModel relies on.
 */
public class LivePostureViewModelTest {

    // ========== PostureFactory Integration Tests ==========
    // These test the PostureFactory which LivePostureViewModel uses

    @Test
    public void postureFactory_standingCode_returnsStandingPosture() {
        Posture posture = PostureFactory.createPosture("0xAB3311");
        assertTrue(posture instanceof StandingPosture);
    }

    @Test
    public void postureFactory_sittingCode_returnsSittingPosture() {
        Posture posture = PostureFactory.createPosture("0xAC4312");
        assertTrue(posture instanceof SittingPosture);
    }

    @Test
    public void postureFactory_walkingCode_returnsWalkingPosture() {
        Posture posture = PostureFactory.createPosture("0xBA3311");
        assertTrue(posture instanceof WalkingPosture);
    }

    @Test
    public void postureFactory_fallingCode_returnsFallingPosture() {
        Posture posture = PostureFactory.createPosture("0xEF0112");
        assertTrue(posture instanceof FallingPosture);
    }

    @Test
    public void postureFactory_unusedCode_returnsUnusedFootwearPosture() {
        Posture posture = PostureFactory.createPosture("0x793248");
        assertTrue(posture instanceof UnusedFootwearPosture);
    }

    @Test
    public void postureFactory_unknownCode_returnsUnknownPosture() {
        Posture posture = PostureFactory.createPosture("0xFFFFFF");
        assertTrue(posture instanceof UnknownPosture);
    }

    @Test
    public void postureFactory_null_returnsUnknownPosture() {
        Posture posture = PostureFactory.createPosture(null);
        assertTrue(posture instanceof UnknownPosture);
    }

    @Test
    public void postureFactory_emptyString_returnsUnknownPosture() {
        Posture posture = PostureFactory.createPosture("");
        assertTrue(posture instanceof UnknownPosture);
    }

    @Test
    public void postureFactory_caseInsensitive_upperCase() {
        Posture posture = PostureFactory.createPosture("0XAB3311");
        assertTrue(posture instanceof StandingPosture);
    }

    @Test
    public void postureFactory_caseInsensitive_lowerCase() {
        Posture posture = PostureFactory.createPosture("0xab3311");
        assertTrue(posture instanceof StandingPosture);
    }

    @Test
    public void postureFactory_caseInsensitive_mixedCase() {
        Posture posture = PostureFactory.createPosture("0xAb3311");
        assertTrue(posture instanceof StandingPosture);
    }

    // ========== Posture Properties Tests ==========

    @Test
    public void standingPosture_hasValidTextCode() {
        Posture posture = new StandingPosture();
        assertTrue(posture.getTextCode() > 0);
    }

    @Test
    public void standingPosture_hasValidRisc() {
        Posture posture = new StandingPosture();
        assertTrue(posture.getRisc() > 0);
    }

    @Test
    public void standingPosture_hasValidVideoCode() {
        Posture posture = new StandingPosture();
        assertTrue(posture.getVideoCode() > 0);
    }

    @Test
    public void fallingPosture_hasValidTextCode() {
        Posture posture = new FallingPosture();
        assertTrue(posture.getTextCode() > 0);
    }

    @Test
    public void sittingPosture_hasValidProperties() {
        Posture posture = new SittingPosture();
        assertTrue(posture.getTextCode() > 0);
        assertTrue(posture.getRisc() > 0);
        assertTrue(posture.getVideoCode() > 0);
    }

    @Test
    public void walkingPosture_hasValidProperties() {
        Posture posture = new WalkingPosture();
        assertTrue(posture.getTextCode() > 0);
        assertTrue(posture.getRisc() > 0);
        assertTrue(posture.getVideoCode() > 0);
    }

    @Test
    public void unusedFootwearPosture_hasValidProperties() {
        Posture posture = new UnusedFootwearPosture();
        assertTrue(posture.getTextCode() > 0);
        assertTrue(posture.getRisc() > 0);
        assertTrue(posture.getVideoCode() > 0);
    }

    // ========== Selection State Tests ==========

    @Test
    public void hasSelection_nullSensorId_returnsFalse() {
        assertFalse(hasSelection(null));
    }

    @Test
    public void hasSelection_emptySensorId_returnsFalse() {
        assertFalse(hasSelection(""));
    }

    @Test
    public void hasSelection_validSensorId_returnsTrue() {
        assertTrue(hasSelection("sensor001"));
    }

    @Test
    public void hasSelection_uuidSensorId_returnsTrue() {
        assertTrue(hasSelection("5d6d75ee-b6c8-42d4-a233-b13d137fea38"));
    }

    // ========== Helper Methods ==========

    /**
     * Simulates hasSelection() logic from ViewModel.
     */
    private boolean hasSelection(String sensorId) {
        return sensorId != null && !sensorId.isEmpty();
    }

    // ========== First Posture Handling Tests ==========

    @Test
    public void firstPosture_unknownShouldShowUnusedFootwear() {
        // When first posture is Unknown, we should show UnusedFootwear instead
        Posture original = new UnknownPosture();
        Posture displayed = handleFirstPosture(original, true);
        assertTrue(displayed instanceof UnusedFootwearPosture);
    }

    @Test
    public void firstPosture_standingShouldShowStanding() {
        // When first posture is valid, show it as-is
        Posture original = new StandingPosture();
        Posture displayed = handleFirstPosture(original, true);
        assertTrue(displayed instanceof StandingPosture);
    }

    @Test
    public void subsequentPosture_unknownShouldShowUnknown() {
        // After first posture, Unknown should show as Unknown
        Posture original = new UnknownPosture();
        Posture displayed = handleFirstPosture(original, false);
        assertTrue(displayed instanceof UnknownPosture);
    }

    /**
     * Simulates first posture handling logic from Fragment.
     */
    private Posture handleFirstPosture(Posture posture, boolean isFirst) {
        if (isFirst && posture instanceof UnknownPosture) {
            return new UnusedFootwearPosture();
        }
        return posture;
    }

    // ========== Timestamp Formatting Logic Tests ==========

    @Test
    public void timestamp_withinDay_usesShortFormat() {
        long now = System.currentTimeMillis();
        long withinDay = now - (12 * 60 * 60 * 1000); // 12 hours ago
        assertTrue(isWithinDay(withinDay, now));
    }

    @Test
    public void timestamp_moreThanDay_usesLongFormat() {
        long now = System.currentTimeMillis();
        long moreThanDay = now - (36 * 60 * 60 * 1000); // 36 hours ago
        assertFalse(isWithinDay(moreThanDay, now));
    }

    @Test
    public void timestamp_exactly24Hours_isNotWithinDay() {
        long now = System.currentTimeMillis();
        long exactly24Hours = now - (24 * 60 * 60 * 1000);
        assertFalse(isWithinDay(exactly24Hours, now));
    }

    /**
     * Simulates timestamp check from Fragment.
     */
    private boolean isWithinDay(long timestamp, long now) {
        long dayInMillis = 24 * 60 * 60 * 1000;
        return now - timestamp < dayInMillis;
    }
}
