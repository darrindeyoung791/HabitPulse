package io.github.darrindeyoung791.habitpulse.ai.llm

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 协议层测试：ToolDef 序列化、请求携带 tools/tool_choice、非流式响应解析 tool_calls/usage/reasoning、
 * 流式 chunk 解析（content / tool_calls 分片 / reasoning_content / usage）。
 */
class LLMRequestTest {

    private val gson = Gson()

    @Test
    fun `ToolDef serializes with function type and JSON schema`() {
        val tool = ToolDef(
            function = FunctionDef(
                name = "create_habit",
                description = "创建新习惯",
                parameters = mapOf(
                    "type" to "object",
                    "properties" to mapOf("title" to mapOf("type" to "string"))
                )
            )
        )
        val json = gson.toJson(tool)
        assertTrue(json.contains("\"type\":\"function\""))
        assertTrue(json.contains("\"name\":\"create_habit\""))
        assertTrue(json.contains("\"description\":\"创建新习惯\""))
        assertTrue(json.contains("\"parameters\""))
    }

    @Test
    fun `ChatRequest serializes tools and tool_choice`() {
        val request = ChatRequest(
            model = "glm-4-flash-250414",
            messages = listOf(Message(role = "user", content = "hi")),
            tools = listOf(
                ToolDef(function = FunctionDef("reply", "纯文本回复", null))
            ),
            toolChoice = "auto"
        )
        val json = gson.toJson(request)
        assertTrue(json.contains("\"tools\""))
        assertTrue(json.contains("\"tool_choice\":\"auto\""))
        assertTrue(json.contains("\"name\":\"reply\""))
    }

    @Test
    fun `Message serializes role tool with tool_call_id and tool_calls`() {
        val message = Message(
            role = "tool",
            content = "{\"ok\":true}",
            toolCallId = "call_123"
        )
        val json = gson.toJson(message)
        assertTrue(json.contains("\"role\":\"tool\""))
        assertTrue(json.contains("\"tool_call_id\":\"call_123\""))

        val assistant = Message(
            role = "assistant",
            content = "",
            toolCalls = listOf(
                ToolCallData(id = "call_123", type = "function", function = FunctionCallData("reply", "{\"text\":\"hi\"}"))
            )
        )
        val json2 = gson.toJson(assistant)
        assertTrue(json2.contains("\"tool_calls\""))
        assertTrue(json2.contains("\"call_123\""))
    }

    @Test
    fun `ChatResponse parses tool_calls reasoning and usage from non-stream`() {
        val json = """
            {
              "id": "x",
              "model": "glm-4-flash-250414",
              "choices": [{
                "index": 0,
                "finish_reason": "tool_calls",
                "message": {
                  "role": "assistant",
                  "content": "",
                  "reasoning_content": "先查设置状态",
                  "tool_calls": [{
                    "id": "call_9",
                    "type": "function",
                    "function": { "name": "get_settings_status", "arguments": "{}" }
                  }]
                }
              }],
              "usage": { "prompt_tokens": 100, "completion_tokens": 50, "total_tokens": 150 }
            }
        """.trimIndent()
        val response = gson.fromJson(json, ChatResponse::class.java)
        val message = response.choices?.firstOrNull()?.message
        assertEquals("先查设置状态", message?.reasoningContent)
        assertEquals("call_9", message?.toolCalls?.firstOrNull()?.id)
        assertEquals("get_settings_status", message?.toolCalls?.firstOrNull()?.function?.name)
        assertEquals("{}", message?.toolCalls?.firstOrNull()?.function?.arguments)
        assertEquals(100, response.usage?.promptTokens)
        assertEquals(150, response.usage?.totalTokens)
    }

    @Test
    fun `stream chunk parses content reasoning and tool_calls fragment`() {
        val json = """
            {
              "id": "x",
              "model": "glm-4-flash-250414",
              "choices": [{
                "index": 0,
                "delta": {
                  "reasoning_content": "思考中",
                  "tool_calls": [{
                    "index": 0,
                    "id": "call_5",
                    "function": { "name": "reply", "arguments": "{\"text\":\"完成" }
                  }]
                }
              }]
            }
        """.trimIndent()
        val chunk = gson.fromJson(json, ChatCompletionChunk::class.java)
        val delta = chunk.choices?.firstOrNull()?.delta
        assertEquals("思考中", delta?.reasoningContent)
        assertEquals(0, delta?.toolCalls?.firstOrNull()?.index)
        assertEquals("call_5", delta?.toolCalls?.firstOrNull()?.id)
        assertEquals("reply", delta?.toolCalls?.firstOrNull()?.function?.name)
        assertEquals("{\"text\":\"完成", delta?.toolCalls?.firstOrNull()?.function?.arguments)
        assertNull(delta?.content)
    }

    @Test
    fun `stream chunk with only usage before DONE`() {
        val json = """
            {
              "id": "x",
              "model": "glm-4-flash-250414",
              "choices": [],
              "usage": { "prompt_tokens": 10, "completion_tokens": 5, "total_tokens": 15 }
            }
        """.trimIndent()
        val chunk = gson.fromJson(json, ChatCompletionChunk::class.java)
        assertEquals(10, chunk.usage?.promptTokens)
        assertEquals(15, chunk.usage?.totalTokens)
        assertTrue(chunk.choices.isNullOrEmpty())
    }
}
