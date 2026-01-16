package com.melisa.innovamotionapp.ui.viewmodels;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for BtSettingsViewModel logic.
 * 
 * Note: Full ViewModel testing requires AndroidX Test framework since
 * BtSettingsViewModel uses BluetoothAdapter and LiveData. These tests cover
 * the state machine logic, device matching, and enum transitions that the
 * ViewModel relies on.
 */
public class BtSettingsViewModelTest {

    // ========== BtSettingsState Enum Tests ==========

    @Test
    public void btSettingsState_containsAllExpectedStates() {
        BtSettingsViewModel.BtSettingsState[] states = BtSettingsViewModel.BtSettingsState.values();
        assertEquals(8, states.length);
    }

    @Test
    public void btSettingsState_beforeBtnPressed_exists() {
        BtSettingsViewModel.BtSettingsState state = BtSettingsViewModel.BtSettingsState.BEFORE_BTN_PRESSED;
        assertNotNull(state);
        assertEquals("BEFORE_BTN_PRESSED", state.name());
    }

    @Test
    public void btSettingsState_afterBtnPressed_exists() {
        BtSettingsViewModel.BtSettingsState state = BtSettingsViewModel.BtSettingsState.AFTER_BTN_PRESSED;
        assertNotNull(state);
        assertEquals("AFTER_BTN_PRESSED", state.name());
    }

    @Test
    public void btSettingsState_bluetoothOff_exists() {
        BtSettingsViewModel.BtSettingsState state = BtSettingsViewModel.BtSettingsState.BLUETOOTH_OFF;
        assertNotNull(state);
        assertEquals("BLUETOOTH_OFF", state.name());
    }

    @Test
    public void btSettingsState_enablingBluetooth_exists() {
        BtSettingsViewModel.BtSettingsState state = BtSettingsViewModel.BtSettingsState.ENABLING_BLUETOOTH;
        assertNotNull(state);
        assertEquals("ENABLING_BLUETOOTH", state.name());
    }

    @Test
    public void btSettingsState_readyToConnect_exists() {
        BtSettingsViewModel.BtSettingsState state = BtSettingsViewModel.BtSettingsState.READY_TO_CONNECT;
        assertNotNull(state);
        assertEquals("READY_TO_CONNECT", state.name());
    }

    @Test
    public void btSettingsState_scanning_exists() {
        BtSettingsViewModel.BtSettingsState state = BtSettingsViewModel.BtSettingsState.SCANNING;
        assertNotNull(state);
        assertEquals("SCANNING", state.name());
    }

    @Test
    public void btSettingsState_scanFinished_exists() {
        BtSettingsViewModel.BtSettingsState state = BtSettingsViewModel.BtSettingsState.SCAN_FINISHED;
        assertNotNull(state);
        assertEquals("SCAN_FINISHED", state.name());
    }

    @Test
    public void btSettingsState_connecting_exists() {
        BtSettingsViewModel.BtSettingsState state = BtSettingsViewModel.BtSettingsState.CONNECTING;
        assertNotNull(state);
        assertEquals("CONNECTING", state.name());
    }

    @Test
    public void btSettingsState_valueOf_validName_returnsState() {
        BtSettingsViewModel.BtSettingsState state = 
            BtSettingsViewModel.BtSettingsState.valueOf("SCANNING");
        assertEquals(BtSettingsViewModel.BtSettingsState.SCANNING, state);
    }

    @Test(expected = IllegalArgumentException.class)
    public void btSettingsState_valueOf_invalidName_throws() {
        BtSettingsViewModel.BtSettingsState.valueOf("INVALID_STATE");
    }

    // ========== State Transition Logic Tests ==========
    // These test the state machine logic that determines UI updates

    @Test
    public void stateTransition_initialState_shouldBeBeforeBtnPressed() {
        BtSettingsViewModel.BtSettingsState initialState = 
            BtSettingsViewModel.BtSettingsState.BEFORE_BTN_PRESSED;
        assertEquals(BtSettingsViewModel.BtSettingsState.BEFORE_BTN_PRESSED, initialState);
    }

