# TASK 6: Aggregator UI - Live Posture Viewer (Tab 2)

**Assigned To:** UI/Android Developer  
**Estimated Effort:** 2-3 days  
**Dependencies:** Task 5 (shares Activity), Task 3 (Person Names)  
**Status:** Pending

---

## Context

Select any monitored person from dropdown and see their current posture with animation. This is the second tab in the Aggregator Dashboard.

---

## Deliverables

### 1. Create `LivePostureFragment.java`

```java
public class LivePostureFragment extends Fragment {
    private FragmentLivePostureBinding binding;
    private LivePostureViewModel viewModel;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLivePostureBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        viewModel = new ViewModelProvider(this).get(LivePostureViewModel.class);
        
        setupPersonSelector();
        observePosture();
    }
    
    private void setupPersonSelector() {
        // Observe available persons and populate spinner
        viewModel.getAvailablePersons().observe(getViewLifecycleOwner(), persons -> {
            ArrayAdapter<MonitoredPerson> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                persons
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            binding.personSpinner.setAdapter(adapter);
        });
        
        // Handle selection changes
        binding.personSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                MonitoredPerson selected = (MonitoredPerson) parent.getItemAtPosition(position);
                viewModel.selectPerson(selected.getSensorId());
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                viewModel.selectPerson(null);
            }
        });
    }
    
    private void observePosture() {
        viewModel.getCurrentPosture().observe(getViewLifecycleOwner(), posture -> {
            if (posture != null) {
                displayPosture(posture);
            } else {
                showNoDataState();
            }
        });
        
        viewModel.getLastUpdateTime().observe(getViewLifecycleOwner(), timestamp -> {
            if (timestamp != null) {
                binding.lastUpdateText.setText(
                    getString(R.string.last_update, formatTime(timestamp))
                );
            }
        });
        
        viewModel.getSelectedPersonName().observe(getViewLifecycleOwner(), name -> {
            binding.personNameText.setText(name);
        });
    }
    
    /**
     * Display posture using existing logic from BtConnectedActivity
     */
    private void displayPosture(Posture posture) {
        // Show posture description
        String description = getString(posture.getTextCode(), 
            viewModel.getSelectedPersonName().getValue());
        binding.descriptionText.setText(description);
        
        // Show risk level
        binding.riskText.setText(getString(posture.getRisc()));
        
        // Play posture animation/video
        if (posture instanceof UnknownPosture) {
            binding.videoView.stopPlayback();
            binding.videoView.setVideoURI(null);
        } else {
            String videoPath = "android.resource://" + 
                requireContext().getPackageName() + "/" + posture.getVideoCode();
            binding.videoView.setVideoPath(videoPath);
            binding.videoView.start();
        }
    }
    
    private void showNoDataState() {
        binding.descriptionText.setText(R.string.no_data_available);
        binding.riskText.setText("-");
        binding.videoView.stopPlayback();
    }
}
```

### 2. Create `LivePostureViewModel.java`

