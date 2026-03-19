package io.github.darrindeyoung791.habitpulse.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.darrindeyoung791.habitpulse.HabitPulseApplication
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.data.model.Habit
import io.github.darrindeyoung791.habitpulse.data.model.RepeatCycle
import io.github.darrindeyoung791.habitpulse.data.model.SupervisionMethod
import io.github.darrindeyoung791.habitpulse.ui.theme.HabitPulseTheme
import io.github.darrindeyoung791.habitpulse.ui.utils.rememberDebounceClickHandler
import io.github.darrindeyoung791.habitpulse.ui.utils.rememberNavigationGuard
import io.github.darrindeyoung791.habitpulse.viewmodel.HabitViewModel
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.time.LocalTime
import java.util.UUID

/**
 * TimePicker 对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    currentTime: LocalTime,
    onDismissRequest: () -> Unit,
    onConfirmRequest: (LocalTime) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = currentTime.hour,
        initialMinute = currentTime.minute,
        is24Hour = false
    )
    var displayMode by remember { mutableStateOf(TimePickerDisplayMode.Picker) }

    TimePickerDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = "")
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    onConfirmRequest(selectedTime)
                }
            ) {
                Text(text = stringResource(id = R.string.create_habit_time_picker_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest
            ) {
                Text(text = stringResource(id = R.string.create_habit_time_picker_dismiss))
            }
        },
        modeToggleButton = {
            TimePickerDialogDefaults.DisplayModeToggle(
                onDisplayModeChange = {
                    displayMode = if (displayMode == TimePickerDisplayMode.Picker) {
                        TimePickerDisplayMode.Input
                    } else {
                        TimePickerDisplayMode.Picker
                    }
                },
                displayMode = displayMode
            )
        }
    ) {
        if (displayMode == TimePickerDisplayMode.Picker) {
            TimePicker(state = timePickerState)
        } else {
            TimeInput(state = timePickerState)
        }
    }
}

/**
 * 习惯编辑模式
 */
enum class EditMode {
    CREATE,  // 新建习惯模式
    EDIT     // 编辑习惯模式
}

/**
 * 习惯重复周期
 */
enum class RepeatCycle {
    DAILY,   // 每日
    WEEKLY   // 每周
}

