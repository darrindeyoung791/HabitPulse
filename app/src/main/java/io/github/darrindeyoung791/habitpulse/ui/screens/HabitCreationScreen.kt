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
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.darrindeyoung791.habitpulse.HabitPulseApplication
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.data.model.Habit
import io.github.darrindeyoung791.habitpulse.data.model.RepeatCycle
import io.github.darrindeyoung791.habitpulse.ui.theme.HabitPulseTheme
import io.github.darrindeyoung791.habitpulse.ui.utils.rememberDebounceClickHandler
import io.github.darrindeyoung791.habitpulse.ui.utils.rememberHideKeyboardAndNavigateBack
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (displayMode == TimePickerDisplayMode.Picker) {
                TimePicker(state = timePickerState)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.create_habit_time_picker_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                TimeInput(state = timePickerState)
            }
        }
    }
}

private val VALID_EMAIL = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
private val VALID_PHONE = Regex("^[+]?[0-9\\s-]{7,20}$")

/**
 * 添加监督人对话框 - 带 Tab 切换（邮箱/电话），防止误切换和误触关闭
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSupervisorDialog(
    initialRegionCode: String,
    hasAddedEmail: Boolean,
    hasAddedPhone: Boolean,
    onDismiss: () -> Unit,
    onAddEmail: (String) -> Unit,
    onAddPhone: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var emailInput by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf(false) }
    var phoneInput by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf(false) }
    var currentRegionCode by remember { mutableStateOf(initialRegionCode) }
    var prefixExpanded by remember { mutableStateOf(false) }
    val emailFocusRequester = remember { FocusRequester() }
    val phoneFocusRequester = remember { FocusRequester() }
    val smsWarningColor = Color(0xFFFFF3CD)
    val smsWarningTextColor = Color(0xFF664D00)
    val context = LocalContext.current

    val canSwitchToEmail = phoneInput.isBlank()
    val canSwitchToPhone = emailInput.isBlank()

    LaunchedEffect(selectedTab) {
        if (selectedTab == 0) {
            emailFocusRequester.requestFocus()
        } else {
            phoneFocusRequester.requestFocus()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedTab == 0) {
                        if (VALID_EMAIL.matches(emailInput)) {
                            onAddEmail(emailInput)
                        } else {
                            emailError = true
                        }
                    } else {
                        val fullPhone = "${currentRegionCode.substringBefore(" ")}$phoneInput"
                        if (VALID_PHONE.matches(fullPhone)) {
                            onAddPhone(fullPhone)
                        } else {
                            phoneError = true
                        }
                    }
                },
                enabled = if (selectedTab == 0) emailInput.isNotBlank() else phoneInput.isNotBlank()
            ) {
                Text(stringResource(R.string.create_habit_time_picker_confirm))
            }
        },
        dismissButton = {
            val showDone = hasAddedEmail && hasAddedPhone
            TextButton(onClick = onDismiss) {
                Text(stringResource(if (showDone) R.string.create_habit_add_supervisor_done else R.string.create_habit_time_picker_dismiss))
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        ),
        title = { Text(stringResource(R.string.create_habit_add_supervisor_title)) },
        text = {
            Column {
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = {
                            if (canSwitchToEmail) {
                                selectedTab = 0
                            } else {
                                android.widget.Toast.makeText(
                                    context,
                                    R.string.create_habit_tab_switch_blocked,
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        text = { Text(stringResource(R.string.create_habit_tab_email)) },
                        modifier = if (!canSwitchToEmail) Modifier.alpha(0.38f) else Modifier
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            if (canSwitchToPhone) {
                                selectedTab = 1
                            } else {
                                android.widget.Toast.makeText(
                                    context,
                                    R.string.create_habit_tab_switch_blocked,
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        text = { Text(stringResource(R.string.create_habit_tab_phone)) },
                        modifier = if (!canSwitchToPhone) Modifier.alpha(0.38f) else Modifier
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                when (selectedTab) {
                    0 -> {
                        Text(
                            text = stringResource(R.string.create_habit_add_email_dialog_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it; emailError = false },
                            label = { Text(stringResource(R.string.create_habit_supervisor_email_hint)) },
                            singleLine = true,
                            isError = emailError,
                            supportingText = if (emailError) {
                                { Text(stringResource(R.string.create_habit_supervisor_email_invalid)) }
                            } else null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth().focusRequester(emailFocusRequester)
                        )
                    }
                    1 -> {
                        Text(
                            text = stringResource(R.string.create_habit_add_phone_dialog_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .background(smsWarningColor, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Outlined.Warning, contentDescription = null, modifier = Modifier.size(16.dp), tint = smsWarningTextColor)
                            Text(stringResource(R.string.create_habit_supervisor_phone_sms_cost_warning), style = MaterialTheme.typography.bodySmall, color = smsWarningTextColor)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val regionCodeSelector = stringResource(R.string.region_code_selector)
                            Box {
                                OutlinedTextField(
                                    value = currentRegionCode,
                                    onValueChange = {},
                                    modifier = Modifier.width(120.dp).semantics { contentDescription = "$currentRegionCode $regionCodeSelector" },
                                    readOnly = true,
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyLarge,
                                    label = { Text(stringResource(R.string.region_code_label)) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary
                                    ),
                                    trailingIcon = { Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(20.dp)) }
                                )
                                Box(
                                    modifier = Modifier.matchParentSize().clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { prefixExpanded = true }
                                )
                                DropdownMenu(expanded = prefixExpanded, onDismissRequest = { prefixExpanded = false }) {
                                    REGION_CODES.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text("${option.prefix} ${stringResource(option.displayNameRes)}", style = MaterialTheme.typography.bodyMedium) },
                                            onClick = { currentRegionCode = option.prefix; prefixExpanded = false }
                                        )
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = phoneInput,
                                onValueChange = { phoneInput = it; phoneError = false },
                                label = { Text(stringResource(R.string.create_habit_supervisor_phone_hint)) },
                                modifier = Modifier.weight(1f).focusRequester(phoneFocusRequester),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge,
                                isError = phoneError,
                                supportingText = if (phoneError) { { Text(stringResource(R.string.create_habit_supervisor_phone_invalid)) } } else null,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                            )
                        }
                    }
                }
            }
        }
    )
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
            val fakeUserPreferences = io.github.darrindeyoung791.habitpulse.data.preferences.UserPreferences.getInstance(context)
            HabitViewModel(fakeRepository, fakeOnboardingPreferences, fakeUserPreferences)
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
    val estimatedMaxAnimationTime = 5 * 30L + 500L

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

    // Supervision contact state
    var supervisorEmails by remember { mutableStateOf<List<String>>(emptyList()) }
    var supervisorPhones by remember { mutableStateOf<List<String>>(emptyList()) }
    var showAddSupervisorDialog by remember { mutableStateOf(false) }
    var regionCode by remember { mutableStateOf("+86") }
    var showDuplicateEmailToast by remember { mutableStateOf(false) }
    var showDuplicatePhoneToast by remember { mutableStateOf(false) }
    var isSupervisorExpanded by remember { mutableStateOf(false) }
    var hasAddedSupervisorEmail by remember { mutableStateOf(false) }
    var hasAddedSupervisorPhone by remember { mutableStateOf(false) }

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
                reminderTimes = it.getReminderTimesList().sorted()
                notes = it.notes
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
    val hideKeyboardAndNavigateBack = navController?.let { rememberHideKeyboardAndNavigateBack(it) }

    // 显示最大长度 Toast
    if (showMaxLengthToast) {
        LaunchedEffect(Unit) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            android.widget.Toast.makeText(context, R.string.create_habit_max_length_hint, android.widget.Toast.LENGTH_SHORT).show()
            showMaxLengthToast = false
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

    // 错误状态标记
    var showHabitNameError by remember { mutableStateOf(false) }
    var showReminderTimeError by remember { mutableStateOf(false) }
    var showRepeatDaysError by remember { mutableStateOf(false) }

    // 验证必填项
    fun validateInputs(): Boolean {
        // 重置所有错误状态
        showHabitNameError = false
        showReminderTimeError = false
        showRepeatDaysError = false

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
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
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
                                    hideKeyboardAndNavigateBack?.invoke()
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
                                                notes = notes
                                            )?.copyWithRepeatDays(selectedRepeatDays.toList())
                                            ?.copyWithReminderTimes(reminderTimes)
                                            ?.copyWithSupervisorEmails(supervisorEmails)
                                            ?.copyWithSupervisorPhones(supervisorPhones)
                                        } else {
                                            // 新建模式：创建新习惯
                                            Habit(
                                                title = habitName,
                                                repeatCycle = repeatCycle,
                                                notes = notes
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
                .verticalScroll(rememberScrollState())
                .imeNestedScroll(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Repeat cycle selection + repeat days (merged to avoid double spacing)
            AnimatedCreationItem(
                index = 0,
                initialAnimationComplete = initialAnimationComplete
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy((-1).dp)
                    ) {
                    RepeatCycle.values().forEachIndexed { index, cycle ->
                        val isSelected = repeatCycle == cycle
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()

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

                    AnimatedVisibility(
                        visible = repeatCycle == RepeatCycle.WEEKLY,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val dayIndices = listOf(6, 0, 1, 2, 3, 4, 5)
                                val dayLabels = listOf(
                                    stringResource(id = R.string.create_habit_day_sunday),
                                    stringResource(id = R.string.create_habit_day_monday),
                                    stringResource(id = R.string.create_habit_day_tuesday),
                                    stringResource(id = R.string.create_habit_day_wednesday),
                                    stringResource(id = R.string.create_habit_day_thursday),
                                    stringResource(id = R.string.create_habit_day_friday),
                                    stringResource(id = R.string.create_habit_day_saturday)
                                )

                                dayIndices.forEachIndexed { displayPos, actualIndex ->
                                    FilterChip(
                                        selected = selectedRepeatDays.contains(actualIndex),
                                        onClick = {
                                            selectedRepeatDays = if (selectedRepeatDays.contains(actualIndex)) {
                                                selectedRepeatDays - actualIndex
                                            } else {
                                                selectedRepeatDays + actualIndex
                                            }
                                            showRepeatDaysError = false
                                        },
                                        label = {
                                            Text(
                                                text = dayLabels[displayPos],
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
            } // Close AnimatedCreationItem for repeat cycle + days

            // 2. Habit name input field
            AnimatedCreationItem(
                index = 1,
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
                index = 2,
                initialAnimationComplete = initialAnimationComplete
            ) {
                AddSection(
                    title = stringResource(id = R.string.create_habit_reminder_time_label),
                    addButtonLabelRes = R.string.create_habit_add_reminder_button,
                    onAddClick = {
                        currentTimePickerTime = java.time.LocalTime.now()
                        showTimePicker = true
                    },
                    itemCount = reminderTimes.size,
                    emptyTextRes = R.string.create_habit_no_reminder_set,
                    countTextRes = R.string.create_habit_reminder_set_count,
                    isListExpanded = isReminderExpanded,
                    onListExpandedChange = { isReminderExpanded = it },
                    showError = showReminderTimeError
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
                                    contentDescription = stringResource(R.string.accessibility_delete_reminder_time, time),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // 5. Supervisor section (merged email + phone)
            AnimatedCreationItem(
                index = 3,
                initialAnimationComplete = initialAnimationComplete
            ) {
                SupervisorSection(
                    supervisorEmails = supervisorEmails,
                    supervisorPhones = supervisorPhones,
                    isExpanded = isSupervisorExpanded,
                    onExpandedChange = { isSupervisorExpanded = it },
                    onAddClick = { showAddSupervisorDialog = true },
                    onDeleteEmail = { index ->
                        supervisorEmails = supervisorEmails.filterIndexed { i, _ -> i != index }
                    },
                    onDeletePhone = { index ->
                        supervisorPhones = supervisorPhones.filterIndexed { i, _ -> i != index }
                    }
                )
            }

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
                            reminderTimes = (reminderTimes + timeString).sorted()
                            showReminderTimeError = false
                        }
                        showTimePicker = false
                    }
                )
            }

            // 6. Notes section
            AnimatedCreationItem(
                index = 4,
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

            // Supervisor add dialog (tabbed: email + phone)
            if (showAddSupervisorDialog) {
                AddSupervisorDialog(
                    initialRegionCode = regionCode,
                    hasAddedEmail = hasAddedSupervisorEmail,
                    hasAddedPhone = hasAddedSupervisorPhone,
                    onDismiss = { showAddSupervisorDialog = false },
                    onAddEmail = { email ->
                        if (!supervisorEmails.contains(email)) {
                            supervisorEmails = supervisorEmails + email
                            hasAddedSupervisorEmail = true
                        } else {
                            showDuplicateEmailToast = true
                        }
                        showAddSupervisorDialog = false
                    },
                    onAddPhone = { phone ->
                        if (!supervisorPhones.contains(phone)) {
                            supervisorPhones = supervisorPhones + phone
                            hasAddedSupervisorPhone = true
                        } else {
                            showDuplicatePhoneToast = true
                        }
                        showAddSupervisorDialog = false
                    }
                )
            }
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

data class RegionCodeOption(val prefix: String, val displayNameRes: Int)

@Suppress("unused")
val REGION_CODES = listOf(
    RegionCodeOption("+86", R.string.region_cn),
    RegionCodeOption("+1", R.string.region_us_ca),
    RegionCodeOption("+44", R.string.region_uk),
    RegionCodeOption("+81", R.string.region_jp),
    RegionCodeOption("+82", R.string.region_kr),
    RegionCodeOption("+61", R.string.region_au),
    RegionCodeOption("+852", R.string.region_hk),
    RegionCodeOption("+886", R.string.region_tw),
    RegionCodeOption("+65", R.string.region_sg),
    RegionCodeOption("+49", R.string.region_de),
    RegionCodeOption("+33", R.string.region_fr),
    RegionCodeOption("+91", R.string.region_in),
    RegionCodeOption("+39", R.string.region_it),
    RegionCodeOption("+55", R.string.region_br),
    RegionCodeOption("+7", R.string.region_ru),
    RegionCodeOption("+34", R.string.region_es),
    RegionCodeOption("+31", R.string.region_nl),
    RegionCodeOption("+46", R.string.region_se),
    RegionCodeOption("+41", R.string.region_ch),
    RegionCodeOption("+47", R.string.region_no),
    RegionCodeOption("+45", R.string.region_dk),
    RegionCodeOption("+358", R.string.region_fi),
    RegionCodeOption("+48", R.string.region_pl),
    RegionCodeOption("+30", R.string.region_gr),
    RegionCodeOption("+60", R.string.region_my),
    RegionCodeOption("+63", R.string.region_ph),
    RegionCodeOption("+62", R.string.region_id),
    RegionCodeOption("+66", R.string.region_th),
    RegionCodeOption("+84", R.string.region_vn),
    RegionCodeOption("+977", R.string.region_np),
    RegionCodeOption("+94", R.string.region_lk),
    RegionCodeOption("+971", R.string.region_ae),
    RegionCodeOption("+966", R.string.region_sa),
    RegionCodeOption("+972", R.string.region_il),
    RegionCodeOption("+27", R.string.region_za),
    RegionCodeOption("+20", R.string.region_eg),
    RegionCodeOption("+234", R.string.region_ng),
    RegionCodeOption("+54", R.string.region_ar),
    RegionCodeOption("+56", R.string.region_cl),
    RegionCodeOption("+57", R.string.region_co),
    RegionCodeOption("+52", R.string.region_mx),
    RegionCodeOption("+64", R.string.region_nz),
)

/**
 * 统一的添加区域组件 - 标题 + 添加按钮(右侧) + 状态行 + 可展开列表
 */
