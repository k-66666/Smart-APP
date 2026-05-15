# 快速入门指南

## 🚀 5分钟快速开始

### 1. 打开项目

在Android Studio中打开项目：
```
File > Open > 选择 MySmart 文件夹
```

### 2. 同步Gradle

等待Gradle自动同步完成（右下角会显示进度）。如果没有自动同步：
```
File > Sync Project with Gradle Files
```

### 3. 准备设备

**选项A: 使用真实Android设备（推荐）**
1. 在设备上启用开发者选项
2. 启用USB调试
3. 用USB线连接到电脑
4. 在设备上授权USB调试

**选项B: 使用Android模拟器**
1. Tools > Device Manager
2. 创建新设备（推荐Pixel 6, API 36）
3. 启动模拟器

### 4. 运行应用

点击工具栏的绿色运行按钮 ▶️ 或按 `Shift + F10`

### 5. 授予权限

首次启动时，应用会请求以下权限：
- ✅ 蓝牙连接权限
- ✅ 蓝牙扫描权限
- ✅ 位置权限（蓝牙扫描需要）
- ✅ 通知权限

**全部点击"允许"**

### 6. 配对蓝牙设备

在使用应用之前，需要先在系统设置中配对蓝牙设备：

1. 打开设备的 **设置 > 蓝牙**
2. 确保蓝牙已开启
3. 扫描并配对你的单片机设备
4. 记住设备名称

### 7. 连接设备

1. 在应用中点击 **"连接设备"** 按钮
2. 从列表中选择你的设备
3. 等待连接成功（状态显示"已连接"）

### 8. 查看数据

连接成功后，应用会自动显示：
- 🌡️ 温度
- 💧 湿度
- 🌫️ 空气质量
- ☀️ 光照强度

数据每2秒更新一次。

### 9. 控制模块

使用底部的开关控制输出模块：
- **显示模块**: 控制显示屏开关
- **声光报警**: 控制报警器开关
- **驱动模块**: 控制驱动器开关

---

## 🔧 故障排除

### 问题1: 无法扫描到设备

**解决方案:**
1. 确认蓝牙已开启
2. 确认设备已在系统设置中配对
3. 检查是否授予了所有权限
4. 重启应用

### 问题2: 连接失败

**解决方案:**
1. 确认设备在蓝牙范围内（<10米）
2. 确认设备未被其他应用占用
3. 尝试在系统设置中取消配对，然后重新配对
4. 重启蓝牙

### 问题3: 数据不更新

**解决方案:**
1. 检查连接状态是否为"已连接"
2. 确认单片机正在发送数据
3. 检查数据更新时间
4. 尝试断开并重新连接

### 问题4: Gradle同步失败

**解决方案:**
1. 检查网络连接
2. 清理项目: `Build > Clean Project`
3. 重新构建: `Build > Rebuild Project`
4. 删除 `.gradle` 文件夹并重新同步

---

## 📱 测试模式（无硬件）

如果你没有实际的单片机设备，可以使用模拟数据进行测试：

### 创建模拟数据生成器

在 `MainActivity.java` 的 `onCreate` 方法末尾添加：

```java
// 仅用于测试 - 生成模拟数据
if (BuildConfig.DEBUG) {
    startMockDataGeneration();
}

private void startMockDataGeneration() {
    new Handler().postDelayed(new Runnable() {
        @Override
        public void run() {
            // 生成随机传感器数据
            SensorData mockData = new SensorData(
                20 + (float)(Math.random() * 10),  // 温度 20-30°C
                50 + (float)(Math.random() * 20),  // 湿度 50-70%
                50 + (int)(Math.random() * 100),   // 空气质量 50-150
                500 + (int)(Math.random() * 1000)  // 光照 500-1500 Lux
            );
            
            updateSensorData(mockData);
            
            // 每2秒生成一次
            new Handler().postDelayed(this, 2000);
        }
    }, 1000);
}
```

---

## 🎯 下一步

现在你已经成功运行了应用！接下来可以：

1. **查看代码**: 了解MVVM架构的实现
2. **修改UI**: 自定义界面样式和布局
3. **添加功能**: 实现历史数据查询和图表显示
4. **测试协议**: 验证与单片机的通信协议

---

## 📚 相关文档

- [README.md](README.md) - 完整项目说明
- [design.md](.kiro/specs/smart-environment-monitor-app/design.md) - 详细设计文档
- [requirements.md](.kiro/specs/smart-environment-monitor-app/requirements.md) - 需求文档
- [DEVELOPMENT_LOG.md](.kiro/specs/smart-environment-monitor-app/DEVELOPMENT_LOG.md) - 开发日志

---

## 💡 提示

- 保持设备在蓝牙范围内
- 定期清理旧数据以节省存储空间
- 使用真实设备测试蓝牙功能（模拟器蓝牙支持有限）
- 查看Logcat了解详细的运行日志

---

## 🆘 需要帮助？

如果遇到问题：
1. 查看 [故障排除](#-故障排除) 部分
2. 检查 Logcat 日志（标签: BluetoothConnMgr, SensorRepository）
3. 查看 [README.md](README.md) 中的详细说明

祝你使用愉快！🎉
