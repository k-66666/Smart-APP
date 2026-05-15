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
 * 监控页面Fragment - 实时监控
 */
public class MonitorFragment extends Fragment {
    
    private MainViewModel viewModel;
    
    // UI组件
    private TextView tvConnectionStatus;
    private Button btnConnect;
    
    private MaterialCardView cardTemperature;
    private MaterialCardView cardHumidity;
    private MaterialCardView cardAirQuality;
    private MaterialCardView cardCO2;
    private MaterialCardView cardAlarmAlert;
    
    private TextView tvTemperature;
    private TextView tvHumidity;
    private TextView tvAirQuality;
    private TextView tvCO2;
    
    private SwitchMaterial switchLight;
    private SwitchMaterial switchUV;
    private SwitchMaterial switchFan;
    
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
        cardCO2 = view.findViewById(R.id.cardCO2);
        cardAlarmAlert = view.findViewById(R.id.cardAlarmAlert);
        
        tvTemperature = view.findViewById(R.id.tvTemperature);
        tvHumidity = view.findViewById(R.id.tvHumidity);
        tvAirQuality = view.findViewById(R.id.tvAirQuality);
        tvCO2 = view.findViewById(R.id.tvCO2);
        
        switchLight = view.findViewById(R.id.switchLight);
        switchUV = view.findViewById(R.id.switchUV);
        switchFan = view.findViewById(R.id.switchFan);
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
        // 映射：Display -> Light, Alarm -> UV/Alert UI, Driver -> Fan
        viewModel.getDisplayModuleOn().observe(getViewLifecycleOwner(), isOn -> {
            switchLight.setChecked(Boolean.TRUE.equals(isOn));
        });
        
        viewModel.getAlarmModuleOn().observe(getViewLifecycleOwner(), isOn -> {
            switchUV.setChecked(Boolean.TRUE.equals(isOn));
            // 自动触发报警 UI 提示
            if (cardAlarmAlert != null) {
                cardAlarmAlert.setVisibility(Boolean.TRUE.equals(isOn) ? View.VISIBLE : View.GONE);
            }
        });
        
        viewModel.getDriverModuleOn().observe(getViewLifecycleOwner(), isOn -> {
            switchFan.setChecked(Boolean.TRUE.equals(isOn));
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
        switchLight.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                viewModel.toggleDisplayModule();
            }
        });
        
        switchUV.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                viewModel.toggleAlarmModule();
            }
        });
        
        switchFan.setOnCheckedChangeListener((buttonView, isChecked) -> {
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
                switchLight.setEnabled(true);
                switchUV.setEnabled(true);
                switchFan.setEnabled(true);
                break;
            case CONNECTING:
                tvConnectionStatus.setText("连接中...");
                tvConnectionStatus.setTextColor(0xFF8E8E93);
                btnConnect.setText("取消");
                btnConnect.setEnabled(true);
                switchLight.setEnabled(false);
                switchUV.setEnabled(false);
                switchFan.setEnabled(false);
                break;
            case ERROR:
                tvConnectionStatus.setText("连接失败");
                tvConnectionStatus.setTextColor(requireContext().getColor(android.R.color.holo_red_dark));
                btnConnect.setText("重试");
                btnConnect.setEnabled(true);
                switchLight.setEnabled(false);
                switchUV.setEnabled(false);
                switchFan.setEnabled(false);
                break;
            default: // DISCONNECTED
                tvConnectionStatus.setText("未连接");
                tvConnectionStatus.setTextColor(0xFF8E8E93);
                btnConnect.setText("连接");
                btnConnect.setEnabled(true);
                switchLight.setEnabled(false);
                switchUV.setEnabled(false);
                switchFan.setEnabled(false);
                break;
        }
    }
    
    private void updateSensorData(SensorData data) {
        // 更新温度
        tvTemperature.setText(String.format("%.1f", data.getTemperature()));
        setCardAccent(cardTemperature, data.getTemperature() < 15 || data.getTemperature() > 35);
        
        // 更新湿度
        tvHumidity.setText(String.format("%.0f", data.getHumidity()));
        setCardAccent(cardHumidity, data.getHumidity() < 20 || data.getHumidity() > 80);
        
        // 更新空气质量
        tvAirQuality.setText(String.format("%d", data.getAirQuality()));
        setCardAccent(cardAirQuality, data.getAirQuality() > 150);
        
        // 更新CO2浓度
        tvCO2.setText(String.format("%d", data.getCo2Concentration()));
        setCardAccent(cardCO2, data.getCo2Concentration() > 1000);
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
