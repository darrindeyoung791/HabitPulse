package com.ddy.habitpulse

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.ddy.habitpulse.enums.RepeatCycle
import com.ddy.habitpulse.enums.SupervisionMethod
import com.ddy.habitpulse.ui.theme.HabitPulseTheme
import com.ddy.habitpulse.viewmodel.HabitViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HabitPulseTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val viewModel: HabitViewModel = viewModel()
    var showHabitSheet by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Modal bottom sheet state - configure to skip partially expanded state so it expands fully when shown
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
                        val intent = Intent(context, SettingsActivity::class.java)
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
            if (!showHabitSheet) { // Only show the FAB when the bottom sheet is not visible
                ExtendedFloatingActionButton(
                    text = { Text("新建习惯") },
                    icon = { Icon(Icons.Filled.Add, contentDescription = "Add") },
                    onClick = { 
                        isEditing = false
                        showHabitSheet = true 
                    }
                )
            }
        },
        content = { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                // Empty state when no habits exist
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
                    RoundedCornerShape(28.dp)         // Rounded during drag/animation (when closing)
                } else {
                    RoundedCornerShape(0.dp)          // Square when static (fully expanded)
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
                onSave = {
                    if (viewModel.isFormValid()) {
                        viewModel.saveHabit()
                        showHabitSheet = false
                        Toast.makeText(context, "习惯已保存", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "请填写所有必填项目", Toast.LENGTH_SHORT).show()
                    }
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
            text = "新建习惯",
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
                        // Not selected: Toned down button with grey background and smaller corner radius
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
                        // Not selected: Toned down button with grey background and smaller corner radius
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
                            text = if (viewModel.reminderTimes.isEmpty()) "未设置提醒时间" else "已设置 ${viewModel.reminderTimes.size} 个时间",
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
                                text = if (viewModel.reminderTimes.isEmpty()) "未设置提醒时间" else "已设置时间: ${viewModel.reminderTimes.first()}",
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
                    
                    // Input field for new supervisor with trailing button and validation
                    OutlinedTextField(
                        value = newSupervisorPhone,
                        onValueChange = { newSupervisorPhone = it },
                        label = { Text("输入电话号码") },
                        modifier = Modifier.fillMaxWidth(),
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
                    onValueChange = { viewModel.setNotes(it) },
                    label = { Text("备注信息，可换行") },
                    modifier = Modifier.fillMaxWidth(),
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
                        onSave()
                    } else {
                        Toast.makeText(context, "请填写所有必填项目", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
    
    // Time picker dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState()
        val confirmEnabled = remember { mutableStateOf(true) }
        
        // Use the original timePickerState defined above
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("选择时间") },
            text = {
                TimePicker(
                    state = timePickerState,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val formattedTime = "${timePickerState.hour.toString().padStart(2, '0')}:${timePickerState.minute.toString().padStart(2, '0')}"
                        
                        if (viewModel.repeatCycle == RepeatCycle.DAILY) {
                            // For daily, just add the time to the list
                            if (!viewModel.reminderTimes.contains(formattedTime)) {
                                viewModel.addReminderTime(formattedTime)
                            }
                        } else if (viewModel.repeatCycle == RepeatCycle.WEEKLY) {
                            // For weekly, replace any existing time with the new one (only one time for all selected days)
                            if (!viewModel.reminderTimes.contains(formattedTime)) {
                                viewModel.setReminderTimes(listOf(formattedTime))
                            } else {
                                // If this time already exists, we don't add it again
                                Toast.makeText(context, "该时间已存在", Toast.LENGTH_SHORT).show()
                            }
                        }
                        
                        showTimePicker = false
                    },
                    enabled = confirmEnabled.value
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

// Data classes for options
data class RepeatCycleOption(val value: RepeatCycle, val label: String)
data class SupervisionMethodOption(val value: SupervisionMethod, val label: String)

val repeatCycleOptions = listOf(
    RepeatCycleOption(RepeatCycle.DAILY, "每日"),
    RepeatCycleOption(RepeatCycle.WEEKLY, "每周")
)

val supervisionMethodOptions = listOf(
    SupervisionMethodOption(SupervisionMethod.LOCAL_NOTIFICATION_ONLY, "不监督，仅本地通知我"),
    SupervisionMethodOption(SupervisionMethod.SMS_REPORTING, "短信汇报")
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
    
    // Check if it contains only valid phone number characters: digits, spaces, dashes, parentheses, plus sign
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
            onSave = {},
            onCancel = {}
        )
    }
}