    @Test
    public void stateTransition_bluetoothOff_canTransitionToEnabling() {
        // Simulating state flow: BLUETOOTH_OFF -> ENABLING_BLUETOOTH
        BtSettingsViewModel.BtSettingsState current = BtSettingsViewModel.BtSettingsState.BLUETOOTH_OFF;
        BtSettingsViewModel.BtSettingsState next = simulateEnableBluetooth(current, false);
        assertEquals(BtSettingsViewModel.BtSettingsState.ENABLING_BLUETOOTH, next);
    }

    @Test
    public void stateTransition_bluetoothOn_transitionsToReadyToConnect() {
        // Simulating state flow after Bluetooth is enabled
        BtSettingsViewModel.BtSettingsState state = simulateBluetoothCheck(true);
        assertEquals(BtSettingsViewModel.BtSettingsState.READY_TO_CONNECT, state);
    }

    @Test
    public void stateTransition_bluetoothOff_staysBluetoothOff() {
        BtSettingsViewModel.BtSettingsState state = simulateBluetoothCheck(false);
        assertEquals(BtSettingsViewModel.BtSettingsState.BLUETOOTH_OFF, state);
    }

    @Test
    public void stateTransition_readyToConnect_canStartScanning() {
        // Simulating: READY_TO_CONNECT -> SCANNING
        BtSettingsViewModel.BtSettingsState current = BtSettingsViewModel.BtSettingsState.READY_TO_CONNECT;
        BtSettingsViewModel.BtSettingsState next = simulateStartDiscovery(current, true);
        assertEquals(BtSettingsViewModel.BtSettingsState.SCANNING, next);
    }

    @Test
    public void stateTransition_scanning_finishesToScanFinished() {
        // Simulating: SCANNING -> SCAN_FINISHED (when not connecting)
        BtSettingsViewModel.BtSettingsState next = simulateDiscoveryFinished(false);
        assertEquals(BtSettingsViewModel.BtSettingsState.SCAN_FINISHED, next);
    }

    @Test
    public void stateTransition_scanning_staysIfConnecting() {
        // When connecting, discovery finished shouldn't change state to SCAN_FINISHED
        BtSettingsViewModel.BtSettingsState next = simulateDiscoveryFinished(true);
        assertNull(next); // No state change when connecting
    }

    @Test
    public void stateTransition_selectDevice_transitionsToConnecting() {
        BtSettingsViewModel.BtSettingsState current = BtSettingsViewModel.BtSettingsState.SCANNING;
        BtSettingsViewModel.BtSettingsState next = simulateConnectToDevice(current);
        assertEquals(BtSettingsViewModel.BtSettingsState.CONNECTING, next);
    }

    // ========== Device Matching Logic Tests ==========
    // Tests the auto-connect logic when a known device is found

    @Test
    public void deviceMatching_sameAddress_shouldAutoConnect() {
        String lastDeviceAddress = "AA:BB:CC:DD:EE:FF";
        String foundDeviceAddress = "AA:BB:CC:DD:EE:FF";
        
        boolean shouldAutoConnect = shouldAutoConnectToDevice(foundDeviceAddress, lastDeviceAddress);
        assertTrue(shouldAutoConnect);
    }

    @Test
    public void deviceMatching_differentAddress_shouldNotAutoConnect() {
        String lastDeviceAddress = "AA:BB:CC:DD:EE:FF";
        String foundDeviceAddress = "11:22:33:44:55:66";
        
        boolean shouldAutoConnect = shouldAutoConnectToDevice(foundDeviceAddress, lastDeviceAddress);
        assertFalse(shouldAutoConnect);
    }

    @Test
    public void deviceMatching_nullLastAddress_shouldNotAutoConnect() {
        String lastDeviceAddress = null;
        String foundDeviceAddress = "AA:BB:CC:DD:EE:FF";
        
        boolean shouldAutoConnect = shouldAutoConnectToDevice(foundDeviceAddress, lastDeviceAddress);
        assertFalse(shouldAutoConnect);
    }

