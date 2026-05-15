# STM32智能防霉柜与Android应用通信协议

## 概述

本文档描述STM32智能防霉柜系统与Android应用之间的通信协议。

## 硬件架构

- **主控芯片**: STM32F103C8
- **WiFi模块**: ESP8266 (通过USART2连接)
- **传感器**:
  - DHT11: 温湿度传感器
  - TVOC-301: 空气质量传感器 (TVOC + CO2)
  - 光敏传感器: 检测环境光线
  - 门磁传感器: 检测柜门开关
- **执行器**:
  - 继电器1: 控制风扇
  - 继电器2: 控制紫外线消毒灯
  - 继电器3: 控制照明灯
  - 蜂鸣器: 报警提示
- **显示**: OLED显示屏 (I2C接口)
- **时钟**: DS1302 RTC模块

## 通信方式

### 连接方式
- **协议**: TCP/IP
- **端口**: 8080
- **数据格式**: JSON字符串
- **字符编码**: UTF-8

### WiFi配置
STM32通过AT指令配置ESP8266:
```c
WIFI_SSID: "YOUR_SSID"
WIFI_PASSWORD: "YOUR_PASSWORD"
WIFI_TCP_HOST: "192.168.1.100"  // Android设备IP
WIFI_TCP_PORT: 8080
```

## 数据协议

### 1. 上行数据（STM32 → Android）

#### 传感器数据上报
**发送频率**: 每5秒一次

**数据格式**:
```json
{
  "temp": 25.5,
  "humi": 60.3,
  "tvoc": 150,
  "co2": 450,
  "fan": 1,
  "uv": 0,
  "light": 1,
  "alarm": 0
}
```

**字段说明**:

| 字段 | 类型 | 单位 | 范围 | 说明 |
|------|------|------|------|------|
| temp | float | °C | -40 ~ 80 | 温度值，精度0.1°C |
| humi | float | % | 0 ~ 100 | 湿度值，精度0.1% |
| tvoc | int | μg/m³ | 0 ~ 9999 | TVOC浓度 |
| co2 | int | ppm | 0 ~ 5000 | CO2浓度（估算值） |
| fan | int | - | 0/1 | 风扇状态：0=关闭，1=开启 |
| uv | int | - | 0/1 | 紫外线灯状态：0=关闭，1=开启 |
| light | int | - | 0/1 | 照明灯状态：0=关闭，1=开启 |
| alarm | int | - | 0/1 | 报警状态：0=正常，1=报警 |

#### STM32端实现
```c
// app_wifi_comm.c
sprintf(json_buffer,
        "{\"temp\":%d.%d,\"humi\":%d.%d,\"tvoc\":%u,\"co2\":%u,"
        "\"fan\":%d,\"uv\":%d,\"light\":%d,\"alarm\":%d}",
        t10 / 10, tf,
        h10 / 10, hf,
        (unsigned)p_state->sensor.tvoc,
        (unsigned)p_state->sensor.co2,
        (p_state->fan_state == DEVICE_ON) ? 1 : 0,
        (p_state->uv_state == DEVICE_ON || p_state->uv_sanitizing) ? 1 : 0,
        (p_state->light_state == DEVICE_ON) ? 1 : 0,
        (int)p_state->alarm_state);
```

#### Android端解析
```java
// ProtocolParser.java
public static SensorData parseSensorData(String jsonString) {
    JSONObject json = new JSONObject(jsonString);
    float temperature = (float) json.optDouble("temp", 25.0);
    float humidity = (float) json.optDouble("humi", 50.0);
    int tvoc = json.optInt("tvoc", 0);
    int co2 = json.optInt("co2", 400);
    // ...
}
```

### 2. 下行控制（Android → STM32）

#### 控制命令格式
```json
{
  "cmd": "control",
  "fan": 1,
  "uv": 0,
  "light": 2
}
```

**字段说明**:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| cmd | string | 是 | 命令类型，固定为"control" |
| fan | int | 否 | 风扇控制：0=关闭，1=开启，2=自动 |
| uv | int | 否 | 紫外线控制：0=关闭，1=开启，2=自动 |
| light | int | 否 | 灯光控制：0=关闭，1=开启，2=自动 |

**注意**: 
- 可以只发送需要控制的设备字段
- 自动模式(2)下，设备由STM32根据传感器数据自动控制

