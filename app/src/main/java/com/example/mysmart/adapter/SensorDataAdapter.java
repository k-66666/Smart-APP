package com.example.mysmart.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mysmart.R;
import com.example.mysmart.model.SensorData;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 传感器数据列表适配器
 */
public class SensorDataAdapter extends RecyclerView.Adapter<SensorDataAdapter.ViewHolder> {
    
    private List<SensorData> dataList = new ArrayList<>();
    private String sensorType = "temperature";
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    
    public void setData(List<SensorData> data) {
        this.dataList = data;
        notifyDataSetChanged();
    }
    
    public void setSensorType(String type) {
        this.sensorType = type;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sensor_data, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SensorData data = dataList.get(position);
        
        // 显示时间
        holder.tvTime.setText(dateFormat.format(new Date(data.getTimestamp())));
        
        // 根据传感器类型显示数值
        String value;
        String status = "正常";
        int statusColor = android.R.color.holo_green_dark;
        
        switch (sensorType) {
            case "temperature":
                value = String.format("%.1f°C", data.getTemperature());
                if (data.getTemperature() < 15 || data.getTemperature() > 30) {
                    status = "异常";
                    statusColor = android.R.color.holo_red_dark;
                }
                break;
            case "humidity":
                value = String.format("%.0f%%", data.getHumidity());
                if (data.getHumidity() < 30 || data.getHumidity() > 70) {
                    status = "异常";
                    statusColor = android.R.color.holo_red_dark;
                }
                break;
            case "airQuality":
                value = String.format("%d AQI", data.getAirQuality());
                if (data.getAirQuality() > 150) {
                    status = "异常";
                    statusColor = android.R.color.holo_red_dark;
                }
                break;
            case "co2Concentration":
                value = String.format("%d ppm", data.getCo2Concentration());
                if (data.getCo2Concentration() > 1000) {
                    status = "异常";
                    statusColor = android.R.color.holo_red_dark;
                }
                break;
            default:
                value = "--";
        }
        
        holder.tvValue.setText(value);
        holder.tvStatus.setText(status);
        holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(statusColor));
    }
    
    @Override
    public int getItemCount() {
        return dataList.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime;
        TextView tvValue;
        TextView tvStatus;
        
        ViewHolder(View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvValue = itemView.findViewById(R.id.tvValue);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
