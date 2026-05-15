package com.example.mysmart.config;

/**
 * 设备配置类
 * 与STM32防霉柜系统配置对应
 */
public class DeviceConfig {
    
    // ========== 湿度阈值配置 ==========
    // 对应STM32: HUMIDITY_HIGH_THRESHOLD
    public static final float HUMIDITY_HIGH_THRESHOLD = 65.0f;
    
    // 对应STM32: HUMIDITY_LOW_THRESHOLD
    public static final float HUMIDITY_LOW_THRESHOLD = 50.0f;
    
    // 对应STM32: HUMIDITY_ALARM_THRESHOLD
    public static final float HUMIDITY_ALARM_THRESHOLD = 75.0f;
    
    // ========== 紫外线消毒配置 ==========
    // 对应STM32: UV_DISINFECT_DURATION (分钟)
    public static final int UV_DISINFECT_DURATION_MINUTES = 15;
    
    // 对应STM32: FAN_RUN_TIME_BEFORE_UV (分钟)
    public static final int FAN_RUN_TIME_BEFORE_UV_MINUTES = 30;
    
    // 对应STM32: UV_SCHEDULED_HOUR
    public static final int UV_SCHEDULED_HOUR = 2;
    
    // 对应STM32: UV_SCHEDULED_MINUTE
    public static final int UV_SCHEDULED_MINUTE = 0;
    
    // ========== WiFi连接配置 ==========
    // 对应STM32: WIFI_TCP_PORT
    public static final int WIFI_TCP_PORT = 8080;
    
    // 数据更新间隔（毫秒）- 对应STM32的5秒发送间隔
    public static final int DATA_UPDATE_INTERVAL_MS = 5000;
    
    // ========== 传感器数据范围 ==========
    public static final float TEMPERATURE_MIN = -40.0f;
    public static final float TEMPERATURE_MAX = 80.0f;
    
    public static final float HUMIDITY_MIN = 0.0f;
    public static final float HUMIDITY_MAX = 100.0f;
    
    public static final int TVOC_MIN = 0;
    public static final int TVOC_MAX = 9999;
    
    public static final int CO2_MIN = 0;
    public static final int CO2_MAX = 5000;
    
    // ========== 空气质量等级阈值 ==========
    // TVOC等级（μg/m³）- 参考GB/T 18883-2002
    public static final int TVOC_EXCELLENT = 220;
    public static final int TVOC_GOOD = 660;
    public static final int TVOC_MODERATE = 2200;
    public static final int TVOC_POOR = 5500;
    
    // CO2等级（ppm）
    public static final int CO2_EXCELLENT = 600;
    public static final int CO2_GOOD = 1000;
    public static final int CO2_MODERATE = 1500;
    public static final int CO2_POOR = 2000;
    
    // ========== 设备状态 ==========
    public static final int DEVICE_STATE_OFF = 0;
    public static final int DEVICE_STATE_ON = 1;
    public static final int DEVICE_STATE_AUTO = 2;
    
    // ========== 设备类型 ==========
    public static final String DEVICE_TYPE_FAN = "fan";
    public static final String DEVICE_TYPE_UV = "uv";
    public static final String DEVICE_TYPE_LIGHT = "light";
    
    /**
     * 获取湿度状态描述
     */
    public static String getHumidityStatus(float humidity) {
        if (humidity < HUMIDITY_LOW_THRESHOLD) {
            return "干燥";
        } else if (humidity < HUMIDITY_HIGH_THRESHOLD) {
            return "适宜";
        } else if (humidity < HUMIDITY_ALARM_THRESHOLD) {
            return "偏高";
        } else {
            return "过高";
        }
    }
    
    /**
     * 判断是否需要除湿
     */
    public static boolean needsDehumidification(float humidity) {
        return humidity > HUMIDITY_HIGH_THRESHOLD;
    }
    
    /**
     * 判断是否需要报警
     */
    public static boolean needsAlarm(float humidity) {
        return humidity > HUMIDITY_ALARM_THRESHOLD;
    }
    
    /**
     * 获取TVOC等级
     */
    public static String getTvocLevel(int tvoc) {
        if (tvoc <= TVOC_EXCELLENT) {
            return "优秀";
        } else if (tvoc <= TVOC_GOOD) {
            return "良好";
        } else if (tvoc <= TVOC_MODERATE) {
            return "轻度污染";
        } else if (tvoc <= TVOC_POOR) {
            return "中度污染";
        } else {
            return "重度污染";
        }
    }
    
    /**
     * 获取CO2等级
     */
    public static String getCo2Level(int co2) {
        if (co2 <= CO2_EXCELLENT) {
            return "优秀";
        } else if (co2 <= CO2_GOOD) {
            return "良好";
        } else if (co2 <= CO2_MODERATE) {
            return "一般";
        } else if (co2 <= CO2_POOR) {
            return "较差";
        } else {
            return "很差";
        }
    }
    
    /**
     * 获取设备状态描述
     */
    public static String getDeviceStateDescription(int state) {
        switch (state) {
            case DEVICE_STATE_OFF:
                return "关闭";
            case DEVICE_STATE_ON:
                return "开启";
            case DEVICE_STATE_AUTO:
                return "自动";
            default:
                return "未知";
        }
    }
}
