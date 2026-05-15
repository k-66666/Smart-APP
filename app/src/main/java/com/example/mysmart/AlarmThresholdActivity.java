package com.example.mysmart;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

/**
 * 报警阈值设置Activity
 */
public class AlarmThresholdActivity extends AppCompatActivity {
    
    private SharedPreferences sharedPreferences;
    
    // UI组件
    private SwitchMaterial switchAlarmEnabled;
    private TextInputEditText etTempMin;
    private TextInputEditText etTempMax;
    private TextInputEditText etHumidityMin;
    private TextInputEditText etHumidityMax;
    private TextInputEditText etAirQualityMax;
    private TextInputEditText etLightMin;
    private TextInputEditText etLightMax;
    private Button btnSave;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_threshold);
        
        sharedPreferences = getSharedPreferences("alarm_settings", MODE_PRIVATE);
        
        initViews();
        loadSettings();
        setupListeners();
    }
    
    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        
        switchAlarmEnabled = findViewById(R.id.switchAlarmEnabled);
        etTempMin = findViewById(R.id.etTempMin);
        etTempMax = findViewById(R.id.etTempMax);
        etHumidityMin = findViewById(R.id.etHumidityMin);
        etHumidityMax = findViewById(R.id.etHumidityMax);
        etAirQualityMax = findViewById(R.id.etAirQualityMax);
        etLightMin = findViewById(R.id.etLightMin);
        etLightMax = findViewById(R.id.etLightMax);
        btnSave = findViewById(R.id.btnSave);
    }
    
    private void loadSettings() {
        // 加载报警开关
        boolean alarmEnabled = sharedPreferences.getBoolean("alarm_enabled", true);
        switchAlarmEnabled.setChecked(alarmEnabled);
        
        // 加载温度阈值
        float tempMin = sharedPreferences.getFloat("temp_min", 15.0f);
        float tempMax = sharedPreferences.getFloat("temp_max", 30.0f);
        etTempMin.setText(String.valueOf(tempMin));
        etTempMax.setText(String.valueOf(tempMax));
        
        // 加载湿度阈值
        float humidityMin = sharedPreferences.getFloat("humidity_min", 30.0f);
        float humidityMax = sharedPreferences.getFloat("humidity_max", 70.0f);
        etHumidityMin.setText(String.valueOf(humidityMin));
        etHumidityMax.setText(String.valueOf(humidityMax));
        
        // 加载空气质量阈值
        int airQualityMax = sharedPreferences.getInt("air_quality_max", 150);
        etAirQualityMax.setText(String.valueOf(airQualityMax));
        
        // 加载光照强度阈值
        int lightMin = sharedPreferences.getInt("light_min", 100);
        int lightMax = sharedPreferences.getInt("light_max", 5000);
        etLightMin.setText(String.valueOf(lightMin));
        etLightMax.setText(String.valueOf(lightMax));
    }
    
    private void setupListeners() {
        btnSave.setOnClickListener(v -> saveSettings());
    }
    
    private void saveSettings() {
        try {
            // 获取输入值
            float tempMin = Float.parseFloat(etTempMin.getText().toString());
            float tempMax = Float.parseFloat(etTempMax.getText().toString());
            float humidityMin = Float.parseFloat(etHumidityMin.getText().toString());
            float humidityMax = Float.parseFloat(etHumidityMax.getText().toString());
            int airQualityMax = Integer.parseInt(etAirQualityMax.getText().toString());
            int lightMin = Integer.parseInt(etLightMin.getText().toString());
            int lightMax = Integer.parseInt(etLightMax.getText().toString());
            
            // 验证输入
            if (tempMin >= tempMax) {
                Toast.makeText(this, "温度上限必须大于下限", Toast.LENGTH_SHORT).show();
                return;
            }
            if (humidityMin >= humidityMax) {
                Toast.makeText(this, "湿度上限必须大于下限", Toast.LENGTH_SHORT).show();
                return;
            }
            if (lightMin >= lightMax) {
                Toast.makeText(this, "光照强度上限必须大于下限", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 保存设置
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("alarm_enabled", switchAlarmEnabled.isChecked());
            editor.putFloat("temp_min", tempMin);
            editor.putFloat("temp_max", tempMax);
            editor.putFloat("humidity_min", humidityMin);
            editor.putFloat("humidity_max", humidityMax);
            editor.putInt("air_quality_max", airQualityMax);
            editor.putInt("light_min", lightMin);
            editor.putInt("light_max", lightMax);
            editor.apply();
            
            Toast.makeText(this, "阈值已保存", Toast.LENGTH_SHORT).show();
            finish();
            
        } catch (NumberFormatException e) {
            Toast.makeText(this, "请输入有效的数值", Toast.LENGTH_SHORT).show();
        }
    }
}
