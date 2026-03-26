package io.github.darrindeyoung791.habitpulse.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.darrindeyoung791.habitpulse.HabitPulseApplication
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.data.model.Habit
import io.github.darrindeyoung791.habitpulse.data.model.RepeatCycle
import io.github.darrindeyoung791.habitpulse.ui.theme.HabitPulseTheme
import io.github.darrindeyoung791.habitpulse.ui.utils.rememberDebounceClickHandler
import io.github.darrindeyoung791.habitpulse.viewmodel.HabitViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

enum class HomeSection { Habits, Contacts, Records }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onCreateHabit: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onEditHabit: (Habit) -> Unit,
    application: HabitPulseApplication? = null,
    onHomeDataLoaded: () -> Unit = {}
) {
    // 防重复点击处理器
    val clickHandler = rememberDebounceClickHandler()
    val scope = rememberCoroutineScope()

    // Focus requester for TalkBack initial focus
    val titleFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }

    // 获取 ViewModel - 使用 Application 中的单例
    val viewModel: HabitViewModel = if (application != null) {
        application.habitViewModel
    } else {
        // Preview mode: create a ViewModel with fake in-memory repository
        remember {
            val fakeHabitDao = FakeHabitDao()
            val fakeCompletionDao = FakeHabitCompletionDao()
            val fakeRepository = io.github.darrindeyoung791.habitpulse.data.repository.HabitRepository(fakeHabitDao, fakeCompletionDao)
            HabitViewModel(fakeRepository)
        }
    }

    // 收集习惯列表状态（使用空列表作为初始值，但会通过 isLoading 控制显示）
    val habits by viewModel.habitsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    // 收集加载状态
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle(initialValue = true)
    // 收集新添加的习惯 ID（用于触发动画）
    val newlyAddedHabitId by viewModel.newlyAddedHabitId.collectAsStateWithLifecycle(initialValue = null)

    // 当数据加载完成时，通知调用方
    LaunchedEffect(isLoading) {
        if (!isLoading) {
            onHomeDataLoaded()
        }
    }

    // 当页面首次加载时，请求焦点到 title
    LaunchedEffect(Unit) {
        // 延迟一小段时间确保 UI 已经渲染完成
        delay(500)
        // 请求焦点到 title
        titleFocusRequester.requestFocus()
    }

    // 当新习惯添加后，延迟触发动画（等待导航动画完成）
    LaunchedEffect(newlyAddedHabitId) {
        if (newlyAddedHabitId != null) {
            // 延迟 300ms 让导航返回动画完成，然后重置 ID 触发卡片进入动画
            delay(300)
            viewModel.resetNewlyAddedHabitId()
        }
    }

    val configuration = LocalConfiguration.current
    var screenWidthDp = configuration.screenWidthDp
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // 获取用户偏好设置
    val userPreferences = application?.let {
        remember { io.github.darrindeyoung791.habitpulse.data.preferences.UserPreferences.getInstance(it) }
    }
    val forceTabletLandscape: Boolean by userPreferences?.forceTabletLandscapeFlow?.collectAsStateWithLifecycle(initialValue = false) ?: remember { mutableStateOf(false) }

    // 如果启用了强制平板横屏模式，将屏幕宽度视为 ≥1200dp
    if (forceTabletLandscape && isLandscape && screenWidthDp < 1200) {
        screenWidthDp = 1200
    }

    // Navigation mode decision logic:
    // - Tablet landscape (≥1200dp): PermanentNavigationDrawer with hamburger menu
    // - Phone landscape (<1200dp): NavigationRail
    // - All portrait modes: BottomNavigationBar
    val isPermanentDrawer = screenWidthDp >= 1200 && isLandscape
    val useRail = screenWidthDp < 1200 && isLandscape
    val useBottomBar = !isPermanentDrawer && !useRail

    var currentSection by rememberSaveable { mutableStateOf(HomeSection.Habits) }
    var isDrawerExpanded by rememberSaveable { mutableStateOf(true) }

    // 为每个 Section 保存独立的滚动状态
    val habitsScrollState = remember { androidx.compose.foundation.lazy.LazyListState() }
    val recordsScrollState = remember { androidx.compose.foundation.lazy.LazyListState() }

    // Animated drawer width
    val animatedDrawerWidth by animateDpAsState(
        targetValue = if (isDrawerExpanded) 240.dp else 80.dp,
        animationSpec = tween(300),
        label = "drawerWidth"
    )

    // 为每个 Section 使用独立的 scrollBehavior
    val habitsScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val recordsScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    // 根据当前 Section 切换 scrollBehavior
    val currentScrollBehavior = when (currentSection) {
        HomeSection.Habits -> habitsScrollBehavior
        HomeSection.Records -> recordsScrollBehavior
        else -> TopAppBarDefaults.pinnedScrollBehavior()
    }

    val displayCutout = WindowInsets.displayCutout
    val layoutDirection = LocalLayoutDirection.current
    val isRailCutoutLeft = useRail && displayCutout.getLeft(LocalDensity.current, layoutDirection) > 0
    val isRailCutoutRight = useRail && displayCutout.getRight(LocalDensity.current, layoutDirection) > 0

    val sectionItems = listOf(
        HomeSection.Habits,
        HomeSection.Contacts,
        HomeSection.Records
    )

    val navigateToSection: (HomeSection) -> Unit = { targetSection ->
        if (currentSection == targetSection) {
            // 如果点击的是当前页面，滚动到顶部
            when (targetSection) {
                HomeSection.Habits -> {
                    scope.launch {
                        habitsScrollState.animateScrollToItem(0)
                    }
                }
                HomeSection.Records -> {
                    scope.launch {
                        recordsScrollState.animateScrollToItem(0)
                    }
                }
                HomeSection.Contacts -> {
                    // Contacts 页面没有滚动列表，不做处理
                }
            }
        } else {
            // 切换到不同页面
            currentSection = targetSection
        }
    }

    // 主页主体内容 - 不再接收 nestedScrollConn，由各组件自己处理
    val homeBody: @Composable (Modifier) -> Unit = { modifier ->
        when (currentSection) {
            HomeSection.Habits -> {
                if (isLoading) {
                    Box(
                        modifier = modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (habits.isEmpty()) {
                    EmptyStateContent(
                        modifier = modifier,
                        onCreateHabit = {
                            scope.launch {
                                clickHandler.processClick {
                                    onCreateHabit()
                                }
                            }
                        }
                    )
                } else {
                    HabitListContent(
                        modifier = modifier,
                        habits = habits,
                        onHabitClick = { onEditHabit(it) },
                        onCheckIn = { viewModel.incrementCompletionCount(it) },
                        onUndoCompletion = { viewModel.undoHabitCompletion(it) },
                        onDeleteHabit = { viewModel.deleteHabit(it) },
                        nestedScrollConnection = habitsScrollBehavior.nestedScrollConnection,
                        newlyAddedHabitId = newlyAddedHabitId,
                        listState = habitsScrollState,
                        forceTabletLandscape = forceTabletLandscape == true
                    )
                }
            }
            HomeSection.Contacts -> {
                BlankSectionContent(
                    modifier = modifier,
                    title = stringResource(id = R.string.main_tab_contacts),
                    description = stringResource(id = R.string.main_blank_contacts_description)
                )
            }
            HomeSection.Records -> {
                RecordsScreenContent(
                    modifier = modifier,
                    application = application,
                    scrollBehavior = recordsScrollBehavior,
                    listState = recordsScrollState
                )
            }
        }
    }

    val topAppBarContent: @Composable (Boolean) -> Unit = { isRailVisible ->
        val currentTitle = when (currentSection) {
            HomeSection.Habits -> stringResource(id = R.string.main_title_habits)
            HomeSection.Contacts -> stringResource(id = R.string.main_title_contacts)
            HomeSection.Records -> stringResource(id = R.string.main_title_records)
        }

        // 根据当前 Section 选择正确的 scrollBehavior
        val currentTopAppBarScrollBehavior = when (currentSection) {
            HomeSection.Records -> recordsScrollBehavior
            HomeSection.Habits -> habitsScrollBehavior
            else -> TopAppBarDefaults.pinnedScrollBehavior()
        }

        if (isRailVisible) {
            // Phone landscape: use TopAppBar
            // TopAppBar handles its own insets (top for status bar, end for camera if needed)
            TopAppBar(
                windowInsets = WindowInsets.safeDrawing.only(
                    if (isRailCutoutRight) WindowInsetsSides.Top + WindowInsetsSides.End else WindowInsetsSides.Top
                ),
                title = {
                    Text(
                        text = currentTitle,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .focusRequester(titleFocusRequester)
                            .focusable()
                            .semantics {
                                heading()
                            }
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                clickHandler.processClick {
                                    onNavigateToSettings()
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(id = R.string.main_settings)
                        )
                    }
                },
                scrollBehavior = currentTopAppBarScrollBehavior
            )
        } else {
            // Other modes: use LargeTopAppBar with exitUntilCollapsed behavior
            LargeTopAppBar(
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top),
                title = {
                    Column {
                        // Main title - animate font size based on scroll state
                        val collapsedFraction = currentScrollBehavior.state.collapsedFraction
                        val currentTextStyle = if (collapsedFraction < 0.5f) {
                            MaterialTheme.typography.headlineLarge
                        } else {
                            MaterialTheme.typography.titleLarge
                        }
                        Text(
                            text = currentTitle,
                            style = currentTextStyle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .focusRequester(titleFocusRequester)
                                .focusable()
                                .semantics {
                                    heading()
                                }
                        )
                        // Subtitle - only show for Habits section when expanded
                        if (currentSection == HomeSection.Habits && collapsedFraction < 0.5f) {
                            Text(
                                text = stringResource(id = R.string.main_subtitle_habit_count, habits.size),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                clickHandler.processClick {
                                    onNavigateToSettings()
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(id = R.string.main_settings)
                        )
                    }
                },
                scrollBehavior = currentScrollBehavior,
                modifier = Modifier.nestedScroll(currentScrollBehavior.nestedScrollConnection)
            )
        }
    }

    val showFab = currentSection == HomeSection.Habits
    val newHabitLabel = stringResource(id = R.string.main_new_habit)

    if (isPermanentDrawer) {
        PermanentNavigationDrawer(
            drawerContent = {
                PermanentDrawerSheet(
                    modifier = Modifier
                        .width(animatedDrawerWidth)
                        .fillMaxHeight()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Start))
                ) {
                    // Drawer header with menu/collapse button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            contentAlignment = if (isDrawerExpanded) Alignment.CenterEnd else Alignment.Center
                        ) {
                            IconButton(
                                onClick = { isDrawerExpanded = !isDrawerExpanded },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = if (isDrawerExpanded) Icons.Filled.ChevronLeft else Icons.Filled.Menu,
                                    contentDescription = if (isDrawerExpanded) stringResource(id = R.string.main_collapse_drawer) else stringResource(id = R.string.main_expand_drawer)
                                )
                            }
                        }
                    }

                    // Navigation items - use consistent layout for both states
                    // to prevent icon size changes during animation
                    sectionItems.forEach { section ->
                        val isSelected = currentSection == section

                        if (isDrawerExpanded) {
                            // Expanded state: full NavigationDrawerItem with label
                            NavigationDrawerItem(
                                label = { Text(text = when (section) {
                                    HomeSection.Habits -> stringResource(id = R.string.main_tab_habits)
                                    HomeSection.Contacts -> stringResource(id = R.string.main_tab_contacts)
                                    HomeSection.Records -> stringResource(id = R.string.main_tab_records)
                                }) },
                                icon = {
                                    Icon(
                                        imageVector = when (section) {
                                            HomeSection.Habits -> Icons.Filled.List
                                            HomeSection.Contacts -> Icons.Filled.People
                                            HomeSection.Records -> Icons.Filled.Assessment
                                        },
                                        contentDescription = null
                                    )
                                },
                                selected = isSelected,
                                onClick = { navigateToSection(section) },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                        } else {
                            // Collapsed state: use same Box structure as CollapsedNavigationBar
                            // to ensure consistent icon size and centering
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 16.dp)
                                    .then(
                                        if (isSelected) {
                                            Modifier
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .size(56.dp)
                                    .clickable(onClick = { navigateToSection(section) }),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (section) {
                                        HomeSection.Habits -> Icons.Filled.List
                                        HomeSection.Contacts -> Icons.Filled.People
                                        HomeSection.Records -> Icons.Filled.Assessment
                                    },
                                    contentDescription = when (section) {
                                        HomeSection.Habits -> stringResource(id = R.string.main_tab_habits)
                                        HomeSection.Contacts -> stringResource(id = R.string.main_tab_contacts)
                                        HomeSection.Records -> stringResource(id = R.string.main_tab_records)
                                    },
                                    tint = if (isSelected) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
            }
        ) {
            // Tablet landscape mode
            // Drawer handles start inset, Scaffold handles top and end insets
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = { topAppBarContent(false) },
                floatingActionButton = {
                    if (showFab) {
                        ExtendedFloatingActionButton(
                            onClick = {
                                scope.launch {
                                    clickHandler.processClick {
                                        onCreateHabit()
                                    }
                                }
                            },
                            icon = {
                                Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                            },
                            text = { Text(text = newHabitLabel) },
                            modifier = Modifier
                                .semantics { contentDescription = newHabitLabel }
                        )
                    }
                },
                // Scaffold handles top and end insets (start is handled by drawer)
                contentWindowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.End
                )
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    homeBody(Modifier.fillMaxSize())
                }
            }
        }
    } else if (useRail) {
        // NavigationRail layout for landscape phones (<1200dp)
        // Rail occupies full height on left, content area on right
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // NavigationRail - fixed on left side
            // Handles start inset for camera cutout
            NavigationRail(
                modifier = Modifier
                    .fillMaxHeight()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Start)),
                containerColor = Color.Transparent
            ) {
                sectionItems.forEach { section ->
                    NavigationRailItem(
                        icon = {
                            Icon(
                                imageVector = when (section) {
                                    HomeSection.Habits -> Icons.Filled.List
                                    HomeSection.Contacts -> Icons.Filled.People
                                    HomeSection.Records -> Icons.Filled.Assessment
                                },
                                contentDescription = when (section) {
                                    HomeSection.Habits -> stringResource(id = R.string.main_tab_habits)
                                    HomeSection.Contacts -> stringResource(id = R.string.main_tab_contacts)
                                    HomeSection.Records -> stringResource(id = R.string.main_tab_records)
                                }
                            )
                        },
                        label = {
                            Text(text = when (section) {
                                HomeSection.Habits -> stringResource(id = R.string.main_tab_habits)
                                HomeSection.Contacts -> stringResource(id = R.string.main_tab_contacts)
                                HomeSection.Records -> stringResource(id = R.string.main_tab_records)
                            })
                        },
                        selected = currentSection == section,
                        onClick = { navigateToSection(section) }
                    )
                }
            }

            // Content area on right side
            // TopAppBar handles its own top inset
            // Content handles end inset if camera cutout is on right
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                // TopAppBar handles its own insets via windowInsets parameter
                topAppBarContent(true)

                // Scrollable content area
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    homeBody(
                        Modifier.fillMaxSize().then(
                            if (isRailCutoutRight) {
                                Modifier.windowInsetsPadding(
                                    WindowInsets.safeDrawing.only(WindowInsetsSides.End)
                                )
                            } else {
                                Modifier
                            }
                        )
                    )

                    // FAB - floating above content
                    if (showFab) {
                        ExtendedFloatingActionButton(
                            onClick = {
                                scope.launch {
                                    clickHandler.processClick {
                                        onCreateHabit()
                                    }
                                }
                            },
                            icon = {
                                Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                            },
                            text = { Text(text = newHabitLabel) },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .windowInsetsPadding(
                                    WindowInsets.safeDrawing.only(
                                        if (isRailCutoutRight) WindowInsetsSides.Bottom + WindowInsetsSides.End else WindowInsetsSides.Bottom
                                    )
                                )
                                .padding(16.dp)
                                .semantics { contentDescription = newHabitLabel }
                        )
                    }
                }
            }
        }
    } else {
        // Bottom Navigation Bar layout for portrait modes
        Scaffold(
            modifier = Modifier
                .fillMaxSize(),
            topBar = { topAppBarContent(false) },
            bottomBar = {
                if (useBottomBar) {
                    // Bottom Navigation Bar for portrait modes
                    // Use surface color to match system navigation bar color
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        sectionItems.forEach { section ->
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = when (section) {
                                            HomeSection.Habits -> Icons.Filled.List
                                            HomeSection.Contacts -> Icons.Filled.People
                                            HomeSection.Records -> Icons.Filled.Assessment
                                        },
                                        contentDescription = when (section) {
                                            HomeSection.Habits -> stringResource(id = R.string.main_tab_habits)
                                            HomeSection.Contacts -> stringResource(id = R.string.main_tab_contacts)
                                            HomeSection.Records -> stringResource(id = R.string.main_tab_records)
                                        }
                                    )
                                },
                                label = { Text(text = when (section) {
                                    HomeSection.Habits -> stringResource(id = R.string.main_tab_habits)
                                    HomeSection.Contacts -> stringResource(id = R.string.main_tab_contacts)
                                    HomeSection.Records -> stringResource(id = R.string.main_tab_records)
                                }) },
                                selected = currentSection == section,
                                onClick = { navigateToSection(section) }
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (showFab) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            scope.launch {
                                clickHandler.processClick {
                                    onCreateHabit()
                                }
                            }
                        },
                        icon = {
                            Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                        },
                        text = { Text(text = newHabitLabel) },
                        modifier = Modifier
                            .semantics { contentDescription = newHabitLabel }
                    )
                }
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { paddingValues ->
            // Collapsed NavigationBar for tablet landscape
            if (isPermanentDrawer) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    CollapsedNavigationBar(
                        sectionItems = sectionItems,
                        currentSection = currentSection,
                        onNavigateToSection = navigateToSection,
                        habitsContentDescription = stringResource(id = R.string.main_tab_habits),
                        contactsContentDescription = stringResource(id = R.string.main_tab_contacts),
                        recordsContentDescription = stringResource(id = R.string.main_tab_records)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        homeBody(Modifier.fillMaxSize())
                    }
                }
            } else {
                // Bottom bar layout - content fills entire area
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    homeBody(Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
fun BlankSectionContent(
    modifier: Modifier = Modifier,
    title: String,
    description: String
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Collapsed NavigationBar for tablet landscape mode.
 * Appears as a vertical rail with circular selection indicator.
 */
@Composable
fun CollapsedNavigationBar(
    sectionItems: List<HomeSection>,
    currentSection: HomeSection,
    onNavigateToSection: (HomeSection) -> Unit,
    modifier: Modifier = Modifier,
    habitsContentDescription: String,
    contactsContentDescription: String,
    recordsContentDescription: String
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(112.dp),  // Increased width for better spacing
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        sectionItems.forEach { section ->
            val isSelected = currentSection == section

            // Use a single Box with consistent sizing
            Box(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .then(
                        if (isSelected) {
                            Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                        } else {
                            Modifier.size(56.dp)
                        }
                    )
                    .clickable(onClick = { onNavigateToSection(section) }),
                contentAlignment = Alignment.Center
            ) {
                // Icon with fixed size, centered in the Box
                Icon(
                    imageVector = when (section) {
                        HomeSection.Habits -> Icons.Filled.List
                        HomeSection.Contacts -> Icons.Filled.People
                        HomeSection.Records -> Icons.Filled.Assessment
                    },
                    contentDescription = when (section) {
                        HomeSection.Habits -> habitsContentDescription
                        HomeSection.Contacts -> contactsContentDescription
                        HomeSection.Records -> recordsContentDescription
                    },
                    tint = if (isSelected) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    // No explicit size modifier - Icon uses default 24dp
                )
            }
        }
    }
}

@Composable
fun EmptyStateContent(
    modifier: Modifier = Modifier,
    onCreateHabit: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.LibraryAdd,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(id = R.string.main_no_habits),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onCreateHabit) {
            Text(
                text = stringResource(id = R.string.main_create_habit),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun HabitListContent(
    modifier: Modifier = Modifier,
    habits: List<Habit>,
    onHabitClick: (Habit) -> Unit,
    onCheckIn: (Habit) -> Unit,
    onUndoCompletion: (Habit) -> Unit,
    onDeleteHabit: (Habit) -> Unit,
    nestedScrollConnection: androidx.compose.ui.input.nestedscroll.NestedScrollConnection? = null,
    newlyAddedHabitId: UUID? = null,
    listState: androidx.compose.foundation.lazy.LazyListState = remember { androidx.compose.foundation.lazy.LazyListState() },
    forceTabletLandscape: Boolean = false
) {
    val configuration = LocalConfiguration.current
    var screenWidthDp = configuration.screenWidthDp
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // 如果启用了强制平板横屏模式，将屏幕宽度视为 ≥1200dp
    if (forceTabletLandscape && isLandscape && screenWidthDp < 1200) {
        screenWidthDp = 1200
    }

    // Use staggered grid only for tablets in landscape (≥1200dp)
    val useStaggeredGrid = isLandscape && screenWidthDp >= 1200

    // Use consistent 16.dp horizontal padding to align with LargeTopAppBar title
    val horizontalPadding = 16.dp

    if (useStaggeredGrid) {
        // Waterfall layout with synchronized scrolling for landscape tablets
        // Split habits into two columns: odd and even indices
        val column1Habits = habits.filterIndexed { index, _ -> index % 2 == 0 }
        val column2Habits = habits.filterIndexed { index, _ -> index % 2 == 1 }

        val staggeredModifier = if (nestedScrollConnection != null) modifier.nestedScroll(nestedScrollConnection) else modifier
        
        // Use LazyColumn with listState for proper scroll control
        LazyColumn(
            modifier = staggeredModifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left column
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        column1Habits.forEach { habit ->
                            HabitCard(
                                habit = habit,
                                onClick = { onHabitClick(habit) },
                                onCheckIn = { onCheckIn(habit) },
                                onUndoCompletion = { onUndoCompletion(habit) },
                                onEditHabit = { onHabitClick(habit) },
                                onDeleteHabit = { onDeleteHabit(habit) },
                                isNewlyAdded = (habit.id == newlyAddedHabitId)
                            )
                        }
                    }

                    // Right column
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        column2Habits.forEach { habit ->
                            HabitCard(
                                habit = habit,
                                onClick = { onHabitClick(habit) },
                                onCheckIn = { onCheckIn(habit) },
                                onUndoCompletion = { onUndoCompletion(habit) },
                                onEditHabit = { onHabitClick(habit) },
                                onDeleteHabit = { onDeleteHabit(habit) },
                                isNewlyAdded = (habit.id == newlyAddedHabitId)
                            )
                        }
                    }
                }
            }
            // Add bottom spacer to prevent FAB from covering last item
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    } else {
        // Single column layout for phones and portrait mode
        val listModifier = if (nestedScrollConnection != null) modifier.nestedScroll(nestedScrollConnection) else modifier
        LazyColumn(
            modifier = listModifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = habits,
                key = { it.id.toString() },
                contentType = { "habitCard" }
            ) { habit ->
                HabitCard(
                    habit = habit,
                    onClick = { onHabitClick(habit) },
                    onCheckIn = { onCheckIn(habit) },
                    onUndoCompletion = { onUndoCompletion(habit) },
                    onEditHabit = { onHabitClick(habit) },
                    onDeleteHabit = { onDeleteHabit(habit) },
                    isNewlyAdded = (habit.id == newlyAddedHabitId)
                )
            }
            // Add bottom spacer to prevent FAB from covering last item
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitCard(
    habit: Habit,
    onClick: () -> Unit,
    onCheckIn: () -> Unit,
    onUndoCompletion: () -> Unit,
    onEditHabit: () -> Unit,
    onDeleteHabit: () -> Unit,
    modifier: Modifier = Modifier,
    isNewlyAdded: Boolean = false
) {
    var showMenu by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showNotesDialog by remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current

    val reminderTimes = habit.getReminderTimesList()
    val repeatDays = habit.getRepeatDaysList()

    // Animation states for enter/exit effects
    var isVisible by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    
    // For newly added habits, start invisible and animate in after delay
    var isInitiallyVisible by remember { mutableStateOf(!isNewlyAdded) }
    
    // Delay animation trigger for newly added habits
    LaunchedEffect(isNewlyAdded) {
        if (isNewlyAdded) {
            // Wait for navigation animation to complete before showing enter animation
            delay(50)
            isInitiallyVisible = true
        }
    }

    AnimatedVisibility(
        visible = isVisible && isInitiallyVisible,
        enter = fadeIn(animationSpec = spring(dampingRatio = 0.8f, stiffness = 200f)) +
                scaleIn(initialScale = 0.85f, animationSpec = spring(dampingRatio = 0.8f, stiffness = 200f)) +
                slideInVertically(initialOffsetY = { it / 4 }, animationSpec = spring(dampingRatio = 0.8f, stiffness = 200f)),
        exit = fadeOut(animationSpec = tween(150)) +
               scaleOut(targetScale = 0.85f, animationSpec = tween(150)) +
               slideOutVertically(targetOffsetY = { it / 4 }, animationSpec = tween(150)),
        modifier = modifier
            .animateContentSize(
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 200f)
            )
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
                    .combinedClickable(
                        onClick = {
                            onClick()
                            isVisible = true
                        },
                        onLongClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            showMenu = true
                        }
                    ),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左侧内容：习惯信息
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 习惯名称（大字体）
                        Text(
                            text = habit.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        // 已完成次数
                        Text(
                            text = stringResource(id = R.string.habit_card_completed_count, habit.completionCount),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // 重复周期和提醒时间
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Build day names map for reuse
                            val dayNames = listOf(
                                stringResource(id = R.string.habit_card_repeat_days_sunday),
                                stringResource(id = R.string.habit_card_repeat_days_monday),
                                stringResource(id = R.string.habit_card_repeat_days_tuesday),
                                stringResource(id = R.string.habit_card_repeat_days_wednesday),
                                stringResource(id = R.string.habit_card_repeat_days_thursday),
                                stringResource(id = R.string.habit_card_repeat_days_friday),
                                stringResource(id = R.string.habit_card_repeat_days_saturday)
                            )
                            val daySeparator = stringResource(id = R.string.habit_card_repeat_days_separator)
                            val repeatCycleText = when (habit.repeatCycle) {
                                RepeatCycle.DAILY -> stringResource(id = R.string.habit_card_repeat_daily)
                                RepeatCycle.WEEKLY -> {
                                    val daysText = repeatDays.joinToString(daySeparator) { dayIndex ->
                                        dayNames.getOrElse(dayIndex) { "" }
                                    }
                                    stringResource(id = R.string.habit_card_repeat_days_format, daysText)
                                }
                            }
                            val reminderText = if (reminderTimes.isEmpty()) {
                                ""
                            } else {
                                val firstTime = reminderTimes.first()
                                if (reminderTimes.size == 1) {
                                    stringResource(id = R.string.habit_card_reminder_single, firstTime)
                                } else {
                                    stringResource(
                                        id = R.string.habit_card_reminder_count,
                                        firstTime,
                                        reminderTimes.size
                                    )
                                }
                            }
                            val repeatInfoText = if (reminderText.isNotEmpty()) {
                                "$repeatCycleText · $reminderText"
                            } else {
                                repeatCycleText
                            }
                            Text(
                                text = repeatInfoText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.combinedClickable(
                                    onClick = { showReminderDialog = true },
                                    onLongClick = {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        showMenu = true
                                    }
                                )
                            )
                        }

                        // 备注信息（仅当有备注时显示）
                        if (habit.notes.isNotBlank()) {
                            Text(
                                text = formatNotesPreview(habit.notes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.combinedClickable(
                                    onClick = { showNotesDialog = true },
                                    onLongClick = {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        showMenu = true
                                    }
                                )
                            )
                        }
                    }

                    // 右侧：打卡按钮（垂直居中）
                    CheckInButton(
                        onClick = onCheckIn
                    )
                }
            }

            // 长按下拉菜单
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                // 撤销打卡（当已完成次数大于 0 时显示）
                if (habit.completionCount > 0) {
                    DropdownMenuItem(
                        text = {
                            Text(text = stringResource(id = R.string.habit_card_menu_undo))
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Undo,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        onClick = {
                            onUndoCompletion()
                            showMenu = false
                        }
                    )
                }

                // 编辑
                DropdownMenuItem(
                    text = {
                        Text(text = stringResource(id = R.string.habit_card_menu_edit))
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    onClick = {
                        onEditHabit()
                        showMenu = false
                    }
                )

                // 删除
                DropdownMenuItem(
                    text = {
                        Text(text = stringResource(id = R.string.habit_card_menu_delete))
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    onClick = {
                        // Start exit animation first, then call actual delete after animation completes
                        isVisible = false
                        scope.launch {
                            kotlinx.coroutines.delay(150) // Wait for exit animation to complete
                            onDeleteHabit()
                        }
                        showMenu = false
                    }
                )
            }

            // 提醒详情对话框
            if (showReminderDialog) {
                ReminderDetailDialog(
                    reminderTimes = reminderTimes,
                    repeatCycle = habit.repeatCycle,
                    repeatDays = repeatDays,
                    onDismiss = { showReminderDialog = false }
                )
            }

            // 备注详情对话框
            if (showNotesDialog) {
                NotesDetailDialog(
                    notes = habit.notes,
                    onDismiss = { showNotesDialog = false }
                )
            }
        }
    }
}

private fun formatNotesPreview(notes: String): String {
    val lines = notes.lines().take(3).joinToString("\n")
    return if (notes.lines().size > 3) {
        "$lines…"
    } else {
        lines
    }
}

@Composable
fun CheckInButton(
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val cornerRadius by animateFloatAsState(
        targetValue = if (isPressed) 20f else 12f,
        animationSpec = tween(200),
        label = "cornerRadius"
    )
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator }

    Button(
        onClick = {
            isPressed = true
            // Vibrate for 50ms
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(50, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
            onClick()
        },
        modifier = Modifier
            .size(56.dp)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        isPressed = event.changes.any { it.pressed }
                    }
                }
            },
        shape = RoundedCornerShape(cornerRadius.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Check,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(0.7f)
        )
    }
}

