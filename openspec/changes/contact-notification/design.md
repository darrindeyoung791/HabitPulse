## Context

当前 RewardBottomSheet 的 `onNotifySupervisor` 回调为空实现。应用通过 Room 数据库存储习惯及其监督人邮箱/电话，通过 DataStore (UserPreferences) 管理用户设置。通知通过跳转系统应用 (mailto:/smsto:) 实现，不自行发送网络请求。

## Goals / Non-Goals

**Goals:**
- 打卡后「通知监督人」按钮打开二次确认界面，展示监督人列表和可编辑通知内容
- 支持通过 `mailto:` Intent 发送邮件（所有邮箱为收件人）
- 支持通过 `smsto:` Intent 发送短信（逐个联系人）
- 通知内容模板可在设置中编辑，支持 `{habit_name}`、`{checkin_time}` 变量
- 设置页面新增通知模板配置项

**Non-Goals:**
- 不实现自行发送邮件/短信（依赖系统应用）
- 不改动现有联系人管理逻辑
- 不涉及后台自动通知（仅手动触发）

## Decisions

1. **全屏 Dialog 而非独立页面**：通知确认界面使用 `Dialog` 实现，因为其生命周期与 HomeScreen 绑定，无需新增 Navigation 路由。但这可能导致在手机横屏下显示不佳，因此需要专门适配。

2. **两阶段发送**：用户先编辑内容 → 点击「发送邮件」或「发送短信」→ 系统 Intent 跳转。不强制用户必须同时发送邮件和短信。

3. **模板默认值**：默认模板为 `"已完成 {habit_name} 打卡（{checkin_time}），特此通知。"`，存储在 DataStore 中，用户可在设置中修改。

4. **短信逐个发送而非群发**：`smsto:` 不支持多收件人，因此对有多个电话联系人的习惯，显示多个短信入口或合并为一条提示。

5. **确认界面位于 HomeScreen 层**：与 DatePicker 类似，对话框状态在 HomeScreen 管理，确保单一实例。

## Risks / Trade-offs

- **[风险] 系统邮件/短信应用可能未安装** → 发送前检测 `Intent.resolveActivity()`，若无则提示用户安装
- **[风险] `mailto:` 正文长度限制** → 部分邮件客户端对 `mailto:` 长度有限制（约 2000 字符），模板内容简短可避免此问题
- **[风险] 短信按条计费** → 用户通过系统短信应用发送，明确告知将使用短信功能
- **[风险] 横屏下 Dialog 显示问题** → 使用与 DatePicker 相同的 `key()` + 单独 composable 模式适配
