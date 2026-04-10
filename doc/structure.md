# HabitPulse - 项目结构

> HabitPulse 是一款使用 Kotlin 和 Jetpack Compose 构建的 Android 习惯追踪应用。该应用通过智能提醒和社交监督功能，帮助用户建立和保持每日习惯。

**版本**: 0.5.19-alpha | **最低 SDK**: 26 | **目标 SDK**: 36

---

## 1. 功能图

```mermaid
mindmap
  root((HabitPulse))
    习惯管理
      创建习惯
      编辑习惯
      删除习惯
      打卡签到
      搜索习惯
      拖拽排序
      批量删除
    联系人管理
      查看监督人
      删除联系人
      关联习惯
    记录查询
      历史记录
      按习惯筛选
      按日期筛选
    计划功能
      智能提醒
      社交监督
      日历视图
      计数模块
      AI 建议
      数据备份
```

---

## 2. 架构层次图

### 2.1 整体架构总图

```mermaid
graph TB
    subgraph 入口
        LA[LauncherActivity<br/>启动器]
        WA[WelcomeActivity<br/>引导页]
        MA[MainActivity<br/>主活动]
        SA[SettingsActivity<br/>设置页]
        OA[OpenSourceLicensesActivity<br/>开源许可]
    end

    subgraph 应用层
        APP[HabitPulseApplication<br/>应用类]
        subgraph 单例组件
            DB[HabitDatabase<br/>数据库]
            REP[HabitRepository<br/>数据仓库]
            HVM[HabitViewModel<br/>习惯视图模型]
            RVM[RecordsViewModel<br/>记录视图模型]
            CVM[ContactsViewModel<br/>联系人视图模型]
            PREF[OnboardingPreferences<br/>引导页偏好]
        end
    end

    subgraph UI 层
        HS[HomeScreen<br/>主页]
        HCS[HabitCreationScreen<br/>习惯创建页]
        MSS[MultiSelectSortScreen<br/>多选排序页]
        RS[RecordsScreen<br/>记录页]
        CS[ContactsScreen<br/>联系人页]
        ADS[AdScreen<br/>广告页]
    end

    subgraph 数据层
        subgraph 数据模型
            HAB[Habit 实体]
            HC[HabitCompletion 实体]
        end
        
        subgraph 数据访问对象
            HDAO[HabitDao]
            HCDAO[HabitCompletionDao]
        end
        
        subgraph 类型转换器
            LC[ListConverters<br/>列表转换]
            EC[EnumConverters<br/>枚举转换]
        end
        
        subgraph 偏好设置
            UP[UserPreferences<br/>DataStore 存储]
        end
    end

    subgraph 服务与工具
        FNS[ForegroundNotificationService<br/>前台通知]
        BR[BootReceiver<br/>开机自启]
        NH[NotificationHelper<br/>通知工具]
        NPH[NotificationPermissionHelper<br/>权限工具]
        AU[AccessibilityUtils<br/>无障碍工具]
        DCH[DebounceClickHandler<br/>防抖处理器]
        NG[NavigationGuard<br/>导航守卫]
    end

    LA -->|首次运行| WA
    LA -->|返回用户| MA
    
    WA -->|同意协议| MA
    
    MA --> HS
    HS --> HCS
    HS --> MSS
    HS --> RS
    HS --> CS
    HS -->|设置| SA
    HS -->|广告| ADS
    
    SA --> OA
    
    APP --> DB
    APP --> REP
    APP --> HVM
    APP --> RVM
    APP --> CVM
    APP --> PREF
    
    HS -.使用.-> HVM
    HS -.使用.-> RVM
    HS -.使用.-> CVM
    
    HVM --> REP
    RVM --> REP
    CVM --> REP
    
    REP --> HDAO
    REP --> HCDAO
    
    HDAO --> HAB
    HCDAO --> HC
    HDAO --> LC
    HDAO --> EC
    
    FNS --> NH
    BR --> FNS
    PREF --> UP
```

### 2.2 UI 层结构图

