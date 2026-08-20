# HabitPulse 新欢迎页（Welcome）重设计原型

> **权威需求与设计规格见 `DESIGN_GUIDE.md`**（汇总历轮评审全部结论，实施以它为准）。
> 本目录为 **WelcomeActivity 重构**的可交互 HTML 原型，供设计评审使用，**尚未落实到 Android 代码**。
> 设计风格对齐新版设置（NewSettings*）的优雅 Material 3 分段列表，首屏采用 Apple 式克制的极简欢迎。

## 文件说明

| 文件 | 用途 |
|------|------|
| `DESIGN_GUIDE.md` | **权威设计指南**：全部需求、样式、动画、图标/形状原则（禁 emoji）、i18n、验收清单 |
| `index.html` | 总览页：设计要点 + 原型入口 |
| `portrait.html` | **核心交互原型**：完整三步引导流程，支持明暗主题、手机/横屏/平板三形态切换 |
| `assets/prototype.css` | 共享样式（M3 fallback 配色 + seed 色板） |
| `assets/prototype.js` | 共享交互逻辑（步骤流转、开关、DND 滑块、受限模式） |

直接用浏览器打开 `index.html` 即可浏览（无需服务器）。

## 交互说明

- **明暗主题**：右上角「浅色 / 深色」按钮（状态记忆在 localStorage）。
- **设备形态**：右上角「手机竖屏 / 横屏 / 平板」切换设备外框与布局：
  - 三种形态内容完全一致，仅宽度不同（竖屏 360px 单列居中；横屏顶部对齐可滚动、内容最大宽 460px；平板垂直居中、内容最大宽 520px）；无品牌区、无图标。
- **流程**：`继续` → 权限声明 → `我同意，继续` → 通知设置 → `完成` → 完成页 → `进入 HabitPulse` 回到起始。
- **开关**：整行可点；关闭「提醒」会联动禁用「免打扰」；打开「免打扰」展开时段滑块。**常驻通知默认开启**。
- **DND 滑块**：双端可拖动，跨午夜高亮；点击轨道跳至最近端。
- **不同意**：弹确认对话框，告知「不同意将只能退出应用」，操作 = 取消 / 退出应用；引导中不再提供受限模式入口。
- **隐私政策 / 服务条款**：权限声明页的占位链接，点击提示「即将上线」。
- **逐层进入动画**：每屏内容在进入时依次从下方 22px 淡入上浮（MD3 emphasized-decelerate，`cubic-bezier(.05,.7,.1,1)`），步进 120ms；第一屏仅在首次进入播放，其余各屏每次进入都播放。

## 新流程结构（三步 + 完成）

| 步骤 | 内容 | 说明 |
|------|------|------|
| 1 欢迎 | 两行文本 + 「继续」 | Apple 式克制极简，无卡片无图标行，仅标题 + 一句简介 + 单个按钮 |
| 2 权限声明 | 两条权限图标行 + 隐私政策/服务条款链接 | 同意/不同意按钮；链接为未来预留；不同意弹退出确认对话框 |
| 3 通知设置 | 提醒 / 免打扰（时段滑块）/ 常驻通知 | 分段开关列表 |
| 完成 | 「进入」 | 绿色对勾 + 一句鼓励语，不展示设置信息 |

## 与现有 Welcome 流程的差异

| 项 | 现有 | 新设计 |
|----|------|--------|
| 步骤数 | 3 步（同意 → 通知 → AI） | 欢迎 → 权限声明 → 通知（3 步 + 完成页），AI 步骤移除 |
| 首屏 | Logo + 简介 + 权限行 + 按钮 | 两行文本 + 单个「继续」按钮（Apple 克制） |
| 权限声明 | 与首屏合并 | 独立第二步，含隐私政策/服务条款占位链接 |
| 权限说明 | 纯图标 + 文本行 | 分段列表 + 图标 chip（遵循 seed 色板） |
| 通知开关 | `Surface(onClick)` 卡片（12dp / surfaceVariant） | `SettingsSegmentedSwitch`（surfaceContainer / 16-4-20dp 圆角） |
| DND 时段 | 展开滑块卡片 | `SettingsSegmentedBox` 内嵌双端滑块 |
| 顶部 | 无 | 步骤指示器（三点进度 + 1/3 计数） |
| 进入动画 | 无 | 每屏逐层浮现（首屏仅首次，其余每次进入） |
| 常驻通知 | 默认关闭 | 默认开启 |
| 按钮 | 方角 Button / TextButton | 大圆角主按钮 + 文本次按钮（Pixel 风格） |

## 实现落地建议（通过评审后）

1. 新建 `NewWelcomeActivity` + `NewWelcomeScreen`（含 `welcome/` 下三个步骤组件），删除旧 `WelcomeScreen.kt` / `Welcome*Step.kt`，`LauncherActivity` 指向新 Activity。
2. 复用 `SettingsSegmentedItem` / `SettingsSegmentedSwitch` / `SettingsSegmentedBox` / `SettingsTextLinkButton` 公共组件；`SettingsSegmentedBox` 若需在 welcome 使用需将其改为 public。
3. 设备形态一律走 `rememberDeviceFormInfo()`（`ui/DeviceFormFactor.kt`），禁止自行读 `LocalConfiguration`。
4. 业务逻辑（权限请求、偏好写入、onboarding 状态流转）保留在 Activity 层，UI 仅换肤。
5. 文案全部走 `strings.xml`（6 个 locale 文件同步）；隐私政策/服务条款链接预留占位，后续接入。