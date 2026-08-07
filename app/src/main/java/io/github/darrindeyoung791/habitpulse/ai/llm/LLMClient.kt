package io.github.darrindeyoung791.habitpulse.ai.llm

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

open class LLMClient(
    private val config: LLMConfig
) {
    private val gson = Gson()

    sealed class StreamChunk {
        data class Content(val delta: String) : StreamChunk()
        data class ToolCallFragment(
            val progress: Map<String, FunctionCallData>
        ) : StreamChunk()
        data class ToolCallComplete(
            val toolCalls: List<ToolCallData>
        ) : StreamChunk()
        data class Reasoning(val delta: String) : StreamChunk()
        data class Usage(val usage: TokenUsage) : StreamChunk()
        data class Done(val fullContent: String) : StreamChunk()
        data class Error(val message: String, val code: Int? = null) : StreamChunk()
    }

    sealed class LLMResult {
        data class Success(
            val content: String,
            val usage: TokenUsage? = null,
            val toolCalls: List<ToolCallData> = emptyList(),
            val thoughts: String = ""
        ) : LLMResult()
        data class Error(val message: String, val code: Int? = null) : LLMResult()
    }

    data class TokenUsage(
        val promptTokens: Int,
        val completionTokens: Int,
        val totalTokens: Int
    )

    open suspend fun chat(
        messages: List<Message>,
        tools: List<ToolDef>? = null,
        toolChoice: String? = null
    ): LLMResult = withContext(Dispatchers.IO) {
        var lastError: Exception? = null
        var lastCode: Int? = null

        repeat(config.maxRetries) { attempt ->
            ensureActive()
            try {
                val result = doChat(messages, tools, toolChoice)
                return@withContext result
            } catch (e: Exception) {
                lastError = e
                lastCode = getErrorCode(e)
                if (attempt < config.maxRetries - 1) {
                    kotlinx.coroutines.delay(1000L * (attempt + 1))
                }
            }
        }

        LLMResult.Error(
            message = formatErrorMessage(lastError, lastCode),
            code = lastCode
        )
    }

    open fun chatStream(
        messages: List<Message>,
        tools: List<ToolDef>? = null,
        toolChoice: String? = null
    ): Flow<StreamChunk> = flow {
        var lastError: Exception? = null
        var lastCode: Int? = null

        repeat(config.maxRetries) { attempt ->
            var connection: HttpURLConnection? = null
            try {
                connection = createConnection(isStreaming = true)
                connection.doOutput = true

                val request = ChatRequest(
                    model = config.modelName,
                    messages = messages,
                    stream = true,
                    temperature = 0.7f,
                    maxTokens = config.maxOutputTokens,
                    tools = tools,
                    toolChoice = toolChoice,
                    thinking = config.thinkingParam()
                )

                val body = gson.toJson(request)
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

                val responseCode = connection.responseCode

                if (responseCode != 200) {
                    val errorBody = readErrorStream(connection)
                    val errorMsg = parseErrorResponse(errorBody, responseCode)
                    emit(StreamChunk.Error(errorMsg, responseCode))
                    connection.disconnect()
                    return@flow
                }

                val reader = BufferedReader(
                    InputStreamReader(connection.inputStream, Charsets.UTF_8)
                )

                val fullContent = StringBuilder()
                val reasoningContent = StringBuilder()
                val toolCallAccumulator = ToolCallAccumulator()
                var lastUsage: TokenUsage? = null
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    val currentLine = line ?: continue
                    if (!currentLine.startsWith("data: ")) continue

                    val data = currentLine.substring(6).trim()
                    if (data == "[DONE]") break
                    if (data.isEmpty()) continue

                    val chunk = parseStreamChunk(data)
                    if (chunk != null) {
                        val choice = chunk.choices?.firstOrNull()
                        val delta = choice?.delta
                        if (delta?.content != null) {
                            emit(StreamChunk.Content(delta.content))
                            fullContent.append(delta.content)
                        }
                        if (delta?.reasoningContent != null) {
                            reasoningContent.append(delta.reasoningContent)
                            emit(StreamChunk.Reasoning(delta.reasoningContent))
                        }
                        delta?.toolCalls?.let { toolCalls ->
                            toolCallAccumulator.accumulate(toolCalls)
                            val progress = toolCallAccumulator.currentProgress()
                            if (progress.isNotEmpty()) {
                                emit(StreamChunk.ToolCallFragment(progress))
                            }
                            if (toolCallAccumulator.isComplete()) {
                                emit(StreamChunk.ToolCallComplete(toolCallAccumulator.completed()))
                            }
                        }
                        chunk.usage?.let { usage ->
                            lastUsage = TokenUsage(
                                promptTokens = usage.promptTokens ?: 0,
                                completionTokens = usage.completionTokens ?: 0,
                                totalTokens = usage.totalTokens ?: 0
                            )
                        }
                    }
                }

                lastUsage?.let { emit(StreamChunk.Usage(it)) }
                emit(StreamChunk.Done(fullContent.toString()))
                connection.disconnect()
                return@flow
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                lastError = e
                lastCode = getErrorCode(e)
                if (attempt < config.maxRetries - 1) {
                    Thread.sleep(1000L * (attempt + 1))
                }
            } finally {
                connection?.disconnect()
            }
        }

        emit(
            StreamChunk.Error(
                message = formatErrorMessage(lastError, lastCode),
                code = lastCode
            )
        )
    }.flowOn(Dispatchers.IO)

    private fun doChat(messages: List<Message>, tools: List<ToolDef>? = null, toolChoice: String? = null): LLMResult {
        val connection = createConnection(isStreaming = false)
        connection.doOutput = true

        val request = ChatRequest(
            model = config.modelName,
            messages = messages,
            stream = false,
            temperature = 0.7f,
            maxTokens = config.maxOutputTokens,
            tools = tools,
            toolChoice = toolChoice,
            thinking = config.thinkingParam()
        )

        val body = gson.toJson(request)
        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

        val responseCode = connection.responseCode

        if (responseCode != 200) {
            val errorBody = readErrorStream(connection)
            val errorMsg = parseErrorResponse(errorBody, responseCode)
            connection.disconnect()
            return LLMResult.Error(message = errorMsg, code = responseCode)
        }

        val response = connection.inputStream.use { input ->
            BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { it.readText() }
        }
        connection.disconnect()

        val chatResponse = gson.fromJson(response, ChatResponse::class.java)
        val message = chatResponse?.choices?.firstOrNull()?.message
        val content = message?.content ?: ""
        val finishReason = chatResponse?.choices?.firstOrNull()?.finishReason
        val usage = chatResponse?.usage

        val tokenUsage = usage?.let {
            TokenUsage(
                promptTokens = it.promptTokens ?: 0,
                completionTokens = it.completionTokens ?: 0,
                totalTokens = it.totalTokens ?: 0
            )
        }

        val toolCalls = message?.toolCalls.orEmpty()
        val thoughts = message?.reasoningContent ?: ""

        return if ((finishReason == "tool_calls" || toolCalls.isNotEmpty()) && content.isBlank()) {
            LLMResult.Success(response, tokenUsage, toolCalls, thoughts)
        } else {
            LLMResult.Success(content, tokenUsage, toolCalls, thoughts)
        }
    }

    private fun createConnection(isStreaming: Boolean): HttpURLConnection {
        val endpoint = config.getChatCompletionsUrl()
        val url = URL(endpoint)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Authorization", "Bearer ${config.apiKey}")

        if (isStreaming) {
            connection.setRequestProperty("Accept", "text/event-stream")
            connection.setRequestProperty("Cache-Control", "no-cache")
        } else {
            connection.setRequestProperty("Accept", "application/json")
        }

        connection.useCaches = false
        connection.connectTimeout = config.timeoutMs
        connection.readTimeout = config.timeoutMs
        connection.doInput = true
        return connection
    }

    private fun readErrorStream(connection: HttpURLConnection): String {
        return try {
            connection.errorStream?.use { errorInput ->
                BufferedReader(InputStreamReader(errorInput, Charsets.UTF_8)).use {
                    it.readText()
                }
            } ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun parseStreamChunk(data: String): ChatCompletionChunk? {
        return try {
            gson.fromJson(data, ChatCompletionChunk::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseErrorResponse(response: String, responseCode: Int): String {
        if (response.isNotBlank()) {
            try {
                val errorResponse = gson.fromJson(response, ChatResponse::class.java)
                val msg = errorResponse?.error?.message
                if (!msg.isNullOrBlank()) return msg
            } catch (_: Exception) {
                val simple = extractSimpleErrorMessage(response)
                if (simple != null) return simple
            }
        }
        return getDefaultErrorMessage(responseCode)
    }

    private fun extractSimpleErrorMessage(response: String): String? {
        val trimmed = response.trim()
        if (trimmed.length > 200 || trimmed.contains("://") || trimmed.contains("<")) return null
        val patterns = listOf(
            Regex(""""message"\s*:\s*"([^"]+)""""),
            Regex(""""error"\s*:\s*"([^"]+)""""),
            Regex(""""msg"\s*:\s*"([^"]+)"""")
        )
        for (pattern in patterns) {
            val match = pattern.find(response)
            if (match != null) return match.groupValues[1]
        }
        return null
    }

    private fun getDefaultErrorMessage(code: Int): String = when (code) {
        401 -> "认证失败，请检查 API Key"
        403 -> "访问被拒绝，请检查 API Key 权限"
        429 -> "请求过于频繁，请稍后重试"
        in 400..499 -> "请求错误 (HTTP $code)"
        in 500..599 -> "服务器错误 (HTTP $code)，请稍后重试"
        else -> "HTTP $code"
    }

    private fun getErrorCode(e: Exception): Int? {
        return when (e) {
            is SocketTimeoutException -> 0
            is java.net.ConnectException -> -1
            is java.net.UnknownHostException -> -2
            else -> null
        }
    }

    /**
     * 累积流式 `delta.tool_calls` 分片，按 index 聚合 id/name/arguments。
     * `isComplete()` 在累积了至少一个分片且 arguments 以 `}` 结尾时判定为完整。
     */
    private class ToolCallAccumulator {
        private val byIndex = LinkedHashMap<Int, DeltaToolCall>()

        fun accumulate(fragments: List<DeltaToolCall>) {
            for (fragment in fragments) {
                val index = fragment.index ?: 0
                val existing = byIndex[index] ?: DeltaToolCall(index = index)
                val mergedFunction = DeltaFunctionCallData(
                    name = fragment.function?.name ?: existing.function?.name,
                    arguments = (existing.function?.arguments ?: "") + (fragment.function?.arguments ?: "")
                )
                byIndex[index] = DeltaToolCall(
                    index = index,
                    id = fragment.id ?: existing.id,
                    function = mergedFunction
                )
            }
        }

        fun currentProgress(): Map<String, FunctionCallData> {
            return byIndex.values
                .filter { it.id != null && it.function?.name != null }
                .associate { it.id!! to FunctionCallData(it.function!!.name, it.function!!.arguments) }
        }

        fun isComplete(): Boolean {
            return byIndex.values.any { it.function?.arguments?.endsWith("}") == true }
        }

        fun completed(): List<ToolCallData> {
            return byIndex.values
                .filter { it.id != null && it.function?.name != null && it.function!!.arguments?.endsWith("}") == true }
                .map {
                    ToolCallData(
                        id = it.id,
                        type = "function",
                        function = FunctionCallData(
                            name = it.function!!.name,
                            arguments = it.function!!.arguments
                        )
                    )
                }
        }
    }

    private fun formatErrorMessage(e: Exception?, code: Int?): String {
        if (e is SocketTimeoutException) {
            return "连接超时，请检查网络或 API 地址"
        }
        if (e is java.net.ConnectException) {
            return "无法连接到服务器，请检查 API 地址"
        }
        if (e is java.net.UnknownHostException) {
            return "无法解析域名，请检查 API 地址"
        }
        if (e is java.net.MalformedURLException) {
            return "API 地址格式不正确"
        }
        return when (code) {
            0 -> "连接超时，请检查网络或 API 地址"
            -1 -> "无法连接到服务器，请检查 API 地址"
            -2 -> "无法解析域名，请检查 API 地址"
            else -> e?.message ?: "连接失败，请检查配置后重试"
        }
    }
}
