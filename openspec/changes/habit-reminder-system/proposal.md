## Why

习惯打卡提醒是习惯追踪应用的核心功能。当前 HabitPulse 虽有前台保活服务和状态计算引擎，但缺乏主动提醒机制——用户在习惯即将开始时不会收到通知。每30分钟提醒未来一小时内即将开始的习惯，能确保用户有充足时间响应，避免"10:00提醒10:01习惯"导致来不及的问题。

## What Changes

- 新增 AlarmManager + BroadcastReceiver 定时提醒系统，每30分钟（整点和半点）触发一次
- 提醒通知包含：标题（即将开始的习惯数量）+ 正文（习惯列表）
- 点击通知跳转到「即将开始」详情页面（与 HomeScreen EntryZone 点开的 TodayHabitsScreen 一致）
- 新增免打扰（DND）功能：可配置起始/结束时间，30分钟步进滑块，默认 22:00~07:00
- 新增提醒设置项到设置页面的「通知」分类下
- 新增子页面（类似 AISettingsActivity）用于检查保活状态和发送示例通知
- 开机重启后自动恢复定时提醒（BootReceiver 扩展）
- 防止用户误操作导致全天不被提醒的防护措施

## Capabilities

### New Capabilities
- `reminder-scheduling`: 使用 AlarmManager 每30分钟精确触发提醒，含开机恢复
- `reminder-notification`: 构建并发送包含即将开始习惯列表的通知，含点击跳转
- `dnd-config`: 免打扰时段配置（UI + 存储 + 运行时检查）
- `reminder-settings`: 设置页面下的提醒开关、DND配置、保活状态子页面

### Modified Capabilities
- (none — 首次引入提醒系统，无现有 spec 发生需求变更)

## Impact

- 新增文件：`ReminderReceiver.kt`（BroadcastReceiver）、`ReminderSettingsActivity.kt`、`ReminderSettingsScreen.kt`、`ReminderManager.kt`（调度逻辑）
- 修改文件：`UserPreferences.kt`（新增 DND 和提醒开关配置）、`AndroidManifest.xml`（注册新 Receiver 和 Activity）、`BootReceiver.kt`（开机恢复提醒）、`SettingsActivity.kt`（新增提醒设置项）
- 新增通知渠道：`reminder_channel`（高优先级，用于提醒通知）
- 依赖无新增
