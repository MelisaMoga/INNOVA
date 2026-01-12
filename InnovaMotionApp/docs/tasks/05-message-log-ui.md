# TASK 5: Aggregator UI - Live Message Monitor (Tab 1)

**Assigned To:** UI/Android Developer  
**Estimated Effort:** 3-4 days  
**Dependencies:** Task 2 (Data Model), Task 3 (Person Names)  
**Status:** Pending

---

## Context

New debugging interface for aggregator phones showing real-time message log with per-person statistics.

---

## Deliverables

### 1. Create `AggregatorDashboardActivity.java` in `activities/`

```java
public class AggregatorDashboardActivity extends BaseActivity {
    private ActivityAggregatorDashboardBinding binding;
    private AggregatorDashboardPagerAdapter pagerAdapter;
    
    @Override
    protected void onBaseCreate(@Nullable Bundle savedInstanceState) {
        binding = ActivityAggregatorDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        setupViewPager();
        setupTabLayout();
    }
    
    private void setupViewPager() {
        pagerAdapter = new AggregatorDashboardPagerAdapter(this);
        binding.viewPager.setAdapter(pagerAdapter);
    }
    
    private void setupTabLayout() {
        new TabLayoutMediator(binding.tabLayout, binding.viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText(R.string.tab_message_log);
                    tab.setIcon(R.drawable.ic_list);
                    break;
                case 1:
                    tab.setText(R.string.tab_live_posture);
                    tab.setIcon(R.drawable.ic_person);
                    break;
            }
        }).attach();
    }
}

// Pager Adapter
public class AggregatorDashboardPagerAdapter extends FragmentStateAdapter {
    public AggregatorDashboardPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }
    
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new MessageLogFragment();
            case 1: return new LivePostureFragment();
            default: throw new IllegalArgumentException("Invalid position: " + position);
        }
    }
    
    @Override
    public int getItemCount() {
        return 2;
    }
}
```

### 2. Create `MessageLogFragment.java` in new `ui/fragments/` package

```java
public class MessageLogFragment extends Fragment {
    private FragmentMessageLogBinding binding;
    private MessageLogViewModel viewModel;
    private MessageLogAdapter adapter;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMessageLogBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        viewModel = new ViewModelProvider(this).get(MessageLogViewModel.class);
        
        setupRecyclerView();
        setupFilters();
        observeData();
    }
    
    private void setupRecyclerView() {
        adapter = new MessageLogAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(adapter);
        
        // Auto-scroll to bottom for new messages
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                if (binding.autoScrollSwitch.isChecked()) {
                    binding.recyclerView.smoothScrollToPosition(adapter.getItemCount() - 1);
                }
            }
        });
    }
    
    private void setupFilters() {
        // Sensor filter spinner
        viewModel.getAvailableSensors().observe(getViewLifecycleOwner(), sensors -> {
            // Populate filter spinner
        });
        
        binding.filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedSensor = (String) parent.getItemAtPosition(position);
                viewModel.setFilterSensor(selectedSensor);
            }
            // ...
        });
    }
    
    private void observeData() {
        viewModel.getMessages().observe(getViewLifecycleOwner(), messages -> {
            adapter.submitList(messages);
        });
        
        viewModel.getMessageCountsPerSensor().observe(getViewLifecycleOwner(), counts -> {
            // Update summary header
            updateSummaryHeader(counts);
        });
    }
}
```

### 3. Create `MessageLogAdapter.java` in `ui/adapters/`

```java
public class MessageLogAdapter extends ListAdapter<MessageLogItem, MessageLogAdapter.ViewHolder> {
    
    public MessageLogAdapter() {
        super(new DiffCallback());
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMessageLogBinding binding = ItemMessageLogBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemMessageLogBinding binding;
        
        ViewHolder(ItemMessageLogBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
        
        void bind(MessageLogItem item) {
            binding.timestampText.setText(formatTimestamp(item.getTimestamp()));
            binding.sensorNameText.setText(item.getDisplayName());
            binding.hexCodeText.setText(item.getHexCode());
            binding.postureIcon.setImageResource(item.getPostureIconRes());
            
            // Highlight falls in red
            if (item.isFall()) {
                binding.cardView.setCardBackgroundColor(
                    ContextCompat.getColor(itemView.getContext(), R.color.fall_alert_background));
            } else {
                binding.cardView.setCardBackgroundColor(
                    ContextCompat.getColor(itemView.getContext(), R.color.card_background));
            }
        }
    }
    
    static class DiffCallback extends DiffUtil.ItemCallback<MessageLogItem> {
        @Override
        public boolean areItemsTheSame(@NonNull MessageLogItem oldItem, @NonNull MessageLogItem newItem) {
            return oldItem.getId() == newItem.getId();
        }
        
        @Override
        public boolean areContentsTheSame(@NonNull MessageLogItem oldItem, @NonNull MessageLogItem newItem) {
            return oldItem.equals(newItem);
        }
    }
}
```

### 4. Create `MessageLogViewModel.java` in `ui/viewmodels/`

