# ListItem（列表项）样式与设计规范

> 本文档描述新版设置界面（及后续页面）使用的分段列表项（segmented list item）的设计与实现规范，
> 作为后续统一开发的唯一参考。改动视觉规格前请先同步更新本文档。

## 概览

列表项体系位于 `ui/screens/settings/components/` 包，由 6 个公共组件 + 3 个内部实现组成：

| 组件 | 类型 | 用途 |
|------|------|------|
| `SettingsSegmentedItem` | 公共 | 导航 / 动作列表项（可选图标 + 文本 + 可选箭头） |
| `SettingsSegmentedSwitch` | 公共 | 开关列表项（整行可切换） |
| `SettingsSegmentedGroup` | 公共 | 竖直分组容器（内部自动加 2dp 间距） |
| `SettingsTextLinkButton` | 公共 | 紧凑文本链接按钮（无 48dp 最小触控区，仅文本高度） |
| `SettingsExpandableListSurface` | 公共 | 可展开列表项：点击表头展开/收起（表头文本 + chevron 旋转 + 展开内容同一表面） |
| `SettingsSegmentedBox` | internal | 非点击分段表面：圆角 / 底色，放置任意内容（如滑块） |
| `SettingsListSurface` | internal | 共享行表面：圆角 / 底色 / 涟漪 / 点击 |
| `SettingsIconChip` | internal | 前置图标圆角方块 chip |
| `AccentPalette` / `rememberAccentTint` | internal | 图标 chip 固定低饱和强调色板 |

新设置各页面（`NewSettings*Screen`）统一使用上述组件。**旧设置 `SettingsActivity.kt` 中的
`SettingsListItem` / `SettingsSwitchItem` 为 legacy 封装，新页面不得再使用**。

> **活体示例**：调试设置 → 「UI catalog」（`SettingsUICatalogScreen.kt`）以可交互页面集中展示
> 上述全部元素及分组圆角 / `tintOffset` / 状态变体特性，可作为视觉对照参考。该页文案为硬编码英文，
> 暂未资源化，后续扩展其他界面时再统一迁移 design token 式管理。

---

## 1. 设计规格

### 1.1 底色

- 行底色：`MaterialTheme.colorScheme.surfaceContainer`（**动态取色**）。
- 与主页习惯卡片保持一致（`HabitScreen.kt:1148-1149` 的 `CardDefaults.cardColors(containerColor = surfaceContainer)`）。

### 1.2 圆角

| 状态 | 值 | 说明 |
|------|-----|------|
| 边缘项（组内首/末，或单例组） | `LargeCorner = 16.dp` | 首项 = 左上/右上，末项 = 左下/右下 |
| 内部项 | `SmallCorner = 4.dp` | 仅相邻的边用小圆角 |
| **按下态** | `PressedCorner = 20.dp` | 四个角同时动画到 20dp，**比边缘项圆角更大** |

- 圆角过渡用 `animateDpAsState` 平滑形变（按下变大，松手回弹）。
- 单例组 = 独立圆角卡片（四角都是 `LargeCorner`）。

### 1.3 间距

| 位置 | 值 |
|------|-----|
| 组内项间距 | `SettingsGroupItemGap = 2.dp` |
| 组与组之间 | `SettingsBetweenGroupGap = 16.dp` |
| 行内水平 padding | `16.dp` |
| 行内垂直 padding | `12.dp` |
| 行内元素间距 | `Row` 的 `spacedBy(16.dp)` |
| 标题与副标题间距 | 文本列 `spacedBy(4.dp)` |

- **组与组之间**在页面上用 `Spacer(modifier = Modifier.height(SettingsBetweenGroupGap))` 分隔，
  使不同分组（如设置首页的「帮助与反馈」独立组）视觉上明显大于组内 4dp 间距。
  参考实现：`NewSettingsHomeScreen.kt`（主组与帮助组之间）、`NewSettingsAIScreen.kt`
  （流式输出组与记忆组之间，均为 `Spacer(16.dp)`）。

