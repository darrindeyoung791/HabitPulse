package com.ddy.habitpulse

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.TimeInput
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import com.ddy.habitpulse.db.Habit
import com.ddy.habitpulse.enums.RepeatCycle
import com.ddy.habitpulse.enums.SupervisionMethod
import com.ddy.habitpulse.ui.theme.HabitPulseTheme
import com.ddy.habitpulse.viewmodel.HabitListViewModel
import com.ddy.habitpulse.viewmodel.HabitViewModel
import java.util.Calendar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HabitPulseTheme {
                val habitListViewModel: HabitListViewModel = viewModel { HabitListViewModel(this@MainActivity.application) }
                MainScreen(habitListViewModel = habitListViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    habitListViewModel: HabitListViewModel,
    modifier: Modifier = Modifier
) {
    val viewModel: HabitViewModel = viewModel()
    var showHabitSheet by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Modal bottom sheet state - configure to skip partially expanded state so it expands fully
    // when shown
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("HabitPulse") },
                navigationIcon = {
                    IconButton(onClick = { /* Handle navigation icon */ }) {
                        Icon(
                            imageVector = Icons.Filled.CalendarMonth,
                            contentDescription = "Calendar"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val intent = Intent(
                            context,
                            SettingsActivity::class.java)
                        context.startActivity(intent)
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("新建习惯") },
                icon = { Icon(Icons.Filled.Add, contentDescription = "Add") },
                onClick = {
                    isEditing = false
                    showHabitSheet = true
                }
            )
        },
        content = { innerPadding ->
            val habits = habitListViewModel.habits.collectAsState().value
            
            if (habits.isEmpty()) {
                // Empty state when no habits exist
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LibraryAdd,
                            contentDescription = "No habits",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "暂无习惯",
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = { 
                                isEditing = false
                                showHabitSheet = true 
                            }
                        ) {
                            Text(
                                text = "去新建一个",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            } else {
                // Display habit cards
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(habits) { habit ->
                        HabitCard(
                            habit = habit,
                            onEditClick = {
                                // Set up editing for this habit
                                viewModel.setTitle(habit.title)
                                viewModel.setRepeatCycle(habit.repeatCycle)
                                viewModel.setSelectedDay(0, false) // Clear selections first
                                habit.repeatDays.forEach { day ->
                                    viewModel.setSelectedDay(day, true)
                                } 
                                viewModel.setReminderTimes(habit.reminderTimes)
                                viewModel.setNotes(habit.notes)
                                viewModel.setSupervisionMethod(habit.supervisionMethod)
                                viewModel.setSupervisorPhoneNumbers(habit.supervisorPhoneNumbers)
                                
                                isEditing = true
                                showHabitSheet = true
                                viewModel.loadHabitForEdit(habit.id) // Load habit data for editing
                            },
                            onDeleteClick = {
                                habitListViewModel.deleteHabitById(habit.id)
                            },
                            onToggleCompleted = {
                                habitListViewModel.updateHabitCompleted(habit.id, !habit.completed)
                            }
                        )
                    }
                }
            }
        }
    )
    
    // Modal bottom sheet for adding/editing habits
    if (showHabitSheet) {
        val sheetShape by remember(
            bottomSheetState.currentValue,
            bottomSheetState.targetValue,
            bottomSheetState.isVisible
        ) {
            derivedStateOf {
                // With skipPartiallyExpanded = true, the sheet only goes between expanded and hidden
                // We can still detect dragging by checking if current != target
                val isDraggingOrAnimating =
                    bottomSheetState.targetValue != bottomSheetState.currentValue

                if (isDraggingOrAnimating) {
                    RoundedCornerShape(28.dp) // Rounded during drag/animation (when closing)
                } else {
                    RoundedCornerShape(0.dp)  // Square when static (fully expanded)
                }
            }
        }
        
        ModalBottomSheet(
            onDismissRequest = { 
                showHabitSheet = false 
                viewModel.resetForm()
            },
            sheetState = bottomSheetState,
            shape = sheetShape,
            dragHandle = {
                // 自定义指示条，减小高度以避免遮挡摄像头
                Box(
                    modifier = Modifier
                        .padding(top = 48.dp, bottom = 4.dp) // 48dp 不一定适用于每个设备
                        .width(32.dp)
                        .height(4.dp) // 减小高度
                        .background(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
                        )
                )
            }
        ) {
            HabitFormContent(
                viewModel = viewModel,
                habitListViewModel = habitListViewModel,
                isEditing = isEditing,
                onSave = {
                    showHabitSheet = false
                    // First save the habit through the HabitViewModel
                    viewModel.saveHabit()
                    // Use a small delay to ensure the database transaction completes before refreshing
                    // This helps prevent race conditions where the list is refreshed before the habit is saved
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        habitListViewModel.loadHabits() // Refresh the list after save operation
                    }, 100) // 100ms delay should be sufficient
                    Toast.makeText(
                        context,
                        "习惯已保存",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onCancel = {
                    showHabitSheet = false
                    viewModel.resetForm()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitFormContent(
    viewModel: HabitViewModel,
    habitListViewModel: HabitListViewModel? = null,
    isEditing: Boolean = false,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    
    // State for showing time picker
    var showTimePicker by remember { mutableStateOf(false) }
    
    // For SMS reporting supervisor management
    var newSupervisorPhone by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = if (isEditing) "编辑习惯" else "新建习惯",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
        
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true)
        ) {
            item {
                // Repeat Cycle Selection with segmented buttons (below title)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Daily button
                    if (viewModel.repeatCycle == RepeatCycle.DAILY) {
                        // Selected: Filled button with smaller corner radius
                        Button(
                            onClick = { viewModel.setRepeatCycle(RepeatCycle.DAILY) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp) // Smaller corner radius
                        ) {
                            Text("每日")
                        }
                    } else {
                        // Not selected: Toned down button with grey background and smaller corner
                        // radius
                        Button(
                            onClick = { viewModel.setRepeatCycle(RepeatCycle.DAILY) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(16.dp) // Smaller corner radius
                        ) {
                            Text("每日")
                        }
                    }
                    
                    // Weekly button  
                    if (viewModel.repeatCycle == RepeatCycle.WEEKLY) {
                        // Selected: Filled button with smaller corner radius
                        Button(
                            onClick = { viewModel.setRepeatCycle(RepeatCycle.WEEKLY) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp) // Smaller corner radius
                        ) {
                            Text("每周")
                        }
                    } else {
                        // Not selected: Toned down button with grey background and smaller corner
                        // radius
                        Button(
                            onClick = { viewModel.setRepeatCycle(RepeatCycle.WEEKLY) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(16.dp) // Smaller corner radius
                        ) {
                            Text("每周")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                // Habit Title
                OutlinedTextField(
                    value = viewModel.title,
                    onValueChange = { viewModel.setTitle(it) },
                    label = { Text("习惯标题") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Daily repeat cycle - Multiple reminder times
                if (viewModel.repeatCycle == RepeatCycle.DAILY) {
                    Text(
                        text = "提醒时间",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (viewModel.reminderTimes.isEmpty()){
                                "未设置提醒时间"
                            } else {
                                "已设置 ${viewModel.reminderTimes.size} 个时间"
                            },
                            modifier = Modifier.weight(1f)
                        )
                        
                        OutlinedButton(
                            onClick = { 
                                showTimePicker = true 
                            }
                        ) {
                            Text("添加提醒时间")
                        }
                    }
                    
                    // Display selected times with delete option
                    if (viewModel.reminderTimes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        viewModel.reminderTimes.forEach { time ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = time,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.removeReminderTime(time) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "删除时间"
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Weekly repeat cycle - Day selection and time handling
                if (viewModel.repeatCycle == RepeatCycle.WEEKLY) {
                    Text(
                        text = "选择日期",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // Week day selection using checkboxes
                    val weekDays = listOf(
                        "周一" to 0,
                        "周二" to 1, 
                        "周三" to 2,
                        "周四" to 3,
                        "周五" to 4,
                        "周六" to 5,
                        "周日" to 6
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        weekDays.forEach { (dayName, dayIndex) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Checkbox(
                                    checked = viewModel.selectedDays.contains(dayIndex),
                                    onCheckedChange = { isChecked ->
                                        viewModel.setSelectedDay(dayIndex, isChecked)
                                    }
                                )
                                Text(
                                    text = dayName,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    
                    // Show time selection for each selected day
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (viewModel.selectedDays.isNotEmpty()) {
                        Text(
                            text = "每日提醒时间",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (viewModel.reminderTimes.isEmpty()){
                                    "未设置提醒时间"
                                } else {
                                    "已设置时间: ${viewModel.reminderTimes.first()}"
                                },
                                modifier = Modifier.weight(1f)
                            )
                            
                            OutlinedButton(
                                onClick = { 
                                    showTimePicker = true 
                                }
                            ) {
                                Text("设置提醒时间")
                            }
                        }
                        
                        // Display the selected time
                        if (viewModel.reminderTimes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = viewModel.reminderTimes.first(),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { 
                                        viewModel.setReminderTimes(emptyList()) 
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "删除时间"
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Move Notes field to the bottom, so removing it from here
                // This part will be replaced in the new location
                
                Text(
                    text = "监督方式",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                var supervisionMethodExpanded by remember { mutableStateOf(false) }
                
                // Dropdown for supervision method
                ExposedDropdownMenuBox(
                    expanded = supervisionMethodExpanded,
                    onExpandedChange = { supervisionMethodExpanded = !supervisionMethodExpanded }
                ) {
                    // This is the text field that triggers the dropdown
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        value = getSupervisionMethodLabel(viewModel.supervisionMethod),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("选择监督方式") }
                    )
                    
                    // This is the actual dropdown menu
                    ExposedDropdownMenu(
                        expanded = supervisionMethodExpanded,
                        onDismissRequest = { supervisionMethodExpanded = false }
                    ) {
                        supervisionMethodOptions.forEach { method ->
                            DropdownMenuItem(
                                text = { Text(method.label) },
                                onClick = {
                                    viewModel.setSupervisionMethod(method.value)
                                    supervisionMethodExpanded = false
                                },
                                contentPadding = PaddingValues(16.dp)
                            )
                        }
                    }
                }
                
                // Show supervisor phone input when SMS reporting is selected
                if (viewModel.supervisionMethod == SupervisionMethod.SMS_REPORTING) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "监督人电话号码",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "添加一位或多位监督人的电话号码",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // Validate phone number format
                    val phoneValid by remember(newSupervisorPhone) {
                        derivedStateOf {
                            newSupervisorPhone.isBlank() || isPhoneNumberValid(newSupervisorPhone)
                        }
                    }
                    
                    val duplicatePhone by remember(newSupervisorPhone, viewModel.supervisorPhoneNumbers) {
                        derivedStateOf {
                            newSupervisorPhone.isNotBlank() && viewModel.supervisorPhoneNumbers.contains(newSupervisorPhone)
                        }
                    }
                    
                    // Display existing supervisors with delete option
                    if (viewModel.supervisorPhoneNumbers.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        viewModel.supervisorPhoneNumbers.forEach { phone ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = phone,
                                    modifier = Modifier
                                        .weight(1f)
                                        .align(Alignment.CenterVertically)
                                )
                                IconButton(
                                    onClick = { viewModel.removeSupervisorPhoneNumber(phone) },
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "删除监督人"
                                    )
                                }
                            }
                        }
                    }
                    
                    // Input field for new supervisor with trailing button and validation
                    OutlinedTextField(
                        value = newSupervisorPhone,
                        onValueChange = { 
                            // Limit to 20 characters and remove newlines
                            val cleanValue = it.replace("\n", "").take(20)
                            newSupervisorPhone = cleanValue 
                        },
                        label = { Text("输入电话号码") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        maxLines = 1,
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    if (newSupervisorPhone.isNotBlank() && phoneValid && !duplicatePhone) {
                                        viewModel.addSupervisorPhoneNumber(newSupervisorPhone)
                                        newSupervisorPhone = ""
                                    }
                                },
                                enabled = newSupervisorPhone.isNotBlank() && phoneValid && !duplicatePhone
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "添加"
                                )
                            }
                        },
                        supportingText = {
                            if (!phoneValid) {
                                Text(
                                    text = "请输入有效的电话号码",
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else if (duplicatePhone) {
                                Text(
                                    text = "该号码已存在",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        isError = !phoneValid || duplicatePhone
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "备注信息 ",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Notes field (moved to the bottom)
                OutlinedTextField(
                    value = viewModel.notes,
                    onValueChange = { 
                        // Limit to 2000 characters but allow newlines for notes
                        if (it.length <= 2000) {
                            viewModel.setNotes(it)
                        }
                    },
                    label = { Text("备注信息，可换行") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 10, // Allow multiple lines for notes
                )
                
                // Add some bottom padding to ensure content is not hidden
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
        
        // Extended FAB for Save button
        Box(
            modifier = Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.BottomEnd
        ) {
            ExtendedFloatingActionButton(
                text = { Text("保存") },
                icon = { Icon(Icons.Filled.Save, contentDescription = "保存") },
                onClick = {
                    if (viewModel.isFormValid()) {
                        onSave() // This will trigger the save in the parent composable
                    } else {
                        Toast.makeText(
                            context,
                            "请填写所有必填项目",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
        }
    }
    
    // Time picker dialog
    if (showTimePicker) {
        val calendar = Calendar.getInstance()
        val timePickerState = rememberTimePickerState(
            initialHour = calendar.get(Calendar.HOUR_OF_DAY),
            initialMinute = calendar.get(Calendar.MINUTE),
            is24Hour = false // 使用12小时制
        )

        TimePickerDialog(
            state = timePickerState,
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val formattedTime = "${timePickerState.hour.toString().padStart(2, '0')}:${timePickerState.minute.toString().padStart(2, '0')}"

                        if (viewModel.repeatCycle == RepeatCycle.DAILY) {
                            if (!viewModel.reminderTimes.contains(formattedTime)) {
                                viewModel.addReminderTime(formattedTime)
                            } else {
                                Toast.makeText(context, "该时间已存在", Toast.LENGTH_SHORT).show()
                            }
                        } else if (viewModel.repeatCycle == RepeatCycle.WEEKLY) {
                            if (!viewModel.reminderTimes.contains(formattedTime)) {
                                viewModel.setReminderTimes(listOf(formattedTime))
                            } else {
                                Toast.makeText(context, "该时间已存在", Toast.LENGTH_SHORT).show()
                            }
                        }
                        showTimePicker = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showTimePicker = false }
                ) {
                    Text("取消")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    state: TimePickerState,
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
    title: String = "选择时间",
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surface,
) {
    var showTimeInput by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        ),
        modifier = Modifier
            .widthIn(max = 360.dp)
            .padding(16.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge
                )
                IconButton(onClick = { showTimeInput = !showTimeInput }) {
                    Icon(
                        imageVector = if (showTimeInput) Icons.Outlined.Schedule else Icons.Outlined.Keyboard,
                        contentDescription = if (showTimeInput) "切换到表盘" else "切换到键盘输入"
                    )
                }
            }
        },
        text = {
            Box(contentAlignment = Alignment.Center) {
                if (showTimeInput) {
                    TimeInput(state = state)
                } else {
                    TimePicker(state = state)
                }
            }
        },
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        containerColor = containerColor
    )
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HabitCard(
    habit: Habit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onToggleCompleted: () -> Unit
) {
    var contextMenuVisible by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onEditClick() }, // Click to edit
                onLongClick = { contextMenuVisible = true } // Long press to show context menu
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Habit title (full width)
            Text(
                text = habit.title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Completion count indicator below title
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "完成次数",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "已完成 ${habit.completionCount} 次",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Repeat cycle information
            val repeatInfo = when (habit.repeatCycle) {
                RepeatCycle.DAILY -> {
                    "重复: 每日 • ${habit.reminderTimes.joinToString(", ")}"
                }
                RepeatCycle.WEEKLY -> {
                    val days = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
                    val selectedDaysText = habit.repeatDays.map { days[it] }.joinToString(", ")
                    "重复: $selectedDaysText • ${habit.reminderTimes.firstOrNull() ?: ""}"
                }
            }
            
            Text(
                text = repeatInfo,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Supervision method
            val supervisionText = when (habit.supervisionMethod) {
                SupervisionMethod.LOCAL_NOTIFICATION_ONLY -> "监督: 本地通知"
                SupervisionMethod.SMS_REPORTING -> "监督: 短信汇报"
            }
            
            Text(
                text = supervisionText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Notes (if any)
            if (habit.notes.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Note,
                        contentDescription = "备注",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = habit.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
            
            // Phone numbers (if any)
            if (habit.supervisionMethod == SupervisionMethod.SMS_REPORTING && habit.supervisorPhoneNumbers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Phone,
                        contentDescription = "电话",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = habit.supervisorPhoneNumbers.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Completion button at the bottom (after all other content)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onToggleCompleted
                ) {
                    Icon(
                        imageVector = if (habit.completed) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = if (habit.completed) "已完成" else "完成",
                        tint = if (habit.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // Context menu - shown when long press occurs
        DropdownMenu(
            expanded = contextMenuVisible,
            onDismissRequest = { contextMenuVisible = false }
        ) {
            DropdownMenuItem(
                text = { Text("编辑") },
                onClick = {
                    onEditClick()
                    contextMenuVisible = false
                }
            )
            DropdownMenuItem(
                text = { Text("删除") },
                onClick = {
                    onDeleteClick()
                    contextMenuVisible = false
                }
            )
        }
    }
}


// Data classes for options
data class RepeatCycleOption(val value: RepeatCycle, val label: String)
data class SupervisionMethodOption(val value: SupervisionMethod, val label: String)

val repeatCycleOptions = listOf(
    RepeatCycleOption(RepeatCycle.DAILY, "每日"),
    RepeatCycleOption(RepeatCycle.WEEKLY, "每周")
)

val supervisionMethodOptions = listOf(
    SupervisionMethodOption(
        SupervisionMethod.LOCAL_NOTIFICATION_ONLY,
        "不监督，仅本地通知我"
    ),
    SupervisionMethodOption(
        SupervisionMethod.SMS_REPORTING,
        "短信汇报"
    )
)

fun getRepeatCycleLabel(repeatCycle: RepeatCycle): String {
    return when (repeatCycle) {
        RepeatCycle.DAILY -> "每日"
        RepeatCycle.WEEKLY -> "每周"
    }
}

fun getSupervisionMethodLabel(supervisionMethod: SupervisionMethod): String {
    return when (supervisionMethod) {
        SupervisionMethod.LOCAL_NOTIFICATION_ONLY -> "不监督，仅本地通知我"
        SupervisionMethod.SMS_REPORTING -> "短信汇报"
    }
}

fun isPhoneNumberValid(phone: String): Boolean {
    // Remove any whitespace
    val cleanPhone = phone.trim()
    
    // Basic check: non-empty and has reasonable length (10-15 digits)
    if (cleanPhone.isEmpty() || cleanPhone.length < 10 || cleanPhone.length > 15) {
        return false
    }
    
    // Check if it contains only valid phone number characters: digits, spaces, dashes, parentheses,
    // plus sign
    val validPhonePattern = Regex("^[0-9+\\-()\\s]+$")
    if (!validPhonePattern.matches(cleanPhone)) {
        return false
    }
    
    // Check if it has at least 10 digits after removing non-digit characters
    val digitsOnly = cleanPhone.filter { it.isDigit() }
    return digitsOnly.length >= 10 && digitsOnly.length <= 15
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    HabitPulseTheme {
        val viewModel: HabitViewModel = viewModel()
        HabitFormContent(
            viewModel = viewModel,
            isEditing = false,
            onSave = {},
            onCancel = {}
        )
    }
}