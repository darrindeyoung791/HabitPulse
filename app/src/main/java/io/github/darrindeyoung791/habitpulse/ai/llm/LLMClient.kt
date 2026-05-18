package io.github.darrindeyoung791.habitpulse.ai.llm

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class LLMClient(
    private val config: LLMConfig
) {
    private val gson = Gson()

    sealed class LLMResult {
        data class Success(val content: String, val usage: TokenUsage? = null) : LLMResult()
        data class Error(val message: String, val code: Int? = null) : LLMResult()
    }

    data class TokenUsage(
        val promptTokens: Int,
        val completionTokens: Int,
        val totalTokens: Int
    )

    suspend fun chat(messages: List<Message>): LLMResult = withContext(Dispatchers.IO) {
        var lastError: Exception? = null
        var lastCode: Int? = null

        repeat(config.maxRetries) { attempt ->
            try {
                val result = doChat(messages)
                return@withContext result
            } catch (e: Exception) {
                lastError = e
                if (attempt < config.maxRetries - 1) {
                    Thread.sleep(1000L * (attempt + 1))
                }
            }
        }

        LLMResult.Error(
            message = lastError?.message ?: "Unknown error after ${config.maxRetries} retries",
            code = lastCode
        )
    }

    fun chatStream(messages: List<Message>): Flow<String> = flow {
        var lastError: Exception? = null

        repeat(config.maxRetries) { attempt ->
            try {
                val connection = createConnection()
                connection.doOutput = true

                val request = ChatRequest(
                    model = config.modelName,
                    messages = messages,
                    stream = true,
                    temperature = 0.7f,
                    maxTokens = 4096
                )

                val body = gson.toJson(request)
                connection.outputStream.use { it.write(body.toByteArray()) }

                val reader = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
                var buffer = StringBuilder()

                reader.useLines { lines ->
                    for (line in lines) {
                        if (line.startsWith("data: ")) {
                            val data = line.substring(6)
                            if (data == "[DONE]") break

                            val chunk = parseStreamChunk(data)
                            if (chunk != null) {
                                emit(chunk)
                            }
                        }
                    }
                }

                connection.disconnect()
                return@flow
            } catch (e: Exception) {
                lastError = e
                if (attempt < config.maxRetries - 1) {
                    Thread.sleep(1000L * (attempt + 1))
                }
            }
        }

        throw lastError ?: Exception("Stream failed after ${config.maxRetries} retries")
    }

    private fun doChat(messages: List<Message>): LLMResult {
        val connection = createConnection()
        connection.doOutput = true

        val request = ChatRequest(
            model = config.modelName,
            messages = messages,
            stream = false,
            temperature = 0.7f,
            maxTokens = 4096
        )

        val body = gson.toJson(request)
        connection.outputStream.use { it.write(body.toByteArray()) }

        val responseCode = connection.responseCode
        val response = connection.inputStream.use { input ->
            BufferedReader(InputStreamReader(input, "UTF-8")).use { reader ->
                reader.readText()
            }
        }
        connection.disconnect()

        return if (responseCode == 200) {
            val chatResponse = gson.fromJson(response, ChatResponse::class.java)
            val content = chatResponse.choices?.firstOrNull()?.message?.content ?: ""
            LLMResult.Success(content)
        } else {
            val errorResponse = try {
                gson.fromJson(response, ChatResponse::class.java)
            } catch (e: Exception) {
                null
            }
            LLMResult.Error(
                message = errorResponse?.error?.message ?: "HTTP $responseCode",
                code = responseCode
            )
        }
    }

    private fun createConnection(): HttpURLConnection {
        val url = URL(config.apiEndpoint)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Authorization", "Bearer ${config.apiKey}")
        connection.connectTimeout = config.timeoutMs
        connection.readTimeout = config.timeoutMs
        return connection
    }

    private fun parseStreamChunk(data: String): String? {
        return try {
            val map = gson.fromJson(data, Map::class.java)
            val choices = map["choices"] as? List<Map<String, Any?>>
            val delta = choices?.firstOrNull()?.get("delta") as? Map<String, Any?>
            delta?.get("content") as? String
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        fun fromPreferences(
            apiEndpoint: String,
            apiKey: String,
            modelName: String
        ): LLMClient {
            return LLMClient(
                config = LLMConfig(
                    apiEndpoint = apiEndpoint,
                    apiKey = apiKey,
                    modelName = modelName
                )
            )
        }
    }
}