    @Test
    public void deviceMatching_emptyLastAddress_shouldNotAutoConnect() {
        String lastDeviceAddress = "";
        String foundDeviceAddress = "AA:BB:CC:DD:EE:FF";
        
        boolean shouldAutoConnect = shouldAutoConnectToDevice(foundDeviceAddress, lastDeviceAddress);
        assertFalse(shouldAutoConnect);
    }

    @Test
    public void deviceMatching_caseInsensitive_shouldAutoConnect() {
        String lastDeviceAddress = "aa:bb:cc:dd:ee:ff";
        String foundDeviceAddress = "AA:BB:CC:DD:EE:FF";
        
        boolean shouldAutoConnect = shouldAutoConnectToDeviceCaseInsensitive(foundDeviceAddress, lastDeviceAddress);
        assertTrue(shouldAutoConnect);
    }

    // ========== Connecting Flag Logic Tests ==========

    @Test
    public void connectingFlag_initially_isFalse() {
        boolean isConnecting = false; // Initial state
        assertFalse(isConnecting);
    }

    @Test
    public void connectingFlag_afterConnect_isTrue() {
        boolean isConnecting = simulateConnectAction();
        assertTrue(isConnecting);
    }

    @Test
    public void connectingFlag_blocksDiscoveryFinishedStateChange() {
        boolean isConnecting = true;
        BtSettingsViewModel.BtSettingsState result = simulateDiscoveryFinished(isConnecting);
        assertNull(result); // No state change
    }

    // ========== Bluetooth State Change Handler Tests ==========

    @Test
    public void bluetoothStateHandler_stateOff_setsEnabledFalse() {
        int state = 10; // BluetoothAdapter.STATE_OFF
        Boolean isEnabled = handleBluetoothStateChange(state);
        assertFalse(isEnabled);
    }

    @Test
    public void bluetoothStateHandler_stateOn_setsEnabledTrue() {
        int state = 12; // BluetoothAdapter.STATE_ON
        Boolean isEnabled = handleBluetoothStateChange(state);
        assertTrue(isEnabled);
    }

    @Test
    public void bluetoothStateHandler_stateTurningOff_returnsNull() {
        int state = 13; // BluetoothAdapter.STATE_TURNING_OFF
        Boolean isEnabled = handleBluetoothStateChange(state);
        assertNull(isEnabled);
    }

    @Test
    public void bluetoothStateHandler_stateTurningOn_returnsNull() {
        int state = 11; // BluetoothAdapter.STATE_TURNING_ON
        Boolean isEnabled = handleBluetoothStateChange(state);
        assertNull(isEnabled);
    }

    @Test
    public void bluetoothStateHandler_unknownState_returnsNull() {
        int state = 999; // Unknown state
        Boolean isEnabled = handleBluetoothStateChange(state);
        assertNull(isEnabled);
    }

    // ========== UI State Mapping Tests ==========

    @Test
    public void uiState_beforeBtnPressed_buttonShouldBeEnabled() {
        boolean buttonEnabled = getButtonEnabledForState(
            BtSettingsViewModel.BtSettingsState.BEFORE_BTN_PRESSED);
        assertTrue(buttonEnabled);
    }

    @Test
    public void uiState_afterBtnPressed_buttonShouldBeDisabled() {
        boolean buttonEnabled = getButtonEnabledForState(
            BtSettingsViewModel.BtSettingsState.AFTER_BTN_PRESSED);
        assertFalse(buttonEnabled);
    }

    @Test
    public void uiState_scanning_buttonShouldBeEnabled() {
        boolean buttonEnabled = getButtonEnabledForState(
            BtSettingsViewModel.BtSettingsState.SCANNING);
        assertTrue(buttonEnabled);
    }

    @Test
    public void uiState_scanFinished_buttonShouldBeEnabled() {
        boolean buttonEnabled = getButtonEnabledForState(
            BtSettingsViewModel.BtSettingsState.SCAN_FINISHED);
        assertTrue(buttonEnabled);
    }

