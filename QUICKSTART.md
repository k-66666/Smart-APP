# 快速启动指南

## 🚀 5分钟快速测试

### 方式1: Demo模式（无需硬件）

```
1. 打开MySmart应用
2. 点击底部"设备"图标
3. 点击"添加设备"
4. 输入:
   - 设备名称: 测试设备
   - 设备ID: demo
   - 连接方式: WiFi
5. 点击"连接"
6. 返回"监控"页面
7. 查看模拟数据（每2秒更新）
8. 测试控制按钮（不会崩溃）
```

### 方式2: 蓝牙连接（需要蓝牙模块）

```
1. 确保STM32连接蓝牙模块(HC-05/HC-06)
2. 打开手机蓝牙
3. 打开MySmart应用
4. 授予蓝牙和位置权限
5. 点击"扫描设备"
6. 选择你的设备（如"HC-05"）
7. 点击"连接"
8. 查看实时数据
9. 测试控制功能
```

### 方式3: WiFi连接（需要WiFi模块）

```
1. 确保STM32和手机在同一WiFi
2. 打开MySmart应用
3. 点击"扫描WiFi设备"
4. 选择STM32的IP地址
5. 点击"连接"
6. 查看实时数据
7. 测试控制功能
```

## � 应用界面说明

### 监控页面（主页）
- **连接状态**: 显示当前连接状态
- **传感器数据**: 温度、湿度、TVOC、CO2
- **控制开关**: 
  - 风扇开关（对应Driver模块）
  - 紫外线开关（对应Alarm模块）
  - 照明开关（对应Display模块）

### 数据页面
- 查看历史数据
- 数据图表显示
- 数据导出功能

### 设备页面
- 扫描设备
- 添加设备
- 连接/断开设备
- 设备列表管理

### 设置页面
- 报警阈值设置
- 数据保留时间
- 应用信息

## 🔧 STM32配置

### 蓝牙模式

#### 硬件连接
```
STM32 PA2(TX) -> 蓝牙模块 RX
STM32 PA3(RX) <- 蓝牙模块 TX
VCC -> 3.3V或5V
GND -> GND
```

#### 代码配置
```c
// main.c
BSP_USART2_Init(115200);  // 波特率115200

// 主循环
while (1) {
    APP_DataCollect(&g_system_state);
    APP_InputScan(&g_system_state);
    APP_WiFi_ReceiveCommand();  // 接收蓝牙命令
    APP_ControlLogic_Process(&g_system_state);
    APP_Display_Update(&g_system_state);
    
    // 每5秒发送一次数据
    static uint32_t send_timer = 0;
    send_timer += SYSTEM_LOOP_DELAY_MS;
    if (send_timer >= 5000u) {
        send_timer = 0;
        APP_WiFi_SendData(&g_system_state);
    }
    
    delay_ms(SYSTEM_LOOP_DELAY_MS);
}
```

### WiFi模式

#### 配置WiFi参数
```c
// sys_config.h
#define WIFI_SSID         "你的WiFi名称"
#define WIFI_PASSWORD     "你的WiFi密码"
#define WIFI_TCP_PORT     8080
```

#### 启用WiFi
```c
// main.c
APP_WiFi_Init();  // 取消注释

// 主循环中
APP_WiFi_SendData(&g_system_state);  // 取消注释
```

## 🐛 常见问题快速解决

### Q: Demo模式点击按钮崩溃？
**A**: 已修复！重新编译安装即可。

### Q: 扫描不到蓝牙设备？
**A**: 
1. 检查蓝牙是否开启
2. 授予蓝牙和位置权限
3. 确保蓝牙模块通电且可被发现

### Q: 连接后无数据？
**A**:
1. 检查STM32是否正常运行
2. 检查波特率是否为115200
3. 使用串口助手测试蓝牙模块

### Q: 控制命令无响应？
**A**:
1. 确认STM32已集成app_wifi_command.c
2. 确认main.c中调用了APP_WiFi_ReceiveCommand()
3. 检查蓝牙模块是否支持双向通信

## 📊 数据格式示例

### STM32发送（每5秒）
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

### Android发送（控制命令）
```json
{"cmd":"control","fan":1}      // 开启风扇
{"cmd":"control","uv":0}       // 关闭紫外线
{"cmd":"control","light":2}    // 照明自动模式
```

## ✅ 验证清单

### 基本功能
- [ ] Demo模式可以连接
- [ ] Demo模式控制按钮不崩溃
- [ ] 可以扫描蓝牙设备
- [ ] 可以连接蓝牙设备
- [ ] 可以接收数据
- [ ] 可以发送控制命令

### 高级功能
- [ ] WiFi连接正常
- [ ] 数据持续更新
- [ ] 历史数据记录
- [ ] 报警功能正常
- [ ] 设备状态同步

## 📚 更多文档

- [PROTOCOL.md](PROTOCOL.md) - 详细通信协议
- [BLUETOOTH_GUIDE.md](BLUETOOTH_GUIDE.md) - 蓝牙使用指南
- [TESTING_GUIDE.md](TESTING_GUIDE.md) - 完整测试流程
- [VERIFICATION_CHECKLIST.md](VERIFICATION_CHECKLIST.md) - 功能验证清单
- [STM32_INTEGRATION_GUIDE.md](../Stm32-ZNYG/STM32_INTEGRATION_GUIDE.md) - STM32集成指南

## 🎯 下一步

1. **测试Demo模式**: 验证应用基本功能
2. **连接蓝牙**: 测试与实际硬件通信
3. **验证数据**: 确认数据准确性
4. **测试控制**: 验证控制命令生效
5. **长期运行**: 测试稳定性

**祝你使用愉快！** 🎉
