# TASK 8: Supervisor Multi-Person Dashboard

**Assigned To:** UI Developer  
**Estimated Effort:** 3-4 days  
**Dependencies:** Task 2 (Data Model), Task 3 (Person Names sync)  
**Status:** Pending

---

## Context

Supervisors currently see one person. New dashboard shows ALL monitored persons at once with real-time status.

---

## Deliverables

### 1. Create `SupervisorDashboardActivity.java` in `activities/`

```java
public class SupervisorDashboardActivity extends BaseActivity {
    private ActivitySupervisorDashboardBinding binding;
    private SupervisorDashboardViewModel viewModel;
    private PersonCardAdapter adapter;
    
    @Override
    protected void onBaseCreate(@Nullable Bundle savedInstanceState) {
        binding = ActivitySupervisorDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        viewModel = new ViewModelProvider(this).get(SupervisorDashboardViewModel.class);
        
        setupToolbar();
        setupRecyclerView();
        setupSwipeRefresh();
        observeData();
    }
    
    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setTitle(R.string.supervisor_dashboard);
    }
    
    private void setupRecyclerView() {
        adapter = new PersonCardAdapter(personStatus -> {
            // Navigate to detail view
            Intent intent = new Intent(this, BtConnectedActivity.class);
            intent.putExtra(BtConnectedActivity.EXTRA_SENSOR_ID, personStatus.getSensorId());
            intent.putExtra(BtConnectedActivity.EXTRA_PERSON_NAME, personStatus.getDisplayName());
            startActivity(intent);
        });
        
        // Grid layout for cards (2 columns on phones, 3 on tablets)
        int spanCount = getResources().getInteger(R.integer.dashboard_span_count);
        binding.recyclerView.setLayoutManager(new GridLayoutManager(this, spanCount));
        binding.recyclerView.setAdapter(adapter);
    }
    
    private void setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener(() -> {
            viewModel.refreshData();
        });
    }
    
    private void observeData() {
        viewModel.getPersonStatuses().observe(this, statuses -> {
            adapter.submitList(statuses);
            binding.swipeRefresh.setRefreshing(false);
            
            // Show empty state
            binding.emptyState.setVisibility(
                statuses.isEmpty() ? View.VISIBLE : View.GONE
            );
        });
        
        viewModel.getIsLoading().observe(this, isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });
    }
}
```

### 2. Create `PersonCardAdapter.java` in `ui/adapters/`

```java
public class PersonCardAdapter extends ListAdapter<PersonStatus, PersonCardAdapter.ViewHolder> {
    
    private static final long STALE_THRESHOLD_MS = 5 * 60 * 1000; // 5 minutes
    
    private final OnPersonClickListener clickListener;
    
    public interface OnPersonClickListener {
        void onClick(PersonStatus person);
    }
    
    public PersonCardAdapter(OnPersonClickListener listener) {
        super(new DiffCallback());
        this.clickListener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPersonCardBinding binding = ItemPersonCardBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), clickListener);
    }
    
    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemPersonCardBinding binding;
        
        ViewHolder(ItemPersonCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
        
        void bind(PersonStatus person, OnPersonClickListener listener) {
            Context ctx = itemView.getContext();
            
            // Person name
            binding.personNameText.setText(person.getDisplayName());
            
            // Posture icon
            binding.postureIcon.setImageResource(person.getPostureIconRes());
            
            // Last update time
            String timeAgo = formatTimeAgo(person.getLastUpdateTime());
            binding.lastUpdateText.setText(timeAgo);
            
            // Status indicator color
            int statusColor;
            if (person.isAlert()) {
                // Red for falls
                statusColor = ContextCompat.getColor(ctx, R.color.status_alert);
                binding.alertBadge.setVisibility(View.VISIBLE);
            } else if (isStale(person.getLastUpdateTime())) {
                // Yellow for stale data
                statusColor = ContextCompat.getColor(ctx, R.color.status_stale);
                binding.alertBadge.setVisibility(View.GONE);
            } else {
                // Green for active
                statusColor = ContextCompat.getColor(ctx, R.color.status_active);
                binding.alertBadge.setVisibility(View.GONE);
            }
            binding.statusIndicator.setBackgroundColor(statusColor);
            
            // Click handler
            binding.cardView.setOnClickListener(v -> listener.onClick(person));
        }
        
        private boolean isStale(long timestamp) {
            return System.currentTimeMillis() - timestamp > STALE_THRESHOLD_MS;
        }
        
        private String formatTimeAgo(long timestamp) {
            long diff = System.currentTimeMillis() - timestamp;
            if (diff < 60_000) return "Just now";
            if (diff < 3600_000) return (diff / 60_000) + "m ago";
            if (diff < 86400_000) return (diff / 3600_000) + "h ago";
            return (diff / 86400_000) + "d ago";
        }
    }
    
    static class DiffCallback extends DiffUtil.ItemCallback<PersonStatus> {
        @Override
        public boolean areItemsTheSame(@NonNull PersonStatus oldItem, @NonNull PersonStatus newItem) {
            return oldItem.getSensorId().equals(newItem.getSensorId());
        }
        
        @Override
        public boolean areContentsTheSame(@NonNull PersonStatus oldItem, @NonNull PersonStatus newItem) {
            return oldItem.equals(newItem);
        }
    }
}
```

