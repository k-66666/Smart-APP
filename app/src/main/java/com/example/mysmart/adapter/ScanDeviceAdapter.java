package com.example.mysmart.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mysmart.R;
import com.example.mysmart.model.DeviceInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 扫描设备列表适配器
 */
public class ScanDeviceAdapter extends RecyclerView.Adapter<ScanDeviceAdapter.ViewHolder> {

    private List<DeviceInfo> devices = new ArrayList<>();
    private OnDeviceClickListener listener;

    public interface OnDeviceClickListener {
        void onDeviceClick(DeviceInfo device);
    }

    public ScanDeviceAdapter(OnDeviceClickListener listener) {
        this.listener = listener;
    }

    public void setDevices(List<DeviceInfo> devices) {
        this.devices = devices != null ? devices : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_scan_device, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DeviceInfo device = devices.get(position);
        
        String name = device.getDisplayName();
        holder.tvDeviceName.setText(name != null && !name.isEmpty() ? name : "未知设备");
        holder.tvDeviceAddress.setText(device.getDeviceId());
        
        // 根据类型设置图标
        String type = device.getConnectionType();
        if ("WIFI".equals(type)) {
            holder.ivDeviceIcon.setImageResource(android.R.drawable.ic_menu_sort_by_size);
            holder.ivDeviceIcon.setColorFilter(0xFF30D158);
        } else {
            holder.ivDeviceIcon.setImageResource(android.R.drawable.stat_sys_data_bluetooth);
            holder.ivDeviceIcon.setColorFilter(0xFF0A84FF);
        }
        
        holder.btnConnect.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeviceClick(device);
            }
        });
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivDeviceIcon;
        TextView tvDeviceName;
        TextView tvDeviceAddress;
        View btnConnect;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivDeviceIcon = itemView.findViewById(R.id.ivDeviceIcon);
            tvDeviceName = itemView.findViewById(R.id.tvDeviceName);
            tvDeviceAddress = itemView.findViewById(R.id.tvDeviceAddress);
            btnConnect = itemView.findViewById(R.id.btnConnectDevice);
        }
    }
}
