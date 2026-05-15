package com.example.mysmart.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.mysmart.MainActivity;
import com.example.mysmart.R;
import com.example.mysmart.config.DeviceConfig;
import com.example.mysmart.model.SensorData;

/**
 * 报警服务
 * 监控传感器数据，当超过阈值时触发报警通知
 * 
 * 报警触发条件（与STM32同步）：
 * 1. 湿度超过75%（HUMIDITY_ALARM_THRESHOLD）
 * 2. 紫外线消毒完成提示
 * 3. TVOC或CO2浓度过高（可选）
 */
public class AlarmService {
    
    private static final String TAG = "AlarmService";
    private static final String CHANNEL_ID = "sensor_alarm_channel";
    private static final String CHANNEL_NAME = "智能防霉柜报警";
    private static final int NOTIFICATION_ID = 1001;
    
    private final Context context;
    private final SharedPreferences alarmSettings;
    private final NotificationManager notificationManager;
    
    // 报警状态
    private boolean isHumidityAlarmActive = false;
    private boolean isUvCompleteAlarmActive = false;
    private boolean isTvocAlarmActive = false;
    private boolean isCo2AlarmActive = false;
    
    // 上次报警时间（防止频繁报警）
    private long lastAlarmTime = 0;
    private static final long ALARM_COOLDOWN_MS = 60000; // 1分钟冷却时间
    
    public AlarmService(Context context) {
        this.context = context.getApplicationContext();
        this.alarmSettings = context.getSharedPreferences("alarm_settings", Context.MODE_PRIVATE);
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        createNotificationChannel();
        Log.d(TAG, "报警服务已初始化");
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
            channel.setDescription("智能防霉柜环境监测报警通知");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            channel.enableLights(true);
            
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "通知渠道已创建");
            }
        }
    }
    
    /**
     * 检查传感器数据并触发报警
     * 与STM32报警逻辑同步
     */
    public void checkAndAlarm(SensorData data) {
        if (data == null) {
            return;
        }
        
        // 检查报警是否启用
        boolean alarmEnabled = alarmSettings.getBoolean("alarm_enabled", true);
        if (!alarmEnabled) {
            return;
        }
        
        boolean shouldAlarm = false;
        StringBuilder alarmMessage = new StringBuilder();
        
        // 1. 检查湿度报警（与STM32同步：HUMIDITY_ALARM_THRESHOLD = 75%）
        if (data.getHumidity() > DeviceConfig.HUMIDITY_ALARM_THRESHOLD) {
            if (!isHumidityAlarmActive) {
                isHumidityAlarmActive = true;
                shouldAlarm = true;
                alarmMessage.append("⚠️ 湿度过高: ").append(String.format("%.1f%%", data.getHumidity()));
                Log.w(TAG, "湿度报警触发: " + data.getHumidity() + "%");
            }
        } else {
            if (isHumidityAlarmActive) {
                isHumidityAlarmActive = false;
                Log.d(TAG, "湿度报警解除");
            }
        }
        
        // 2. 检查紫外线消毒完成（从设备状态同步）
        if (data.isAlarmState() && !isUvCompleteAlarmActive) {
            isUvCompleteAlarmActive = true;
            shouldAlarm = true;
            if (alarmMessage.length() > 0) alarmMessage.append("\n");
            alarmMessage.append("✅ 紫外线消毒已完成");
            Log.i(TAG, "紫外线消毒完成提示");
        } else if (!data.isAlarmState() && isUvCompleteAlarmActive) {
            isUvCompleteAlarmActive = false;
        }
        
        // 3. 检查TVOC浓度（可选，严重污染时报警）
        boolean tvocAlarmEnabled = alarmSettings.getBoolean("tvoc_alarm_enabled", true);
        if (tvocAlarmEnabled && data.getAirQuality() > DeviceConfig.TVOC_POOR) {
            if (!isTvocAlarmActive) {
                isTvocAlarmActive = true;
                shouldAlarm = true;
                if (alarmMessage.length() > 0) alarmMessage.append("\n");
                alarmMessage.append("⚠️ TVOC浓度过高: ").append(data.getAirQuality()).append(" μg/m³");
                Log.w(TAG, "TVOC报警触发: " + data.getAirQuality());
            }
        } else {
            if (isTvocAlarmActive) {
                isTvocAlarmActive = false;
                Log.d(TAG, "TVOC报警解除");
            }
        }
        
        // 4. 检查CO2浓度（可选，浓度很差时报警）
        boolean co2AlarmEnabled = alarmSettings.getBoolean("co2_alarm_enabled", true);
        if (co2AlarmEnabled && data.getCo2Concentration() > DeviceConfig.CO2_POOR) {
            if (!isCo2AlarmActive) {
                isCo2AlarmActive = true;
                shouldAlarm = true;
                if (alarmMessage.length() > 0) alarmMessage.append("\n");
                alarmMessage.append("⚠️ CO2浓度过高: ").append(data.getCo2Concentration()).append(" ppm");
                Log.w(TAG, "CO2报警触发: " + data.getCo2Concentration());
            }
        } else {
            if (isCo2AlarmActive) {
                isCo2AlarmActive = false;
                Log.d(TAG, "CO2报警解除");
            }
        }
        
        // 触发报警通知（带冷却时间）
        if (shouldAlarm) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastAlarmTime > ALARM_COOLDOWN_MS) {
                sendAlarmNotification(alarmMessage.toString());
                lastAlarmTime = currentTime;
            }
        }
    }
    
    /**
     * 发送报警通知
     */
    private void sendAlarmNotification(String message) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // 使用系统默认报警音
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmSound == null) {
            alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("智能防霉柜报警")
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setSound(alarmSound)
                .setVibrate(new long[]{0, 1000, 500, 1000});
        
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
            Log.i(TAG, "报警通知已发送: " + message);
        }
    }
    
    /**
     * 发送恢复正常通知
     */
    public void sendRecoveryNotification() {
        if (!isHumidityAlarmActive && !isTvocAlarmActive && !isCo2AlarmActive) {
            Intent intent = new Intent(context, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("智能防霉柜")
                    .setContentText("✅ 环境参数已恢复正常")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent);
            
            if (notificationManager != null) {
                notificationManager.notify(NOTIFICATION_ID + 1, builder.build());
                Log.i(TAG, "恢复正常通知已发送");
            }
        }
    }
    
    /**
     * 重置所有报警状态
     */
    public void resetAlarmStates() {
        isHumidityAlarmActive = false;
        isUvCompleteAlarmActive = false;
        isTvocAlarmActive = false;
        isCo2AlarmActive = false;
        Log.d(TAG, "所有报警状态已重置");
    }
    
    /**
     * 获取当前报警状态
     */
    public boolean isAnyAlarmActive() {
        return isHumidityAlarmActive || isUvCompleteAlarmActive || isTvocAlarmActive || isCo2AlarmActive;
    }
}
