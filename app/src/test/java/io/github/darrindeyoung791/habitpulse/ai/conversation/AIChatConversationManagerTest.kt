package io.github.darrindeyoung791.habitpulse.ai.conversation

import io.github.darrindeyoung791.habitpulse.ai.llm.FunctionCallData
import io.github.darrindeyoung791.habitpulse.ai.llm.LLMClient
import io.github.darrindeyoung791.habitpulse.ai.llm.LLMConfig
import io.github.darrindeyoung791.habitpulse.ai.llm.Message
import io.github.darrindeyoung791.habitpulse.ai.llm.ToolCallData
import io.github.darrindeyoung791.habitpulse.ai.llm.ToolDef
import io.github.darrindeyoung791.habitpulse.ai.tools.chat.ChatEvent
import io.github.darrindeyoung791.habitpulse.ai.tools.chat.ChatToolRegistry
import io.github.darrindeyoung791.habitpulse.ai.tools.chat.CreateHabitChatTool
import io.github.darrindeyoung791.habitpulse.ai.tools.chat.EditHabitChatTool
import io.github.darrindeyoung791.habitpulse.ai.tools.chat.AskQuestionChatTool
import io.github.darrindeyoung791.habitpulse.ai.tools.chat.ReplyChatTool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 新版对话引擎测试：工具链循环 / 暂停点 / 重试上限 / 流式回退 / usage 累计 / Guard 拦截。
 * 通过脚本化的 FakeLLMClient 注入确定性响应序列。
 */
class AIChatConversationManagerTest {

    private fun toolRegistry() = ChatToolRegistry(
        listOf(
            CreateHabitChatTool,
            EditHabitChatTool,
            AskQuestionChatTool,
            ReplyChatTool
        )
    )

    private class ScriptedLLMClient : LLMClient(
        LLMConfig(apiEndpoint = "http://fake", apiKey = "k", modelName = "m", maxRetries = 1)
    ) {
        val calls = mutableListOf<List<Message>>()
        private val nonStreaming = ArrayDeque<LLMClient.LLMResult>()
        private val streamingScripts = ArrayDeque<() -> Flow<LLMClient.StreamChunk>>()

        fun enqueueNonStreaming(result: LLMClient.LLMResult) = nonStreaming.addLast(result)
        fun enqueueStreaming(script: () -> Flow<LLMClient.StreamChunk>) = streamingScripts.addLast(script)

        override suspend fun chat(
            messages: List<Message>,
            tools: List<ToolDef>?,
            toolChoice: String?
        ): LLMClient.LLMResult {
            calls += messages.toList()
            if (nonStreaming.isEmpty()) error("unexpected non-streaming chat call #${calls.size}")
            return nonStreaming.removeFirst()
        }

        override fun chatStream(
            messages: List<Message>,
            tools: List<ToolDef>?,
            toolChoice: String?
        ): Flow<LLMClient.StreamChunk> {
            calls += messages.toList()
            if (streamingScripts.isEmpty()) error("unexpected streaming chat call #${calls.size}")
            return streamingScripts.removeFirst().invoke()
        }
    }

    private class EventCollector(val events: MutableList<ChatEvent>, val job: Job)

    private fun toolCall(name: String, args: String, id: String = "t_${System.nanoTime()}") =
        ToolCallData(id = id, type = "function", function = FunctionCallData(name = name, arguments = args))

    private fun CoroutineScope.newManager(client: ScriptedLLMClient, streaming: Boolean = false) =
        AIChatConversationManager(
            llmClient = client,
            toolRegistry = toolRegistry(),
            streamingEnabled = streaming,
            scope = this,
            systemPrompt = "测试系统提示词",
            thinkingEnabled = false,
            maxToolRetries = 10
        )

    /** 启动事件收集器并确保其先到达 collect 挂起点，避免漏收首事件。 */
    private suspend fun CoroutineScope.collectEvents(manager: AIChatConversationManager): EventCollector {
        val events = mutableListOf<ChatEvent>()
        val job = launch { manager.events.collect { e -> if (e != null) events += e } }
        yield()
        return EventCollector(events, job)
    }

