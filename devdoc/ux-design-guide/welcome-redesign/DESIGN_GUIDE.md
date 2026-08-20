# HabitPulse 欢迎页重设计 · 完整设计指南（DESIGN GUIDE）

> 本文件是 `WelcomeActivity` 重构的**权威需求与设计规格**，聚合了历轮评审的全部结论。
> 配套可交互原型：`portrait.html`（核心）、`index.html`（总览）、`README.md`（交互说明）。
> 状态：**设计已定稿，待实施**。目标分支：`settings-ui-refresh`。

---

## 1. 目标与范围

把旧的欢迎页（单页合并版 + 3 步 AI 向导）重构为**新版设置风格**的优雅引导流程，风格对齐 `NewSettings*`（Material 3 分段列表 + seed 色板 + 大圆角按钮 + 逐层进入动画）。

### 范围（In Scope）
- 新的引导流程：**欢迎 → 权限声明 → 通知设置 → 完成**（3 步 + 完成页）
- 复用新版设置公共组件与样式规范
- 设备形态自适应（手机竖屏 / 手机横屏 / 平板）
- 逐层进入动画（全部屏幕）
- 图标一律使用 Material Icons / Canvas 绘制，**严禁 emoji**

### 范围外（Out of Scope）
- AI 配置步骤**移除**（引导完成后到「设置 → AI 配置」里配置）
- 通知提醒系统（AlarmManager）实现
- 真正的多列/大屏布局（首版仅做到"内容一致、更宽"）

---

## 2. 流程结构（4 屏）

| 屏 | 内容 | 底部操作 |
|----|------|---------|
| **1 欢迎** | 仅两行文本（标题 + 一句简介），**无 Logo、无图标、无权限行** | 单个「继续」主按钮 |
| **2 权限声明** | 两条权限图标行 + 隐私政策/服务条款占位链接 | 「我同意，继续」主按钮 + 「不同意」次按钮 |
| **3 通知设置** | 提醒 / 免打扰（内嵌时段滑块）/ 常驻通知 分段开关 | 「完成」主按钮 + 「跳过，稍后设置」次按钮 |
| **4 完成** | 绿色对勾图形 + 「一切就绪」+ 一句鼓励语。**不展示任何用户设置信息** | 「进入 HabitPulse」主按钮 |

- 顶部为**三点线性步骤指示器** + 右侧「1/3」「2/3」「3/3」「完成」标签；步骤 2 起左侧返回箭头。
- 前进路径：`继续`(1→2) → `我同意，继续`(2→3) → `完成`(3→4) → `进入`(4→主界面)。
- 「不同意」：弹出**确认对话框**，告知「不同意将只能退出应用」；提供「取消」（返回权限声明页）与「退出应用」两个操作。**引导流程不再提供受限模式入口**（受限模式仅作为历史用户设置页的兼容能力保留，`enterLimitedMode` 不再在引导中调用）。
- 「跳过，稍后设置」与「完成」行为等价（都写入当前偏好后进入主界面）。
- 隐私政策/服务条款为**占位链接**，点击提示「即将上线」，未来接入真实文档。

---

## 3. 视觉风格规范

### 3.1 整体
- 背景沿用应用默认背景（`surface`），无额外装饰。
- 内容垂直居中（竖屏/平板）；手机横屏顶部对齐可滚动。
- 所有圆角/间距/字体遵循应用既有 Material 3 主题与字体缩放（`HabitPulseTheme`，含应用内字体大小 `FONT_SCALE`）。

