package com.melisa.innovamotionapp.ui.viewmodels;

import static org.junit.Assert.*;

import com.melisa.innovamotionapp.data.posture.Posture;
import com.melisa.innovamotionapp.data.posture.PostureFactory;
import com.melisa.innovamotionapp.data.posture.types.FallingPosture;
import com.melisa.innovamotionapp.data.posture.types.SittingPosture;
import com.melisa.innovamotionapp.data.posture.types.StandingPosture;
import com.melisa.innovamotionapp.data.posture.types.UnknownPosture;
import com.melisa.innovamotionapp.data.posture.types.WalkingPosture;
import com.melisa.innovamotionapp.ui.models.PersonStatus;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Unit tests for SupervisorDashboardViewModel logic.
 * 
 * Tests sorting behavior, alert detection, and posture factory integration.
 */
public class SupervisorDashboardViewModelTest {

    // ========== Alert Detection Tests ==========

    @Test
    public void isAlert_fallingPosture_returnsTrue() {
        Posture posture = PostureFactory.createPosture("0xEF0112"); // Falling
        assertTrue(posture instanceof FallingPosture);
    }

    @Test
    public void isAlert_standingPosture_returnsFalse() {
        Posture posture = PostureFactory.createPosture("0xAB3311"); // Standing
        assertFalse(posture instanceof FallingPosture);
    }

    @Test
    public void isAlert_sittingPosture_returnsFalse() {
        Posture posture = PostureFactory.createPosture("0xAC4312"); // Sitting
        assertFalse(posture instanceof FallingPosture);
    }

    @Test
    public void isAlert_walkingPosture_returnsFalse() {
        Posture posture = PostureFactory.createPosture("0xBA3311"); // Walking
        assertFalse(posture instanceof FallingPosture);
    }

    @Test
    public void isAlert_unknownPosture_returnsFalse() {
        Posture posture = PostureFactory.createPosture("0xFFFFFF"); // Unknown
        assertFalse(posture instanceof FallingPosture);
    }

    // ========== Sorting Logic Tests ==========

    @Test
    public void sorting_alertsFirst() {
        List<PersonStatus> statuses = new ArrayList<>();
        
        Posture standing = new StandingPosture();
        Posture falling = new FallingPosture();
        
        statuses.add(new PersonStatus("s1", "Zeno", standing, 1000L, false));
        statuses.add(new PersonStatus("s2", "Ana", falling, 1000L, true));
        statuses.add(new PersonStatus("s3", "Maria", standing, 1000L, false));
        
        // Sort using the same logic as ViewModel
        Collections.sort(statuses, (a, b) -> {
            if (a.isAlert() != b.isAlert()) {
                return a.isAlert() ? -1 : 1;
            }
            return a.getDisplayName().compareToIgnoreCase(b.getDisplayName());
        });
        
        // Ana (alert) should be first
        assertEquals("Ana", statuses.get(0).getDisplayName());
        assertTrue(statuses.get(0).isAlert());
    }

    @Test
    public void sorting_alphabeticalWithinSameAlertStatus() {
        List<PersonStatus> statuses = new ArrayList<>();
        
        Posture standing = new StandingPosture();
        
        statuses.add(new PersonStatus("s1", "Zeno", standing, 1000L, false));
        statuses.add(new PersonStatus("s2", "Ana", standing, 1000L, false));
        statuses.add(new PersonStatus("s3", "Maria", standing, 1000L, false));
        
        // Sort using the same logic as ViewModel
        Collections.sort(statuses, (a, b) -> {
            if (a.isAlert() != b.isAlert()) {
                return a.isAlert() ? -1 : 1;
            }
            return a.getDisplayName().compareToIgnoreCase(b.getDisplayName());
        });
        
        // Should be alphabetically sorted
        assertEquals("Ana", statuses.get(0).getDisplayName());
        assertEquals("Maria", statuses.get(1).getDisplayName());
        assertEquals("Zeno", statuses.get(2).getDisplayName());
    }

    @Test
    public void sorting_multipleAlerts_alertsSortedAlphabetically() {
        List<PersonStatus> statuses = new ArrayList<>();
        
        Posture falling = new FallingPosture();
        Posture standing = new StandingPosture();
        
        statuses.add(new PersonStatus("s1", "Zeno", falling, 1000L, true));
        statuses.add(new PersonStatus("s2", "Ana", falling, 1000L, true));
        statuses.add(new PersonStatus("s3", "Maria", standing, 1000L, false));
        
        // Sort using the same logic as ViewModel
        Collections.sort(statuses, (a, b) -> {
            if (a.isAlert() != b.isAlert()) {
                return a.isAlert() ? -1 : 1;
            }
            return a.getDisplayName().compareToIgnoreCase(b.getDisplayName());
        });
        
        // Alerts first, alphabetically
        assertEquals("Ana", statuses.get(0).getDisplayName());
        assertTrue(statuses.get(0).isAlert());
        assertEquals("Zeno", statuses.get(1).getDisplayName());
        assertTrue(statuses.get(1).isAlert());
        // Non-alert last
        assertEquals("Maria", statuses.get(2).getDisplayName());
        assertFalse(statuses.get(2).isAlert());
    }

