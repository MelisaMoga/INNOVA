# TASK 1: Multi-User Packet Parser

**Assigned To:** Backend/Bluetooth Developer  
**Estimated Effort:** 2-3 days  
**Dependencies:** None (foundational task)  
**Status:** Pending

---

## Context

The current protocol receives single hex codes like `0xAB3311\n`. The new protocol sends packets with multiple sensor readings:

```
sensor001;0xAB3311\n
sensor002;0xEF0112\n
END_PACKET\n
```

---

## Deliverables

### 1. Create `PacketParser.java` in `bluetooth/` package

- Parse incoming lines and buffer them until `END_PACKET\n` is received
- Extract `sensorId` and `hexCode` from each line using semicolon delimiter
- Return a list of `ParsedReading` objects when packet is complete
- Handle malformed lines gracefully (log and skip)

**Suggested implementation:**

```java
public class PacketParser {
    private final List<ParsedReading> buffer = new ArrayList<>();
    
    /**
     * Feed a line to the parser.
     * @return List of readings if packet is complete, null otherwise
     */
    public List<ParsedReading> feedLine(String line) {
        if ("END_PACKET".equals(line.trim())) {
            List<ParsedReading> result = new ArrayList<>(buffer);
            buffer.clear();
            return result;
        }
        
        ParsedReading reading = parseLine(line);
        if (reading != null) {
            buffer.add(reading);
        }
        return null;
    }
    
    private ParsedReading parseLine(String line) {
        // Parse "sensorId;hexCode" format
        // Return null and log if malformed
    }
}
```

### 2. Create `ParsedReading.java` data class in `bluetooth/` or `data/` package

Fields:
- `String sensorId` - The sensor/person identifier from hardware
- `String hexCode` - The posture hex code (e.g., "0xAB3311")
- `long receivedTimestamp` - When this reading was received

```java
public class ParsedReading {
    private final String sensorId;
    private final String hexCode;
    private final long receivedTimestamp;
    
    // Constructor, getters
}
```

### 3. Create unit tests for edge cases

Test scenarios:
- Empty packets (just `END_PACKET\n`)
- Malformed lines (no semicolon, empty sensor ID, empty hex code)
- Large packets (100+ readings)
- Mixed valid/invalid lines (valid lines should be kept, invalid skipped)
- Multiple consecutive packets
- Packet without terminator (buffer grows indefinitely - consider max size limit)

---

## Integration Points

- Will be called from `DeviceCommunicationThread.startReceiving()` instead of direct callback
- Existing callback `onDataReceived()` signature may need extension or new callback interface:

```java
public interface DataCallback {
    void onConnectionEstablished(BluetoothDevice device);
    void onDataReceived(BluetoothDevice device, String data); // Legacy single-line
    void onPacketReceived(BluetoothDevice device, List<ParsedReading> readings); // NEW
    void onConnectionDisconnected();
}
```

---

## Files to Review

- [`DeviceCommunicationThread.java`](../../app/src/main/java/com/melisa/innovamotionapp/bluetooth/DeviceCommunicationThread.java) - current line-by-line reading logic

---

## Acceptance Criteria

- [ ] `PacketParser` correctly buffers lines until `END_PACKET`
- [ ] Semicolon delimiter parsing works for various sensor ID formats (UUIDs, simple IDs)
- [ ] Malformed lines are logged and skipped without crashing
- [ ] Unit tests cover all edge cases
- [ ] No memory leaks (buffer is cleared after packet completion)
