package io.github.darrindeyoung791.habitpulse.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay

/**
 * 防重复点击处理器
 * 确保在指定的时间间隔内只响应一次点击
 */
@Stable
class DebounceClickHandler(
    private val debounceTime: Long = 300L
) {
    private var isProcessing = mutableStateOf(false)

    /**
     * 处理点击事件，在防抖时间内只执行一次
     * @param action 要执行的动作
     */
    suspend fun processClick(action: suspend () -> Unit) {
        if (isProcessing.value) return

        isProcessing.value = true

        try {
            action()
        } finally {
            delay(debounceTime)
            isProcessing.value = false
        }
    }
}

/**
 * 创建一个防重复点击处理器
 * @param debounceTime 防抖时间（毫秒），默认 300ms
 */
@Composable
fun rememberDebounceClickHandler(debounceTime: Long = 300L): DebounceClickHandler {
    return remember { DebounceClickHandler(debounceTime) }
}
