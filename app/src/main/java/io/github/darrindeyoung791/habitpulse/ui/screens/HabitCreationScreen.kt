package io.github.darrindeyoung791.habitpulse.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.ui.theme.HabitPulseTheme
import io.github.darrindeyoung791.habitpulse.ui.utils.rememberDebounceClickHandler
import io.github.darrindeyoung791.habitpulse.ui.utils.rememberNavigationGuard
import kotlinx.coroutines.launch

/**
 * 习惯编辑模式
 */
enum class EditMode {
    CREATE,  // 新建习惯模式
    EDIT     // 编辑习惯模式
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitCreationScreen(
    onNavigateBack: () -> Unit,
    editMode: EditMode = EditMode.CREATE,
    navController: androidx.navigation.NavHostController? = null
) {
    var habitName by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    // 防重复点击处理器，防止快速连续点击导致多次导航
    val clickHandler = rememberDebounceClickHandler()
    // 导航保护器，防止返回到主页以上
    val navigationGuard = navController?.let { rememberNavigationGuard(it) }

    val titleRes = when (editMode) {
        EditMode.CREATE -> R.string.create_habit_title
        EditMode.EDIT -> R.string.edit_habit_title
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = titleRes),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    val scope = rememberCoroutineScope()
                    TextButton(
                        onClick = {
                            if (clickHandler.isEnabled) {
                                scope.launch {
                                    clickHandler.processClick {
                                        // 优先使用导航保护器进行安全返回
                                        if (navigationGuard != null) {
                                            navigationGuard.safePopBackStack()
                                        } else {
                                            onNavigateBack()
                                        }
                                    }
                                }
                            }
                        },
                        enabled = clickHandler.isEnabled
                    ) {
                        Text(
                            text = stringResource(id = R.string.create_habit_cancel_button),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                actions = {
                    val scope = rememberCoroutineScope()
                    TextButton(
                        onClick = {
                            if (clickHandler.isEnabled && habitName.isNotBlank() && !isSaving) {
                                scope.launch {
                                    clickHandler.processClick {
                                        // TODO: Implement save logic
                                        isSaving = true
                                        // 优先使用导航保护器进行安全返回
                                        if (navigationGuard != null) {
                                            navigationGuard.safePopBackStack()
                                        } else {
                                            onNavigateBack()
                                        }
                                    }
                                }
                            }
                        },
                        enabled = habitName.isNotBlank() && !isSaving && clickHandler.isEnabled
                    ) {
                        Text(
                            text = stringResource(id = R.string.create_habit_save_button),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Habit name input field
            OutlinedTextField(
                value = habitName,
                onValueChange = { habitName = it },
                label = {
                    Text(text = stringResource(id = R.string.create_habit_habit_name_label))
                },
                placeholder = {
                    Text(text = stringResource(id = R.string.create_habit_habit_name_placeholder))
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        // Close keyboard when done is pressed
                    }
                ),
            )

            // TODO: Add more habit creation fields here
            // - Repeat cycle (daily/weekly)
            // - Reminder times
            // - Notes
            // - Supervision settings

            Spacer(modifier = Modifier.weight(1f))

            // Helper text
            Text(
                text = "更多设置将在此处添加...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HabitCreationScreenPreview() {
    HabitPulseTheme {
        HabitCreationScreen(onNavigateBack = {})
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HabitCreationScreenDarkPreview() {
    HabitPulseTheme(darkTheme = true) {
        HabitCreationScreen(onNavigateBack = {})
    }
}