## 1. Data Layer — Template存储

- [x] 1.1 在 `UserPreferences.kt` 的 `PreferencesKeys` 中添加 `NOTIFICATION_TEMPLATE` key
- [x] 1.2 添加 `notificationTemplateFlow: Flow<String?>` 属性（null 表示使用默认模板）
- [x] 1.3 添加 `setNotificationTemplate(template: String)` 和 `resetNotificationTemplate()` 方法

## 2. 通知渠道工具类

- [x] 2.1 创建 `NotificationSender.kt` 工具类/方法：
  - `sendEmail(context, emails, subject, body)` — 构建 `mailto:` Intent，检测可用性
  - `sendSms(context, phone, body)` — 构建 `smsto:` Intent，检测可用性
  - 模板变量替换方法：`replaceTemplateVariables(template, habit, checkinTime)`
- [x] 2.2 处理 Intent 不可用时的 Toast 提示

## 3. 通知确认对话框

- [x] 3.1 创建 `NotificationConfirmDialog.kt` composable：
  - 全屏 Dialog
  - 展示习惯名称、所有监督人邮箱/电话（带图标）
  - 可编辑文本框预填充渲染后的模板
  - 「发送邮件」和「发短信给 %s」按钮
  - 「取消」按钮
  - 变量渲染：{habit_name} → 习惯标题, {checkin_time} → 当前时间 HH:mm, {habit_notes} → 习惯备注
- [x] 3.2 处理横屏适配
- [x] 3.3 处理多个电话联系人：列表显示，每个电话单独按钮触发短信
- [x] 3.4 处理取消/返回时的 Toast 提示（通过 Dismiss 回调）

## 4. 集成到 HabitViewModel

- [x] 4.1 添加 `notificationConfirmHabit` StateFlow 状态
- [x] 4.2 添加 `showNotificationConfirm` 布尔 StateFlow 状态
- [x] 4.3 实现 `showNotificationConfirm(habit)` 和 `dismissNotificationConfirm()` 方法
- [x] 4.4 `showNotificationConfirm()` 自动关闭奖励弹窗再打开确认弹窗

## 5. 集成到 HabitScreen 和 TodayHabitsScreen

- [x] 5.1 在 HabitScreen 中收集 `notificationConfirmHabit` 和 `showNotificationConfirm` 状态
- [x] 5.2 在 HabitScreen Scaffold 结束处添加 `NotificationConfirmDialog` 调用
- [x] 5.3 将 HabitScreen 中 RewardBottomSheet 的 `onNotifySupervisor` 改为 `viewModel.showNotificationConfirm(habit)`
- [x] 5.4 在 TodayHabitsScreen 中做同样的修改

## 6. 设置页面 — 通知模板配置

- [x] 6.1 在 `SettingsActivity.kt` 的通知部分下新增「通知模板」设置项
- [x] 6.2 实现模板编辑对话框：文本框 + 变量说明 + 保存按钮 + 重置为默认按钮
- [x] 6.3 保存时持久化到 UserPreferences，成功时显示 Toast

## 7. 多语言字符串（全部4个locale都必须添加）

⚠️ 每个string key 都必须同时在以下4个文件存在，不允许遗漏任一 locale：
- `values/strings.xml`（中文简体默认）
- `values-en-rUS/strings.xml`（英文）
- `values-zh-rHK/strings.xml`（繁体中文香港）
- `values-zh-rTW/strings.xml`（繁体中文台湾）

- [x] 7.1 新增 string resources（每个key添加到全部4个strings.xml）：
  - `notification_confirm_title` — 通知监督人 / Notify Supervisors
  - `notification_confirm_send_email` — 发送邮件 / Send Email
  - `notification_confirm_send_sms` — 发短信给 %s / Send SMS to %s
  - `notification_confirm_cancel` — 取消 / Cancel
  - `notification_confirm_no_email_app` — 未找到邮件应用 / No email app found
  - `notification_confirm_no_sms_app` — 未找到短信应用 / No SMS app found
  - `notification_template_settings_title` — 通知模板 / Notification Template
  - `notification_template_settings_desc` — 自定义通知内容 / Customize notification message
  - `notification_template_label` — 通知内容模板 / Message Template
  - `notification_template_variable_hint` — 可用变量：{habit_name} 习惯名称, {checkin_time} 打卡时间, {habit_notes} 习惯备注
    / Available variables: {habit_name} habit name, {checkin_time} check-in time, {habit_notes} habit notes
  - `notification_template_save_success` — 模板已保存 / Template saved
  - `notification_template_reset` — 重置为默认 / Reset to Default
  - `notification_confirm_dismiss_toast` — 记得手动通知监督人哦 / Remember to notify your supervisors
  - `notification_confirm_no_contacts` — 暂无监督人可通知 / No supervisors to notify
  - `notification_email_subject` — 关于 {habit_name} 的打卡通知 / Check-in notification for {habit_name}
  - `notification_default_template` — 已完成 {habit_name} 打卡（{checkin_time}），特此通知。
    / Completed {habit_name} check-in ({checkin_time}), just so you know.
