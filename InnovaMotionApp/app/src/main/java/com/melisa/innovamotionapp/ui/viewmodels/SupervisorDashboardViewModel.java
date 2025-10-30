package com.melisa.innovamotionapp.ui.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Transformations;

import com.melisa.innovamotionapp.data.database.InnovaDatabase;
import com.melisa.innovamotionapp.data.database.ReceivedBtDataEntity;
import com.melisa.innovamotionapp.data.models.ChildPostureData;
import com.melisa.innovamotionapp.data.models.ChildProfile;
import com.melisa.innovamotionapp.data.posture.Posture;
import com.melisa.innovamotionapp.data.posture.PostureFactory;
import com.melisa.innovamotionapp.sync.ChildRegistryManager;
import com.melisa.innovamotionapp.sync.UserSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel for Supervisor Dashboard
 * 
 * Queries all children's latest posture data and combines with child registry
 * to create ChildPostureData objects for display.
 */
public class SupervisorDashboardViewModel extends AndroidViewModel {
    private static final String TAG = "SupervisorDashboardVM";
    
    private final InnovaDatabase database;
    private final UserSession userSession;
    private final ChildRegistryManager childRegistry;
    private final ExecutorService executorService;
    
    private final MediatorLiveData<List<ChildPostureData>> childrenData = new MediatorLiveData<>();
    private LiveData<List<ReceivedBtDataEntity>> latestMessagesLiveData;
    
    public SupervisorDashboardViewModel(@NonNull Application application) {
        super(application);
        
        this.database = InnovaDatabase.getInstance(application);
        this.userSession = UserSession.getInstance(application);
        this.childRegistry = userSession.getChildRegistry();
        this.executorService = Executors.newSingleThreadExecutor();
        
        Log.i(TAG, "SupervisorDashboardViewModel initialized");
        
        // Setup data observation
        setupDataObservation();
    }
    
    /**
     * Setup observation of latest messages from database
     */
    private void setupDataObservation() {
        // Get list of supervised user IDs (children from linked aggregator)
        List<String> supervisedUserIds = userSession.getSupervisedUserIds();
        
        if (supervisedUserIds.isEmpty()) {
            Log.w(TAG, "No supervised users found");
            childrenData.setValue(new ArrayList<>());
            return;
        }
        
        Log.i(TAG, "Observing " + supervisedUserIds.size() + " children");
        
        // Query latest messages for all children
        latestMessagesLiveData = database.receivedBtDataDao()
                .getLatestForOwners(supervisedUserIds);
        
        // Transform messages into ChildPostureData
        childrenData.addSource(latestMessagesLiveData, messages -> {
            executorService.execute(() -> {
                List<ChildPostureData> childrenPostureData = processMessages(messages, supervisedUserIds);
                childrenData.postValue(childrenPostureData);
            });
        });
    }
    
    /**
     * Process messages and combine with child profiles
     * Thread: Background (ExecutorService)
     */
    private List<ChildPostureData> processMessages(List<ReceivedBtDataEntity> messages, 
                                                   List<String> allChildIds) {
        List<ChildPostureData> result = new ArrayList<>();
        
        // Create map of childId -> latest message
        Map<String, ReceivedBtDataEntity> latestByChild = new HashMap<>();
        for (ReceivedBtDataEntity message : messages) {
            String childId = message.getOwnerUserId();
            if (childId != null) {
                // Keep only the latest message per child
                if (!latestByChild.containsKey(childId) ||
                    latestByChild.get(childId).getTimestamp() < message.getTimestamp()) {
                    latestByChild.put(childId, message);
                }
            }
        }
        
        Log.d(TAG, "Processing " + allChildIds.size() + " children, " + 
              latestByChild.size() + " have data");
        
        // Create ChildPostureData for each child
        for (String childId : allChildIds) {
            ChildProfile profile = null;
            if (childRegistry != null) {
                profile = childRegistry.getChild(childId);
            }
            
            ReceivedBtDataEntity latestMessage = latestByChild.get(childId);
            
            if (latestMessage != null) {
                // Parse posture
                Posture posture = null;
                try {
                    posture = PostureFactory.createPosture(latestMessage.getReceivedMsg());
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing posture for child " + childId, e);
                }
                
                ChildPostureData childData = new ChildPostureData(
                    childId,
                    profile,
                    posture,
                    latestMessage.getTimestamp(),
                    latestMessage.getReceivedMsg()
                );
                result.add(childData);
            } else {
                // No data yet for this child
                ChildPostureData childData = new ChildPostureData(
                    childId,
                    profile,
                    null,
                    0L,
                    null
                );
                result.add(childData);
            }
        }
        
        Log.d(TAG, "Created " + result.size() + " ChildPostureData objects");
        return result;
    }
    
    /**
     * Get LiveData of all children with their latest posture data
     */
    public LiveData<List<ChildPostureData>> getChildrenData() {
        return childrenData;
    }
    
    /**
     * Get linked aggregator ID (for display)
     */
    public String getLinkedAggregatorId() {
        return userSession.getLinkedAggregatorId();
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}