```mermaid
flowchart LR
    subgraph 活动层
        LA[LauncherActivity<br/>启动活动]
        WA[WelcomeActivity<br/>引导活动]
        MA[MainActivity<br/>主活动<br/>NavHost 容器]
        SA[SettingsActivity<br/>设置活动]
    end

    subgraph 主屏幕
        HS[HomeScreen<br/>主页容器]
        
        subgraph 三个标签页
            HT1[习惯标签页]
            HT2[联系人标签页]
            HT3[记录标签页]
        end
    end

    subgraph 子页面
        HCS[HabitCreationScreen<br/>创建/编辑习惯]
        MSS[MultiSelectSortScreen<br/>多选拖拽排序]
        RS[RecordsScreenContent<br/>记录内容]
        CS[ContactsScreenContent<br/>联系人内容]
    end

    subgraph 弹窗组件
        DD[删除确认弹窗]
        DP[日期选择弹窗]
        BS[底部操作面板]
    end

    LA --> WA
    WA --> MA
    MA --> HS
    
    HS --> HT1
    HS --> HT2
    HS --> HT3
    
    HT1 --> RS
    HT2 --> CS
    
    HS -->|点击 FAB| HCS
    HS -->|长按卡片| MSS
    HS -->|点击设置图标| SA
    
    HS -.弹出.-> DD
    HS -.弹出.-> DP
    CS -.弹出.-> BS
```

### 2.3 核心业务图

```mermaid
flowchart TB
    subgraph UI 组合函数
        HS[HomeScreen<br/>主页]
        HCS[HabitCreationScreen<br/>创建/编辑页]
    end

    subgraph 视图模型
        HVM[HabitViewModel<br/>StateFlow<br/>习惯列表状态]
        RVM[RecordsViewModel<br/>StateFlow<br/>记录分组状态]
        CVM[ContactsViewModel<br/>StateFlow<br/>联系人列表状态]
    end

    subgraph 数据仓库
        REP[HabitRepository<br/>单一数据源<br/>复合操作协调]
    end

    subgraph 数据库层
        DB[(Room Database<br/>v3 版本)]
        HDAO[HabitDao<br/>Flow 操作]
        HCDAO[HabitCompletionDao<br/>Flow 操作]
    end

    subgraph 数据表
        HAB[(habits<br/>习惯表)]
        HC[(habit_completions<br/>完成记录表)]
    end

    subgraph 类型转换器
        LC[ListConverters<br/>JSON 序列化]
        EC[EnumConverters<br/>字符串映射]
    end

    HS -->|collectAsState<br/>收集状态| HVM
    HS -->|collectAsState<br/>收集状态| RVM
    HS -->|collectAsState<br/>收集状态| CVM
    
    HCS -->|保存/更新/删除| HVM
    
    HVM -->|CRUD 操作| REP
    RVM -->|查询操作| REP
    CVM -->|查询操作| REP
    
    REP -->|Flow 包装| HDAO
    REP -->|Flow 包装| HCDAO
    
    HDAO --> HAB
    HCDAO --> HC
    
    HDAO -->|List<Int><br/>List<String>| LC
    HDAO -->|RepeatCycle<br/>SupervisionMethod| EC
    
    HAB -.外键级联删除.-> HC
```

### 2.4 服务与工具图

```mermaid
flowchart TB
    subgraph 前台服务
        FNS[ForegroundNotificationService<br/>保活前台服务]
        NH[NotificationHelper<br/>通知创建与管理]
    end

    subgraph 开机自启
        BR[BootReceiver<br/>监听 BOOT_COMPLETED]
    end

    subgraph 权限管理
        NPH[NotificationPermissionHelper<br/>请求通知权限]
    end

    subgraph UI 工具
        AU[AccessibilityUtils<br/>检测 TalkBack]
        DCH[DebounceClickHandler<br/>300ms 防抖]
        NG[NavigationGuard<br/>500ms 导航防抖]
    end

    subgraph 偏好设置
        OP[OnboardingPreferences<br/>SharedPreferences<br/>引导页状态]
        UP[UserPreferences<br/>DataStore<br/>用户设置]
    end

    BR -->|重启| FNS
    FNS -->|构建通知| NH
    NPH -->|请求权限| NH
    
    HS[HomeScreen] -->|检测无障碍| AU
    HS -->|点击事件| DCH
    HS -->|导航操作| NG
    
    WA[WelcomeActivity] --> OP
    SA[SettingsActivity] --> UP
    HS -->|读取/写入| UP
```

