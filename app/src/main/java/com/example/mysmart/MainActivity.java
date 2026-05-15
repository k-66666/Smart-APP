package com.example.mysmart;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
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
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels * 0.9), android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        
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
        showScanDialog("蓝牙设备扫描", "正在搜索已配对的蓝牙设备...", true);
    }
    
    private void scanWifiDevices() {
        viewModel.scanWifiDevices();
        showScanDialog("WiFi 设备扫描", "正在扫描局域网设备...", false);
    }
    
    private void showScanDialog(String title, String subtitle, boolean isBluetooth) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_scan);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels * 0.9), android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        
        TextView tvTitle = dialog.findViewById(R.id.tvScanDialogTitle);
        TextView tvSubtitle = dialog.findViewById(R.id.tvScanDialogSubtitle);
        android.widget.ProgressBar progress = dialog.findViewById(R.id.progressScan);
        androidx.recyclerview.widget.RecyclerView rvDevices = dialog.findViewById(R.id.rvScanDevices);
        android.widget.LinearLayout layoutEmpty = dialog.findViewById(R.id.layoutEmpty);
        android.widget.ImageView ivIcon = dialog.findViewById(R.id.ivScanIcon);
        
        if (tvTitle != null) tvTitle.setText(title);
        if (tvSubtitle != null) tvSubtitle.setText(subtitle);
        
        if (!isBluetooth) {
            ivIcon.setImageResource(android.R.drawable.ic_menu_sort_by_size);
            ivIcon.setColorFilter(0xFF30D158);
        }
        
        rvDevices.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        com.example.mysmart.adapter.ScanDeviceAdapter adapter = new com.example.mysmart.adapter.ScanDeviceAdapter(device -> {
            dialog.dismiss();
            viewModel.connectDevice(device);
        });
        rvDevices.setAdapter(adapter);
        
        viewModel.getScannedDevices().observe(this, devices -> {
            if (devices != null && !devices.isEmpty()) {
                progress.setVisibility(android.view.View.GONE);
                layoutEmpty.setVisibility(android.view.View.GONE);
                rvDevices.setVisibility(android.view.View.VISIBLE);
                if (tvSubtitle != null) tvSubtitle.setText("发现 " + devices.size() + " 个设备");
                adapter.setDevices(devices);
            }
        });
        
        viewModel.getIsScanning().observe(this, isScanning -> {
            if (Boolean.FALSE.equals(isScanning)) {
                progress.setVisibility(android.view.View.GONE);
                if (adapter.getItemCount() == 0) {
                    layoutEmpty.setVisibility(android.view.View.VISIBLE);
                    if (tvSubtitle != null) tvSubtitle.setText("扫描完成");
                }
            }
        });
        
        dialog.findViewById(R.id.btnCancelScan).setOnClickListener(v -> dialog.dismiss());
        
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