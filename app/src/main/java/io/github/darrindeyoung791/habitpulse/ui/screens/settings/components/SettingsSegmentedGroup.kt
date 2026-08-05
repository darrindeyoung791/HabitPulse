package io.github.darrindeyoung791.habitpulse.ui.screens.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier

/**
 * 竖直分组容器（内部自动加 4dp 间距）。
 *
 * @param tintOffset 页面级图标配色偏移：同一页面内若有多个分组，第二个及之后的分组
 * 传入前面所有分组的图标项总数，使整页图标颜色不重复。单分组页面无需传。
 */
@Composable
fun SettingsSegmentedGroup(
    modifier: Modifier = Modifier,
    tintOffset: Int = 0,
    content: @Composable ColumnScope.() -> Unit
) {
    CompositionLocalProvider(LocalAccentTintOffset provides tintOffset) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SettingsGroupItemGap)
        ) {
            content()
        }
    }
}
