package com.example.mysmart.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.mysmart.connection.ConnectionManager;
import com.example.mysmart.connection.ConnectionState;
import com.example.mysmart.model.ControlCommand;
import com.example.mysmart.model.DeviceInfo;
import com.example.mysmart.model.SensorData;
import com.example.mysmart.repository.SensorRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * 主界面ViewModel
 */
public class MainViewModel extends AndroidViewModel {
    
    private final SensorRepository repository;
    private final MutableLiveData<List<DeviceInfo>> scannedDevices = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isScanning = new MutableLiveData<>(false);
    
    // 控制模块状态
    private final MutableLiveData<Boolean> displayModuleOn = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> alarmModuleOn = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> driverModuleOn = new MutableLiveData<>(false);
    
    public MainViewModel(@NonNull Application application) {
        super(application);
        this.repository = SensorRepository.getInstance(application);
    }
    
    /**
     * 扫描设备
     */
    public void scanDevices() {
        isScanning.setValue(true);
        List<DeviceInfo> devices = new ArrayList<>();
        
        repository.scanDevices(new ConnectionManager.ScanCallback() {
            @Override
            public void onDeviceFound(DeviceInfo device) {
                devices.add(device);
                scannedDevices.postValue(new ArrayList<>(devices));
            }
            
            @Override
            public void onScanComplete() {
                isScanning.postValue(false);
                if (devices.isEmpty()) {
                    errorMessage.postValue("未找到可用设备，请确保蓝牙设备已开启并处于可发现状态");
                }
            }
            
            @Override
            public void onError(String error) {
                isScanning.postValue(false);
                errorMessage.postValue(error);
            }
        });
    }
    
    /**
     * 扫描WiFi设备
     */
    public void scanWifiDevices() {
        isScanning.setValue(true);
        List<DeviceInfo> devices = new ArrayList<>();
        
        repository.scanWifiDevices(new ConnectionManager.ScanCallback() {
            @Override
            public void onDeviceFound(DeviceInfo device) {
                devices.add(device);
                scannedDevices.postValue(new ArrayList<>(devices));
            }
            
            @Override
            public void onScanComplete() {
                isScanning.postValue(false);
                if (devices.isEmpty()) {
                    errorMessage.postValue("未找到可用WiFi设备");
                }
            }
            
            @Override
            public void onError(String error) {
                isScanning.postValue(false);
                errorMessage.postValue(error);
            }
        });
    }
    
    /**
     * 连接设备
     */
    public void connectDevice(DeviceInfo device) {
        repository.connect(device, new ConnectionManager.ConnectionCallback() {
            @Override
            public void onConnected() {
                successMessage.postValue("已连接到 " + device.getDisplayName());
            }
            
            @Override
            public void onDisconnected() {
                successMessage.postValue("已断开连接");
                resetControlStates();
            }
            
            @Override
            public void onError(String error) {
                errorMessage.postValue(error);
            }
        });
    }
    
    /**
     * 断开连接
     */
    public void disconnect() {
        repository.disconnect();
    }
    
    /**
     * 控制显示模块
     */
    public void toggleDisplayModule() {
        boolean newState = !Boolean.TRUE.equals(displayModuleOn.getValue());
        ControlCommand command = newState ? ControlCommand.DISPLAY_ON : ControlCommand.DISPLAY_OFF;
        
        sendControlCommand(command, success -> {
            if (success) {
                displayModuleOn.postValue(newState);
            }
        });
    }
    
    /**
     * 控制声光报警模块
     */
    public void toggleAlarmModule() {
        boolean newState = !Boolean.TRUE.equals(alarmModuleOn.getValue());
        ControlCommand command = newState ? ControlCommand.ALARM_ON : ControlCommand.ALARM_OFF;
        
        sendControlCommand(command, success -> {
            if (success) {
                alarmModuleOn.postValue(newState);
            }
        });
    }
    
    /**
     * 控制驱动模块
     */
    public void toggleDriverModule() {
        boolean newState = !Boolean.TRUE.equals(driverModuleOn.getValue());
        ControlCommand command = newState ? ControlCommand.DRIVER_ON : ControlCommand.DRIVER_OFF;
        
        sendControlCommand(command, success -> {
            if (success) {
                driverModuleOn.postValue(newState);
            }
        });
    }
    
    /**
     * 发送控制命令
     */
    private void sendControlCommand(ControlCommand command, CommandCallback callback) {
        repository.sendControlCommand(command, new ConnectionManager.ResponseCallback() {
            @Override
            public void onSuccess() {
                successMessage.postValue(command.getDescription() + " 成功");
                callback.onResult(true);
            }
            
            @Override
            public void onError(String error) {
                errorMessage.postValue("控制失败: " + error);
                callback.onResult(false);
            }
        });
    }
    
    /**
     * 重置控制状态
     */
    private void resetControlStates() {
        displayModuleOn.postValue(false);
        alarmModuleOn.postValue(false);
        driverModuleOn.postValue(false);
    }
    
    // Getters for LiveData
    public LiveData<SensorData> getCurrentData() {
        return repository.getCurrentData();
    }
    
    public LiveData<ConnectionState> getConnectionState() {
        return repository.getConnectionState();
    }
    
    public LiveData<DeviceInfo> getCurrentDevice() {
        return repository.getCurrentDevice();
    }
    
    public LiveData<List<DeviceInfo>> getScannedDevices() {
        return scannedDevices;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public LiveData<String> getSuccessMessage() {
        return successMessage;
    }
    
    public LiveData<Boolean> getIsScanning() {
        return isScanning;
    }
    
    public LiveData<Boolean> getDisplayModuleOn() {
        return displayModuleOn;
    }
    
    public LiveData<Boolean> getAlarmModuleOn() {
        return alarmModuleOn;
    }
    
    public LiveData<Boolean> getDriverModuleOn() {
        return driverModuleOn;
    }
    
    /**
     * 命令回调接口
     */
    private interface CommandCallback {
        void onResult(boolean success);
    }
}
