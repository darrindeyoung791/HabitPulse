package io.github.darrindeyoung791.habitpulse.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import android.util.Log
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.*
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.activity.compose.BackHandler
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.darrindeyoung791.habitpulse.HabitPulseApplication
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.data.model.Habit
import io.github.darrindeyoung791.habitpulse.data.model.RepeatCycle
import io.github.darrindeyoung791.habitpulse.ui.theme.HabitPulseTheme
import io.github.darrindeyoung791.habitpulse.ui.utils.rememberDebounceClickHandler
import io.github.darrindeyoung791.habitpulse.ui.screens.DateFilterButton
import io.github.darrindeyoung791.habitpulse.viewmodel.HabitViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

enum class HomeSection { Habits, Contacts, Records }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onCreateHabit: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onEditHabit: (Habit) -> Unit,
    onNavigateToMultiSelect: (habitId: UUID) -> Unit = {},
    application: HabitPulseApplication? = null,
    onHomeDataLoaded: () -> Unit = {}
) {
    val context = LocalContext.current
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
            val fakeOnboardingPreferences = io.github.darrindeyoung791.habitpulse.utils.OnboardingPreferences(context.applicationContext)
            HabitViewModel(fakeRepository, fakeOnboardingPreferences)
        }
    }

    // 收集习惯列表状态（使用空列表作为初始值，但会通过 isLoading 控制显示）
    val habits by viewModel.habitsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    // 收集搜索后的习惯列表
    val filteredHabits by viewModel.filteredHabitsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    // 收集加载状态
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle(initialValue = true)

    // 记录是否已加载过一次习惯数据，用于避免场景切换时闪烁“暂无习惯”空页面
    var hasLoadedHabits by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isLoading) {
        if (!isLoading) {
            hasLoadedHabits = true
        }
    }
    // 收集新添加的习惯 ID（用于触发动画）
    val newlyAddedHabitId by viewModel.newlyAddedHabitId.collectAsStateWithLifecycle(initialValue = null)
    // 收集搜索关键词
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle(initialValue = "")

    // 收集联系人列表状态（用于 Contacts section 的副标题）
    val allContacts by (application?.contactsViewModel?.allContactsFlow ?: kotlinx.coroutines.flow.flowOf(emptyList())).collectAsStateWithLifecycle(initialValue = emptyList())

    // 搜索状态（是否正在搜索）
    var isSearchActive by remember { mutableStateOf(false) }
    // 搜索焦点状态
    var isSearchFocused by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // 联系人搜索状态
    var contactsSearchQuery by remember { mutableStateOf("") }
    var isContactsSearchActive by remember { mutableStateOf(false) }
    val contactsFocusManager = LocalFocusManager.current

    // 当搜索激活时，拦截系统返回键
    BackHandler(enabled = isSearchActive || isContactsSearchActive) {
        if (isContactsSearchActive) {
            isContactsSearchActive = false
            contactsFocusManager.clearFocus()
        } else {
            isSearchActive = false
            viewModel.clearSearch()
            focusManager.clearFocus()
        }
    }

    // 当退出搜索模式时，清除搜索关键词
    LaunchedEffect(isSearchActive) {
        if (!isSearchActive) {
            viewModel.clearSearch()
            focusManager.clearFocus()
        }
    }

    // 当退出联系人搜索时，清除搜索关键词
    LaunchedEffect(isContactsSearchActive) {
        if (!isContactsSearchActive) {
            contactsSearchQuery = ""
            application?.contactsViewModel?.clearSearch()
            contactsFocusManager.clearFocus()
        }
    }

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
    val screenWidthDp = configuration.screenWidthDp
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    // Use smallestScreenWidthDp to detect device type (independent of orientation)
    // Tablet: smallestScreenWidthDp >= 600dp
    // Phone: smallestScreenWidthDp < 600dp
    val smallestScreenWidthDp = configuration.smallestScreenWidthDp
    val isTabletDevice = smallestScreenWidthDp >= 600

    // 获取用户偏好设置
    val userPreferences = application?.let {
        remember { io.github.darrindeyoung791.habitpulse.data.preferences.UserPreferences.getInstance(it) }
    }
    val forceTabletLandscape: Boolean by userPreferences?.forceTabletLandscapeFlow?.collectAsStateWithLifecycle(initialValue = false) ?: remember { mutableStateOf(false) }

    // Navigation mode decision logic:
    // - Tablet in landscape: PermanentNavigationDrawer with hamburger menu
    // - Phone in landscape: NavigationRail
    // - All portrait modes: BottomNavigationBar
    val isPermanentDrawer = isTabletDevice && isLandscape
    val useRail = !isTabletDevice && isLandscape
    val useBottomBar = !isPermanentDrawer && !useRail

    // Fallback: forceTabletLandscape for edge cases
    val effectiveIsPermanentDrawer = if (forceTabletLandscape && isLandscape && !isTabletDevice) {
        true
    } else {
        isPermanentDrawer
    }
    val effectiveUseRail = if (forceTabletLandscape && isLandscape && !isTabletDevice) {
        false
    } else {
        useRail
    }

    // Detect waterfall mode (tablet landscape dual-column layout)
    // This is critical for scroll-to-top functionality
    val isWaterfallMode = isLandscape && screenWidthDp >= 840

    var currentSection by rememberSaveable { mutableStateOf(HomeSection.Habits) }
    var isDrawerExpanded by rememberSaveable { mutableStateOf(true) }

    // Reset Records filter when navigating to Records section
    // This ensures users always see all records when entering Records screen
    LaunchedEffect(currentSection) {
        if (currentSection == HomeSection.Records) {
            application?.recordsViewModel?.selectHabit(null)
        }
    }

    // 为每个 Section 保存独立的滚动状态
    // 使用 rememberSaveable 让导航进出后保持滚动位置（取消/返回不应回到顶部）
    val habitsScrollState = rememberSaveable(saver = androidx.compose.foundation.lazy.LazyListState.Saver) {
        androidx.compose.foundation.lazy.LazyListState()
    }
    // 使用 remember 而非 rememberSaveable 来避免 waterfall 滚动状态在重组时出现问题
    // Fix: rememberSaveable may not work reliably with ScrollState.Saver in waterfall layout
    val waterfallScrollState = remember {
        ScrollState(0)
    }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val recordsScrollState = rememberSaveable(saver = androidx.compose.foundation.lazy.LazyListState.Saver) {
        androidx.compose.foundation.lazy.LazyListState()
    }
    val contactsScrollState = rememberSaveable(saver = androidx.compose.foundation.lazy.LazyListState.Saver) {
        androidx.compose.foundation.lazy.LazyListState()
    }

    // Animated drawer width
    val animatedDrawerWidth by animateDpAsState(
        targetValue = if (isDrawerExpanded) 240.dp else 80.dp,
        animationSpec = tween(300),
        label = "drawerWidth"
    )

    // 为每个 Section 使用独立的 scrollBehavior
    val habitsScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val recordsScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val contactsScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // 监听 scrollToTop 请求，滚动到顶部并展开 AppBar
    val scrollToTop by viewModel.scrollToTop.collectAsStateWithLifecycle(initialValue = 0)

    LaunchedEffect(scrollToTop) {
        if (scrollToTop > 0) {
            // Expand AppBar first then scroll to top (ensure AppBar doesn't push content down after scroll)
            habitsScrollBehavior.state.heightOffset = 0f
            scope.launch {
                // CRITICAL: Only scroll the appropriate scroll state based on layout mode
                if (isWaterfallMode) {
                    // Waterfall mode: only scroll waterfallScrollState with animation
                    try {
                        waterfallScrollState.animateScrollTo(0)
                    } catch (e: Exception) {
                        Log.d("HomeScreen", "waterfall animateScrollTo failed: ${e.message}")
                        // Fallback to instant scroll if animation fails
                        try {
                            waterfallScrollState.scrollTo(0)
                        } catch (e2: Exception) {
                            Log.d("HomeScreen", "waterfall scrollTo fallback failed: ${e2.message}")
                        }
                    }
                } else {
                    // Single column mode: only scroll habitsScrollState
                    try {
                        habitsScrollState.animateScrollToItem(0)
                    } catch (e: Exception) {
                        Log.d("HomeScreen", "habits animateScrollToItem failed: ${e.message}")
                    }
                }
                viewModel.consumeScrollToTop()
            }
        }
    }

    // 根据当前 Section 切换 scrollBehavior
    val currentScrollBehavior = when (currentSection) {
        HomeSection.Habits -> habitsScrollBehavior
        HomeSection.Records -> recordsScrollBehavior
        HomeSection.Contacts -> contactsScrollBehavior
        else -> TopAppBarDefaults.pinnedScrollBehavior()
    }

    val displayCutout = WindowInsets.displayCutout
    val layoutDirection = LocalLayoutDirection.current
    val isRailCutoutLeft = effectiveUseRail && displayCutout.getLeft(LocalDensity.current, layoutDirection) > 0
    val isRailCutoutRight = effectiveUseRail && displayCutout.getRight(LocalDensity.current, layoutDirection) > 0

    val sectionItems = listOf(
        HomeSection.Habits,
        HomeSection.Contacts,
        HomeSection.Records
    )

    val navigateToSection: (HomeSection) -> Unit = { targetSection ->
        if (currentSection == targetSection) {
            // 如果点击的是当前页面，滚动到顶部并展开 AppBar
            when (targetSection) {
                HomeSection.Habits -> {
                    scope.launch {
                        // Expand AppBar first to avoid it pushing content down after scroll
                        habitsScrollBehavior.state.heightOffset = 0f
                        
                        // CRITICAL: Only scroll the appropriate scroll state based on layout mode
                        // In waterfall mode, only use waterfallScrollState to avoid blocking on animateScrollToItem
                        if (isWaterfallMode) {
                            // Waterfall mode: only scroll waterfallScrollState with animation
                            try {
                                waterfallScrollState.animateScrollTo(0)
                            } catch (e: Exception) {
                                Log.d("HomeScreen", "waterfall animateScrollTo failed: ${e.message}")
                                // Fallback to instant scroll if animation fails
                                try {
                                    waterfallScrollState.scrollTo(0)
                                } catch (e2: Exception) {
                                    Log.d("HomeScreen", "waterfall scrollTo fallback failed: ${e2.message}")
                                }
                            }
                        } else {
                            // Single column mode: only scroll habitsScrollState
                            try {
                                habitsScrollState.animateScrollToItem(0)
                            } catch (e: Exception) {
                                Log.d("HomeScreen", "habits animateScrollToItem failed: ${e.message}")
                            }
                        }
                    }
                }
                HomeSection.Records -> {
                    scope.launch {
                        recordsScrollBehavior.state.heightOffset = 0f
                        try {
                            recordsScrollState.animateScrollToItem(0)
                        } catch (e: Exception) {
                            Log.d("HomeScreen", "records animateScrollToItem failed: ${e.message}")
                        }
                    }
                }
                HomeSection.Contacts -> {
                    scope.launch {
                        contactsScrollBehavior.state.heightOffset = 0f
                        try {
                            contactsScrollState.animateScrollToItem(0)
                        } catch (e: Exception) {
                            Log.d("HomeScreen", "contacts animateScrollToItem failed: ${e.message}")
                        }
                    }
                }
            }
        } else {
            // 切换到不同页面
            currentSection = targetSection
        }
    }

    // 主页主体内容 - 不再接收 nestedScrollConn，由各组件自己处理
    // 使用 AnimatedContent 实现 Section 切换时的淡入淡出动画
    val homeBody: @Composable (Modifier) -> Unit = { modifier ->
        AnimatedContent(
            targetState = currentSection,
            transitionSpec = {
                fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150))
            },
            label = "sectionTransition"
        ) { targetSection ->
            when (targetSection) {
                HomeSection.Habits -> {
                    if (isLoading) {
                        Box(
                            modifier = modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        // 使用 Column 布局，搜索框和内容区域垂直排列
                        Column(
                            modifier = modifier.fillMaxSize()
                        ) {
                            // 搜索框 - 使用 AnimatedVisibility 带滑入/滑出动画
                            // 搜索框的高度变化会自动推动下方内容
                            AnimatedVisibility(
                                visible = isSearchActive,
                                enter = slideInVertically(
                                    initialOffsetY = { height -> -height },
                                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 200f)
                                ) + fadeIn(animationSpec = tween(200)),
                                exit = slideOutVertically(
                                    targetOffsetY = { height -> -height },
                                    animationSpec = tween(200)
                                ) + fadeOut(animationSpec = tween(200))
                            ) {
                                SearchBarFixed(
                                    searchQuery = searchQuery,
                                    onSearchQueryChange = { query -> viewModel.setSearchQuery(query) },
                                    onClearSearch = {
                                        viewModel.setSearchQuery("")
                                    },
                                    onBackClick = {
                                        isSearchActive = false
                                        viewModel.clearSearch()
                                    },
                                    placeholder = stringResource(id = R.string.search_habits_hint),
                                    accessibilityLabel = stringResource(id = R.string.accessibility_search_habits),
                                    focusRequester = searchFocusRequester,
                                    isFocused = isSearchFocused,
                                    onFocusedChange = { focused -> isSearchFocused = focused },
                                    isSearchActive = isSearchActive
                                )
                            }

                            // 内容区域 - 使用 animateContentSize 让列表项平滑移动
                            val shownHabits = if (isSearchActive) filteredHabits else habits

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .animateContentSize(
                                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 200f)
                                    )
                            ) {
                                if (isSearchActive && filteredHabits.isEmpty()) {
                                    // 搜索但无结果
                                    SearchEmptyState(
                                        modifier = Modifier.fillMaxSize(),
                                        onClearSearch = {
                                            isSearchActive = false
                                            viewModel.clearSearch()
                                        }
                                    )
                                } else if (!isSearchActive && !hasLoadedHabits) {
                                    // 正在加载或刚从编辑/多选页面回来，避免闪烁“暂无习惯”
                                    Box(modifier = Modifier.fillMaxSize()) {}
                                } else if (!isSearchActive && habits.isEmpty()) {
                                    // 所有习惯为空
                                    EmptyStateContent(
                                        modifier = Modifier.fillMaxSize(),
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
                                        modifier = Modifier.fillMaxSize(),
                                        habits = filteredHabits,
                                        onHabitClick = { onEditHabit(it) },
                                        onCheckIn = { viewModel.incrementCompletionCount(it) },
                                        onUndoCompletion = { viewModel.undoHabitCompletion(it) },
                                        onDeleteHabit = {
                                            viewModel.deleteHabit(it)
                                            // 删除习惯后刷新记录界面
                                            application?.recordsViewModel?.refreshRecords()
                                        },
                                        onNavigateToMultiSelect = onNavigateToMultiSelect,
                                        nestedScrollConnection = habitsScrollBehavior.nestedScrollConnection,
                                        newlyAddedHabitId = newlyAddedHabitId,
                                        listState = habitsScrollState,
                                        waterfallScrollState = waterfallScrollState,
                                        bringIntoViewRequester = bringIntoViewRequester,
                                        forceTabletLandscape = forceTabletLandscape == true,
                                        searchQuery = searchQuery
                                    )
                                }
                            }
                        }
                    }
                }
                HomeSection.Contacts -> {
                    ContactsScreenContent(
                        modifier = modifier,
                        application = application,
                        scrollBehavior = contactsScrollBehavior,
                        listState = contactsScrollState,
                        searchQuery = contactsSearchQuery,
                        onSearchQueryChange = { 
                            contactsSearchQuery = it
                            application?.contactsViewModel?.setSearchQuery(it)
                        },
                        isSearchActive = isContactsSearchActive,
                        onSearchActiveChange = { isContactsSearchActive = it }
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
            HomeSection.Contacts -> contactsScrollBehavior
            else -> TopAppBarDefaults.pinnedScrollBehavior()
        }

        if (isRailVisible) {
            // Phone landscape: use TopAppBar
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
                            .semantics { heading() }
                    )
                },
                actions = {
                    // Search button - only show in Habits section
                    if (currentSection == HomeSection.Habits) {
                        IconButton(
                            onClick = {
                                if (isSearchActive) {
                                    isSearchActive = false
                                    viewModel.clearSearch()
                                    focusManager.clearFocus()
                                } else {
                                    isSearchActive = true
                                    searchFocusRequester.requestFocus()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = stringResource(id = R.string.accessibility_search_habits)
                            )
                        }
                    }
                    // Search button - only show in Contacts section
                    if (currentSection == HomeSection.Contacts) {
                        IconButton(
                            onClick = {
                                if (isContactsSearchActive) {
                                    isContactsSearchActive = false
                                    contactsFocusManager.clearFocus()
                                } else {
                                    isContactsSearchActive = true
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = stringResource(id = R.string.accessibility_search_contacts)
                            )
                        }
                    }
                    // Date filter button - only show in Records section
                    if (currentSection == HomeSection.Records) {
                        val recordsVM = application?.recordsViewModel
                        if (recordsVM != null) {
                            val recSelectedDate by recordsVM.selectedDate.collectAsStateWithLifecycle()

                            DateFilterButton(
                                selectedDate = recSelectedDate,
                                onDateSelected = { recordsVM.setDatePickerExpanded(true) },
                                onDateCleared = { recordsVM.clearDate() }
                            )
                        }
                    }
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
                                .semantics { heading() }
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
                        // Subtitle - only show for Contacts section when expanded
                        if (currentSection == HomeSection.Contacts && collapsedFraction < 0.5f) {
                            val reminderHabitsCount = habits.count { it.supervisionMethod != io.github.darrindeyoung791.habitpulse.data.model.SupervisionMethod.NONE }
                            Text(
                                text = stringResource(id = R.string.main_subtitle_contact_count, allContacts.size, reminderHabitsCount),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        // Subtitle - only show for Records section when expanded
                        if (currentSection == HomeSection.Records && collapsedFraction < 0.5f) {
                            val recordsVM = application?.recordsViewModel
                            if (recordsVM != null) {
                                val completionDaysCount by recordsVM.completionDaysCountFlow.collectAsStateWithLifecycle()
                                Text(
                                    text = stringResource(id = R.string.records_subtitle_completion_days, completionDaysCount),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Search button - only show in Habits section
                    if (currentSection == HomeSection.Habits) {
                        IconButton(
                            onClick = {
                                if (isSearchActive) {
                                    isSearchActive = false
                                    viewModel.clearSearch()
                                    focusManager.clearFocus()
                                } else {
                                    isSearchActive = true
                                    searchFocusRequester.requestFocus()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = stringResource(id = R.string.accessibility_search_habits)
                            )
                        }
                    }
                    // Search button - only show in Contacts section
                    if (currentSection == HomeSection.Contacts) {
                        IconButton(
                            onClick = {
                                if (isContactsSearchActive) {
                                    isContactsSearchActive = false
                                    contactsFocusManager.clearFocus()
                                } else {
                                    isContactsSearchActive = true
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = stringResource(id = R.string.accessibility_search_contacts)
                            )
                        }
                    }
                    // Date filter button - only show in Records section
                    if (currentSection == HomeSection.Records) {
                        val recordsVM = application?.recordsViewModel
                        if (recordsVM != null) {
                            val recSelectedDate by recordsVM.selectedDate.collectAsStateWithLifecycle()

                            DateFilterButton(
                                selectedDate = recSelectedDate,
                                onDateSelected = { recordsVM.setDatePickerExpanded(true) },
                                onDateCleared = { recordsVM.clearDate() }
                            )
                        }
                    }
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

    if (effectiveIsPermanentDrawer) {
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
                                    imageVector = if (isDrawerExpanded) Icons.AutoMirrored.Filled.MenuOpen else Icons.Filled.Menu,
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
    } else if (effectiveUseRail) {
        // NavigationRail layout for landscape phones
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
            if (effectiveIsPermanentDrawer) {
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

    // DatePicker Dialog - Single source of truth, managed by HomeScreen
    // Using key() to force complete recreation when orientation changes
    // This prevents both dialogs from appearing during recomposition
    val recordsVM = application?.recordsViewModel
    if (recordsVM != null) {
        val recSelectedDate by recordsVM.selectedDate.collectAsStateWithLifecycle()
        val recDatePickerExpanded by recordsVM.datePickerExpanded.collectAsStateWithLifecycle()

        if (recDatePickerExpanded) {
            DatePickerDialogContent(
                isPhoneLandscape = effectiveUseRail,
                selectedDate = recSelectedDate,
                onDismiss = { recordsVM.setDatePickerExpanded(false) },
                onDateSelected = { recordsVM.selectDate(it) }
            )
        }
    }
}

/**
 * DatePicker Dialog Content - Handles orientation-dependent display mode
 *
 * Uses key() to force complete recreation when orientation changes,
 * preventing both dialogs from appearing during recomposition.
 *
 * @param isPhoneLandscape True if phone in landscape mode (width < 1200dp)
 * @param selectedDate Currently selected date
 * @param onDismiss Callback when dialog is dismissed
 * @param onDateSelected Callback when date is selected
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialogContent(
    isPhoneLandscape: Boolean,
    selectedDate: LocalDate?,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    // CRITICAL: Use key() to force complete recreation when orientation changes
    // This prevents state bleeding between orientation changes and ensures
    // only one dialog appears at a time
    key(isPhoneLandscape) {
        val initialDisplayMode = if (isPhoneLandscape) DisplayMode.Input else DisplayMode.Picker
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate?.atStartOfDay()?.toInstant(java.time.ZoneOffset.UTC)?.toEpochMilli()
                ?: System.currentTimeMillis(),
            initialDisplayMode = initialDisplayMode
        )

        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = LocalDate.ofInstant(
                                java.time.Instant.ofEpochMilli(millis),
                                java.time.ZoneId.systemDefault()
                            )
                            onDateSelected(date)
                        }
                        onDismiss()
                    }
                ) {
                    Text(text = stringResource(id = R.string.records_date_picker_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(id = R.string.records_date_picker_dismiss))
                }
            }
        ) {
            DatePicker(state = datePickerState)
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
    onNavigateToMultiSelect: (habitId: UUID) -> Unit,
    nestedScrollConnection: androidx.compose.ui.input.nestedscroll.NestedScrollConnection? = null,
    newlyAddedHabitId: UUID? = null,
    listState: androidx.compose.foundation.lazy.LazyListState = remember { androidx.compose.foundation.lazy.LazyListState() },
    waterfallScrollState: ScrollState,  // 移除默认值，强制调用者必须传入
    bringIntoViewRequester: BringIntoViewRequester? = null,
    forceTabletLandscape: Boolean = false,
    searchQuery: String = ""
) {
    val configuration = LocalConfiguration.current
    var screenWidthDp = configuration.screenWidthDp
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // 如果启用了强制平板横屏模式，将屏幕宽度视为 ≥840dp
    if (forceTabletLandscape && isLandscape && screenWidthDp < 840) {
        screenWidthDp = 840
    }

    // Use staggered grid only for tablets in landscape (≥840dp)
    val useStaggeredGrid = isLandscape && screenWidthDp >= 840

    // Use consistent 16.dp horizontal padding to align with LargeTopAppBar title
    val horizontalPadding = 16.dp

    if (useStaggeredGrid) {
        // Waterfall layout for tablets in landscape (瀑布流)
        val column1Habits = habits.filterIndexed { index, _ -> index % 2 == 0 }
        val column2Habits = habits.filterIndexed { index, _ -> index % 2 == 1 }

        val waterfallModifier = if (nestedScrollConnection != null) modifier.nestedScroll(nestedScrollConnection) else modifier

        ScrollableWaterfallWithScrollbar(
            modifier = waterfallModifier,
            scrollState = waterfallScrollState
        ) {
            // Top anchor for bringIntoView requests (used to ensure the waterfall scroll container
            // scrolls to the top when requested from outside)
            if (bringIntoViewRequester != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .bringIntoViewRequester(bringIntoViewRequester)
                ) {}
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
                            onNavigateToMultiSelect = { habitId -> onNavigateToMultiSelect(habitId) },
                            isNewlyAdded = (habit.id == newlyAddedHabitId),
                            searchQuery = searchQuery,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

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
                            onNavigateToMultiSelect = { habitId -> onNavigateToMultiSelect(habitId) },
                            isNewlyAdded = (habit.id == newlyAddedHabitId),
                            searchQuery = searchQuery,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    } else {
        // Single column layout for phones and portrait mode
        val listModifier = if (nestedScrollConnection != null) modifier.nestedScroll(nestedScrollConnection) else modifier
        ScrollableLazyColumnWithScrollbar(
            modifier = listModifier,
            listState = listState,
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
                    onNavigateToMultiSelect = { habitId -> onNavigateToMultiSelect(habitId) },
                    isNewlyAdded = (habit.id == newlyAddedHabitId),
                    searchQuery = searchQuery
                )
            }
            // Add bottom spacer to prevent FAB from covering last item
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun ScrollableLazyColumnWithScrollbar(
    modifier: Modifier = Modifier,
    listState: LazyListState,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: LazyListScope.() -> Unit
) {
    val isScrollbarVisible = remember { mutableStateOf(true) }
    val scrollbarAlpha by animateFloatAsState(
        targetValue = if (isScrollbarVisible.value) 1f else 0f,
        animationSpec = tween(durationMillis = 300)
    )

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.isScrollInProgress
        }.collect { scrolling ->
            isScrollbarVisible.value = true
            if (!scrolling) {
                delay(1200)
                if (!listState.isScrollInProgress) {
                    isScrollbarVisible.value = false
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
        ) {
            content()
        }
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(4.dp)
                .align(Alignment.CenterEnd)
                .padding(end = 2.dp)
                .alpha(scrollbarAlpha)
        ) {
            val layoutInfo = listState.layoutInfo
            val totalCount = layoutInfo.totalItemsCount
            val visibleItems = layoutInfo.visibleItemsInfo
            val visibleCount = visibleItems.size

            if (totalCount > 0 && visibleCount > 0) {
                val density = LocalDensity.current

                // Estimate content height in pixels using visible item sizes + average for unseen
                val viewportHeightPx = layoutInfo.viewportSize.height.toFloat()
                val sumVisibleHeights = visibleItems.sumOf { it.size }.toFloat()
                val averageItemHeightPx = (sumVisibleHeights / visibleCount).coerceAtLeast(1f)
                val remainingItems = (totalCount - visibleCount).coerceAtLeast(0)
                val estimatedTotalContentHeightPx = (sumVisibleHeights + remainingItems * averageItemHeightPx).coerceAtLeast(viewportHeightPx)

                // Current scroll position in pixels (estimate)
                val currentScrollPx = listState.firstVisibleItemIndex * averageItemHeightPx + listState.firstVisibleItemScrollOffset.toFloat()
                val totalScrollablePx = (estimatedTotalContentHeightPx - viewportHeightPx).coerceAtLeast(1f)
                val scrollFraction = (currentScrollPx / totalScrollablePx).coerceIn(0f, 1f)

                // Indicator size as fraction of viewport / total content
                val indicatorHeightFraction = (viewportHeightPx / estimatedTotalContentHeightPx).coerceIn(0.03f, 1f)
                val indicatorHeightPx = viewportHeightPx * indicatorHeightFraction
                val offsetYPx = (viewportHeightPx - indicatorHeightPx) * scrollFraction

                // Animate pixels for smooth transitions during scrolling
                val animIndicatorHeightPx by animateFloatAsState(targetValue = indicatorHeightPx, animationSpec = tween(durationMillis = 80))
                val animOffsetYPx by animateFloatAsState(targetValue = offsetYPx, animationSpec = tween(durationMillis = 80))

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val indicatorHeight = with(density) { animIndicatorHeightPx.toDp() }
                    val offsetY = with(density) { animOffsetYPx.toDp() }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(indicatorHeight)
                            .offset(y = offsetY)
                            .background(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun ScrollableWaterfallWithScrollbar(
    modifier: Modifier = Modifier,
    scrollState: ScrollState,
    content: @Composable ColumnScope.() -> Unit
) {
    val isScrollbarVisible = remember { mutableStateOf(true) }
    val scrollbarAlpha by animateFloatAsState(
        targetValue = if (isScrollbarVisible.value) 1f else 0f,
        animationSpec = tween(durationMillis = 300)
    )

    LaunchedEffect(scrollState.isScrollInProgress) {
        isScrollbarVisible.value = true
        if (!scrollState.isScrollInProgress) {
            delay(1200)
            if (!scrollState.isScrollInProgress) {
                isScrollbarVisible.value = false
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            content()
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(4.dp)
                .align(Alignment.CenterEnd)
                .padding(end = 2.dp)
                .alpha(scrollbarAlpha)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val density = LocalDensity.current
                val maxH = maxHeight
                val maxHPx = with(density) { maxH.toPx() }
                val totalScroll = scrollState.maxValue.toFloat().coerceAtLeast(1f)
                val totalContentHeightPx = (maxHPx + totalScroll).coerceAtLeast(maxHPx)

                val viewportHeightPx = maxHPx
                val indicatorHeightFraction = (viewportHeightPx / totalContentHeightPx).coerceIn(0.03f, 1f)
                val indicatorHeightPx = viewportHeightPx * indicatorHeightFraction
                val scrollFraction = (scrollState.value.toFloat() / totalScroll).coerceIn(0f, 1f)
                val offsetYPx = (viewportHeightPx - indicatorHeightPx) * scrollFraction

                val animIndicatorHeightPx by animateFloatAsState(targetValue = indicatorHeightPx, animationSpec = tween(durationMillis = 80))
                val animOffsetYPx by animateFloatAsState(targetValue = offsetYPx, animationSpec = tween(durationMillis = 80))

                val indicatorHeight = with(density) { animIndicatorHeightPx.toDp() }
                val offsetY = with(density) { animOffsetYPx.toDp() }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(indicatorHeight)
                        .offset(y = offsetY)
                        .background(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(2.dp)
                        )
                )
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
    onNavigateToMultiSelect: (habitId: java.util.UUID) -> Unit = {},
    modifier: Modifier = Modifier,
    isNewlyAdded: Boolean = false,
    searchQuery: String = ""
) {
    var showMenu by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showNotesDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current

    val reminderTimes = habit.getReminderTimesList()
    val repeatDays = habit.getRepeatDaysList()
    val supervisorEmails = habit.getSupervisorEmailsList()
    val supervisorPhones = habit.getSupervisorPhonesList()

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
                        // 习惯名称（大字体）- 支持搜索高亮
                        Text(
                            text = highlightText(habit.title, searchQuery),
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
                        onClick = onCheckIn,
                        contentDescription = stringResource(id = R.string.accessibility_habit_checkin, habit.title)
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

                // 多选与排序
                DropdownMenuItem(
                    text = {
                        Text(text = stringResource(id = R.string.habit_context_menu_multi_select))
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.DragHandle,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    onClick = {
                        onNavigateToMultiSelect(habit.id)
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
                        showDeleteDialog = true
                        showMenu = false
                    }
                )
            }

            // 删除确认对话框
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = {
                        Text(
                            text = stringResource(id = R.string.habit_card_delete_confirm_title),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    text = {
                        Text(
                            text = stringResource(
                                id = R.string.habit_card_delete_confirm_message,
                                habit.title
                            )
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                // Start exit animation first, then call actual delete after animation completes
                                isVisible = false
                                scope.launch {
                                    kotlinx.coroutines.delay(150) // Wait for exit animation to complete
                                    onDeleteHabit()
                                }
                                showDeleteDialog = false
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(text = stringResource(id = R.string.habit_card_delete_confirm))
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDeleteDialog = false }
                        ) {
                            Text(text = stringResource(id = R.string.habit_card_delete_cancel))
                        }
                    }
                )
            }

            // 提醒详情对话框
            if (showReminderDialog) {
                ReminderDetailDialog(
                    reminderTimes = reminderTimes,
                    repeatCycle = habit.repeatCycle,
                    repeatDays = repeatDays,
                    supervisorEmails = supervisorEmails,
                    supervisorPhones = supervisorPhones,
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
    onClick: () -> Unit,
    contentDescription: String
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
            .semantics { this.contentDescription = contentDescription }
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
    supervisorEmails: List<String> = emptyList(),
    supervisorPhones: List<String> = emptyList(),
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

                // 监督人信息（如有）
                val hasSupervisors = supervisorEmails.isNotEmpty() || supervisorPhones.isNotEmpty()
                if (hasSupervisors) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // 监督人邮箱
                    if (supervisorEmails.isNotEmpty()) {
                        Text(
                            text = stringResource(id = R.string.create_habit_supervisor_email_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        supervisorEmails.forEach { email ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Email,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = email,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }

                    // 监督人电话
                    if (supervisorPhones.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(id = R.string.create_habit_supervisor_phone_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        supervisorPhones.forEach { phone ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Phone,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = phone,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
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
            onDeleteHabit = {},
            onNavigateToMultiSelect = {}
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
            onDeleteHabit = {},
            onNavigateToMultiSelect = {}
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
            onDeleteHabit = {},
            onNavigateToMultiSelect = {}
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

// ============= Search Bar Fixed =============

/**
 * Search bar fixed below TopAppBar
 * Uses 16.dp horizontal padding to align with habit cards
 * Does not overlap content - content scrolls underneath
 */
@Composable
internal fun SearchBarFixed(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onBackClick: () -> Unit,
    placeholder: String,
    accessibilityLabel: String,
    focusRequester: FocusRequester,
    isFocused: Boolean,
    onFocusedChange: (Boolean) -> Unit,
    isSearchActive: Boolean
) {
    // Fixed height container with padding
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)  // 56dp search bar + 16dp vertical padding
            .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button to exit search
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.settings_back),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Search text field
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState -> onFocusedChange(focusState.isFocused) }
                        .semantics {
                            paneTitle = accessibilityLabel
                        },
                    placeholder = {
                        Text(
                            text = placeholder,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp)
                )

                // Clear button - only show when there is text to clear
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = onClearSearch,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = stringResource(id = R.string.accessibility_clear_search),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Request focus when search bar appears - with delay to ensure UI is rendered
    LaunchedEffect(Unit) {
        delay(100)  // Small delay to ensure the search bar is fully rendered
        focusRequester.requestFocus()
    }
}

/**
 * Empty state for search with no results
 */
@Composable
private fun SearchEmptyState(
    modifier: Modifier = Modifier,
    onClearSearch: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(id = R.string.search_no_results),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(id = R.string.search_no_results_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onClearSearch) {
            Text(
                text = stringResource(id = R.string.accessibility_clear_search),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ============= Search Highlight Helpers =============

/**
 * Highlight matched text in the given text
 */
@Composable
private fun highlightText(text: String, query: String): AnnotatedString {
    if (query.isBlank()) {
        return AnnotatedString(text)
    }

    val annotatedString = buildAnnotatedString {
        var startIndex = 0
        val lowerCaseText = text.lowercase()
        val lowerCaseQuery = query.lowercase()

        while (startIndex < text.length) {
            val matchIndex = lowerCaseText.indexOf(lowerCaseQuery, startIndex)
            if (matchIndex == -1) {
                append(text.substring(startIndex))
                break
            }

            // Append text before match
            if (matchIndex > startIndex) {
                append(text.substring(startIndex, matchIndex))
            }

            // Append matched text with highlight style
            withStyle(style = SpanStyle(background = MaterialTheme.colorScheme.primaryContainer)) {
                append(text.substring(matchIndex, matchIndex + query.length))
            }

            startIndex = matchIndex + query.length
        }
    }

    return annotatedString
}
