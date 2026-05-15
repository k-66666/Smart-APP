package com.example.mysmart.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.mysmart.model.SensorData;

import java.util.List;

/**
 * 传感器数据访问对象
 */
@Dao
public interface SensorDataDao {
    
    @Insert
    void insert(SensorData data);
    
    @Query("SELECT * FROM sensor_data WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    List<SensorData> getDataInRange(long startTime, long endTime);
    
    @Query("SELECT * FROM sensor_data WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp ASC")
    List<SensorData> getDataInRangeAscending(long startTime, long endTime);
    
    @Query("DELETE FROM sensor_data WHERE timestamp < :cutoffTime")
    void deleteOldData(long cutoffTime);
    
    @Query("SELECT * FROM sensor_data ORDER BY timestamp DESC LIMIT 1")
    SensorData getLatestData();
    
    @Query("SELECT * FROM sensor_data ORDER BY timestamp DESC LIMIT 1")
    LiveData<SensorData> getLatestDataLive();
    
    @Query("SELECT COUNT(*) FROM sensor_data")
    int getDataCount();
    
    @Query("DELETE FROM sensor_data")
    void deleteAll();
}