### 3. Create `PersonStatus.java` model

```java
public class PersonStatus {
    private final String sensorId;
    private final String displayName;
    private final Posture currentPosture;
    private final long lastUpdateTime;
    private final boolean isAlert;
    
    // Constructor, getters, equals, hashCode
    
    public int getPostureIconRes() {
        return currentPosture.getPictureCode();
    }
}
```

### 4. Create `SupervisorDashboardViewModel.java`

```java
public class SupervisorDashboardViewModel extends AndroidViewModel {
    private final ReceivedBtDataDao dao;
    private final PersonNameManager personNameManager;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    
    private final LiveData<List<PersonStatus>> personStatuses;
    
    public SupervisorDashboardViewModel(@NonNull Application application) {
        super(application);
        
        dao = InnovaDatabase.getInstance(application).receivedBtDataDao();
        personNameManager = PersonNameManager.getInstance(application);
        
        // Get latest reading for each sensor
        LiveData<List<ReceivedBtDataEntity>> latestPerSensor = dao.getLatestForEachSensor();
        
        // Transform to PersonStatus with display names
        personStatuses = Transformations.map(latestPerSensor, entities -> {
            List<PersonStatus> statuses = new ArrayList<>();
            for (ReceivedBtDataEntity entity : entities) {
                String sensorId = entity.getSensorId();
                if (sensorId == null) continue;
                
                String displayName = personNameManager.getDisplayName(sensorId);
                Posture posture = PostureFactory.createPosture(entity.getReceivedMsg());
                boolean isAlert = posture instanceof FallingPosture;
                
                statuses.add(new PersonStatus(
                    sensorId,
                    displayName,
                    posture,
                    entity.getTimestamp(),
                    isAlert
                ));
            }
            
            // Sort: alerts first, then by name
            Collections.sort(statuses, (a, b) -> {
                if (a.isAlert() != b.isAlert()) {
                    return a.isAlert() ? -1 : 1;
                }
                return a.getDisplayName().compareToIgnoreCase(b.getDisplayName());
            });
            
            return statuses;
        });
    }
    
    public LiveData<List<PersonStatus>> getPersonStatuses() {
        return personStatuses;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public void refreshData() {
        // Trigger Firestore sync refresh
        isLoading.setValue(true);
        // ... sync logic ...
        isLoading.setValue(false);
    }
}
```

### 5. Create layouts

**`activity_supervisor_dashboard.xml`**
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">
    
    <com.google.android.material.appbar.AppBarLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content">
        
        <com.google.android.material.appbar.MaterialToolbar
            android:id="@+id/toolbar"
            android:layout_width="match_parent"
            android:layout_height="?attr/actionBarSize" />
    </com.google.android.material.appbar.AppBarLayout>
    
    <FrameLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:layout_behavior="@string/appbar_scrolling_view_behavior">
        
        <androidx.swiperefreshlayout.widget.SwipeRefreshLayout
            android:id="@+id/swipeRefresh"
            android:layout_width="match_parent"
            android:layout_height="match_parent">
            
            <androidx.recyclerview.widget.RecyclerView
                android:id="@+id/recyclerView"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:padding="8dp"
                android:clipToPadding="false" />
        </androidx.swiperefreshlayout.widget.SwipeRefreshLayout>
        
        <!-- Empty State -->
        <LinearLayout
            android:id="@+id/emptyState"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="center"
            android:orientation="vertical"
            android:gravity="center"
            android:visibility="gone">
            
            <ImageView
                android:layout_width="100dp"
                android:layout_height="100dp"
                android:src="@drawable/ic_people_outline" />
            
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/no_monitored_persons_supervisor"
                android:layout_marginTop="16dp" />
        </LinearLayout>
        
        <ProgressBar
            android:id="@+id/progressBar"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="center"
            android:visibility="gone" />
    </FrameLayout>