---

## 3. 数据库 ER 图

### 3.1 数据库 ER 图

```mermaid
erDiagram
    habits ||--o{ habit_completions : "一对多 级联删除"
    
    habits {
        TEXT id PK "UUID 主键"
        TEXT title "习惯标题"
        TEXT repeatCycle "DAILY 或 WEEKLY"
        TEXT repeatDays "JSON 数组"
        TEXT reminderTimes "JSON 数组"
        TEXT notes "可选备注"
        TEXT supervisionMethod "NONE/EMAIL/SMS"
        TEXT supervisorEmails "JSON 邮箱数组"
        TEXT supervisorPhones "JSON 电话数组"
        BOOLEAN completedToday "今日完成状态"
        INTEGER completionCount "总完成次数"
        INTEGER lastCompletedDate "上次完成时间戳"
        INTEGER createdDate "创建时间戳"
        INTEGER modifiedDate "最后修改时间戳"
        INTEGER sortOrder "自定义排序"
        TEXT timeZone "时区 ID"
    }

    habit_completions {
        TEXT id PK "UUID 主键"
        TEXT habitId FK "外键 引用 habits.id"
        INTEGER completedDate "完成时间戳"
        TEXT completedDateLocal "本地日期 yyyy-MM-dd"
        TEXT timeZone "记录时的时区 ID"
    }
```

### 3.2 habits 表详细结构

```mermaid
classDiagram
    class habits {
        +TEXT id PK "UUID 主键"
        +TEXT title "习惯标题"
        +TEXT repeatCycle "DAILY 每日 / WEEKLY 每周"
        +TEXT repeatDays "JSON 数组 [1,3,5] 表示周一、三、五"
        +TEXT reminderTimes "JSON 数组 [08:00, 20:00]"
        +TEXT notes "可选备注"
        +TEXT supervisionMethod "NONE 无 / EMAIL 邮件 / SMS 短信"
        +TEXT supervisorEmails "JSON 监督人邮箱数组"
        +TEXT supervisorPhones "JSON 监督人电话数组"
        +BOOLEAN completedToday "今日是否完成 0/1"
        +INTEGER completionCount "总完成次数"
        +INTEGER lastCompletedDate "上次完成时间戳 毫秒"
        +INTEGER createdDate "创建时间戳 毫秒"
        +INTEGER modifiedDate "最后修改时间戳 毫秒"
        +INTEGER sortOrder "自定义排序值 越小越靠前"
        +TEXT timeZone "时区 ID 如 Asia/Shanghai"
    }
    
    note for habits "索引: PRIMARY KEY (id)"
```

### 3.3 habit_completions 表详细结构

```mermaid
classDiagram
    class habit_completions {
        +TEXT id PK "UUID 主键"
        +TEXT habitId FK "外键 引用 habits.id"
        +INTEGER completedDate "完成时间戳 毫秒"
        +TEXT completedDateLocal "本地日期 yyyy-MM-dd"
        +TEXT timeZone "记录时的时区 ID"
    }
    
    note for habit_completions "索引: PRIMARY KEY (id), INDEX (habitId), INDEX (completedDateLocal)"
    note for habit_completions "约束: FOREIGN KEY (habitId) REFERENCES habits(id) ON DELETE CASCADE"
```

---

## 4. 响应式导航策略

