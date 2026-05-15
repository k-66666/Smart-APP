package com.example.mysmart.connection;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.mysmart.model.DeviceInfo;
import com.example.mysmart.model.SensorData;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * WiFi连接管理器实现
 */
public class WifiConnectionManager implements ConnectionManager {
    
    private static final String TAG = "WifiConnMgr";
    private static final int DEFAULT_PORT = 8888;
    private static final int CONNECT_TIMEOUT = 5000;
    private static final int HEARTBEAT_INTERVAL = 30000;
    
    private final Context context;
    private final Handler mainHandler;
    private final Handler heartbeatHandler;
    
    private ConnectionState state = ConnectionState.DISCONNECTED;
    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private DeviceInfo currentDevice;
    private DataListener dataListener;
    private ConnectionCallback connectionCallback;
    
    private Thread receiveThread;
    private volatile boolean isReceiving = false;
    
    public WifiConnectionManager(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.heartbeatHandler = new Handler(Looper.getMainLooper());
    }
    
    @Override
    public void scanDevices(ScanCallback callback) {
        // WiFi设备扫描 - 简化版本，扫描局域网内的设备
        new Thread(() -> {
            try {
                WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                        .getSystemService(Context.WIFI_SERVICE);
                
                if (wifiManager == null || !wifiManager.isWifiEnabled()) {
                    mainHandler.post(() -> callback.onError("WiFi未开启，请在系统设置中启用WiFi"));
                    return;
                }
                
                // 获取当前WiFi信息
                String localIp = getLocalIpAddress(wifiManager);
                if (localIp == null) {
                    mainHandler.post(() -> callback.onError("未连接到WiFi网络"));
                    return;
                }
                
                // 扫描局域网内的设备（简化版本 - 扫描常用IP段）
                String subnet = localIp.substring(0, localIp.lastIndexOf("."));
                
                // 扫描常见的设备IP
                String[] commonIps = {
                    subnet + ".100",
                    subnet + ".101",
                    subnet + ".102",
                    subnet + ".200",
                    subnet + ".201"
                };
                
                for (String ip : commonIps) {
                    if (isDeviceReachable(ip, DEFAULT_PORT)) {
                        DeviceInfo device = new DeviceInfo(ip, "WiFi设备 (" + ip + ")", "WIFI");
                        mainHandler.post(() -> callback.onDeviceFound(device));
                    }
                }
                
                mainHandler.post(callback::onScanComplete);
                
            } catch (Exception e) {
                Log.e(TAG, "WiFi扫描失败", e);
                mainHandler.post(() -> callback.onError("扫描失败: " + e.getMessage()));
            }
        }).start();
    }
    
    @Override
    public void stopScan() {
        // WiFi扫描停止
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
                socket = new Socket();
                socket.connect(new InetSocketAddress(device.getDeviceId(), DEFAULT_PORT), CONNECT_TIMEOUT);
                
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
                
                setState(ConnectionState.CONNECTED);
                mainHandler.post(() -> callback.onConnected());
                
                // 启动数据接收线程
                startReceiving();
                
                // 启动心跳
                startHeartbeat();
                
            } catch (IOException e) {
                Log.e(TAG, "WiFi连接失败", e);
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
    
    private String getLocalIpAddress(WifiManager wifiManager) {
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
        if (ipAddress == 0) return null;
        
        return String.format("%d.%d.%d.%d",
                (ipAddress & 0xff),
                (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff),
                (ipAddress >> 24 & 0xff));
    }
    
    private boolean isDeviceReachable(String ip, int port) {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), 1000);
            socket.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
