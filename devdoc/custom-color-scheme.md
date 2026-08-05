# 自定义色彩方案（Seed-based accent colors）实现规范

> 本文档描述 HabitPulse 中由「颜色种子」派生强调色/卡片配色的统一实现，作为全应用配色方案的唯一参考。
> 新增任何需要「非主题主色」的强调色（图标 chip、入口卡片、统计色块等）时，**必须**复用这套方案，
> 不得就地硬编码明暗主题下的具体色值。

## 概览

HabitPulse 的系统配色使用 Material 3 动态取色（Monet / `dynamicLightColorScheme`），主色随壁纸变化。
但对「图标 chip」「首页入口卡片」等需要**与主色无关的固定强调色**的场景，需要一套独立、稳定的配色。

本方案的核心思路：**不硬编码色值**，而是以一组「颜色种子」作输入，交给 Google 的
`material-color-utilities` 取色器（tonal spot 色板）派生前景/背景色，从而：

- 配色在整个应用内一致（图标 chip、入口卡片共用同一组种子）。
- 天然适配明/暗主题（同一种子在明暗下自动给出两套变体）。
- 保证 `content`（前景）与 `container`（背景）约 **7:1** 的对比度（WCAG AA 以上）。

| 文件 | 包 | 角色 |
|------|-----|------|
| `ui/theme/SeedColor.kt` | 主题核心 | 定义 `AccentTint`、`AccentSeeds`、`seedAccentTint`、`rememberSeedAccentTint` |
| `SettingsIconTint.kt` | 设置组件 | 图标 chip 取色入口（`rememberAccentTint` + `LocalAccentTintOffset`） |
| `HabitScreen.kt` | 首页 | 入口卡片从种子派生容器/内容色 |

---

## 1. 核心定义（SeedColor.kt）

### 1.1 AccentTint

生产中的前景/背景是一个对：

```kotlin
internal data class AccentTint(
    val container: Color,  // 承载背景（如卡片底、chip 底），来源于 seed 的 primaryContainer
    val content: Color     // 其上内容前景（如标题、图标），来源于 seed 的 onPrimaryContainer
)
```

- `container` 用作背景，`content` 用作背景之上的文字/图标。
- 二者来自同一种子的 `primaryContainer` / `onPrimaryContainer`，是 Material 取色器保证对比度的一对。

### 1.2 AccentSeeds（内置种子）

固定 6 组种子（蓝 / 绿 / 橙 / 紫 / 红 / 青），全应用统一引用：

```kotlin
internal val AccentSeeds: List<Color> = listOf(
    Color(0xFF2A5AA8), // blue
    Color(0xFF2E7D46), // green
    Color(0xFF9A6B2A), // orange
    Color(0xFF5E3F9E), // purple
    Color(0xFFA63C47), // red
    Color(0xFF2E7C80)  // teal
)
```

- 按 `index % 6` 循环取用；同一种子在不同页面/分组间保持一致语义（例如「今日」恒为蓝、`0`，
  「即将开始」恒为紫、`3`，「逾期」恒为红、`4`，「统计」恒为青、`5`）。

### 1.3 seedAccentTint（取色）

```kotlin
internal fun seedAccentTint(seed: Color, dark: Boolean): AccentTint {
    val scheme = SchemeTonalSpot(Hct.fromInt(seed.toArgb()), dark, 0.0)
    return AccentTint(
        container = Color(scheme.primaryContainer),
        content   = Color(scheme.onPrimaryContainer)
    )
}
```

- 输入：`Hct.fromInt(seed.toArgb())`（把 `androidx.compose.ui.graphics.Color` 转成 Hct 色彩空间）、
  明暗标志、对比度级别 `0.0`。
- 输出：`primaryContainer` / `onPrimaryContainer`。
- `@Composable` 封装 `rememberSeedAccentTint(seed)` 依据 `isSystemInDarkTheme()` 缓存结果，
  明暗切换时自动重算。

> **依赖来源（重要）**：`Hct` / `SchemeTonalSpot` 等品类位于
> `com.google.android.material.color.utilities`。它们**不**来自独立的
> `material-color-utilities` 依赖，而是由 `com.google.android.material:material:1.14.0`
> 内嵌提供。项目当前的镜像源（google / central / aliyun）无独立 `material-color-utilities`
> artifact（会 404），务必复用已内嵌的类，不要新引入外部依赖。

---

## 2. 派生用法

### 2.1 设置页图标 chip（SettingsIconTint.kt）

```kotlin
@Composable
internal fun rememberAccentTint(index: Int): AccentTint {
    val dark = isSystemInDarkTheme()
    val globalIndex = (LocalAccentTintOffset.current + index).mod(AccentSeeds.size)
    val seed = AccentSeeds[globalIndex]
    return remember(seed, dark) { seedAccentTint(seed, dark) }
}
```

