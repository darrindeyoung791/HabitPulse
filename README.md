# HabitPulse

[![MIT License](https://img.shields.io/badge/License-MIT-yellow.svg)](https://choosealicense.com/licenses/mit/)

HabitPulse 是一个 Material Design 3 风格的、简洁的习惯追踪应用，帮助用户建立和维持良好的日常习惯。通过智能提醒和社交监督功能，让习惯养成更加有效。

HabitPulse 使用 AI 辅助开发。如果您使用 Qwen Code 或 Claude Code 进行二次开发，读取 `QWEN.md` 以获得重要信息。

## 功能特性

- **习惯追踪**：创建、管理和追踪日常习惯
- **智能提醒**：基于时间的提醒功能，帮助用户坚持习惯
- **社交监督**：习惯完成情况可通知指定联系人
- **后台保活**：长期后台运行，确保提醒准时送达
- **MD3设计**：采用 Material Design 3 风格，界面简洁美观

## 技术栈

- **语言**: Kotlin 2.3.10
- **UI 框架**: Jetpack Compose with Material Design 3
- **导航**: Navigation Compose
- **数据库**: Room 2.8.4
- **调度**: AlarmManager (计划中)
- **构建系统**: Gradle Kotlin DSL
- **最低 SDK**: 26 (Android 8.0)
- **目标 SDK**: 36

## 当前版本

**v0.3.5-alpha**（未发布）

## 版本号规则

HabitPulse 使用[语义化版本](https://semver.org/lang/zh-CN/)，版本格式为：`主版本号.次版本号.修订号`。版本号递增规则如下：

1. 主版本号：当做了不兼容的 API 修改
2. 次版本号：当做了向下兼容的功能性新增
3. 修订号：当做了向下兼容的问题修正

其中，本项目约定：

- 只有功能完全实现的正式版，主版本号才为 1 或更高
- 只有当我认为新增的功能应该算作一次完整的实现，修订号才递增
- ~~每次 commit 视作至少一次修订，需递增修订号，即便只是在文档等应用之外的地方变更也递增~~

> 取消这个规定，是因为我经常忘记在 commit 前增加版本号，这样的事情发生得太多了。fk。

- 版本号后可附加 `alpha`、`beta` ，分别用于指示内部测试版（内测版）和公共测试版（公测版）
- 主版本为 0 的版本必须添加 `alpha` 后缀
- 正式发布版版本不带后缀

例如，`0.1.11-alpha`、`1.0.1`、`1.0.2-beta` 是正确的版本名，而 `0.1.1`、`0.1.11-beta` 是错误的版本名。

版本代号在每次正式发布后递增。



## 数据库结构

### habits 表

| 列名                       | 类型 | 描述                                                             |
|--------------------------|------|----------------------------------------------------------------|
| id                       | TEXT (PRIMARY KEY) | 习惯的唯一标识符 (UUID)                                               |
| title                    | TEXT | 习惯的标题                                                          |
| repeatCycle              | TEXT | 重复周期（DAILY 或 WEEKLY）                                           |
| repeatDays               | TEXT | 重复天数列表（用于每周习惯，以 JSON 格式存储，如 [1,3,5] 表示周一、三、五）                  |
| reminderTimes            | TEXT | 提醒时间列表（以 JSON 格式存储，如 ["08:00","20:00"] 表示早上 8 点和晚上 8 点）        |
| notes                    | TEXT | 习惯的备注信息                                                        |
| supervisionMethod        | TEXT | 监督方式（NONE、EMAIL 或 SMS）                                     |
| supervisorEmails         | TEXT | 监督人邮箱地址列表（以 JSON 格式存储）                                         |
| supervisorPhones         | TEXT | 监督人手机号列表（以 JSON 格式存储）                                          |
| completedToday           | INTEGER (BOOLEAN) | 今天是否已完成（0=未完成，1=已完成）                                           |
| completionCount          | INTEGER | 总完成次数                                                          |
| lastCompletedDate        | INTEGER | 最后完成日期（时间戳，可为 null）                                                      |
| createdDate              | INTEGER | 创建日期（时间戳）                                                      |
| modifiedDate             | INTEGER | 最后修改日期（时间戳）                                                  |

### 枚举类型说明

#### RepeatCycle（重复周期）
- `DAILY`：每日重复
- `WEEKLY`：每周重复

#### SupervisionMethod（监督方式）
- `NONE`：不监督，仅本地提醒
- `EMAIL`：邮件汇报
- `SMS`：短信汇报

### 数据库配置
- 数据库名称：`habitpulse_database`
- 数据库版本：1
- 使用 `ListConverters` 和 `EnumConverters` 进行类型转换，支持 `List<Int>`、`List<String>`、`RepeatCycle`、`SupervisionMethod` 等类型的存储和读取

## 开发计划

1. 项目初始化：创建基础项目结构，配置 Gradle 依赖
2. UI基础：实现原生 MD3 风格的主界面，保持简洁
3. 数据层：创建 Room 数据库，用于存储习惯数据
4. 核心功能：实现添加和管理习惯的功能
5. 提醒功能：使用 AlarmManager 实现定时提醒
6. 后台保活：实现长期后台运行机制（注意 Android 限制）
7. 邮件短信集成：实现邮件和短信发送功能，当前仅跳转到发送页面
8. 用户配置：创建设置界面，配置提醒时间和联系人
9. 测试优化：功能测试和性能优化
10. 打包发布：准备发布版本