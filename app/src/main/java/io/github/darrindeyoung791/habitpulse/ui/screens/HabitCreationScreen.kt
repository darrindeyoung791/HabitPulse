package io.github.darrindeyoung791.habitpulse.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current

    // 获取 ViewModel - use preview mode if application is null
    val viewModel: HabitViewModel = if (application != null) {
        remember {
            HabitViewModel.Factory(application).create(HabitViewModel::class.java)
        }
    } else {
        // Preview mode: create a ViewModel with fake in-memory repository
        remember {
            val fakeHabitDao = FakeHabitDaoForCreation()
            val fakeCompletionDao = FakeHabitCompletionDaoForCreation()
            val fakeRepository = io.github.darrindeyoung791.habitpulse.data.repository.HabitRepository(fakeHabitDao, fakeCompletionDao)
            val fakeOnboardingPreferences = io.github.darrindeyoung791.habitpulse.utils.OnboardingPreferences(context.applicationContext)
            HabitViewModel(fakeRepository, fakeOnboardingPreferences)
        }
    }

    // 收集 ViewModel 状态
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val saveSuccess by viewModel.saveSuccess.collectAsStateWithLifecycle()

    // 避免旧的保存成功状态在进入创建/编辑页面后被误触发导致立即跳转和回到顶部
    LaunchedEffect(Unit) {
        viewModel.resetSaveSuccess()
    }

    // Track whether the initial staggered animation phase has completed
    // Once true, newly composed items (from scrolling or visibility changes) should skip animation
    var initialAnimationComplete by remember { mutableStateOf(false) }

    // Estimate max delay needed for initial staggered animation
    // We have ~6 major components, each with 30ms delay + ~500ms animation duration
    val estimatedMaxAnimationTime = 6 * 30L + 500L

    // Mark initial animation as complete after estimated time
    // This ensures newly composed items (from scrolling) skip the animation
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(estimatedMaxAnimationTime)
        initialAnimationComplete = true
    }

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
    var showEmailMaxToast by remember { mutableStateOf(false) }
    var showPhoneMaxToast by remember { mutableStateOf(false) }
    var showDuplicateEmailToast by remember { mutableStateOf(false) }
    var showDuplicatePhoneToast by remember { mutableStateOf(false) }
    var showInvalidEmailToast by remember { mutableStateOf(false) }
    var showInvalidPhoneToast by remember { mutableStateOf(false) }
    var isEmailListExpanded by remember { mutableStateOf(false) }
    var isPhoneListExpanded by remember { mutableStateOf(false) }

    // Repeat days state (for weekly cycle)
    var selectedRepeatDays by remember { mutableStateOf<Set<Int>>(setOf()) }

    // Notes state
    var notes by remember { mutableStateOf("") }
    var showNotesMaxToast by remember { mutableStateOf(false) }

    // Focus requester for habit name field (only for CREATE mode)
    val habitNameFocusRequester = remember { FocusRequester() }

    // Request focus on habit name field when creating new habit
    LaunchedEffect(editMode) {
        if (editMode == EditMode.CREATE) {
            habitNameFocusRequester.requestFocus()
        }
    }

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
            viewModel.requestScrollToTop()
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
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            android.widget.Toast.makeText(context, R.string.create_habit_max_length_hint, android.widget.Toast.LENGTH_SHORT).show()
            showMaxLengthToast = false
        }
    }

    // 显示邮箱最大长度 Toast
    if (showEmailMaxToast) {
        LaunchedEffect(Unit) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            android.widget.Toast.makeText(context, R.string.create_habit_max_length_hint, android.widget.Toast.LENGTH_SHORT).show()
            showEmailMaxToast = false
        }
    }

    // 显示电话最大长度 Toast
    if (showPhoneMaxToast) {
        LaunchedEffect(Unit) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            android.widget.Toast.makeText(context, R.string.create_habit_max_length_hint, android.widget.Toast.LENGTH_SHORT).show()
            showPhoneMaxToast = false
        }
    }

    // 显示备注最大长度 Toast
    if (showNotesMaxToast) {
        LaunchedEffect(Unit) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            android.widget.Toast.makeText(context, R.string.create_habit_notes_max_length_hint, android.widget.Toast.LENGTH_SHORT).show()
            showNotesMaxToast = false
        }
    }

    // 显示重复邮箱 Toast
    if (showDuplicateEmailToast) {
        LaunchedEffect(Unit) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            android.widget.Toast.makeText(context, R.string.create_habit_duplicate_supervisor, android.widget.Toast.LENGTH_SHORT).show()
            showDuplicateEmailToast = false
        }
    }

    // 显示重复电话 Toast
    if (showDuplicatePhoneToast) {
        LaunchedEffect(Unit) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            android.widget.Toast.makeText(context, R.string.create_habit_duplicate_supervisor, android.widget.Toast.LENGTH_SHORT).show()
            showDuplicatePhoneToast = false
        }
    }

    // 显示必填项验证 Toast
    var showValidationFailedToast by remember { mutableStateOf(false) }
    if (showValidationFailedToast) {
        LaunchedEffect(Unit) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            android.widget.Toast.makeText(context, R.string.create_habit_validation_failed, android.widget.Toast.LENGTH_SHORT).show()
            showValidationFailedToast = false
        }
    }

    // 显示无效邮箱 Toast
    if (showInvalidEmailToast) {
        LaunchedEffect(Unit) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            android.widget.Toast.makeText(context, R.string.create_habit_supervisor_email_invalid, android.widget.Toast.LENGTH_SHORT).show()
            showInvalidEmailToast = false
        }
    }

    // 显示无效电话 Toast
    if (showInvalidPhoneToast) {
        LaunchedEffect(Unit) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            android.widget.Toast.makeText(context, R.string.create_habit_supervisor_phone_invalid, android.widget.Toast.LENGTH_SHORT).show()
            showInvalidPhoneToast = false
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
                            if (!isSaving) {
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
                                            // Scroll to top only when saving
                                            viewModel.requestScrollToTop()
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isSaving
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
            // 1. Repeat cycle selection - Button group with custom shapes
            AnimatedCreationItem(
                index = 0,
                initialAnimationComplete = initialAnimationComplete
            ) {
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
                        MaterialTheme.colorScheme.surfaceVariant
                    }

                    val contentColor = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
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
            } // Close AnimatedCreationItem for repeat cycle

            // 2. Repeat days selection (for weekly cycle)
            AnimatedCreationItem(
                index = 1,
                initialAnimationComplete = initialAnimationComplete
            ) {
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
            } // Close AnimatedCreationItem for repeat days

            // 3. Habit name input field
            AnimatedCreationItem(
                index = 2,
                initialAnimationComplete = initialAnimationComplete
            ) {
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(habitNameFocusRequester),
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
            } // Close AnimatedCreationItem for habit name

            // 4. Reminder time section
            AnimatedCreationItem(
                index = 3,
                initialAnimationComplete = initialAnimationComplete
            ) {
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
            } // Close AnimatedCreationItem for reminder time

            // TimePicker Dialog
            if (showTimePicker) {
                TimePickerDialog(
                    currentTime = currentTimePickerTime,
                    onDismissRequest = { showTimePicker = false },
                    onConfirmRequest = { selectedTime ->
                        val timeString = String.format("%02d:%02d", selectedTime.hour, selectedTime.minute)
                        if (reminderTimes.contains(timeString)) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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

            // 5. Supervision method section
            AnimatedCreationItem(
                index = 4,
                initialAnimationComplete = initialAnimationComplete
            ) {
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
                                    } else {
                                        showEmailMaxToast = true
                                    }
                                },
                                label = {
                                    Text(text = stringResource(id = R.string.create_habit_supervisor_email_hint))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
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
                                                    showSupervisorEmailError = false
                                                } else {
                                                    showDuplicateEmailToast = true
                                                }
                                            } else {
                                                showInvalidEmailToast = true
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
                                    } else {
                                        showPhoneMaxToast = true
                                    }
                                },
                                label = {
                                    Text(text = stringResource(id = R.string.create_habit_supervisor_phone_hint))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
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
                                                    showSupervisorPhoneError = false
                                                } else {
                                                    showDuplicatePhoneToast = true
                                                }
                                            } else {
                                                showInvalidPhoneToast = true
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
            } // Close AnimatedCreationItem for supervision method

            // 6. Notes section
            AnimatedCreationItem(
                index = 5,
                initialAnimationComplete = initialAnimationComplete
            ) {
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
                } // Close Card
            } // Close AnimatedCreationItem for notes

            // TODO: Add more habit creation fields here
            // - Habit repeat days (for weekly cycle)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HabitCreationScreenPreview() {
    HabitPulseTheme {
        HabitCreationScreen(
            onNavigateBack = {},
            application = null  // Use preview mode with fake data
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HabitCreationScreenDarkPreview() {
    HabitPulseTheme(darkTheme = true) {
        HabitCreationScreen(
            onNavigateBack = {},
            application = null  // Use preview mode with fake data
        )
    }
}

/**
 * Reusable wrapper composable for staggered entry animations in HabitCreationScreen.
 * 
 * Wraps child content with the same animation pattern as MultiSelectSortScreen:
 * - Distance-based delay: index * 30ms
 * - Alpha: 0→1
 * - Scale: 0.93→1
 * - TranslationY: 25px→0
 * - Spring spec: dampingRatio=0.85f, stiffness=Spring.StiffnessMediumLow
 * 
 * When initialAnimationComplete is true, skips animation entirely to prevent
 * newly composed items (from scrolling) from animating.
 * 
 * @param index Position in the list for stagger delay calculation
 * @param initialAnimationComplete Whether initial animation phase has completed
 * @param content Child composable to animate
 */
@Composable
private fun AnimatedCreationItem(
    index: Int,
    initialAnimationComplete: Boolean,
    content: @Composable () -> Unit
) {
    // Calculate animation delay based on index
    val animationDelayMs = index * 30L

    // If initial animation phase is complete, skip animation entirely
    // This prevents newly composed items (from scrolling) from animating
    val shouldAnimate = !initialAnimationComplete

    // Staggered enter animation state
    var animationTriggered by remember { mutableStateOf(!shouldAnimate) }

    LaunchedEffect(Unit) {
        if (shouldAnimate && !animationTriggered) {
            kotlinx.coroutines.delay(animationDelayMs)
            animationTriggered = true
        }
    }

    // Use transition to animate alpha, scale, and translation
    val transition = updateTransition(targetState = animationTriggered, label = "staggeredEnter")

    val alpha by transition.animateFloat(
        transitionSpec = {
            spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
        },
        label = "alpha"
    ) { triggered ->
        if (triggered) 1f else 0f
    }

    val scale by transition.animateFloat(
        transitionSpec = {
            spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
        },
        label = "scale"
    ) { triggered ->
        if (triggered) 1f else 0.93f
    }

    val translationY by transition.animateFloat(
        transitionSpec = {
            spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
        },
        label = "translationY"
    ) { triggered ->
        if (triggered) 0f else 25f // ~25px slide from below (subtle)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha
                this.translationY = translationY
                this.scaleX = scale
                this.scaleY = scale
            }
    ) {
        content()
    }
}

/**
 * Fake HabitDao implementation for Android Studio Preview in HabitCreationScreen.
 * Provides in-memory storage for UI preview.
 */
@Suppress("unused")
private class FakeHabitDaoForCreation : io.github.darrindeyoung791.habitpulse.data.database.dao.HabitDao {
    private val habits = mutableListOf<io.github.darrindeyoung791.habitpulse.data.model.Habit>()

    override fun getAllHabitsFlow(): kotlinx.coroutines.flow.Flow<List<io.github.darrindeyoung791.habitpulse.data.model.Habit>> {
        return kotlinx.coroutines.flow.flowOf(habits)
    }

    override suspend fun getAllHabits(): List<io.github.darrindeyoung791.habitpulse.data.model.Habit> = habits

    override fun getHabitByIdFlow(id: java.util.UUID): kotlinx.coroutines.flow.Flow<io.github.darrindeyoung791.habitpulse.data.model.Habit?> {
        return kotlinx.coroutines.flow.flowOf(habits.find { it.id == id })
    }

    override suspend fun getHabitById(id: java.util.UUID): io.github.darrindeyoung791.habitpulse.data.model.Habit? {
        return habits.find { it.id == id }
    }

    override fun getIncompleteHabitsFlow(): kotlinx.coroutines.flow.Flow<List<io.github.darrindeyoung791.habitpulse.data.model.Habit>> {
        return kotlinx.coroutines.flow.flowOf(habits.filter { !it.completedToday })
    }

    override fun getCompletedHabitsFlow(): kotlinx.coroutines.flow.Flow<List<io.github.darrindeyoung791.habitpulse.data.model.Habit>> {
        return kotlinx.coroutines.flow.flowOf(habits.filter { it.completedToday })
    }

    override suspend fun insert(habit: io.github.darrindeyoung791.habitpulse.data.model.Habit): Long {
        habits.add(habit)
        return 0
    }

    override suspend fun update(habit: io.github.darrindeyoung791.habitpulse.data.model.Habit) {
        val index = habits.indexOfFirst { it.id == habit.id }
        if (index >= 0) {
            habits[index] = habit
        }
    }

    override suspend fun delete(habit: io.github.darrindeyoung791.habitpulse.data.model.Habit) {
        habits.removeIf { it.id == habit.id }
    }

    override suspend fun deleteAll() {
        habits.clear()
    }

    override suspend fun updateCompletionStatus(id: java.util.UUID, completed: Boolean, timestamp: Long) {
        val index = habits.indexOfFirst { it.id == id }
        if (index >= 0) {
            habits[index] = habits[index].copy(
                completedToday = completed,
                lastCompletedDate = timestamp,
                completionCount = if (completed) habits[index].completionCount + 1 else habits[index].completionCount,
                modifiedDate = timestamp
            )
        }
    }

    override suspend fun undoCompletionStatus(id: java.util.UUID, timestamp: Long) {
        val index = habits.indexOfFirst { it.id == id }
        if (index >= 0) {
            habits[index] = habits[index].copy(
                completedToday = false,
                completionCount = maxOf(0, habits[index].completionCount - 1),
                modifiedDate = timestamp
            )
        }
    }

    override suspend fun incrementCompletionCount(id: java.util.UUID, timestamp: Long) {
        val index = habits.indexOfFirst { it.id == id }
        if (index >= 0) {
            habits[index] = habits[index].copy(
                completedToday = true,
                completionCount = habits[index].completionCount + 1,
                lastCompletedDate = timestamp,
                modifiedDate = timestamp
            )
        }
    }

    override suspend fun resetAllCompletionStatus(timestamp: Long) {
        habits.replaceAll { habit ->
            habit.copy(
                completedToday = false,
                modifiedDate = timestamp
            )
        }
    }

    override fun getHabitCount(): kotlinx.coroutines.flow.Flow<Int> {
        return kotlinx.coroutines.flow.flowOf(habits.size)
    }

    override fun searchHabitsFlow(query: String): kotlinx.coroutines.flow.Flow<List<io.github.darrindeyoung791.habitpulse.data.model.Habit>> {
        val searchQuery = query.trim('%')
        return kotlinx.coroutines.flow.flowOf(
            habits.filter { habit ->
                habit.title.contains(searchQuery, ignoreCase = true) ||
                habit.notes.contains(searchQuery, ignoreCase = true)
            }
        )
    }

    override fun getHabitsBySortOrderFlow(): kotlinx.coroutines.flow.Flow<List<io.github.darrindeyoung791.habitpulse.data.model.Habit>> {
        return kotlinx.coroutines.flow.flowOf(habits.sortedBy { it.sortOrder })
    }

    override suspend fun updateSortOrder(id: java.util.UUID, sortOrder: Int, timestamp: Long) {
        val index = habits.indexOfFirst { it.id == id }
        if (index >= 0) {
            habits[index] = habits[index].copy(sortOrder = sortOrder, modifiedDate = timestamp)
        }
    }

    override suspend fun deleteHabitsByIds(habitIds: Set<java.util.UUID>) {
        habits.removeAll { it.id in habitIds }
    }
}

/**
 * Fake HabitCompletionDao implementation for Android Studio Preview.
 * Provides in-memory storage for UI preview.
 */
@Suppress("unused")
private class FakeHabitCompletionDaoForCreation : io.github.darrindeyoung791.habitpulse.data.database.dao.HabitCompletionDao {
    private val completions = mutableListOf<io.github.darrindeyoung791.habitpulse.data.model.HabitCompletion>()

    override fun getCompletionsByHabitIdFlow(habitId: java.util.UUID): kotlinx.coroutines.flow.Flow<List<io.github.darrindeyoung791.habitpulse.data.model.HabitCompletion>> {
        return kotlinx.coroutines.flow.flowOf(completions.filter { it.habitId == habitId })
    }

    override fun getAllCompletionsFlow(): kotlinx.coroutines.flow.Flow<List<io.github.darrindeyoung791.habitpulse.data.model.HabitCompletion>> {
        return kotlinx.coroutines.flow.flowOf(completions.toList())
    }

    override suspend fun getCompletionsByHabitId(habitId: java.util.UUID): List<io.github.darrindeyoung791.habitpulse.data.model.HabitCompletion> {
        return completions.filter { it.habitId == habitId }
    }

    override suspend fun getCompletionsByHabitIdAndDate(
        habitId: java.util.UUID,
        date: String
    ): List<io.github.darrindeyoung791.habitpulse.data.model.HabitCompletion> {
        return completions.filter { it.habitId == habitId && it.completedDateLocal == date }
    }

    override suspend fun getCompletionsByHabitIdAndDateRange(
        habitId: java.util.UUID,
        startDate: String,
        endDate: String
    ): List<io.github.darrindeyoung791.habitpulse.data.model.HabitCompletion> {
        return completions.filter { it.habitId == habitId && it.completedDateLocal in startDate..endDate }
    }

    override suspend fun getCompletionsByDate(date: String): List<io.github.darrindeyoung791.habitpulse.data.model.HabitCompletion> {
        return completions.filter { it.completedDateLocal == date }
    }

    override suspend fun getTodayCompletionCount(habitId: java.util.UUID, date: String): Int {
        return completions.count { it.habitId == habitId && it.completedDateLocal == date }
    }

    override suspend fun insert(completion: io.github.darrindeyoung791.habitpulse.data.model.HabitCompletion): Long {
        completions.add(completion)
        return 0
    }

    override suspend fun insertAll(completions: List<io.github.darrindeyoung791.habitpulse.data.model.HabitCompletion>) {
        this.completions.addAll(completions)
    }

    override suspend fun delete(completion: io.github.darrindeyoung791.habitpulse.data.model.HabitCompletion) {
        completions.remove(completion)
    }

    override suspend fun deleteByHabitId(habitId: java.util.UUID) {
        completions.removeAll { it.habitId == habitId }
    }

    override suspend fun deleteByDate(date: String) {
        completions.removeAll { it.completedDateLocal == date }
    }

    override suspend fun deleteAll() {
        completions.clear()
    }

    override fun getCompletionCount(): kotlinx.coroutines.flow.Flow<Int> {
        return kotlinx.coroutines.flow.flowOf(completions.size)
    }

    override suspend fun getCompletionCountByHabitId(habitId: java.util.UUID): Int {
        return completions.count { it.habitId == habitId }
    }
}