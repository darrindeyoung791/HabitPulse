package io.github.darrindeyoung791.habitpulse.ui.screens.settings

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.data.preferences.UserPreferences
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSectionHeader
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedGroup
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedItem
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedNumberItem
import kotlinx.coroutines.launch

/**
 * AI 配置列表页：
 * - 列出所有配置，点行 = 设为当前使用，行尾铅笔 = 进入编辑页
 * - 长按行 = 弹出删除菜单
 * - 「添加 AI 配置」进入新建页（提示使用 OpenAI 兼容格式）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSettingsAIScreen(
    onBack: () -> Unit,
    onOpenHelp: () -> Unit,
    onAddConfig: () -> Unit,
    onEditConfig: (configId: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userPreferences = remember { UserPreferences.getInstance(context) }

    val configs by userPreferences.aiConfigsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val activeConfig by userPreferences.activeConfigFlow.collectAsStateWithLifecycle(initialValue = null)

    var menuConfigId by remember { mutableStateOf<String?>(null) }
    var pendingDeleteConfigId by remember { mutableStateOf<String?>(null) }

    NewSettingsScaffold(
        title = stringResource(id = R.string.settings_category_ai),
        onBack = onBack,
        onHelp = onOpenHelp,
        contentPadding = PaddingValues(16.dp)
    ) {
        SettingsSectionHeader(text = stringResource(id = R.string.ai_config_providers))

        Text(
            text = stringResource(id = R.string.ai_config_page_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
        )

        SettingsSegmentedGroup {
            if (configs.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.ai_config_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            configs.forEachIndexed { index, config ->
                val isActive = config.id == activeConfig?.id
                Box {
                    SettingsSegmentedNumberItem(
                        index = index,
                        count = configs.size + 1,
                        number = index + 1,
                        headline = config.name,
                        supportingText = "${config.modelName} · ${endpointHost(config.apiEndpoint)}",
                        selected = isActive,
                        onClick = {
                            scope.launch { userPreferences.setActiveAIConfig(config.id) }
                        },
                        onLongClick = { menuConfigId = config.id },
                        trailing = {
                            IconButton(onClick = { onEditConfig(config.id) }) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = stringResource(id = R.string.ai_config_edit)
                                )
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = menuConfigId == config.id,
                        onDismissRequest = { menuConfigId = null }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.ai_config_delete)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                menuConfigId = null
                                pendingDeleteConfigId = config.id
                            }
                        )
                    }
                }
            }
            SettingsSegmentedItem(
                index = configs.size,
                count = configs.size + 1,
                headline = stringResource(id = R.string.ai_config_add),
                supportingText = stringResource(id = R.string.ai_config_add_openai_hint),
                leadingIcon = Icons.Outlined.Add,
                tintIndex = 5,
                showArrow = true,
                onClick = onAddConfig
            )
        }
    }

    pendingDeleteConfigId?.let { configId ->
        AlertDialog(
            onDismissRequest = { pendingDeleteConfigId = null },
            title = { Text(stringResource(id = R.string.ai_config_delete_title)) },
            text = { Text(stringResource(id = R.string.ai_config_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingDeleteConfigId = null
                    scope.launch {
                        userPreferences.deleteAIConfig(configId)
                        if (configId == activeConfig?.id) {
                            val remaining = configs.firstOrNull { it.id != configId }
                            if (remaining != null) {
                                userPreferences.setActiveAIConfig(remaining.id)
                            }
                        }
                    }
                }) { Text(stringResource(id = R.string.dialog_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteConfigId = null }) {
                    Text(stringResource(id = R.string.dialog_cancel))
                }
            }
        )
    }
}

private fun endpointHost(endpoint: String): String {
    return try {
        Uri.parse(endpoint.trim()).host ?: endpoint
    } catch (e: Exception) {
        endpoint
    }
}
