package com.example.mysmart.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mysmart.AlarmThresholdActivity;
import com.example.mysmart.DeviceManagementActivity;
import com.example.mysmart.R;
import com.example.mysmart.SettingsActivity;

/**
 * 我的页面Fragment
 */
public class ProfileFragment extends Fragment {
    
    private LinearLayout layoutAlarmThreshold;
    private LinearLayout layoutDeviceManagement;
    private LinearLayout layoutSettings;
    private LinearLayout layoutAbout;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupListeners();
    }
    
    private void initViews(View view) {
        layoutAlarmThreshold = view.findViewById(R.id.layoutAlarmThreshold);
        layoutDeviceManagement = view.findViewById(R.id.layoutDeviceManagement);
        layoutSettings = view.findViewById(R.id.layoutSettings);
        layoutAbout = view.findViewById(R.id.layoutAbout);
    }
    
    private void setupListeners() {
        layoutAlarmThreshold.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AlarmThresholdActivity.class);
            startActivity(intent);
        });
        
        layoutDeviceManagement.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), DeviceManagementActivity.class);
            startActivity(intent);
        });
        
        layoutSettings.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), SettingsActivity.class);
            startActivity(intent);
        });
        
        layoutAbout.setOnClickListener(v -> {
            // TODO: 显示关于页面
        });
    }
}
