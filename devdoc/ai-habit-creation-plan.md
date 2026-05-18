# AI 创建习惯功能 - A2UI 协议实现规范

> 本文档详细规定如何使用 A2UI v0.9 协议 + AGenUI SDK 实现 AI 对话界面。

## 0. 参考资料

| 资源 | 地址 |
|------|------|
| **A2UI v0.9 协议规范** | `https://a2ui.org/specification/v0.9-a2ui/` |
| **A2UI GitHub** | `https://github.com/google/A2UI` |
| **AGenUI SDK** | `ref/AGenUI/` |
| **A2UI Skill** | `ref/AGenUI/skills/a2ui-generation/` |
| **对话 UI 参考** | `ref/gpt_mobile/` |
| **Catalog Schema** | `https://a2ui.org/specification/v0_9/basic_catalog.json` |

## 1. 协议版本选择

使用 **A2UI v0.9 (Draft)** — 理由：
- 专为嵌入 LLM System Prompt 设计，LLM 更容易生成合规 JSON
- 使用 `createSurface` / `updateComponents` / `updateDataModel` 三种消息类型
- 组件使用平铺结构 `{"component": "Text", "text": "..."}` 而非嵌套 `{"Text": {"text": "..."}}`
- 统一 Catalog（组件 + 函数在同一文件）

## 2. 消息类型详解

### 2.1 `createSurface` — 创建 Surface

```json
{
  "version": "v0.9",
  "createSurface": {
    "surfaceId": "habit_chat",
    "catalogId": "https://a2ui.org/specification/v0_9/basic_catalog.json",
    "theme": {
      "primaryColor": "#415F91",
      "agentDisplayName": "AI 助手"
    },
    "sendDataModel": false
  }
}
```

**必须字段**：
- `version`: `"v0.9"`
- `createSurface.surfaceId`: 唯一标识
- `createSurface.catalogId`: 指向 Catalog URI

### 2.2 `updateComponents` — 更新组件

```json
{
  "version": "v0.9",
  "updateComponents": {
    "surfaceId": "habit_chat",
    "components": [
      {
        "id": "root",
        "component": "Column",
        "children": ["title", "content", "actions"]
      },
      {
        "id": "title",
        "component": "Text",
        "text": "确认要创建以下习惯吗？",
        "variant": "h4"
      }
    ]
  }
}
```

**关键规则**：
- 必须有 `id="root"` 的组件
- 子组件通过 `children` 数组引用子组件 ID（不使用内联）
- 所有组件在 `components` 数组中平铺定义

### 2.3 `updateDataModel` — 更新数据模型

```json
{
  "version": "v0.9",
  "updateDataModel": {
    "surfaceId": "habit_chat",
    "path": "/selectedTime",
    "value": "07:00"
  }
}
```

### 2.4 `deleteSurface` — 删除 Surface

```json
{
  "version": "v0.9",
  "deleteSurface": {
    "surfaceId": "habit_chat"
  }
}
```

## 3. A2UI 组件与问题类型映射

### 3.1 完整映射表

| 问题 type (ask_question) | A2UI 组件 | 实现方式 |
|-------------------------|-----------|---------|
| `choice` (单选) | `ChoicePicker` | `variant="mutuallyExclusive"`, `displayStyle="chips"` |
| `day_of_week` (星期选择) | `ChoicePicker` | 7 个 Chip + 快捷选项 Chips |
| `multi_choice` (多选) | `ChoicePicker` | `variant="multipleSelection"`, `displayStyle="checkbox"` |
| `time` (时间选择) | `DateTimeInput` + `Row` | `enableTime=true` + 快捷时间 Chips |
| `text` (文本输入) | `TextField` | `variant="longText"` |
| `confirm` (确认) | `Card` + `List` + `Row` | 组合组件 |

### 3.2 组件详细规范

#### 3.2.1 ChoicePicker — 单选/多选

**Schema**（A2UI v0.9）：

```json
{
  "component": "ChoicePicker",
  "label": "选择重复周期",
  "variant": "mutuallyExclusive",
  "displayStyle": "chips",
  "options": [
    { "label": "每天", "value": "DAILY" },
    { "label": "每周特定几天", "value": "WEEKLY" }
  ],
  "value": []
}
```

**属性**：