### 1.4 文本

| 元素 | 样式 | 颜色（启用） | 颜色（禁用） |
|------|------|------|------|
| 标题 | `bodyLarge` | `onSurface` | `onSurfaceVariant` |
| 副标题 | `bodyMedium` | `onSurfaceVariant` | `onSurfaceVariant` |

- 禁用项无副标题时，默认显示 `settings_coming_soon`（「即将推出」）。

### 1.5 箭头（trailing chevron）

- 图标：`Icons.AutoMirrored.Filled.KeyboardArrowRight`，tint `onSurfaceVariant`。
- **仅当 `showArrow = true` 时渲染**（`SettingsSegmentedItem` 参数，默认 `true`）。
- `showArrow = false` 的场景（无子页面可打开）：
  - 禁用占位项（如「连接与同步」「记忆」）；
  - 弹窗动作项（如「清缓存 / 清 Cookie」）；
  - 权限授权项（如常驻通知授权）；
  - 无操作项（如「应用版本」）。

### 1.6 分隔线

- **禁止使用 `HorizontalDivider`**。项与组之间靠分段圆角 + 间距分隔。
- 依赖：行与行之间必须有 2dp 以上的视觉间隙，否则圆角形变会失去分段感。

### 1.7 禁用态

- 整行不可点击：不触发 `onClick`、无涟漪、无按下圆角动画。
- 视觉：标题 `onSurfaceVariant`，图标 chip / 开关使用禁用配色（见 1.9 / 1.10）。

---

## 2. 交互与动效

### 2.1 按压涟漪（Ripple）

**实现要点（容易踩坑）**：

```kotlin
// ✅ 正确：显式 clickable + LocalIndication，涟漪被 clip 裁剪到当前圆角
Box(
    modifier = Modifier
        .fillMaxWidth()
        .clip(shape)                                  // 先裁剪
        .background(MaterialTheme.colorScheme.surfaceContainer)
        .clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,     // 关键
            enabled = enabled,
            onClick = onClick
        )
)
```

```kotlin
// ❌ 错误：M3 Surface(onClick=...) 在当前主题下不产生按压涟漪
// 原因：M3 1.5.0-alpha24 Surface 的 onClick 重载在 focus 配置为 InsetRing 时
//      添加 ripple(enablePressIndication = false)，即显式禁用按压指示。
```

- 涟漪会随 `clip(shape)` 的圆角一起形变：按下时圆角增大到 20dp，涟漪边界同步扩大。
- 参考实现：`SettingsSegmentedItem.kt` 的 `SettingsListSurface`、`RecordsScreen.kt:666` 的 `CompletionRecordCard`。

### 2.2 圆角形变

- 用 `collectIsPressedAsState()` 读取按下态，`animateDpAsState` 驱动四个角的 dp 值。
- 禁用态必须同时 `pressed && enabled` 才应用 `PressedCorner`。

---

## 3. 图标 chip（前置图标）

- 圆角方块背景 + 居中图标，颜色来自**固定低饱和强调色板** `AccentPalette`（非动态取色），
  6 组色按 `index` 循环（`index % 6`），每组含明/暗两套变体，随系统明暗自动切换。

| 布局 | chip 尺寸 | 图标尺寸 | chip 圆角 |
|------|----------|---------|----------|
| 单行（无副标题） | 28.dp | 18.dp | 9.dp |
| 双行（有副标题） | 40.dp | 24.dp | 12.dp |

- 启用：`tint.container`（chip 底）/ `tint.content`（图标）。
- 禁用：`surfaceContainerHighest`（chip 底）/ `onSurfaceVariant`（图标）。

色板示例（仅作参考，实际以 `SettingsIconTint.kt` 为准）：

