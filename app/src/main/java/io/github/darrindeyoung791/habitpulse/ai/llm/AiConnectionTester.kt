package io.github.darrindeyoung791.habitpulse.ai.llm

import android.content.Context
import io.github.darrindeyoung791.habitpulse.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

/**
 * 测试连接结果。
 */
sealed class ConnectionTestResult(val isSuccess: Boolean, val error: String? = null) {
    object Success : ConnectionTestResult(true)
    data class Error(val message: String) : ConnectionTestResult(false, message)
}

/**
 * AI 配置「测试连接」共享实现，供 AI 设置列表页与编辑页复用。
 */
object AiConnectionTester {

    /**
     * 向 [endpoint] 发送一条最小请求，校验端点、密钥与模型是否可用。
     */
    suspend fun test(context: Context, endpoint: String, apiKey: String, model: String): ConnectionTestResult {
        val res = context.resources
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(LLMConfig.ensureChatCompletionsUrl(endpoint))
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $apiKey")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true
                connection.useCaches = false
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val body = """
                    {
                        "model": "$model",
                        "messages": [{"role": "user", "content": "hi"}],
                        "max_tokens": 10
                    }
                """.trimIndent()

                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

                val responseCode = connection.responseCode

                when (responseCode) {
                    200 -> ConnectionTestResult.Success
                    401 -> ConnectionTestResult.Error(res.getString(R.string.ai_error_auth_failed))
                    403 -> ConnectionTestResult.Error(res.getString(R.string.ai_error_access_denied))
                    429 -> ConnectionTestResult.Success
                    in 400..499 -> {
                        val errorBody = try {
                            connection.errorStream?.use { errorInput ->
                                BufferedReader(InputStreamReader(errorInput, Charsets.UTF_8)).use { it.readText() }
                            } ?: ""
                        } catch (_: Exception) { "" }
                        ConnectionTestResult.Error(extractErrorMessage(errorBody) ?: "HTTP $responseCode")
                    }
                    in 500..599 -> ConnectionTestResult.Error(res.getString(R.string.ai_error_server_error, responseCode))
                    else -> ConnectionTestResult.Error("HTTP $responseCode")
                }
            } catch (e: SocketTimeoutException) {
                ConnectionTestResult.Error(res.getString(R.string.ai_error_timeout))
            } catch (e: ConnectException) {
                ConnectionTestResult.Error(res.getString(R.string.ai_error_connection_failed))
            } catch (e: UnknownHostException) {
                ConnectionTestResult.Error(res.getString(R.string.ai_error_dns_failed))
            } catch (e: MalformedURLException) {
                ConnectionTestResult.Error(res.getString(R.string.ai_error_invalid_url))
            } catch (e: Exception) {
                ConnectionTestResult.Error(e.message ?: res.getString(R.string.ai_error_unknown))
            }
        }
    }

    private fun extractErrorMessage(response: String): String? {
        return try {
            val regex = """"message"\s*:\s*"([^"]+)"""".toRegex()
            regex.find(response)?.groupValues?.getOrNull(1)
        } catch (e: Exception) {
            null
        }
    }
}