    @Test
    fun `plain reply emits assistant text`() = runBlocking {
        val client = ScriptedLLMClient()
        client.enqueueNonStreaming(LLMClient.LLMResult.Success("你好呀", null, emptyList()))
        val manager = newManager(client)
        val collector = collectEvents(manager)

        manager.startConversation("hi")
        yield()
        collector.job.cancel()
        assertTrue(collector.events.any { it is ChatEvent.AssistantText && it.text == "你好呀" })
        assertEquals(1, client.calls.size)
    }

    @Test
    fun `llm error emits error event and stops`() = runBlocking {
        val client = ScriptedLLMClient()
        client.enqueueNonStreaming(LLMClient.LLMResult.Error("模拟失败"))
        val manager = newManager(client)
        val collector = collectEvents(manager)

        manager.startConversation("hi")
        yield()
        collector.job.cancel()
        assertTrue(collector.events.any { it is ChatEvent.Error && it.message == "模拟失败" })
        assertEquals(1, client.calls.size)
    }

    @Test
    fun `tool loop continues until final text reply`() = runBlocking {
        val client = ScriptedLLMClient()
        client.enqueueNonStreaming(
            LLMClient.LLMResult.Success(
                "",
                null,
                listOf(toolCall("reply", """{"text":"我调用了工具"}"""))
            )
        )
        client.enqueueNonStreaming(LLMClient.LLMResult.Success("好的，已经处理", null, emptyList()))
        val manager = newManager(client)
        val collector = collectEvents(manager)

        manager.startConversation("帮我回复一下")
        yield()
        collector.job.cancel()
        assertTrue(collector.events.any { it is ChatEvent.ToolExecuted && it.toolName == "reply" })
        assertTrue(collector.events.any { it is ChatEvent.AssistantText && it.text == "好的，已经处理" })
        assertEquals(2, client.calls.size)
        // 工具结果已以 role=tool 回灌给第二轮
        assertTrue(client.calls[1].any { it.role == "tool" })
    }

    @Test
    fun `create_habit pauses for user without further llm calls`() = runBlocking {
        val client = ScriptedLLMClient()
        client.enqueueNonStreaming(
            LLMClient.LLMResult.Success(
                "",
                null,
                listOf(toolCall("create_habit", """{"title":"读书","repeat_cycle":"DAILY"}"""))
            )
        )
        val manager = newManager(client)
        val collector = collectEvents(manager)

        manager.startConversation("帮我创建读书习惯")
        yield()
        collector.job.cancel()
        assertTrue(collector.events.any { it is ChatEvent.PauseForUser && it.toolName == "create_habit" })
        assertEquals(1, client.calls.size)
    }

    @Test
    fun `continueConversation resumes after pause`() = runBlocking {
        val client = ScriptedLLMClient()
        client.enqueueNonStreaming(
            LLMClient.LLMResult.Success(
                "",
                null,
                listOf(toolCall("ask_question", """{"type":"text","prompt":"请补充名称"}"""))
            )
        )
        client.enqueueNonStreaming(LLMClient.LLMResult.Success("好的", null, emptyList()))
        val manager = newManager(client)
        val collector = collectEvents(manager)

        manager.startConversation("创建习惯")
        yield()
        assertTrue(collector.events.any { it is ChatEvent.PauseForUser && it.toolName == "ask_question" })

        manager.continueConversation("跑步")
        yield()
        collector.job.cancel()
        assertTrue(collector.events.any { it is ChatEvent.AssistantText && it.text == "好的" })
        assertEquals(2, client.calls.size)
    }

    @Test
    fun `tool errors retry up to max then emit error`() = runBlocking {
        val client = ScriptedLLMClient()
        repeat(10) {
            client.enqueueNonStreaming(
                LLMClient.LLMResult.Success("", null, listOf(toolCall("no_such_tool", "{}")))
            )
        }
        val manager = newManager(client)
        val collector = collectEvents(manager)

        manager.startConversation("hi")
        yield()
        collector.job.cancel()
        assertTrue(collector.events.any { it is ChatEvent.ToolError })
        assertTrue(collector.events.any { it is ChatEvent.Error })
        assertEquals(10, client.calls.size)
    }

