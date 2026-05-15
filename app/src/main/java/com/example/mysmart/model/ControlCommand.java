package com.example.mysmart.model;

/**
 * 控制命令枚举
 */
public enum ControlCommand {
    DISPLAY_ON(0x01, "显示模块开"),
    DISPLAY_OFF(0x02, "显示模块关"),
    ALARM_ON(0x03, "声光报警开"),
    ALARM_OFF(0x04, "声光报警关"),
    DRIVER_ON(0x05, "驱动模块开"),
    DRIVER_OFF(0x06, "驱动模块关");
    
    private final int code;
    private final String description;
    
    ControlCommand(int code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public int getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static ControlCommand fromCode(int code) {
        for (ControlCommand cmd : values()) {
            if (cmd.code == code) {
                return cmd;
            }
        }
        return null;
    }
}
