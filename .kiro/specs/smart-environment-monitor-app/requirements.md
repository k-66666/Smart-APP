# Requirements Document

## Introduction

智能环境监测APP是一个Android应用程序，用于通过无线通信模块（蓝牙或WiFi）连接和控制基于单片机的智能环境监测系统。该系统能够实时监测环境参数（温度、湿度、空气质量、光照强度），并允许用户远程控制输出模块、查看历史数据、设置报警阈值。

## Glossary

- **App**: 智能环境监测Android应用程序
- **MCU_System**: 单片机智能环境监测系统（包含传感器、单片机、输出模块）
- **Wireless_Module**: 无线通信模块（蓝牙或WiFi）
- **Connection_Manager**: 连接管理器，负责建立和维护与单片机的通信
- **Sensor_Data**: 传感器数据，包括温度、湿度、空气质量、光照强度
- **Data_Visualizer**: 数据可视化组件
- **Alarm_Manager**: 报警管理器
- **Control_Command**: 控制命令，用于远程控制单片机输出模块
- **Data_Storage**: 数据存储模块
- **Notification_Service**: 通知服务

## Requirements

### 需求 1: 无线连接建立

**用户故事:** 作为用户，我希望能够通过蓝牙或WiFi连接到单片机系统，以便开始监测和控制环境参数。

#### 验收标准

1. WHEN 用户启动App, THE Connection_Manager SHALL 扫描可用的MCU_System设备
2. WHEN 用户选择一个MCU_System设备, THE Connection_Manager SHALL 在5秒内建立连接
3. IF 连接失败, THEN THE Connection_Manager SHALL 显示错误信息并提供重试选项
4. WHILE 连接已建立, THE Connection_Manager SHALL 每30秒发送心跳包以维持连接
5. WHEN 连接断开, THE App SHALL 显示断开通知并尝试自动重连最多3次

### 需求 2: 实时传感器数据接收

**用户故事:** 作为用户，我希望实时查看传感器数据，以便了解当前的环境状况。

#### 验收标准

1. WHILE 连接已建立, THE App SHALL 每2秒接收一次Sensor_Data
2. WHEN 接收到Sensor_Data, THE App SHALL 在500毫秒内更新显示界面
3. THE App SHALL 显示温度值（精度0.1°C，范围-40°C至80°C）
4. THE App SHALL 显示湿度值（精度1%，范围0%至100%）
5. THE App SHALL 显示空气质量指数（范围0至500）
6. THE App SHALL 显示光照强度值（单位Lux，范围0至100000）
7. IF 接收到的Sensor_Data格式无效, THEN THE App SHALL 记录错误并保持显示上一次有效数据

### 需求 3: 传感器数据显示

**用户故事:** 作为用户，我希望以清晰直观的方式查看传感器数据，以便快速理解环境状况。

#### 验收标准

1. THE App SHALL 在主界面显示所有四种传感器的当前数值
2. THE App SHALL 为每种传感器数据使用不同的图标和颜色标识
3. WHEN 传感器数值超过用户设定的阈值, THE App SHALL 以红色高亮显示该数值
4. THE App SHALL 显示每个传感器数据的最后更新时间戳
5. WHEN 数据超过10秒未更新, THE App SHALL 显示"数据过期"警告

### 需求 4: 远程控制输出模块

**用户故事:** 作为用户，我希望远程控制单片机的输出模块，以便根据需要调整系统行为。

#### 验收标准

1. THE App SHALL 提供控制显示模块开关的功能
2. THE App SHALL 提供控制声光报警模块开关的功能
3. THE App SHALL 提供控制驱动模块开关的功能
4. WHEN 用户发送Control_Command, THE App SHALL 在1秒内将命令发送到MCU_System
5. WHEN MCU_System确认执行Control_Command, THE App SHALL 更新控制界面状态
6. IF Control_Command发送失败, THEN THE App SHALL 显示错误信息并提供重试选项
7. WHILE 连接未建立, THE App SHALL 禁用所有控制按钮