| # | 浅色底 / 图标 | 深色底 / 图标 |
|----|--------------|--------------|
| 0（蓝） | `#DCE8FB` / `#2A5AA8` | `#1F2D4A` / `#9FC0F0` |
| 1（绿） | `#DCEFDC` / `#2E7D46` | `#1E3327` / `#97D0A4` |
| 2（橙） | `#FBEBD6` / `#9A6B2A` | `#36281A` / `#E3BC7E` |
| 3（紫） | `#EAE1F7` / `#5E3F9E` | `#2B2340` / `#BCA6E8` |
| 4（红） | `#F9E0E0` / `#A63C47` | `#3A2224` / `#E8A0A6` |
| 5（青） | `#D9EEEF` / `#2E7C80` | `#1E3234` / `#9CD3D6` |

---

## 4. 开关行（Switch）

- **整行是切换目标**：点击行任意位置触发 `onCheckedChange(!checked)`。
- trailing 的 `Switch` 渲染为 `onCheckedChange = null`（非交互，仅反映状态），避免双击竞争。
- 行与 `Switch` **共享同一个 `MutableInteractionSource`**：按行时 Switch 同步显示按下态。
- 颜色由 `ThemeSwitchColors()` 提供：

| 状态 | 轨道 | 滑块/边框 |
|------|------|----------|
| 开 | `primary` | `onPrimary` |
| 关 | `surfaceContainerHighest` | `outline` |
| 禁用 | `primary(38%)` / `surfaceContainerHighest(60%)` | `onPrimary(38%)` / `onSurface(38%)` |

---

## 4.5 非点击分段表面（SettingsSegmentedBox）

- 用途：把**非交互内容**（如字体大小滑杆、震动调试滑块）放进分段组，获得与列表项一致的
  圆角 + `surfaceContainer` 底色，但不响应点击（无涟漪、无按下圆角动画）。
- 使用：`SettingsSegmentedBox(index = ..., count = ...) { ... }`，`index` / `count` 与组内其他项
  一起决定圆角；内部内容自带 padding（如滑块 `padding(horizontal = 16.dp)`）。
- 参考实现：`SettingsFontScaleScreen.kt`（字体大小档位滑杆）、`SettingsDebugVibrationScreen.kt`。
- 注意：**跟随开关展开/收起的滑块不再使用本组件**——免打扰时段滑块（`DndRangeSlider`）已迁移到
  `SettingsExpandableListSurface` 的「开关行 + 同表面展开体」模式（见 4.7），
  避免开关行与滑块之间出现 2dp 分段间隙。

## 4.7 可展开列表项（SettingsExpandableListSurface）

- 用途：点击在「展开 / 收起」之间切换的列表项（如开放源代码许可页每个开源库、通知设置的
  免打扰时段）。整个条目 = 标题 + 可选副标题 + 可选 `badgeContent` 槽位 + 尾部指示器；
  展开内容与表头位于**同一块** `surfaceContainer` 表面内（`AnimatedVisibility` + spring 动画），
  保持分段观感、中间无间隙。
- **整面可点击**：`clickable` 位于表面根节点而非表头行——涟漪与按下圆角形变覆盖表头和
  展开体整体；展开区内嵌的可点击子控件（按钮等）按 Compose 最内层消费规则优先响应，
  不会误触发展开切换。
- `badgeContent`：常驻表头槽位（如 license pill badge），在标题/副标题下方渲染，
  **与展开状态无关，始终可见**。
- 尾部指示器二选一：
  - 默认 chevron：`Icons.Filled.KeyboardArrowDown` 随展开态 180° 旋转（`animateFloatAsState`）；
  - `trailing` 槽位：传入自定义 composable 替换 chevron。「开关即展开」变体在此传入
    `Switch(onCheckedChange = null)`，`checked` 镜像 `expanded`；把组件的
    `interactionSource` 共享给 Switch 即可同步按压态（同 `SettingsSegmentedSwitch` 手法）。
- 参数：`index` / `count` 决定圆角；`expanded` / `onToggle` 由调用方持有；可选
  `leadingIcon`（渲染 `SettingsIconChip`，配 `tintIndex` 取色板）/ `enabled`
  （禁用点击、图标 chip 走禁用配色）；`content` 为展开区（自带水平 padding，
  底部留 12dp 以上呼吸空间）。