### 3.2 分段列表（遵循 `listitem-style.md`）
- 行底色 `surfaceContainer`，**行间无分隔线**，行高内边距 16px（原型中为 16dp 上下内边距）。
- 组合圆角：组边缘 **16dp**、组内部行间 **4dp**。
- 图标 chip：**40dp 圆形**，seed 色板着底色。
- 列表项 = 图标 chip + 两行文本（标题 + 副标题），**不可点击**（`SettingsSegmentedItem` 的展示形态）；开关行 = 图标 chip + 两行文本 + 右侧开关（`SettingsSegmentedSwitch`）；滑块 = 列表下方展开的嵌入块（`SettingsSegmentedBox`）。
- 开关默认值：**提醒=开、免打扰=开、常驻通知=开**（常驻通知由旧的默认关闭改为**默认开启**）。
- 联动：关闭「提醒」→「免打扰」联动关闭并禁用（整行降透明度 0.45）；「免打扰」开 → 展开时段滑块。

### 3.3 图标 chip 色板（遵循 `custom-color-scheme.md` 的 seed 色板）
| 项 | seed | 图标（Material Icons，禁 emoji） |
|----|------|------|
| 权限·发送持久通知 | 蓝(0) | `Icons.Outlined.Notifications` |
| 权限·在后台持续运行 | 紫(3) | `Icons.Outlined.Autorenew`（旧实现为 `SettingsBackupRestore`，以原型为准为"循环"语义） |
| 设置·提醒 | 蓝(0) | `Icons.Outlined.NotificationsActive`（原型用闹钟图标，实现时选语义匹配的图标） |
| 设置·免打扰 | 青(5) | `Icons.Outlined.DoNotDisturbOn`（原型用月亮/半圆图标，实现时选语义匹配的图标） |
| 设置·常驻通知 | 绿(1) | `Icons.Outlined.NotificationsActive` / `NotificationImportant`（常驻 = 可靠，选语义匹配图标） |

> 说明：原型中图标为占位示意。**落地时统一从 `Icons.Outlined.*` 选择语义最贴合的矢量图标**，若语义不满足再由 UI 层 `Canvas` 绘制（见 §6 图标/形状原则）。

### 3.4 按钮
- 主按钮：`Button`，全宽，高约 50-54dp，圆角 `MaterialTheme.shapes.medium`（大圆角、Pixel 风格），`titleMedium` + SemiBold。
- 次按钮：`TextButton`，高 44dp，`bodyMedium`，`onSurfaceVariant`。
- 底部按钮区与内容区分离：长内容滚动、按钮固定/随底（原型中 step-bottom 独立于 step-body）。

### 3.5 步骤指示器
- 三点：当前与已完成为实心（线性进度：`i <= currentStep` 即点亮，原型实现为 `i < currentStep`，即第 N 步时点亮前 N 个点），未到为空心。
- 右侧标签「1/3」等。
- 完成页标签为「完成」。

### 3.6 不同意确认对话框
- MD3 `AlertDialog`（主题 `colorScheme` 配色，圆角 28dp 量级）。
- 标题：「不同意将无法使用」；正文：「HabitPulse 需要你的同意才能正常提供提醒功能。如果你不同意，将只能退出应用。」
- 操作：「取消」（返回权限声明页）+「退出应用」（`finish()` 退出应用）。
- 对话框同样遵循逐层/淡入动画（原型 `riseIn`），不使用 emoji 图标。

---

## 4. 设备形态自适应

统一使用 `rememberDeviceFormInfo()`（`ui/DeviceFormFactor.kt`），**禁止**页面自行读 `LocalConfiguration` / `smallestScreenWidthDp` / `screenWidthDp` / `orientation` 判定形态。

### 4.1 三种形态（首版策略：同内容、仅宽度不同）
| 形态 | 判定 | 内容 | 垂直对齐 |
|------|------|------|---------|
| 手机竖屏 | `!isLandscape` | 单列，内容最大宽 **360dp** | 居中 |
| 手机横屏 | `isPhoneLandscape` | 与竖屏完全一致，内容最大宽 **460dp** | 顶部对齐（flex-start）可滚动 |
| 平板（横屏） | `isTabletLandscape` | 与竖屏完全一致，内容最大宽 **520dp** | 居中 |

- **无品牌区、无横屏双栏、无额外图标** —— 所有形态渲染同一套内容，仅宽度/字号微调。
- 平板需合理最大宽度，避免过宽难看（`widthIn(max = 520.dp)` 之类）。
- 字号：竖屏标题 headline（约 28sp，原型 h1 对应），平板可略放大。

