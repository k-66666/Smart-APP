package com.example.mysmart.connection;

import android.util.Log;

import com.example.mysmart.model.ControlCommand;
import com.example.mysmart.model.SensorData;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 通信协议解析器
 * 
 * 协议说明：
 * STM32通过WiFi模块（ESP8266）发送JSON格式的传感器数据
 * 
 * 数据格式示例：
 * {"temp":25.5,"humi":60.3,"tvoc":120,"co2":450,"fan":1,"uv":0,"light":1,"alarm":0}
 * 
 * 字段说明：
 * - temp: 温度（°C），范围 -40 to 80
 * - humi: 湿度（%），范围 0 to 100
 * - tvoc: TVOC浓度（μg/m³），范围 0 to 9999
 * - co2: CO2浓度（ppm），范围 0 to 5000
 * - fan: 风扇状态（0=关闭，1=开启）
 * - uv: 紫外线灯状态（0=关闭，1=开启）
 * - light: 照明灯状态（0=关闭，1=开启）
 * - alarm: 报警状态（0=关闭，1=开启）
 * 
 * 控制命令格式：
 * {"cmd":"fan","value":1}  - 控制风扇
 * {"cmd":"uv","value":0}   - 控制紫外线灯
 * {"cmd":"light","value":1} - 控制照明灯
 */
public class ProtocolParser {
    
    private static final String TAG = "ProtocolParser";
    