#### Android端发送
```java
// ProtocolParser.java
public static byte[] buildControlCommand(String deviceType, int state) {
    JSONObject json = new JSONObject();
    json.put("cmd", "control");
    json.put(deviceType, state);  // deviceType: "fan", "uv", "light"
    return json.toString().getBytes("UTF-8");
}
```

#### STM32端接收（待实现）
```c
// 建议在 app_wifi_comm.c 中添加
void APP_WiFi_ParseCommand(const char *json_str) {
    // 解析JSON命令
    // 更新设备状态
    // 例如: 解析 "fan":1 -> 设置 g_system_state.fan_state = DEVICE_ON
}
```

### 3. 心跳包（可选）

**Android → STM32**:
```json
{
  "cmd": "heartbeat",
  "timestamp": 1234567890
}
```

**用途**: 保持连接活跃，检测连接状态

## 系统控制逻辑

### 风扇自动控制
```c
// STM32: app_control_logic.c
if (humidity > 65.0f) {
    fan_state = DEVICE_ON;   // 开启除湿
} else if (humidity < 50.0f) {
    fan_state = DEVICE_OFF;  // 关闭风扇
}
```

### 紫外线消毒逻辑
1. **安全锁定**: 柜门打开时，紫外线灯强制关闭
2. **自动消毒触发条件**:
   - 条件1: 风扇连续运行30分钟 + 湿度>65%
   - 条件2: 定时消毒（凌晨2:00）
3. **消毒时长**: 15分钟
4. **完成提示**: 消毒完成后蜂鸣器报警

```c
// STM32: app_control_logic.c
if (door_state == DOOR_OPEN) {
    uv_locked = 1;  // 门开时锁定紫外线
    uv_sanitizing = 0;
    RELAY_UV_OFF();
}

// 自动消毒条件1
if (fan_run_continuous_sec >= 30*60 && humidity > 65.0f) {
    uv_sanitizing = 1;
}

// 自动消毒条件2
if (hour == 2 && minute == 0) {
    uv_sanitizing = 1;
}
```

### 照明灯自动控制
```c
// STM32: app_control_logic.c
// 门打开 + 环境暗 -> 自动开灯
if (door_prev == DOOR_CLOSED && door_state == DOOR_OPEN && light == LIGHT_DARK) {
    light_state = DEVICE_ON;
}

// 门关闭后延迟2秒关灯
if (door_prev == DOOR_OPEN && door_state == DOOR_CLOSED) {
    light_off_remain_ms = 2000;
}
```

### 报警触发条件
```c
// STM32: app_control_logic.c
if (humidity > 75.0f || uv_done_alarm) {
    alarm_state = 1;
    ALARM_ON();
}
```

## 配置参数对照表

| 功能 | STM32配置 | Android配置 | 说明 |
|------|-----------|-------------|------|
| 湿度高阈值 | HUMIDITY_HIGH_THRESHOLD = 65.0f | HUMIDITY_HIGH_THRESHOLD = 65.0f | 风扇开启阈值 |
| 湿度低阈值 | HUMIDITY_LOW_THRESHOLD = 50.0f | HUMIDITY_LOW_THRESHOLD = 50.0f | 风扇关闭阈值 |
| 湿度报警阈值 | HUMIDITY_ALARM_THRESHOLD = 75.0f | HUMIDITY_ALARM_THRESHOLD = 75.0f | 蜂鸣器报警 |
| 消毒时长 | UV_DISINFECT_DURATION = 15 | UV_DISINFECT_DURATION_MINUTES = 15 | 单位：分钟 |
| 风扇预运行 | FAN_RUN_TIME_BEFORE_UV = 30 | FAN_RUN_TIME_BEFORE_UV_MINUTES = 30 | 消毒前风扇运行时间 |
| 定时消毒 | UV_SCHEDULED_HOUR = 2 | UV_SCHEDULED_HOUR = 2 | 凌晨2点 |
| TCP端口 | WIFI_TCP_PORT = 8080 | WIFI_TCP_PORT = 8080 | WiFi通信端口 |
| 数据间隔 | 5000ms | DATA_UPDATE_INTERVAL_MS = 5000 | 数据上报间隔 |

## Android应用实现要点

### 1. 连接管理
```java
// WifiConnectionManager.java
- 默认端口: 8080
- 连接超时: 5秒
- 心跳间隔: 30秒
- 支持JSON流式解析（处理粘包）
```