    @Test
    fun `repeated setting errors trigger guard block`() = runBlocking {
        val client = ScriptedLLMClient()
        val tc = toolCall("update_setting", """{"key":"reminder_enabled","value":true}""")
        repeat(3) { client.enqueueNonStreaming(LLMClient.LLMResult.Success("", null, listOf(tc))) }
        val manager = newManager(client)
        val collector = collectEvents(manager)

        manager.startConversation("hi")
        yield()
        collector.job.cancel()
        assertTrue(collector.events.any { it is ChatEvent.GuardBlocked })
        assertEquals(3, client.calls.size)
    }

    @Test
    fun `streaming accumulates content and usage`() = runBlocking {
        val client = ScriptedLLMClient()
        client.enqueueStreaming {
            flow {
                emit(LLMClient.StreamChunk.Content("你"))
                emit(LLMClient.StreamChunk.Content("好"))
                emit(LLMClient.StreamChunk.Usage(LLMClient.TokenUsage(10, 5, 15)))
                emit(LLMClient.StreamChunk.Done("你好"))
            }
        }
        val manager = newManager(client, streaming = true)
        val collector = collectEvents(manager)

        manager.startConversation("hi")
        yield()
        collector.job.cancel()
        assertTrue(collector.events.any { it is ChatEvent.AssistantText && it.text == "你好" })
        assertEquals(10L, manager.usage.value.promptTokens)
        assertEquals(5L, manager.usage.value.completionTokens)
        assertEquals(15L, manager.usage.value.totalTokens)
        assertEquals(1, manager.usage.value.completedCalls)
    }

    @Test
    fun `empty streaming falls back to non streaming once`() = runBlocking {
        val client = ScriptedLLMClient()
        client.enqueueStreaming { flow { emit(LLMClient.StreamChunk.Done("")) } }
        client.enqueueNonStreaming(LLMClient.LLMResult.Success("回退文本", null, emptyList()))
        val manager = newManager(client, streaming = true)
        val collector = collectEvents(manager)

        manager.startConversation("hi")
        yield()
        collector.job.cancel()
        assertTrue(collector.events.any { it is ChatEvent.AssistantText && it.text == "回退文本" })
        assertEquals(2, client.calls.size)
    }

    @Test
    fun `non streaming usage accumulates session total`() = runBlocking {
        val client = ScriptedLLMClient()
        client.enqueueNonStreaming(
            LLMClient.LLMResult.Success(
                "回复",
                LLMClient.TokenUsage(20, 10, 30),
                emptyList()
            )
        )
        val manager = newManager(client)

        manager.startConversation("hi")
        yield()
        assertEquals(30L, manager.usage.value.totalTokens)
        assertEquals(1, manager.usage.value.completedCalls)
    }

    @Test
    fun `retryLastTurn resends after removing assistant turn`() = runBlocking {
        val client = ScriptedLLMClient()
        client.enqueueNonStreaming(
            LLMClient.LLMResult.Success("", null, listOf(toolCall("reply", """{"text":"先调用"}""")))
        )
        client.enqueueNonStreaming(LLMClient.LLMResult.Success("第一轮回复", null, emptyList()))
        val manager = newManager(client)
        val collector = collectEvents(manager)

        manager.startConversation("hi")
        yield()
        assertTrue(collector.events.any { it is ChatEvent.AssistantText && it.text == "第一轮回复" })

        client.enqueueNonStreaming(LLMClient.LLMResult.Success("重试后的回复", null, emptyList()))
        manager.retryLastTurn()
        yield()
        collector.job.cancel()
        assertTrue(collector.events.any { it is ChatEvent.AssistantText && it.text == "重试后的回复" })
        assertEquals(3, client.calls.size)
    }

    @Test
    fun `stop prevents further llm calls`() = runBlocking {
        val client = ScriptedLLMClient()
        client.enqueueNonStreaming(LLMClient.LLMResult.Success("text", null, emptyList()))
        val manager = newManager(client)
        val collector = collectEvents(manager)

        manager.startConversation("hi")
        yield()
        manager.stop()
        yield()
        collector.job.cancel()
        assertTrue(collector.events.any { it is ChatEvent.Stopped })
        val before = client.calls.size
        manager.startConversation("again")
        assertEquals(before, client.calls.size)
    }
}