```java
public class LivePostureViewModel extends AndroidViewModel {
    private final ReceivedBtDataDao dao;
    private final MonitoredPersonDao personDao;
    private final PersonNameManager personNameManager;
    
    private final MutableLiveData<String> selectedSensorId = new MutableLiveData<>();
    private final LiveData<Posture> currentPosture;
    private final LiveData<Long> lastUpdateTime;
    private final LiveData<String> selectedPersonName;
    private final LiveData<List<MonitoredPerson>> availablePersons;
    
    public LivePostureViewModel(@NonNull Application application) {
        super(application);
        
        InnovaDatabase db = InnovaDatabase.getInstance(application);
        dao = db.receivedBtDataDao();
        personDao = db.monitoredPersonDao();
        personNameManager = PersonNameManager.getInstance(application);
        
        // Available persons for dropdown
        availablePersons = personDao.getAllMonitoredPersons();
        
        // Current posture based on selected sensor
        LiveData<ReceivedBtDataEntity> latestReading = Transformations.switchMap(
            selectedSensorId, 
            sensorId -> {
                if (sensorId == null || sensorId.isEmpty()) {
                    return new MutableLiveData<>(null);
                }
                return dao.getLatestForSensor(sensorId);
            }
        );
        
        // Transform reading to Posture
        currentPosture = Transformations.map(latestReading, entity -> {
            if (entity == null) return null;
            return PostureFactory.createPosture(entity.getReceivedMsg());
        });
        
        // Extract timestamp
        lastUpdateTime = Transformations.map(latestReading, entity -> {
            if (entity == null) return null;
            return entity.getTimestamp();
        });
        
        // Selected person name
        selectedPersonName = Transformations.map(selectedSensorId, sensorId -> {
            if (sensorId == null) return "";
            return personNameManager.getDisplayName(sensorId);
        });
    }
    
    public void selectPerson(String sensorId) {
        selectedSensorId.setValue(sensorId);
    }
    
    public LiveData<List<MonitoredPerson>> getAvailablePersons() {
        return availablePersons;
    }
    
    public LiveData<Posture> getCurrentPosture() {
        return currentPosture;
    }
    
    public LiveData<Long> getLastUpdateTime() {
        return lastUpdateTime;
    }
    
    public LiveData<String> getSelectedPersonName() {
        return selectedPersonName;
    }
}
```

### 3. Create layout `fragment_live_posture.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">
    
    <!-- Person Selector -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical">
        
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/select_person"
            android:textStyle="bold" />
        
        <Spinner
            android:id="@+id/personSpinner"
            android:layout_width="0dp"
            android:layout_weight="1"
            android:layout_height="wrap_content"
            android:layout_marginStart="8dp" />
    </LinearLayout>
    
    <!-- Person Name (large display) -->
    <TextView
        android:id="@+id/personNameText"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textSize="24sp"
        android:textStyle="bold"
        android:gravity="center"
        android:layout_marginTop="16dp" />
    
    <!-- Posture Video/Animation -->
    <VideoView
        android:id="@+id/videoView"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:layout_marginTop="16dp" />
    
    <!-- Posture Description -->
    <TextView
        android:id="@+id/descriptionText"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textSize="18sp"
        android:gravity="center"
        android:layout_marginTop="16dp" />
    
    <!-- Risk Level -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center"
        android:layout_marginTop="8dp">
        
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/risk_level" />
        
        <TextView
            android:id="@+id/riskText"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginStart="8dp"
            android:textStyle="bold" />
    </LinearLayout>
    
    <!-- Last Update Time -->
    <TextView
        android:id="@+id/lastUpdateText"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textSize="12sp"
        android:gravity="center"
        android:textColor="@android:color/darker_gray"
        android:layout_marginTop="8dp" />
        
</LinearLayout>
```

---

## Reuse Existing Components

Reference these files for posture display logic:

- [`BtConnectedActivity.java`](../../app/src/main/java/com/melisa/innovamotionapp/activities/BtConnectedActivity.java) - `displayPostureData()` method
- [`PostureFactory.java`](../../app/src/main/java/com/melisa/innovamotionapp/data/posture/PostureFactory.java) - hex to Posture conversion
- Existing video resources in `res/raw/`

---

## Files to Create

- `app/src/main/java/com/melisa/innovamotionapp/ui/fragments/LivePostureFragment.java`
- `app/src/main/java/com/melisa/innovamotionapp/ui/viewmodels/LivePostureViewModel.java`
- `app/src/main/res/layout/fragment_live_posture.xml`

---

## Acceptance Criteria

- [ ] Person dropdown populated with all monitored persons
- [ ] Selecting a person shows their latest posture
- [ ] Posture animation/video plays correctly
- [ ] Description and risk level displayed
- [ ] Last update timestamp shown
- [ ] Updates in real-time as new data arrives
- [ ] Graceful handling when no data available
- [ ] Reuses existing posture display logic (DRY)
