package io.github.darrindeyoung791.habitpulse

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.webkit.CookieManager
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Tablet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.darrindeyoung791.habitpulse.data.model.Habit
import io.github.darrindeyoung791.habitpulse.data.model.HabitCompletion
import io.github.darrindeyoung791.habitpulse.data.model.RepeatCycle
import io.github.darrindeyoung791.habitpulse.data.model.SupervisionMethod
import io.github.darrindeyoung791.habitpulse.data.preferences.UserPreferences
import io.github.darrindeyoung791.habitpulse.data.repository.HabitRepository
import io.github.darrindeyoung791.habitpulse.navigation.RouteConfig
import io.github.darrindeyoung791.habitpulse.service.ForegroundNotificationService
import io.github.darrindeyoung791.habitpulse.ui.screens.WebViewScreen
import io.github.darrindeyoung791.habitpulse.ui.theme.HabitPulseTheme
import io.github.darrindeyoung791.habitpulse.utils.AccessibilityUtils
import io.github.darrindeyoung791.habitpulse.utils.NotificationHelper
import io.github.darrindeyoung791.habitpulse.utils.NotificationPermissionHelper
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.File
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
    val activity = context as? ComponentActivity
    val scope = rememberCoroutineScope()
    val userPreferences = remember { UserPreferences.getInstance(context) }

    // WebView is now opened in a separate Activity (WebViewActivity)

    // 收集开屏广告设置状态
    val showSplashAd by userPreferences.showSplashAdFlow.collectAsStateWithLifecycle(initialValue = false)
    // 收集强制平板横屏模式设置状态
    val forceTabletLandscape by userPreferences.forceTabletLandscapeFlow.collectAsStateWithLifecycle(initialValue = false)
    // 收集持久通知设置状态
    val persistentNotification by userPreferences.persistentNotificationFlow.collectAsStateWithLifecycle(initialValue = false)

    // Use smallestScreenWidthDp to detect device type (independent of orientation)
    // Tablet: smallestScreenWidthDp >= 600dp
    // Phone: smallestScreenWidthDp < 600dp
    val configuration = LocalConfiguration.current
    val smallestScreenWidthDp = configuration.smallestScreenWidthDp
    val isTabletDevice = smallestScreenWidthDp >= 600
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isTabletLandscape = isTabletDevice && isLandscape
    // 只有在非平板横屏设备上才显示此开关
    val showForceTabletLandscapeSwitch = !isTabletLandscape

    // Check if TalkBack is enabled - poll every second to react to status changes
    var isTalkBackEnabled by remember { mutableStateOf(AccessibilityUtils.isTalkBackEnabled(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            isTalkBackEnabled = AccessibilityUtils.isTalkBackEnabled(context)
        }
    }

    // Track notification permission status
    var hasNotificationPermission by remember { mutableStateOf(NotificationHelper.hasNotificationPermission(context)) }

    // Refresh permission check when screen is composed
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            hasNotificationPermission = NotificationHelper.hasNotificationPermission(context)
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

    // Dialog for force tablet landscape mode warning
    var showForceTabletLandscapeDialog by remember { mutableStateOf(false) }
    var pendingForceTabletLandscapeValue by remember { mutableStateOf(false) }

    // Dialog for persistent notification when in limited mode
    var showPersistentNotificationLimitedDialog by remember { mutableStateOf(false) }
    var pendingPersistentNotificationValue by remember { mutableStateOf(false) }

    // Dialog for clear WebView cache
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showClearCookiesDialog by remember { mutableStateOf(false) }

    val TAP_TIME_WINDOW = 10_000L // 10 seconds
    val TAP_COUNT_THRESHOLD = 5

    // Permission request launcher for notifications
    val requestNotificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, update preference and start service
            scope.launch {
                userPreferences.setPersistentNotification(true)
                NotificationHelper.createNotificationChannel(context)
                ForegroundNotificationService.toggleService(context, true)
            }
        } else {
            // Permission denied, revert switch to off
            scope.launch {
                userPreferences.setPersistentNotification(false)
                android.widget.Toast.makeText(
                    context,
                    context.getString(R.string.settings_persistent_notification_permission_denied),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
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
                            val application = context.applicationContext as HabitPulseApplication
                            val database = application.database
                            val repository = HabitRepository(database.habitDao(), database.habitCompletionDao())
                            val (habits, completions) = generateSampleHabits()
                            // Insert habits first
                            habits.forEach { habit ->
                                database.habitDao().insert(habit)
                            }
                            // Then insert completion records
                            completions.forEach { completion ->
                                database.habitCompletionDao().insert(completion)
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

    // Dialog for force tablet landscape mode warning
    if (showForceTabletLandscapeDialog) {
        AlertDialog(
            onDismissRequest = { showForceTabletLandscapeDialog = false },
            title = {
                Text(text = stringResource(id = R.string.settings_force_tablet_landscape_warning_title))
            },
            text = {
                Text(text = stringResource(id = R.string.settings_force_tablet_landscape_warning_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showForceTabletLandscapeDialog = false
                        scope.launch {
                            userPreferences.setForceTabletLandscape(pendingForceTabletLandscapeValue)
                        }
                    }
                ) {
                    Text(text = stringResource(id = R.string.dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showForceTabletLandscapeDialog = false }) {
                    Text(text = stringResource(id = R.string.dialog_cancel))
                }
            }
        )
    }

    // Dialog for persistent notification when in limited mode
    if (showPersistentNotificationLimitedDialog) {
        AlertDialog(
            onDismissRequest = { showPersistentNotificationLimitedDialog = false },
            title = {
                Text(text = stringResource(id = R.string.settings_persistent_notification_limited_mode_title))
            },
            text = {
                Text(text = stringResource(id = R.string.settings_persistent_notification_limited_mode_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPersistentNotificationLimitedDialog = false
                        // Request permission after user confirms
                        if (!hasNotificationPermission) {
                            requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            scope.launch {
                                userPreferences.setPersistentNotification(pendingPersistentNotificationValue)
                                if (pendingPersistentNotificationValue) {
                                    NotificationHelper.createNotificationChannel(context)
                                    ForegroundNotificationService.toggleService(context, true)
                                } else {
                                    ForegroundNotificationService.toggleService(context, false)
                                }
                            }
                        }
                    }
                ) {
                    Text(text = stringResource(id = R.string.dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showPersistentNotificationLimitedDialog = false
                    // Revert switch state
                    scope.launch {
                        userPreferences.setPersistentNotification(!pendingPersistentNotificationValue)
                    }
                }) {
                    Text(text = stringResource(id = R.string.dialog_cancel))
                }
            }
        )
    }

    // Dialog for clear WebView cache
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = {
                Text(text = stringResource(id = R.string.settings_clear_webview_cache_dialog_title))
            },
            text = {
                Text(text = stringResource(id = R.string.settings_clear_webview_cache_dialog_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearCacheDialog = false
                        clearWebViewCache(context)
                    }
                ) {
                    Text(text = stringResource(id = R.string.settings_clear_cache))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text(text = stringResource(id = R.string.settings_cancel))
                }
            }
        )
    }

    // Dialog for deep clean (clear cookies and login)
    if (showClearCookiesDialog) {
        AlertDialog(
            onDismissRequest = { showClearCookiesDialog = false },
            title = {
                Text(text = stringResource(id = R.string.settings_clear_webview_cookies_dialog_title))
            },
            text = {
                Text(text = stringResource(id = R.string.settings_clear_webview_cookies_dialog_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearCookiesDialog = false
                        clearWebViewAll(context)
                    }
                ) {
                    Text(text = stringResource(id = R.string.settings_clear_cache))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCookiesDialog = false }) {
                    Text(text = stringResource(id = R.string.settings_cancel))
                }
            }
        )
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
                },
                actions = {
                    IconButton(onClick = {
                        val intent = Intent(context, WebViewActivity::class.java)
                        context.startActivity(intent)
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.HelpOutline,
                            contentDescription = stringResource(R.string.webview_help)
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
                    supportingText = stringResource(
                        id = if (isTalkBackEnabled) {
                            R.string.settings_support_habitpulse_switch_description_talkback
                        } else {
                            R.string.settings_support_habitpulse_switch_description
                        }
                    ),
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

            // 通知部分
            item {
                // Section header
                Text(
                    text = stringResource(id = R.string.settings_notifications),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                )

                if (!hasNotificationPermission) {
                    // No permission: show button to authorize
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            // Check if permission is permanently denied
                            val shouldShowRationale = activity?.let { 
                                NotificationHelper.shouldShowPermissionRationale(it) 
                            } ?: true
                            
                            if (shouldShowRationale) {
                                // Permission was denied before, but not permanently - request again
                                requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                // Permission permanently denied or not requested - open settings
                                NotificationPermissionHelper.openAppSettings(context)
                            }
                        },
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
                                Icon(
                                    imageVector = Icons.Outlined.Notifications,
                                    contentDescription = null
                                )
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.settings_persistent_notification),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = stringResource(id = R.string.settings_persistent_notification_description),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            TextButton(
                                onClick = {
                                    // Check if permission is permanently denied
                                    val shouldShowRationale = activity?.let { 
                                        NotificationHelper.shouldShowPermissionRationale(it) 
                                    } ?: true
                                    
                                    if (shouldShowRationale) {
                                        // Permission was denied before, but not permanently - request again
                                        requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        // Permission permanently denied or not requested - open settings
                                        NotificationPermissionHelper.openAppSettings(context)
                                    }
                                }
                            ) {
                                Text(
                                    text = stringResource(id = R.string.settings_persistent_notification_authorize)
                                )
                            }
                        }
                    }
                } else {
                    // Has permission: show switch
                    SettingsSwitchItem(
                        headline = stringResource(id = R.string.settings_persistent_notification),
                        supportingText = stringResource(id = R.string.settings_persistent_notification_description),
                        checked = persistentNotification,
                        onCheckedChange = { isChecked ->
                            if (!isChecked) {
                                // User wants to disable
                                scope.launch {
                                    userPreferences.setPersistentNotification(false)
                                    ForegroundNotificationService.toggleService(context, false)
                                }
                            } else {
                                // User wants to enable
                                // Check if in limited mode first
                                val application = context.applicationContext as HabitPulseApplication
                                val isLimitedMode = application.habitViewModel.isLimitedMode.value

                                if (isLimitedMode) {
                                    // Show dialog warning about exiting limited mode
                                    pendingPersistentNotificationValue = true
                                    showPersistentNotificationLimitedDialog = true
                                } else {
                                    // Has permission, just enable it
                                    scope.launch {
                                        userPreferences.setPersistentNotification(true)
                                        NotificationHelper.createNotificationChannel(context)
                                        ForegroundNotificationService.toggleService(context, true)
                                    }
                                }
                            }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = null
                            )
                        }
                    )
                }
            }

            // 存储部分
            item {
                // Section header
                Text(
                    text = stringResource(id = R.string.settings_storage),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                )

                // 说明文字
                Text(
                    text = stringResource(id = R.string.settings_storage_notice),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // Clear WebView cache
                SettingsListItem(
                    headline = stringResource(id = R.string.settings_clear_webview_cache),
                    supportingText = stringResource(id = R.string.settings_clear_webview_cache_description),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null
                        )
                    },
                    onClick = { showClearCacheDialog = true }
                )

                // Deep clean (clear cookies and login)
                SettingsListItem(
                    headline = stringResource(id = R.string.settings_clear_webview_cookies),
                    supportingText = stringResource(id = R.string.settings_clear_webview_cookies_description),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.DeleteForever,
                            contentDescription = null
                        )
                    },
                    onClick = { showClearCookiesDialog = true }
                )
            }

            // 视觉部分
            if (showForceTabletLandscapeSwitch) {
                item {
                    // Section header
                    Text(
                        text = stringResource(id = R.string.settings_visual),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                    )

                    // Force tablet landscape mode switch
                    SettingsSwitchItem(
                        headline = stringResource(id = R.string.settings_force_tablet_landscape),
                        supportingText = stringResource(id = R.string.settings_force_tablet_landscape_description),
                        checked = forceTabletLandscape,
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                // Show warning dialog when enabling
                                pendingForceTabletLandscapeValue = true
                                showForceTabletLandscapeDialog = true
                            } else {
                                // Disable without warning
                                scope.launch {
                                    userPreferences.setForceTabletLandscape(false)
                                }
                            }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Tablet,
                                contentDescription = null
                            )
                        }
                    )
                }
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
                    },
                    onClick = {
                        val intent = Intent(context, WebViewActivity::class.java).apply {
                            putExtra(WebViewActivity.EXTRA_INITIAL_URL, "https://darrindeyoung791.github.io/HabitPulse/team")
                        }
                        context.startActivity(intent)
                    }
                )

                SettingsListItem(
                    headline = stringResource(id = R.string.settings_open_source_licenses),
                    supportingText = stringResource(id = R.string.settings_open_source_licenses_description),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Article,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        val intent = Intent(context, OpenSourceLicensesActivity::class.java)
                        context.startActivity(intent)
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
                            val intent = Intent(context, WebViewActivity::class.java).apply {
                                putExtra(WebViewActivity.EXTRA_INITIAL_URL, RouteConfig.GITHUB_URL)
                            }
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

private fun clearWebViewCache(context: Context) {
    try {
        val cacheDir = context.cacheDir
        deleteDir(cacheDir)

        val externalCacheDir = context.externalCacheDir
        externalCacheDir?.let { deleteDir(it) }

        android.widget.Toast.makeText(
            context,
            context.getString(R.string.settings_clear_webview_cache_success),
            android.widget.Toast.LENGTH_SHORT
        ).show()
    } catch (e: Exception) {
        // Silent fail
    }
}

private fun clearWebViewAll(context: Context) {
    try {
        val cacheDir = context.cacheDir
        deleteDir(cacheDir)

        val externalCacheDir = context.externalCacheDir
        externalCacheDir?.let { deleteDir(it) }

        android.webkit.CookieManager.getInstance().removeAllCookies(null)

        android.widget.Toast.makeText(
            context,
            context.getString(R.string.settings_clear_webview_cookies_success),
            android.widget.Toast.LENGTH_SHORT
        ).show()
    } catch (e: Exception) {
        // Silent fail
    }
}

private fun deleteDir(dir: File): Boolean {
    if (dir.isDirectory) {
        dir.listFiles()?.forEach { child ->
            deleteDir(child)
        }
    }
    return dir.delete()
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
 * Generate 40 sample habits for debugging purposes
 * Includes varied titles, reminder times, supervision methods, and notes
 * Mix of Chinese and English content
 * Random completion counts for realistic data
 * Some habits have super long notes (~1000 chars) and many reminders (~10) for UI testing
 * 
 * Contact distribution:
 * - Habits 1-20: Unique supervisors (~20 contacts for testing Contacts screen)
 * - Habits 21-40: Frequent supervisors (for Bottom Sheet scrolling test)
 *
 * @return Pair of List<Habit> and List<HabitCompletion> (habits and their completion records)
 */
private fun generateSampleHabits(): Pair<List<Habit>, List<HabitCompletion>> {
    val random = kotlin.random.Random(System.currentTimeMillis())

    // Unique supervisors for testing Contacts screen (~20 total contacts)
    // 12 unique email addresses
    val uniqueEmail1 = "alice.supervisor@example.com"
    val uniqueEmail2 = "bob.coach@health.com"
    val uniqueEmail3 = "carol.mentor@fitness.com"
    val uniqueEmail4 = "david.trainer@sports.com"
    val uniqueEmail5 = "emma.guide@wellness.com"
    val uniqueEmail6 = "frank.advisor@lifestyle.com"
    val uniqueEmail7 = "grace.helper@habits.com"
    val uniqueEmail8 = "henry.support@daily.com"
    val uniqueEmail9 = "iris.partner@goals.com"
    val uniqueEmail10 = "jack.buddy@routine.com"
    val uniqueEmail11 = "kate.friend@tracker.com"
    val uniqueEmail12 = "leo.monitor@check.com"
    
    // 8 unique phone numbers
    val uniquePhone1 = "+8613800138001"
    val uniquePhone2 = "+8613900139002"
    val uniquePhone3 = "+8613700137003"
    val uniquePhone4 = "+8613600136004"
    val uniquePhone5 = "+8613500135005"
    val uniquePhone6 = "+8613400134006"
    val uniquePhone7 = "+8613300133007"
    val uniquePhone8 = "+8613200132008"

    // Frequent supervisors (appear in multiple habits for testing Bottom Sheet scrolling)
    val frequentEmail1 = "supervisor@example.com"
    val frequentEmail2 = "manager@example.com"
    val frequentPhone1 = "+8613800138000"
    val frequentPhone2 = "+8613900139000"

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

    // Helper to generate date string
    fun formatDate(daysAgo: Int): String {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -daysAgo)
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH) + 1
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        return String.format("%04d-%02d-%02d", year, month, day)
    }

    // Helper to generate timestamp for a specific day (days ago)
    fun generateTimestamp(daysAgo: Int, hour: Int = 12, minute: Int = 0): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -daysAgo)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, hour)
        calendar.set(java.util.Calendar.MINUTE, minute)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    val longNote = generateLongNote()
    val habits = mutableListOf<Habit>()
    val completions = mutableListOf<HabitCompletion>()

    // Chinese habits with unique supervisors (10 habits = ~10 contacts)
    val habit1 = Habit(
        title = "每天喝水",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = JSONArray().apply { put("08:00"); put("12:00"); put("18:00") }.toString(),
        notes = longNote,
        supervisionMethod = SupervisionMethod.EMAIL,
        supervisorEmails = JSONArray().apply { put(uniqueEmail1) }.toString(),
        completionCount = 45,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit1)
    // Generate completion records for the past 30 days (randomly)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.3f) { // 70% completion rate
            completions.add(HabitCompletion(
                habitId = habit1.id,
                completedDate = generateTimestamp(i, 9 + random.nextInt(12), random.nextInt(60)),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit2 = Habit(
        title = "晨跑锻炼",
        repeatCycle = RepeatCycle.WEEKLY,
        repeatDays = JSONArray().apply { put(1); put(3); put(5) }.toString(),
        reminderTimes = generateManyReminders(),
        notes = "跑步前记得热身\n跑完后要拉伸\n注意呼吸节奏\n选择合适的跑鞋\n循序渐进增加距离",
        supervisionMethod = SupervisionMethod.SMS,
        supervisorPhones = JSONArray().apply { put(uniquePhone1) }.toString(),
        completionCount = 28,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit2)
    for (i in 0 until 30) {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
        val dayOfWeek = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7 // Convert to 0=Monday
        if (dayOfWeek in listOf(1, 3, 5) && random.nextFloat() > 0.2f) {
            completions.add(HabitCompletion(
                habitId = habit2.id,
                completedDate = generateTimestamp(i, 7, 30),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit3 = Habit(
        title = "阅读书籍",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = JSONArray().apply { put("21:00") }.toString(),
        notes = "每天至少读 30 分钟\n记录读书笔记\n分享读书心得",
        supervisionMethod = SupervisionMethod.EMAIL,
        supervisorEmails = JSONArray().apply { put(uniqueEmail2) }.toString(),
        completionCount = 25,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit3)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.25f) {
            completions.add(HabitCompletion(
                habitId = habit3.id,
                completedDate = generateTimestamp(i, 21, random.nextInt(60)),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit4 = Habit(
        title = "冥想练习",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = JSONArray().apply { put("07:00"); put("22:00") }.toString(),
        notes = longNote,
        supervisionMethod = SupervisionMethod.SMS,
        supervisorPhones = JSONArray().apply { put(uniquePhone2) }.toString(),
        completionCount = 20,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit4)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.35f) {
            completions.add(HabitCompletion(
                habitId = habit4.id,
                completedDate = generateTimestamp(i, 7, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit5 = Habit(
        title = "学习编程",
        repeatCycle = RepeatCycle.WEEKLY,
        repeatDays = JSONArray().apply { put(2); put(4); put(6) }.toString(),
        reminderTimes = generateManyReminders(),
        notes = "完成一个小型项目\n复习基础知识\n练习算法题\n阅读技术文档\n参与开源项目",
        supervisionMethod = SupervisionMethod.EMAIL,
        supervisorEmails = JSONArray().apply { put(uniqueEmail3) }.toString(),
        completionCount = 18,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit5)
    for (i in 0 until 30) {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
        val dayOfWeek = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
        if (dayOfWeek in listOf(2, 4, 6) && random.nextFloat() > 0.3f) {
            completions.add(HabitCompletion(
                habitId = habit5.id,
                completedDate = generateTimestamp(i, 20, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit6 = Habit(
        title = "早睡早起",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = JSONArray().apply { put("22:30"); put("06:30") }.toString(),
        supervisionMethod = SupervisionMethod.SMS,
        supervisorPhones = JSONArray().apply { put(uniquePhone3) }.toString(),
        completionCount = 22,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit6)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.3f) {
            completions.add(HabitCompletion(
                habitId = habit6.id,
                completedDate = generateTimestamp(i, 6, 30),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit7 = Habit(
        title = "健康饮食",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = JSONArray().apply { put("08:00"); put("12:00"); put("18:00") }.toString(),
        notes = "少油少盐\n多吃蔬菜水果\n控制糖分摄入\n适量蛋白质\n多喝水",
        supervisionMethod = SupervisionMethod.EMAIL,
        supervisorEmails = JSONArray().apply { put(uniqueEmail4) }.toString(),
        completionCount = 27,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit7)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.2f) {
            completions.add(HabitCompletion(
                habitId = habit7.id,
                completedDate = generateTimestamp(i, 12, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit8 = Habit(
        title = "瑜伽拉伸",
        repeatCycle = RepeatCycle.WEEKLY,
        repeatDays = JSONArray().apply { put(0); put(2); put(4); put(6) }.toString(),
        reminderTimes = JSONArray().apply { put("20:00") }.toString(),
        supervisionMethod = SupervisionMethod.SMS,
        supervisorPhones = JSONArray().apply { put(uniquePhone4) }.toString(),
        completionCount = 15,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit8)
    for (i in 0 until 30) {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
        val dayOfWeek = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
        if (dayOfWeek in listOf(0, 2, 4, 6) && random.nextFloat() > 0.4f) {
            completions.add(HabitCompletion(
                habitId = habit8.id,
                completedDate = generateTimestamp(i, 20, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit9 = Habit(
        title = "写日记",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = JSONArray().apply { put("23:00") }.toString(),
        notes = longNote,
        supervisionMethod = SupervisionMethod.EMAIL,
        supervisorEmails = JSONArray().apply { put(uniqueEmail5) }.toString(),
        completionCount = 19,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit9)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.4f) {
            completions.add(HabitCompletion(
                habitId = habit9.id,
                completedDate = generateTimestamp(i, 23, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit10 = Habit(
        title = "戒烟",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = JSONArray().apply { put("09:00") }.toString(),
        notes = "坚持就是胜利\n想想健康的重要性\n避免诱因\n寻找替代方法",
        supervisionMethod = SupervisionMethod.SMS,
        supervisorPhones = JSONArray().apply { put(uniquePhone5) }.toString(),
        completionCount = 30,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit10)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.1f) { // 90% success rate
            completions.add(HabitCompletion(
                habitId = habit10.id,
                completedDate = generateTimestamp(i, 9, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    // English habits with unique supervisors (10 habits = ~10 more contacts, total ~20)
    val habit11 = Habit(
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
        supervisorEmails = JSONArray().apply { put(uniqueEmail6) }.toString(),
        completionCount = 26,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit11)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.2f) {
            completions.add(HabitCompletion(
                habitId = habit11.id,
                completedDate = generateTimestamp(i, 10, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit12 = Habit(
        title = "Morning Jog",
        repeatCycle = RepeatCycle.WEEKLY,
        repeatDays = JSONArray().apply { put(1); put(3); put(5) }.toString(),
        reminderTimes = generateManyReminders(),
        notes = "Warm up before running\nStretch after exercise\nWear proper shoes\nStart slow and increase gradually\nTrack your progress",
        supervisionMethod = SupervisionMethod.SMS,
        supervisorPhones = JSONArray().apply { put(uniquePhone6) }.toString(),
        completionCount = 20,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit12)
    for (i in 0 until 30) {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
        val dayOfWeek = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
        if (dayOfWeek in listOf(1, 3, 5) && random.nextFloat() > 0.3f) {
            completions.add(HabitCompletion(
                habitId = habit12.id,
                completedDate = generateTimestamp(i, 6, 30),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit13 = Habit(
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
        supervisionMethod = SupervisionMethod.EMAIL,
        supervisorEmails = JSONArray().apply { put(uniqueEmail7) }.toString(),
        completionCount = 24,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit13)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.25f) {
            completions.add(HabitCompletion(
                habitId = habit13.id,
                completedDate = generateTimestamp(i, 20, 30),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit14 = Habit(
        title = "Meditation",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = generateManyReminders(),
        supervisionMethod = SupervisionMethod.SMS,
        supervisorPhones = JSONArray().apply { put(uniquePhone7) }.toString(),
        completionCount = 18,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit14)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.4f) {
            completions.add(HabitCompletion(
                habitId = habit14.id,
                completedDate = generateTimestamp(i, 6, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit15 = Habit(
        title = "Learn Coding",
        repeatCycle = RepeatCycle.WEEKLY,
        repeatDays = JSONArray().apply { put(2); put(4); put(6) }.toString(),
        reminderTimes = JSONArray().apply { put("18:00") }.toString(),
        notes = "Complete one small project\nReview basic concepts\nPractice algorithms\nRead documentation",
        supervisionMethod = SupervisionMethod.EMAIL,
        supervisorEmails = JSONArray().apply { put(uniqueEmail8) }.toString(),
        completionCount = 16,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit15)
    for (i in 0 until 30) {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
        val dayOfWeek = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
        if (dayOfWeek in listOf(2, 4, 6) && random.nextFloat() > 0.35f) {
            completions.add(HabitCompletion(
                habitId = habit15.id,
                completedDate = generateTimestamp(i, 19, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit16 = Habit(
        title = "Early Sleep",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = JSONArray().apply { put("22:00"); put("06:00") }.toString(),
        notes = "Avoid screens before bed\nCreate a bedtime routine\nKeep bedroom cool and dark",
        supervisionMethod = SupervisionMethod.SMS,
        supervisorPhones = JSONArray().apply { put(uniquePhone8) }.toString(),
        completionCount = 21,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit16)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.3f) {
            completions.add(HabitCompletion(
                habitId = habit16.id,
                completedDate = generateTimestamp(i, 22, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit17 = Habit(
        title = "Healthy Eating",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = JSONArray().apply { put("08:00"); put("12:00"); put("18:00") }.toString(),
        notes = "Less oil and salt\nMore vegetables and fruits\nBalanced nutrition\nPortion control",
        supervisionMethod = SupervisionMethod.EMAIL,
        supervisorEmails = JSONArray().apply { put(uniqueEmail9) }.toString(),
        completionCount = 28,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit17)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.15f) {
            completions.add(HabitCompletion(
                habitId = habit17.id,
                completedDate = generateTimestamp(i, 12, 30),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit18 = Habit(
        title = "Yoga Stretching",
        repeatCycle = RepeatCycle.WEEKLY,
        repeatDays = JSONArray().apply { put(0); put(2); put(4); put(6) }.toString(),
        reminderTimes = JSONArray().apply { put("19:00") }.toString(),
        supervisionMethod = SupervisionMethod.SMS,
        supervisorPhones = JSONArray().apply { put(frequentPhone1) }.toString(),
        completionCount = 14,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit18)
    for (i in 0 until 30) {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
        val dayOfWeek = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
        if (dayOfWeek in listOf(0, 2, 4, 6) && random.nextFloat() > 0.45f) {
            completions.add(HabitCompletion(
                habitId = habit18.id,
                completedDate = generateTimestamp(i, 19, 30),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit19 = Habit(
        title = "Write Journal",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = JSONArray().apply { put("22:30") }.toString(),
        notes = "Record today's events\nReflect and improve\nSet goals for tomorrow",
        supervisionMethod = SupervisionMethod.EMAIL,
        supervisorEmails = JSONArray().apply { put(uniqueEmail10) }.toString(),
        completionCount = 17,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit19)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.45f) {
            completions.add(HabitCompletion(
                habitId = habit19.id,
                completedDate = generateTimestamp(i, 22, 30),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit20 = Habit(
        title = "Quit Sugar",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = JSONArray().apply { put("10:00") }.toString(),
        notes = "Avoid sugary drinks\nChoose healthy snacks\nRead food labels carefully",
        supervisionMethod = SupervisionMethod.SMS,
        supervisorPhones = JSONArray().apply { put(frequentPhone2) }.toString(),
        completionCount = 25,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit20)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.2f) {
            completions.add(HabitCompletion(
                habitId = habit20.id,
                completedDate = generateTimestamp(i, 10, 30),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    // Additional 20 habits (21-40) with frequent supervisors for Bottom Sheet scrolling test
    // These habits use frequentEmail1, frequentEmail2, frequentPhone1, frequentPhone2
    // Plus the last 2 unique contacts (uniqueEmail11, uniqueEmail12)

    val habit21 = Habit(
        title = "每日护肤",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = JSONArray().apply { put("07:00"); put("21:00") }.toString(),
        notes = "洁面→爽肤水→精华→面霜\n每周去角质 1-2 次\n注意防晒",
        supervisionMethod = SupervisionMethod.EMAIL,
        supervisorEmails = JSONArray().apply { put(uniqueEmail11) }.toString(),
        completionCount = 23,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit21)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.3f) {
            completions.add(HabitCompletion(
                habitId = habit21.id,
                completedDate = generateTimestamp(i, 21, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit22 = Habit(
        title = "记账理财",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = JSONArray().apply { put("22:00") }.toString(),
        notes = longNote,
        supervisionMethod = SupervisionMethod.EMAIL,
        supervisorEmails = JSONArray().apply { put(uniqueEmail12) }.toString(),
        completionCount = 19,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit22)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.35f) {
            completions.add(HabitCompletion(
                habitId = habit22.id,
                completedDate = generateTimestamp(i, 22, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit23 = Habit(
        title = "练习吉他",
        repeatCycle = RepeatCycle.WEEKLY,
        repeatDays = JSONArray().apply { put(1); put(3); put(5) }.toString(),
        reminderTimes = JSONArray().apply { put("19:00") }.toString(),
        notes = "练习音阶\n学习和弦\n弹奏曲目\n节奏训练",
        supervisionMethod = SupervisionMethod.SMS,
        supervisorPhones = JSONArray().apply { put(frequentPhone1) }.toString(),
        completionCount = 15,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit23)
    for (i in 0 until 30) {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
        val dayOfWeek = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
        if (dayOfWeek in listOf(1, 3, 5) && random.nextFloat() > 0.4f) {
            completions.add(HabitCompletion(
                habitId = habit23.id,
                completedDate = generateTimestamp(i, 19, 30),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit24 = Habit(
        title = "学习外语",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = generateManyReminders(),
        notes = "背单词\n练听力\n口语对话\n阅读理解",
        supervisionMethod = SupervisionMethod.EMAIL,
        supervisorEmails = JSONArray().apply { put(frequentEmail1) }.toString(),
        completionCount = 22,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit24)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.25f) {
            completions.add(HabitCompletion(
                habitId = habit24.id,
                completedDate = generateTimestamp(i, 18, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit25 = Habit(
        title = "整理房间",
        repeatCycle = RepeatCycle.WEEKLY,
        repeatDays = JSONArray().apply { put(0) }.toString(),
        reminderTimes = JSONArray().apply { put("10:00") }.toString(),
        supervisionMethod = SupervisionMethod.NONE,
        completionCount = 12,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit25)
    for (i in 0 until 30) {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
        val dayOfWeek = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
        if (dayOfWeek == 0 && random.nextFloat() > 0.3f) {
            completions.add(HabitCompletion(
                habitId = habit25.id,
                completedDate = generateTimestamp(i, 10, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit26 = Habit(
        title = "午休时间",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = JSONArray().apply { put("13:00") }.toString(),
        notes = "午休 20-30 分钟\n不要超过 30 分钟\n避免影响夜间睡眠",
        supervisionMethod = SupervisionMethod.SMS,
        supervisorPhones = JSONArray().apply { put(frequentPhone2) }.toString(),
        completionCount = 26,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit26)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.2f) {
            completions.add(HabitCompletion(
                habitId = habit26.id,
                completedDate = generateTimestamp(i, 13, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit27 = Habit(
        title = "补充维生素",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = JSONArray().apply { put("08:00") }.toString(),
        supervisionMethod = SupervisionMethod.EMAIL,
        supervisorEmails = JSONArray().apply { put(frequentEmail1) }.toString(),
        completionCount = 28,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit27)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.15f) {
            completions.add(HabitCompletion(
                habitId = habit27.id,
                completedDate = generateTimestamp(i, 8, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit28 = Habit(
        title = "散步放松",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = JSONArray().apply { put("19:30") }.toString(),
        notes = "饭后散步 30 分钟\n呼吸新鲜空气\n放松身心",
        supervisionMethod = SupervisionMethod.SMS,
        supervisorPhones = JSONArray().apply { put(frequentPhone1) }.toString(),
        completionCount = 24,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit28)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.25f) {
            completions.add(HabitCompletion(
                habitId = habit28.id,
                completedDate = generateTimestamp(i, 19, 30),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit29 = Habit(
        title = "练习书法",
        repeatCycle = RepeatCycle.WEEKLY,
        repeatDays = JSONArray().apply { put(2); put(6) }.toString(),
        reminderTimes = JSONArray().apply { put("15:00") }.toString(),
        notes = longNote,
        supervisionMethod = SupervisionMethod.EMAIL,
        supervisorEmails = JSONArray().apply { put(frequentEmail2) }.toString(),
        completionCount = 14,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit29)
    for (i in 0 until 30) {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
        val dayOfWeek = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
        if (dayOfWeek in listOf(2, 6) && random.nextFloat() > 0.35f) {
            completions.add(HabitCompletion(
                habitId = habit29.id,
                completedDate = generateTimestamp(i, 15, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit30 = Habit(
        title = "深度清洁",
        repeatCycle = RepeatCycle.WEEKLY,
        repeatDays = JSONArray().apply { put(6) }.toString(),
        reminderTimes = JSONArray().apply { put("09:00") }.toString(),
        supervisionMethod = SupervisionMethod.NONE,
        completionCount = 11,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit30)
    for (i in 0 until 30) {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
        val dayOfWeek = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
        if (dayOfWeek == 6 && random.nextFloat() > 0.4f) {
            completions.add(HabitCompletion(
                habitId = habit30.id,
                completedDate = generateTimestamp(i, 9, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    // English habits 21-30
    val habit31 = Habit(
        title = "Skincare Routine",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = JSONArray().apply { put("07:30"); put("21:30") }.toString(),
        notes = "Cleanse → Tone → Serum → Moisturize\nExfoliate 1-2 times per week\nApply sunscreen daily",
        supervisionMethod = SupervisionMethod.EMAIL,
        supervisorEmails = JSONArray().apply { put(frequentEmail1) }.toString(),
        completionCount = 21,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit31)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.3f) {
            completions.add(HabitCompletion(
                habitId = habit31.id,
                completedDate = generateTimestamp(i, 21, 30),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit32 = Habit(
        title = "Track Expenses",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = JSONArray().apply { put("23:00") }.toString(),
        notes = "Record all daily expenses\nReview weekly budget\nCategorize spending\nSet savings goals",
        supervisionMethod = SupervisionMethod.EMAIL,
        supervisorEmails = JSONArray().apply { put(frequentEmail1) }.toString(),
        completionCount = 18,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit32)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.35f) {
            completions.add(HabitCompletion(
                habitId = habit32.id,
                completedDate = generateTimestamp(i, 23, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit33 = Habit(
        title = "Guitar Practice",
        repeatCycle = RepeatCycle.WEEKLY,
        repeatDays = JSONArray().apply { put(2); put(4); put(6) }.toString(),
        reminderTimes = JSONArray().apply { put("20:00") }.toString(),
        notes = "Practice scales\nLearn new chords\nPlay songs\nWork on rhythm",
        supervisionMethod = SupervisionMethod.SMS,
        supervisorPhones = JSONArray().apply { put(frequentPhone2) }.toString(),
        completionCount = 16,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit33)
    for (i in 0 until 30) {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
        val dayOfWeek = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
        if (dayOfWeek in listOf(2, 4, 6) && random.nextFloat() > 0.4f) {
            completions.add(HabitCompletion(
                habitId = habit33.id,
                completedDate = generateTimestamp(i, 20, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit34 = Habit(
        title = "Language Learning",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = generateManyReminders(),
        notes = "Vocabulary practice\nListening exercises\nSpeaking practice\nReading comprehension",
        supervisionMethod = SupervisionMethod.EMAIL,
        supervisorEmails = JSONArray().apply { put(frequentEmail2); put(frequentEmail1) }.toString(),
        completionCount = 20,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit34)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.28f) {
            completions.add(HabitCompletion(
                habitId = habit34.id,
                completedDate = generateTimestamp(i, 17, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit35 = Habit(
        title = "Tidy Up Room",
        repeatCycle = RepeatCycle.WEEKLY,
        repeatDays = JSONArray().apply { put(0) }.toString(),
        reminderTimes = JSONArray().apply { put("11:00") }.toString(),
        supervisionMethod = SupervisionMethod.NONE,
        completionCount = 13,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit35)
    for (i in 0 until 30) {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
        val dayOfWeek = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
        if (dayOfWeek == 0 && random.nextFloat() > 0.35f) {
            completions.add(HabitCompletion(
                habitId = habit35.id,
                completedDate = generateTimestamp(i, 11, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit36 = Habit(
        title = "Afternoon Break",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = JSONArray().apply { put("15:00") }.toString(),
        notes = "Take a 15-minute break\nStretch and move around\nRest your eyes from screens",
        supervisionMethod = SupervisionMethod.SMS,
        supervisorPhones = JSONArray().apply { put(frequentPhone1) }.toString(),
        completionCount = 25,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit36)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.22f) {
            completions.add(HabitCompletion(
                habitId = habit36.id,
                completedDate = generateTimestamp(i, 15, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit37 = Habit(
        title = "Take Vitamins",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = JSONArray().apply { put("09:00") }.toString(),
        supervisionMethod = SupervisionMethod.EMAIL,
        supervisorEmails = JSONArray().apply { put(frequentEmail1) }.toString(),
        completionCount = 27,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit37)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.18f) {
            completions.add(HabitCompletion(
                habitId = habit37.id,
                completedDate = generateTimestamp(i, 9, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit38 = Habit(
        title = "Evening Walk",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = JSONArray().apply { put("18:30") }.toString(),
        notes = "Walk for 30 minutes\nBreathe fresh air\nRelax and unwind",
        supervisionMethod = SupervisionMethod.SMS,
        supervisorPhones = JSONArray().apply { put(frequentPhone2) }.toString(),
        completionCount = 23,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit38)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.27f) {
            completions.add(HabitCompletion(
                habitId = habit38.id,
                completedDate = generateTimestamp(i, 18, 30),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit39 = Habit(
        title = "Calligraphy Practice",
        repeatCycle = RepeatCycle.WEEKLY,
        repeatDays = JSONArray().apply { put(1); put(5) }.toString(),
        reminderTimes = JSONArray().apply { put("16:00") }.toString(),
        notes = "Practice basic strokes\nCopy master works\nFocus on form and rhythm\nEnjoy the process",
        supervisionMethod = SupervisionMethod.EMAIL,
        supervisorEmails = JSONArray().apply { put(frequentEmail2) }.toString(),
        completionCount = 15,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit39)
    for (i in 0 until 30) {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
        val dayOfWeek = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
        if (dayOfWeek in listOf(1, 5) && random.nextFloat() > 0.38f) {
            completions.add(HabitCompletion(
                habitId = habit39.id,
                completedDate = generateTimestamp(i, 16, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit40 = Habit(
        title = "Deep Cleaning",
        repeatCycle = RepeatCycle.WEEKLY,
        repeatDays = JSONArray().apply { put(0) }.toString(),
        reminderTimes = JSONArray().apply { put("10:00") }.toString(),
        supervisionMethod = SupervisionMethod.NONE,
        completionCount = 10,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit40)
    for (i in 0 until 30) {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
        val dayOfWeek = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
        if (dayOfWeek == 0 && random.nextFloat() > 0.45f) {
            completions.add(HabitCompletion(
                habitId = habit40.id,
                completedDate = generateTimestamp(i, 10, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    return Pair(habits, completions)
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