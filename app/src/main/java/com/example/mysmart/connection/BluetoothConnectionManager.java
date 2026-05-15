package com.example.mysmart.connection;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.example.mysmart.model.DeviceInfo;
import com.example.mysmart.model.SensorData;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

/**
 * 蓝牙连接管理器实现
 */
public class BluetoothConnectionManager implements ConnectionManager {
    
    private static final String TAG = "BluetoothConnMgr";
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int HEARTBEAT_INTERVAL = 30000; // 30秒
    
    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private final Handler mainHandler;
    private final Handler heartbeatHandler;
    
    private ConnectionState state = ConnectionState.DISCONNECTED;
    private BluetoothSocket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private DeviceInfo currentDevice;
    private DataListener dataListener;
    private ConnectionCallback connectionCallback;
    
    private Thread receiveThread;
    private volatile boolean isReceiving = false;
    
    public BluetoothConnectionManager(Context context) {
        this.context = context.getApplicationContext();
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.heartbeatHandler = new Handler(Looper.getMainLooper());
    }
    
    @Override
    public void scanDevices(ScanCallback callback) {
        if (bluetoothAdapter == null) {
            callback.onError("设备不支持蓝牙");
            return;
        }
        
        if (!bluetoothAdapter.isEnabled()) {
            callback.onError("蓝牙未开启，请在系统设置中启用蓝牙");
            return;
        }
        
        try {
            // 获取已配对的设备
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            
            if (pairedDevices.isEmpty()) {
                callback.onScanComplete();
                return;
            }
            
            for (BluetoothDevice device : pairedDevices) {
                String name = device.getName();
                String address = device.getAddress();
                
                DeviceInfo deviceInfo = new DeviceInfo(address, name, "BLUETOOTH");
                callback.onDeviceFound(deviceInfo);
            }
            
            callback.onScanComplete();
        } catch (SecurityException e) {
            Log.e(TAG, "扫描设备失败 - 权限不足", e);
            callback.onError("缺少蓝牙权限，请授予权限后重试");
        } catch (Exception e) {
            Log.e(TAG, "扫描设备失败", e);
            callback.onError("扫描失败: " + e.getMessage());
        }
    }
    
    @Override
    public void stopScan() {
        // 蓝牙经典模式不需要停止扫描
    }
    
    @Override
    public void connect(DeviceInfo device, ConnectionCallback callback) {
        if (state == ConnectionState.CONNECTED || state == ConnectionState.CONNECTING) {
            callback.onError("已经连接或正在连接中");
            return;
        }
        
        this.connectionCallback = callback;
        this.currentDevice = device;
        setState(ConnectionState.CONNECTING);
        
        new Thread(() -> {
            try {
                BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(device.getDeviceId());
                socket = bluetoothDevice.createRfcommSocketToServiceRecord(SPP_UUID);
                
                // 连接
                socket.connect();
                
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
                
                setState(ConnectionState.CONNECTED);
                mainHandler.post(() -> callback.onConnected());
                
                // 启动数据接收线程
                startReceiving();
                
                // 启动心跳
                startHeartbeat();
                
            } catch (SecurityException e) {
                Log.e(TAG, "连接失败 - 权限不足", e);
                notifyError("缺少蓝牙权限，请授予权限后重试");
            } catch (IOException e) {
                Log.e(TAG, "连接失败", e);
                notifyError("连接失败: " + e.getMessage());
                cleanup();
            }
        }).start();
    }
    
    @Override
    public void disconnect() {
        stopHeartbeat();
        stopReceiving();
        cleanup();
        setState(ConnectionState.DISCONNECTED);
        
        if (connectionCallback != null) {
            mainHandler.post(() -> connectionCallback.onDisconnected());
        }
    }
    
    @Override
    public void sendCommand(byte[] command, ResponseCallback callback) {
        if (state != ConnectionState.CONNECTED) {
            if (callback != null) {
                callback.onError("未连接");
            }
            return;
        }
        
        new Thread(() -> {
            try {
                outputStream.write(command);
                outputStream.flush();
                
                if (callback != null) {
                    mainHandler.post(callback::onSuccess);
                }
            } catch (IOException e) {
                Log.e(TAG, "发送命令失败", e);
                if (callback != null) {
                    mainHandler.post(() -> callback.onError("发送失败: " + e.getMessage()));
                }
            }
        }).start();
    }
    
    @Override
    public void setDataListener(DataListener listener) {
        this.dataListener = listener;
    }
    
    @Override
    public ConnectionState getState() {
        return state;
    }
    
    @Override
    public DeviceInfo getCurrentDevice() {
        return currentDevice;
    }
    
    private void setState(ConnectionState newState) {
        this.state = newState;
        Log.d(TAG, "连接状态: " + newState.getDescription());
    }
    
    private void startReceiving() {
        isReceiving = true;
        receiveThread = new Thread(() -> {
            byte[] buffer = new byte[1024];
            
            while (isReceiving && state == ConnectionState.CONNECTED) {
                try {
                    int bytesRead = inputStream.read(buffer);
                    if (bytesRead > 0) {
                        byte[] frame = new byte[bytesRead];
                        System.arraycopy(buffer, 0, frame, 0, bytesRead);
                        
                        // 解析数据
                        try {
                            SensorData data = ProtocolParser.parseSensorData(frame);
                            if (dataListener != null) {
                                mainHandler.post(() -> dataListener.onDataReceived(data));
                            }
                        } catch (ProtocolParser.ProtocolException e) {
                            Log.e(TAG, "数据解析失败", e);
                            if (dataListener != null) {
                                mainHandler.post(() -> dataListener.onError("数据解析失败: " + e.getMessage()));
                            }
                        }
                    }
                } catch (IOException e) {
                    if (isReceiving) {
                        Log.e(TAG, "接收数据失败", e);
                        notifyError("连接断开");
                        disconnect();
                    }
                    break;
                }
            }
        });
        receiveThread.start();
    }
    
    private void stopReceiving() {
        isReceiving = false;
        if (receiveThread != null) {
            receiveThread.interrupt();
            receiveThread = null;
        }
    }
    
    private void startHeartbeat() {
        heartbeatHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (state == ConnectionState.CONNECTED) {
                    byte[] heartbeat = ProtocolParser.buildHeartbeat();
                    sendCommand(heartbeat, null);
                    heartbeatHandler.postDelayed(this, HEARTBEAT_INTERVAL);
                }
            }
        }, HEARTBEAT_INTERVAL);
    }
    
    private void stopHeartbeat() {
        heartbeatHandler.removeCallbacksAndMessages(null);
    }
    
    private void cleanup() {
        try {
            if (inputStream != null) {
                inputStream.close();
                inputStream = null;
            }
            if (outputStream != null) {
                outputStream.close();
                outputStream = null;
            }
            if (socket != null) {
                socket.close();
                socket = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "清理资源失败", e);
        }
    }
    
    private void notifyError(String error) {
        setState(ConnectionState.ERROR);
        if (connectionCallback != null) {
            mainHandler.post(() -> connectionCallback.onError(error));
        }
    }
}