| 属性 | 类型 | 说明 |
|------|------|------|
| `component` | const | `"ChoicePicker"` |
| `label` | DynamicString | 分组标签 |
| `variant` | enum | `mutuallyExclusive` / `multipleSelection` |
| `displayStyle` | enum | `chips` / `checkbox` |
| `options` | array | 选项列表 |
| `options[].label` | DynamicString | 显示文本 |
| `options[].value` | string | 稳定值 |
| `value` | DynamicStringList | 当前选中值列表 |

**A2UI JSON 示例（choice）**：

```json
{
  "id": "choice_picker",
  "component": "ChoicePicker",
  "label": "选择重复周期",
  "variant": "mutuallyExclusive",
  "displayStyle": "chips",
  "options": [
    { "label": "每天", "value": "DAILY" },
    { "label": "每周特定几天", "value": "WEEKLY" }
  ],
  "value": []
}
```

#### 3.2.2 DateTimeInput — 时间选择

**Schema**（A2UI v0.9）：

```json
{
  "component": "DateTimeInput",
  "value": "",
  "enableDate": false,
  "enableTime": true,
  "label": "选择提醒时间"
}
```

**属性**：

| 属性 | 类型 | 说明 |
|------|------|------|
| `component` | const | `"DateTimeInput"` |
| `value` | DynamicString | ISO 8601 格式值 |
| `enableDate` | boolean | 允许选择日期 |
| `enableTime` | boolean | 允许选择时间 |
| `min` | DynamicString | 最小值 (ISO 8601) |
| `max` | DynamicString | 最大值 (ISO 8601) |
| `label` | DynamicString | 标签文本 |

**A2UI JSON 示例（time）**：

```json
{
  "id": "time_question",
  "component": "DateTimeInput",
  "value": "",
  "enableTime": true,
  "label": "选择提醒时间"
}
```

#### 3.2.3 TextField — 文本输入

**Schema**（A2UI v0.9）：

```json
{
  "component": "TextField",
  "label": "备注信息",
  "value": "",
  "variant": "longText"
}
```

**属性**：

| 属性 | 类型 | 说明 |
|------|------|------|
| `component` | const | `"TextField"` |
| `label` | DynamicString | 输入框标签 |
| `value` | DynamicString | 当前值 |
| `variant` | enum | `longText` / `shortText` / `number` / `obscured` |
| `validationRegexp` | string | 正则校验 |

#### 3.2.4 Button — 按钮

**Schema**（A2UI v0.9）：

```json
{
  "component": "Button",
  "child": { "componentId": "btn_text" },
  "variant": "primary",
  "action": { "type": "DataChange", "dataPath": "/action", "value": "confirm" }
}
```

**属性**：

| 属性 | 类型 | 说明 |
|------|------|------|
| `component` | const | `"Button"` |
| `child` | ComponentId | 子组件 ID（必须是 Text 或 Icon） |
| `variant` | enum | `default` / `primary` / `borderless` |
| `action` | Action | 交互动作 |

**Action 类型**：

```json
// 数据变更
{ "type": "DataChange", "dataPath": "/selectedTime", "value": "07:00" }

// 导航
{ "type": "Navigation", "route": "confirm" }
```

#### 3.2.5 CheckBox — 复选框

**Schema**（A2UI v0.9）：

```json
{
  "component": "CheckBox",
  "label": "每天 07:00 吃鸡蛋",
  "value": true
}
```

#### 3.2.6 Row / Column — 布局

```json
{
  "id": "confirm_row",
  "component": "Row",
  "justify": "spaceBetween",
  "align": "center",
  "children": ["cancel_btn", "confirm_btn"]
}
```

#### 3.2.7 Card — 卡片容器

```json
{
  "id": "confirm_card",
  "component": "Card",
  "child": { "componentId": "confirm_content" }
}
```

**注意**：`child` 必须是单个组件 ID，多个子组件需要用 Column/Row 包裹。

#### 3.2.8 List — 列表

```json
{
  "id": "habit_list",
  "component": "List",
  "direction": "vertical",
  "children": [
    { "componentId": "habit_item_0" },
    { "componentId": "habit_item_1" }
  ]
}
```

#### 3.2.9 Text — 文本

```json
{
  "id": "title",
  "component": "Text",
  "text": "确认要创建以下习惯吗？",
  "variant": "h4"
}
```

**variant 可选值**：`h1` / `h2` / `h3` / `h4` / `h5` / `caption` / `body`

#### 3.2.10 Divider — 分隔线

```json
{
  "id": "divider",
  "component": "Divider",
  "axis": "horizontal"
}
```

