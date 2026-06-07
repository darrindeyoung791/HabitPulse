## 1. 基础数据层 — UserPreferences 扩展

- [x] 1.1 在 `UserPreferences.kt` 中添加 `REMINDER_ENABLED`、`DND_ENABLED`、`DND_START_TIME`、`DND_END_TIME` 的键定义、Flow 获取器、suspend 设置器
- [x] 1.2 添加 `nextAlarmTimeMillis`（Long?）存储下一次提醒时间，用于子页面显示
- [x] 1.3 添加 `reminderSentCount`（Int）今日已发送次数

## 2. 提醒调度引擎 — ReminderManager

- [x] 2.1 新建 `utils/ReminderManager.kt`：`object ReminderManager` 包含 `scheduleNextAlarm(context)` 方法，计算下一个 :00/:30 时间点，使用 `AlarmManager.setAlarmClock()` 设置精确闹钟
- [x] 2.2 实现 `cancelAlarm(context)` 方法取消待处理的提醒闹钟
- [x] 2.3 实现 `isAlarmScheduled(context)` 方法（通过 `AlarmManager.getNextAlarmClock()` 或 SharedPreferences 标记）
- [x] 2.4 实现 `isWithinDnd(context)` 方法：从 UserPreferences 读取 DND 配置，用 `LocalTime` 判断当前时间是否在免打扰时段内
- [x] 2.5 实现 `getNextSlotTime()` 辅助方法计算下一个 :00/:30 时间点（毫秒）

## 3. 提醒接收器 — ReminderReceiver

- [x] 3.1 新建 `receiver/ReminderReceiver.kt`：`BroadcastReceiver` 在 `onReceive()` 中处理提醒逻辑
- [x] 3.2 检查通知权限 → 无权限则仅调度下一闹钟并返回
- [x] 3.3 检查 DND → 在 DND 窗口内则调度下一闹钟并返回
- [x] 3.4 查询数据库：通过 `HabitRepository` 获取所有习惯 → 用 `SlotCheckInEngine.isApplicableToday()` 筛选当日习惯
- [x] 3.5 过滤未来1小时内有未完成时段的习惯（slotTime > now 且 slotTime <= now + 1h）
- [x] 3.6 构建并发送通知（标题、正文、点击跳转 PendingIntent）
- [x] 3.7 调度下一闹钟 + 递增已发送计数

## 4. 通知构建 — ReminderNotificationBuilder

- [x] 4.1 新建 `utils/ReminderNotificationBuilder.kt`：辅助对象，负责构建提醒通知的 `NotificationCompat.Builder`
- [x] 4.2 创建 `habit_reminder` 通知渠道（`IMPORTANCE_HIGH`）
- [x] 4.3 实现 `buildReminderNotification(context, habitTitles)`：生成标题（含计数）、正文（InboxStyle 列表）、图标、分类 CATEGORY_REMINDER
- [x] 4.4 实现 `buildPendingIntent(context)`：创建 PendingIntent 指向 `MainActivity`，附加 `EXTRA_NAVIGATE_TO = "about_to_start"`，使用 `FLAG_UPDATE_CURRENT`
- [x] 4.6 实现 `buildTestNotification(context)`：含示例习惯标题的测试通知

## 5. MainActivity 深度链接处理

- [x] 5.1 在 `MainActivity.kt` 中检查 Intent 的 `EXTRA_NAVIGATE_TO` extra
- [x] 5.2 在 `onCreate` 和 `onNewIntent` 中处理该 extra，调用 NavController 导航到 `Route.TodayHabits.createRoute("about_to_start")`
- [x] 5.3 定义 `EXTRA_NAVIGATE_TO` 常量为 `"about_to_start"`

## 6. 设置页面集成

- [x] 6.1 在 `SettingsActivity.kt` 的「通知」分类下添加提醒主开关（`REMINDER_ENABLED`），开关变化时调用 `ReminderManager.scheduleNextAlarm()` 或 `cancelAlarm()`
- [x] 6.2 添加 DND 开关和 DND 时间范围滑块 UI（MD3 Slider，步进30分钟，范围 20:00~08:00）
- [x] 6.3 添加「提醒设置」列表项，点击跳转到 `ReminderSettingsActivity`
- [x] 6.4 添加相应的字符串资源到 `values/strings.xml` 和所有语言文件夹

## 7. 子页面 — ReminderSettingsActivity + ReminderSettingsScreen

- [x] 7.1 新建 `ReminderSettingsActivity.kt`（类似 `AISettingsActivity.kt`）
- [x] 7.2 新建 `ui/screens/ReminderSettingsScreen.kt`：保活状态指示器、下一提醒时间、发送测试通知按钮、今日提醒统计
- [x] 7.3 实现 `isServiceRunning(context, serviceClass)` 工具方法用于检测前台服务状态
- [x] 7.4 发送测试通知功能
- [x] 7.5 添加 ReminderSettingsActivity 到 `AndroidManifest.xml`

## 8. 开机恢复 & AndroidManifest 注册

- [x] 8.1 在 `BootReceiver.kt` 中增加 `ReminderManager.scheduleNextAlarm(context)` 调用（前台服务启动后）
- [x] 8.2 在 `AndroidManifest.xml` 中注册 `ReminderReceiver`（`<receiver>` 标签）
- [x] 8.3 在 `HabitPulseApplication.kt` 中的应用初始化中创建 `habit_reminder` 通知渠道并调度首次提醒

## 9. 字符串资源国际化

- [x] 9.1 更新 `res/values/strings.xml`（中文）新增提醒相关字符串
- [x] 9.2 更新 `res/values-en-rUS/strings.xml`（英文）
- [x] 9.3 更新 `res/values-zh-rHK/strings.xml`（繁中香港）
- [x] 9.4 更新 `res/values-zh-rTW/strings.xml`（繁中台湾）

## 10. 测试

- [x] 10.1 运行现有单元测试确保未破坏（65项全部通过）
- [ ] 10.2 手动测试：通知触发、DND 窗口、点击跳转、开机恢复、测试通知