### 2. 数据解析
```java
// ProtocolParser.java
- 使用StringBuilder缓冲接收数据
- 提取完整JSON对象（处理分包）
- 数据范围验证和修正
- 设备状态同步
```

### 3. 界面显示
- 实时显示温湿度、TVOC、CO2
- 显示设备运行状态（风扇、紫外线、灯光）
- 湿度等级提示（干燥/适宜/偏高/过高）
- 空气质量等级（优秀/良好/污染）
- 报警状态提示

### 4. 控制功能
- 手动控制：开/关
- 自动模式：由STM32根据传感器自动控制
- 实时状态反馈

## STM32端待完善功能

### 1. 命令接收处理
当前STM32只实现了数据上报，建议添加命令接收功能：

```c
// 在 app_wifi_comm.c 中添加
void APP_WiFi_ReceiveCommand(void) {
    uint8_t buffer[256];
    uint16_t len = BSP_USART2_Receive(buffer, sizeof(buffer));
    
    if (len > 0) {
        buffer[len] = '\0';
        // 查找JSON对象
        char *json_start = strchr((char*)buffer, '{');
        if (json_start != NULL) {
            // 简单的JSON解析
            if (strstr(json_start, "\"cmd\":\"control\"") != NULL) {
                // 解析控制命令
                if (strstr(json_start, "\"fan\":0") != NULL) {
                    g_system_state.fan_state = DEVICE_OFF;
                    g_system_state.manual_fan = 1;
                } else if (strstr(json_start, "\"fan\":1") != NULL) {
                    g_system_state.fan_state = DEVICE_ON;
                    g_system_state.manual_fan = 1;
                } else if (strstr(json_start, "\"fan\":2") != NULL) {
                    g_system_state.fan_state = DEVICE_AUTO;
                    g_system_state.manual_fan = 0;
                }
                // 类似处理 uv 和 light
            }
        }
    }
}
```

### 2. 在主循环中调用
```c
// main.c
while (1) {
    APP_DataCollect(&g_system_state);
    APP_InputScan(&g_system_state);
    APP_WiFi_ReceiveCommand();  // 添加命令接收
    APP_ControlLogic_Process(&g_system_state);
    APP_Display_Update(&g_system_state);
    
    // WiFi数据发送
    static uint32_t wifi_timer = 0;
    wifi_timer += SYSTEM_LOOP_DELAY_MS;
    if (wifi_timer >= 5000u) {
        wifi_timer = 0;
        APP_WiFi_SendData(&g_system_state);
    }
    
    delay_ms(SYSTEM_LOOP_DELAY_MS);
}
```

## 测试建议

### 1. 模拟测试
Android应用已支持模拟模式：
```java
// 连接到 "demo" 或 "127.0.0.1" 进入模拟模式
DeviceInfo demoDevice = new DeviceInfo("demo", "模拟设备", "WIFI");
```

### 2. 实际测试步骤
1. 配置STM32的WiFi SSID和密码
2. 确保Android设备和STM32在同一局域网
3. 在Android应用中扫描设备（IP地址）
4. 连接并观察数据接收
5. 测试控制命令发送

### 3. 调试工具
- STM32: USART2串口输出调试信息
- Android: Logcat查看通信日志
- 网络抓包: Wireshark分析TCP数据包

## 常见问题

### Q1: 连接不上设备
- 检查WiFi是否在同一网络
- 确认STM32的WiFi模块已初始化
- 检查端口号是否匹配（8080）
- 查看防火墙设置

### Q2: 数据解析失败
- 检查JSON格式是否正确
- 确认字符编码为UTF-8
- 查看是否有粘包/分包问题

### Q3: 控制命令无响应
- 确认STM32已实现命令接收功能
- 检查JSON格式是否正确
- 查看STM32串口调试输出

## 版本历史

- v1.0 (2026-05): 初始版本，实现基本通信协议
  - 支持JSON格式数据传输
  - 实现传感器数据上报
  - 实现设备状态同步
  - 定义控制命令格式

## 参考资料

- STM32代码: `Stm32-ZNYG/Drivers/APP/`
- Android代码: `MySmart/app/src/main/java/com/example/mysmart/`
- 配置文件: `DeviceConfig.java`
- 协议解析: `ProtocolParser.java`
