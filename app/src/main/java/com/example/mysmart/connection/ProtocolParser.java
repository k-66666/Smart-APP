package com.example.mysmart.connection;

import com.example.mysmart.model.ControlCommand;
import com.example.mysmart.model.SensorData;

/**
 * 通信协议解析器
 * 
 * 数据帧格式：
 * [帧头(2字节)] [数据长度(1字节)] [命令类型(1字节)] [数据(N字节)] [校验和(1字节)]
 * 
 * 帧头：0xAA 0x55
 * 命令类型：
 *   0x01 - 传感器数据
 *   0x02 - 控制命令
 *   0x03 - 心跳包
 *   0x04 - 确认应答
 * 校验和：所有字节异或
 */
public class ProtocolParser {
    
    // 帧头
    private static final byte FRAME_HEADER_1 = (byte) 0xAA;
    private static final byte FRAME_HEADER_2 = (byte) 0x55;
    
    // 命令类型
    private static final byte CMD_SENSOR_DATA = 0x01;
    private static final byte CMD_CONTROL = 0x02;
    private static final byte CMD_HEARTBEAT = 0x03;
    private static final byte CMD_ACK = 0x04;
    
    // 最小帧长度（帧头2 + 长度1 + 命令1 + 校验1）
    private static final int MIN_FRAME_LENGTH = 5;
    
    /**
     * 解析传感器数据帧
     * 数据格式：温度(4字节float) + 湿度(4字节float) + 空气质量(4字节int) + 光照强度(4字节int)
     */
    public static SensorData parseSensorData(byte[] frame) throws ProtocolException {
        if (!validateFrame(frame)) {
            throw new ProtocolException("帧校验失败");
        }
        
        if (frame[3] != CMD_SENSOR_DATA) {
            throw new ProtocolException("不是传感器数据帧");
        }
        
        int dataLength = frame[2] & 0xFF;
        if (dataLength != 16) { // 4个float/int，每个4字节
            throw new ProtocolException("数据长度不正确");
        }
        
        try {
            // 解析数据
            float temperature = bytesToFloat(frame, 4);
            float humidity = bytesToFloat(frame, 8);
            int airQuality = bytesToInt(frame, 12);
            int lightIntensity = bytesToInt(frame, 16);
            
            // 验证数据范围
            if (temperature < -40 || temperature > 80) {
                throw new ProtocolException("温度值超出范围");
            }
            if (humidity < 0 || humidity > 100) {
                throw new ProtocolException("湿度值超出范围");
            }
            if (airQuality < 0 || airQuality > 500) {
                throw new ProtocolException("空气质量值超出范围");
            }
            if (lightIntensity < 0 || lightIntensity > 100000) {
                throw new ProtocolException("光照强度值超出范围");
            }
            
            return new SensorData(temperature, humidity, airQuality, lightIntensity);
        } catch (Exception e) {
            throw new ProtocolException("数据解析失败: " + e.getMessage());
        }
    }
    
    /**
     * 构建控制命令帧
     */
    public static byte[] buildControlCommand(ControlCommand command) {
        byte[] frame = new byte[6]; // 帧头2 + 长度1 + 命令1 + 控制码1 + 校验1
        
        frame[0] = FRAME_HEADER_1;
        frame[1] = FRAME_HEADER_2;
        frame[2] = 1; // 数据长度
        frame[3] = CMD_CONTROL;
        frame[4] = (byte) command.getCode();
        frame[5] = calculateChecksum(frame, 5);
        
        return frame;
    }
    
    /**
     * 构建心跳包
     */
    public static byte[] buildHeartbeat() {
        byte[] frame = new byte[5]; // 帧头2 + 长度1 + 命令1 + 校验1
        
        frame[0] = FRAME_HEADER_1;
        frame[1] = FRAME_HEADER_2;
        frame[2] = 0; // 数据长度
        frame[3] = CMD_HEARTBEAT;
        frame[4] = calculateChecksum(frame, 4);
        
        return frame;
    }
    
    /**
     * 验证帧的完整性
     */
    private static boolean validateFrame(byte[] frame) {
        if (frame == null || frame.length < MIN_FRAME_LENGTH) {
            return false;
        }
        
        // 检查帧头
        if (frame[0] != FRAME_HEADER_1 || frame[1] != FRAME_HEADER_2) {
            return false;
        }
        
        // 检查长度
        int dataLength = frame[2] & 0xFF;
        int expectedLength = MIN_FRAME_LENGTH + dataLength;
        if (frame.length != expectedLength) {
            return false;
        }
        
        // 检查校验和
        byte expectedChecksum = calculateChecksum(frame, frame.length - 1);
        return frame[frame.length - 1] == expectedChecksum;
    }
    
    /**
     * 计算校验和（异或）
     */
    private static byte calculateChecksum(byte[] data, int length) {
        byte checksum = 0;
        for (int i = 0; i < length; i++) {
            checksum ^= data[i];
        }
        return checksum;
    }
    
    /**
     * 字节数组转float（大端序）
     */
    private static float bytesToFloat(byte[] bytes, int offset) {
        int intBits = bytesToInt(bytes, offset);
        return Float.intBitsToFloat(intBits);
    }
    
    /**
     * 字节数组转int（大端序）
     */
    private static int bytesToInt(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24) |
               ((bytes[offset + 1] & 0xFF) << 16) |
               ((bytes[offset + 2] & 0xFF) << 8) |
               (bytes[offset + 3] & 0xFF);
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
