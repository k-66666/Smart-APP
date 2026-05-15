package com.example.mysmart;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.mysmart.fragment.DataFragment;
import com.example.mysmart.fragment.MonitorFragment;
import com.example.mysmart.fragment.ProfileFragment;
import com.example.mysmart.model.DeviceInfo;
import com.example.mysmart.viewmodel.MainViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    
    private static final int PERMISSION_REQUEST_CODE = 1001;
    
    private MainViewModel viewModel;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 初始化ViewModel
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        
        // 初始化底部导航
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        setupBottomNavigation();
        
        // 玻璃悬浮效果 (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            com.google.android.material.card.MaterialCardView dock = findViewById(R.id.bottomNavCard);
            if (dock != null) {
                // 降低模糊半径，防止内容糊掉
                dock.setRenderEffect(android.graphics.RenderEffect.createBlurEffect(16f, 16f, android.graphics.Shader.TileMode.CLAMP));
                dock.setCardBackgroundColor(getResources().getColor(R.color.glass_bg_light, getTheme()));
            }
        }
        
        // 默认显示监控页面
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new MonitorFragment())
                    .commit();
        }
        
        // 请求权限
        requestPermissions();
    }
    
    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_monitor) {
                selectedFragment = new MonitorFragment();
            } else if (itemId == R.id.nav_data) {
                selectedFragment = new DataFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }
            
            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, selectedFragment)
                        .commit();
                return true;
            }
            return false;
        });
    }
    
    public void showDeviceSelectionDialog() {
        if (!checkBluetoothPermissions()) {
            Toast.makeText(this, "请先授予权限", Toast.LENGTH_SHORT).show();
            return;
        }
        
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_connection);
        dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels * 0.9), android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        
        dialog.findViewById(R.id.cardBluetooth).setOnClickListener(v -> {
            dialog.dismiss();
            scanBluetoothDevices();
        });
        
        dialog.findViewById(R.id.cardWifiScan).setOnClickListener(v -> {
            dialog.dismiss();
            scanWifiDevices();
        });
        
        dialog.findViewById(R.id.cardManualInput).setOnClickListener(v -> {
            dialog.dismiss();
            showManualWifiDialog();
        });
        
        dialog.show();
    }
    
    private void showManualWifiDialog() {
        android.widget.EditText editText = new android.widget.EditText(this);
        editText.setHint("输入 IP 或输入 'demo' 体验模拟数据");
        editText.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        
        new AlertDialog.Builder(this)
            .setTitle("输入设备 IP 地址")
            .setView(editText)
            .setPositiveButton("连接", (dialog, which) -> {
                String ip = editText.getText().toString().trim();
                if (!ip.isEmpty()) {
                    DeviceInfo device = new DeviceInfo(ip, "WiFi设备 (" + ip + ")", "WIFI");
                    viewModel.connectDevice(device);
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    private boolean checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) 
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) 
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    
    private void scanBluetoothDevices() {
        viewModel.scanDevices();
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择蓝牙设备");
        builder.setMessage("正在扫描蓝牙设备...");
        
        AlertDialog dialog = builder.create();
        
        viewModel.getScannedDevices().observe(this, devices -> {
            if (devices != null && !devices.isEmpty()) {
                String[] deviceNames = new String[devices.size()];
                for (int i = 0; i < devices.size(); i++) {
                    deviceNames[i] = devices.get(i).getDisplayName();
                }
                
                dialog.dismiss();
                
                new AlertDialog.Builder(this)
                    .setTitle("选择设备")
                    .setItems(deviceNames, (dialogInterface, which) -> {
                        DeviceInfo selectedDevice = devices.get(which);
                        viewModel.connectDevice(selectedDevice);
                    })
                    .setNegativeButton("取消", null)
                    .show();
            }
        });
        
        viewModel.getIsScanning().observe(this, isScanning -> {
            if (Boolean.FALSE.equals(isScanning)) {
                dialog.dismiss();
            }
        });
        
        dialog.show();
    }
    
    private void scanWifiDevices() {
        viewModel.scanWifiDevices();
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择WiFi设备");
        builder.setMessage("正在扫描WiFi设备...");
        
        AlertDialog dialog = builder.create();
        
        viewModel.getScannedDevices().observe(this, devices -> {
            if (devices != null && !devices.isEmpty()) {
                String[] deviceNames = new String[devices.size()];
                for (int i = 0; i < devices.size(); i++) {
                    deviceNames[i] = devices.get(i).getDisplayName();
                }
                
                dialog.dismiss();
                
                new AlertDialog.Builder(this)
                    .setTitle("选择设备")
                    .setItems(deviceNames, (dialogInterface, which) -> {
                        DeviceInfo selectedDevice = devices.get(which);
                        viewModel.connectDevice(selectedDevice);
                    })
                    .setNegativeButton("取消", null)
                    .show();
            }
        });
        
        viewModel.getIsScanning().observe(this, isScanning -> {
            if (Boolean.FALSE.equals(isScanning)) {
                dialog.dismiss();
            }
        });
        
        dialog.show();
    }
    
    private void requestPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();
        
        // 蓝牙权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) 
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) 
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN);
            }
        }
        
        // 位置权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        
        // 通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
        
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, 
                permissionsToRequest.toArray(new String[0]), 
                PERMISSION_REQUEST_CODE);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (!allGranted) {
                Toast.makeText(this, "需要授予权限才能使用蓝牙功能", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewModel.disconnect();
    }
}