@Composable
fun ReminderDetailDialog(
    reminderTimes: List<String>,
    repeatCycle: RepeatCycle,
    repeatDays: List<Int>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(id = R.string.habit_card_reminder_dialog_title))
        },
        text = {
            Column(
                modifier = Modifier
                    .widthIn(max = 350.dp)
                    .fillMaxWidth()
                    .heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.75f)
                    .verticalScroll(rememberScrollState())
            ) {
                // 重复周期
                // Build day names map for reuse
                val dayNames = listOf(
                    stringResource(id = R.string.habit_card_repeat_days_sunday),
                    stringResource(id = R.string.habit_card_repeat_days_monday),
                    stringResource(id = R.string.habit_card_repeat_days_tuesday),
                    stringResource(id = R.string.habit_card_repeat_days_wednesday),
                    stringResource(id = R.string.habit_card_repeat_days_thursday),
                    stringResource(id = R.string.habit_card_repeat_days_friday),
                    stringResource(id = R.string.habit_card_repeat_days_saturday)
                )
                val daySeparator = stringResource(id = R.string.habit_card_repeat_days_separator)
                val repeatCycleText = when (repeatCycle) {
                    RepeatCycle.DAILY -> stringResource(id = R.string.habit_card_repeat_daily)
                    RepeatCycle.WEEKLY -> {
                        val daysText = repeatDays.joinToString(daySeparator) { dayIndex ->
                            dayNames.getOrElse(dayIndex) { "" }
                        }
                        stringResource(id = R.string.habit_card_repeat_days_format, daysText)
                    }
                }
                Text(
                    text = repeatCycleText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 提醒时间列表
                if (reminderTimes.isNotEmpty()) {
                    Text(
                        text = stringResource(id = R.string.create_habit_reminder_time_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    reminderTimes.forEach { time ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = time,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                } else {
                    Text(
                        text = stringResource(id = R.string.create_habit_no_reminder_set),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.habit_card_reminder_dialog_dismiss))
            }
        }
    )
}

@Composable
fun NotesDetailDialog(
    notes: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(id = R.string.habit_card_notes_dialog_title))
        },
        text = {
            Column(
                modifier = Modifier
                    .widthIn(max = 350.dp)
                    .fillMaxWidth()
                    .heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.75f)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.habit_card_notes_dialog_dismiss))
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HabitPulseTheme {
        HomeScreen(
            onCreateHabit = {},
            onNavigateToSettings = {},
            onEditHabit = {},
            application = null,  // Use preview mode with fake data
            onHomeDataLoaded = {}
        )
    }
}