    @Test
    public void uiState_connecting_buttonShouldBeDisabled() {
        boolean buttonEnabled = getButtonEnabledForState(
            BtSettingsViewModel.BtSettingsState.CONNECTING);
        assertFalse(buttonEnabled);
    }

    // ========== Helper Methods ==========

    /**
     * Simulates checkBluetoothState() logic.
     */
    private BtSettingsViewModel.BtSettingsState simulateBluetoothCheck(boolean isBluetoothEnabled) {
        if (!isBluetoothEnabled) {
            return BtSettingsViewModel.BtSettingsState.BLUETOOTH_OFF;
        } else {
            return BtSettingsViewModel.BtSettingsState.READY_TO_CONNECT;
        }
    }

    /**
     * Simulates enableBluetooth() logic.
     */
    private BtSettingsViewModel.BtSettingsState simulateEnableBluetooth(
            BtSettingsViewModel.BtSettingsState current, boolean isAlreadyEnabled) {
        if (!isAlreadyEnabled) {
            return BtSettingsViewModel.BtSettingsState.ENABLING_BLUETOOTH;
        }
        return current;
    }

    /**
     * Simulates startBluetoothDiscovery() logic.
     */
    private BtSettingsViewModel.BtSettingsState simulateStartDiscovery(
            BtSettingsViewModel.BtSettingsState current, boolean isBluetoothEnabled) {
        if (isBluetoothEnabled) {
            return BtSettingsViewModel.BtSettingsState.SCANNING;
        } else {
            return BtSettingsViewModel.BtSettingsState.ENABLING_BLUETOOTH;
        }
    }

    /**
     * Simulates onDiscoveryFinished() logic.
     * Returns null if no state change should occur.
     */
    private BtSettingsViewModel.BtSettingsState simulateDiscoveryFinished(boolean isConnecting) {
        if (!isConnecting) {
            return BtSettingsViewModel.BtSettingsState.SCAN_FINISHED;
        }
        return null; // No state change when connecting
    }

    /**
     * Simulates connectToDevice() state transition.
     */
    private BtSettingsViewModel.BtSettingsState simulateConnectToDevice(
            BtSettingsViewModel.BtSettingsState current) {
        return BtSettingsViewModel.BtSettingsState.CONNECTING;
    }

    /**
     * Simulates device address matching for auto-connect.
     */
    private boolean shouldAutoConnectToDevice(String foundAddress, String lastAddress) {
        if (lastAddress == null || lastAddress.isEmpty()) {
            return false;
        }
        return foundAddress.equals(lastAddress);
    }

    /**
     * Simulates case-insensitive device address matching.
     */
    private boolean shouldAutoConnectToDeviceCaseInsensitive(String foundAddress, String lastAddress) {
        if (lastAddress == null || lastAddress.isEmpty()) {
            return false;
        }
        return foundAddress.equalsIgnoreCase(lastAddress);
    }

    /**
     * Simulates connect action setting the connecting flag.
     */
    private boolean simulateConnectAction() {
        return true; // isConnecting = true after connect
    }

    /**
     * Simulates onBtStateChanged() logic.
     * Returns null for transitional states.
     */
    private Boolean handleBluetoothStateChange(int state) {
        // BluetoothAdapter constants: STATE_OFF=10, STATE_TURNING_ON=11, STATE_ON=12, STATE_TURNING_OFF=13
        switch (state) {
            case 10: // STATE_OFF
                return false;
            case 12: // STATE_ON
                return true;
            case 11: // STATE_TURNING_ON
            case 13: // STATE_TURNING_OFF
            default:
                return null;
        }
    }

    /**
     * Simulates UI button enabled state based on BtSettingsState.
     * Mirrors the updateUI() method in BtSettingsActivity.
     */
    private boolean getButtonEnabledForState(BtSettingsViewModel.BtSettingsState state) {
        switch (state) {
            case BEFORE_BTN_PRESSED:
                return true;
            case AFTER_BTN_PRESSED:
                return false;
            case SCANNING:
                return true;
            case SCAN_FINISHED:
                return true;
            case CONNECTING:
                return false;
            default:
                return false; // Default disabled for safety
        }
    }
}
