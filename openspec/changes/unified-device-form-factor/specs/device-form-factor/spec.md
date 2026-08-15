## ADDED Requirements

### Requirement: Single source of form-factor data
系统 SHALL 提供唯一的 `rememberDeviceFormInfo()` Composable（位于 `ui/DeviceFormFactor.kt`），任何界面调用它即可获得一致的设备形态信息，禁止各页面自行通过 `LocalConfiguration` 判定设备形态。

#### Scenario: 任意界面获取形态信息
- **WHEN** 某个 Composable 调用 `rememberDeviceFormInfo()`
- **THEN** 返回一个 `DeviceFormInfo`，其中包含 `isTabletDevice`、`isLandscape` 及派生标志，且与当前窗口一致

#### Scenario: 窗口变化自动响应
- **WHEN** 窗口宽度/高度变化（旋转、分屏调整、折叠屏折叠/展开、桌面窗口缩放）跨过判定断点
- **THEN** 返回的 `DeviceFormInfo` 自动更新并触发重组，无需手动刷新

#### Scenario: 禁止各自判定
- **WHEN** 页面需要判断设备形态
- **THEN** 必须通过 `rememberDeviceFormInfo()` 获取，而非直接读取 `configuration.smallestScreenWidthDp` / `screenWidthDp` / `orientation`

### Requirement: Device class classification
`isTabletDevice` SHALL 基于窗口最小边判定：当 `min(windowWidthDp, windowHeightDp) >= 600` 时为平板（与既有 `smallestScreenWidthDp >= 600` 语义一致），该判定 SHALL 与方向无关。

#### Scenario: 手机竖屏
- **WHEN** 窗口宽 400dp、高 800dp（`min = 400 < 600`）
- **THEN** `isTabletDevice == false`

#### Scenario: 平板横屏
- **WHEN** 窗口宽 1280dp、高 800dp（`min = 800 >= 600`）
- **THEN** `isTabletDevice == true`

#### Scenario: 手机横屏不应误判平板
- **WHEN** 窗口宽 640dp、高 360dp（宽度 ≥ 600 但 `min = 360 < 600`）
- **THEN** `isTabletDevice == false`

### Requirement: Orientation classification
`isLandscape` SHALL 基于窗口宽高比较：当 `windowWidthDp >= windowHeightDp` 时为横屏。

#### Scenario: 横屏
- **WHEN** 窗口宽 800dp、高 360dp
- **THEN** `isLandscape == true`

#### Scenario: 竖屏
- **WHEN** 窗口宽 360dp、高 800dp
- **THEN** `isLandscape == false`

### Requirement: Derived layout flags
`DeviceFormInfo` SHALL 提供派生标志，且满足以下定义：
- `isTabletLandscape = isTabletDevice && isLandscape`
- `isPhoneLandscape = !isTabletDevice && isLandscape`
- `isWideLayout = isLandscape && windowWidthSizeClass >= EXPANDED`（等价横屏且宽 ≥ 840dp）
- `isLargeWindow = windowWidthSizeClass >= LARGE`（宽 ≥ 1200dp）

#### Scenario: 宽屏标志
- **WHEN** 窗口宽 1000dp、横屏（widthSizeClass == EXPANDED）
- **THEN** `isWideLayout == true` 且 `isLargeWindow == false`

#### Scenario: 手机横屏派生
- **WHEN** 窗口宽 640dp、高 360dp（手机横屏）
- **THEN** `isPhoneLandscape == true`、`isTabletLandscape == false`

### Requirement: Behavior parity after migration
迁移后所有既有设备形态相关行为 SHALL 保持不变：导航模式（BottomBar/Rail/PermanentDrawer）、横屏双列/瀑布流、`forceTabletLandscape` 覆盖、DatePicker 输入/选择形态。

#### Scenario: 手机竖屏导航
- **WHEN** 手机竖屏（`isTabletDevice == false` 且 `isLandscape == false`）
- **THEN** 使用底部导航栏（BottomBar），不使用 Rail 或 Drawer

#### Scenario: 手机横屏导航
- **WHEN** 手机横屏（`isTabletDevice == false` 且 `isLandscape == true`）
- **THEN** 使用 NavigationRail

#### Scenario: 平板横屏导航
- **WHEN** 平板横屏（`isTabletDevice == true` 且 `isLandscape == true`）
- **THEN** 使用 PermanentNavigationDrawer，且顶部显示汉堡菜单

#### Scenario: forceTabletLandscape 覆盖
- **WHEN** `forceTabletLandscape` 为 true 且手机横屏
- **THEN** 按平板横屏行为处理（使用 Drawer、`isWideLayout` 生效）

### Requirement: Expose raw window info for future extension
`DeviceFormInfo` SHALL 携带原始 `WindowSizeClass`（宽/高大小类）与 `WindowPosture`，供未来折叠屏/桌面功能使用，不丢失官方数据。

#### Scenario: 折叠屏扩展
- **WHEN** 未来新增折叠屏姿态感知功能
- **THEN** 可直接读取 `DeviceFormInfo.windowPosture`（hinge/tabletop 等）与 `windowSizeClass` 细粒度断点，无需改动既有调用方

#### Scenario: 大屏/超大屏断点
- **WHEN** 需要区分 LARGE（1200dp）与 XLARGE（1600dp）
- **THEN** 通过 `windowSizeClass.windowWidthSizeClass` 精确比较，`isLargeWindow` 提供 ≥ 1200dp 的粗粒度判断
