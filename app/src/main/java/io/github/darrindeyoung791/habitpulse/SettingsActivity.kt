package io.github.darrindeyoung791.habitpulse

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.darrindeyoung791.habitpulse.data.model.Habit
import io.github.darrindeyoung791.habitpulse.data.model.RepeatCycle
import io.github.darrindeyoung791.habitpulse.data.model.SupervisionMethod
import io.github.darrindeyoung791.habitpulse.data.preferences.UserPreferences
import io.github.darrindeyoung791.habitpulse.data.repository.HabitRepository
import io.github.darrindeyoung791.habitpulse.ui.theme.HabitPulseTheme
import io.github.darrindeyoung791.habitpulse.utils.AccessibilityUtils
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.UUID

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable edge-to-edge display - system bar colors handled by HabitPulseTheme
        enableEdgeToEdge()
        setContent {
            HabitPulseTheme {
                SettingsScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userPreferences = remember { UserPreferences.getInstance(context) }

    // 收集开屏广告设置状态
    val showSplashAd by userPreferences.showSplashAdFlow.collectAsStateWithLifecycle(initialValue = false)

    // Check if TalkBack is enabled - poll every second to react to status changes
    var isTalkBackEnabled by remember { mutableStateOf(AccessibilityUtils.isTalkBackEnabled(context)) }
    
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            isTalkBackEnabled = AccessibilityUtils.isTalkBackEnabled(context)
        }
    }

    val versionName = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val version = packageInfo.versionName ?: "1.0.0"
            val isDebug = context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
            val buildType = if (isDebug) " (Debug)" else " (Release)"
            "$version$buildType"
        } catch (e: Exception) {
            "1.0.0"
        }
    }
    
    // Debug: track version tap count for sample data
    var versionTapCount by remember { mutableStateOf(0) }
    var versionTapStartTime by remember { mutableStateOf(0L) }
    var showSampleDataDialog by remember { mutableStateOf(false) }
    var showSuccessMessage by remember { mutableStateOf(false) }

    val TAP_TIME_WINDOW = 10_000L // 10 seconds
    val TAP_COUNT_THRESHOLD = 5
    
    // Dialog for adding sample data
    if (showSampleDataDialog) {
        AlertDialog(
            onDismissRequest = { showSampleDataDialog = false },
            title = {
                Text(text = stringResource(id = R.string.debug_add_sample_data_dialog_title))
            },
            text = {
                Text(text = stringResource(id = R.string.debug_add_sample_data_dialog_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSampleDataDialog = false
                        // Generate and insert sample data
                        scope.launch {
                            val database = (context.applicationContext as HabitPulseApplication).database
                            val repository = HabitRepository(database.habitDao())
                            generateSampleHabits().forEach { habit ->
                                repository.insertHabit(habit)
                            }
                            showSuccessMessage = true
                            // Show toast message
                            android.widget.Toast.makeText(
                                context,
                                context.getString(R.string.debug_sample_data_added),
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                ) {
                    Text(text = stringResource(id = R.string.debug_add_sample_data_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSampleDataDialog = false }) {
                    Text(text = stringResource(id = R.string.debug_add_sample_data_no))
                }
            }
        )
    }
    
    // Success message
    if (showSuccessMessage) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(2000)
            showSuccessMessage = false
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        (context as? ComponentActivity)?.finish()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.settings_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 支持部分
            item {
                // Section header
                Text(
                    text = stringResource(id = R.string.settings_support_habitpulse, stringResource(id = R.string.app_name)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                
                // 支持说明文字
                Text(
                    text = stringResource(id = R.string.settings_support_habitpulse_description, stringResource(id = R.string.app_name)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                // Support HabitPulse switch
                SettingsSwitchItem(
                    headline = stringResource(id = R.string.settings_support_habitpulse_switch),
                    supportingText = stringResource(id = R.string.settings_support_habitpulse_switch_description, stringResource(id = R.string.app_name)),
                    checked = showSplashAd && !isTalkBackEnabled,
                    enabled = !isTalkBackEnabled,
                    onCheckedChange = { isChecked ->
                        if (isChecked && isTalkBackEnabled) {
                            // Show toast when trying to enable splash ad with TalkBack on
                            android.widget.Toast.makeText(
                                context,
                                context.getString(R.string.accessibility_talkback_splash_ad_disabled),
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            scope.launch {
                                userPreferences.setShowSplashAd(isChecked)
                            }
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.FavoriteBorder,
                            contentDescription = null
                        )
                    }
                )
            }
            
            // 关于部分
            item {
                // Section header
                Text(
                    text = stringResource(id = R.string.settings_about_section),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                )

                // Privacy notice
                val appName = stringResource(id = R.string.app_name)
                Text(
                    text = stringResource(id = R.string.settings_privacy_notice, appName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // About section
                SettingsListItem(
                    headline = stringResource(id = R.string.settings_app_version_label),
                    supportingText = versionName,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - versionTapStartTime > TAP_TIME_WINDOW) {
                            // Reset if outside time window
                            versionTapCount = 1
                            versionTapStartTime = currentTime
                        } else {
                            versionTapCount++
                            if (versionTapCount >= TAP_COUNT_THRESHOLD) {
                                showSampleDataDialog = true
                                versionTapCount = 0
                                versionTapStartTime = 0
                            }
                        }
                    }
                )

                SettingsListItem(
                    headline = stringResource(id = R.string.settings_developer),
                    supportingText = stringResource(id = R.string.settings_developer_name),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null
                        )
                    }
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    TextButton(
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                intent.data = Uri.fromParts("package", context.packageName, null)
                                context.startActivity(intent)
                            } catch (e: ActivityNotFoundException) {
                                // Fallback for some devices
                                val intent = Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
                                context.startActivity(intent)
                            }
                        },
                        contentPadding = PaddingValues(start = 0.dp, end = 16.dp),
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text(
                            text = stringResource(id = R.string.settings_app_info_button, appName),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    TextButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/darrindeyoung791/HabitPulse"))
                            context.startActivity(intent)
                        },
                        contentPadding = PaddingValues(start = 0.dp, end = 16.dp),
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text(
                            text = stringResource(id = R.string.settings_github_button),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSwitchItem(
    headline: String,
    supportingText: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    leadingIcon: @Composable () -> Unit,
    enabled: Boolean = true
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = { if (enabled) onCheckedChange(!checked) },
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                leadingIcon()
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = headline,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    }
}

@Composable
fun SettingsListItem(
    headline: String,
    supportingText: String,
    leadingIcon: @Composable () -> Unit,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                leadingIcon()
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = headline,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Generate 20 sample habits for debugging purposes
 * Includes varied titles, reminder times, supervision methods, and notes
 * Mix of Chinese and English content
 * Random completion counts for realistic data
 * Some habits have super long notes (~1000 chars) and many reminders (~10) for UI testing
 */
private fun generateSampleHabits(): List<Habit> {
    val random = kotlin.random.Random(System.currentTimeMillis())
    
    // Generate ~1000 character long note for testing
    fun generateLongNote(): String {
        val baseText = "这是一段测试用的超长备注文本，用于检验 UI 在显示大量文字时的表现。"
        val repeatedText = baseText.repeat(15)
        return repeatedText + "\n\n此外，还需要注意以下几点：\n" +
                "1. 每天保持足够的饮水量\n" +
                "2. 注意饮食均衡\n" +
                "3. 保证充足的睡眠\n" +
                "4. 定期进行体育锻炼\n" +
                "5. 保持良好的心态\n" +
                "6. 避免过度使用电子设备\n" +
                "7. 注意用眼卫生\n" +
                "8. 定期体检\n" +
                "9. 养成良好的生活习惯\n" +
                "10. 坚持就是胜利！\n\n" +
                "这段文字的目的是测试界面在显示大量文本时的滚动性能和视觉效果。" +
                "请确保文字能够正常换行，不会出现溢出或截断的问题。" +
                "同时也测试卡片高度是否能够自适应内容变化。"
    }
    
    // Generate ~10 reminder times for testing
    fun generateManyReminders(): String {
        val times = listOf("06:00", "07:00", "08:00", "09:00", "10:00", 
                          "11:00", "12:00", "13:00", "14:00", "15:00",
                          "16:00", "17:00", "18:00", "19:00", "20:00")
        return JSONArray().apply {
            times.take(random.nextInt(8, 12)).forEach { put(it) }
        }.toString()
    }
    
    val longNote = generateLongNote()
    
    val sampleHabits = listOf(
        // Chinese habits - with super long note for UI testing
        Habit(
            title = "每天喝水",
            repeatCycle = RepeatCycle.DAILY,
            reminderTimes = JSONArray().apply { put("08:00"); put("12:00"); put("18:00") }.toString(),
            notes = longNote, // ~1000 character note
            supervisionMethod = SupervisionMethod.EMAIL,
            supervisorEmails = JSONArray().apply { put("health@example.com") }.toString(),
            completionCount = random.nextInt(0, 100),
            createdDate = System.currentTimeMillis() - random.nextInt(0, 30) * 24 * 60 * 60 * 1000L
        ),
        // Chinese habit - with many reminders for UI testing
        Habit(
            title = "晨跑锻炼",
            repeatCycle = RepeatCycle.WEEKLY,
            repeatDays = JSONArray().apply { put(1); put(3); put(5) }.toString(),
            reminderTimes = generateManyReminders(), // ~10 reminders (generated fresh)
            notes = "跑步前记得热身\n跑完后要拉伸\n注意呼吸节奏\n选择合适的跑鞋\n循序渐进增加距离",
            supervisionMethod = SupervisionMethod.SMS,
            supervisorPhones = JSONArray().apply { put("+8613800138000") }.toString(),
            completionCount = random.nextInt(0, 100),
            createdDate = System.currentTimeMillis() - random.nextInt(0, 30) * 24 * 60 * 60 * 1000L
        ),
        Habit(
            title = "阅读书籍",
            repeatCycle = RepeatCycle.DAILY,
            reminderTimes = JSONArray().apply { put("21:00") }.toString(),
            notes = "每天至少读 30 分钟\n记录读书笔记\n分享读书心得",
            supervisionMethod = SupervisionMethod.NONE,
            completionCount = random.nextInt(0, 100),
            createdDate = System.currentTimeMillis() - random.nextInt(0, 30) * 24 * 60 * 60 * 1000L
        ),
        // Another long note habit
        Habit(
            title = "冥想练习",
            repeatCycle = RepeatCycle.DAILY,
            reminderTimes = JSONArray().apply { put("07:00"); put("22:00") }.toString(),
            notes = longNote, // ~1000 character note
            supervisionMethod = SupervisionMethod.EMAIL,
            supervisorEmails = JSONArray().apply { put("mindfulness@example.com") }.toString(),
            completionCount = random.nextInt(0, 100),
            createdDate = System.currentTimeMillis() - random.nextInt(0, 30) * 24 * 60 * 60 * 1000L
        ),
        // Another many reminders habit
        Habit(
            title = "学习编程",
            repeatCycle = RepeatCycle.WEEKLY,
            repeatDays = JSONArray().apply { put(2); put(4); put(6) }.toString(),
            reminderTimes = generateManyReminders(), // ~10 reminders (generated fresh)
            notes = "完成一个小型项目\n复习基础知识\n练习算法题\n阅读技术文档\n参与开源项目",
            supervisionMethod = SupervisionMethod.SMS,
            supervisorPhones = JSONArray().apply { put("+8613900139000") }.toString(),
            completionCount = random.nextInt(0, 100),
            createdDate = System.currentTimeMillis() - random.nextInt(0, 30) * 24 * 60 * 60 * 1000L
        ),
        Habit(
            title = "早睡早起",
            repeatCycle = RepeatCycle.DAILY,
            reminderTimes = JSONArray().apply { put("22:30"); put("06:30") }.toString(),
            supervisionMethod = SupervisionMethod.NONE,
            completionCount = random.nextInt(0, 100),
            createdDate = System.currentTimeMillis() - random.nextInt(0, 30) * 24 * 60 * 60 * 1000L
        ),
        Habit(
            title = "健康饮食",
            repeatCycle = RepeatCycle.DAILY,
            reminderTimes = JSONArray().apply { put("08:00"); put("12:00"); put("18:00") }.toString(),
            notes = "少油少盐\n多吃蔬菜水果\n控制糖分摄入\n适量蛋白质\n多喝水",
            supervisionMethod = SupervisionMethod.EMAIL,
            supervisorEmails = JSONArray().apply { put("diet@example.com"); put("nutrition@example.com") }.toString(),
            completionCount = random.nextInt(0, 100),
            createdDate = System.currentTimeMillis() - random.nextInt(0, 30) * 24 * 60 * 60 * 1000L
        ),
        Habit(
            title = "瑜伽拉伸",
            repeatCycle = RepeatCycle.WEEKLY,
            repeatDays = JSONArray().apply { put(0); put(2); put(4); put(6) }.toString(),
            reminderTimes = JSONArray().apply { put("20:00") }.toString(),
            supervisionMethod = SupervisionMethod.NONE,
            completionCount = random.nextInt(0, 100),
            createdDate = System.currentTimeMillis() - random.nextInt(0, 30) * 24 * 60 * 60 * 1000L
        ),
        // Long note for testing
        Habit(
            title = "写日记",
            repeatCycle = RepeatCycle.DAILY,
            reminderTimes = JSONArray().apply { put("23:00") }.toString(),
            notes = longNote, // ~1000 character note
            supervisionMethod = SupervisionMethod.EMAIL,
            supervisorEmails = JSONArray().apply { put("diary@example.com") }.toString(),
            completionCount = random.nextInt(0, 100),
            createdDate = System.currentTimeMillis() - random.nextInt(0, 30) * 24 * 60 * 60 * 1000L
        ),
        Habit(
            title = "戒烟",
            repeatCycle = RepeatCycle.DAILY,
            reminderTimes = JSONArray().apply { put("09:00") }.toString(),
            notes = "坚持就是胜利\n想想健康的重要性\n避免诱因\n寻找替代方法",
            supervisionMethod = SupervisionMethod.SMS,
            supervisorPhones = JSONArray().apply { put("+8613700137000"); put("+8613600136000") }.toString(),
            completionCount = random.nextInt(0, 100),
            createdDate = System.currentTimeMillis() - random.nextInt(0, 30) * 24 * 60 * 60 * 1000L
        ),
        
        // English habits - with super long note for UI testing
        Habit(
            title = "Drink Water",
            repeatCycle = RepeatCycle.DAILY,
            reminderTimes = JSONArray().apply { put("09:00"); put("14:00"); put("17:00") }.toString(),
            notes = "This is a super long note for testing UI display performance. " +
                    "It should contain around 1000 characters to properly test how the interface " +
                    "handles large amounts of text. The note should wrap correctly and not overflow " +
                    "or get cut off. Additionally, the card height should adapt to the content. " +
                    "Remember to stay hydrated throughout the day. Drink at least 8 glasses of water. " +
                    "Keep a water bottle with you. Set reminders if needed. Track your progress. " +
                    "Make it a habit. Your body will thank you. Water is essential for health. " +
                    "It helps with digestion, circulation, and temperature regulation. " +
                    "Proper hydration improves energy levels and cognitive function. " +
                    "Don't wait until you're thirsty. Drink regularly throughout the day. " +
                    "Monitor your urine color - it should be light yellow. " +
                    "Adjust intake based on activity level and weather. " +
                    "This text continues to ensure we have enough characters for testing purposes. " +
                    "The UI should handle this gracefully with proper scrolling and layout. " +
                    "Make sure the text is readable and the card expands as needed. " +
                    "Testing edge cases is important for robust application development. " +
                    "This note serves that purpose effectively.",
            supervisionMethod = SupervisionMethod.EMAIL,
            supervisorEmails = JSONArray().apply { put("water@health.com") }.toString(),
            completionCount = random.nextInt(0, 100),
            createdDate = System.currentTimeMillis() - random.nextInt(0, 30) * 24 * 60 * 60 * 1000L
        ),
        // English habit - with many reminders for UI testing
        Habit(
            title = "Morning Jog",
            repeatCycle = RepeatCycle.WEEKLY,
            repeatDays = JSONArray().apply { put(1); put(3); put(5) }.toString(),
            reminderTimes = generateManyReminders(), // ~10 reminders (generated fresh)
            notes = "Warm up before running\nStretch after exercise\nWear proper shoes\nStart slow and increase gradually\nTrack your progress",
            supervisionMethod = SupervisionMethod.SMS,
            supervisorPhones = JSONArray().apply { put("+1234567890") }.toString(),
            completionCount = random.nextInt(0, 100),
            createdDate = System.currentTimeMillis() - random.nextInt(0, 30) * 24 * 60 * 60 * 1000L
        ),
        // Another long note
        Habit(
            title = "Read Books",
            repeatCycle = RepeatCycle.DAILY,
            reminderTimes = JSONArray().apply { put("20:00") }.toString(),
            notes = "This is another extended note for comprehensive UI testing. " +
                    "Reading is an excellent habit that improves knowledge and cognitive abilities. " +
                    "Set aside dedicated time each day for reading. Choose books that interest you. " +
                    "Take notes while reading to retain information better. Join a book club to discuss. " +
                    "Set reading goals like finishing a certain number of books per year. " +
                    "Create a comfortable reading space with good lighting. " +
                    "Minimize distractions while reading. Turn off electronic devices. " +
                    "Read before bed to improve sleep quality. " +
                    "Mix different genres to broaden your perspective. " +
                    "Keep a reading journal to track your thoughts. " +
                    "Share book recommendations with friends. " +
                    "Visit libraries and bookstores regularly. " +
                    "This extended text ensures proper testing of the UI components. " +
                    "The interface should display this content clearly with appropriate formatting. " +
                    "Scrolling should be smooth and the layout should be responsive. " +
                    "Testing with realistic data helps identify potential issues early.",
            supervisionMethod = SupervisionMethod.NONE,
            completionCount = random.nextInt(0, 100),
            createdDate = System.currentTimeMillis() - random.nextInt(0, 30) * 24 * 60 * 60 * 1000L
        ),
        // Another many reminders habit
        Habit(
            title = "Meditation",
            repeatCycle = RepeatCycle.DAILY,
            reminderTimes = generateManyReminders(), // ~10 reminders (generated fresh)
            supervisionMethod = SupervisionMethod.EMAIL,
            supervisorEmails = JSONArray().apply { put("zen@meditation.com") }.toString(),
            completionCount = random.nextInt(0, 100),
            createdDate = System.currentTimeMillis() - random.nextInt(0, 30) * 24 * 60 * 60 * 1000L
        ),
        Habit(
            title = "Learn Coding",
            repeatCycle = RepeatCycle.WEEKLY,
            repeatDays = JSONArray().apply { put(2); put(4); put(6) }.toString(),
            reminderTimes = JSONArray().apply { put("18:00") }.toString(),
            notes = "Complete one small project\nReview basic concepts\nPractice algorithms\nRead documentation",
            supervisionMethod = SupervisionMethod.SMS,
            supervisorPhones = JSONArray().apply { put("+0987654321") }.toString(),
            completionCount = random.nextInt(0, 100),
            createdDate = System.currentTimeMillis() - random.nextInt(0, 30) * 24 * 60 * 60 * 1000L
        ),
        Habit(
            title = "Early Sleep",
            repeatCycle = RepeatCycle.DAILY,
            reminderTimes = JSONArray().apply { put("22:00"); put("06:00") }.toString(),
            notes = "Avoid screens before bed\nCreate a bedtime routine\nKeep bedroom cool and dark",
            supervisionMethod = SupervisionMethod.NONE,
            completionCount = random.nextInt(0, 100),
            createdDate = System.currentTimeMillis() - random.nextInt(0, 30) * 24 * 60 * 60 * 1000L
        ),
        Habit(
            title = "Healthy Eating",
            repeatCycle = RepeatCycle.DAILY,
            reminderTimes = JSONArray().apply { put("08:00"); put("12:00"); put("18:00") }.toString(),
            notes = "Less oil and salt\nMore vegetables and fruits\nBalanced nutrition\nPortion control",
            supervisionMethod = SupervisionMethod.EMAIL,
            supervisorEmails = JSONArray().apply { put("healthy@food.com"); put("nutrition@diet.com") }.toString(),
            completionCount = random.nextInt(0, 100),
            createdDate = System.currentTimeMillis() - random.nextInt(0, 30) * 24 * 60 * 60 * 1000L
        ),
        Habit(
            title = "Yoga Stretching",
            repeatCycle = RepeatCycle.WEEKLY,
            repeatDays = JSONArray().apply { put(0); put(2); put(4); put(6) }.toString(),
            reminderTimes = JSONArray().apply { put("19:00") }.toString(),
            supervisionMethod = SupervisionMethod.NONE,
            completionCount = random.nextInt(0, 100),
            createdDate = System.currentTimeMillis() - random.nextInt(0, 30) * 24 * 60 * 60 * 1000L
        ),
        Habit(
            title = "Write Journal",
            repeatCycle = RepeatCycle.DAILY,
            reminderTimes = JSONArray().apply { put("22:30") }.toString(),
            notes = "Record today's events\nReflect and improve\nSet goals for tomorrow",
            supervisionMethod = SupervisionMethod.EMAIL,
            supervisorEmails = JSONArray().apply { put("journal@write.com") }.toString(),
            completionCount = random.nextInt(0, 100),
            createdDate = System.currentTimeMillis() - random.nextInt(0, 30) * 24 * 60 * 60 * 1000L
        ),
        Habit(
            title = "Quit Sugar",
            repeatCycle = RepeatCycle.DAILY,
            reminderTimes = JSONArray().apply { put("10:00") }.toString(),
            notes = "Avoid sugary drinks\nChoose healthy snacks\nRead food labels carefully",
            supervisionMethod = SupervisionMethod.SMS,
            supervisorPhones = JSONArray().apply { put("+1122334455"); put("+5566778899") }.toString(),
            completionCount = random.nextInt(0, 100),
            createdDate = System.currentTimeMillis() - random.nextInt(0, 30) * 24 * 60 * 60 * 1000L
        )
    )
    
    return sampleHabits
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    HabitPulseTheme {
        SettingsScreen()
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SettingsScreenDarkPreview() {
    HabitPulseTheme(darkTheme = true) {
        SettingsScreen()
    }
}