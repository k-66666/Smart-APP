package com.example.mysmart.model;

/**
 * 报警阈值配置类
 */
public class AlarmThreshold {
    private float temperatureMin = 15.0f;
    private float temperatureMax = 30.0f;
    private float humidityMin = 30.0f;
    private float humidityMax = 70.0f;
    private int airQualityMax = 150;
    private int lightIntensityMin = 100;
    private int lightIntensityMax = 5000;
    
    public AlarmThreshold() {
    }
    
    // Getters and Setters
    public float getTemperatureMin() {
        return temperatureMin;
    }
    
    public void setTemperatureMin(float temperatureMin) {
        this.temperatureMin = temperatureMin;
    }
    
    public float getTemperatureMax() {
        return temperatureMax;
    }
    
    public void setTemperatureMax(float temperatureMax) {
        this.temperatureMax = temperatureMax;
    }
    
    public float getHumidityMin() {
        return humidityMin;
    }
    
    public void setHumidityMin(float humidityMin) {
        this.humidityMin = humidityMin;
    }
    
    public float getHumidityMax() {
        return humidityMax;
    }
    
    public void setHumidityMax(float humidityMax) {
        this.humidityMax = humidityMax;
    }
    
    public int getAirQualityMax() {
        return airQualityMax;
    }
    
    public void setAirQualityMax(int airQualityMax) {
        this.airQualityMax = airQualityMax;
    }
    
    public int getLightIntensityMin() {
        return lightIntensityMin;
    }
    
    public void setLightIntensityMin(int lightIntensityMin) {
        this.lightIntensityMin = lightIntensityMin;
    }
    
    public int getLightIntensityMax() {
        return lightIntensityMax;
    }
    
    public void setLightIntensityMax(int lightIntensityMax) {
        this.lightIntensityMax = lightIntensityMax;
    }
    
    /**
     * 验证阈值设置是否有效
     */
    public boolean isValid() {
        return temperatureMax > temperatureMin &&
               humidityMax > humidityMin &&
               lightIntensityMax > lightIntensityMin &&
               airQualityMax > 0;
    }
}
