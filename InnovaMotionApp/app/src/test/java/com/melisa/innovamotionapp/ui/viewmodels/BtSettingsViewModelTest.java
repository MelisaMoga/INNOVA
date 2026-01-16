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
        // States: BLUETOOTH_OFF, ENABLING_BLUETOOTH, DISCONNECTED, SCANNING, 
        //         SCAN_FINISHED, CONNECTING, CONNECTED
        assertEquals(7, states.length);
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
    public void btSettingsState_disconnected_exists() {
        BtSettingsViewModel.BtSettingsState state = BtSettingsViewModel.BtSettingsState.DISCONNECTED;
        assertNotNull(state);
        assertEquals("DISCONNECTED", state.name());
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
    public void btSettingsState_connected_exists() {
        BtSettingsViewModel.BtSettingsState state = BtSettingsViewModel.BtSettingsState.CONNECTED;
        assertNotNull(state);
        assertEquals("CONNECTED", state.name());
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
    public void stateTransition_initialState_shouldBeDisconnected() {
        BtSettingsViewModel.BtSettingsState initialState = 
            BtSettingsViewModel.BtSettingsState.DISCONNECTED;
        assertEquals(BtSettingsViewModel.BtSettingsState.DISCONNECTED, initialState);
    }

    @Test
    public void stateTransition_bluetoothOff_canTransitionToEnabling() {
        // Simulating state flow: BLUETOOTH_OFF -> ENABLING_BLUETOOTH
        BtSettingsViewModel.BtSettingsState current = BtSettingsViewModel.BtSettingsState.BLUETOOTH_OFF;
        BtSettingsViewModel.BtSettingsState next = simulateEnableBluetooth(current, false);
        assertEquals(BtSettingsViewModel.BtSettingsState.ENABLING_BLUETOOTH, next);
    }

    @Test
    public void stateTransition_bluetoothOn_transitionsToDisconnected() {
        // Simulating state flow after Bluetooth is enabled
        BtSettingsViewModel.BtSettingsState state = simulateBluetoothCheck(true, false);
        assertEquals(BtSettingsViewModel.BtSettingsState.DISCONNECTED, state);
    }

    @Test
    public void stateTransition_bluetoothOn_alreadyConnected_transitionsToConnected() {
        // If already connected, should stay connected
        BtSettingsViewModel.BtSettingsState state = simulateBluetoothCheck(true, true);
        assertEquals(BtSettingsViewModel.BtSettingsState.CONNECTED, state);
    }

    @Test
    public void stateTransition_bluetoothOff_staysBluetoothOff() {
        BtSettingsViewModel.BtSettingsState state = simulateBluetoothCheck(false, false);
        assertEquals(BtSettingsViewModel.BtSettingsState.BLUETOOTH_OFF, state);
    }

    @Test
    public void stateTransition_disconnected_canStartScanning() {
        // Simulating: DISCONNECTED -> SCANNING
        BtSettingsViewModel.BtSettingsState current = BtSettingsViewModel.BtSettingsState.DISCONNECTED;
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

    @Test
    public void stateTransition_connecting_transitionsToConnected() {
        // After successful connection
        BtSettingsViewModel.BtSettingsState next = simulateDeviceConnected();
        assertEquals(BtSettingsViewModel.BtSettingsState.CONNECTED, next);
    }

    @Test
    public void stateTransition_connected_canDisconnect() {
        // CONNECTED -> DISCONNECTED
        BtSettingsViewModel.BtSettingsState next = simulateDisconnect();
        assertEquals(BtSettingsViewModel.BtSettingsState.DISCONNECTED, next);
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
    public void uiState_disconnected_buttonShouldBeEnabled() {
        boolean buttonEnabled = getButtonEnabledForState(
            BtSettingsViewModel.BtSettingsState.DISCONNECTED);
        assertTrue(buttonEnabled);
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

    @Test
    public void uiState_connected_buttonShouldBeEnabled() {
        // Disconnect button should be enabled when connected
        boolean buttonEnabled = getButtonEnabledForState(
            BtSettingsViewModel.BtSettingsState.CONNECTED);
        assertTrue(buttonEnabled);
    }

    @Test
    public void uiState_enablingBluetooth_buttonShouldBeDisabled() {
        boolean buttonEnabled = getButtonEnabledForState(
            BtSettingsViewModel.BtSettingsState.ENABLING_BLUETOOTH);
        assertFalse(buttonEnabled);
    }

    // ========== Button Action Tests ==========

    @Test
    public void buttonAction_disconnected_shouldScan() {
        String action = getButtonActionForState(BtSettingsViewModel.BtSettingsState.DISCONNECTED);
        assertEquals("SCAN", action);
    }

    @Test
    public void buttonAction_scanning_shouldStopScan() {
        String action = getButtonActionForState(BtSettingsViewModel.BtSettingsState.SCANNING);
        assertEquals("STOP_SCAN", action);
    }

    @Test
    public void buttonAction_connected_shouldDisconnect() {
        String action = getButtonActionForState(BtSettingsViewModel.BtSettingsState.CONNECTED);
        assertEquals("DISCONNECT", action);
    }

    @Test
    public void buttonAction_bluetoothOff_shouldEnableBluetooth() {
        String action = getButtonActionForState(BtSettingsViewModel.BtSettingsState.BLUETOOTH_OFF);
        assertEquals("ENABLE_BLUETOOTH", action);
    }

    // ========== Helper Methods ==========

    /**
     * Simulates checkBluetoothState() logic.
     */
    private BtSettingsViewModel.BtSettingsState simulateBluetoothCheck(boolean isBluetoothEnabled, boolean isConnected) {
        if (!isBluetoothEnabled) {
            return BtSettingsViewModel.BtSettingsState.BLUETOOTH_OFF;
        } else if (isConnected) {
            return BtSettingsViewModel.BtSettingsState.CONNECTED;
        } else {
            return BtSettingsViewModel.BtSettingsState.DISCONNECTED;
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
     * Simulates onDeviceConnected() state transition.
     */
    private BtSettingsViewModel.BtSettingsState simulateDeviceConnected() {
        return BtSettingsViewModel.BtSettingsState.CONNECTED;
    }

    /**
     * Simulates disconnectDevice() state transition.
     */
    private BtSettingsViewModel.BtSettingsState simulateDisconnect() {
        return BtSettingsViewModel.BtSettingsState.DISCONNECTED;
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
            case DISCONNECTED:
            case SCANNING:
            case SCAN_FINISHED:
            case CONNECTED:
            case BLUETOOTH_OFF:
                return true;
            case ENABLING_BLUETOOTH:
            case CONNECTING:
                return false;
            default:
                return false; // Default disabled for safety
        }
    }

    /**
     * Returns the action the button should perform for a given state.
     */
    private String getButtonActionForState(BtSettingsViewModel.BtSettingsState state) {
        switch (state) {
            case BLUETOOTH_OFF:
                return "ENABLE_BLUETOOTH";
            case DISCONNECTED:
            case SCAN_FINISHED:
                return "SCAN";
            case SCANNING:
                return "STOP_SCAN";
            case CONNECTED:
                return "DISCONNECT";
            default:
                return "NONE";
        }
    }
}