- 组内项用 `index` 选种子；`LocalAccentTintOffset` 让同一页面内**连续的分组**继续往下取色，
  避免同页图标颜色重复（`SettingsSegmentedGroup(tintOffset = …)` 提供偏移）。
- 用法：`tint.container`（chip 底）/ `tint.content`（图标）；禁用态回退到
  `surfaceContainerHighest` / `onSurfaceVariant`（见 `SettingsIconChip`）。

### 2.2 首页入口卡片（HabitScreen.kt）

```kotlin
val todayTint   = rememberSeedAccentTint(AccentSeeds[0])   // blue
val aboutTint   = rememberSeedAccentTint(AccentSeeds[3])   // purple
val overdueTint = rememberSeedAccentTint(AccentSeeds[4])   // red
val statsTint   = rememberSeedAccentTint(AccentSeeds[5])   // teal
```

组装 `EntryItem` 时：

| `EntryItem` 字段 | 取值 | 说明 |
|------------------|------|------|
| `cardColor`（卡片背景） | `tint.container` | **不透明**的容器色，保证与前景对比度 |
| `contentColor`（标题/计数） | `tint.content` | 卡片上的文字/数字 |
| `containerColor`（图标/角标 chip 底） | `tint.content.copy(alpha = 0.12f)` | 用前景低透明度模拟染色 chip |
| `iconTint` | `tint.content` | 图标 |

- 中性项（如「连接与同步」「统计」之外的系统项）用 `surfaceContainerHigh`（卡片）/
  `surfaceContainerHighest`（chip）、`onSurface`（标题）等系统中性色，不走种子。
- `contentColor` / `cardColor` 由 `EntryItem` 显式携带（`EntryZone.kt` 中
  `entry.contentColor ?: onSurface`），**不要在 `remember { … }` 内访问 `MaterialTheme.colorScheme`**
  （见 §4 常见问题）。

---

## 3. 使用约定（开发指南）

1. **所有强调色必须走种子派生**：先确认能否用 `AccentSeeds` 中的语义种子，能找到就
   `rememberSeedAccentTint(AccentSeeds[i])`；不要自己写十六进制并放两套明暗版本。
2. **同一语义的种子全局一致**：「今日/提醒」= 0 蓝，「即将开始」= 3 紫，「逾期」= 4 红，
   「统计」= 5 青；新增场景先查是否有语义匹配的种子。
3. **前景/背景成对使用**：卡片底用 `tint.container`，其上文字/图标用 `tint.content`；
   `container` 与 `content` 永不混用其他色，保证对比度。
4. **chip/角标的半透明染色**：用 `tint.content.copy(alpha = 0.12f)`，不要用
   `tint.container.copy(alpha = …)` 做 chip 底（后者会稀释对比度）。
5. **中性场景不走种子**：系统导航、无关紧要的占位项用 `surfaceContainerHigh` /
   `surfaceContainerHighest` / `onSurface(..Variant)`。
6. **新增一种强调语义时**：优先复用 `AccentSeeds` 现有 6 种子；确需新增再扩展 `AccentSeeds`，
   并同步更新本文档的语义映射表。

---

## 4. 常见问题（含迁移踩坑记录）

- **Color 构造编译失败（seed-color 重载不存在）**：当前 material3 1.4.0 的
  `dynamicLightColorScheme` 等误以为有 `(seed, dark, contrast)` 静态重载——**没有**。
  不要往 `SchemeTonalSpot` / `Hct` 的来源上猜 API，直接引用
  `com.google.android.material.color.utilities` 下的真实类即可。
- **`SchemeTonalSpot` 构造参数**：是 `(Hct, Boolean isDark, Double contrastLevel)`，
  按**位置参数**传，如 `SchemeTonalSpot(Hct.fromInt(seed.toArgb()), dark, 0.0)`。
- **在 `remember { … }` 里访问 `MaterialTheme.colorScheme`**：非 Composable 的 `remember` lambda
  捕获不到 `colorScheme`（编译/行为异常）。先把 `lanContentColor` 等中性色在 Composable 作用域提出来
  再进 `remember`（参考 `HabitScreen.kt` 中 `lanContentColor` 的提法）。
- **对比度不达标**：容器色用了 `content.copy(alpha)` 或混入系统低对比色。回到
  `cardColor = container`（不透明）+ `contentColor = content` 的成对用法。
- **同页图标颜色重复**：没设 `tintOffset`。连续分组记得给 `SettingsSegmentedGroup(tintOffset = …)`。
- **想换库/加依赖**：不要新增 `material-color-utilities` 依赖（镜像 404）；沿用
  `com.google.android.material:material:1.14.0` 内嵌的 utilities。