    /**
     * 解析JSON格式的传感器数据
     * 
     * @param jsonString JSON字符串，格式：{"temp":25.5,"humi":60.3,"tvoc":120,"co2":450,...}
     * @return SensorData对象
     * @throws ProtocolException 解析失败时抛出
     */
    public static SensorData parseSensorData(String jsonString) throws ProtocolException {
        try {
            JSONObject json = new JSONObject(jsonString);
            
            // 解析传感器数据
            float temperature = (float) json.optDouble("temp", 25.0);
            float humidity = (float) json.optDouble("humi", 50.0);
            int tvoc = json.optInt("tvoc", 0);
            int co2 = json.optInt("co2", 400);
            
            // 验证数据范围
            if (temperature < -40 || temperature > 80) {
                Log.w(TAG, "温度值超出正常范围: " + temperature);
                temperature = Math.max(-40, Math.min(80, temperature));
            }
            if (humidity < 0 || humidity > 100) {
                Log.w(TAG, "湿度值超出正常范围: " + humidity);
                humidity = Math.max(0, Math.min(100, humidity));
            }
            if (tvoc < 0 || tvoc > 9999) {
                Log.w(TAG, "TVOC值超出正常范围: " + tvoc);
                tvoc = Math.max(0, Math.min(9999, tvoc));
            }
            if (co2 < 0 || co2 > 5000) {
                Log.w(TAG, "CO2值超出正常范围: " + co2);
                co2 = Math.max(0, Math.min(5000, co2));
            }
            
            // 创建传感器数据对象（TVOC作为空气质量指标）
            SensorData sensorData = new SensorData(temperature, humidity, tvoc, co2);
            
            // 解析设备状态（可选，用于状态同步）
            if (json.has("fan")) {
                sensorData.setFanState(json.optInt("fan", 0) == 1);
            }
            if (json.has("uv")) {
                sensorData.setUvState(json.optInt("uv", 0) == 1);
            }
            if (json.has("light")) {
                sensorData.setLightState(json.optInt("light", 0) == 1);
            }
            if (json.has("alarm")) {
                sensorData.setAlarmState(json.optInt("alarm", 0) == 1);
            }
            
            return sensorData;
            
        } catch (JSONException e) {
            Log.e(TAG, "JSON解析失败: " + jsonString, e);
            throw new ProtocolException("JSON解析失败: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "数据解析异常: " + jsonString, e);
            throw new ProtocolException("数据解析失败: " + e.getMessage());
        }
    }
    
    /**
     * 解析字节数组格式的传感器数据（兼容旧版本）
     * 
     * @param data 字节数组
     * @return SensorData对象
     * @throws ProtocolException 解析失败时抛出
     */
    public static SensorData parseSensorData(byte[] data) throws ProtocolException {
        try {
            // 尝试将字节数组转换为字符串
            String jsonString = new String(data, "UTF-8").trim();
            return parseSensorData(jsonString);
        } catch (Exception e) {
            throw new ProtocolException("字节数组转换失败: " + e.getMessage());
        }
    }
    
    /**
     * 构建控制命令（JSON格式）
     * 
     * @param deviceType 设备类型 ("fan", "uv", "light")
     * @param state 状态 (0=关闭, 1=开启, 2=自动)
     * @return JSON字节数组
     */
    public static byte[] buildControlCommand(String deviceType, int state) {
        try {
            JSONObject json = new JSONObject();
            json.put("cmd", "control");
            json.put(deviceType, state);
            
            String jsonString = json.toString();
            Log.d(TAG, "发送控制命令: " + jsonString);
            
            return jsonString.getBytes("UTF-8");
        } catch (Exception e) {
            Log.e(TAG, "构建控制命令失败", e);
            return new byte[0];
        }
    }
    
    /**
     * 构建控制命令（使用ControlCommand对象）
     */
    public static byte[] buildControlCommand(ControlCommand command) {
        String deviceType;
        int state;
        
        switch (command.getCode()) {
            case 0x10: // 风扇关闭
                deviceType = "fan";
                state = 0;
                break;
            case 0x11: // 风扇开启
                deviceType = "fan";
                state = 1;
                break;
            case 0x12: // 风扇自动
                deviceType = "fan";
                state = 2;
                break;
            case 0x20: // 紫外线关闭
                deviceType = "uv";
                state = 0;
                break;
            case 0x21: // 紫外线开启
                deviceType = "uv";
                state = 1;
                break;
            case 0x22: // 紫外线自动
                deviceType = "uv";
                state = 2;
                break;
            case 0x30: // 灯光关闭
                deviceType = "light";
                state = 0;
                break;
            case 0x31: // 灯光开启
                deviceType = "light";
                state = 1;
                break;
            case 0x32: // 灯光自动
                deviceType = "light";
                state = 2;
                break;
            default:
                Log.w(TAG, "未知的控制命令: " + command.getCode());
                return new byte[0];
        }
        
        return buildControlCommand(deviceType, state);
    }
    
    /**
     * 构建心跳包（JSON格式）
     */
    public static byte[] buildHeartbeat() {
        try {
            JSONObject json = new JSONObject();
            json.put("cmd", "heartbeat");
            json.put("timestamp", System.currentTimeMillis());
            
            return json.toString().getBytes("UTF-8");
        } catch (Exception e) {
            Log.e(TAG, "构建心跳包失败", e);
            return new byte[0];
        }
    }
    
    /**
     * 验证JSON数据的完整性
     */
    public static boolean isValidJson(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return false;
        }
        
        try {
            new JSONObject(jsonString);
            return true;
        } catch (JSONException e) {
            return false;
        }
    }
    
    /**
     * 从字节流中提取完整的JSON字符串
     * 支持多个JSON对象连续发送的情况
     */
    public static String extractJsonFromBuffer(StringBuilder buffer) {
        String bufferStr = buffer.toString();
        
        // 查找完整的JSON对象（以 { 开始，} 结束）
        int startIndex = bufferStr.indexOf('{');
        if (startIndex == -1) {
            return null;
        }
        
        int braceCount = 0;
        int endIndex = -1;
        
        for (int i = startIndex; i < bufferStr.length(); i++) {
            char c = bufferStr.charAt(i);
            if (c == '{') {
                braceCount++;
            } else if (c == '}') {
                braceCount--;
                if (braceCount == 0) {
                    endIndex = i;
                    break;
                }
            }
        }
        
        if (endIndex != -1) {
            String json = bufferStr.substring(startIndex, endIndex + 1);
            // 从缓冲区中移除已提取的JSON
            buffer.delete(0, endIndex + 1);
            return json;
        }
        
        return null;
    }
    
    /**
     * 协议异常
     */
    public static class ProtocolException extends Exception {
        public ProtocolException(String message) {
            super(message);
        }
    }
}
