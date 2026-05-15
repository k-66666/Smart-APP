package com.example.mysmart.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mysmart.R;
import com.example.mysmart.model.DeviceInfo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 设备列表适配器
 */
public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {
    
    private List<DeviceInfo> deviceList;
    private OnDeviceActionListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    
    public interface OnDeviceActionListener {
        void onConnect(DeviceInfo device);
        void onDelete(DeviceInfo device);
        void onEdit(DeviceInfo device);
    }
    
    public DeviceAdapter(List<DeviceInfo> deviceList, OnDeviceActionListener listener) {
        this.deviceList = deviceList;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DeviceInfo device = deviceList.get(position);
        
        holder.tvDeviceName.setText(device.getDisplayName());
        holder.tvDeviceId.setText(device.getDeviceId());
        holder.tvConnectionType.setText(device.getConnectionType());
        holder.tvLastConnected.setText("最后连接: " + dateFormat.format(new Date(device.getLastConnected())));
        
        holder.btnConnect.setOnClickListener(v -> {
            if (listener != null) {
                listener.onConnect(device);
            }
        });
        
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(device);
            }
        });
        
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEdit(device);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return deviceList.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDeviceName;
        TextView tvDeviceId;
        TextView tvConnectionType;
        TextView tvLastConnected;
        Button btnConnect;
        Button btnDelete;
        Button btnEdit;
        
        ViewHolder(View itemView) {
            super(itemView);
            tvDeviceName = itemView.findViewById(R.id.tvDeviceName);
            tvDeviceId = itemView.findViewById(R.id.tvDeviceId);
            tvConnectionType = itemView.findViewById(R.id.tvConnectionType);
            tvLastConnected = itemView.findViewById(R.id.tvLastConnected);
            btnConnect = itemView.findViewById(R.id.btnConnect);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnEdit = itemView.findViewById(R.id.btnEdit);
        }
    }
}