- 参考实现：
  - `OpenSourceLicensesActivity.kt`（chevron 变体，badge 常驻表头，展开区显示功能按钮）；
  - `SettingsNotificationsScreen.kt` / `WelcomeNotificationsStep.kt`（开关即展开变体，
    免打扰滑块与开关行同一表面，替代旧「开关行 + `SettingsSegmentedBox` 两段式」）；
  - `SettingsUICatalogScreen.kt`（两种变体的对照演示）。
- 注意：展开态切换不会改变 `index` / `count`，末项展开时底部圆角仍保持 `LargeCorner`。

## 4.6 紧凑文本链接按钮（SettingsTextLinkButton）

- 用途：设置页内独立成行的文本按钮（如「去「设置」自定义通知…」「保持后台运行…」）。
- 与默认 `TextButton` 的区别：通过 `CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp)`
  移除 48dp 最小触控区，使堆叠的文本按钮仅占文本高度、间距更紧凑。
- 样式：`shape = RectangleShape`，`contentPadding = PaddingValues(horizontal = 16.dp)`，
  文本 `bodyMedium`。
- 使用：传字符串用 `SettingsTextLinkButton(text = ..., onClick = ...)`；
  传资源 id 用 `SettingsTextLinkButtonRes(textRes = ..., onClick = ...)`。
- 参考实现：`NewSettingsNotificationsScreen.kt`（自定义通知 / 保持后台运行）、
  `NewSettingsAboutDetailScreen.kt`（应用信息 / GitHub 链接）。
- 注意：弹窗内的确认/取消按钮（`AlertDialog` 的 `TextButton`）**不**用此组件，保持系统默认触控区。

---

## 5. 使用约定（开发指南）

1. **同一控件在任意页面必须同一样式**：导航行用 `SettingsSegmentedItem`，开关行用
   `SettingsSegmentedSwitch`，禁止就地拼 `Surface + Row`。
2. **分组**：把同段落的项包进 `SettingsSegmentedGroup`，传 `index`（组内从 0 起）与 `count`（组内总数），
   圆角自动计算。
3. **箭头**：只有打开子页面（导航到新页面/Activity）才保留 `showArrow = true`；弹窗、权限、
   禁用、无操作项一律 `showArrow = false`。
4. **图标**：需要区分主题的项传 `leadingIcon`；不需要图标的项**省略该参数**（如调试页的
   「添加示例习惯」，文本直接贴左侧）。
5. **涟漪**：任何可点击行必须走 `clickable(interactionSource, indication = LocalIndication.current)`
   模式，不要用 `Surface(onClick)`。
6. **动态取色**：行底色用 `surfaceContainer`；图标 chip 是刻意固定的低饱和色板，不要改成动态取色。
7. **禁用占位项**：`enabled = false` + `onClick = {}`，自动显示「即将推出」。
8. **RTL**：图标用 `autoMirrored` 变体，padding 用 `start/end`，不写死方向。
9. **触感震动**：可点击的列表项 / 开关行 / 文本按钮 / 返回帮助按钮默认带按压震动，由
   `PressVibrationFeedback` 自动附加；**不要**在业务组件里直接调 `vibrator`（见第 7 节）。
10. **FAB 页面底部留白**：挂 `floatingActionButton` 的页面必须传
   `SettingsScaffold(reserveFabSpace = true)`，内容尾部自动追加 1/4 屏高的 `Spacer`，
   避免 FAB 遮挡滚动到底部的元素（已启用：AI 配置编辑、通知模板、UI catalog）。

---

## 6. 常见问题

- **点击没有涟漪？** → 检查是否误用 `Surface(onClick = ...)`；改为显式 `clickable + LocalIndication`。
- **圆角没有随按压变大？** → 确认按下态读取的是 `pressed && enabled`，且四个角都用 `animateDpAsState`。
- **开关按了没反应？** → 确认 `SettingsSegmentedSwitch` 的 `onCheckedChange` 传给行 `onClick`，
  且 `Switch` 的 `onCheckedChange = null`。
