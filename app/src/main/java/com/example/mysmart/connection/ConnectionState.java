package com.example.mysmart.connection;

/**
 * 连接状态枚举
 */
public enum ConnectionState {
    DISCONNECTED("未连接"),
    CONNECTING("连接中"),
    CONNECTED("已连接"),
    ERROR("连接错误");
    
    private final String description;
    
    ConnectionState(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
