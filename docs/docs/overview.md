# HabitPulse 概述

HabitPulse 是一款专为 Android 设备设计的习惯追踪应用，致力于帮助用户建立和维持良好的日常习惯。通过简洁直观的界面设计和智能化的提醒机制，让习惯养成变得更加轻松有效。

## 各种功能抢先看

<div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 24px; margin: 32px 0;">
  
  <FeatureCard 
    icon="📋"
    title="习惯管理"
    description="灵活创建、智能提醒、批量操作，让习惯养成更轻松"
    :points="['支持自定义习惯标题、备注和重复周期', '可设置多个提醒时间，不错过打卡', '支持批量删除，快速清理习惯']"
  />

  <FeatureCard 
    icon="✅"
    title="打卡记录"
    description="完整历史、日期筛选、习惯过滤，可视化进度展示"
    :points="['记录每次打卡的时间', '按单个习惯或全部习惯筛选记录', '清晰展示完成进度和趋势']"
  />

  <FeatureCard 
    icon="👥"
    title="社交监督"
    description="联系人监督，责任提醒，让坚持更有动力"
    :points="['为习惯添加监督人（邮箱/电话）', '习惯完成情况可通知指定联系人', '多一份监督，多一份坚持']"
  />

  <FeatureCard 
    icon="🎨"
    title="精美设计"
    description="Material Design 3，流畅动画，响应式布局"
    :points="['采用优雅现代的 Material Design 3 设计', '支持 Android 12+ 动态主题色', '自适应手机和平板，横竖屏完美适配']"
  />

</div>


## 为什么选择 HabitPulse？

HabitPulse 小巧精简，专注习惯追踪核心功能，不臃肿。同时采用离线优先设计，所有数据都存储在本地，充分保护你的隐私。

作为一款开源软件，HabitPulse 基于 MIT 协议开放源代码，做到完全透明可信。HabitPulse 在持续更新中，团队积极维护，还会不断加入新特性。

## 技术架构

HabitPulse 采用现代化 Android 开发技术栈：

| 组件 | 说明 |
|------|------|
| **Jetpack Compose** | 声明式 UI 框架，现代化开发体验 |
| **Material Design 3** | 采用最新的 Material Design 设计规范 |
| **Room 数据库** | 本地数据持久化，离线可用 |
| **ViewModel + Flow** | 响应式架构，数据驱动 UI |
| **Navigation Compose** | 导航组件，实现流畅的页面切换动画 |
| **DataStore** | 现代化的偏好设置存储方案 |

更多技术信息，前往 [HabitPulse GitHub 存储库](https://github.com/darrindeyoung791/HabitPulse) 进一步了解。


## 下一步操作

- [获取 HabitPulse](/download)
- [前往 HabitPulse GitHub 存储库](https://github.com/darrindeyoung791/HabitPulse)