package com.example.mysmart.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.mysmart.model.DeviceInfo;

import java.util.List;

/**
 * 设备信息访问对象
 */
@Dao
public interface DeviceInfoDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DeviceInfo device);
    
    @Update
    void update(DeviceInfo device);
    
    @Delete
    void delete(DeviceInfo device);
    
    @Query("SELECT * FROM devices ORDER BY lastConnected DESC")
    List<DeviceInfo> getAllDevices();
    
    @Query("SELECT * FROM devices WHERE deviceId = :deviceId")
    DeviceInfo getDeviceById(String deviceId);
    
    @Query("SELECT * FROM devices ORDER BY lastConnected DESC LIMIT 1")
    DeviceInfo getLastConnectedDevice();
    
    @Query("DELETE FROM devices")
    void deleteAll();
}