## 4. 完整 A2UI 消息示例

### 4.1 choice 类型（单选卡片）

```json
{
  "version": "v0.9",
  "updateComponents": {
    "surfaceId": "habit_chat",
    "components": [
      {
        "id": "root",
        "component": "Column",
        "children": ["question_text", "choice_picker"]
      },
      {
        "id": "question_text",
        "component": "Text",
        "text": "吃鸡蛋是每天坚持还是每周某几天？",
        "variant": "body"
      },
      {
        "id": "choice_picker",
        "component": "ChoicePicker",
        "label": "选择重复周期",
        "variant": "mutuallyExclusive",
        "displayStyle": "chips",
        "options": [
          { "label": "每天", "value": "DAILY" },
          { "label": "每周特定几天", "value": "WEEKLY" }
        ],
        "value": []
      }
    ]
  }
}
```

### 4.2 time 类型（时间选择器）

```json
{
  "version": "v0.9",
  "updateComponents": {
    "surfaceId": "habit_chat",
    "components": [
      {
        "id": "root",
        "component": "Column",
        "children": ["question_text", "quick_time_row", "time_picker"]
      },
      {
        "id": "question_text",
        "component": "Text",
        "text": "早上几点吃鸡蛋？",
        "variant": "body"
      },
      {
        "id": "quick_time_row",
        "component": "Row",
        "justify": "start",
        "children": ["chip_06", "chip_07", "chip_08"]
      },
      {
        "id": "chip_06",
        "component": "Button",
        "child": { "componentId": "chip_text_06" },
        "variant": "borderless",
        "action": { "type": "DataChange", "dataPath": "/selectedTime", "value": "06:00" }
      },
      {
        "id": "chip_text_06",
        "component": "Text",
        "text": "6点",
        "variant": "body"
      },
      {
        "id": "chip_07",
        "component": "Button",
        "child": { "componentId": "chip_text_07" },
        "variant": "borderless",
        "action": { "type": "DataChange", "dataPath": "/selectedTime", "value": "07:00" }
      },
      {
        "id": "chip_text_07",
        "component": "Text",
        "text": "7点",
        "variant": "body"
      },
      {
        "id": "chip_08",
        "component": "Button",
        "child": { "componentId": "chip_text_08" },
        "variant": "borderless",
        "action": { "type": "DataChange", "dataPath": "/selectedTime", "value": "08:00" }
      },
      {
        "id": "chip_text_08",
        "component": "Text",
        "text": "8点",
        "variant": "body"
      },
      {
        "id": "time_picker",
        "component": "DateTimeInput",
        "value": "",
        "enableTime": true,
        "label": "精确选择时间"
      }
    ]
  }
}
```

### 4.3 day_of_week 类型（星期选择器）

