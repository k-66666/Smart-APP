package com.example.mysmart;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.switchmaterial.SwitchMaterial;

/**
 * 设置页面
 */
public class SettingsActivity extends AppCompatActivity {
    
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    
    // UI组件
    private Spinner spinnerRefreshInterval;
    private Spinner spinnerTemperatureUnit;
    private Spinner spinnerTheme;
    private SwitchMaterial switchAutoReconnect;
    private Spinner spinnerDataRetention;
    private Button btnSave;
    
    // 刷新间隔选项
    private int[] refreshIntervals = {1, 2, 5, 10};
    private String[] refreshIntervalNames = {"1秒", "2秒", "5秒", "10秒"};
    
    // 温度单位选项
    private String[] temperatureUnits = {"celsius", "fahrenheit"};
    private String[] temperatureUnitNames = {"摄氏度(°C)", "华氏度(°F)"};
    
    // 主题选项
    private String[] themes = {"light", "dark", "system"};
    private String[] themeNames = {"浅色", "深色", "跟随系统"};
    
    // 数据保留天数选项
    private int[] retentionDays = {7, 15, 30, 60};
    private String[] retentionDayNames = {"7天", "15天", "30天", "60天"};
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        // 初始化SharedPreferences
        sharedPreferences = getSharedPreferences("app_settings", MODE_PRIVATE);
        editor = sharedPreferences.edit();
        
        initViews();
        setupSpinners();
        loadSettings();
        setupListeners();
    }
    
    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }
        
        spinnerRefreshInterval = findViewById(R.id.spinnerRefreshInterval);
        spinnerTemperatureUnit = findViewById(R.id.spinnerTemperatureUnit);
        spinnerTheme = findViewById(R.id.spinnerTheme);
        switchAutoReconnect = findViewById(R.id.switchAutoReconnect);
        spinnerDataRetention = findViewById(R.id.spinnerDataRetention);
        btnSave = findViewById(R.id.btnSave);
    }
    
    private void setupSpinners() {
        // 刷新间隔
        ArrayAdapter<String> refreshAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, refreshIntervalNames);
        refreshAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRefreshInterval.setAdapter(refreshAdapter);
        
        // 温度单位
        ArrayAdapter<String> tempAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, temperatureUnitNames);
        tempAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTemperatureUnit.setAdapter(tempAdapter);
        
        // 主题
        ArrayAdapter<String> themeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, themeNames);
        themeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTheme.setAdapter(themeAdapter);
        
        // 数据保留天数
        ArrayAdapter<String> retentionAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, retentionDayNames);
        retentionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDataRetention.setAdapter(retentionAdapter);
    }
    
    private void loadSettings() {
        // 加载刷新间隔
        int savedInterval = sharedPreferences.getInt("refresh_interval", 2);
        for (int i = 0; i < refreshIntervals.length; i++) {
            if (refreshIntervals[i] == savedInterval) {
                spinnerRefreshInterval.setSelection(i);
                break;
            }
        }
        
        // 加载温度单位
        String savedUnit = sharedPreferences.getString("temperature_unit", "celsius");
        for (int i = 0; i < temperatureUnits.length; i++) {
            if (temperatureUnits[i].equals(savedUnit)) {
                spinnerTemperatureUnit.setSelection(i);
                break;
            }
        }
        
        // 加载主题
        String savedTheme = sharedPreferences.getString("theme", "light");
        for (int i = 0; i < themes.length; i++) {
            if (themes[i].equals(savedTheme)) {
                spinnerTheme.setSelection(i);
                break;
            }
        }
        
        // 加载自动重连
        boolean autoReconnect = sharedPreferences.getBoolean("auto_reconnect", true);
        switchAutoReconnect.setChecked(autoReconnect);
        
        // 加载数据保留天数
        int savedRetention = sharedPreferences.getInt("data_retention", 30);
        for (int i = 0; i < retentionDays.length; i++) {
            if (retentionDays[i] == savedRetention) {
                spinnerDataRetention.setSelection(i);
                break;
            }
        }
    }
    
    private void setupListeners() {
        btnSave.setOnClickListener(v -> saveSettings());
    }
    
    private void saveSettings() {
        // 保存刷新间隔
        int selectedInterval = spinnerRefreshInterval.getSelectedItemPosition();
        editor.putInt("refresh_interval", refreshIntervals[selectedInterval]);
        
        // 保存温度单位
        int selectedUnit = spinnerTemperatureUnit.getSelectedItemPosition();
        editor.putString("temperature_unit", temperatureUnits[selectedUnit]);
        
        // 保存主题
        int selectedTheme = spinnerTheme.getSelectedItemPosition();
        String themeValue = themes[selectedTheme];
        editor.putString("theme", themeValue);
        
        // 应用主题
        applyTheme(themeValue);
        
        // 保存自动重连
        editor.putBoolean("auto_reconnect", switchAutoReconnect.isChecked());
        
        // 保存数据保留天数
        int selectedRetention = spinnerDataRetention.getSelectedItemPosition();
        editor.putInt("data_retention", retentionDays[selectedRetention]);
        
        editor.apply();
        
        Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show();
        finish();
    }
    
    private void applyTheme(String theme) {
        switch (theme) {
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case "system":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
}
