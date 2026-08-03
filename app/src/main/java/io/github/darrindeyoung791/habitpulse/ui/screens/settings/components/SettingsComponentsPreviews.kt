package io.github.darrindeyoung791.habitpulse.ui.screens.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.darrindeyoung791.habitpulse.ui.theme.HabitPulseTheme

@Preview(showBackground = true, widthDp = 400)
@Composable
fun SettingsSingleItemGroupPreview() {
    HabitPulseTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSegmentedGroup {
                SettingsSegmentedItem(
                    index = 0,
                    count = 1,
                    headline = "AI 配置",
                    supportingText = "AI 助手配置",
                    leadingIcon = Icons.Outlined.AutoAwesome,
                    onClick = {}
                )
            }
            Spacer(modifier = Modifier.height(SettingsBetweenGroupGap))
            SettingsSegmentedGroup {
                SettingsSegmentedItem(
                    index = 0,
                    count = 1,
                    headline = "连接与同步",
                    enabled = false,
                    leadingIcon = Icons.Outlined.Settings,
                    onClick = {}
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
fun SettingsGroupedItemsPreview() {
    HabitPulseTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSegmentedGroup {
                SettingsSegmentedItem(
                    index = 0,
                    count = 4,
                    headline = "通知",
                    leadingIcon = Icons.Outlined.Notifications,
                    onClick = {}
                )
                SettingsSegmentedSwitch(
                    index = 1,
                    count = 4,
                    headline = "保持后台运行",
                    supportingText = "常驻通知保持应用在后台运行",
                    checked = true,
                    onCheckedChange = {}
                )
                SettingsSegmentedItem(
                    index = 2,
                    count = 4,
                    headline = "习惯提醒",
                    leadingIcon = Icons.Outlined.Alarm,
                    onClick = {}
                )
                SettingsSegmentedItem(
                    index = 3,
                    count = 4,
                    headline = "通知模板",
                    supportingText = "自定义提醒文本",
                    onClick = {}
                )
            }
            Spacer(modifier = Modifier.height(SettingsBetweenGroupGap))
            SettingsSegmentedGroup {
                SettingsSegmentedSwitch(
                    index = 0,
                    count = 1,
                    headline = "支持 HabitPulse",
                    supportingText = "启用开屏广告",
                    checked = false,
                    onCheckedChange = {}
                )
            }
            Spacer(modifier = Modifier.height(SettingsBetweenGroupGap))
            SettingsSegmentedGroup {
                SettingsSegmentedItem(
                    index = 0,
                    count = 2,
                    headline = "空间清理",
                    leadingIcon = Icons.Outlined.Storage,
                    onClick = {}
                )
                SettingsSegmentedItem(
                    index = 1,
                    count = 2,
                    headline = "界面与显示",
                    leadingIcon = Icons.Outlined.Settings,
                    onClick = {}
                )
            }
        }
    }
}