### 需求 5: 历史数据存储

**用户故事:** 作为用户，我希望查看历史环境数据，以便分析环境变化趋势。

#### 验收标准

1. WHEN 接收到Sensor_Data, THE Data_Storage SHALL 将数据保存到本地数据库
2. THE Data_Storage SHALL 保存每条Sensor_Data的时间戳
3. THE Data_Storage SHALL 保留最近30天的历史数据
4. WHEN 历史数据超过30天, THE Data_Storage SHALL 自动删除最旧的数据
5. THE Data_Storage SHALL 支持按日期范围查询历史数据
6. THE Data_Storage SHALL 支持按传感器类型查询历史数据

### 需求 6: 数据可视化

**用户故事:** 作为用户，我希望以图表形式查看历史数据，以便直观地分析环境变化趋势。

#### 验收标准

1. THE Data_Visualizer SHALL 提供折线图显示温度历史数据
2. THE Data_Visualizer SHALL 提供折线图显示湿度历史数据
3. THE Data_Visualizer SHALL 提供折线图显示空气质量历史数据
4. THE Data_Visualizer SHALL 提供折线图显示光照强度历史数据
5. THE Data_Visualizer SHALL 支持选择时间范围（最近1小时、6小时、24小时、7天、30天）
6. THE Data_Visualizer SHALL 支持缩放和平移图表
7. WHEN 用户点击图表上的数据点, THE Data_Visualizer SHALL 显示该时刻的详细数值

### 需求 7: 报警阈值设置

**用户故事:** 作为用户，我希望设置环境参数的报警阈值，以便在环境异常时及时收到通知。

#### 验收标准

1. THE App SHALL 允许用户为温度设置上限和下限阈值
2. THE App SHALL 允许用户为湿度设置上限和下限阈值
3. THE App SHALL 允许用户为空气质量设置上限阈值
4. THE App SHALL 允许用户为光照强度设置上限和下限阈值
5. THE App SHALL 验证阈值输入的有效性（上限必须大于下限）
6. WHEN 用户保存阈值设置, THE App SHALL 将设置持久化到本地存储
7. WHEN App重启, THE App SHALL 加载上次保存的阈值设置

### 需求 8: 报警通知

**用户故事:** 作为用户，我希望在环境参数超过阈值时收到通知，以便及时采取措施。

#### 验收标准

1. WHEN Sensor_Data超过用户设定的阈值, THE Alarm_Manager SHALL 触发报警
2. WHEN 报警触发, THE Notification_Service SHALL 发送系统通知
3. THE Notification_Service SHALL 在通知中显示超标的传感器类型和当前数值
4. THE Notification_Service SHALL 支持通知声音和振动
5. THE App SHALL 允许用户启用或禁用报警通知功能
6. WHEN 同一传感器持续超标, THE Alarm_Manager SHALL 每5分钟最多发送一次通知
7. WHEN 传感器数值恢复正常, THE Alarm_Manager SHALL 发送恢复正常的通知

### 需求 9: 数据导出

**用户故事:** 作为用户，我希望导出历史数据，以便在其他工具中进行进一步分析。

#### 验收标准

1. THE App SHALL 提供导出历史数据为CSV格式的功能
2. THE App SHALL 允许用户选择导出的日期范围
3. THE App SHALL 允许用户选择导出的传感器类型
4. WHEN 用户确认导出, THE App SHALL 在5秒内生成CSV文件
5. THE App SHALL 将导出的CSV文件保存到设备的下载目录
6. WHEN 导出完成, THE App SHALL 显示成功消息并提供打开文件的选项
7. THE CSV文件 SHALL 包含时间戳、传感器类型、数值三列数据

### 需求 10: 设备管理

**用户故事:** 作为用户，我希望管理已配对的设备，以便快速连接到常用的单片机系统。

#### 验收标准

