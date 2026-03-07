# HabitPulse

[![MIT License](https://img.shields.io/badge/License-MIT-yellow.svg)](https://choosealicense.com/licenses/mit/)

HabitPulse 是一个简洁的习惯追踪应用，帮助用户建立和维持良好的日常习惯。通过智能提醒和社交监督功能，让习惯养成更加有效。

HabitPulse 使用 AI 辅助开发

## 功能特性

- **习惯追踪**：创建、管理和追踪日常习惯
- **智能提醒**：基于时间的提醒功能，帮助用户坚持习惯
- **社交监督**：习惯完成情况可通知指定联系人
- **后台保活**：长期后台运行，确保提醒准时送达
- **MD3设计**：采用 Material Design 3 风格，界面简洁美观

## 技术栈

- Kotlin
- Android SDK
- Room 数据库
- AlarmManager
- Material Design 3

## 数据库结构

### habits 表

| 列名 | 类型 | 描述 |
|------|------|------|
| id | INTEGER (PRIMARY KEY, AUTOINCREMENT) | 习惯的唯一标识符 |
| title | TEXT | 习惯的标题 |
| repeatCycle | TEXT | 重复周期（DAILY 或 WEEKLY） |
| repeatDays | TEXT | 重复天数列表（用于每周习惯，以 JSON 格式存储，如 [1,3,5] 表示周一、三、五） |
| reminderTimes | TEXT | 提醒时间列表（以 JSON 格式存储，如 ["08:00","20:00"] 表示早上 8 点和晚上 8 点） |
| notes | TEXT | 习惯的备注信息 |
| supervisionMethod | TEXT | 监督方式（LOCAL_NOTIFICATION_ONLY 或 EMAIL_REPORTING） |
| supervisorEmailAddresses | TEXT | 监督人邮箱地址列表（以 JSON 格式存储） |
| completed | INTEGER (BOOLEAN) | 今天是否已完成（0=未完成，1=已完成） |
| completionCount | INTEGER | 总完成次数 |
| createdDate | INTEGER | 创建日期（时间戳） |

### 枚举类型说明

#### RepeatCycle（重复周期）
- `DAILY`：每日重复
- `WEEKLY`：每周重复

#### SupervisionMethod（监督方式）
- `LOCAL_NOTIFICATION_ONLY`：仅本地通知
- `EMAIL_REPORTING`：邮件报告

### 数据库配置
- 数据库名称：`habit_database`
- 数据库版本：2
- 使用 `HabitTypeConverters` 进行类型转换，支持 List<Int>、List<String> 等类型的存储和读取

## 开发计划

1. 项目初始化：创建基础项目结构，配置 Gradle 依赖
2. UI基础：实现原生 MD3 风格的主界面，保持简洁
3. 数据层：创建 Room 数据库，用于存储习惯数据
4. 核心功能：实现添加和管理习惯的功能
5. 提醒功能：使用 AlarmManager 实现定时提醒
6. 后台保活：实现长期后台运行机制（注意 Android 限制）
7. 邮件集成：添加邮件发送权限并实现发送功能
8. 用户配置：创建设置界面，配置提醒时间和联系人
9. 测试优化：功能测试和性能优化
10. 打包发布：准备发布版本