- **两个页面的同一控件看起来不一样？** → 对照本规范核对：底色 / 圆角 / 字体 / 间距 / 图标 chip 尺寸。
- **文本按钮之间间距太大？** → 用 `SettingsTextLinkButton`（已移除 48dp 最小触控区）；不要用裸 `TextButton` 堆叠。
- **滑块想放进分段组却出现整行点击效果？** → 用 `SettingsSegmentedBox`（非点击表面），不要用 `SettingsSegmentedItem`。
- **快速按下并抬起不震动？** → 必须是 `PressVibrationFeedback` 的「逐事件收集」实现；不要改成用
  `collectIsPressedAsState` 观察组合期状态（见 7.4 踩坑）。
- **禁用的按钮松手瞬间不震动？** → 确认按下时该按钮是否可用：`armed` 在按下瞬间快照，
  可用→松手仍震动；若按下时已 `enabled = false`（真·禁用占位项）全程静默是预期行为。
- **关闭「应用内全部震动」后仍在震？** → 检查是否有地方绕过了 `rememberHapticsEnabled()` 直接调
  `vibrator`（含打卡按钮，见 7.3）。

---

## 7. 按压触感震动（Press Haptics）

> 设置页可点击控件（列表项 / 开关行 / 文本按钮 / 返回帮助按钮）与免打扰步进滑动条统一带按压触感
> 震动，作为交互反馈的一部分。改动震动方案前请先同步更新本节。

### 7.1 设计规格

| 项 | 值 | 说明 |
|----|-----|------|
| 单次时长 | `PressVibrationDurationMs = 25ms`（默认） | 主页打卡按钮（50ms）的一半，模拟卡片按压触感 |
| 单次强度 | `PressVibrationDefaultAmplitude = 128`（默认，1-255） | 仅设备硬件支持幅度控制（`hasAmplitudeControl()`）时生效 |
| 触发时机 | **按下** + **松手**各一次 | 按下瞬间立即震，松手瞬间立即震，松手不再等待动画 |
| 门控 | 「通用 → 关闭应用内全部震动」 | `HAPTIC_FEEDBACK_ENABLED`（默认 true = 震动），开关默认关闭 |
| 禁用项 | 全程静默 | 按下时 `enabled = false` 则不武装、不震动 |
| 用户可调 | 调试 → 震动调试子页面 | 时长 5-200ms 与强度 1-255 滑块，含测试按钮与恢复默认，存储于 DataStore（`PRESS_VIBRATION_DURATION_MS` / `PRESS_VIBRATION_AMPLITUDE`） |

### 7.2 核心实现（`ui/utils/PressVibrationFeedback.kt`）

```kotlin
@Composable
fun PressVibrationFeedback(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    val hapticsEnabled = rememberHapticsEnabled()
    val (durationMs, amplitude) = rememberPressVibrationParams()   // 读取用户配置
    val currentEnabled by rememberUpdatedState(enabled)
    val currentHaptics by rememberUpdatedState(hapticsEnabled)
    val currentDuration by rememberUpdatedState(durationMs)
    val currentAmplitude by rememberUpdatedState(amplitude)

    LaunchedEffect(interactionSource, context) {
        var armed = false
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    armed = currentEnabled && currentHaptics   // 按下瞬间快照
                    if (armed) vibrateShort(context, currentDuration, currentAmplitude)
                }
                is PressInteraction.Release -> {
                    if (armed) vibrateShort(context, currentDuration, currentAmplitude)
                    armed = false
                }
                is PressInteraction.Cancel -> armed = false
                else -> Unit
            }
        }
    }
}
```

同文件提供两个辅助入口：

- `rememberHapticsEnabled()`：`@Composable`，读取 `UserPreferences.hapticsEnabledFlow`（默认 true）。
- `vibrateShort(context)`：非 Composable 的 25ms 一次性震动，供手动触发场景（打卡按钮、滑动条）复用。
  签名 `vibrateShort(context, durationMs = PressVibrationDurationMs, amplitude = PressVibrationDefaultAmplitude)`；
  强度仅当 `vibrator.hasAmplitudeControl()` 为 true 时生效，否则回退 `VibrationEffect.DEFAULT_AMPLITUDE`。
