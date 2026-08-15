## Why

应用里共有 11 个文件在各自独立判断设备形态（平板/手机、横屏/竖屏），且阈值互相矛盾：平板判定同时存在 `smallestScreenWidthDp >= 600` 与 `screenWidthDp >= 1200` 两套标准，宽屏又有独立的 840dp 阈值和 `宽 > 高` 判断。这种"各自为政"导致同一设备在不同页面上表现不一致，后续新增页面也极易再次复制偏差逻辑。

## What Changes

- 引入官方 `androidx.compose.material3.adaptive:adaptive`（`currentWindowAdaptiveInfo()`，底层为 Jetpack WindowManager 的 WindowSizeClass），替换手写 `LocalConfiguration` 设备判定。
- 新增单一入口 `ui/DeviceFormFactor.kt`：`DeviceFormInfo` 数据类 + `rememberDeviceFormInfo()` Composable，统一提供设备类（手机/平板）、方向、宽屏标志及现有全部派生布尔，同时保留原始 `WindowSizeClass` 供未来折叠屏/桌面扩展。
- 迁移 11 处自研判定调用点到单一入口（HomeScreen、HabitScreen、RecordsScreen、ContactsScreen、WelcomeScreen、SettingsActivity、RewardBottomSheet、NotificationConfirmDialog、NewSettingsGeneralScreen、AIChatScreen、AICreateHabitScreen）。
- 移除 `libs.versions.toml` 中 material3 的 `1.5.0-alpha24` 固定版本，由 Compose BOM 统一管理（material3 与 material3-adaptive 版本对齐）。
- 同步更新 `AGENTS.md` 与 `QWEN.md`（项目约定：结构/约定重大变化必须双文档同步），并在 `doc/` 或 `docs/` 处补充开发文档。

## Capabilities

### New Capabilities
- `device-form-factor`: 统一的设备形态判定（设备类、方向、窗口宽度类、派生布局标志）作为全应用单一数据源，所有页面通过该能力获取形态信息。

### Modified Capabilities
<!-- 无既有 spec 的行为变更 -->

## Impact

- **依赖**：新增 `androidx.compose.material3.adaptive:adaptive`；`libs.versions.toml` 移除 material3 版本 pin（风险：若 BOM 内 material3 低于现有 alpha24，可能触发编译错误，需在验证阶段处理）。
- **代码**：`ui/DeviceFormFactor.kt`（新增）；11 处调用点迁移（HomeScreen/HabitScreen/RecordsScreen/ContactsScreen/WelcomeScreen/SettingsActivity/RewardBottomSheet/NotificationConfirmDialog/NewSettingsGeneralScreen/AIChatScreen/AICreateHabitScreen）。
- **行为兼容**：现有导航模式决策（BottomBar/Rail/Drawer）、双列/瀑布流、DatePicker 形态、forceTabletLandscape 覆盖逻辑保持不变，仅判定来源统一。
- **文档**：AGENTS.md、QWEN.md、开发文档。
