package io.github.darrindeyoung791.habitpulse.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.darrindeyoung791.habitpulse.OpenSourceLicensesContent
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.navigation.getDeviceCornerRadius
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsBetweenGroupGap
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedGroup
import io.github.darrindeyoung791.habitpulse.ui.screens.settings.components.SettingsSegmentedItem
import io.github.darrindeyoung791.habitpulse.ui.utils.PressVibrationFeedback

/** 右侧面板内嵌的设置页面路由（全屏特例：语言/字体大小/AI 编辑配置不在此列）。 */
internal enum class SettingsPage {
    General, AI, Notifications, Template, About, Debug, DebugReminder, DebugVibration, UICatalog, Licenses
}

/** 左栏宽度（仿 Android 15 平板设置双栏比例，约屏宽 40% 上限）。 */
private val SettingsTwoPaneLeftWidth = 360.dp

/** 右侧悬浮面板与屏幕边缘/左栏之间的间距。 */
private val SettingsTwoPanePanelGap = 12.dp

/** 页面切换淡入淡出时长。 */
private const val SettingsPaneFadeMillis = 150

/** 当前页面所属的顶级分类（左栏 selected 高亮依据）。 */
private fun categoryRootOf(page: SettingsPage): SettingsPage = when (page) {
    SettingsPage.Template -> SettingsPage.Notifications
    SettingsPage.Debug,
    SettingsPage.DebugReminder,
    SettingsPage.DebugVibration,
    SettingsPage.UICatalog,
    SettingsPage.Licenses -> SettingsPage.About
    else -> page
}

