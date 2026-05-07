# HabitPulse - 主页重构计划 (HomeScreen Refactoring)

> 记录主页重构的全部细节与计划进度
>
> **版本**: 0.5.20-alpha | **目标 commit**: 0265b8e | **日期**: 2026-05-07 | **状态**: ✅ 完成

---

## 0. 修订记录 (2026-05-07)

### 架构调整

1. **路由结构变更** (最终决策):
   - 原计划: 保持单一 Route.Home，内部用 currentSection 切换
   - 新决策: 拆分为独立路由 Route.Habits, Route.Contacts, Route.Records
   - 理由: 支持 deep link、简化返回逻辑、ShareTransition 跨路由

2. **搜索状态管理** (最终决策):
   - 原计划: Habits 搜索移到 HabitScreenContent
   - 新决策: Contacts/Records 搜索按钮保留在 HomeScreen，Habits 搜索移到 HabitScreenContent
   - 理由:
     - Contacts 搜索: `isContactsSearchQuery, isContactsSearchActive` 在 HomeScreen 管理 (按"home 界面照旧")
     - Records 筛选: `selectedDate` 在 RecordsViewModel 管理 HomeScreen 只显示按钮
     - Habits 搜索: `isSearchActive, searchQuery` 在 HomeScreen 管理，迁移到 HabitScreenContent

3. **RewardBottomSheet** (最终决策):
   - 原计划: 移到 HabitScreenContent
   - 新决策: 移为 HabitScreenContent 内部调用，可独立实例化
   - 理由: 可能用于其他场景（新版本提醒等）

4. **滚动状态** (保持不变):
   - 三个 section 各有独立 scrollState
   - 使用 rememberSaveable 保持导航后位置

### 目标文件结构

```
HomeScreen (~700行)           HabitScreen (~1200行)
├── 布局容器                 ├── 状态收集 (habits, filteredHabits...)
├── TopAppBar               ├── 搜索状态 + SearchBarFixed
├── Bottom Nav/Rail/Drawer   ├── HabitListContent
├── AnimatedContent         ├── EmptyStateContent
└── DatePicker Dialog      ├── SearchEmptyState
                        └── RewardBottomSheet
```

### 目标行数

| 文件 | 当前 | 目标 |
|------|------|------|
| HomeScreen.kt | 2982 | ≤800 |
| HabitScreen.kt | 新建 | ≤1300 |

---

## 一、目标概述

### 1.1 现状

**HomeScreen.kt 文件统计**:
- 总行数: 2982 行
- 主函数 `HomeScreen`: 第 101-1204 行
- 辅助函数 `HabitListContent`: 第 1397-2465 行

**当前架构**:
```
HomeScreen (单一文件，2982 行)
├── 布局容器 (TopAppBar, Bottom Navigation)
├── 状态管理 (currentSection: Habits/Contacts/Records)
├── 习惯业务 (HabitListContent, 打卡, 搜索)
├── 联系人业务 (调用 ContactsScreenContent)
└── 记录业务 (调用 RecordsScreenContent)
```

### 1.2 目标架构

```
HomeScreen (布局容器，~700 行)
├── 布局容器 (TopAppBar, Bottom Navigation)
├── 状态管理 (currentSection)
└── 内容切换 (AnimatedContent)

HabitScreen (新建，~1200 行) [NEW]
├── 习惯列表 (HabitListContent)
├── 搜索功能 (SearchBarFixed)
├── 打卡功能 (RewardBottomSheet)
└── 空状态 (EmptyStateContent)

ContactsScreen (已有)
RecordsScreen (已有)
```

---

## 二、当前代码分析

### 2.1 HomeScreen.kt 代码分布

| 行号区间 | 内容 | 行数 | 处理方式 |
|---------|------|------|----------|
| 1-99 | Package + Import | 99 | 保留 |
| 99 | HomeSection enum | 1 | 保留 |
| 101-275 | 主函数参数 + ViewModel 获取 | ~170 | 部分保留 |
| 275-426 | 设备检测 + section 状态 + scroll 状态 | ~150 | 保留 |
| 430-579 | homeBody 内容区域 | ~150 | 重构 |
| 581-823 | TopAppBar 构建 | ~240 | 保留 |
| 825-1100 | Bottom Navigation 构建 | ~275 | 保留 |
| 1102-1180 | FAB + Drawer 构建 | ~80 | 保留 |
| 1185-1203 | RewardBottomSheet | ~20 | **移动到 HabitScreen** |
| 1358-1394 | EmptyStateContent | ~36 | **移动到 HabitScreen** |
| 1397-2465 | HabitListContent | ~1068 | **移动到 HabitScreen** |
| 2795-2870 | SearchBarFixed | ~75 | **移动到 HabitScreen** |
| 2898-2969 | SearchEmptyState | ~71 | **移动到 HabitScreen** |
| 2467-2982 | Preview + Fake 数据 | ~515 | 保留 |

