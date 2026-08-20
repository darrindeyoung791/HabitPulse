## 1. 依赖与基础

- [ ] 1.1 `gradle/libs.versions.toml` 新增 `androidx-compose-material3-adaptive`（group=`androidx.compose.material3.adaptive`，name=`adaptive`，无版本号随 BOM）与 `androidx-window-core`（group=`androidx.window`，name=`window-core`，最新版）
- [ ] 1.2 移除 `libs.versions.toml` 中 `material3 = "1.5.0-alpha24"` 版本与 `material3` library 的 `version.ref`，交由 BOM 管理
- [ ] 1.3 `app/build.gradle.kts` 添加 `implementation(libs.androidx.compose.material3.adaptive)` 与 `implementation(libs.androidx.window.core)`
- [ ] 1.4 运行 `.\gradlew.bat :app:dependencies --configuration debugCompileClasspath`（或直接编译）确认依赖解析成功、material3 版本为 BOM 所定

## 2. 全局判定器

- [ ] 2.1 新建 `app/src/main/java/io/github/darrindeyoung791/habitpulse/ui/DeviceFormFactor.kt`：
  - 常量 `TABLET_MIN_WIDTH_DP=600`、`WIDE_LAYOUT_MIN_WIDTH_DP=840`、`LARGE_SCREEN_MIN_WIDTH_DP=1200`
  - 纯函数 `classifyDeviceForm(widthDp: Dp, heightDp: Dp, windowSizeClass: WindowSizeClass): DeviceFormInfo`（无 Android 依赖，可单测）
  - `data class DeviceFormInfo`（`windowSizeClass`、`windowPosture`、`isTabletDevice`、`isLandscape`、`windowWidthDp`、`windowHeightDp` + 派生 `isTabletLandscape`/`isPhoneLandscape`/`isWideLayout`/`isLargeWindow`）
  - `@Composable fun rememberDeviceFormInfo(): DeviceFormInfo`：内部 `currentWindowAdaptiveInfo()` + `WindowMetricsCalculator` 取窗口 dp，单点 `@OptIn(ExperimentalMaterial3AdaptiveApi::class)`
- [ ] 2.2 新建单测 `app/src/test/java/.../ui/DeviceFormFactorTest.kt`：覆盖 600/840/1200 边界、手机竖/横屏、平板竖/横屏、方形窗口、派生标志（对齐 spec `device-form-factor` 各 Scenario）
- [ ] 2.3 运行 `.\gradlew.bat testDebugUnitTest` 通过

## 3. 迁移独立组件（不涉及传参）

- [ ] 3.1 `RewardBottomSheet.kt`：删除 `smallestScreenWidthDp`/`isLandscape` 本地判定，改用 `rememberDeviceFormInfo().isPhoneLandscape`（含 `isPhoneLandscape` 内边距逻辑，行 96-102、175）
- [ ] 3.2 `NotificationConfirmDialog.kt`：同 3.1（行 54-58、73）
- [ ] 3.3 `NewSettingsGeneralScreen.kt`：`isTabletDevice`/`isTabletLandscape`/`showForceTabletLandscapeSwitch` 改用 `DeviceFormInfo`（行 54-59）
- [ ] 3.4 `SettingsActivity.kt`（旧设置）：`isTabletDevice`/`isTabletLandscape` 改用 `DeviceFormInfo`（行 107-119）
- [ ] 3.5 `AIChatScreen.kt`：`isLandscape` 改用 `DeviceFormInfo`（行 122-123、241）
- [ ] 3.6 `AICreateHabitScreen.kt`：`isLandscape` 改用 `DeviceFormInfo`（行 149-150、240、482）
- [ ] 3.7 `WelcomeScreen.kt`：先审查 `shouldUseSplitLayout`/`isTablet` 的用途（行 44-48），再替换为 `DeviceFormInfo`（`isLandscape` + `isLargeWindow` 或 `isTabletDevice`），保持视觉不变

## 4. 迁移 HomeScreen 及子屏

- [ ] 4.1 `HomeScreen.kt`：删除本地 `screenWidthDp`/`smallestScreenWidthDp`/`isLandscape`/`isTabletDevice` 判定（行 186-223），改为单次 `rememberDeviceFormInfo()`；导航模式由 `info.isTabletLandscape`/`info.isPhoneLandscape` 驱动；`forceTabletLandscape` 覆盖逻辑保留但基于 `info`；`isWaterfallMode` 改用 `info.isWideLayout`
- [ ] 4.2 `HomeScreen.kt` DatePicker（行 980、1018-1035）：`isPhoneLandscape = effectiveUseRail` 改为 `info.isPhoneLandscape`，注释更新
- [ ] 4.3 给 `HabitScreenContent` 传 `DeviceFormInfo`（新增参数），其 `useStaggeredGrid = info.isWideLayout || (forceTabletLandscape && info.isLandscape)`，删除本地 `screenWidthDp`/`isLandscape` 判定与 `screenWidthDp=840` hack（行 591-599）
- [ ] 4.4 `RecordsScreenContent`：传 `DeviceFormInfo`，`useTwoColumnLayout = info.isWideLayout`（行 239-243）
- [ ] 4.5 `ContactsScreenContent`：传 `DeviceFormInfo`，`useTwoColumnLayout = info.isWideLayout`（行 185-189）

## 5. 验证与收尾

- [ ] 5.1 全局搜索确认无残留：`smallestScreenWidthDp`、`configuration.screenWidthDp`、`ORIENTATION_LANDSCAPE`（`@Preview` 的 uiMode 除外）
- [ ] 5.2 运行 `.\gradlew.bat :app:compileDebugKotlin` 无错误
- [ ] 5.3 运行 `.\gradlew.bat testDebugUnitTest` 全绿
- [ ] 5.4 真机/模拟器验证：手机竖/横屏、平板竖/横屏的导航模式切换；`forceTabletLandscape` 开关；双列/瀑布流；DatePicker 形态
- [ ] 5.5 更新 `AGENTS.md` 与 `QWEN.md`：Responsive Navigation 表与开发约定改为引用 `rememberDeviceFormInfo`；记录"设备形态统一判定"约定（含 600/840/1200 断点）
- [ ] 5.6 在 `doc/`（开发文档）补充设备形态判定器说明；如 `docs/`（用户向）无相关章节则不涉及
