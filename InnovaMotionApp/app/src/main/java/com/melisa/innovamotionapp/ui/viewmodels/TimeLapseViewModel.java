package com.melisa.innovamotionapp.ui.viewmodels;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.melisa.innovamotionapp.data.database.InnovaDatabase;
import com.melisa.innovamotionapp.data.database.ReceivedBtDataDao;
import com.melisa.innovamotionapp.data.database.ReceivedBtDataEntity;
import com.melisa.innovamotionapp.utils.GlobalData;

import java.util.List;

public class TimeLapseViewModel extends AndroidViewModel {
    private final ReceivedBtDataDao receivedBtDataDao;
    private final MutableLiveData<List<ReceivedBtDataEntity>> latestDevicePostureData = new MutableLiveData<>();


    public TimeLapseViewModel(Application application) {
        super(application);
        InnovaDatabase database = InnovaDatabase.getInstance(application);
        receivedBtDataDao = database.receivedBtDataDao();
    }


    public LiveData<List<ReceivedBtDataEntity>> getDataForLatestDevice() {
        // Get saved received msg data and get a list of Postures
        String lastDeviceAddress = GlobalData.getInstance().userDeviceSettingsStorage.getLatestDeviceAddress();
        return receivedBtDataDao.getDataForDevice(lastDeviceAddress);
    }
}