```json
{
  "version": "v0.9",
  "updateComponents": {
    "surfaceId": "habit_chat",
    "components": [
      {
        "id": "root",
        "component": "Column",
        "children": ["question_text", "day_buttons_row", "quick_mode_row"]
      },
      {
        "id": "question_text",
        "component": "Text",
        "text": "哪些天吃鸡蛋？",
        "variant": "body"
      },
      {
        "id": "day_buttons_row",
        "component": "Row",
        "justify": "spaceEvenly",
        "children": ["day_mon", "day_tue", "day_wed", "day_thu", "day_fri", "day_sat", "day_sun"]
      },
      {
        "id": "day_mon",
        "component": "Button",
        "child": { "componentId": "day_text_mon" },
        "variant": "borderless",
        "action": { "type": "DataChange", "dataPath": "/selectedDays/0", "value": "1" }
      },
      {
        "id": "day_text_mon",
        "component": "Text",
        "text": "一",
        "variant": "body"
      },
      {
        "id": "day_tue",
        "component": "Button",
        "child": { "componentId": "day_text_tue" },
        "variant": "borderless",
        "action": { "type": "DataChange", "dataPath": "/selectedDays/1", "value": "2" }
      },
      {
        "id": "day_text_tue",
        "component": "Text",
        "text": "二",
        "variant": "body"
      },
      {
        "id": "day_wed",
        "component": "Button",
        "child": { "componentId": "day_text_wed" },
        "variant": "borderless",
        "action": { "type": "DataChange", "dataPath": "/selectedDays/2", "value": "3" }
      },
      {
        "id": "day_text_wed",
        "component": "Text",
        "text": "三",
        "variant": "body"
      },
      {
        "id": "day_thu",
        "component": "Button",
        "child": { "componentId": "day_text_thu" },
        "variant": "borderless",
        "action": { "type": "DataChange", "dataPath": "/selectedDays/3", "value": "4" }
      },
      {
        "id": "day_text_thu",
        "component": "Text",
        "text": "四",
        "variant": "body"
      },
      {
        "id": "day_fri",
        "component": "Button",
        "child": { "componentId": "day_text_fri" },
        "variant": "borderless",
        "action": { "type": "DataChange", "dataPath": "/selectedDays/4", "value": "5" }
      },
      {
        "id": "day_text_fri",
        "component": "Text",
        "text": "五",
        "variant": "body"
      },
      {
        "id": "day_sat",
        "component": "Button",
        "child": { "componentId": "day_text_sat" },
        "variant": "borderless",
        "action": { "type": "DataChange", "dataPath": "/selectedDays/5", "value": "6" }
      },
      {
        "id": "day_text_sat",
        "component": "Text",
        "text": "六",
        "variant": "body"
      },
      {
        "id": "day_sun",
        "component": "Button",
        "child": { "componentId": "day_text_sun" },
        "variant": "borderless",
        "action": { "type": "DataChange", "dataPath": "/selectedDays/6", "value": "0" }
      },
      {
        "id": "day_text_sun",
        "component": "Text",
        "text": "日",
        "variant": "body"
      },
      {
        "id": "quick_mode_row",
        "component": "Row",
        "justify": "start",
        "children": ["chip_weekdays", "chip_weekend"]
      },
      {
        "id": "chip_weekdays",
        "component": "Button",
        "child": { "componentId": "chip_weekdays_text" },
        "variant": "borderless",
        "action": { "type": "DataChange", "dataPath": "/quickMode", "value": "weekdays" }
      },
      {
        "id": "chip_weekdays_text",
        "component": "Text",
        "text": "工作日",
        "variant": "body"
      },
      {
        "id": "chip_weekend",
        "component": "Button",
        "child": { "componentId": "chip_weekend_text" },
        "variant": "borderless",
        "action": { "type": "DataChange", "dataPath": "/quickMode", "value": "weekend" }
      },
      {
        "id": "chip_weekend_text",
        "component": "Text",
        "text": "周末",
        "variant": "body"
      }
    ]
  }
}
```

### 4.4 confirm 类型（确认卡片）

```json
{
  "version": "v0.9",
  "updateComponents": {
    "surfaceId": "habit_chat",
    "components": [
      {
        "id": "root",
        "component": "Column",
        "children": ["confirm_card"]
      },
      {
        "id": "confirm_card",
        "component": "Card",
        "child": { "componentId": "confirm_content" }
      },
      {
        "id": "confirm_content",
        "component": "Column",
        "children": ["confirm_title", "divider", "habit_list", "confirm_row"]
      },
      {
        "id": "confirm_title",
        "component": "Text",
        "text": "确认要创建以下习惯吗？",
        "variant": "h4"
      },
      {
        "id": "divider",
        "component": "Divider",
        "axis": "horizontal"
      },
      {
        "id": "habit_list",
        "component": "List",
        "direction": "vertical",
        "children": [
          { "componentId": "habit_item_0" },
          { "componentId": "habit_item_1" }
        ]
      },
      {
        "id": "habit_item_0",
        "component": "CheckBox",
        "label": "① 每天 07:00 吃鸡蛋",
        "value": true
      },
      {
        "id": "habit_item_1",
        "component": "CheckBox",
        "label": "② 每周四 18:00 吃肯德基",
        "value": true
      },
      {
        "id": "confirm_row",
        "component": "Row",
        "justify": "spaceBetween",
        "children": ["cancel_btn", "confirm_btn"]
      },
      {
        "id": "cancel_btn",
        "component": "Button",
        "child": { "componentId": "cancel_text" },
        "variant": "borderless",
        "action": { "type": "Navigation", "route": "cancel" }
      },
      {
        "id": "cancel_text",
        "component": "Text",
        "text": "取消"
      },
      {
        "id": "confirm_btn",
        "component": "Button",
        "child": { "componentId": "confirm_text" },
        "variant": "primary",
        "action": { "type": "Navigation", "route": "confirm" }
      },
      {
        "id": "confirm_text",
        "component": "Text",
        "text": "确认创建"
      }
    ]
  }
}
```

