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
    private int airQuality;        // 空气质量/TVOC (0 to 9999 μg/m³)
    private int co2Concentration;  // 二氧化碳浓度 (ppm)
    
    // 设备状态（从STM32同步）
    private boolean fanState;      // 风扇状态
    private boolean uvState;       // 紫外线灯状态
    private boolean lightState;    // 照明灯状态
    private boolean alarmState;    // 报警状态
    
    public SensorData() {
        this.timestamp = System.currentTimeMillis();
    }
    
    @androidx.room.Ignore
    public SensorData(float temperature, float humidity, int airQuality, int co2Concentration) {
        this.timestamp = System.currentTimeMillis();
        this.temperature = temperature;
        this.humidity = humidity;
        this.airQuality = airQuality;
        this.co2Concentration = co2Concentration;
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
    
    public int getCo2Concentration() {
        return co2Concentration;
    }
    
    public void setCo2Concentration(int co2Concentration) {
        this.co2Concentration = co2Concentration;
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
    
    // 设备状态的 Getters 和 Setters
    public boolean isFanState() {
        return fanState;
    }
    
    public void setFanState(boolean fanState) {
        this.fanState = fanState;
    }
    
    public boolean isUvState() {
        return uvState;
    }
    
    public void setUvState(boolean uvState) {
        this.uvState = uvState;
    }
    
    public boolean isLightState() {
        return lightState;
    }
    
    public void setLightState(boolean lightState) {
        this.lightState = lightState;
    }
    
    public boolean isAlarmState() {
        return alarmState;
    }
    
    public void setAlarmState(boolean alarmState) {
        this.alarmState = alarmState;
    }
    
    /**
     * 获取TVOC等级描述
     * 根据国标GB/T 18883-2002
     */
    public String getTvocLevel() {
        if (airQuality <= 220) {
            return "优秀";
        } else if (airQuality <= 660) {
            return "良好";
        } else if (airQuality <= 2200) {
            return "轻度污染";
        } else if (airQuality <= 5500) {
            return "中度污染";
        } else {
            return "重度污染";
        }
    }
    
    /**
     * 获取CO2等级描述
     */
    public String getCo2Level() {
        if (co2Concentration <= 600) {
            return "优秀";
        } else if (co2Concentration <= 1000) {
            return "良好";
        } else if (co2Concentration <= 1500) {
            return "一般";
        } else if (co2Concentration <= 2000) {
            return "较差";
        } else {
            return "很差";
        }
    }
    
    /**
     * 获取湿度等级描述
     * 根据防霉柜的阈值设置
     */
    public String getHumidityLevel() {
        if (humidity < 50) {
            return "干燥";
        } else if (humidity < 65) {
            return "适宜";
        } else if (humidity < 75) {
            return "偏高";
        } else {
            return "过高";
        }
    }
}
