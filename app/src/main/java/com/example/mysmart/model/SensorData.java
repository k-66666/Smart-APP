package com.example.mysmart.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 传感器数据实体类
 */
@Entity(tableName = "sensor_data")
public class SensorData {
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    private long timestamp;        // 时间戳
    private float temperature;     // 温度 (-40 to 80°C)
    private float humidity;        // 湿度 (0 to 100%)
    private int airQuality;        // 空气质量 (0 to 500)
    private int lightIntensity;    // 光照强度 (0 to 100000 Lux)
    
    public SensorData() {
        this.timestamp = System.currentTimeMillis();
    }
    
    @androidx.room.Ignore
    public SensorData(float temperature, float humidity, int airQuality, int lightIntensity) {
        this.timestamp = System.currentTimeMillis();
        this.temperature = temperature;
        this.humidity = humidity;
        this.airQuality = airQuality;
        this.lightIntensity = lightIntensity;
    }
    
    // Getters and Setters
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public float getTemperature() {
        return temperature;
    }
    
    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }
    
    public float getHumidity() {
        return humidity;
    }
    
    public void setHumidity(float humidity) {
        this.humidity = humidity;
    }
    
    public int getAirQuality() {
        return airQuality;
    }
    
    public void setAirQuality(int airQuality) {
        this.airQuality = airQuality;
    }
    
    public int getLightIntensity() {
        return lightIntensity;
    }
    
    public void setLightIntensity(int lightIntensity) {
        this.lightIntensity = lightIntensity;
    }
    
    /**
     * 检查数据是否过期（超过10秒）
     */
    public boolean isExpired() {
        return System.currentTimeMillis() - timestamp > 10000;
    }
    
    /**
     * 获取数据年龄（秒）
     */
    public long getAgeInSeconds() {
        return (System.currentTimeMillis() - timestamp) / 1000;
    }
}