- `rememberPressVibrationParams()`：`@Composable`，返回 `Pair<Long, Int>`（时长/强度），读取用户调试页配置。

### 7.3 接入位置

| 控件 | 接入方式 |
|------|----------|
| 列表项 / 开关行（`SettingsSegmentedItem` / `SettingsSegmentedSwitch`） | `SettingsListSurface` 内统一 `PressVibrationFeedback(interactionSource, enabled)` |
| 文本链接按钮（`SettingsTextLinkButton`） | 本地 `remember { MutableInteractionSource() }` 传入 `TextButton` 并附加 |
| 返回 / 帮助按钮（`NewSettingsScaffold`） | 各 `IconButton` 本地 interactionSource 并附加 |
| 主页打卡按钮（`HabitScreen.CheckInButton`） | 保留原有 50ms 震动，但用 `rememberHapticsEnabled()` 门控（保证「全部震动」开关生效） |
| 免打扰步进滑动条（`DndRangeSlider`） | 不走交互源；在 `onValueChange` 中 snapped 值变化时 `vibrateShort`（点击轨道 / 拖动跨步 → 震动，同一步内拖动不震），时长/强度同样读取用户配置 |
| 设置页滚动到顶/底（`NewSettingsScaffold`） | 见 7.5 |

其余界面（首页卡片、记录页等）按钮**日后接入**，一律复用 `PressVibrationFeedback` / `rememberHapticsEnabled`，
不要在各自页面自行调 `vibrator`。

### 7.5 滚动到顶/底边缘震动（Scroll Edge Haptics）

设置页统一滚动容器（`NewSettingsScaffold`）在滚动到**顶部或底部**时震**一下**（单次 `vibrateShort`，
不是按压的按下+松手两下）。

实现要点：

- **监听方式**：`LaunchedEffect(scrollState)` + `snapshotFlow { scrollState.value to scrollState.maxValue }`
  逐次比较上次位置；`value <= 0` 且上次 `> 0` → 触顶；`value >= maxValue` 且上次 `< maxValue` → 触底，
  触发时调用一次 `vibrateShort(context, currentDuration, currentAmplitude)`。
- **不可滚动页面无效果**：`maxValue <= 0`（内容不满一屏，无法滚动）时不进入判断，边缘拖拽不震动。
- **门控与参数**：同样受「关闭应用内全部震动」开关（`rememberHapticsEnabled()`）与调试页
  时长/强度配置（`rememberPressVibrationParams()`）控制；用 `rememberUpdatedState` 读取，
  避免开关/参数变化时重启 collector。
- **单次触发**：触顶/触底各自只在「从非边缘位置进入边缘」时震一次，停留在边缘不再重复震。

### 7.4 关键要点（踩坑记录）

1. **必须「逐事件收集」，不要用 `collectIsPressedAsState`**：Compose 会把同一帧内的按下/抬起
   状态合并，极快的按下-抬起可能永远读不到中间 `true`，导致不震动；直接 `interactions.collect`
   每条 Press/Release 事件都不会丢。
2. **用 `rememberUpdatedState` 避免重启 collector**：`enabled` / 开关变化时若以它们为
   `LaunchedEffect` 的 key 重启收集，会重置 `armed` 并可能错过本次按压的 Release；
   正确做法是 collector 常驻、通过 `rememberUpdatedState` 读最新值。
3. **`armed` 在按下瞬间快照**：按下时若可用则武装，之后即使组件主动禁用（如 AI「测试连接」
   按下后 `isTesting=true`）松手仍按武装状态震动；按下时本身禁用则全程静默。
4. **既有震动也要门控**：任何已存在的震动（如打卡按钮 50ms）必须走 `rememberHapticsEnabled()`，
   否则「关闭应用内全部震动」选项名不副实。
