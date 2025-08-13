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

public class StatisticsViewModel extends AndroidViewModel {
    private final ReceivedBtDataDao dao;
    private final GlobalData global = GlobalData.getInstance();
    private final MutableLiveData<String> targetUserId = new MutableLiveData<>();

    public StatisticsViewModel(@NonNull Application app) {
        super(app);
        dao = InnovaDatabase.getInstance(app).receivedBtDataDao();
        // Default: keep null until Activity sets it when session is ready
        targetUserId.setValue(null);
    }

    public void setTargetUserId(String userId) { targetUserId.setValue(userId); }

    public LiveData<List<ReceivedBtDataEntity>> getAllForUser() {
        return Transformations.switchMap(targetUserId, uid -> {
            Log.i("UI/StatsVM", "subscribe targetUser=" + uid);
            if (uid == null) return new MutableLiveData<>(Collections.emptyList());
            return dao.getAllForUserLive(uid);
        });
    }

    public LiveData<List<ReceivedBtDataEntity>> getRangeForUser(long start, long end) {
        return Transformations.switchMap(targetUserId, uid -> {
            Log.i("UI/StatsVM", "subscribe targetUser=" + uid + " range=[" + start + "," + end + "]");
            if (uid == null) return new MutableLiveData<>(Collections.emptyList());
            return dao.getRangeForUserLive(uid, start, end);
        });
    }
}