### 2.2 需要提取的代码

**HabitListContent 函数签名** (行 1397):
```kotlin
@Composable
fun HabitListContent(
    modifier: Modifier = Modifier,
    habits: List<Habit>,
    onHabitClick: (Habit) -> Unit,
    onCheckIn: (Habit) -> Unit,
    onUndoCompletion: (Habit) -> Unit,
    onDeleteHabit: (Habit) -> Unit,
    onNavigateToMultiSelect: (habitId: UUID) -> Unit,
    nestedScrollConnection: NestedScrollConnection? = null,
    newlyAddedHabitId: UUID? = null,
    listState: LazyListState = remember { LazyListState() },
    waterfallScrollState: ScrollState,
    bringIntoViewRequester: BringIntoViewRequester? = null,
    forceTabletLandscape: Boolean = false,
    searchQuery: String = "",
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null,
    multiSelectTargetHabitId: UUID? = null
)
```

### 2.3 相关文件

| 文件 | 路径 | 说明 |
|------|------|------|
| HomeScreen.kt | ui/screens/HomeScreen.kt | 主文件 (需拆分) |
| ContactsScreen.kt | ui/screens/ContactsScreen.kt | 已有，参考 |
| RecordsScreen.kt | ui/screens/RecordsScreen.kt | 已有，参考 |
| HabitPulseNavGraph.kt | navigation/HabitPulseNavGraph.kt | 需修改 |
| Route.kt | navigation/Route.kt | 需扩展 |
| RewardBottomSheet.kt | ui/screens/RewardBottomSheet.kt | 已有，独立组件 |

---

## 三、重构步骤

### 步骤 1: 扩展 Route 定义

**目标**: 添加独立路由

**操作**:
1. 在 `Route.kt` 添加 `Route.Contacts`, `Route.Records`

```kotlin
sealed class Route(val route: String) {
    object Home : Route("home")
    object Habits : Route("habits")
    object Contacts : Route("contacts")
    object Records : Route("records")
    object CreateHabit : Route("create_habit")
    object EditHabit : Route("edit_habit/{habitId}") {
        fun createRoute(habitId: UUID): String = "edit_habit/$habitId"
    }
    object Settings : Route("settings")
    object MultiSelectSort : Route("multi_select_sort")
    object Help : Route("help?url={url}") {
        fun createRoute(url: String): String = "help?url=${URLEncoder.encode(url, "UTF-8")}"
    }
}
```

---

### 步骤 2: 创建 HabitScreen.kt 文件结构

**目标**: 创建 `HabitScreenContent` 函数骨架

**操作**:
1. 创建 `ui/screens/HabitScreen.kt`
2. 添加 package 声明
3. 添加必要 import
4. 定义 `HabitScreenContent` 函数

**函数签名**:
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitScreenContent(
    modifier: Modifier = Modifier,
    application: HabitPulseApplication? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    listState: LazyListState = remember { LazyListState() },
    waterfallScrollState: ScrollState = remember { ScrollState(0) },
    bringIntoViewRequester: BringIntoViewRequester = remember { BringIntoViewRequester() },
    forceTabletLandscape: Boolean = false,
    onCreateHabit: () -> Unit,
    onEditHabit: (Habit) -> Unit,
    onNavigateToMultiSelect: (UUID) -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null
)
```

---

### 步骤 3: 移动 HabitListContent

**目标**: 将 `HabitListContent` 函数从 HomeScreen.kt 移动到 HabitScreen.kt

**操作**:
1. 复制 `HabitListContent` 函数 (行 1397-2465) 到 HabitScreen.kt
2. 保留所有参数和实现

---

### 步骤 4: 移动辅助函数

**目标**: 移动辅助显示函数

**需要移动**:
1. `EmptyStateContent` (行 1358-1394)
2. `SearchBarFixed` (行 2795-2870)
3. `SearchEmptyState` (行 2898-2969)

---

### 步骤 5: 移动状态管理

**目标**: 将习惯相关的状态管理移动到 HabitScreenContent

**从 HomeScreen 移动**:
1. ViewModel 获取
2. habits 状态收集
3. filteredHabits 状态收集
4. isLoading 状态收集
5. searchQuery 状态收集
6. newlyAddedHabitId 状态收集
7. rewardSheetHabit / showRewardSheet 状态收集
8. hasLoadedHabits 状态
9. isSearchActive / isSearchFocused 状态

---

### 步骤 6: 移动 RewardBottomSheet

**目标**: 将奖励弹窗移动到 HabitScreenContent

**原代码位置**: 行 1185-1203

```kotlin
// 奖励底部弹窗
if (showRewardSheet && rewardSheetHabit != null) {
    val currentHabit = rewardSheetHabit!!
    val displayCompletionCount = currentHabit.completionCount + 1
    
    RewardBottomSheet(
        habit = currentHabit,
        completionCount = displayCompletionCount,
        onDismiss = { viewModel.dismissRewardSheet() },
        onComplete = { viewModel.dismissRewardSheet() },
        onNotifySupervisor = { viewModel.dismissRewardSheet() },
        onSkipNotification = { viewModel.dismissRewardSheet() }
    )
}
```

---

### 步骤 7: 修改 HomeScreen 调用

**目标**: HomeScreen 调用新的 HabitScreenContent

**修改位置**: 行 430-577

**原代码** (简化):
```kotlin
HomeSection.Habits -> {
    // 大量习惯相关代码...
}

