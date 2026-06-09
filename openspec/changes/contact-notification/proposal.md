## Why

打卡成功后通知监督人功能目前仅停留在 UI 层面（RewardBottomSheet 的按钮存在但 `onNotifySupervisor` 回调为空实现）。用户需要一个完整的通知流程：打卡后确认通知、编辑/自定义通知内容、通过邮件和短信渠道发送通知、以及在设置中管理通知模板。

## What Changes

- **通知确认界面**：点击「通知监督人」后打开一个新界面（或全屏 Dialog），展示：
  - 该习惯关联的监督人列表（区分邮箱和电话）
  - 可编辑的通知内容模板（预填充，允许修改）
  - 区分「发邮件」和「发短信」操作入口
- **邮件发送**：通过 `mailto:` Intent 拉起系统邮件应用，收件人填充该习惯的所有监督人邮箱，正文填充通知内容
- **短信发送**：通过 `smsto:` Intent 拉起系统短信应用，逐个联系人或群发
- **通知内容模板**：
  - 新增 `UserPreferences` 存储预定义模板文本（默认值包含习惯名称、打卡时间等信息）
  - 模板支持变量占位符：`{habit_name}`、`{checkin_time}`
  - 在通知确认界面可自由编辑修改
- **设置页面新增「通知模板」设置项**：可在设置中查看和编辑默认模板
- 所有操作跳转系统应用，不自行发送网络请求

## Capabilities

### New Capabilities
- `notification-confirm`: 打卡后的通知二次确认界面，展示联系人、编辑内容、选择发送方式
- `notification-template`: 通知内容模板的管理（默认模板、设置中编辑、变量替换）
- `notification-channel`: 通过系统 Intent 发送邮件和短信的具体逻辑

### Modified Capabilities
（无现有 spec 需要修改）

## Impact

- `app/src/main/java/io/github/darrindeyoung791/habitpulse/ui/screens/RewardBottomSheet.kt` — `onNotifySupervisor` 回调改为导航到确认界面
- `app/src/main/java/io/github/darrindeyoung791/habitpulse/data/preferences/UserPreferences.kt` — 新增通知模板存储
- `app/src/main/java/io/github/darrindeyoung791/habitpulse/SettingsActivity.kt` — 新增「通知模板」设置项
- `app/src/main/java/io/github/darrindeyoung791/habitpulse/viewmodel/HabitViewModel.kt` — 新增通知确认相关状态
- 新增文件：通知确认界面 composable、通知工具类
- 所有 strings.xml 需新增对应多语言字符串
