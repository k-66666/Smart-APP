package com.example.mysmart.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.mysmart.connection.BluetoothConnectionManager;
import com.example.mysmart.connection.ConnectionManager;
import com.example.mysmart.connection.ConnectionState;
import com.example.mysmart.connection.ProtocolParser;
import com.example.mysmart.database.AppDatabase;
import com.example.mysmart.database.SensorDataDao;
import com.example.mysmart.model.ControlCommand;
import com.example.mysmart.model.DeviceInfo;
import com.example.mysmart.model.SensorData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 传感器数据仓库
 */
public class SensorRepository {
    
    private static final String TAG = "SensorRepository";
    private static volatile SensorRepository INSTANCE;
    
    private final SensorDataDao sensorDataDao;
    private final ConnectionManager connectionManager;
    private final ConnectionManager wifiConnectionManager;
    private final ExecutorService executorService;
    private final com.example.mysmart.service.AlarmService alarmService;
    
    private final MutableLiveData<SensorData> currentData = new MutableLiveData<>();
    private final MutableLiveData<ConnectionState> connectionState = new MutableLiveData<>(ConnectionState.DISCONNECTED);
    private final MutableLiveData<DeviceInfo> currentDevice = new MutableLiveData<>();
    
    private ConnectionManager activeConnectionManager;
    
    private SensorRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        this.sensorDataDao = database.sensorDataDao();
        this.connectionManager = new BluetoothConnectionManager(context);
        this.wifiConnectionManager = new com.example.mysmart.connection.WifiConnectionManager(context);
        this.executorService = Executors.newFixedThreadPool(2);
        this.activeConnectionManager = connectionManager;
        this.alarmService = new com.example.mysmart.service.AlarmService(context);
        
        setupDataListener();
    }
    
    public static SensorRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (SensorRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SensorRepository(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }
    
    /**
     * 设置数据监听器
     */
    private void setupDataListener() {
        ConnectionManager.DataListener dataListener = new ConnectionManager.DataListener() {
            @Override
            public void onDataReceived(SensorData data) {
                // 更新LiveData
                currentData.postValue(data);
                
                // 检查报警
                alarmService.checkAndAlarm(data);
                
                // 保存到数据库
                executorService.execute(() -> {
                    try {
                        sensorDataDao.insert(data);
                        Log.d(TAG, "数据已保存到数据库");
                    } catch (Exception e) {
                        Log.e(TAG, "保存数据失败", e);
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "数据接收错误: " + error);
            }
        };
        
        connectionManager.setDataListener(dataListener);
        wifiConnectionManager.setDataListener(dataListener);
    }
    
    /**
     * 扫描设备
     */
    public void scanDevices(ConnectionManager.ScanCallback callback) {
        connectionManager.scanDevices(callback);
    }
    
    /**
     * 扫描WiFi设备
     */
    public void scanWifiDevices(ConnectionManager.ScanCallback callback) {
        wifiConnectionManager.scanDevices(callback);
    }
    
    /**
     * 连接设备
     */
    public void connect(DeviceInfo device, ConnectionManager.ConnectionCallback callback) {
        connectionState.postValue(ConnectionState.CONNECTING);
        
        // 根据设备类型选择连接管理器
        if ("WIFI".equals(device.getConnectionType())) {
            activeConnectionManager = wifiConnectionManager;
        } else {
            activeConnectionManager = connectionManager;
        }
        
        activeConnectionManager.connect(device, new ConnectionManager.ConnectionCallback() {
            @Override
            public void onConnected() {
                connectionState.postValue(ConnectionState.CONNECTED);
                currentDevice.postValue(device);
                callback.onConnected();
            }
            
            @Override
            public void onDisconnected() {
                connectionState.postValue(ConnectionState.DISCONNECTED);
                currentDevice.postValue(null);
                callback.onDisconnected();
            }
            
            @Override
            public void onError(String error) {
                connectionState.postValue(ConnectionState.ERROR);
                callback.onError(error);
            }
        });
    }
    
    /**
     * 断开连接
     */
    public void disconnect() {
        activeConnectionManager.disconnect();
        connectionState.postValue(ConnectionState.DISCONNECTED);
        currentDevice.postValue(null);
    }
    
    /**
     * 发送控制命令
     */
    public void sendControlCommand(ControlCommand command, ConnectionManager.ResponseCallback callback) {
        byte[] commandFrame = ProtocolParser.buildControlCommand(command);
        activeConnectionManager.sendCommand(commandFrame, callback);
    }
    
    /**
     * 获取实时数据
     */
    public LiveData<SensorData> getCurrentData() {
        return currentData;
    }
    
    /**
     * 获取连接状态
     */
    public LiveData<ConnectionState> getConnectionState() {
        return connectionState;
    }
    
    /**
     * 获取当前设备
     */
    public LiveData<DeviceInfo> getCurrentDevice() {
        return currentDevice;
    }
    
    /**
     * 获取历史数据
     */
    public void getHistoryData(long startTime, long endTime, DataCallback<List<SensorData>> callback) {
        executorService.execute(() -> {
            try {
                List<SensorData> data = sensorDataDao.getDataInRangeAscending(startTime, endTime);
                callback.onSuccess(data);
            } catch (Exception e) {
                Log.e(TAG, "查询历史数据失败", e);
                callback.onError("查询失败: " + e.getMessage());
            }
        });
    }
    
    /**
     * 清理旧数据
     */
    public void cleanOldData(int retentionDays) {
        executorService.execute(() -> {
            try {
                long cutoffTime = System.currentTimeMillis() - (retentionDays * 24L * 60 * 60 * 1000);
                sensorDataDao.deleteOldData(cutoffTime);
                Log.d(TAG, "已清理" + retentionDays + "天前的数据");
            } catch (Exception e) {
                Log.e(TAG, "清理数据失败", e);
            }
        });
    }
    
    /**
     * 删除所有数据
     */
    public void deleteAllData(DataCallback<Void> callback) {
        executorService.execute(() -> {
            try {
                sensorDataDao.deleteAll();
                callback.onSuccess(null);
            } catch (Exception e) {
                Log.e(TAG, "删除数据失败", e);
                callback.onError("删除失败: " + e.getMessage());
            }
        });
    }
    
    /**
     * 数据回调接口
     */
    public interface DataCallback<T> {
        void onSuccess(T data);
        void onError(String error);
    }
}
