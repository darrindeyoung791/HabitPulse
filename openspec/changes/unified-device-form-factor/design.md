## Context

HabitPulse 目前有 11 个文件各自通过 `LocalConfiguration`（`smallestScreenWidthDp`、`screenWidthDp`、`orientation`）判定设备形态，阈值不统一（600/840/1200dp、宽>高），同一设备在不同页面表现可能不一致。应用为多 Activity + Compose 架构，未来形态未定（可能迁移单 Activity/折叠屏支持）。

现有导航决策（记录于 AGENTS.md）：
- 手机竖屏 → BottomBar；手机横屏 → NavigationRail；平板横屏 → PermanentDrawer；平板竖屏 → BottomBar
- 横屏且宽 ≥ 840dp → 双列/瀑布流布局
- `forceTabletLandscape`（DataStore 偏好）可把手机横屏强制为平板横屏行为

项目为 Compose BOM 2026.06.01，material3 显式固定为 `1.5.0-alpha24`；尚未引入任何 `androidx.window` / `material3-adaptive` 依赖。

## Goals / Non-Goals

**Goals:**
- 提供单一 `rememberDeviceFormInfo()` Composable 作为全应用设备形态数据源，消除 11 处各自判定
- 基于官方 `material3-adaptive`（底层 Jetpack WindowManager WindowSizeClass），窗口尺寸变化（旋转/分屏/折叠）自动响应
- 迁移后现有行为（导航模式、双列/瀑布流、forceTabletLandscape、DatePicker 形态）保持完全一致
- 暴露原始 `WindowSizeClass`/`Posture`，为未来折叠屏/桌面留扩展点
- 依赖尽可能用最新、由 BOM 统一管理

**Non-Goals:**
- 不引入折叠屏姿态专用 UI（仅预留数据）
- 不重构各页面布局本身，只统一"判定来源"
- 不做资源限定符（`sw600dp` 等）改造

## Decisions

### D1. 依赖：`material3-adaptive` + 显式 `androidx.window:window-core`
新增 `androidx.compose.material3.adaptive:adaptive`（BOM 管理），并显式声明 `androidx.window:window-core` 以便直接使用 `WindowMetricsCalculator` / `WindowSizeClass` 类型。
- **备选**：只用 `material3-window-size-class`（旧 API，返回 DpSize 需手动换算）→ 否决，官方已推荐 material3-adaptive
- **备选**：只读 `LocalConfiguration`（现状）→ 否决，无法自动响应多窗口/桌面调整
- **备选**：只依赖 material3-adaptive 的传递依赖 → 否决，`WindowMetricsCalculator` 未必暴露到编译类路径，显式声明更稳

### D2. API：双层 `DeviceFormInfo`（业务布尔 + 原始窗口类）
`ui/DeviceFormFactor.kt` 提供：
- `data class DeviceFormInfo(windowSizeClass: WindowSizeClass, windowPosture: Posture, isTabletDevice: Boolean, isLandscape: Boolean, windowWidthDp: Dp, windowHeightDp: Dp)`，派生属性 `isTabletLandscape`、`isPhoneLandscape`、`isWideLayout`、`isLargeWindow`
- `@Composable rememberDeviceFormInfo(): DeviceFormInfo` —— 内部 `currentWindowAdaptiveInfo()` + `WindowMetricsCalculator`，单点 `@OptIn(ExperimentalMaterial3AdaptiveApi::class)`
- 纯函数 `classifyDeviceForm(widthDp, heightDp, windowSizeClass): DeviceFormInfo`，无 Android 依赖，可 JVM 单测（沿用项目 SlotCheckInEngine 的可测纯逻辑惯例）

### D3. 平板判定：`min(宽,高) >= 600dp`（窗口维度）
用窗口实际最小边替代 `configuration.smallestScreenWidthDp`。对当前窗口两者等价，且多窗口/桌面下更正确。
- **备选**：用 `widthSizeClass >= MEDIUM`（600dp）→ 否决，Medium 只看宽度，手机横屏宽 640 会被误判为平板