enum class SupervisionMethod {
    NONE,      // 不监督，仅本地联系
    EMAIL,     // 邮件汇报
    SMS        // 短信汇报
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitCreationScreen(
    onNavigateBack: () -> Unit,
    editMode: EditMode = EditMode.CREATE,
    habitId: UUID? = null,
    navController: androidx.navigation.NavHostController? = null,
    application: HabitPulseApplication? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = application ?: context.applicationContext as HabitPulseApplication
    
    // 获取 ViewModel
    val viewModel: HabitViewModel = remember {
        HabitViewModel.Factory(app).create(HabitViewModel::class.java)
    }
    
    // 收集 ViewModel 状态
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val saveSuccess by viewModel.saveSuccess.collectAsStateWithLifecycle()
    
    // UI 状态变量
    var habitName by remember { mutableStateOf("") }
    var repeatCycle by remember { mutableStateOf(RepeatCycle.DAILY) }
    var reminderTimes by remember { mutableStateOf<List<String>>(emptyList()) }
    var showTimePicker by remember { mutableStateOf(false) }
    var currentTimePickerTime by remember { mutableStateOf(java.time.LocalTime.now()) }
    var isReminderExpanded by remember { mutableStateOf(false) }
    var showMaxLengthToast by remember { mutableStateOf(false) }

    // Supervision method state
    var supervisionMethod by remember { mutableStateOf(SupervisionMethod.NONE) }
    var supervisorEmails by remember { mutableStateOf<List<String>>(emptyList()) }
    var supervisorPhones by remember { mutableStateOf<List<String>>(emptyList()) }
    var emailInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var showEmailError by remember { mutableStateOf(false) }
    var showPhoneError by remember { mutableStateOf(false) }
    var showEmailMaxToast by remember { mutableStateOf(false) }
    var showPhoneMaxToast by remember { mutableStateOf(false) }
    var showDuplicateEmailToast by remember { mutableStateOf(false) }
    var showDuplicatePhoneToast by remember { mutableStateOf(false) }
    var isEmailListExpanded by remember { mutableStateOf(false) }
    var isPhoneListExpanded by remember { mutableStateOf(false) }

    // Repeat days state (for weekly cycle)
    var selectedRepeatDays by remember { mutableStateOf<Set<Int>>(setOf()) }

    // Notes state
    var notes by remember { mutableStateOf("") }
    var showNotesMaxToast by remember { mutableStateOf(false) }
    
    // 加载现有习惯数据（编辑模式）
    LaunchedEffect(habitId, editMode) {
        if (editMode == EditMode.EDIT && habitId != null) {
            val habit = viewModel.getHabitById(habitId)
            habit?.let {
                habitName = it.title
                repeatCycle = it.repeatCycle
                selectedRepeatDays = it.getRepeatDaysList().toSet()
                reminderTimes = it.getReminderTimesList()
                notes = it.notes
                supervisionMethod = it.supervisionMethod
                supervisorEmails = it.getSupervisorEmailsList()
                supervisorPhones = it.getSupervisorPhonesList()
            }
        }
    }
    
    // 处理保存成功后的导航
    LaunchedEffect(saveSuccess) {
        if (saveSuccess == true) {
            onNavigateBack()
            viewModel.resetSaveSuccess()
        }
    }

    // 防重复点击处理器，防止快速连续点击导致多次导航
    val clickHandler = rememberDebounceClickHandler()
    // 导航保护器，防止返回到主页以上
    val navigationGuard = navController?.let { rememberNavigationGuard(it) }

    // 显示最大长度 Toast
    if (showMaxLengthToast) {
        LaunchedEffect(Unit) {
            android.widget.Toast.makeText(context, R.string.create_habit_max_length_hint, android.widget.Toast.LENGTH_SHORT).show()
            showMaxLengthToast = false
        }
    }
    
    // 显示邮箱最大长度 Toast
    if (showEmailMaxToast) {
        LaunchedEffect(Unit) {
            android.widget.Toast.makeText(context, R.string.create_habit_max_length_hint, android.widget.Toast.LENGTH_SHORT).show()
            showEmailMaxToast = false
        }
    }
    
    // 显示电话最大长度 Toast
    if (showPhoneMaxToast) {
        LaunchedEffect(Unit) {
            android.widget.Toast.makeText(context, R.string.create_habit_max_length_hint, android.widget.Toast.LENGTH_SHORT).show()
            showPhoneMaxToast = false
        }
    }
    
    // 显示备注最大长度 Toast
    if (showNotesMaxToast) {
        LaunchedEffect(Unit) {
            android.widget.Toast.makeText(context, R.string.create_habit_notes_max_length_hint, android.widget.Toast.LENGTH_SHORT).show()
            showNotesMaxToast = false
        }
    }
    
    // 显示重复邮箱 Toast
    if (showDuplicateEmailToast) {
        LaunchedEffect(Unit) {
            android.widget.Toast.makeText(context, R.string.create_habit_duplicate_supervisor, android.widget.Toast.LENGTH_SHORT).show()
            showDuplicateEmailToast = false
        }
    }
    
    // 显示重复电话 Toast
    if (showDuplicatePhoneToast) {
        LaunchedEffect(Unit) {
            android.widget.Toast.makeText(context, R.string.create_habit_duplicate_supervisor, android.widget.Toast.LENGTH_SHORT).show()
            showDuplicatePhoneToast = false
        }
    }

    // 显示必填项验证 Toast
    var showValidationFailedToast by remember { mutableStateOf(false) }
    if (showValidationFailedToast) {
        LaunchedEffect(Unit) {
            android.widget.Toast.makeText(context, R.string.create_habit_validation_failed, android.widget.Toast.LENGTH_SHORT).show()
            showValidationFailedToast = false
        }
    }

    // 错误状态标记
    var showHabitNameError by remember { mutableStateOf(false) }
    var showReminderTimeError by remember { mutableStateOf(false) }
    var showRepeatDaysError by remember { mutableStateOf(false) }
    var showSupervisorEmailError by remember { mutableStateOf(false) }
    var showSupervisorPhoneError by remember { mutableStateOf(false) }

    // 邮箱验证正则
    fun isValidEmail(email: String): Boolean {
        val emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
        return email.matches(emailPattern.toRegex())
    }

    // 电话验证正则（简单验证，允许数字、+、-、空格）
    fun isValidPhone(phone: String): Boolean {
        val phonePattern = "^[+]?[0-9\\s-]{7,20}$"
        return phone.matches(phonePattern.toRegex())
    }

    // 验证必填项
    fun validateInputs(): Boolean {
        // 重置所有错误状态
        showHabitNameError = false
        showReminderTimeError = false
        showRepeatDaysError = false
        showSupervisorEmailError = false
        showSupervisorPhoneError = false

        var isValid = true

        // 习惯名称必填
        if (habitName.isBlank()) {
            showHabitNameError = true
            isValid = false
        }

        // 提醒时间必填
        if (reminderTimes.isEmpty()) {
            showReminderTimeError = true
            isValid = false
        }

        // 每周活动必须选中至少一天
        if (repeatCycle == RepeatCycle.WEEKLY && selectedRepeatDays.isEmpty()) {
            showRepeatDaysError = true
            isValid = false
        }

        // 选择邮件监督时，至少添加一个邮箱
        if (supervisionMethod == SupervisionMethod.EMAIL && supervisorEmails.isEmpty()) {
            showSupervisorEmailError = true
            isValid = false
        }

        // 选择短信监督时，至少添加一个电话号码
        if (supervisionMethod == SupervisionMethod.SMS && supervisorPhones.isEmpty()) {
            showSupervisorPhoneError = true
            isValid = false
        }

        if (!isValid) {
            showValidationFailedToast = true
        }

        return isValid
    }

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
                            if (clickHandler.isEnabled && !isSaving) {
                                scope.launch {
                                    clickHandler.processClick {
                                        // 验证必填项
                                        if (!validateInputs()) {
                                            return@processClick
                                        }
                                        
                                        // 构建 Habit 对象
                                        val habit = if (editMode == EditMode.EDIT && habitId != null) {
                                            // 编辑模式：更新现有习惯
                                            viewModel.getHabitById(habitId)?.copy(
                                                title = habitName,
                                                repeatCycle = repeatCycle,
                                                notes = notes,
                                                supervisionMethod = supervisionMethod
                                            )?.copyWithRepeatDays(selectedRepeatDays.toList())
                                            ?.copyWithReminderTimes(reminderTimes)
                                            ?.copyWithSupervisorEmails(supervisorEmails)
                                            ?.copyWithSupervisorPhones(supervisorPhones)
                                        } else {
                                            // 新建模式：创建新习惯
                                            Habit(
                                                title = habitName,
                                                repeatCycle = repeatCycle,
                                                notes = notes,
                                                supervisionMethod = supervisionMethod
                                            )
                                            .copyWithRepeatDays(selectedRepeatDays.toList())
                                            .copyWithReminderTimes(reminderTimes)
                                            .copyWithSupervisorEmails(supervisorEmails)
                                            .copyWithSupervisorPhones(supervisorPhones)
                                        }
                                        
                                        // 保存习惯
                                        if (habit != null) {
                                            viewModel.saveHabit(habit)
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isSaving && clickHandler.isEnabled
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Repeat cycle selection - Button group with custom shapes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy((-1).dp)
            ) {
                RepeatCycle.values().forEachIndexed { index, cycle ->
                    val isSelected = repeatCycle == cycle
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    
                    // 按压时圆角变大动画
                    val cornerSize = animateDpAsState(
                        targetValue = if (isPressed) 24.dp else 8.dp,
                        label = "cornerSize"
                    )
                    
                    val backgroundColor = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    }
                    
                    val contentColor = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    }
                    
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) { repeatCycle = cycle },
                        shape = RoundedCornerShape(
                            topStart = if (index == 0) cornerSize.value else 0.dp,
                            topEnd = if (index == RepeatCycle.values().lastIndex) cornerSize.value else 0.dp,
                            bottomStart = if (index == 0) cornerSize.value else 0.dp,
                            bottomEnd = if (index == RepeatCycle.values().lastIndex) cornerSize.value else 0.dp
                        ),
                        color = backgroundColor,
                        contentColor = contentColor
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (cycle) {
                                    RepeatCycle.DAILY -> stringResource(id = R.string.create_habit_daily_option)
                                    RepeatCycle.WEEKLY -> stringResource(id = R.string.create_habit_weekly_option)
                                },
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }

            // Repeat days selection (for weekly cycle)
            AnimatedVisibility(
                visible = repeatCycle == RepeatCycle.WEEKLY,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (showRepeatDaysError) {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        } else {
                            androidx.compose.ui.graphics.Color.Transparent
                        }
                    ),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Sunday (0) to Saturday (6)
                            val dayLabels = listOf(
                                stringResource(id = R.string.create_habit_day_sunday),
                                stringResource(id = R.string.create_habit_day_monday),
                                stringResource(id = R.string.create_habit_day_tuesday),
                                stringResource(id = R.string.create_habit_day_wednesday),
                                stringResource(id = R.string.create_habit_day_thursday),
                                stringResource(id = R.string.create_habit_day_friday),
                                stringResource(id = R.string.create_habit_day_saturday)
                            )

                            dayLabels.forEachIndexed { index, label ->
                                FilterChip(
                                    selected = selectedRepeatDays.contains(index),
                                    onClick = {
                                        selectedRepeatDays = if (selectedRepeatDays.contains(index)) {
                                            selectedRepeatDays - index
                                        } else {
                                            selectedRepeatDays + index
                                        }
                                        // 用户选择日期时清除错误状态
                                        showRepeatDaysError = false
                                    },
                                    label = {
                                        Text(
                                            text = label,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Habit name input field
            OutlinedTextField(
                value = habitName,
                onValueChange = { newValue ->
                    if (newValue.length > 100) {
                        showMaxLengthToast = true
                    }
                    habitName = newValue.take(100)
                    // 用户输入时清除错误状态
                    showHabitNameError = false
                },
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
                isError = showHabitNameError,
                supportingText = if (showHabitNameError) {
                    { Text(text = context.getString(R.string.create_habit_habit_name_label)) }
                } else null,
            )

            // Reminder time section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (showReminderTimeError) {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    } else {
                        androidx.compose.ui.graphics.Color.Transparent
                    }
                ),
                shape = RoundedCornerShape(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    // Header row with add button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.create_habit_reminder_time_label),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (showReminderTimeError) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )

                        // Button with animated shape
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()

                        // 按压时圆角变大动画
                        val cornerSize = animateDpAsState(
                            targetValue = if (isPressed) 24.dp else 8.dp,
                            label = "cornerSize"
                        )

                        Surface(
                            modifier = Modifier
                                .height(40.dp)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null
                                ) {
                                    currentTimePickerTime = java.time.LocalTime.now()
                                    showTimePicker = true
                                },
                            shape = RoundedCornerShape(cornerSize.value),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = stringResource(id = R.string.create_habit_add_reminder_button),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Reminder status row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (reminderTimes.isEmpty()) {
                                stringResource(id = R.string.create_habit_no_reminder_set)
                            } else if (isReminderExpanded) {
                                stringResource(id = R.string.create_habit_reminder_set_count, reminderTimes.size)
                            } else {
                                stringResource(id = R.string.create_habit_reminder_set_count, reminderTimes.size)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        if (reminderTimes.isNotEmpty()) {
                            TextButton(
                                onClick = { isReminderExpanded = !isReminderExpanded }
                            ) {
                                Text(
                                    text = if (isReminderExpanded) {
                                        stringResource(id = R.string.create_habit_collapse_button)
                                    } else {
                                        stringResource(id = R.string.create_habit_expand_button)
                                    }
                                )
                                Icon(
                                    imageVector = if (isReminderExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                                    contentDescription = if (isReminderExpanded) "收起" else "展开",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    
                    // Expandable reminder times list
                    AnimatedVisibility(
                        visible = isReminderExpanded && reminderTimes.isNotEmpty(),
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            reminderTimes.forEachIndexed { index, time ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = time,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    IconButton(
                                        onClick = {
                                            reminderTimes = reminderTimes.filterIndexed { i, _ -> i != index }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = "删除提醒",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // TimePicker Dialog
            if (showTimePicker) {
                TimePickerDialog(
                    currentTime = currentTimePickerTime,
                    onDismissRequest = { showTimePicker = false },
                    onConfirmRequest = { selectedTime ->
                        val timeString = String.format("%02d:%02d", selectedTime.hour, selectedTime.minute)
                        if (reminderTimes.contains(timeString)) {
                            android.widget.Toast.makeText(context, context.getString(R.string.create_habit_duplicate_time), android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            reminderTimes = reminderTimes + timeString
                            // 添加提醒时间后清除错误状态
                            showReminderTimeError = false
                        }
                        showTimePicker = false
                    }
                )
            }

            // Supervision method section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (supervisionMethod == SupervisionMethod.EMAIL && showSupervisorEmailError) {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    } else if (supervisionMethod == SupervisionMethod.SMS && showSupervisorPhoneError) {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    } else {
                        androidx.compose.ui.graphics.Color.Transparent
                    }
                ),
                shape = RoundedCornerShape(0.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(id = R.string.create_habit_supervision_label),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (supervisionMethod == SupervisionMethod.EMAIL && showSupervisorEmailError) {
                            MaterialTheme.colorScheme.error
                        } else if (supervisionMethod == SupervisionMethod.SMS && showSupervisorPhoneError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Supervision method dropdown
                    var expanded by remember { mutableStateOf(false) }
                    val supervisionOptions = listOf(
                        stringResource(id = R.string.create_habit_supervision_none),
                        stringResource(id = R.string.create_habit_supervision_email),
                        stringResource(id = R.string.create_habit_supervision_sms)
                    )
                    
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = when (supervisionMethod) {
                                SupervisionMethod.NONE -> supervisionOptions[0]
                                SupervisionMethod.EMAIL -> supervisionOptions[1]
                                SupervisionMethod.SMS -> supervisionOptions[2]
                            },
                            onValueChange = {},
                            readOnly = true,
                            label = {
                                Text(text = stringResource(id = R.string.create_habit_supervision_hint))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            supervisionOptions.forEachIndexed { index, option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        supervisionMethod = when (index) {
                                            0 -> SupervisionMethod.NONE
                                            1 -> SupervisionMethod.EMAIL
                                            2 -> SupervisionMethod.SMS
                                            else -> SupervisionMethod.NONE
                                        }
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    // Email input section
                    AnimatedVisibility(
                        visible = supervisionMethod == SupervisionMethod.EMAIL,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = stringResource(id = R.string.create_habit_supervisor_email_label),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Email input row
                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { 
                                    if (it.length <= 100) {
                                        emailInput = it
                                        showEmailError = false
                                    } else {
                                        showEmailMaxToast = true
                                    }
                                },
                                label = {
                                    Text(text = stringResource(id = R.string.create_habit_supervisor_email_hint))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                isError = showEmailError,
                                supportingText = if (showEmailError) {
                                    {
                                        Text(text = stringResource(id = R.string.create_habit_supervisor_email_invalid))
                                    }
                                } else null,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email
                                ),
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            if (isValidEmail(emailInput)) {
                                                if (!supervisorEmails.contains(emailInput)) {
                                                    supervisorEmails = supervisorEmails + emailInput
                                                    emailInput = ""
                                                    showEmailError = false
                                                    showSupervisorEmailError = false
                                                } else {
                                                    showDuplicateEmailToast = true
                                                }
                                            } else {
                                                showEmailError = true
                                            }
                                        },
                                        enabled = emailInput.isNotBlank()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Add,
                                            contentDescription = stringResource(id = R.string.create_habit_supervisor_email_add),
                                            tint = if (emailInput.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                            )
                            
                            // Email list
                            if (supervisorEmails.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Email list header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.create_habit_supervisor_count, supervisorEmails.size),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    TextButton(
                                        onClick = { isEmailListExpanded = !isEmailListExpanded }
                                    ) {
                                        Text(
                                                text = if (isEmailListExpanded) {
                                                    stringResource(id = R.string.create_habit_collapse_button)
                                                } else {
                                                    stringResource(id = R.string.create_habit_expand_button)
                                                }
                                            )
                                        Icon(
                                            imageVector = if (isEmailListExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                                            contentDescription = if (isEmailListExpanded) "收起" else "展开",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                
                                // Expandable email list
                                AnimatedVisibility(
                                    visible = isEmailListExpanded,
                                    enter = expandVertically(),
                                    exit = shrinkVertically()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(top = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        supervisorEmails.forEachIndexed { index, email ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = email,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                IconButton(
                                                    onClick = {
                                                        supervisorEmails = supervisorEmails.filterIndexed { i, _ -> i != index }
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Delete,
                                                        contentDescription = "删除邮箱",
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Phone input section
                    AnimatedVisibility(
                        visible = supervisionMethod == SupervisionMethod.SMS,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = stringResource(id = R.string.create_habit_supervisor_phone_label),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Phone input row
                            OutlinedTextField(
                                value = phoneInput,
                                onValueChange = { 
                                    if (it.length <= 20) {
                                        phoneInput = it
                                        showPhoneError = false
                                    } else {
                                        showPhoneMaxToast = true
                                    }
                                },
                                label = {
                                    Text(text = stringResource(id = R.string.create_habit_supervisor_phone_hint))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                isError = showPhoneError,
                                supportingText = if (showPhoneError) {
                                    {
                                        Text(text = stringResource(id = R.string.create_habit_supervisor_phone_invalid))
                                    }
                                } else null,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Phone
                                ),
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            if (isValidPhone(phoneInput)) {
                                                if (!supervisorPhones.contains(phoneInput)) {
                                                    supervisorPhones = supervisorPhones + phoneInput
                                                    phoneInput = ""
                                                    showPhoneError = false
                                                    showSupervisorPhoneError = false
                                                } else {
                                                    showDuplicatePhoneToast = true
                                                }
                                            } else {
                                                showPhoneError = true
                                            }
                                        },
                                        enabled = phoneInput.isNotBlank()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Add,
                                            contentDescription = stringResource(id = R.string.create_habit_supervisor_phone_add),
                                            tint = if (phoneInput.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                            )
                            
                            // Phone list
                            if (supervisorPhones.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Phone list header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.create_habit_supervisor_count, supervisorPhones.size),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    TextButton(
                                        onClick = { isPhoneListExpanded = !isPhoneListExpanded }
                                    ) {
                                        Text(
                                                text = if (isPhoneListExpanded) {
                                                    stringResource(id = R.string.create_habit_collapse_button)
                                                } else {
                                                    stringResource(id = R.string.create_habit_expand_button)
                                                }
                                            )
                                        Icon(
                                            imageVector = if (isPhoneListExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                                            contentDescription = if (isPhoneListExpanded) "收起" else "展开",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                
                                // Expandable phone list
                                AnimatedVisibility(
                                    visible = isPhoneListExpanded,
                                    enter = expandVertically(),
                                    exit = shrinkVertically()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(top = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        supervisorPhones.forEachIndexed { index, phone ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = phone,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                IconButton(
                                                    onClick = {
                                                        supervisorPhones = supervisorPhones.filterIndexed { i, _ -> i != index }
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Delete,
                                                        contentDescription = "删除号码",
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Notes section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                ),
                shape = RoundedCornerShape(0.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(id = R.string.create_habit_notes_label),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = notes,
                        onValueChange = {
                            if (it.length <= 2000) {
                                notes = it
                            } else {
                                showNotesMaxToast = true
                            }
                        },
                        label = {
                            Text(text = stringResource(id = R.string.create_habit_notes_hint))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        maxLines = 10,
                        minLines = 5,
                    )
                }
            }

            // TODO: Add more habit creation fields here
            // - Habit repeat days (for weekly cycle)
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