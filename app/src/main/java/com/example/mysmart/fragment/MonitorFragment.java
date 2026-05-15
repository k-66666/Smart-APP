package com.example.mysmart.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.mysmart.R;
import com.example.mysmart.connection.ConnectionState;
import com.example.mysmart.model.SensorData;
import com.example.mysmart.viewmodel.MainViewModel;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;

/**
 * 监控页面Fragment
 */
public class MonitorFragment extends Fragment {
    
    private MainViewModel viewModel;
    
    // UI组件
    private TextView tvConnectionStatus;
    private Button btnConnect;
    private MaterialCardView cardTemperature;
    private MaterialCardView cardHumidity;
    private MaterialCardView cardAirQuality;
    private MaterialCardView cardLightIntensity;
    private TextView tvTemperature;
    private TextView tvHumidity;
    private TextView tvAirQuality;
    private TextView tvLightIntensity;
    private SwitchMaterial switchDisplay;
    private SwitchMaterial switchAlarm;
    private SwitchMaterial switchDriver;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_monitor, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        
        initViews(view);
        setupObservers();
        setupListeners();
    }
    
    private void initViews(View view) {
        tvConnectionStatus = view.findViewById(R.id.tvConnectionStatus);
        btnConnect = view.findViewById(R.id.btnConnect);
        
        cardTemperature = view.findViewById(R.id.cardTemperature);
        cardHumidity = view.findViewById(R.id.cardHumidity);
        cardAirQuality = view.findViewById(R.id.cardAirQuality);
        cardLightIntensity = view.findViewById(R.id.cardLightIntensity);
        
        tvTemperature = view.findViewById(R.id.tvTemperature);
        tvHumidity = view.findViewById(R.id.tvHumidity);
        tvAirQuality = view.findViewById(R.id.tvAirQuality);
        tvLightIntensity = view.findViewById(R.id.tvLightIntensity);
        
        switchDisplay = view.findViewById(R.id.switchDisplay);
        switchAlarm = view.findViewById(R.id.switchAlarm);
        switchDriver = view.findViewById(R.id.switchDriver);
    }
    
    private void setupObservers() {
        // 观察连接状态
        viewModel.getConnectionState().observe(getViewLifecycleOwner(), state -> {
            updateConnectionUI(state);
        });
        
        // 观察传感器数据
        viewModel.getCurrentData().observe(getViewLifecycleOwner(), data -> {
            if (data != null) {
                updateSensorData(data);
            }
        });
        
        // 观察错误消息
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
        
        // 观察控制模块状态
        viewModel.getDisplayModuleOn().observe(getViewLifecycleOwner(), isOn -> {
            switchDisplay.setChecked(Boolean.TRUE.equals(isOn));
        });
        
        viewModel.getAlarmModuleOn().observe(getViewLifecycleOwner(), isOn -> {
            switchAlarm.setChecked(Boolean.TRUE.equals(isOn));
        });
        
        viewModel.getDriverModuleOn().observe(getViewLifecycleOwner(), isOn -> {
            switchDriver.setChecked(Boolean.TRUE.equals(isOn));
        });
    }
    
    private void setupListeners() {
        btnConnect.setOnClickListener(v -> {
            ConnectionState curState = viewModel.getConnectionState().getValue();
            if (curState == ConnectionState.CONNECTED) {
                viewModel.disconnect();
            } else {
                ((com.example.mysmart.MainActivity) requireActivity()).showDeviceSelectionDialog();
            }
        });
        
        // 控制开关监听
        switchDisplay.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                viewModel.toggleDisplayModule();
            }
        });
        
        switchAlarm.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                viewModel.toggleAlarmModule();
            }
        });
        
        switchDriver.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                viewModel.toggleDriverModule();
            }
        });
    }
    
    private void updateConnectionUI(ConnectionState state) {
        switch (state) {
            case CONNECTED:
                tvConnectionStatus.setText("已连接");
                tvConnectionStatus.setTextColor(requireContext().getColor(android.R.color.holo_green_dark));
                btnConnect.setText("断开");
                btnConnect.setEnabled(true);
                switchDisplay.setEnabled(true);
                switchAlarm.setEnabled(true);
                switchDriver.setEnabled(true);
                break;
            case CONNECTING:
                tvConnectionStatus.setText("连接中...");
                tvConnectionStatus.setTextColor(0xFF8E8E93);
                btnConnect.setText("取消");
                btnConnect.setEnabled(true);
                switchDisplay.setEnabled(false);
                switchAlarm.setEnabled(false);
                switchDriver.setEnabled(false);
                break;
            case ERROR:
                tvConnectionStatus.setText("连接失败");
                tvConnectionStatus.setTextColor(requireContext().getColor(android.R.color.holo_red_dark));
                btnConnect.setText("重试");
                btnConnect.setEnabled(true);
                switchDisplay.setEnabled(false);
                switchAlarm.setEnabled(false);
                switchDriver.setEnabled(false);
                break;
            default: // DISCONNECTED
                tvConnectionStatus.setText("未连接");
                tvConnectionStatus.setTextColor(0xFF8E8E93);
                btnConnect.setText("连接");
                btnConnect.setEnabled(true);
                switchDisplay.setEnabled(false);
                switchAlarm.setEnabled(false);
                switchDriver.setEnabled(false);
                break;
        }
    }
    
    private void updateSensorData(SensorData data) {
        // 更新温度
        tvTemperature.setText(String.format("%.1f°C", data.getTemperature()));
        setCardAccent(cardTemperature, data.getTemperature() < 15 || data.getTemperature() > 35);
        
        // 更新湿度
        tvHumidity.setText(String.format("%.0f%%", data.getHumidity()));
        setCardAccent(cardHumidity, data.getHumidity() < 20 || data.getHumidity() > 80);
        
        // 更新空气质量
        tvAirQuality.setText(String.format("%d", data.getAirQuality()));
        setCardAccent(cardAirQuality, data.getAirQuality() > 150);
        
        // 更新光照强度
        tvLightIntensity.setText(String.format("%d", data.getLightIntensity()));
        setCardAccent(cardLightIntensity, false);
    }
    
    private void setCardAccent(MaterialCardView card, boolean isAlert) {
        if (isAlert) {
            card.setCardBackgroundColor(0xFFFFF2F2); // 淡红色警告背景
            card.setStrokeColor(0xFFFF3B30);
            card.setStrokeWidth(1);
        } else {
            card.setCardBackgroundColor(0xFFFFFFFF);
            card.setStrokeWidth(0);
        }
    }
}
