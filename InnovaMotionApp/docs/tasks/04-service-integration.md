# TASK 4: DeviceCommunicationService Multi-User Integration

**Assigned To:** Backend/Bluetooth Developer  
**Estimated Effort:** 2-3 days  
**Dependencies:** Task 1 (Parser), Task 2 (Data Model)  
**Status:** Pending

---

## Context

The service currently processes single readings. It needs to buffer lines, detect `END_PACKET`, and process batches.

---

## Deliverables

### 1. Modify `DeviceCommunicationService.java`

Replace single-reading `onDataReceived` handling with packet-based processing:

```java
public class DeviceCommunicationService extends Service {
    // ... existing fields ...
    
    private PacketParser packetParser;
    private PersonNameManager personNameManager;
    
    @Override
    public void onCreate() {
        super.onCreate();
        // ... existing init ...
        
        packetParser = new PacketParser();
        personNameManager = PersonNameManager.getInstance(this);
    }
    
    public void connectToDevice(BluetoothDevice device) {
        // ... existing connection setup ...
        
        deviceCommunicationThread = new DeviceCommunicationThread(device, new DeviceCommunicationThread.DataCallback() {
            
            @Override
            public void onDataReceived(BluetoothDevice device, String receivedData) {
                // Feed line to packet parser
                List<ParsedReading> completedPacket = packetParser.feedLine(receivedData);
                
                if (completedPacket != null && !completedPacket.isEmpty()) {
                    // Packet is complete, process it
                    processPacket(device, completedPacket);
                }
            }
            
            // ... other callbacks unchanged ...
        });
    }
    
    /**
     * Process a complete packet of readings.
     */
    private void processPacket(BluetoothDevice device, List<ParsedReading> readings) {
        Log.d(TAG, "[Service] Processing packet with " + readings.size() + " readings");
        
        final long now = System.currentTimeMillis();
        String ownerUid = null;
        if (userSession.isLoaded() && userSession.isSupervised()) {
            ownerUid = firestoreSyncService.getCurrentUserId();
        }
        
        List<ReceivedBtDataEntity> entities = new ArrayList<>();
        
        for (ParsedReading reading : readings) {
            // Ensure this sensor exists in person names database
            personNameManager.ensureSensorExists(reading.getSensorId());
            
            // Create entity with sensor ID
            ReceivedBtDataEntity entity = new ReceivedBtDataEntity(
                device.getAddress(),
                reading.getReceivedTimestamp(),
                reading.getHexCode(),
                ownerUid,
                reading.getSensorId()
            );
            entities.add(entity);
            
            // Convert to Posture for UI updates and fall detection
            Posture posture = PostureFactory.createPosture(reading.getHexCode());
            
            // Check for falls - include sensor ID in alert
            if (posture instanceof FallingPosture) {
                String personName = personNameManager.getDisplayName(reading.getSensorId());
                AlertNotifications.notifyFall(
                    DeviceCommunicationService.this,
                    personName,
                    getString(R.string.notif_fall_text_generic)
                );
            }
            
            // Update UI with most recent reading
            // Note: May want to track per-sensor for multi-user dashboard
            GlobalData.getInstance().setReceivedPosture(posture);
        }
        
        // Batch insert all entities
        synchronized (lock) {
            temporaryReceivedBtDataListToSave.addAll(entities);
        }
        
        // Log file (optional - consider if still needed)
        try {
            for (ParsedReading reading : readings) {
                fileOutputStream.write((reading.getSensorId() + ";" + reading.getHexCode() + "\n").getBytes());
            }
        } catch (IOException e) {
            Log.e(TAG, "ERROR writing posture file", e);
        }
    }
}
```

### 2. Update `DeviceCommunicationThread.DataCallback`

Option A: Keep existing interface, parser handles buffering in Service.

Option B: Add new callback method:

```java
public interface DataCallback {
    void onConnectionEstablished(BluetoothDevice device);
    void onDataReceived(BluetoothDevice device, String data);
    
    // NEW: Called when a complete packet is received
    default void onPacketReceived(BluetoothDevice device, List<ParsedReading> readings) {
        // Default implementation for backward compatibility
    }
    
    void onConnectionDisconnected();
}
```

### 3. Update fall notification logic

Modify `AlertNotifications.notifyFall()` to include person name:

```java
public static void notifyFall(Context context, String personName, String messageBody) {
    String title = context.getString(R.string.notif_fall_title, personName);
    // ... existing notification code using personName in title/body ...
}
```

Update string resources:
```xml
<string name="notif_fall_title">Fall Detected: %1$s</string>
<string name="notif_fall_text_generic">may have fallen. Check immediately.</string>
```

---

## Files to Modify

- [`DeviceCommunicationService.java`](../../app/src/main/java/com/melisa/innovamotionapp/bluetooth/DeviceCommunicationService.java)
- [`DeviceCommunicationThread.java`](../../app/src/main/java/com/melisa/innovamotionapp/bluetooth/DeviceCommunicationThread.java) (if adding new callback)
- [`AlertNotifications.java`](../../app/src/main/java/com/melisa/innovamotionapp/utils/AlertNotifications.java)
- String resources (`strings.xml`)

---

## Integration Points

- Uses `PacketParser` from Task 1
- Uses updated `ReceivedBtDataEntity` from Task 2
- Uses `PersonNameManager` from Task 3

---

## Acceptance Criteria

- [ ] Packets are correctly buffered until `END_PACKET`
- [ ] Each reading in packet creates a separate database entity with sensorId
- [ ] Fall notifications include person name/sensor ID
- [ ] Multiple readings per packet are batch-inserted efficiently
- [ ] New sensor IDs are auto-registered in person names table
- [ ] Legacy single-line format still works (backward compatibility - optional)
- [ ] No blocking on main thread
