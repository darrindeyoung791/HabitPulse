## Context

首页 Habits 标签目前只有单一的习惯卡片列表（单列 LazyColumn 或双列瀑布流），缺少功能入口的视觉引导。用户希望首页顶部有可横向滚动的入口区域，初始包含"今天的习惯"和"局域网同步"两个入口，未来可扩展。

现有布局结构：
- `HabitScreenContent` — 包含搜索栏 + 习惯列表（HabitListContent）
- `HabitListContent` — 根据屏幕宽度决定单列 LazyColumn 或双列 ScrollableWaterfall
- `HomeScreen` — 管理导航模式和 Section 切换

## Goals / Non-Goals

**Goals:**
- 实现 `EntryZone` 可横向滚动入口卡片区域，渲染在习惯列表之上
- 入口卡片包含：图标、标题、描述、状态标签（badge）
- 支持初始两个入口："今天的习惯" 和 "局域网同步"
- 适配四种导航模式（手机竖屏/横屏、平板竖屏/横屏）
- 搜索激活时自动隐藏 EntryZone
- 可扩展的入口列表（未来可添加新卡片）
- "今天的习惯"跳转到新的今日习惯视图
- "局域网同步"跳转到占位页面

**Non-Goals:**
- 不实现局域网同步的实际功能（仅占位页面）
- 不修改数据库 schema
- 不修改 ViewModel 核心 CRUD 逻辑
- 不改变现有习惯卡片的渲染和交互
- 不涉及导航系统架构变更

## Decisions

### D1: EntryZone 随内容滚动（而非固定）
- **选择**：EntryZone 作为习惯列表的一部分一起滚动（列表头部）
- **理由**：实现简单，无需管理固定定位的坐标系统。与 LazyColumn header item 的 slot 体系一致。搜索激活时只需隐藏即可，不影响滚动状态
- **替代方案**：固定定位需要自定义 NestedScrollConnection 来处理碰撞检测，复杂度高

### D2: 使用 LazyRow 做横向滚动
- **选择**：`LazyRow` 渲染横向卡片列表
- **理由**：内置懒加载、平滑滚动、无障碍支持。卡片数量极少（即使扩展到 5-6 个也不会有性能问题），但 LazyRow 提供了正确的语义和触摸行为
- **替代方案**：`Row` + `horizontalScroll` — 更简单但缺少 LazyRow 的无障碍支持

### D3: "今天的习惯"作为新路由页面
- **选择**：新增 `TodayHabitsScreen` 作为独立导航目标
- **理由**：与 `HabitCreationScreen`、`MultiSelectSortScreen` 一致的导航模式。提供专门针对今日过滤的交互空间，不干扰主列表状态
- **替代方案**：在主列表内联过滤 — 会与搜索状态冲突，且需要额外状态管理

### D4: 瀑布流模式下 EntryZone 横跨两列
- **选择**：EntryZone 放置在 ScrollableWaterfall 的 Column 中，占满全宽，位于双列 Row 之前
- **理由**：与列表共享同一个 scrollState，滚动行为自然一致
- **替代方案**：将 EntryZone 放在瀑布流外部（固定定位）— 复杂度高

### D5: Entry Zone 数据模型
```kotlin
data class EntryItem(
    val id: String,
    val icon: ImageVector?,        // 可为 null（统计卡片无图标）
    val title: String?,            // 可为 null（同步未连接时隐藏标题）
    val badgeText: String?,        // 可为 null（同步/统计卡片无徽章）
    val iconTint: Color? = null,   // 覆盖图标颜色（同步未连接时灰色）
    val onClick: () -> Unit
)
```
- **理由**：纯数据驱动，点击行为通过 lambda 注入。可空字段支持条件渲染，iconTint 支持视觉状态区分

### D6: 卡片紧凑化设计
- **选择**：缩小卡片内边距（12→8dp）、图标尺寸（32→24dp）、最小宽度（148→64dp）
- **理由**：入口卡片仅 3 张，无需大尺寸图标和文字。紧凑设计在窄屏手机上减少滚动需求
- **替代方案**：保持原尺寸 — 占用空间大、入门区域过于醒目

### D7: 同步未连接状态表达
- **选择**：隐藏标题文字，图标置灰（onSurface 38% alpha）
- **理由**：不使用文字"未连接"可节省空间；灰色图标暗示功能不可用状态，语义明确
- **替代方案**：显示"未连接"badge — 占用额外宽度

### D8: 统计卡片简化
- **选择**：移除 BarChart 图标和"可视化"徽章，仅保留标题文字
- **理由**：统计功能尚未实现，图标和"可视化"徽章与无功能状态不匹配。纯文字卡片更轻量且诚实
- **替代方案**：保留图标和"敬请期待"徽章 — 语义上矛盾（可视化未实现却显示可视化图标）

### D9: 空习惯隐藏 EntryZone
- **选择**：习惯列表为空时不显示 EntryZone
- **理由**：入口卡片（今日、同步、统计）在有习惯时才有实际意义。无习惯时显示空状态引导更清晰
- **替代方案**：始终显示 EntryZone — 卡片点击跳转后功能仍不完整

## Risks / Trade-offs

| 风险 | 缓解措施 |
|------|----------|
| 横向滚动区域在窄屏手机上宽度不足，卡片内容被截断 | 卡片最小宽度 64dp，允许截断文本用 ellipsis |
| 瀑布流模式下 EntryZone 与双列布局的边距对齐 | EntryZone 使用与瀑布流相同的 horizontalPadding（16dp） |
| LazyRow 套在 LazyColumn 中可能嵌套滚动冲突 | LazyRow 默认拦截横向滚动，LazyColumn 拦截纵向，Compose 自动处理嵌套滚动 |
| 新增路由需要更新 NavGraph 和 Route 定义 | 遵循现有命名规范，复用 HomeScreen 的 callback 模式 |
| 同步/统计卡片条件渲染可能造成视觉不一致 | 同步卡片仅图标、统计卡片仅文字，两者宽度差异较大但设计上可接受（统一为紧凑圆角卡片样式） |
| IconTint 在 remember 块内捕获 MaterialTheme 值，主题切换时可能不更新 | 将 disabledTint 提取为 remember 的 key，主题变化时重新计算 |