/**
 * Fake HabitDao implementation for Android Studio Preview.
 * Provides in-memory storage with sample data for UI preview.
 */
@Suppress("unused")
private class FakeHabitDao : io.github.darrindeyoung791.habitpulse.data.database.dao.HabitDao {
    private val habits = mutableListOf<io.github.darrindeyoung791.habitpulse.data.model.Habit>()

    init {
        // Add sample data for preview
        habits.add(
            io.github.darrindeyoung791.habitpulse.data.model.Habit(
                title = "每天喝水",
                completedToday = false,
                completionCount = 15,
                repeatCycle = RepeatCycle.DAILY,
                reminderTimes = "[\"08:00\",\"12:00\",\"20:00\"]",
                notes = "记得每次喝水时要慢慢喝"
            )
        )
        habits.add(
            io.github.darrindeyoung791.habitpulse.data.model.Habit(
                title = "晨跑",
                completedToday = true,
                completionCount = 30,
                repeatCycle = RepeatCycle.WEEKLY,
                repeatDays = "[1,3,5]",
                reminderTimes = "[\"06:00\"]",
                notes = "跑步前记得热身"
            )
        )
        habits.add(
            io.github.darrindeyoung791.habitpulse.data.model.Habit(
                title = "阅读",
                completedToday = false,
                completionCount = 5,
                repeatCycle = RepeatCycle.DAILY,
                reminderTimes = "[\"21:00\"]",
                notes = "每天至少读 30 分钟"
            )
        )
    }

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
}

