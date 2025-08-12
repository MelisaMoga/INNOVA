package com.melisa.innovamotionapp.data.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {ReceivedBtDataEntity.class}, version = 1)
public abstract class InnovaDatabase extends RoomDatabase {
    private static InnovaDatabase instance;

    public abstract ReceivedBtDataDao receivedBtDataDao();

    public static synchronized InnovaDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    InnovaDatabase.class, "my_database").build();
        }
        return instance;
    }
}
