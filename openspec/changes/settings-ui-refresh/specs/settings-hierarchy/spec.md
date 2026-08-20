## ADDED Requirements

### Requirement: New settings first screen is a category hub
The new settings first screen SHALL display only category entries and MUST NOT contain any fine-grained switches (e.g., 保持后台运行, 支持 HabitPulse). It SHALL present the categories AI 配置, 连接与同步, 通知, 通用, 关于, and 帮助和反馈 in that order.

#### Scenario: First screen lists categories only
- **WHEN** the user opens the new settings first screen
- **THEN** they see the categories AI 配置, 连接与同步, 通知, 通用, 关于, 帮助和反馈 and no switches

#### Scenario: No switch appears on the first screen
- **WHEN** the first screen is composed
- **THEN** no `Switch` control is rendered on it

### Requirement: Category navigation
Each category entry SHALL navigate to its sub-page when tapped: AI 配置 → AI 配置 page; 通知 → 通知 page; 通用 → 通用 page; 关于 → 关于 page. 连接与同步 SHALL be a disabled placeholder and 帮助和反馈 SHALL open the help WebView directly.

#### Scenario: Tapping a category opens its sub-page
- **WHEN** the user taps AI 配置, 通知, 通用, or 关于
- **THEN** the corresponding sub-page is displayed

#### Scenario: 帮助和反馈 opens help directly
- **WHEN** the user taps 帮助和反馈
- **THEN** the app opens the help & feedback WebView (HELP_URL) directly without an intermediate page

### Requirement: Appbar help entry
The new settings first screen SHALL keep a help icon in the TopAppBar that opens the help & feedback WebView (HELP_URL), in addition to the 帮助和反馈 list entry.

#### Scenario: Appbar help icon opens help
- **WHEN** the user taps the help icon in the TopAppBar
- **THEN** the help & feedback WebView opens

### Requirement: AI 配置 sub-page
The AI 配置 sub-page SHALL contain the entries 配置提供商 (navigates to provider configuration) and 记忆 (disabled placeholder).

#### Scenario: AI page navigates to provider config
- **WHEN** the user taps 配置提供商
- **THEN** the provider configuration page opens (API endpoint, API key, model, streaming response, test connection)

#### Scenario: Memory entry is a placeholder
- **WHEN** the AI 配置 sub-page renders
- **THEN** 记忆 is disabled with "即将推出" and does not respond to taps

### Requirement: 通知 sub-page
The 通知 sub-page SHALL contain 保持后台运行, 习惯提醒, and 通知模板 entries. 保持后台运行 SHALL be a switch when notification permission is granted and an authorization row when it is not. 习惯提醒 SHALL navigate to the habit-reminders page, and 通知模板 SHALL navigate to the notification-template page.

#### Scenario: Keep-alive switch with permission granted
- **WHEN** the user has notification permission and opens the 通知 sub-page
- **THEN** 保持后台运行 renders as a switch that starts/stops the foreground keep-alive service

#### Scenario: Keep-alive authorization row without permission
- **WHEN** the user has not granted notification permission
- **THEN** 保持后台运行 renders as a row with an authorization action that requests permission or opens app settings, and the existing limited-mode warning flow is preserved

#### Scenario: Reminder entry navigates
- **WHEN** the user taps 习惯提醒
- **THEN** the habit-reminders page opens (master switch, do-not-disturb, status, test notifications)

#### Scenario: Template entry navigates
- **WHEN** the user taps 通知模板
- **THEN** the notification-template page opens with template editing and reset

### Requirement: 通用 sub-page
The 通用 sub-page SHALL contain 空间清理 (navigates to storage cleanup) and 界面与显示 (disabled placeholder, renamed from 视觉).

#### Scenario: Storage cleanup navigates
- **WHEN** the user taps 空间清理
- **THEN** the storage cleanup page opens with clear-cache and deep-clean actions

#### Scenario: Visual entry is a placeholder
- **WHEN** the 通用 sub-page renders
- **THEN** the entry is labeled 界面与显示, is disabled with "即将推出", and does not respond to taps

### Requirement: 关于 sub-page
The 关于 sub-page SHALL contain 关于 (navigates to the about detail page), 支持 HabitPulse (switch controlling the splash ad, disabled when TalkBack is on), 查看应用信息 (opens system app-info settings), and 查看GitHub (opens the GitHub WebView).

#### Scenario: About detail navigates
- **WHEN** the user taps 关于
- **THEN** the about detail page opens with privacy notice, app version, developer, and open-source licenses

#### Scenario: Support switch controls splash ad
- **WHEN** the user toggles 支持 HabitPulse
- **THEN** the splash-ad preference updates and, when TalkBack is enabled, the switch is disabled with an explanatory message

#### Scenario: App info opens system settings
- **WHEN** the user taps 查看应用信息
- **THEN** the system application-details settings screen for this app opens

#### Scenario: GitHub opens web view
- **WHEN** the user taps 查看GitHub
- **THEN** the GitHub repository opens in the in-app WebView

### Requirement: Internal navigation and back behavior
All new settings pages SHALL live in a single new Activity with internal Compose navigation. Back navigation SHALL walk up the internal page stack and, at the first screen, exit the Activity; predictive back SHALL be supported.

#### Scenario: Back from a sub-page returns to first screen
- **WHEN** the user is on a sub-page and presses back
- **THEN** the previous page in the settings stack is shown, ultimately returning to the first screen

#### Scenario: Back from first screen exits settings
- **WHEN** the user is on the new settings first screen and presses back
- **THEN** the new settings Activity is exited

### Requirement: Legacy settings entry point
The legacy `SettingsActivity` SHALL remain fully functional and SHALL add a single entry at the very bottom that opens the new settings Activity.

#### Scenario: Legacy bottom entry opens new settings
- **WHEN** the user scrolls to the bottom of the legacy settings screen and taps the new-settings entry
- **THEN** the new settings Activity opens

#### Scenario: Legacy settings behavior unchanged
- **WHEN** the user uses the legacy settings screen
- **THEN** all existing entries and dialogs behave as before, with only the new bottom entry added
