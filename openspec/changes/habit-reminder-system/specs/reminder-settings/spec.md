## ADDED Requirements

### Requirement: Settings integration
The settings screen (SettingsActivity) SHALL add a new section or group under the "Notifications" section containing:
- Reminder master switch (toggle for `REMINDER_ENABLED`)
- DND configuration (only visible when reminders are enabled)
  - DND enable/disable switch
  - DND time range sliders (start/end)

#### Scenario: Navigate to notification settings
- **WHEN** user enters Settings > Notifications
- **THEN** they see reminder toggle and DND configuration

### Requirement: ReminderSettingsActivity (sub-page)
The system SHALL provide a sub-page `ReminderSettingsActivity` accessible from the settings screen (e.g., a "提醒设置" / "Reminder Settings" list item).
The activity SHALL follow the same pattern as `AISettingsActivity`:
- `ComponentActivity` with `enableEdgeToEdge()`
- Uses `HabitPulseTheme`
- Hosts `ReminderSettingsScreen` composable

The screen SHALL display:
1. **保活状态** / **Keep-Alive Status**: Shows whether `ForegroundNotificationService` is running (using `isServiceRunning()` utility)
2. **提醒调度状态** / **Scheduling Status**: Shows next scheduled alarm time (stored in SharedPreferences or computed)
3. **发送测试通知** / **Send Test Notification**: Button that triggers a sample reminder notification (with fake habit titles like "测试习惯A", "测试习惯B")
4. **提醒统计** / **Reminder Stats**: Count of reminders sent today, total since install (stored in SharedPreferences)
5. **版本信息** / **Version Info**: Which component versions

#### Scenario: View keep-alive status
- **WHEN** user opens ReminderSettingsActivity
- **THEN** they see whether foreground service is running (green "运行中" / red "未运行")

#### Scenario: Send test notification
- **WHEN** user clicks "Send Test Notification"
- **THEN** a sample reminder notification appears in the shade
- **THEN** DND override counter resets

#### Scenario: Next alarm time is displayed
- **WHEN** reminder is enabled and alarm is scheduled
- **THEN** user can see "下一提醒: 14:30" / "Next reminder: 14:30"
- **WHEN** reminder is disabled
- **THEN** user sees "提醒已关闭" / "Reminders disabled"
