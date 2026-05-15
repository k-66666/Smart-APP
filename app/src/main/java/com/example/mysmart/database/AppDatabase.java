package com.example.mysmart.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.mysmart.model.DeviceInfo;
import com.example.mysmart.model.SensorData;

/**
 * 应用数据库
 */
@Database(entities = {SensorData.class, DeviceInfo.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    
    private static volatile AppDatabase INSTANCE;
    
    public abstract SensorDataDao sensorDataDao();
    public abstract DeviceInfoDao deviceInfoDao();
    
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "smart_environment_db"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}