### D4. 横屏判定：`窗口宽 >= 窗口高`
用 `WindowMetricsCalculator` 的窗口 dp 宽高比较，替代 `configuration.orientation`。多窗口/自由缩放下 `orientation` 可能失真，窗口 dp 最准确。
- **备选**：由 WindowSizeClass 宽高类比较推导 → 否决，粒度太粗、方形窗口不稳

### D5. 宽屏/大屏标志：对齐官方断点
- `isWideLayout = isLandscape && widthSizeClass >= EXPANDED`（= 横屏且宽 ≥ 840dp，等价原 `isWaterfallMode`/`useStaggeredGrid`/`useTwoColumnLayout`）
- `isLargeWindow = widthSizeClass >= LARGE`（= 宽 ≥ 1200dp，替代 WelcomeScreen 的 `screenWidthDp >= 1200`，修正其与全应用不一致的阈值）
- 常量集中在文件内：`TABLET_MIN_WIDTH_DP=600`、`WIDE_LAYOUT_MIN_WIDTH_DP=840`、`LARGE_SCREEN_MIN_WIDTH_DP=1200`

### D6. 迁移策略：自底向上 + HomeScreen 为枢纽
- 先建判定器与单测，再迁移独立组件（Sheet/对话框/独立 Activity/AI 屏），最后迁移 HomeScreen 并向下传参给 HabitScreen/RecordsScreen/ContactsScreen
- HomeScreen 只调一次 `rememberDeviceFormInfo()`，把 `DeviceFormInfo`（或派生态）作为参数传入子屏，消除子屏各自读取配置

### D7. 移除 material3 版本 pin
`libs.versions.toml` 删除 `material3 = "1.5.0-alpha24"`，material3 与 material3-adaptive 均由 BOM 对齐到最新（符合用户"依赖尽可能用最新"要求）。

## Risks / Trade-offs

- [BOM 内 material3 可能低于现有 alpha24，移除 pin 后编译失败] → 验证阶段先跑 `compileDebugKotlin`；若有 alpha 专用 API，按 BOM 版本调整或保留 pin 仅在确认必需时
- [material3-adaptive API 仍在实验期（`@ExperimentalMaterial3AdaptiveApi`），且 `currentWindowAdaptiveInfo`/`V2` 命名随版本演进] → 全部隔离在 DeviceFormFactor.kt 单点，其余文件不感知；实施时以 BOM 实际 API 为准
- [窗口 dp 判定与 `configuration` 在状态栏/方形窗口等边界存在细微差异] → 用 `classifyDeviceForm` 单测锁定阈值边界；真机验证导航模式切换
- [新增依赖体积/兼容性] → 两个官方库均随 BOM，`androidx.window:window-core` 为最小核心包
- [WelcomeScreen `screenWidthDp>=1200` 行为"修复"可能影响引导页排版] → 实施时先看其具体用途再替换，保持视觉不变

## Migration Plan

1. 依赖：`libs.versions.toml` + `app/build.gradle.kts` 新增 `material3-adaptive`、`window-core`，移除 material3 pin
2. 新建 `ui/DeviceFormFactor.kt`（`classifyDeviceForm` 纯函数 + `rememberDeviceFormInfo`）
3. 新增 `DeviceFormFactorTest`（阈值边界：600/840/1200、手机/平板/横竖屏、force 派生）
4. 迁移独立组件：`RewardBottomSheet`、`NotificationConfirmDialog`、`NewSettingsGeneralScreen`、`SettingsActivity`、`WelcomeScreen`、`AIChatScreen`、`AICreateHabitScreen`
5. 迁移 `HomeScreen`（导航模式 + forceTabletLandscape + DatePicker）并向 `HabitScreen`/`RecordsScreen`/`ContactsScreen` 传 `DeviceFormInfo`
6. `compileDebugKotlin` + `testDebugUnitTest`
7. 更新 AGENTS.md、QWEN.md，补充开发文档
8. 回滚策略：判定器与迁移可分开提交；任何一步编译/行为异常可单独 revert，不影响其余页面

## Open Questions

- BOM 2026.06.01 下 `currentWindowAdaptiveInfo()` 的确切签名/实验注解（构建时确认，用 `V2` 或默认版本）
- WelcomeScreen `isTablet`（≥1200）实际用途，替换语义以保持视觉不变为准
