package com.example.mysmart.connection;

import com.example.mysmart.model.DeviceInfo;
import com.example.mysmart.model.SensorData;

/**
 * 连接管理器接口
 */
public interface ConnectionManager {
    
    /**
     * 扫描可用设备
     */
    void scanDevices(ScanCallback callback);
    
    /**
     * 停止扫描
     */
    void stopScan();
    
    /**
     * 连接到设备
     */
    void connect(DeviceInfo device, ConnectionCallback callback);
    
    /**
     * 断开连接
     */
    void disconnect();
    
    /**
     * 发送控制命令
     */
    void sendCommand(byte[] command, ResponseCallback callback);
    
    /**
     * 设置数据监听器
     */
    void setDataListener(DataListener listener);
    
    /**
     * 获取当前连接状态
     */
    ConnectionState getState();
    
    /**
     * 获取当前连接的设备
     */
    DeviceInfo getCurrentDevice();
    
    /**
     * 扫描回调
     */
    interface ScanCallback {
        void onDeviceFound(DeviceInfo device);
        void onScanComplete();
        void onError(String error);
    }
    
    /**
     * 连接回调
     */
    interface ConnectionCallback {
        void onConnected();
        void onDisconnected();
        void onError(String error);
    }
    
    /**
     * 响应回调
     */
    interface ResponseCallback {
        void onSuccess();
        void onError(String error);
    }
    
    /**
     * 数据监听器
     */
    interface DataListener {
        void onDataReceived(SensorData data);
        void onError(String error);
    }
}
