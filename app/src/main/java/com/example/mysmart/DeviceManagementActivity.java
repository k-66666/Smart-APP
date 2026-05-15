package com.example.mysmart;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mysmart.adapter.DeviceAdapter;
import com.example.mysmart.database.AppDatabase;
import com.example.mysmart.database.DeviceInfoDao;
import com.example.mysmart.model.DeviceInfo;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

/**
 * 设备管理Activity
 */
public class DeviceManagementActivity extends AppCompatActivity {
    
    private DeviceInfoDao deviceInfoDao;
    private RecyclerView recyclerView;
    private DeviceAdapter adapter;
    private List<DeviceInfo> deviceList = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_management);
        
        deviceInfoDao = AppDatabase.getInstance(this).deviceInfoDao();
        
        initViews();
        loadDevices();
    }
    
    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        
        recyclerView = findViewById(R.id.recyclerViewDevices);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        adapter = new DeviceAdapter(deviceList, new DeviceAdapter.OnDeviceActionListener() {
            @Override
            public void onConnect(DeviceInfo device) {
                Toast.makeText(DeviceManagementActivity.this, 
                    "连接到 " + device.getDisplayName(), Toast.LENGTH_SHORT).show();
                // TODO: 实现连接逻辑
            }
            
            @Override
            public void onDelete(DeviceInfo device) {
                deleteDevice(device);
            }
            
            @Override
            public void onEdit(DeviceInfo device) {
                // TODO: 实现编辑设备别名
                Toast.makeText(DeviceManagementActivity.this, 
                    "编辑功能开发中", Toast.LENGTH_SHORT).show();
            }
        });
        
        recyclerView.setAdapter(adapter);
    }
    
    private void loadDevices() {
        new Thread(() -> {
            List<DeviceInfo> devices = deviceInfoDao.getAllDevices();
            runOnUiThread(() -> {
                deviceList.clear();
                deviceList.addAll(devices);
                adapter.notifyDataSetChanged();
                
                if (devices.isEmpty()) {
                    Toast.makeText(this, "暂无已保存的设备", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
    
    private void deleteDevice(DeviceInfo device) {
        new Thread(() -> {
            deviceInfoDao.delete(device);
            runOnUiThread(() -> {
                deviceList.remove(device);
                adapter.notifyDataSetChanged();
                Toast.makeText(this, "设备已删除", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }
}
