package com.example.mysmart.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mysmart.R;
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 数据页面Fragment
 */
public class DataFragment extends Fragment {
    
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
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_data, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        repository = SensorRepository.getInstance(requireContext());
        
        initViews(view);
        setupSpinners();
        setupChart();
        setupListeners();
        
        // 加载初始数据
        loadData();
    }
    
    private void initViews(View view) {
        spinnerTimeRange = view.findViewById(R.id.spinnerTimeRange);
        spinnerSensorType = view.findViewById(R.id.spinnerSensorType);
        btnRefresh = view.findViewById(R.id.btnRefresh);
        btnExport = view.findViewById(R.id.btnExport);
        lineChart = view.findViewById(R.id.lineChart);
        tvMaxValue = view.findViewById(R.id.tvMaxValue);
        tvMinValue = view.findViewById(R.id.tvMinValue);
        tvAvgValue = view.findViewById(R.id.tvAvgValue);
    }
    
    private void setupSpinners() {
        // 时间范围
        String[] timeRangeNames = {"最近1小时", "最近6小时", "最近24小时", "最近7天", "最近30天"};
        ArrayAdapter<String> timeAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, timeRangeNames);
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTimeRange.setAdapter(timeAdapter);
        spinnerTimeRange.setSelection(2); // 默认24小时
        
        // 传感器类型
        ArrayAdapter<String> sensorAdapter = new ArrayAdapter<>(requireContext(),
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
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        btnRefresh.setEnabled(true);
                        currentData = data;
                        updateDisplay();
                        Toast.makeText(requireContext(), 
                            "加载了 " + data.size() + " 条数据", Toast.LENGTH_SHORT).show();
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        btnRefresh.setEnabled(true);
                        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
    
    private void updateDisplay() {
        if (currentData.isEmpty()) {
            Toast.makeText(requireContext(), "暂无数据", Toast.LENGTH_SHORT).show();
            return;
        }
        
        int sensorIndex = spinnerSensorType.getSelectedItemPosition();
        String sensorType = sensorTypes[sensorIndex];
        String unit = sensorUnits[sensorIndex];
        
        // 更新图表
        updateChart(sensorType, unit);
        
        // 更新统计信息
        updateStatistics(sensorType, unit);
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
        dataSet.setColor(Color.parseColor("#6200EE"));
        dataSet.setCircleColor(Color.parseColor("#6200EE"));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(3f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(0f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#BB86FC"));
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
    }
    
    private void exportData() {
        if (currentData.isEmpty()) {
            Toast.makeText(requireContext(), "没有数据可导出", Toast.LENGTH_SHORT).show();
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
            
            Toast.makeText(requireContext(), "数据已导出到: " + file.getAbsolutePath(), 
                Toast.LENGTH_LONG).show();
            
        } catch (IOException e) {
            Toast.makeText(requireContext(), "导出失败: " + e.getMessage(), 
                Toast.LENGTH_SHORT).show();
        }
    }
}