HomeSection.Contacts -> {
    ContactsScreenContent(...)
}

HomeSection.Records -> {
    RecordsScreenContent(...)
}
```

**修改为**:
```kotlin
HomeSection.Habits -> {
    HabitScreenContent(
        modifier = modifier,
        application = application,
        scrollBehavior = habitsScrollBehavior,
        listState = habitsScrollState,
        waterfallScrollState = waterfallScrollState,
        bringIntoViewRequester = bringIntoViewRequester,
        forceTabletLandscape = forceTabletLandscape == true,
        onCreateHabit = onCreateHabit,
        onEditHabit = onEditHabit,
        onNavigateToMultiSelect = { habitId -> 
            multiSelectTargetHabitId = habitId
            onNavigateToMultiSelect(habitId)
        },
        sharedTransitionScope = sharedTransitionScope,
        animatedContentScope = animatedContentScope
    )
}

HomeSection.Contacts -> { ... }
HomeSection.Records -> { ... }
```

---

### 步骤 8: 清理 HomeScreen

**目标**: 移除已移动的代码

**移除**:
1. ViewModel 获取逻辑的一部分 (习惯相关)
2. 习惯状态收集
3. 搜索状态管理
4. homeBody 中的习惯代码
5. RewardBottomSheet 代码
6. 移动后的辅助函数

---

## 四、关键决策

### 4.1 Navigation 路由

**决策**: 拆分为独立路由 Route.Habits, Route.Contacts, Route.Records

**理由**:
- 支持 deep link
- 简化返回逻辑
- ShareTransition 跨路由

### 4.2 ViewModel 管理

**决策**: 保持 Application 层单例

**实现方式**:
- `HabitPulseApplication` 中 lazy 单例
- HabitScreenContent 通过 application 参数获取

### 4.3 Search 实现

**决策**: 各 Screen 自行实现

- HabitScreenContent: 习惯搜索 (迁移)
- ContactsScreenContent: 搜索按钮在 HomeScreen，实际搜索在 ContactsScreenContent
- RecordsScreenContent: 无搜索，只有日期筛选在 RecordsViewModel

---

## 五、验收标准

### 5.1 功能验收

- [ ] 打卡后 RewardBottomSheet 正常弹出
- [ ] 三个 tab 切换正常
- [ ] 搜索功能正常
- [ ] 新建习惯 FAB 正常工作
- [ ] 响应式布局正常

### 5.2 代码验收

- [ ] HomeScreen.kt 行数 ≤ 800 行
- [ ] HabitScreen.kt 行数 ≤ 1300 行
- [ ] 编译无错误

---

## 六、风险与缓解

### 6.1 Import 冲��

**缓解**: 
- 逐步移动，每次编译验证
- 使用完全限定名

### 6.2 状态一致性

**缓解**: 
- 使用 ViewModel 单例
- 传递必要回调

---

## 七、进度追踪

- [x] 步骤 1: 扩展 Route 定义 (已添加 Route.Habits/Contacts/Records 定义)
- [x] 步骤 2: 创建 HabitScreen.kt 框架
- [x] 步骤 3: 移动 HabitListContent
- [x] 步骤 4: 移动辅助函数 (EmptyState, SearchBar, SearchEmpty)
- [x] 步骤 5: 移动状态管理 (Search, RewardSheet, ViewModel 获取)
- [x] 步骤 6: 移动 RewardBottomSheet
- [x] 步骤 7: 修改 HomeScreen 调用 (HabitScreenContent 替换)
- [x] 步骤 8: 清理 HomeScreen (移除重复代码，修复搜索)
- [x] 验证: 编译 + 运行测试 (通过)