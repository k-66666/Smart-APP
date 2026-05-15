package com.example.mysmart.model;

/**
 * 控制命令枚举
 * 
 * 命令码定义（与STM32协议对应）：
 * - 风扇控制: 0x10(关), 0x11(开), 0x12(自动)
 * - 紫外线控制: 0x20(关), 0x21(开), 0x22(自动)
 * - 照明控制: 0x30(关), 0x31(开), 0x32(自动)
 */
public enum ControlCommand {
    // 风扇控制
    FAN_OFF(0x10, "风扇关闭"),
    FAN_ON(0x11, "风扇开启"),
    FAN_AUTO(0x12, "风扇自动"),
    
    // 紫外线控制
    UV_OFF(0x20, "紫外线关闭"),
    UV_ON(0x21, "紫外线开启"),
    UV_AUTO(0x22, "紫外线自动"),
    
    // 照明控制
    LIGHT_OFF(0x30, "照明关闭"),
    LIGHT_ON(0x31, "照明开启"),
    LIGHT_AUTO(0x32, "照明自动"),
    
    // 兼容旧版本（映射到新命令）
    @Deprecated
    DISPLAY_ON(0x31, "显示模块开"),  // 映射到LIGHT_ON
    @Deprecated
    DISPLAY_OFF(0x30, "显示模块关"), // 映射到LIGHT_OFF
    @Deprecated
    ALARM_ON(0x21, "声光报警开"),    // 映射到UV_ON
    @Deprecated
    ALARM_OFF(0x20, "声光报警关"),   // 映射到UV_OFF
    @Deprecated
    DRIVER_ON(0x11, "驱动模块开"),   // 映射到FAN_ON
    @Deprecated
    DRIVER_OFF(0x10, "驱动模块关");  // 映射到FAN_OFF
    
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