1. THE App SHALL 保存已成功连接过的MCU_System设备信息
2. THE App SHALL 显示已保存设备的列表（包括设备名称和最后连接时间）
3. THE App SHALL 允许用户从列表中选择设备进行快速连接
4. THE App SHALL 允许用户删除已保存的设备信息
5. THE App SHALL 允许用户为已保存的设备设置别名
6. WHEN 用户启动App且存在已保存设备, THE App SHALL 提示用户是否连接到上次使用的设备

### 需求 11: 通信协议解析

**用户故事:** 作为开发者，我希望正确解析单片机发送的数据协议，以便准确获取传感器数据和控制反馈。

#### 验收标准

1. THE App SHALL 定义与MCU_System通信的数据帧格式
2. WHEN 接收到数据帧, THE App SHALL 验证帧头和校验和
3. IF 数据帧校验失败, THEN THE App SHALL 丢弃该帧并请求重传
4. THE App SHALL 解析数据帧中的传感器数据字段
5. THE App SHALL 解析数据帧中的设备状态字段
6. WHEN 发送Control_Command, THE App SHALL 按照协议格式封装命令帧
7. THE App SHALL 为每个命令帧计算并添加校验和

### 需求 12: 用户界面响应性

**用户故事:** 作为用户，我希望应用界面流畅响应，以便获得良好的使用体验。

#### 验收标准

1. THE App SHALL 在主线程之外执行所有网络通信操作
2. THE App SHALL 在主线程之外执行所有数据库操作
3. WHEN 执行耗时操作, THE App SHALL 显示加载指示器
4. THE App SHALL 在100毫秒内响应用户的触摸操作
5. WHEN 切换界面, THE App SHALL 在300毫秒内完成界面过渡动画
6. THE App SHALL 支持横屏和竖屏显示模式
7. WHEN 屏幕旋转, THE App SHALL 保持当前数据状态

### 需求 13: 错误处理和日志

**用户故事:** 作为开发者，我希望应用能够妥善处理错误并记录日志，以便调试和改进应用。

#### 验收标准

1. WHEN 发生网络错误, THE App SHALL 记录错误类型和时间戳到日志
2. WHEN 发生数据解析错误, THE App SHALL 记录原始数据和错误信息到日志
3. THE App SHALL 提供查看应用日志的功能（仅在调试模式下）
4. THE App SHALL 限制日志文件大小不超过10MB
5. WHEN 日志文件超过10MB, THE App SHALL 删除最旧的日志条目
6. IF 发生未捕获的异常, THEN THE App SHALL 记录异常堆栈并显示友好的错误提示
7. THE App SHALL 提供导出日志文件的功能（用于问题报告）

### 需求 14: 权限管理

**用户故事:** 作为用户，我希望应用只请求必要的权限，以便保护我的隐私和安全。

#### 验收标准

1. WHERE 使用蓝牙连接, THE App SHALL 请求蓝牙权限
2. WHERE 使用WiFi连接, THE App SHALL 请求WiFi权限
3. THE App SHALL 请求通知权限以发送报警通知
4. THE App SHALL 请求存储权限以导出数据文件
5. WHEN 用户拒绝必要权限, THE App SHALL 显示权限说明并引导用户到设置页面
6. THE App SHALL 在首次启动时显示权限使用说明
7. THE App SHALL 仅在需要使用功能时请求对应权限（运行时权限）

### 需求 15: 应用设置

**用户故事:** 作为用户，我希望自定义应用设置，以便根据个人偏好使用应用。

#### 验收标准

1. THE App SHALL 提供设置数据刷新间隔的选项（1秒、2秒、5秒、10秒）
2. THE App SHALL 提供设置温度单位的选项（摄氏度或华氏度）
3. THE App SHALL 提供设置主题的选项（浅色、深色、跟随系统）
4. THE App SHALL 提供启用或禁用自动重连的选项
5. THE App SHALL 提供设置历史数据保留天数的选项（7天、15天、30天、60天）
6. WHEN 用户修改设置, THE App SHALL 立即应用新设置
7. WHEN App重启, THE App SHALL 加载上次保存的设置
