## Why

README.md 长期未更新，版本号停留在 v0.5.19，大量已完成功能未在文档中体现，同时也缺少对 OpenSpec 工作流的介绍。新接触项目的人无法通过 README 快速了解项目真实状态。

## What Changes

- 更新版本号至 v0.7.11-alpha 及 versionCode 144
- 更新功能特性列表，将已完成的 Reward Bottom Sheet、WebView 安全、Foreground Service、繁体中文适配、屏幕架构重构等标记为已完成
- 在简介中增加 OpenSpec 说明，方便新开发者了解本项目的开发工作流
- 更新技术栈表格与项目结构部分，补充新增模块（service/、receiver/、utils 工具类等）
- 更新项目结构树，对齐实际代码结构
- 更新"贡献指南"，加入 OpenSpec 相关指引
- 更新"当前状态"部分，反映真实进度（移除已完成的计划项，调整 In Progress / Planned）

## Capabilities

### New Capabilities
- `doc-readme`: 维护 README.md 文档，确保其准确反映项目最新状态

### Modified Capabilities
无（纯文档更新，不涉及 spec 级行为变更）

## Impact

仅涉及 `README.md` 文件变更，无代码逻辑影响。需同步更新 `AGENTS.md` 中的版本号信息（如适用）。
