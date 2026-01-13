# TASK 7: Person Name Management UI

**Assigned To:** UI Developer  
**Estimated Effort:** 2 days  
**Dependencies:** Task 3 (Person Name backend)  
**Status:** Pending

---

## Context

Aggregator users need UI to assign friendly names to sensor IDs. Example: Map `sensor001` to "Ion Popescu".

---

## Deliverables

### 1. Create `PersonNamesActivity.java` or integrate into Settings

```java
public class PersonNamesActivity extends BaseActivity {
    private ActivityPersonNamesBinding binding;
    private PersonNamesViewModel viewModel;
    private PersonNamesAdapter adapter;
    
    @Override
    protected void onBaseCreate(@Nullable Bundle savedInstanceState) {
        binding = ActivityPersonNamesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        viewModel = new ViewModelProvider(this).get(PersonNamesViewModel.class);
        
        setupToolbar();
        setupRecyclerView();
        observeData();
    }
    
    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.manage_person_names);
    }
    
    private void setupRecyclerView() {
        adapter = new PersonNamesAdapter(person -> {
            // Show edit dialog when row clicked
            showEditDialog(person);
        });
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
    }
    
    private void observeData() {
        viewModel.getAllPersons().observe(this, persons -> {
            adapter.submitList(persons);
            
            // Show empty state if no persons
            binding.emptyState.setVisibility(
                persons.isEmpty() ? View.VISIBLE : View.GONE
            );
        });
    }
    
    private void showEditDialog(MonitoredPerson person) {
        PersonNameEditDialog dialog = PersonNameEditDialog.newInstance(
            person.getSensorId(),
            person.getDisplayName()
        );
        dialog.setOnSaveListener((sensorId, newName) -> {
            viewModel.updateDisplayName(sensorId, newName);
        });
        dialog.show(getSupportFragmentManager(), "edit_name");
    }
}
```

### 2. Create `PersonNameEditDialog.java`

```java
public class PersonNameEditDialog extends DialogFragment {
    private static final String ARG_SENSOR_ID = "sensor_id";
    private static final String ARG_CURRENT_NAME = "current_name";
    
    private DialogPersonNameEditBinding binding;
    private OnSaveListener saveListener;
    
    public interface OnSaveListener {
        void onSave(String sensorId, String newName);
    }
    
    public static PersonNameEditDialog newInstance(String sensorId, String currentName) {
        PersonNameEditDialog dialog = new PersonNameEditDialog();
        Bundle args = new Bundle();
        args.putString(ARG_SENSOR_ID, sensorId);
        args.putString(ARG_CURRENT_NAME, currentName);
        dialog.setArguments(args);
        return dialog;
    }
    
    public void setOnSaveListener(OnSaveListener listener) {
        this.saveListener = listener;
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = DialogPersonNameEditBinding.inflate(getLayoutInflater());
        
        String sensorId = getArguments().getString(ARG_SENSOR_ID);
        String currentName = getArguments().getString(ARG_CURRENT_NAME);
        
        binding.sensorIdText.setText(sensorId);
        binding.nameInput.setText(currentName);
        binding.nameInput.setSelection(currentName.length()); // Cursor at end
        
        return new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.edit_person_name)
            .setView(binding.getRoot())
            .setPositiveButton(R.string.save, (dialog, which) -> {
                String newName = binding.nameInput.getText().toString().trim();
                if (!newName.isEmpty() && saveListener != null) {
                    saveListener.onSave(sensorId, newName);
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .create();
    }
}
```

### 3. Create `PersonNamesAdapter.java`

```java
public class PersonNamesAdapter extends ListAdapter<MonitoredPerson, PersonNamesAdapter.ViewHolder> {
    
    private final OnItemClickListener clickListener;
    
    public interface OnItemClickListener {
        void onClick(MonitoredPerson person);
    }
    
    public PersonNamesAdapter(OnItemClickListener listener) {
        super(new DiffCallback());
        this.clickListener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPersonNameBinding binding = ItemPersonNameBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), clickListener);
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemPersonNameBinding binding;
        
        ViewHolder(ItemPersonNameBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
        
        void bind(MonitoredPerson person, OnItemClickListener listener) {
            binding.displayNameText.setText(person.getDisplayName());
            binding.sensorIdText.setText(person.getSensorId());
            
            binding.editButton.setOnClickListener(v -> listener.onClick(person));
            binding.getRoot().setOnClickListener(v -> listener.onClick(person));
        }
    }
    
    static class DiffCallback extends DiffUtil.ItemCallback<MonitoredPerson> {
        @Override
        public boolean areItemsTheSame(@NonNull MonitoredPerson oldItem, @NonNull MonitoredPerson newItem) {
            return oldItem.getSensorId().equals(newItem.getSensorId());
        }
        
        @Override
        public boolean areContentsTheSame(@NonNull MonitoredPerson oldItem, @NonNull MonitoredPerson newItem) {
            return oldItem.getDisplayName().equals(newItem.getDisplayName());
        }
    }
}
```

