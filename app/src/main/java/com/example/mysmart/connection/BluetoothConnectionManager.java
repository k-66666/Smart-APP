package com.example.mysmart.connection;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.example.mysmart.model.DeviceInfo;
import com.example.mysmart.model.SensorData;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 蓝牙连接管理器实现
 * 支持蓝牙经典模式（SPP协议）
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
    
    // 蓝牙扫描相关
    private ScanCallback scanCallback;
    private final Set<String> foundDevices = new HashSet<>();
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    handleDeviceFound(device);
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                handleScanComplete();
            }
        }
    };
    
    public BluetoothConnectionManager(Context context) {
        this.context = context.getApplicationContext();
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.heartbeatHandler = new Handler(Looper.getMainLooper());
        
        // 注册蓝牙广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        context.registerReceiver(bluetoothReceiver, filter);
    }
    
    /**
     * 处理发现的设备
     */
    private void handleDeviceFound(BluetoothDevice device) {
        try {
            String address = device.getAddress();
            
            // 避免重复添加
            if (foundDevices.contains(address)) {
                return;
            }
            foundDevices.add(address);
            
            String name = device.getName();
            if (name == null || name.isEmpty()) {
                name = "未知设备 (" + address + ")";
            }
            
            DeviceInfo deviceInfo = new DeviceInfo(address, name, "BLUETOOTH");
            
            if (scanCallback != null) {
                mainHandler.post(() -> scanCallback.onDeviceFound(deviceInfo));
            }
            
            Log.d(TAG, "发现蓝牙设备: " + name + " (" + address + ")");
        } catch (SecurityException e) {
            Log.e(TAG, "获取设备信息失败 - 权限不足", e);
        }
    }
    
    /**
     * 处理扫描完成
     */
    private void handleScanComplete() {
        Log.d(TAG, "蓝牙扫描完成，共发现 " + foundDevices.size() + " 个设备");
        
        if (scanCallback != null) {
            mainHandler.post(() -> scanCallback.onScanComplete());
            scanCallback = null;
        }
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
        
        // 检查权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                callback.onError("缺少蓝牙权限，请在应用设置中授予蓝牙权限");
                return;
            }
        } else {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                callback.onError("缺少蓝牙权限，请在应用设置中授予蓝牙和位置权限");
                return;
            }
        }
        
        this.scanCallback = callback;
        foundDevices.clear();
        
        try {
            // 1. 先获取已配对的设备
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            if (pairedDevices != null && !pairedDevices.isEmpty()) {
                for (BluetoothDevice device : pairedDevices) {
                    handleDeviceFound(device);
                }
            }
            
            // 2. 开始扫描新设备
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            
            boolean started = bluetoothAdapter.startDiscovery();
            if (!started) {
                Log.w(TAG, "启动蓝牙扫描失败");
                // 即使扫描失败，也返回已配对的设备
                mainHandler.postDelayed(() -> {
                    if (scanCallback != null) {
                        scanCallback.onScanComplete();
                        scanCallback = null;
                    }
                }, 500);
            } else {
                Log.d(TAG, "开始扫描蓝牙设备...");
                
                // 设置超时（12秒）
                mainHandler.postDelayed(() -> {
                    if (bluetoothAdapter.isDiscovering()) {
                        bluetoothAdapter.cancelDiscovery();
                    }
                }, 12000);
            }
            
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
        try {
            if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
                Log.d(TAG, "停止蓝牙扫描");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "停止扫描失败 - 权限不足", e);
        }
        
        if (scanCallback != null) {
            scanCallback.onScanComplete();
            scanCallback = null;
        }
    }
    
    @Override
    public void connect(DeviceInfo device, ConnectionCallback callback) {
        if (state == ConnectionState.CONNECTED || state == ConnectionState.CONNECTING) {
            callback.onError("已经连接或正在连接中");
            return;
        }
        
        if (bluetoothAdapter == null) {
            callback.onError("设备不支持蓝牙");
            return;
        }
        
        this.connectionCallback = callback;
        this.currentDevice = device;
        setState(ConnectionState.CONNECTING);
        
        new Thread(() -> {
            try {
                // 停止扫描
                if (bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();
                }
                
                BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(device.getDeviceId());
                socket = bluetoothDevice.createRfcommSocketToServiceRecord(SPP_UUID);
                
                // 连接
                socket.connect();
                
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
                
                setState(ConnectionState.CONNECTED);
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onConnected();
                    }
                });
                
                // 启动数据接收线程
                startReceiving();
                
                // 启动心跳
                startHeartbeat();
                
            } catch (SecurityException e) {
                Log.e(TAG, "连接失败 - 权限不足", e);
                cleanupConnection();
                setState(ConnectionState.ERROR);
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError("缺少蓝牙权限，请授予权限后重试");
                    }
                });
            } catch (IOException e) {
                Log.e(TAG, "连接失败", e);
                cleanupConnection();
                setState(ConnectionState.ERROR);
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError("连接失败: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "连接失败 - 未知错误", e);
                cleanupConnection();
                setState(ConnectionState.ERROR);
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError("连接失败: " + e.getMessage());
                    }
                });
            }
        }).start();
    }
    
    @Override
    public void disconnect() {
        stopHeartbeat();
        stopReceiving();
        cleanupConnection();
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
            byte[] buffer = new byte[2048];
            StringBuilder jsonBuffer = new StringBuilder();
            
            while (isReceiving && state == ConnectionState.CONNECTED) {
                try {
                    int bytesRead = inputStream.read(buffer);
                    if (bytesRead > 0) {
                        // 将接收到的字节转换为字符串并添加到缓冲区
                        String received = new String(buffer, 0, bytesRead, "UTF-8");
                        jsonBuffer.append(received);
                        
                        // 尝试从缓冲区提取完整的JSON对象
                        String jsonString;
                        while ((jsonString = ProtocolParser.extractJsonFromBuffer(jsonBuffer)) != null) {
                            Log.d(TAG, "接收到数据: " + jsonString);
                            
                            // 解析JSON数据
                            try {
                                SensorData data = ProtocolParser.parseSensorData(jsonString);
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
                        
                        // 如果缓冲区过大，清理旧数据
                        if (jsonBuffer.length() > 4096) {
                            Log.w(TAG, "缓冲区过大，清理旧数据");
                            jsonBuffer.setLength(0);
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
    
    private void cleanupConnection() {
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
    
    /**
     * 清理所有资源
     */
    public void cleanup() {
        cleanupConnection();
        try {
            context.unregisterReceiver(bluetoothReceiver);
        } catch (Exception e) {
            Log.e(TAG, "注销广播接收器失败", e);
        }
    }
}