@Composable
private fun AddSection(
    title: String,
    addButtonLabelRes: Int,
    onAddClick: () -> Unit,
    itemCount: Int,
    emptyTextRes: Int,
    countTextRes: Int,
    isListExpanded: Boolean,
    onListExpandedChange: (Boolean) -> Unit,
    showError: Boolean = false,
    extraInfo: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (showError) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header row: title on left + add button on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (showError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val cornerSize = animateDpAsState(
                    targetValue = if (isPressed) 24.dp else 8.dp,
                    label = "addButtonCorner"
                )

                val addLabel = stringResource(id = addButtonLabelRes)
                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) { onAddClick() },
                    shape = RoundedCornerShape(cornerSize.value),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = addLabel,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Extra info (e.g., SMS warning for phone section)
            if (extraInfo != null) {
                extraInfo()
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Status row: count/empty text + expand/collapse button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (itemCount == 0) {
                        stringResource(id = emptyTextRes)
                    } else {
                        stringResource(id = countTextRes, itemCount)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (itemCount > 0) {
                    TextButton(
                        onClick = { onListExpandedChange(!isListExpanded) }
                    ) {
                        Text(
                            text = if (isListExpanded) {
                                stringResource(id = R.string.create_habit_collapse_button)
                            } else {
                                stringResource(id = R.string.create_habit_expand_button)
                            }
                        )
                        Icon(
                            imageVector = if (isListExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                            contentDescription = if (isListExpanded) "收起" else "展开",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Expandable content list
            AnimatedVisibility(
                visible = isListExpanded && itemCount > 0,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                    Column(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        content()
                }
            }
        }
    }
}

/**
 * 监督人区域组件 - 合并显示邮箱和电话，带说明文字和展开/收起
 */
@Composable
private fun SupervisorSection(
    supervisorEmails: List<String>,
    supervisorPhones: List<String>,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onAddClick: () -> Unit,
    onDeleteEmail: (Int) -> Unit,
    onDeletePhone: (Int) -> Unit
) {
    val totalCount = supervisorEmails.size + supervisorPhones.size
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header row: title + add button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.create_habit_supervision_label),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val cornerSize = animateDpAsState(
                    targetValue = if (isPressed) 24.dp else 8.dp,
                    label = "addButtonCorner"
                )
                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) { onAddClick() },
                    shape = RoundedCornerShape(cornerSize.value),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = stringResource(R.string.create_habit_supervisor_add),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Status row: count + expand/collapse
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (totalCount == 0) {
                        stringResource(R.string.create_habit_no_supervisor)
                    } else {
                        stringResource(R.string.create_habit_supervisor_count, totalCount)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (totalCount > 0) {
                    TextButton(
                        onClick = { onExpandedChange(!isExpanded) }
                    ) {
                        Text(
                            text = if (isExpanded) {
                                stringResource(R.string.create_habit_collapse_button)
                            } else {
                                stringResource(R.string.create_habit_expand_button)
                            }
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Expandable content list
            AnimatedVisibility(
                visible = isExpanded && totalCount > 0,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    supervisorEmails.forEachIndexed { index, email ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Email,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = email,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { onDeleteEmail(index) }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = stringResource(R.string.accessibility_delete_contact, email),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    supervisorPhones.forEachIndexed { index, phone ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Phone,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = phone,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { onDeletePhone(index) }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = stringResource(R.string.accessibility_delete_contact, phone),
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

    override suspend fun incrementCompletionCountWithCompleted(id: java.util.UUID, completed: Boolean, timestamp: Long) {
        // No-op for preview/fake
    }

    override suspend fun undoSlotCompletion(id: java.util.UUID, completed: Boolean, timestamp: Long) {
        // No-op for preview/fake
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

    override suspend fun getCompletionByHabitIdDateAndSlot(habitId: java.util.UUID, date: String, slotTime: String): io.github.darrindeyoung791.habitpulse.data.model.HabitCompletion? = null
}