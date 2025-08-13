package com.melisa.innovamotionapp.ui.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.melisa.innovamotionapp.data.database.InnovaDatabase;
import com.melisa.innovamotionapp.data.database.ReceivedBtDataDao;
import com.melisa.innovamotionapp.data.database.ReceivedBtDataEntity;
import com.melisa.innovamotionapp.utils.GlobalData;
import com.melisa.innovamotionapp.utils.OwnerSource;

import java.util.Collections;
import java.util.List;

public class TimeLapseViewModel extends AndroidViewModel {
    private final ReceivedBtDataDao dao;
    private final GlobalData global = GlobalData.getInstance();
    private final MutableLiveData<String> targetUserId = new MutableLiveData<>();
    private final MutableLiveData<List<ReceivedBtDataEntity>> latestDevicePostureData = new MutableLiveData<>();

    public TimeLapseViewModel(@NonNull Application app) {
        super(app);
        dao = InnovaDatabase.getInstance(app).receivedBtDataDao();
        // Default: keep null until Activity sets it
        targetUserId.setValue(null);
    }

    public void setTargetUserId(String userId) { targetUserId.setValue(userId); }

    public LiveData<List<ReceivedBtDataEntity>> getAllForUser() {
        return Transformations.switchMap(targetUserId, uid -> {
            Log.i("UI/TimeLapseVM", "subscribe targetUser=" + uid);
            if (uid == null) return new MutableLiveData<>(Collections.emptyList());
            return dao.getAllForUserLive(uid);
        });
    }
}