    @Test
    public void sorting_caseInsensitive() {
        List<PersonStatus> statuses = new ArrayList<>();
        
        Posture standing = new StandingPosture();
        
        statuses.add(new PersonStatus("s1", "zeno", standing, 1000L, false));
        statuses.add(new PersonStatus("s2", "Ana", standing, 1000L, false));
        statuses.add(new PersonStatus("s3", "MARIA", standing, 1000L, false));
        
        // Sort using the same logic as ViewModel
        Collections.sort(statuses, (a, b) -> {
            if (a.isAlert() != b.isAlert()) {
                return a.isAlert() ? -1 : 1;
            }
            return a.getDisplayName().compareToIgnoreCase(b.getDisplayName());
        });
        
        // Should be case-insensitive alphabetically sorted
        assertEquals("Ana", statuses.get(0).getDisplayName());
        assertEquals("MARIA", statuses.get(1).getDisplayName());
        assertEquals("zeno", statuses.get(2).getDisplayName());
    }

    // ========== Posture Factory Integration Tests ==========

    @Test
    public void postureFactory_standingCode_returnsStanding() {
        Posture posture = PostureFactory.createPosture("0xAB3311");
        assertTrue(posture instanceof StandingPosture);
    }

    @Test
    public void postureFactory_sittingCode_returnsSitting() {
        Posture posture = PostureFactory.createPosture("0xAC4312");
        assertTrue(posture instanceof SittingPosture);
    }

    @Test
    public void postureFactory_walkingCode_returnsWalking() {
        Posture posture = PostureFactory.createPosture("0xBA3311");
        assertTrue(posture instanceof WalkingPosture);
    }

    @Test
    public void postureFactory_fallingCode_returnsFalling() {
        Posture posture = PostureFactory.createPosture("0xEF0112");
        assertTrue(posture instanceof FallingPosture);
    }

    @Test
    public void postureFactory_unknownCode_returnsUnknown() {
        Posture posture = PostureFactory.createPosture("0xZZZZZZ");
        assertTrue(posture instanceof UnknownPosture);
    }

    @Test
    public void postureFactory_nullInput_returnsUnknown() {
        Posture posture = PostureFactory.createPosture(null);
        assertTrue(posture instanceof UnknownPosture);
    }

    @Test
    public void postureFactory_emptyInput_returnsUnknown() {
        Posture posture = PostureFactory.createPosture("");
        assertTrue(posture instanceof UnknownPosture);
    }

    @Test
    public void postureFactory_caseInsensitive() {
        // Uppercase
        Posture posture1 = PostureFactory.createPosture("0XAB3311");
        assertTrue(posture1 instanceof StandingPosture);
        
        // Lowercase
        Posture posture2 = PostureFactory.createPosture("0xab3311");
        assertTrue(posture2 instanceof StandingPosture);
        
        // Mixed case
        Posture posture3 = PostureFactory.createPosture("0xAb3311");
        assertTrue(posture3 instanceof StandingPosture);
    }

    // ========== Empty/Null Handling Tests ==========

    @Test
    public void emptyList_sorting_doesNotCrash() {
        List<PersonStatus> statuses = new ArrayList<>();
        
        // Sort should not crash on empty list
        Collections.sort(statuses, (a, b) -> {
            if (a.isAlert() != b.isAlert()) {
                return a.isAlert() ? -1 : 1;
            }
            return a.getDisplayName().compareToIgnoreCase(b.getDisplayName());
        });
        
        assertTrue(statuses.isEmpty());
    }

    @Test
    public void singleItem_sorting_doesNotCrash() {
        List<PersonStatus> statuses = new ArrayList<>();
        Posture standing = new StandingPosture();
        statuses.add(new PersonStatus("s1", "Ion", standing, 1000L, false));
        
        // Sort should not crash on single item
        Collections.sort(statuses, (a, b) -> {
            if (a.isAlert() != b.isAlert()) {
                return a.isAlert() ? -1 : 1;
            }
            return a.getDisplayName().compareToIgnoreCase(b.getDisplayName());
        });
        
        assertEquals(1, statuses.size());
        assertEquals("Ion", statuses.get(0).getDisplayName());
    }
}
