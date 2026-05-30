## 1. 数据模型与基础组件

- [x] 1.1 创建 `EntryItem` 数据类（id, icon, title, description, badgeText, onClick）
- [x] 2.2 创建 `EntryZone` composable，使用 LazyRow 实现横向滚动卡片区域
- [x] 2.3 实现单张入口卡片的渲染（EntryCard composable）：图标、标题、描述、badge
- [x] 2.4 添加右边缘渐变淡出效果，提示可滚动

## 2. 路由与导航

- [x] 2.1 在 `Route.kt` 中新增 `TodayHabits` 和 `LanSync` 路由定义
- [x] 2.2 在 `HabitPulseNavGraph.kt` 中注册新路由，配置进出动画
- [x] 2.3 在 `HomeScreen` 参数中添加 `onViewTodayHabits` 和 `onViewLanSync` 回调
- [x] 2.4 在 `MainActivity` 中将回调传递给 NavGraph

## 3. 与现有列表集成

- [x] 3.1 单列模式（LazyColumn）：将 EntryZone 作为 `item { ... }` 放在 LazyColumn 顶部
- [x] 3.2 瀑布流模式（ScrollableWaterfall）：将 EntryZone 放在双列 Row 之上，横跨全宽
- [x] 3.3 处理搜索激活时 EntryZone 的显示/隐藏动画（AnimatedVisibility）
- [x] 3.4 瀑布流模式下 EntryZone 与列表项的边距对齐（16dp horizontalPadding）

## 4. 今日习惯页面

- [x] 4.1 创建 `TodayHabitsScreen` composable，标题显示"今天的习惯"
- [x] 4.2 复用 `HabitListContent` 展示今日需完成的习惯列表
- [x] 4.3 支持常规模式的列表操作（打卡、编辑、删除等）

## 5. 局域网同步占位页

- [x] 5.1 创建 `LanSyncPlaceholderScreen` composable，显示"功能开发中"提示
- [x] 5.2 包含返回按钮和居中提示文案

## 6. 字符串资源

- [x] 6.1 在 `values/strings.xml` 中添加所有新字符串
- [x] 6.2 在 `values-en-rUS/strings.xml` 中添加英文翻译
- [x] 6.3 在 `values-zh-rHK/strings.xml` 和 `values-zh-rTW/strings.xml` 中添加繁中翻译

## 7. 横向渐变淡出效果

- [x] 7.1 为 LazyRow 添加左右边缘水平渐变淡出叠加层（16dp，horizontalGradient）
- [x] 7.2 修复 IntrinsicSize.Min 崩溃（改用 onSizeChanged 追踪 LazyRow 高度）

## 8. 入口卡片 UI 优化

- [x] 8.1 重构 `EntryItem` 数据类：icon/title 可空，添加 iconTint 字段
- [x] 8.2 紧凑化 `EntryCard` 渲染：内边距 12→8dp，图标 32→24dp，最小宽度 148→64dp，标题 titleMedium→titleSmall
- [x] 8.3 今日卡片：徽章仅显示计数数字（去掉"项"等单位文字）
- [x] 8.4 同步卡片：未连接时隐藏标题文字，图标使用 disabled 灰色（onSurface 38% alpha）
- [x] 8.5 统计卡片：移除 BarChart 图标和"可视化"徽章，仅保留标题文字
- [x] 8.6 空习惯列表时隐藏 EntryZone（入口区域在无习惯时不显示）
- [x] 8.7 更新所有语言字符串资源（今日徽章改为纯 %d 格式）
