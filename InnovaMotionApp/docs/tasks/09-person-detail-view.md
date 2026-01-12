# TASK 9: Individual Person Detail View (Supervisor)

**Assigned To:** UI Developer  
**Estimated Effort:** 1-2 days  
**Dependencies:** Task 8 (Dashboard), existing views  
**Status:** Pending

---

## Context

Reuse existing single-person view (`BtConnectedActivity`) for detailed monitoring of one selected person.

---

## Deliverables

### 1. Modify `BtConnectedActivity.java`

Add support for sensor-specific filtering:

```java
public class BtConnectedActivity extends AppCompatActivity {
    public static final String EXTRA_SENSOR_ID = "extra_sensor_id";
    public static final String EXTRA_PERSON_NAME = "extra_person_name";
    
    private String sensorId; // null = show all (legacy behavior)
    private String personName;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Get intent extras
        sensorId = getIntent().getStringExtra(EXTRA_SENSOR_ID);
        personName = getIntent().getStringExtra(EXTRA_PERSON_NAME);
        
        // Update title if person name provided
        if (personName != null && !personName.isEmpty()) {
            binding.descriptionTextView.setText(personName);
        }
        
        // Modify observer based on whether sensorId is provided
        if (sensorId != null && RoleProvider.isSupervisor()) {
            // Supervisor viewing specific person
            observeSensorSpecificData();
        } else {
            // Existing behavior
            observeGlobalData();
        }
    }
    
    private void observeSensorSpecificData() {
        ReceivedBtDataDao dao = InnovaDatabase.getInstance(this).receivedBtDataDao();
        dao.getLatestForSensor(sensorId).observe(this, entity -> {
            if (entity != null) {
                Posture posture = PostureFactory.createPosture(entity.getReceivedMsg());
                displayPostureData(posture);
            }
        });
    }
    
    private void observeGlobalData() {
        // Existing GlobalData observer code
        globalData.getReceivedPosture().observe(this, this::displayPostureData);
    }
}
```

### 2. OR Create `PersonDetailActivity.java` (if heavy modifications needed)

Only if modifying `BtConnectedActivity` becomes too complex:
- Clone existing layout and logic
- Add sensor-specific filtering from start
- Clean separation from BT-connected flow

### 3. Ensure existing features work per-person

Pass `sensorId` to other activities:

```java
// From detail view, navigate to statistics
public void LaunchStatistics(View view) {
    Intent intent = new Intent(this, StatisticsActivity.class);
    if (sensorId != null) {
        intent.putExtra(StatisticsActivity.EXTRA_SENSOR_ID, sensorId);
        intent.putExtra(StatisticsActivity.EXTRA_PERSON_NAME, personName);
    }
    startActivity(intent);
}
```

Update `StatisticsActivity` and `EnergyConsumptionActivity` to filter by sensorId when provided.

---

## Files to Modify or Reference

- [`BtConnectedActivity.java`](../../app/src/main/java/com/melisa/innovamotionapp/activities/BtConnectedActivity.java)
- [`StatisticsActivity.java`](../../app/src/main/java/com/melisa/innovamotionapp/activities/StatisticsActivity.java)
- [`EnergyConsumptionActivity.java`](../../app/src/main/java/com/melisa/innovamotionapp/activities/EnergyConsumptionActivity.java)
- [`TimeLapseActivity.java`](../../app/src/main/java/com/melisa/innovamotionapp/activities/TimeLapseActivity.java)

---

## Acceptance Criteria

- [ ] Detail view accepts `sensorId` and `personName` via intent extras
- [ ] Title shows person's display name
- [ ] Posture data filtered to specific sensor
- [ ] Statistics work per-person when sensorId provided
- [ ] Energy consumption works per-person when sensorId provided
- [ ] Back navigation returns to dashboard
- [ ] Legacy behavior preserved when no sensorId provided