> 注：真正的多列/自适应大屏布局留待后续，本次只做"更宽"。

---

## 5. 逐层进入动画（全部屏幕）

- 每屏内容在进入时**依次从下方约 22dp 淡入上浮**至最终位置。
- 曲线：MD3 emphasized-decelerate `cubic-bezier(0.05, 0.7, 0.1, 1)`，时长约 800ms，`both` 填充（延迟期间保持透明）。
- 步进：每层 delay +120ms 递增（40 / 160 / 280 / 400 / 520 / 640 / 760ms，最多 7 层）。
- **分层约定**（各屏元素进入顺序）：
  - 欢迎屏：步骤指示器 → 标题 → 简介 → 底部按钮（4 层）
  - 权限声明：步骤指示器 → 标题 → 简介 → 权限列表组 → 同意链接 → 底部按钮（6 层）
  - 通知设置：步骤指示器 → 标题 → 简介 → 开关组 → 常驻开关组 → 底部按钮（6 层）
  - 完成：步骤指示器 → 对勾图形 → 标题 → 简介 → 底部按钮（5 层）
- **播放规则**：第一屏只在**首次进入**时播放一次；其余各屏**每次进入都播放**（返回再前进会重现）。
- 容器本身不再做整体 fadeUp（避免与子层双重上移）。

### 5.1 Compose 落地建议
- 方案 A（推荐）：每层用 `AnimatedVisibility(visible=…, enter = fadeIn(tween(delay=…)) + slideInVertically { 22 } …)`，配合统一的延迟常量表；切换步骤时重建组合以重放动画。
- 方案 B：`graphicsLayer`/`animateFloatAsState` 手写 stagger。若需精确控制"仅首次"与"每次"，用组合键 `key(step, entryCounter)` 触发。
- 参考原型 `assets/prototype.css` 中 `.welcome-in` / `riseIn` keyframe 的分层参数。

---

## 6. 图标与形状绘制原则（重要）

> **Android 实际界面无论如何不允许使用 emoji。** 原型中出现的 ✅/🎉/🌙 等仅为 HTML 占位，落地一律按以下方式绘制：

### 6.1 图标
- 一律使用 Material Icons `ImageVector`（`androidx.compose.material.icons.*`），按语义选择（见 §3.3）。
- 给足 `contentDescription`（无障碍）或对纯装饰图标置 `null` 并外包 `semantics`。

### 6.2 形状（对勾、徽章、多边形等）
参考 `RewardBottomSheet.kt` 的 `AnimatedFeedbackIcon`（`app/src/main/java/io/github/darrindeyoung791/habitpulse/ui/screens/RewardBottomSheet.kt:214-324`）：
- 用 `Canvas` + `Path` 程序化绘制形状（圆角多边形等），支持 `scale` / `graphicsLayer(rotationZ)` 动画。
- 或组合 `Box` + `background(shape = CircleShape…)` + 中央 `Icon`（如完成页绿色对勾：`primaryContainer`/`tertiaryContainer` 圆底 + `Icons.Outlined.Check`）。
- 颜色一律取 `MaterialTheme.colorScheme`（不硬编码），适配明暗主题。

### 6.3 完成页对勾
- 绿色（`tertiaryContainer` 底 + `onTertiaryContainer` 图标，或主题色 `primary`），圆形底 + 白色/主题色对勾，尺寸约 96-120dp，随逐层动画入场（可加轻微缩放）。

---

## 7. 交互与行为

