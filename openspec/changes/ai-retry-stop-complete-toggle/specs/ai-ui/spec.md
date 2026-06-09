## MODIFIED Requirements

### Requirement: 系统显示错误状态
When an error occurs, the system SHALL display the error message and a retry button.

#### Scenario: API 连接错误
- **WHEN** API call fails due to network issues
- **THEN** system displays error message and retry button

#### Scenario: API 密钥错误
- **WHEN** API returns 401 Unauthorized
- **THEN** system displays "API 密钥无效，请前往设置检查" and retry button

#### Scenario: 流式输出中错误发生
- **WHEN** error occurs during streaming output
- **THEN** system stops streaming, displays error text, shows retry button below the last AI message

### Requirement: 用户可以停止对话
The AI Create screen SHALL include a stop button to manually end the conversation, and after stopping, SHALL allow the user to retry, send a new message, or clear the conversation.

#### Scenario: 用户点击停止
- **WHEN** user taps the stop button
- **THEN** system stops AI generation immediately, resets loading state, sets the last AI message as non-streaming, and displays retry button if there is content to retry

#### Scenario: 停止后无习惯卡片
- **WHEN** user taps the stop button before any habits are created
- **THEN** system resets loading state, shows retry button on the last AI message

#### Scenario: 停止后有习惯卡片
- **WHEN** user taps the stop button after some habits are created
- **THEN** system keeps the habit cards visible, resets loading state, shows retry button, and does NOT auto-continue the conversation

### Requirement: 重试按钮清除内容并回填输入框
When the user taps the retry button, the system SHALL remove all content from the user's last message onward, auto-fill the input box with the user's last message text, and re-send the request.

#### Scenario: 点击重试
- **WHEN** user taps the retry button below an AI message
- **THEN** system removes the last user message and all subsequent content (AI messages, habit cards, questions), fills the input box with the user's last message text, and initiates a new AI request

#### Scenario: 重试后修改输入
- **WHEN** user taps retry and the input box is pre-filled with the previous text
- **THEN** user can modify the text before sending, or send it unchanged

## ADDED Requirements

### Requirement: 重试按钮在停止后显示
When generation is stopped by the user, the retry button SHALL appear below the last AI message.

#### Scenario: 停止后显示重试
- **WHEN** user stops generation via the stop button
- **THEN** system displays a retry button below the last AI message, allowing the user to re-send their previous input

#### Scenario: 停止后重试条件判断
- **WHEN** generation has stopped and there is at least one AI message
- **THEN** retry button is visible regardless of whether the last message is marked as streaming

### Requirement: 重试后输入框自动回填
When retry is triggered, the user's last message text SHALL be automatically filled into the input box.

#### Scenario: 回填输入框
- **WHEN** user clicks retry
- **THEN** the input box is pre-populated with the text of the user's last message, and all messages from that user message onward are removed

#### Scenario: 回填后用户可编辑
- **WHEN** input box is pre-filled with the user's last message
- **THEN** user can edit the text before sending
