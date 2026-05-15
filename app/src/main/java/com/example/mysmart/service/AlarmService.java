package com.example.mysmart.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.mysmart.MainActivity;
import com.example.mysmart.R;
import com.example.mysmart.model.SensorData;

/**
 * 报警服务
 */
public class AlarmService {
    
    private static final String CHANNEL_ID = "sensor_alarm_channel";
    private static final String CHANNEL_NAME = "传感器报警";
    private static final int NOTIFICATION_ID = 1001;
    
    private final Context context;
    private final SharedPreferences alarmSettings;
    private final NotificationManager notificationManager;
    
    private long lastAlarmTime = 0;
    private static final long ALARM_INTERVAL = 5 * 60 * 1000; // 5分钟
    
    public AlarmService(Context context) {
        this.context = context.getApplicationContext();
        this.alarmSettings = context.getSharedPreferences("alarm_settings", Context.MODE_PRIVATE);
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        createNotificationChannel();
    }
    
    /**
     * 创建通知渠道
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("传感器数据超过阈值时的报警通知");
            channel.enableVibration(true);
            channel.enableLights(true);
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    /**
     * 检查传感器数据并触发报警
     */
    public void checkAndAlarm(SensorData data) {
        // 检查报警是否启用
        boolean alarmEnabled = alarmSettings.getBoolean("alarm_enabled", true);
        if (!alarmEnabled) {
            return;
        }
        
        // 检查报警间隔
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAlarmTime < ALARM_INTERVAL) {
            return;
        }
        
        // 获取阈值
        float tempMin = alarmSettings.getFloat("temp_min", 15.0f);
        float tempMax = alarmSettings.getFloat("temp_max", 30.0f);
        float humidityMin = alarmSettings.getFloat("humidity_min", 30.0f);
        float humidityMax = alarmSettings.getFloat("humidity_max", 70.0f);
        int airQualityMax = alarmSettings.getInt("air_quality_max", 150);
        int lightMin = alarmSettings.getInt("light_min", 100);
        int lightMax = alarmSettings.getInt("light_max", 5000);
        
        StringBuilder alarmMessage = new StringBuilder();
        
        // 检查温度
        if (data.getTemperature() < tempMin) {
            alarmMessage.append("温度过低: ").append(String.format("%.1f°C", data.getTemperature())).append("\n");
        } else if (data.getTemperature() > tempMax) {
            alarmMessage.append("温度过高: ").append(String.format("%.1f°C", data.getTemperature())).append("\n");
        }
        
        // 检查湿度
        if (data.getHumidity() < humidityMin) {
            alarmMessage.append("湿度过低: ").append(String.format("%.0f%%", data.getHumidity())).append("\n");
        } else if (data.getHumidity() > humidityMax) {
            alarmMessage.append("湿度过高: ").append(String.format("%.0f%%", data.getHumidity())).append("\n");
        }
        
        // 检查空气质量
        if (data.getAirQuality() > airQualityMax) {
            alarmMessage.append("空气质量超标: ").append(data.getAirQuality()).append(" AQI\n");
        }
        
        // 检查光照强度
        if (data.getLightIntensity() < lightMin) {
            alarmMessage.append("光照强度过低: ").append(data.getLightIntensity()).append(" Lux\n");
        } else if (data.getLightIntensity() > lightMax) {
            alarmMessage.append("光照强度过高: ").append(data.getLightIntensity()).append(" Lux\n");
        }
        
        // 如果有报警，发送通知
        if (alarmMessage.length() > 0) {
            sendAlarmNotification("环境参数异常", alarmMessage.toString().trim());
            lastAlarmTime = currentTime;
        }
    }
    
    /**
     * 发送报警通知
     */
    private void sendAlarmNotification(String title, String message) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 500, 200, 500})
                .setContentIntent(pendingIntent);
        
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
    
    /**
     * 发送恢复正常通知
     */
    public void sendRecoveryNotification() {
        sendAlarmNotification("环境参数恢复正常", "所有传感器数据已恢复到正常范围");
    }
}