| 行为 | 实现 |
|------|------|
| 权限请求 | 「我同意，继续」时若未授予 `POST_NOTIFICATIONS`，走 `ActivityResultContracts.RequestPermission`（沿用现有逻辑）；授予后按需启动前台服务 |
| 偏好写入 | 通知设置页完成/跳过时一次性写入 `UserPreferences`：`setReminderEnabled` / `setDndEnabled` / `setDndStartTime` / `setDndEndTime` / `setPersistentNotification`（默认开） |
| 常驻通知 | 默认 `true` 写入 |
| 不同意 | 弹确认对话框「不同意将只能退出应用」，操作=取消 / 退出应用（`finish()`）；不调用 `enterLimitedMode` |
| 完成引导 | `habitViewModel.completeOnboarding()` → `startMainActivityAndFinish()` |
| 返回 | 步骤 2/3 顶部返回箭头可回上一屏；系统返回键同样回退（处理） |
| 触感 | 沿用应用按压震动规范（`PressVibrationFeedback`），尊重全局「关闭应用内全部震动」开关 |
| 无障碍 | 步骤指示器、返回、按钮、开关、对话框均有 contentDescription；TalkBack 可朗读 |

---

## 8. 国际化（i18n）

- 所有文案走 `strings.xml`，6 个 locale 同步：默认（中文）、`values-en-rUS`、`values-en-rGB`、`values-zh-rHK`、`values-zh-rTW`。
- 沿用已有字符串：`welcome_description`、`welcome_permissions_title`、`welcome_permission_notification_title`、`welcome_permission_background_title`、`welcome_agree_button`、`welcome_disagree_button`、`welcome_limited_mode`、`onboarding_step_*` 等；新增补齐缺失项。
- 新增文案命名建议：`welcome_greeting_title`（欢迎标题）、`welcome_greeting_continue`（继续）、`welcome_consent_prefix`/`welcome_privacy_policy`/`welcome_terms_of_service`、`welcome_notification_section*`、`welcome_done_*`、`welcome_skip`、`welcome_privacy_policy_coming_soon`（Toast）。

---

## 9. 实现落地计划（概要）

1. **新增** `NewWelcomeActivity`（替换/删除旧 `WelcomeActivity`）+ `NewWelcomeScreen`，`LauncherActivity` 指向新 Activity；Manifest 更新。
2. 屏幕组件放 `ui/screens/welcome/`：`WelcomeGreetingStep`（欢迎）、`WelcomePermissionsStep`（权限声明）、`WelcomeNotificationsStep`（通知设置，含 DND 滑块）、`WelcomeDoneStep`（完成）、`WelcomeStepIndicator`（顶部指示器）。
3. 复用设置公共组件：`SettingsSegmentedItem` / `SettingsSegmentedSwitch` / `SettingsSegmentedBox` / `SettingsSegmentedGroup` / `SettingsTextLinkButton`；若需在 welcome 使用，把 `SettingsSegmentedBox` 改为 public。
4. 逐层动画封装为通用 `WelcomeStagger`/延迟常量（见 §5）。
5. 设备形态走 `rememberDeviceFormInfo()`（§4）。
6. 业务逻辑（权限请求、偏好写入、onboarding 状态、不同意退出对话框、启动主界面）留在 Activity 层，UI 仅换肤。
7. 字符串资源 6 语言补齐（§8）。
8. **禁止 emoji**，图标/形状按 §6 规则。

---

## 10. 验收清单

- [ ] 四屏流程（欢迎→权限→通知→完成）与原型一致；AI 步骤已移除
- [ ] 手机竖屏 / 横屏 / 平板内容一致、仅宽度不同；无品牌区无横屏双栏
- [ ] 常驻通知默认开启；提醒关闭联动禁用免打扰；DND 滑块跨午夜高亮
- [ ] 不同意 → 确认对话框（取消 / 退出应用）；引导中不再进入受限模式
- [ ] 四屏均有逐层进入动画；首屏仅首次、其余每次进入
- [ ] 完成页不展示设置信息
- [ ] 无任何 emoji；图标为 Material Icons，对勾等形状为 Canvas/主题色绘制
- [ ] 明暗主题下颜色正确（均取 colorScheme）
- [ ] 6 语言文案齐全；无障碍 description 齐全
- [ ] 尊重全局震动开关；新字符串均资源化