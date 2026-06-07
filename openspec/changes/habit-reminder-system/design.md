## Context

HabitPulse 已有完整的前台保活服务（`ForegroundNotificationService`）、习惯状态计算引擎（`SlotCheckInEngine`）和 Room 数据库。用户创建习惯时可设置提醒时间（`reminderTimes`），但当前缺乏主动通知机制。`HabitStatus.ABOUT_TO_START` 已在 UI 中展示，但用户需要打开应用才能看到。

## Goals / Non-Goals

**Goals:**
- 每30分钟（整点和半点）通过 AlarmManager 精确触发提醒通知
- 通知标题显示即将开始的习惯数量，正文列出习惯名称
- 点击通知跳转到 TodayHabitsScreen（about_to_start 过滤）—— 与 EntryZone 点击行为一致
- 可配置免打扰时段（DND），默认 22:00~07:00，步进30分钟
- 设置页面新增提醒开关和 DND 配置
- 子页面（ReminderSettingsActivity）检查保活状态、发送示例通知
- 开机后自动恢复提醒调度
- 防止用户误操作关闭提醒导致全天收不到通知的防护

**Non-Goals:**
- 不修改现有的 SlotCheckInEngine 状态计算逻辑
- 不引入新的依赖库
- 不修改现有的 ForegroundNotificationService

## Decisions

### Decision 1: 独立 AlarmManager + BroadcastReceiver（不依赖前台服务）
- **选择**：`AlarmManager.setAlarmClock()` 设置每30分钟精确闹钟，`ReminderReceiver`（BroadcastReceiver）处理触发逻辑
- **理由**：前台服务在 Android 14+ 限制严格（必须声明 foregroundServiceType），且保活服务不应承担业务逻辑。独立 Receiver 更清晰、可靠
- **替代方案**：WorkManager（最小间隔15分钟，不满足精确30秒需求）；Handler 循环（进程被杀后失效）

### Decision 2: 防止全天误关闭机制
- **选择**：用 SharedPreferences 计数器 `dndOverrideCount` —— 每次用户主动关闭提醒或进入 DND 窗口时累加。若连续 N 次提醒被抑制（比如5次 = 2.5小时），发送一条"你可能错过了提醒"的通知并重置计数器。用户手动发送测试通知或应用重启也重置计数器
- **理由**：用户可能误关闭通知权限或 DND 设置不当导致全天无提醒。主动检测比被动等待用户反馈好

### Decision 3: 通知渠道设计
- **选择**：新建 `CHANNEL_ID = "habit_reminder"`，重要性 `IMPORTANCE_HIGH`（有声音和震动）
- **理由**：提醒通知需要引起用户注意，不应与低优先级的保活通知混用同一渠道

### Decision 4: DND 配置存储
- **选择**：使用现有的 `UserPreferences`（DataStore）存储 DND 开关、起始/结束时间
- **理由**：DataStore 已是项目标准方案，保持一致

### Decision 5: 子页面实现
- **选择**：`ReminderSettingsActivity`（ComponentActivity），与 AISettingsActivity 模式一致
- **内容**：保活状态指示器（前台服务运行中/否）、提醒调度状态（下一触发时间）、发送示例通知按钮、累计提醒统计

### Decision 6: 开机恢复
- **选择**：扩展现有 `BootReceiver`，在启动前台服务后额外调用 `ReminderManager.scheduleReminders(context)`
- **理由**：避免新增 Receiver，利用已有基础设施

## Risks / Trade-offs

- [Risk] `AlarmManager.setAlarmClock()` 在部分厂商 ROM 上可能被延迟 → 接受，这是跨厂商最佳方案
- [Risk] 用户关闭通知权限后提醒仍触发但用户看不到 → 在 Receiver 入口检测权限并跳过
- [Risk] DND 跨天场景（如 22:00~07:00）逻辑易出错 → 使用 `LocalTime` 比较，跨天时按 `isAfter` 判断
- [Risk] 每30分钟触发增加电池消耗 → `setAlarmClock()` 是系统级精确闹钟，单次触发开销极小