@Preview
@Composable
fun HabitCardPreview() {
    HabitPulseTheme {
        HabitCard(
            habit = Habit(
                title = "每天喝水",
                completedToday = false,
                completionCount = 15,
                repeatCycle = RepeatCycle.DAILY,
                reminderTimes = "[\"08:00\",\"12:00\",\"20:00\"]",
                notes = "记得每次喝水时要慢慢喝\n不要一口气喝完\n最好喝温水"
            ),
            onClick = {},
            onCheckIn = {},
            onUndoCompletion = {},
            onEditHabit = {},
            onDeleteHabit = {}
        )
    }
}

@Preview
@Composable
fun HabitCardCompletedPreview() {
    HabitPulseTheme {
        HabitCard(
            habit = Habit(
                title = "晨跑",
                completedToday = true,
                completionCount = 30,
                repeatCycle = RepeatCycle.WEEKLY,
                repeatDays = "[1,3,5]",
                reminderTimes = "[\"06:00\"]",
                notes = "跑步前记得热身\n跑完后要拉伸"
            ),
            onClick = {},
            onCheckIn = {},
            onUndoCompletion = {},
            onEditHabit = {},
            onDeleteHabit = {}
        )
    }
}

@Preview
@Composable
fun HabitCardWithNotesPreview() {
    HabitPulseTheme {
        HabitCard(
            habit = Habit(
                title = "阅读",
                completedToday = false,
                completionCount = 5,
                repeatCycle = RepeatCycle.DAILY,
                reminderTimes = "[\"21:00\"]",
                notes = "每天至少读 30 分钟\n记录读书笔记\n分享读书心得"
            ),
            onClick = {},
            onCheckIn = {},
            onUndoCompletion = {},
            onEditHabit = {},
            onDeleteHabit = {}
        )
    }
}

/**
 * Fake HabitCompletionDao implementation for Android Studio Preview.
 * Provides in-memory storage with sample completion records for UI preview.
 */
@Suppress("unused")
private class FakeHabitCompletionDao : io.github.darrindeyoung791.habitpulse.data.database.dao.HabitCompletionDao {
    private val completions = mutableListOf<io.github.darrindeyoung791.habitpulse.data.model.HabitCompletion>()

    override fun getCompletionsByHabitIdFlow(habitId: java.util.UUID): kotlinx.coroutines.flow.Flow<List<io.github.darrindeyoung791.habitpulse.data.model.HabitCompletion>> {
        return kotlinx.coroutines.flow.flowOf(completions.filter { it.habitId == habitId })
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
