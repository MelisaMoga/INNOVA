package com.melisa.innovamotionapp.data.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {ReceivedBtDataEntity.class, MonitoredPerson.class}, version = 4)
public abstract class InnovaDatabase extends RoomDatabase {
    private static InnovaDatabase instance;
    
    // Migration from version 1 to 2 - adds unique index
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Create unique index on (device_address, timestamp, received_msg)
            database.execSQL("CREATE UNIQUE INDEX index_received_bt_data_device_address_timestamp_received_msg " +
                    "ON received_bt_data (device_address, timestamp, received_msg)");
        }
    };

    public abstract ReceivedBtDataDao receivedBtDataDao();
    
    public abstract MonitoredPersonDao monitoredPersonDao();

    public static synchronized InnovaDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    InnovaDatabase.class, "my_database")
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration() // Fallback for development
                    .build();
            
            // One-liner DB path log (once)
            android.util.Log.i("DB", "Path=" + context.getDatabasePath("my_database").getAbsolutePath());
        }
        return instance;
    }
}
