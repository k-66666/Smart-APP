package com.example.mysmart;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mysmart.adapter.SensorDataAdapter;
import com.example.mysmart.model.SensorData;
import com.example.mysmart.repository.SensorRepository;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.appbar.MaterialToolbar;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 历史数据Activity
 */
public class HistoryActivity extends AppCompatActivity {
    
    private SensorRepository repository;
    
    // UI组件
    private Spinner spinnerTimeRange;
    private Spinner spinnerSensorType;
    private Button btnRefresh;
    private Button btnExport;
    private LineChart lineChart;
    private TextView tvMaxValue;
    private TextView tvMinValue;
    private TextView tvAvgValue;
    private TextView tvDataCount;
    private RecyclerView recyclerViewData;
    
    private SensorDataAdapter adapter;
    private List<SensorData> currentData = new ArrayList<>();
    
    // 时间范围（毫秒）
    private long[] timeRanges = {
        60 * 60 * 1000L,        // 1小时
        6 * 60 * 60 * 1000L,    // 6小时
        24 * 60 * 60 * 1000L,   // 24小时
        7 * 24 * 60 * 60 * 1000L,   // 7天
        30 * 24 * 60 * 60 * 1000L   // 30天
    };
    
    private String[] sensorTypes = {"temperature", "humidity", "airQuality", "co2Concentration"};
    private String[] sensorNames = {"温度", "湿度", "空气质量", "二氧化碳浓度"};
    private String[] sensorUnits = {"°C", "%", "AQI", "ppm"};
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        
        repository = SensorRepository.getInstance(this);
        
        initViews();
        setupSpinners();
        setupChart();
        setupRecyclerView();
        setupListeners();
        