```mermaid
graph TB
    ROOT{屏幕宽度与方向检测}
    
    ROOT -->|宽度 < 840dp<br/>手机竖屏| BN[底部导航栏<br/>Bottom Navigation Bar]
    ROOT -->|840dp ≤ 宽度 < 1200dp<br/>平板竖屏| BN
    
    ROOT -->|宽度 < 1200dp<br/>手机横屏| NR[侧边导航轨<br/>Navigation Rail]
    
    ROOT -->|宽度 ≥ 1200dp<br/>平板横屏| ND[永久导航抽屉<br/>Permanent Navigation Drawer]
    
    BN --> BN_FAB[FAB: 展开状态]
    BN --> BN_HAM[汉堡菜单: 无]
    
    NR --> NR_FAB[FAB: 展开状态]
    NR --> NR_HAM[汉堡菜单: 无]
    
    ND --> ND_COL[折叠状态: 80dp<br/>仅图标 + 圆形选择指示器]
    ND --> ND_EXP[展开状态: 240dp<br/>图标 + 文字标签]
    ND --> ND_FAB[FAB: 展开状态]
    ND --> ND_HAM[汉堡菜单: 有<br/>点击切换展开/折叠]
```

---

## 5. 核心架构模式

```mermaid
graph LR
    subgraph MVVM 模式
        M[Model<br/>数据模型]
        VM[ViewModel<br/>视图模型]
        V[View<br/>Compose UI]
    end
    
    subgraph 数据流
        DAO[DAO<br/>Flow<T>]
        REP[Repository<br/>单一数据源]
        SF[StateFlow<T>]
        CAS[collectAsStateWithLifecycle]
    end
    
    V -->|collectAsStateWithLifecycle| SF
    SF --> VM
    VM -->|CRUD/查询操作| REP
    REP -->|Flow 包装| DAO
    DAO --> M
    
    M -->|数据变更| DAO
    DAO -->|自动发射| REP
    REP -->|自动发射| SF
    SF -->|自动更新| V
```

| 模式 | 实现方式 |
|------|----------|
| **MVVM** | ViewModel 暴露 `StateFlow<T>`，Compose 通过 `collectAsStateWithLifecycle` 收集 |
| **Repository** | 所有数据操作的单一数据源 |
| **响应式数据** | DAO 返回 `Flow<T>`，实现 UI 自动更新 |
| **单 Activity 架构** | 主应用使用 `MainActivity` + Navigation Compose |
| **共享元素过渡** | `SharedTransitionLayout` 实现页面动画切换 |
| **设备自适应 UI** | 基于 `screenWidthDp` 阈值的响应式导航 |
| **预测性返回手势** | Android 13+ 启用 `enableOnBackInvokedCallback` |
| **动态颜色** | Android 12+ Monet 动态主题 |

---

## 6. 数据库迁移历史

```mermaid
graph LR
    V1[v1 初始版本<br/>habits 表] -->|添加 habit_completions 表| V2[v2 版本<br/>完成记录表]
    V2 -->|添加 sortOrder<br/>timeZone 字段| V3[v3 当前版本<br/>排序与时区]
```

| 版本 | 变更内容 |
|------|----------|
| **v1** | 初始架构，创建 `habits` 表 |
| **v2** | 添加 `habit_completions` 表，外键级联删除 |
| **v3** | `habits` 表新增 `sortOrder` 和 `timeZone` 字段 |

---

## 7. 功能路线图

```mermaid
graph TB
    subgraph Completed[已完成]
        C1[习惯 CRUD 操作]
        C2[习惯打卡签到]
        C3[记录页带筛选]
        C4[监督联系人管理]
        C5[多选拖拽排序]
        C6[搜索功能]
        C7[响应式导航]
        C8[前台保活服务]
        C9[中英双语支持]
    end

    subgraph InProgress[进行中]
        P1[计数模块]
        P2[日历视图]
    end

    subgraph Planned[计划中]
        F1[智能提醒]
        F2[社交监督]
        F3[日历视图完整版]
        F4[AI 习惯建议]
        F5[数据备份/导出]
    end

    C1 -.增强.-> F1
    C4 -.增强.-> F2
    P2 -.增强.-> F3
    C2 -.增强.-> F4
    C1 -.增强.-> F5
    
    style Completed fill:#d4edda,stroke:#28a745
    style InProgress fill:#fff3cd,stroke:#ffc107
    style Planned fill:#e2e3e5,stroke:#6c757d
```

---

*最后更新: 2026 年 4 月 10 日*
