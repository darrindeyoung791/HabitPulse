## Context

README.md 当前版本内容基于 v0.5.19-alpha，实际项目已迭代至 v0.7.11-alpha。AGENTS.md 中的功能列表比 README 更完整。需要将 README 与项目实际状态对齐。

## Goals / Non-Goals

**Goals:**
- README 版本号与实际一致（v0.7.11-alpha）
- 功能列表反映已完成项（Reward Bottom Sheet、WebView 安全、Foreground Service 等）
- 新增 OpenSpec 章节介绍开发工作流
- 项目结构树、技术栈表格与实际代码对齐
- 贡献指南加入 OpenSpec 相关说明
- AGENTS.md 中的版本号同步更新

**Non-Goals:**
- 不改动英文版 README（devdoc/readme/README_EN-US.md），本次仅更新中文版
- 不创建新的 screenshots 或预览图
- 不修改代码或资源文件

## Decisions

1. **直接编辑 README.md 而非模板化生成**
   - 原因：README 是手工维护的文档，结构清晰，仅需局部更新
   - 替代方案：无

2. **保留现有 README 结构和风格**
   - 原因：现有结构（简介→功能→预览→快速开始→技术栈→项目结构→数据库→贡献指南→协议）合理，只需更新各节内容
   - 替代方案：完全重写 → 成本高且无必要

3. **OpenSpec 说明位置**
   - 在简介区域（项目说明之后）增加 OpenSpec 标记和简短说明
   - 在贡献指南中增加 OpenSpec 相关开发指引

4. **版本信息同步**
   - 从 app/build.gradle.kts 读取最新版本号
   - AGENTS.md 的"Package Information"节同步更新

## Risks / Trade-offs

- [低] 英文版 README 可能不同步 → 标记为后续任务，本次不处理