### 4. Create `PersonNamesViewModel.java`

```java
public class PersonNamesViewModel extends AndroidViewModel {
    private final PersonNameManager personNameManager;
    private final LiveData<List<MonitoredPerson>> allPersons;
    
    public PersonNamesViewModel(@NonNull Application application) {
        super(application);
        personNameManager = PersonNameManager.getInstance(application);
        allPersons = personNameManager.getAllPersonsLive();
    }
    
    public LiveData<List<MonitoredPerson>> getAllPersons() {
        return allPersons;
    }
    
    public void updateDisplayName(String sensorId, String newName) {
        personNameManager.setDisplayName(sensorId, newName);
    }
}
```

### 5. Create layouts

**`activity_person_names.xml`**
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">
    
    <com.google.android.material.appbar.MaterialToolbar
        android:id="@+id/toolbar"
        android:layout_width="match_parent"
        android:layout_height="?attr/actionBarSize" />
    
    <FrameLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent">
        
        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/recyclerView"
            android:layout_width="match_parent"
            android:layout_height="match_parent" />
        
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
                android:layout_width="80dp"
                android:layout_height="80dp"
                android:src="@drawable/ic_people"
                android:tint="@android:color/darker_gray" />
            
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/no_monitored_persons"
                android:layout_marginTop="16dp" />
                
        </LinearLayout>
    </FrameLayout>
</LinearLayout>
```

**`item_person_name.xml`**
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:padding="16dp"
    android:gravity="center_vertical"
    android:background="?attr/selectableItemBackground">
    
    <LinearLayout
        android:layout_width="0dp"
        android:layout_weight="1"
        android:layout_height="wrap_content"
        android:orientation="vertical">
        
        <TextView
            android:id="@+id/displayNameText"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textSize="16sp"
            android:textStyle="bold" />
        
        <TextView
            android:id="@+id/sensorIdText"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textSize="12sp"
            android:textColor="@android:color/darker_gray" />
    </LinearLayout>
    
    <ImageButton
        android:id="@+id/editButton"
        android:layout_width="48dp"
        android:layout_height="48dp"
        android:src="@drawable/ic_edit"
        android:background="?attr/selectableItemBackgroundBorderless"
        android:contentDescription="@string/edit" />
        
</LinearLayout>
```

**`dialog_person_name_edit.xml`**
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="24dp">
    
    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/sensor_id_label"
        android:textSize="12sp" />
    
    <TextView
        android:id="@+id/sensorIdText"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textSize="14sp"
        android:fontFamily="monospace"
        android:layout_marginBottom="16dp" />
    
    <com.google.android.material.textfield.TextInputLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content">
        
        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/nameInput"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="@string/display_name_hint"
            android:inputType="textPersonName"
            android:maxLength="50" />
            
    </com.google.android.material.textfield.TextInputLayout>
</LinearLayout>
```

### 6. Add navigation entry

In `AggregatorDashboardActivity`, add menu or button:

```java
@Override
public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.aggregator_dashboard_menu, menu);
    return true;
}

@Override
public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    if (item.getItemId() == R.id.action_manage_names) {
        startActivity(new Intent(this, PersonNamesActivity.class));
        return true;
    }
    return super.onOptionsItemSelected(item);
}
```

---

## Files to Create

- `app/src/main/java/com/melisa/innovamotionapp/activities/PersonNamesActivity.java`
- `app/src/main/java/com/melisa/innovamotionapp/ui/dialogs/PersonNameEditDialog.java`
- `app/src/main/java/com/melisa/innovamotionapp/ui/adapters/PersonNamesAdapter.java`
- `app/src/main/java/com/melisa/innovamotionapp/ui/viewmodels/PersonNamesViewModel.java`
- `app/src/main/res/layout/activity_person_names.xml`
- `app/src/main/res/layout/item_person_name.xml`
- `app/src/main/res/layout/dialog_person_name_edit.xml`
- `app/src/main/res/menu/aggregator_dashboard_menu.xml`

---

## Acceptance Criteria

- [ ] List displays all known sensor IDs with their display names
- [ ] Tap or edit button opens dialog to change name
- [ ] Changes are saved immediately and reflected in list
- [ ] Empty state shown when no persons are registered
- [ ] Names are synced to cloud (via PersonNamesFirestoreSync from Task 3)
- [ ] Navigation accessible from Aggregator Dashboard
