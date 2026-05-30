## ADDED Requirements

### Requirement: EntryZone 可横向滚动
系统 SHALL 在 Habits 标签页顶部渲染一个可横向滚动的入口卡片区域（EntryZone）。

#### Scenario: 默认显示
- **WHEN** 用户打开 Habits 标签页且搜索未激活
- **THEN** 顶部显示 EntryZone，包含"今天的习惯"和"局域网同步"两张卡片

#### Scenario: 横向滚动
- **WHEN** 入口卡片总宽度超过屏幕宽度
- **THEN** EntryZone 支持手指横向滑动查看更多卡片

#### Scenario: 搜索激活时隐藏
- **WHEN** 用户点击搜索按钮，搜索栏展开
- **THEN** EntryZone 自动隐藏（AnimatedVisibility with fadeOut）

#### Scenario: 搜索关闭后恢复
- **WHEN** 用户退出搜索
- **THEN** EntryZone 自动恢复显示（AnimatedVisibility with fadeIn）

### Requirement: 入口卡片渲染
每张入口卡片 SHALL 包含：图标、标题、描述文字、状态标签（badge），并响应点击事件。

#### Scenario: 卡片完整渲染
- **WHEN** EntryZone 显示
- **THEN** 每张卡片显示图标、标题、描述和状态标签

#### Scenario: 卡片点击
- **WHEN** 用户点击某张入口卡片
- **THEN** 执行该卡片绑定的 onClick 回调

#### Scenario: 卡片内容截断
- **WHEN** 卡片宽度受限
- **THEN** 标题和描述使用 TextOverflow.Ellipsis 截断，badge 文字自适应

### Requirement: "今天的习惯"入口
"今天的习惯"入口 SHALL 点击后导航到 TodayHabitsScreen，显示当前日期需要完成的习惯列表。

#### Scenario: 点击跳转
- **WHEN** 用户点击"今天的习惯"卡片
- **THEN** 导航到 TodayHabitsScreen，页面标题显示"今天的习惯"，列表过滤为今天需完成的习惯

### Requirement: "局域网同步"入口
"局域网同步"入口 SHALL 点击后导航到占位页面，提示功能开发中。

#### Scenario: 点击跳转
- **WHEN** 用户点击"局域网同步"卡片
- **THEN** 导航到占位页面，显示"功能开发中"提示

#### Scenario: 状态标签
- **WHEN** 局域网同步卡片渲染
- **THEN** 状态标签显示"未连接"

### Requirement: 响应式适配
EntryZone SHALL 在所有导航模式下正常显示。

#### Scenario: 手机竖屏
- **WHEN** 手机竖屏模式（BottomNavigationBar）
- **THEN** EntryZone 占满可用宽度，卡片最小宽度 150dp

#### Scenario: 手机横屏
- **WHEN** 手机横屏模式（NavigationRail）
- **THEN** EntryZone 在内容区域顶部渲染

#### Scenario: 平板横屏瀑布流
- **WHEN** 平板横屏双列瀑布流模式
- **THEN** EntryZone 在瀑布流顶部渲染，横跨双列全宽

#### Scenario: 平板竖屏
- **WHEN** 平板竖屏模式（BottomNavigationBar）
- **THEN** EntryZone 占满可用宽度

### Requirement: 横向渐变淡出效果
系统 SHALL 在 EntryZone 的 LazyRow 左右边缘渲染渐变半透明遮罩。

#### Scenario: 滚动时显示边缘淡出
- **WHEN** LazyRow 未滚动到起点或终点
- **THEN** 对应侧显示 16dp 宽的水平渐变遮罩（background → Transparent），200ms tween 动画
- **WHEN** LazyRow 滚动到起点或终点
- **THEN** 对应侧的渐变遮罩淡出（alpha → 0）

### Requirement: 紧凑化卡片渲染
入口卡片 SHALL 采用紧凑布局以减少横向空间占用。

#### Scenario: 尺寸缩减
- **GIVEN** 入口卡片渲染
- **THEN** 内边距为 8dp，图标框为 24dp（圆角 8dp），图标为 14dp，标题使用 titleSmall
- **THEN** 卡片最小宽度为 64dp

### Requirement: 条件渲染
系统 SHALL 根据卡片类型决定显示的视觉元素。

#### Scenario: 今日卡片
- **GIVEN** "今天"卡片渲染
- **THEN** 显示 List 图标、标题"今天"、仅数字计数徽章（无单位）

#### Scenario: 同步卡片（未连接）
- **GIVEN** 局域网同步卡片渲染且功能未连接
- **THEN** 隐藏标题文字和徽章
- **THEN** Sync 图标颜色使用 disabled 灰色（onSurface color 38% alpha）

#### Scenario: 统计卡片
- **GIVEN** "统计"卡片渲染
- **THEN** 不显示图标和徽章
- **THEN** 仅显示"统计"标题文字

### Requirement: 空习惯隐藏
系统 SHALL 在习惯列表为空时隐藏 EntryZone。

#### Scenario: 无习惯时隐藏
- **WHEN** 习惯列表为空（habits.isEmpty()）
- **THEN** EntryZone 不显示，仅显示空的习惯列表提示
- **WHEN** 习惯列表不为空
- **THEN** EntryZone 在习惯列表顶部正常显示

### Requirement: 可扩展性
EntryZone SHALL 支持通过数据列表添加更多入口卡片。

#### Scenario: 未来新增卡片
- **WHEN** 在 EntryItem 数据列表中添加新卡片
- **THEN** 新卡片在 EntryZone 中自动渲染，位于现有卡片之后
