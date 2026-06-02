# 配置 AI

> [!WARNING] 高级设置
> 本页设置内容属于高级设置，确保你已经了解各操作和设置再操作。拿不准的话，找一个懂行朋友陪同操作。

> [!IMPORTANT] 重要提示
> 本站教程仅适用于最新版本的 HabitPulse，如果你设备上的 HabitPulse 与教程里不一样，请更新 HabitPulse。

HabitPulse 支持接入多种大语言模型对话服务。使用 AI 服务前需要完成下面的配置。

## 获取 API 配置

本质上，我们需要配置下面三个项目：

### API 端点：

这是 AI 模型服务的访问地址，有时也称为「基础 URL」，通常以 `https://` 开头。

例如智谱的模型的 API 端点是：`https://open.bigmodel.cn/api/paas/v4/chat/completions`。

对于 HabitPulse，我们选用对话补全模型。

### API 密钥：

相当于你访问 AI 服务的密码，用于验证你的身份和计费。

你需要前往你的模型提供商的 API Key 页面，为 HabitPulse 新建一个 API Key。

例如智谱的 API Key 页面是：`https://bigmodel.cn/apikey/platform`

### 模型：
指定你要使用的具体 AI 模型名称。

> [!WARNING] 可能产生使用费用！
> 模型有付费免费之分，选用高级模型可能产生使用费用。
> 
> HabitPulse 暂时没有 token 计数功能，因此请通过模型提供商留意 token 消耗。

例如智谱的 `glm-4-flash-250414`，截至目前是一个不错的免费模型。

## 在设置中配置 AI

> [!WARNING] TODO
> 需要将来补充