### 4.5 text 类型（文本输入）

```json
{
  "version": "v0.9",
  "updateComponents": {
    "surfaceId": "habit_chat",
    "components": [
      {
        "id": "root",
        "component": "Column",
        "children": ["question_text", "text_input"]
      },
      {
        "id": "question_text",
        "component": "Text",
        "text": "关于吃鸡蛋还有什么要备注的吗？",
        "variant": "body"
      },
      {
        "id": "text_input",
        "component": "TextField",
        "label": "备注信息",
        "value": "",
        "variant": "longText"
      }
    ]
  }
}
```

## 5. LLM System Prompt 中的 A2UI 规范

在 System Prompt 中嵌入以下规范，引导 LLM 生成合规的 A2UI JSON：

```
## A2UI v0.9 协议输出规范

当你需要向用户展示问题或信息时，输出符合 A2UI v0.9 规范的 JSON 消息。

### 消息类型

1. **createSurface** — 创建渲染表面（对话开始时发送一次）
```json
{
  "version": "v0.9",
  "createSurface": {
    "surfaceId": "habit_chat",
    "catalogId": "https://a2ui.org/specification/v0_9/basic_catalog.json"
  }
}
```

2. **updateComponents** — 更新 UI 组件
```json
{
  "version": "v0.9",
  "updateComponents": {
    "surfaceId": "habit_chat",
    "components": [
      { "id": "root", "component": "Column", "children": ["..."] },
      { "id": "...", "component": "Text", "text": "...", "variant": "body" }
    ]
  }
}
```

3. **updateDataModel** — 更新数据模型
```json
{
  "version": "v0.9",
  "updateDataModel": {
    "surfaceId": "habit_chat",
    "path": "/selectedTime",
    "value": "07:00"
  }
}
```

### 可用组件

| 组件 | 用途 |
|------|------|
| Text | 文本显示（variant: h1-h5, body, caption） |
| Button | 可点击按钮（variant: default, primary, borderless） |
| ChoicePicker | 单选/多选（variant: mutuallyExclusive, multipleSelection; displayStyle: chips, checkbox） |
| DateTimeInput | 日期时间选择（enableDate, enableTime） |
| TextField | 文本输入框（variant: longText, shortText, obscured） |
| CheckBox | 复选框 |
| Row | 水平布局 |
| Column | 垂直布局 |
| List | 列表 |
| Card | 卡片容器 |
| Divider | 分隔线 |

### 交互事件

按钮点击使用 Action：

```json
// 数据变更
{ "type": "DataChange", "dataPath": "/字段路径", "value": "值" }

// 导航（触发程序端回调）
{ "type": "Navigation", "route": "confirm" }
```

### 关键规则

1. 组件必须通过 ID 引用，不内联子组件
2. 必须有 id="root" 的组件作为根节点
3. children 必须是 ID 数组或模板对象
4. Button 的 child 必须是 Text 或 Icon 组件的 ID
5. Card 的 child 必须是单个组件 ID（多个用 Column/Row 包裹）
```

## 6. AGenUI 集成代码

### 6.1 SurfaceManager 封装