```java
public class MessageLogViewModel extends AndroidViewModel {
    private final ReceivedBtDataDao dao;
    private final PersonNameManager personNameManager;
    private final MutableLiveData<String> filterSensor = new MutableLiveData<>(null);
    
    // Transformed data
    private final LiveData<List<MessageLogItem>> messages;
    private final LiveData<List<String>> availableSensors;
    private final LiveData<Map<String, Integer>> messageCountsPerSensor;
    
    public MessageLogViewModel(@NonNull Application application) {
        super(application);
        dao = InnovaDatabase.getInstance(application).receivedBtDataDao();
        personNameManager = PersonNameManager.getInstance(application);
        
        // Get recent messages (limit to last 500)
        LiveData<List<ReceivedBtDataEntity>> rawMessages = dao.getRecentMessages(500);
        
        // Transform to UI model with person names
        messages = Transformations.switchMap(filterSensor, sensor -> {
            LiveData<List<ReceivedBtDataEntity>> filtered = 
                (sensor == null || sensor.isEmpty()) 
                    ? rawMessages 
                    : dao.getMessagesForSensor(sensor, 500);
            
            return Transformations.map(filtered, entities -> {
                List<MessageLogItem> items = new ArrayList<>();
                for (ReceivedBtDataEntity entity : entities) {
                    items.add(new MessageLogItem(
                        entity.getId(),
                        entity.getTimestamp(),
                        entity.getSensorId(),
                        personNameManager.getDisplayName(entity.getSensorId()),
                        entity.getReceivedMsg(),
                        getPostureIcon(entity.getReceivedMsg()),
                        isFallPosture(entity.getReceivedMsg())
                    ));
                }
                return items;
            });
        });
        
        availableSensors = dao.getDistinctSensorIds();
        
        // Message counts per sensor
        messageCountsPerSensor = Transformations.map(rawMessages, entities -> {
            Map<String, Integer> counts = new HashMap<>();
            for (ReceivedBtDataEntity entity : entities) {
                String sensor = entity.getSensorId();
                counts.put(sensor, counts.getOrDefault(sensor, 0) + 1);
            }
            return counts;
        });
    }
    
    public void setFilterSensor(String sensor) {
        filterSensor.setValue(sensor);
    }
    
    // Getters for LiveData...
}
```

### 5. Create layouts

**`activity_aggregator_dashboard.xml`**
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">
    
    <com.google.android.material.tabs.TabLayout
        android:id="@+id/tabLayout"
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />
    
    <androidx.viewpager2.widget.ViewPager2
        android:id="@+id/viewPager"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1" />
        
</LinearLayout>
```

**`fragment_message_log.xml`**
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">
    
    <!-- Summary Header -->
    <LinearLayout
        android:id="@+id/summaryHeader"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:padding="8dp"
        android:orientation="horizontal">
        <!-- Message counts per person will go here -->
    </LinearLayout>
    
    <!-- Filter Controls -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:padding="8dp"
        android:orientation="horizontal">
        
        <Spinner
            android:id="@+id/filterSpinner"
            android:layout_width="0dp"
            android:layout_weight="1"
            android:layout_height="wrap_content" />
        
        <com.google.android.material.switchmaterial.SwitchMaterial
            android:id="@+id/autoScrollSwitch"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Auto-scroll"
            android:checked="true" />
    </LinearLayout>
    
    <!-- Message List -->
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/recyclerView"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1" />
        
</LinearLayout>
```

**`item_message_log.xml`**
```xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.card.MaterialCardView
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/cardView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="4dp">
    
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:padding="8dp">
        
        <ImageView
            android:id="@+id/postureIcon"
            android:layout_width="40dp"
            android:layout_height="40dp" />
        
        <LinearLayout
            android:layout_width="0dp"
            android:layout_weight="1"
            android:layout_height="wrap_content"
            android:layout_marginStart="8dp"
            android:orientation="vertical">
            
            <TextView
                android:id="@+id/sensorNameText"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textStyle="bold" />
            
            <TextView
                android:id="@+id/hexCodeText"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textSize="12sp" />
        </LinearLayout>
        
        <TextView
            android:id="@+id/timestampText"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textSize="10sp" />
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

---

## UI Specification

- Show last 100-500 messages (configurable via Constants)
- Auto-scroll to bottom for new messages (optional toggle)
- Tap message to see full details (optional - could show dialog)
- Fall messages highlighted with red background or icon
- Filter by sensor/person in dropdown
- Summary header shows message count per person

---

## Files to Create

- `app/src/main/java/com/melisa/innovamotionapp/activities/AggregatorDashboardActivity.java`
- `app/src/main/java/com/melisa/innovamotionapp/ui/fragments/MessageLogFragment.java`
- `app/src/main/java/com/melisa/innovamotionapp/ui/adapters/MessageLogAdapter.java`
- `app/src/main/java/com/melisa/innovamotionapp/ui/viewmodels/MessageLogViewModel.java`
- `app/src/main/java/com/melisa/innovamotionapp/ui/models/MessageLogItem.java`
- `app/src/main/res/layout/activity_aggregator_dashboard.xml`
- `app/src/main/res/layout/fragment_message_log.xml`
- `app/src/main/res/layout/item_message_log.xml`

---

## Acceptance Criteria

- [ ] TabLayout with ViewPager2 works correctly
- [ ] Messages display in real-time as they arrive
- [ ] Each message shows timestamp, person name, hex code, posture icon
- [ ] Fall messages are visually highlighted (red)
- [ ] Message count per person displayed in summary
- [ ] Filter dropdown works to show only one person's messages
- [ ] Auto-scroll toggle works
- [ ] Smooth performance with 500+ messages (DiffUtil)