        // 加载初始数据
        loadData();
    }
    
    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        
        spinnerTimeRange = findViewById(R.id.spinnerTimeRange);
        spinnerSensorType = findViewById(R.id.spinnerSensorType);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnExport = findViewById(R.id.btnExport);
        lineChart = findViewById(R.id.lineChart);
        tvMaxValue = findViewById(R.id.tvMaxValue);
        tvMinValue = findViewById(R.id.tvMinValue);
        tvAvgValue = findViewById(R.id.tvAvgValue);
        tvDataCount = findViewById(R.id.tvDataCount);
        recyclerViewData = findViewById(R.id.recyclerViewData);
    }
    
    private void setupSpinners() {
        // 时间范围
        String[] timeRangeNames = {"最近1小时", "最近6小时", "最近24小时", "最近7天", "最近30天"};
        ArrayAdapter<String> timeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, timeRangeNames);
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTimeRange.setAdapter(timeAdapter);
        spinnerTimeRange.setSelection(2); // 默认24小时
        
        // 传感器类型
        ArrayAdapter<String> sensorAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, sensorNames);
        sensorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSensorType.setAdapter(sensorAdapter);
    }
    
    private void setupChart() {
        lineChart.getDescription().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.setDrawGridBackground(false);
        
        // X轴设置
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            private SimpleDateFormat format = new SimpleDateFormat("HH:mm", Locale.getDefault());
            
            @Override
            public String getFormattedValue(float value) {
                return format.format(new Date((long) value));
            }
        });
        
        // Y轴设置
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        
        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false);
        
        // 图例
        lineChart.getLegend().setEnabled(true);
    }
    
    private void setupRecyclerView() {
        adapter = new SensorDataAdapter();
        recyclerViewData.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewData.setAdapter(adapter);
    }
    
    private void setupListeners() {
        spinnerTimeRange.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadData();
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        
        spinnerSensorType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateDisplay();
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        
        btnRefresh.setOnClickListener(v -> {
            btnRefresh.setEnabled(false);
            loadData();
        });
        btnExport.setOnClickListener(v -> exportData());
    }
    
    private void loadData() {
        int timeRangeIndex = spinnerTimeRange.getSelectedItemPosition();
        long endTime = System.currentTimeMillis();
        long startTime = endTime - timeRanges[timeRangeIndex];
        
        repository.getHistoryData(startTime, endTime, new SensorRepository.DataCallback<List<SensorData>>() {
            @Override
            public void onSuccess(List<SensorData> data) {
                runOnUiThread(() -> {
                    btnRefresh.setEnabled(true);
                    currentData = data;
                    updateDisplay();
                    Toast.makeText(HistoryActivity.this, 
                        "加载了 " + data.size() + " 条数据", Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    btnRefresh.setEnabled(true);
                    Toast.makeText(HistoryActivity.this, error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void updateDisplay() {
        if (currentData.isEmpty()) {
            Toast.makeText(this, "暂无数据", Toast.LENGTH_SHORT).show();
            return;
        }
        
        int sensorIndex = spinnerSensorType.getSelectedItemPosition();
        String sensorType = sensorTypes[sensorIndex];
        String unit = sensorUnits[sensorIndex];
        
        // 更新图表
        updateChart(sensorType, unit);
        
        // 更新统计信息
        updateStatistics(sensorType, unit);
        
        // 更新列表
        adapter.setSensorType(sensorType);
        adapter.setData(currentData);
    }
    
    private void updateChart(String sensorType, String unit) {
        List<Entry> entries = new ArrayList<>();
        
        for (SensorData data : currentData) {
            float value = 0;
            switch (sensorType) {
                case "temperature":
                    value = data.getTemperature();
                    break;
                case "humidity":
                    value = data.getHumidity();
                    break;
                case "airQuality":
                    value = data.getAirQuality();
                    break;
                case "co2Concentration":
                    value = data.getCo2Concentration();
                    break;
            }
            entries.add(new Entry(data.getTimestamp(), value));
        }
        
        LineDataSet dataSet = new LineDataSet(entries, sensorNames[spinnerSensorType.getSelectedItemPosition()]);
        dataSet.setColor(Color.BLUE);
        dataSet.setCircleColor(Color.BLUE);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(3f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(9f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.BLUE);
        dataSet.setFillAlpha(50);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        
        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.invalidate();
    }
    
    private void updateStatistics(String sensorType, String unit) {
        float max = Float.MIN_VALUE;
        float min = Float.MAX_VALUE;
        float sum = 0;
        int count = currentData.size();
        
        for (SensorData data : currentData) {
            float value = 0;
            switch (sensorType) {
                case "temperature":
                    value = data.getTemperature();
                    break;
                case "humidity":
                    value = data.getHumidity();
                    break;
                case "airQuality":
                    value = data.getAirQuality();
                    break;
                case "co2Concentration":
                    value = data.getCo2Concentration();
                    break;
            }
            
            max = Math.max(max, value);
            min = Math.min(min, value);
            sum += value;
        }
        
        float avg = sum / count;
        
        tvMaxValue.setText(String.format("%.1f %s", max, unit));
        tvMinValue.setText(String.format("%.1f %s", min, unit));
        tvAvgValue.setText(String.format("%.1f %s", avg, unit));
        tvDataCount.setText(String.valueOf(count));
    }
    
    private void exportData() {
        if (currentData.isEmpty()) {
            Toast.makeText(this, "没有数据可导出", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            String fileName = "sensor_data_" + System.currentTimeMillis() + ".csv";
            File file = new File(downloadsDir, fileName);
            
            FileWriter writer = new FileWriter(file);
            
            // 写入表头
            writer.append("时间,温度(°C),湿度(%),空气质量(AQI),CO2浓度(ppm)\n");
            
            // 写入数据
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            for (SensorData data : currentData) {
                writer.append(dateFormat.format(new Date(data.getTimestamp()))).append(",");
                writer.append(String.valueOf(data.getTemperature())).append(",");
                writer.append(String.valueOf(data.getHumidity())).append(",");
                writer.append(String.valueOf(data.getAirQuality())).append(",");
                writer.append(String.valueOf(data.getCo2Concentration())).append("\n");
            }
            
            writer.flush();
            writer.close();
            
            Toast.makeText(this, "数据已导出到: " + file.getAbsolutePath(), 
                Toast.LENGTH_LONG).show();
            
        } catch (IOException e) {
            Toast.makeText(this, "导出失败: " + e.getMessage(), 
                Toast.LENGTH_SHORT).show();
        }
    }
}