</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

**`item_person_card.xml`**
```xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.card.MaterialCardView
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/cardView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="4dp"
    app:cardElevation="4dp">
    
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical">
        
        <!-- Status indicator bar at top -->
        <View
            android:id="@+id/statusIndicator"
            android:layout_width="match_parent"
            android:layout_height="4dp" />
        
        <FrameLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:padding="12dp">
            
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:gravity="center">
                
                <!-- Posture Icon -->
                <ImageView
                    android:id="@+id/postureIcon"
                    android:layout_width="60dp"
                    android:layout_height="60dp" />
                
                <!-- Person Name -->
                <TextView
                    android:id="@+id/personNameText"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:textSize="14sp"
                    android:textStyle="bold"
                    android:gravity="center"
                    android:maxLines="2"
                    android:ellipsize="end"
                    android:layout_marginTop="8dp" />
                
                <!-- Last Update -->
                <TextView
                    android:id="@+id/lastUpdateText"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:textSize="11sp"
                    android:textColor="@android:color/darker_gray"
                    android:layout_marginTop="4dp" />
            </LinearLayout>
            
            <!-- Alert Badge (for falls) -->
            <ImageView
                android:id="@+id/alertBadge"
                android:layout_width="24dp"
                android:layout_height="24dp"
                android:layout_gravity="top|end"
                android:src="@drawable/ic_warning"
                android:visibility="gone" />
        </FrameLayout>
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

### 6. Update navigation in `MainActivity.java`

```java
public void LaunchMonitoring(View view) {
    SessionGate.getInstance(this).waitForSessionReady(new SessionGate.SessionReadyCallback() {
        @Override
        public void onSessionReady(String userId, String role, List<String> supervisedUserIds) {
            runOnUiThread(() -> {
                if ("supervisor".equals(role)) {
                    // NEW: Route to multi-person dashboard
                    Intent intent = new Intent(MainActivity.this, SupervisorDashboardActivity.class);
                    startActivity(intent);
                } else {
                    // Aggregator: go to aggregator dashboard
                    Intent intent = new Intent(MainActivity.this, AggregatorDashboardActivity.class);
                    startActivity(intent);
                }
            });
        }
        // ...
    });
}
```

---

## Files to Create

- `app/src/main/java/com/melisa/innovamotionapp/activities/SupervisorDashboardActivity.java`
- `app/src/main/java/com/melisa/innovamotionapp/ui/adapters/PersonCardAdapter.java`
- `app/src/main/java/com/melisa/innovamotionapp/ui/models/PersonStatus.java`
- `app/src/main/java/com/melisa/innovamotionapp/ui/viewmodels/SupervisorDashboardViewModel.java`
- `app/src/main/res/layout/activity_supervisor_dashboard.xml`
- `app/src/main/res/layout/item_person_card.xml`
- `app/src/main/res/values/integers.xml` (for dashboard_span_count)

## Files to Modify

- [`MainActivity.java`](../../app/src/main/java/com/melisa/innovamotionapp/activities/MainActivity.java) - update `LaunchMonitoring()` routing

---

## Acceptance Criteria

- [ ] Grid of cards showing all monitored persons
- [ ] Each card shows: name, posture icon, last update time
- [ ] Status colors: green (active), yellow (stale > 5min), red (alert/fall)
- [ ] Alert badge visible for fall postures
- [ ] Tap card navigates to detail view (Task 9)
- [ ] Swipe-to-refresh triggers data sync
- [ ] Empty state when no persons
- [ ] Real-time updates as new data arrives
- [ ] Supervisors routed here from MainActivity