```kotlin
// ai/ui/AGENUISurface.kt
@Composable
fun rememberAGenUISurface(
    activity: ComponentActivity,
    surfaceId: String = "habit_chat",
    onAction: (String, String) -> Unit = { _, _ -> }
): AGenUISurfaceState {
    val surfaceManager = remember {
        SurfaceManager(activity).apply {
            addListener(object : ISurfaceManagerListener {
                override fun onCreateSurface(surface: Surface) {
                    // Surface 创建回调，添加到 Compose 布局
                }

                override fun onDeleteSurface(surface: Surface) {
                    // Surface 销毁回调
                }

                override fun onReceiveActionEvent(event: String) {
                    // 解析 Action 事件并回调
                    val json = JSONObject(event)
                    val actionType = json.optString("type")
                    val route = json.optString("route")
                    val dataPath = json.optString("dataPath")
                    val value = json.optString("value")
                    onAction(actionType, route.ifEmpty { dataPath })
                }
            })
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            surfaceManager.destroy()
        }
    }

    return AGenUISurfaceState(surfaceManager, surfaceId)
}

class AGenUISurfaceState(
    private val surfaceManager: SurfaceManager,
    private val surfaceId: String
) {
    fun createSurface(catalogId: String = "https://a2ui.org/specification/v0_9/basic_catalog.json") {
        surfaceManager.beginTextStream()
        val message = """
            {
              "version": "v0.9",
              "createSurface": {
                "surfaceId": "$surfaceId",
                "catalogId": "$catalogId"
              }
            }
        """.trimIndent()
        surfaceManager.receiveTextChunk(message)
        surfaceManager.endTextStream()
    }

    fun sendComponents(components: List<Map<String, Any>>) {
        surfaceManager.beginTextStream()
        val componentsJson = components.joinToString(",\n") { component ->
            JSONObject(component).toString()
        }
        val message = """
            {
              "version": "v0.9",
              "updateComponents": {
                "surfaceId": "$surfaceId",
                "components": [
                  $componentsJson
                ]
              }
            }
        """.trimIndent()
        surfaceManager.receiveTextChunk(message)
        surfaceManager.endTextStream()
    }

    fun sendDataModel(path: String, value: Any) {
        surfaceManager.beginTextStream()
        val message = JSONObject().apply {
            put("version", "v0.9")
            put("updateDataModel", JSONObject().apply {
                put("surfaceId", surfaceId)
                put("path", path)
                put("value", value)
            })
        }.toString()
        surfaceManager.receiveTextChunk(message)
        surfaceManager.endTextStream()
    }

    fun deleteSurface() {
        surfaceManager.beginTextStream()
        val message = """
            {
              "version": "v0.9",
              "deleteSurface": {
                "surfaceId": "$surfaceId"
              }
            }
        """.trimIndent()
        surfaceManager.receiveTextChunk(message)
        surfaceManager.endTextStream()
    }
}
```

### 6.2 LLM 响应处理流程

```kotlin
// 1. LLM 返回 A2UI JSON
// 2. 解析消息类型
when {
    json.has("createSurface") -> agenui.createSurface(...)
    json.has("updateComponents") -> {
        val components = json.getJSONObject("updateComponents").getJSONArray("components")
        // 渲染组件
    }
    json.has("updateDataModel") -> {
        val path = json.getJSONObject("updateDataModel").getString("path")
        val value = json.getJSONObject("updateDataModel").get("value")
        // 更新本地状态
    }
    // 工具调用（保持原有逻辑）
    json.has("ask_question") || json.has("create_habit") -> handleToolCall(...)
}
```

### 6.3 LLM 输出格式化（Markdown 代码块）

```kotlin
// LLM 输出格式示例
val llmOutput = """
    ```json
    {
      "version": "v0.9",
      "updateComponents": {
        "surfaceId": "habit_chat",
        "components": [...]
      }
    }
    ```
""".trimIndent()

// 解析 JSON
val pattern = Regex("""```json\s*(.*?)\s*```""", RegexOption.DOT_MATCHES_ALL)
val match = pattern.find(llmOutput)
if (match != null) {
    val json = JSONObject(match.groupValues[1])
    // 处理 A2UI 消息
}
```

## 7. 文件清单

| 文件路径 | 说明 |
|---------|------|
| `ai/ui/AGENUISurface.kt` | AGenUI SurfaceManager 封装 |
| `ai/ui/AICreateHabitScreen.kt` | 主界面 |
| `ai/ui/components/AIWelcomeContent.kt` | 欢迎页 |
| `ai/ui/components/AIInputBox.kt` | 输入框 |
| `ai/ui/components/ThinkingBlock.kt` | 思考块 |
| `ai/ui/components/UserBubble.kt` | 用户气泡 |
| `ai/ui/components/AIBubble.kt` | AI 气泡 |
| `ai/a2ui/A2UIParser.kt` | A2UI 消息解析器 |
| `ai/a2ui/A2UIComponentBuilder.kt` | 组件构建器 |
| `ai/a2ui/A2UISystemPrompt.kt` | A2UI System Prompt |

## 8. A2UI Skill 使用方式

参考 `ref/AGenUI/skills/a2ui-generation/SKILL.md`，在 `ConversationManager` 中：

1. 将 A2UI Skill 的 `SKILL.md` 内容作为 System Prompt 的一部分
2. 让 LLM 生成结构化的 A2UI JSON
3. 使用 `A2UIParser` 解析并渲染

**关键提示**：
- 使用 Mode 2（Non-DTO Component）：无 DTO 时生成 A2UI 组件
- 输出顺序：先 `updateComponents`，后 `updateDataModel`
- 验证脚本：`scripts/validate_a2ui.py`