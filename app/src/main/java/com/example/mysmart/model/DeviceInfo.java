package com.example.mysmart.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 设备信息实体类
 */
@Entity(tableName = "devices")
public class DeviceInfo {
    @PrimaryKey
    @NonNull
    private String deviceId;       // MAC地址或IP
    
    private String deviceName;     // 设备名称
    private String alias;          // 用户设置的别名
    private String connectionType; // "BLUETOOTH" or "WIFI"
    private long lastConnected;    // 最后连接时间
    
    public DeviceInfo(@NonNull String deviceId, String deviceName, String connectionType) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.connectionType = connectionType;
        this.lastConnected = System.currentTimeMillis();
    }
    
    // Getters and Setters
    @NonNull
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(@NonNull String deviceId) {
        this.deviceId = deviceId;
    }
    
    public String getDeviceName() {
        return deviceName;
    }
    
    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }
    
    public String getAlias() {
        return alias;
    }
    
    public void setAlias(String alias) {
        this.alias = alias;
    }
    
    public String getConnectionType() {
        return connectionType;
    }
    
    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }
    
    public long getLastConnected() {
        return lastConnected;
    }
    
    public void setLastConnected(long lastConnected) {
        this.lastConnected = lastConnected;
    }
    
    /**
     * 获取显示名称（优先使用别名）
     */
    public String getDisplayName() {
        return (alias != null && !alias.isEmpty()) ? alias : deviceName;
    }
}