/**
 * 平板横屏双栏设置界面（仿 Android 15+ 平板设置）。
 *
 * - 左栏：返回按钮在上（从设置整体返回上一页），加大「设置」标题在下，
 *   分类列表随当前子页显示 selected 态；点击任意分类随时切换并重置右栏栈。
 * - 右栏：surfaceContainerLow 背景变色的悬浮圆角面板（零阴影），内部维护
 *   rememberSaveable 路由栈，默认进入 [SettingsPage.General]（通用设置）。
 * - 系统返回逻辑：栈 > 1 时弹一层（子页面返回上一级）；已在分类根时退出设置。
 * - 全屏特例经回调交宿主以独立 Activity 打开：语言、字体大小、AI 编辑配置；
 *   帮助与反馈为 WebViewActivity，天然全屏，仅在右栏子页 app bar 显示。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTwoPaneScreen(
    onFinish: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenLanguage: () -> Unit,
    onOpenFontScale: () -> Unit,
    onOpenAiEdit: (configId: String?) -> Unit
) {
    val stackSaver = listSaver<List<SettingsPage>, String>(
        save = { pages -> pages.map { it.name } },
        restore = { names -> names.map { SettingsPage.valueOf(it) } }
    )
    var stack by rememberSaveable(stateSaver = stackSaver) {
        mutableStateOf(listOf(SettingsPage.General))
    }

    val currentPage = stack.last()

    fun push(page: SettingsPage) {
        stack = stack + page
    }

    fun pop() {
        if (stack.size > 1) stack = stack.dropLast(1)
    }

    // 系统返回：优先返回子页面的上一页；到分类根后再返回即从设置整体退出
    BackHandler {
        if (stack.size > 1) pop() else onFinish()
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            // 整屏画布用背景变体色：左栏与右侧面板四周的空隙同色连续，
            // 右侧 surface 圆角面板在其上呈现悬浮效果（Android 15 设置同构）
            .background(MaterialTheme.colorScheme.surfaceContainer)
            // 左右两侧（横屏刘海/挖孔）inset 统一处理；顶部 inset 由左右两栏
            // 各自处理
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.End)
            )
    ) {
        // ---------------- 左栏：设置总界面（与整屏画布同底色） ----------------
        // 可折叠「设置」标题 + 滚动分类列表 + 固定返回按钮（层级高于列表，遮挡滚动内容）
        Box(
            modifier = Modifier
                .width(SettingsTwoPaneLeftWidth)
                .fillMaxHeight()
                // 顶部状态栏 inset
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
        ) {
            // ===== 底层：可滚动区（标题 + 分类列表） =====
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    // 底部导航栏 inset
                    .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
            ) {
                // 固定区高度占位（防止首项被遮挡时无视觉锚点）
                Spacer(modifier = Modifier.height(56.dp))
                // 加大的「设置」标题随列表滚动可折叠（向上滚动时收起，与主页一致）
                Text(
                    text = stringResource(id = R.string.settings_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp)
                )
                Spacer(modifier = Modifier.height(SettingsTwoPanePanelGap))

            // 分类列表：selected 高亮当前分类；帮助入口只在右栏子页 app bar，不在左栏重复。
            // 与屏幕左右边缘各留 16dp 边距（与设置单栏内容边距一致）；
            // 列表项容器色反转为 surface，在 surfaceContainer 左栏底上凸显，
            // 并与右栏面板底色呼应（Android 15 设置同构）
            val leftItemContainer = MaterialTheme.colorScheme.surface
            val currentCategory = categoryRootOf(currentPage)
            SettingsSegmentedGroup(
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                SettingsSegmentedItem(
                    index = 0,
                    count = 5,
                    headline = stringResource(id = R.string.settings_category_ai),
                    supportingText = stringResource(id = R.string.settings_category_ai_description),
                    leadingIcon = Icons.Outlined.AutoAwesome,
                    selected = currentCategory == SettingsPage.AI,
                    containerColorOverride = leftItemContainer,
                    onClick = { stack = listOf(SettingsPage.AI) }
                )
                SettingsSegmentedItem(
                    index = 1,
                    count = 5,
                    headline = stringResource(id = R.string.settings_category_lan),
                    enabled = false,
                    showArrow = false,
                    leadingIcon = Icons.Outlined.Lan,
                    containerColorOverride = leftItemContainer,
                    onClick = {}
                )
                SettingsSegmentedItem(
                    index = 2,
                    count = 5,
                    headline = stringResource(id = R.string.settings_notifications),
                    supportingText = stringResource(id = R.string.settings_notifications_description),
                    leadingIcon = Icons.Outlined.Notifications,
                    selected = currentCategory == SettingsPage.Notifications,
                    containerColorOverride = leftItemContainer,
                    onClick = { stack = listOf(SettingsPage.Notifications) }
                )
                SettingsSegmentedItem(
                    index = 3,
                    count = 5,
                    headline = stringResource(id = R.string.settings_category_general),
                    supportingText = stringResource(id = R.string.settings_category_general_description),
                    leadingIcon = Icons.Outlined.Settings,
                    selected = currentCategory == SettingsPage.General,
                    containerColorOverride = leftItemContainer,
                    onClick = { stack = listOf(SettingsPage.General) }
                )
                SettingsSegmentedItem(
                    index = 4,
                    count = 5,
                    headline = stringResource(id = R.string.settings_about),
                    supportingText = stringResource(id = R.string.settings_about_description),
                    leadingIcon = Icons.Outlined.Info,
                    selected = currentCategory == SettingsPage.About,
                    containerColorOverride = leftItemContainer,
                    onClick = { stack = listOf(SettingsPage.About) }
                )
            }

            // 帮助与反馈（与竖屏设置主页一致的独立分组；WebView 全屏打开）
            Spacer(modifier = Modifier.height(SettingsBetweenGroupGap))
            SettingsSegmentedGroup(
                modifier = Modifier.padding(horizontal = 16.dp),
                tintOffset = 5
            ) {
                SettingsSegmentedItem(
                    index = 0,
                    count = 1,
                    headline = stringResource(id = R.string.settings_help_button),
                    supportingText = stringResource(id = R.string.settings_help_button_description),
                    leadingIcon = Icons.AutoMirrored.Outlined.HelpOutline,
                    containerColorOverride = leftItemContainer,
                    onClick = onOpenHelp
                )
            }
        }

            // ===== 上层：固定返回按钮（surface 底色遮挡滚动内容，与主页 app bar 一致） =====
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 8.dp)
                ) {
                    val backInteractionSource = remember { MutableInteractionSource() }
                    PressVibrationFeedback(interactionSource = backInteractionSource)
                    IconButton(
                        onClick = onFinish,
                        interactionSource = backInteractionSource,
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.settings_back)
                        )
                    }
                }
            }
        }

        // ---------------- 右侧：悬浮面板（与页面同底色，靠圆角+边距区分） ----------------
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(
                    start = SettingsTwoPanePanelGap,
                    end = SettingsTwoPanePanelGap,
                    bottom = SettingsTwoPanePanelGap
                )
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                .padding(top = SettingsTwoPanePanelGap)
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .imePadding()
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(getDeviceCornerRadius()),
                // 与页面背景同色；列表卡片（surfaceContainer）在其上自然凸显，
                // 并与左栏反转后的列表项容器色呼应（Android 15 设置同构）
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 0.dp,
                tonalElevation = 0.dp
            ) {
                Crossfade(
                    targetState = currentPage,
                    animationSpec = tween(SettingsPaneFadeMillis),
                    label = "settingsDetailPane",
                    modifier = Modifier.background(Color.Transparent)
                ) { page ->
                    // 内嵌子页面的 Scaffold/TopAppBar 默认自绘不透明 surface 背景，
                    // 会盖住面板底色——提供透明覆盖让面板背景变体色透出
                    CompositionLocalProvider(LocalSettingsContainerColor provides Color.Transparent) {
                        when (page) {
                        SettingsPage.General -> SettingsGeneralScreen(
                            onBack = {},
                            showBack = false,
                            onOpenHelp = onOpenHelp,
                            onOpenLanguage = onOpenLanguage,
                            onOpenFontScale = onOpenFontScale
                        )
                        SettingsPage.AI -> SettingsAIScreen(
                            onBack = {},
                            showBack = false,
                            onOpenHelp = onOpenHelp,
                            onAddConfig = { onOpenAiEdit(null) },
                            onEditConfig = { configId -> onOpenAiEdit(configId) }
                        )
                        SettingsPage.Notifications -> SettingsNotificationsScreen(
                            onBack = {},
                            showBack = false,
                            onOpenHelp = onOpenHelp,
                            onNavigateTemplate = { push(SettingsPage.Template) }
                        )
                        SettingsPage.Template -> SettingsTemplateScreen(
                            onBack = { pop() },
                            onOpenHelp = onOpenHelp
                        )
                        SettingsPage.About -> SettingsAboutDetailScreen(
                            onBack = {},
                            showBack = false,
                            onOpenHelp = onOpenHelp,
                            onNavigateDebug = { push(SettingsPage.Debug) },
                            onOpenLicenses = { push(SettingsPage.Licenses) }
                        )
                        SettingsPage.Debug -> SettingsDebugScreen(
                            onBack = { pop() },
                            onOpenHelp = onOpenHelp,
                            onNavigateDebugReminder = { push(SettingsPage.DebugReminder) },
                            onNavigateDebugVibration = { push(SettingsPage.DebugVibration) },
                            onNavigateUICatalog = { push(SettingsPage.UICatalog) }
                        )
                        SettingsPage.DebugReminder -> SettingsDebugReminderScreen(
                            onBack = { pop() },
                            onOpenHelp = onOpenHelp
                        )
                        SettingsPage.DebugVibration -> SettingsDebugVibrationScreen(
                            onBack = { pop() },
                            onOpenHelp = onOpenHelp
                        )
                        SettingsPage.UICatalog -> SettingsUICatalogScreen(
                            onBack = { pop() },
                            onOpenHelp = onOpenHelp
                        )
                        SettingsPage.Licenses -> {
                            // 不能用 SettingsScaffold（内部 Column+verticalScroll 与 LazyColumn 冲突）
                            val containerOverride = LocalSettingsContainerColor.current
                            Scaffold(
                                modifier = Modifier.fillMaxSize(),
                                containerColor = containerOverride ?: MaterialTheme.colorScheme.surface,
                                topBar = {
                                    TopAppBar(
                                        title = {
                                            Text(
                                                text = stringResource(id = R.string.settings_open_source_licenses),
                                                style = MaterialTheme.typography.titleLarge
                                            )
                                        },
                                        navigationIcon = {
                                            IconButton(onClick = { pop() }) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                    contentDescription = stringResource(id = R.string.settings_back)
                                                )
                                            }
                                        },
                                        colors = TopAppBarDefaults.topAppBarColors(
                                            containerColor = containerOverride ?: MaterialTheme.colorScheme.surface,
                                            scrolledContainerColor = containerOverride ?: MaterialTheme.colorScheme.surface
                                        )
                                    )
                                }
                            ) { innerPadding ->
                                OpenSourceLicensesContent(modifier = Modifier.padding(innerPadding))
                            }
                        }
                    }
                    }
                }
            }
        }
    }
}
