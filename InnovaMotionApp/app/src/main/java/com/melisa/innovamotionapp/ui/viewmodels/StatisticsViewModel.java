package com.melisa.innovamotionapp.ui.viewmodels;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.melisa.innovamotionapp.data.database.InnovaDatabase;
import com.melisa.innovamotionapp.data.database.ReceivedBtDataDao;
import com.melisa.innovamotionapp.data.database.ReceivedBtDataEntity;
import com.melisa.innovamotionapp.utils.GlobalData;

import java.util.List;

public class StatisticsViewModel extends AndroidViewModel {
    private final ReceivedBtDataDao receivedBtDataDao;


    public StatisticsViewModel(Application application) {
        super(application);
        InnovaDatabase database = InnovaDatabase.getInstance(application);
        receivedBtDataDao = database.receivedBtDataDao();
    }


    public LiveData<List<ReceivedBtDataEntity>> getDataForDevice() {
        // Get saved received msg data and get a list of Postures
        String lastDeviceAddress = GlobalData.getInstance().userDeviceSettingsStorage.getLatestDeviceAddress();
        return receivedBtDataDao.getDataForDevice(lastDeviceAddress);
    }

    public LiveData<List<ReceivedBtDataEntity>> getDataForDeviceInRange(long startTime, long endTime) {
        // Get saved received msg data and get a list of Postures
        String lastDeviceAddress = GlobalData.getInstance().userDeviceSettingsStorage.getLatestDeviceAddress();
        return receivedBtDataDao.getDataForDeviceInRange(lastDeviceAddress, startTime, endTime);
    }

}
