package io.github.darrindeyoung791.habitpulse.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.SharedTransitionScope
import io.github.darrindeyoung791.habitpulse.HabitPulseApplication
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.data.model.Habit
import io.github.darrindeyoung791.habitpulse.navigation.getDeviceCornerRadius
import io.github.darrindeyoung791.habitpulse.ui.rememberDeviceFormInfo
import io.github.darrindeyoung791.habitpulse.ui.theme.HabitPulseTheme
import io.github.darrindeyoung791.habitpulse.ui.utils.rememberDebounceClickHandler
import io.github.darrindeyoung791.habitpulse.ui.screens.DateFilterButton
import io.github.darrindeyoung791.habitpulse.viewmodel.HabitViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt

enum class HomeSection { Habits, Contacts, Records }

fun HomeSection.iconResource() = when (this) {
    HomeSection.Habits -> Icons.Filled.CheckCircle
    HomeSection.Contacts -> Icons.Filled.People
    HomeSection.Records -> Icons.Filled.Assessment
}

fun HomeSection.outlinedIconResource() = when (this) {
    HomeSection.Habits -> Icons.Outlined.CheckCircle
    HomeSection.Contacts -> Icons.Outlined.People
    HomeSection.Records -> Icons.Outlined.Assessment
}

/**
 * How long (in seconds) a release fling is assumed to keep pushing the drawer
 * when predicting which side it will settle on (pager-style momentum
 * projection). Larger = short flicks trigger more eagerly.
 */
private const val DrawerFlingProjectionSeconds = 0.16f

@Composable
fun AnimatedNavIcon(
    isSelected: Boolean,
    section: HomeSection,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified
) {
    val iconTint = if (tint != Color.Unspecified) tint else LocalContentColor.current
    Crossfade(
        targetState = isSelected,
        animationSpec = tween(durationMillis = 300),
        label = "navIconAnim"
    ) { selected ->
        Icon(
            imageVector = if (selected) section.iconResource() else section.outlinedIconResource(),
            contentDescription = contentDescription,
            modifier = modifier,
            tint = iconTint
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onCreateHabit: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onEditHabit: (Habit) -> Unit,
    onNavigateToMultiSelect: (habitId: UUID) -> Unit = {},
    onAICreateHabit: () -> Unit = {},
    onViewAboutToStart: () -> Unit = {},
    onViewTodayHabits: () -> Unit = {},
    onViewOverdue: () -> Unit = {},
    onViewLanSync: () -> Unit = {},
    onViewStats: () -> Unit = {},
    application: HabitPulseApplication? = null,
    onHomeDataLoaded: () -> Unit = {},
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null
) {
    val context = LocalContext.current
    // 防重复点击处理器
    val clickHandler = rememberDebounceClickHandler()
    val scope = rememberCoroutineScope()

    // Track which habit is transitioning to MultiSelect (for shared element)
    var multiSelectTargetHabitId by remember { mutableStateOf<UUID?>(null) }

    // FAB selection dialog state
    var showCreateHabitDialog by remember { mutableStateOf(false) }

    // Focus requester for TalkBack initial focus
    val titleFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }

    // 收集联系人列表状态（用于 Contacts section 的副标题�?
    val allContacts by (application?.contactsViewModel?.allContactsFlow ?: kotlinx.coroutines.flow.flowOf(emptyList())).collectAsStateWithLifecycle(initialValue = emptyList())

    // 收集习惯列表（仅用于副标题显示）
    val viewModel: HabitViewModel = if (application != null) {
        application.habitViewModel
    } else {
        remember {
            val fakeHabitDao = FakeHabitDao()
            val fakeCompletionDao = FakeHabitCompletionDao()
            val fakeRepository = io.github.darrindeyoung791.habitpulse.data.repository.HabitRepository(fakeHabitDao, fakeCompletionDao)
            val fakeOnboardingPreferences = io.github.darrindeyoung791.habitpulse.utils.OnboardingPreferences(context.applicationContext)
            val fakeUserPreferences = io.github.darrindeyoung791.habitpulse.data.preferences.UserPreferences.getInstance(context)
            HabitViewModel(fakeRepository, fakeOnboardingPreferences, fakeUserPreferences)
        }
    }
    val habits by viewModel.habitsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    // 联系人搜索状�?
    var contactsSearchQuery by remember { mutableStateOf("") }
    var isContactsSearchActive by remember { mutableStateOf(false) }
    val contactsFocusManager = LocalFocusManager.current

    // 习惯搜索状态
    var isHabitsSearchActive by remember { mutableStateOf(false) }

    // 当联系人搜索激活时，拦截系统返回键
    BackHandler(enabled = isContactsSearchActive) {
        isContactsSearchActive = false
        contactsFocusManager.clearFocus()
    }

    // 当退出联系人搜索时，清除搜索关键�?
    LaunchedEffect(isContactsSearchActive) {
        if (!isContactsSearchActive) {
            contactsSearchQuery = ""
            application?.contactsViewModel?.clearSearch()
            contactsFocusManager.clearFocus()
        }
    }

    // 当页面首次加载时，请求焦点到 title
    LaunchedEffect(Unit) {
        // 延迟一小段时间确保 UI 已经渲染完成
        delay(500)
        // 请求焦点�?title
        titleFocusRequester.requestFocus()
    }

    val deviceForm = rememberDeviceFormInfo()
    val isLandscape = deviceForm.isLandscape
    val isTabletDevice = deviceForm.isTabletDevice

    // 获取用户偏好设置
    val userPreferences = application?.let {
        remember { io.github.darrindeyoung791.habitpulse.data.preferences.UserPreferences.getInstance(it) }
    }
    val forceTabletLandscape: Boolean by userPreferences?.forceTabletLandscapeFlow?.collectAsStateWithLifecycle(initialValue = false) ?: remember { mutableStateOf(false) }

    // Navigation mode decision logic:
    // - Tablet in landscape: PermanentNavigationDrawer with hamburger menu
    // - Phone in landscape: NavigationRail
    // - All portrait modes: custom reveal drawer (page slides right off-screen)
    val isPermanentDrawer = deviceForm.isTabletLandscape
    val useRail = deviceForm.isPhoneLandscape

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
    val isWaterfallMode = deviceForm.isWideLayout

    var currentSection by rememberSaveable { mutableStateOf(HomeSection.Habits) }
    var isDrawerExpanded by rememberSaveable { mutableStateOf(true) }

    // ------------------------------------------------------------------
    // Portrait mode: reveal drawer (same-plane slide, DeepSeek-style)
    // The drawer sits immediately to the LEFT of the main page on one
    // virtual plane. Opening translates BOTH by drawerWidth * fraction:
    // the drawer enters from off-screen left while the page exits right,
    // and the displaced page is dimmed by a scrim overlay.
    // Fraction 0f = fully closed, 1f = fully open.
    // ------------------------------------------------------------------
    var portraitScreenWidthPx by remember { mutableFloatStateOf(0f) }
    // Drawer occupies 3/4 of the screen width.
    val portraitDrawerWidthPx = portraitScreenWidthPx * 3f / 4f
    val portraitDrawerWidth = with(LocalDensity.current) { portraitDrawerWidthPx.toDp() }
    var portraitDrawerOpen by rememberSaveable { mutableStateOf(false) }
    val portraitDrawerFraction = remember { Animatable(if (portraitDrawerOpen) 1f else 0f) }

    // Derived flags via derivedStateOf so per-frame fraction updates do NOT
    // recompose HomeScreen - only boolean threshold crossings do.
    val isPortraitDrawerOpen by remember { derivedStateOf { portraitDrawerFraction.value > 0.5f } }
    val portraitDrawerHidden by remember { derivedStateOf { portraitDrawerFraction.value <= 0.001f } }
    val portraitPageHidden by remember { derivedStateOf { portraitDrawerFraction.value >= 0.999f } }
    val portraitDrawerCatchEnabled by remember { derivedStateOf { portraitDrawerFraction.value > 0.95f } }

    fun openPortraitDrawer() {
        portraitDrawerOpen = true
        scope.launch {
            portraitDrawerFraction.animateTo(
                1f,
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            )
        }
    }

    fun closePortraitDrawer() {
        portraitDrawerOpen = false
        scope.launch {
            portraitDrawerFraction.animateTo(
                0f,
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            )
        }
    }

    /**
     * Settle after a drag with the animation's INITIAL VELOCITY matching the
     * finger velocity at release, so the drawer visibly keeps the gesture's
     * momentum and glides into its anchor (critically damped spring: no
     * overshoot past the fully-open/closed position). Used by the swipe
     * gesture only; button/menu paths keep the fixed tween above.
     */
    fun settlePortraitDrawer(open: Boolean, releaseVelocityPxPerSec: Float) {
        portraitDrawerOpen = open
        scope.launch {
            val widthPx = portraitScreenWidthPx * 3f / 4f
            val initialVelocity = if (widthPx > 1f) releaseVelocityPxPerSec / widthPx else 0f
            portraitDrawerFraction.animateTo(
                targetValue = if (open) 1f else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                initialVelocity = initialVelocity
            )
        }
    }

    // Shared horizontal drag gesture (custom axis-arbitrating detector).
    // Attached to the STATIC root container (NOT the translating page/drawer):
    // its local coordinates therefore equal world/screen coordinates. This is
    // critical - a detector inside the following container would measure the
    // finger in a frame that shifts every frame, collapsing deltas and
    // velocity to ~0 once the drawer catches up, causing bogus bounce-backs.
    //
    // - Swipe right anywhere on the page opens; swipe left on the drawer or
    //   dimmed page closes. Fraction follows the finger frame by frame.
    // - Anti-mistouch: NOTHING is consumed until HORIZONTAL displacement
    //   crosses touch slop FIRST, so child clicks and taps are untouched.
    //   If a vertical scroll claims the pointer stream first (its changes
    //   become consumed) or vertical slop is crossed before horizontal slop,
    //   the gesture is abandoned and can never trigger the drawer - even if
    //   the finger later moves horizontally without lifting.
    val portraitDrawerDragModifier = Modifier.pointerInput(Unit) {
        val touchSlop = viewConfiguration.touchSlop
        val velocityTracker = VelocityTracker()
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var totalX = 0f
            var totalY = 0f
            var engaged = false

            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break

                val delta = change.positionChange()
                totalX += delta.x
                totalY += delta.y

                // Velocity is sampled ONLY from engagement onward: the slow
                // press/drift phase before direction-lock must not dilute the
                // release-speed measurement, or fast flicks read below the
                // fling threshold and wrongly fall back to the distance rule.
                // The release (UP) frame is included too - it usually carries
                // the fastest samples of the whole gesture.
                if (engaged) {
                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                }
                if (!change.pressed) break

                if (!engaged) {
                    // A child (e.g. a vertically scrolling list) claimed the
                    // pointer stream: stand down immediately.
                    if (change.isConsumed) break
                    if (abs(totalX) > touchSlop && abs(totalX) > abs(totalY)) {
                        engaged = true
                        velocityTracker.resetTracking()
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                    } else if (abs(totalY) > touchSlop) {
                        // Vertical gesture won before horizontal slop:
                        // never hijack it for the rest of this touch.
                        break
                    }
                }

                if (engaged) {
                    change.consume()
                    // AwaitPointerEventScope is a restricted suspension scope
                    // (foreign suspend calls like Animatable.snapTo are
                    // forbidden), so hop out via scope.launch instead: it is a
                    // plain (non-suspend) call, launches execute FIFO on the
                    // UI dispatcher, and each snapTo lands within the same
                    // frame - the drawer tracks the finger with no lag.
                    val dx = delta.x
                    scope.launch {
                        val widthPx = portraitScreenWidthPx * 3f / 4f
                        if (widthPx > 1f) {
                            portraitDrawerFraction.snapTo(
                                (portraitDrawerFraction.value + dx / widthPx)
                                    .coerceIn(0f, 1f)
                            )
                        }
                    }
                }
            }

            if (engaged) {
                val velocityX = velocityTracker.calculateVelocity().x
                // Rule 1 (absolute, velocity-independent): once THIS gesture
                // has travelled >= 1/5 of the screen width, direction alone
                // decides - rightward opens, leftward closes.
                // Rule 2 (momentum projection): for shorter gestures, predict
                // where the release speed would carry the drawer and settle to
                // the nearest anchor - short fast flicks project far past mid
                // and trigger; slow short drags stay put; an unreliable speed
                // reading merely degrades to nearest-anchor instead of ever
                // reversing the drawer against the finger.
                val fifthOfScreen = portraitScreenWidthPx / 5f
                val shouldOpen = if (abs(totalX) >= fifthOfScreen) {
                    totalX > 0f
                } else {
                    val widthPx = portraitScreenWidthPx * 3f / 4f
                    val velocityFractionPerSec = if (widthPx > 1f) velocityX / widthPx else 0f
                    portraitDrawerFraction.value +
                        velocityFractionPerSec * DrawerFlingProjectionSeconds > 0.5f
                }
                // Hand the finger's release velocity to the settle animation so
                // the drawer keeps the gesture's momentum instead of playing a
                // canned tween.
                settlePortraitDrawer(open = shouldOpen, releaseVelocityPxPerSec = velocityX)
            }
        }
    }

    val useDrawer = !isLandscape

    // Notify MainActivity that home data has loaded (dismisses splash screen)
    var hasNotifiedLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(habits) {
        if (!hasNotifiedLoaded) {
            hasNotifiedLoaded = true
            onHomeDataLoaded()
        }
    }

    // Reset Records filter when navigating to Records section
    // This ensures users always see all records when entering Records screen
    LaunchedEffect(currentSection) {
        if (currentSection == HomeSection.Records) {
            application?.recordsViewModel?.selectHabit(null)
            application?.recordsViewModel?.clearDate()
        }
    }

    // 为每�?Section 保存独立的滚动状�?
    // 使用 rememberSaveable 让导航进出后保持滚动位置（取�?返回不应回到顶部�?
    val habitsScrollState = rememberSaveable(saver = androidx.compose.foundation.lazy.LazyListState.Saver) {
        androidx.compose.foundation.lazy.LazyListState()
    }
    // Use rememberSaveable with proper Saver to preserve scroll position when returning from other screens
    val waterfallScrollState = rememberSaveable(saver = LazyStaggeredGridState.Saver) {
        LazyStaggeredGridState()
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

    // 为每�?Section 使用独立�?scrollBehavior
    val habitsScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val recordsScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val contactsScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

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
                                waterfallScrollState.animateScrollToItem(0)
                            } catch (e: Exception) {
                                Log.d("HomeScreen", "waterfall animateScrollToItem failed: ${e.message}")
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
            // 切换到不同页�?
            currentSection = targetSection
        }
    }

    // 主页主体内容 - 不再接收 nestedScrollConn，由各组件自己处�?
    // 使用 AnimatedContent 实现 Section 切换时的淡入淡出动画
    // Z 轴切换（细微缩放 + 淡入淡出）
    val homeBody: @Composable (Modifier) -> Unit = { modifier ->
        AnimatedContent(
            targetState = currentSection,
            transitionSpec = {
                // MD3 风格：从中间开始的细微 Z 轴切换
                scaleIn(
                    initialScale = 0.95f,
                    animationSpec = tween(150)
                ) + fadeIn(animationSpec = tween(150)) togetherWith
                scaleOut(
                    targetScale = 1.05f,
                    animationSpec = tween(150)
                ) + fadeOut(animationSpec = tween(150))
            },
            label = "sectionTransition"
        ) { targetSection ->
            when (targetSection) {
                HomeSection.Habits -> {
                    HabitScreenContent(
                        modifier = modifier,
                        application = application,
                        scrollBehavior = habitsScrollBehavior,
                        listState = habitsScrollState,
                        waterfallScrollState = waterfallScrollState,
                        bringIntoViewRequester = bringIntoViewRequester,
                        forceTabletLandscape = forceTabletLandscape == true,
                        onCreateHabit = onCreateHabit,
                        onCreateHabitSelection = { showCreateHabitDialog = true },
                        onEditHabit = onEditHabit,
                        onNavigateToMultiSelect = { habitId ->
                            multiSelectTargetHabitId = habitId
                            onNavigateToMultiSelect(habitId)
                        },
                        onViewAboutToStart = onViewAboutToStart,
                        onViewTodayHabits = onViewTodayHabits,
                        onViewOverdue = onViewOverdue,
                        onViewLanSync = onViewLanSync,
                        onViewStats = onViewStats,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedContentScope = animatedContentScope,
                        nestedScrollConnection = habitsScrollBehavior.nestedScrollConnection,
                        multiSelectTargetHabitId = multiSelectTargetHabitId,
                        isSearchActive = isHabitsSearchActive,
                        onSearchActiveChange = { isHabitsSearchActive = it }
                    )
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

        // 根据当前 Section 选择正确�?scrollBehavior
        val currentTopAppBarScrollBehavior = when (currentSection) {
            HomeSection.Records -> recordsScrollBehavior
            HomeSection.Habits -> habitsScrollBehavior
            HomeSection.Contacts -> contactsScrollBehavior
            else -> TopAppBarDefaults.pinnedScrollBehavior()
        }

        if (isRailVisible) {
            // Phone landscape: use TopAppBar
            Box(modifier = Modifier.clickable { navigateToSection(currentSection) }) {
                TopAppBar(
                windowInsets = WindowInsets.safeDrawing.only(
                    if (isRailCutoutRight) WindowInsetsSides.Top + WindowInsetsSides.End else WindowInsetsSides.Top
                ),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
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
                            onClick = { isHabitsSearchActive = true }
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
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(id = R.string.main_settings)
                        )
                    }
                },
                scrollBehavior = currentTopAppBarScrollBehavior
            )
            }
        } else {
            // Other modes: use LargeTopAppBar with exitUntilCollapsed behavior
            Box(modifier = Modifier.clickable { navigateToSection(currentSection) }) {
                LargeTopAppBar(
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                navigationIcon = {
                    if (useDrawer) {
                        IconButton(
                            onClick = {
                                if (isPortraitDrawerOpen) closePortraitDrawer() else openPortraitDrawer()
                            }
                        ) {
                            Icon(
                                imageVector = if (isPortraitDrawerOpen) Icons.AutoMirrored.Filled.MenuOpen else Icons.Filled.Menu,
                                contentDescription = if (isPortraitDrawerOpen)
                                    stringResource(id = R.string.main_collapse_drawer)
                                else
                                    stringResource(id = R.string.main_expand_drawer)
                            )
                        }
                    }
                },
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
                            val reminderHabitsCount = habits.count { it.hasSupervision }
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
                            onClick = { isHabitsSearchActive = true }
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
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(id = R.string.main_settings)
                        )
                    }
                },
                scrollBehavior = currentScrollBehavior,
                modifier = Modifier.nestedScroll(currentScrollBehavior.nestedScrollConnection)
            )
            }
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
                                    AnimatedNavIcon(
                                        isSelected = isSelected,
                                        section = section,
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
                                AnimatedNavIcon(
                                    isSelected = isSelected,
                                    section = section,
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
                                showCreateHabitDialog = true
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
                            AnimatedNavIcon(
                                isSelected = currentSection == section,
                                section = section,
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
                                showCreateHabitDialog = true
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
        // Portrait mode: reveal drawer (same-plane slide).
        // Drawer and main page are laid out side by side on ONE virtual plane:
        // opening translates both right by drawerWidth * fraction, so the
        // drawer slides in from off-screen while the page exits right and
        // gets dimmed. Offsets read fraction in the placement phase, so the
        // animation causes no recomposition.
        val deviceCornerRadius = getDeviceCornerRadius()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .onSizeChanged { portraitScreenWidthPx = it.width.toFloat() }
                // Gesture lives on the STATIC root so pointer coordinates stay
                // in world space while the plane underneath translates.
                .then(portraitDrawerDragModifier)
        ) {
            // Drawer sheet - enters from off-screen left, moving the same
            // distance as the page (same virtual plane).
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(portraitDrawerWidth)
                    .fillMaxHeight()
                    .offset {
                        IntOffset(
                            ((portraitDrawerFraction.value - 1f) * portraitDrawerWidthPx).roundToInt(),
                            0
                        )
                    }
                    .then(if (portraitDrawerHidden) Modifier.clearAndSetSemantics { } else Modifier),
                // Same background as the main page so the rounded-corner card
                // edge never clashes against a differently-tinted sheet.
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(
                                WindowInsetsSides.Start + WindowInsetsSides.Top
                            )
                        ),
                    // Center the nav items as a group so they stay within
                    // comfortable thumb reach.
                    verticalArrangement = Arrangement.Center
                ) {
                    sectionItems.forEach { section ->
                        val isSelected = currentSection == section
                        NavigationDrawerItem(
                            label = {
                                Text(text = when (section) {
                                    HomeSection.Habits -> stringResource(id = R.string.main_tab_habits)
                                    HomeSection.Contacts -> stringResource(id = R.string.main_tab_contacts)
                                    HomeSection.Records -> stringResource(id = R.string.main_tab_records)
                                })
                            },
                            icon = {
                                AnimatedNavIcon(
                                    isSelected = isSelected,
                                    section = section,
                                    contentDescription = null
                                )
                            },
                            selected = isSelected,
                            // Switch section immediately AND slide back concurrently:
                            // both animations run at the same time.
                            onClick = {
                                navigateToSection(section)
                                closePortraitDrawer()
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }
            }

            // Sliding main page - moves the same distance as the drawer so the
            // two stay rigidly attached on one plane. Clipped to the device
            // corner radius (after offset, so the whole rounded card slides)
            // so the displaced, dimmed page reads as a card over the drawer.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset {
                        IntOffset((portraitDrawerFraction.value * portraitDrawerWidthPx).roundToInt(), 0)
                    }
                    .clip(RoundedCornerShape(deviceCornerRadius))
                    .then(if (portraitPageHidden) Modifier.clearAndSetSemantics { } else Modifier)
            ) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = { topAppBarContent(false) },
                    floatingActionButton = {
                        if (showFab) {
                            ExtendedFloatingActionButton(
                                onClick = {
                                    showCreateHabitDialog = true
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        homeBody(Modifier.fillMaxSize())
                    }
                }

                // Dimming scrim over the displaced page; tap it to close.
                // Not composed while fully closed so it can never interfere
                // with page interactions.
                if (!portraitDrawerHidden) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .drawBehind {
                                drawRect(
                                    color = Color.Black,
                                    alpha = 0.32f * portraitDrawerFraction.value
                                )
                            }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = portraitDrawerCatchEnabled
                            ) { closePortraitDrawer() }
                    )
                }
            }
        }
    }

    // BackHandler for portrait drawer close on back press
    if (useDrawer) {
        val portraitDrawerBackEnabled by remember {
            derivedStateOf { portraitDrawerFraction.targetValue > 0.01f }
        }
        BackHandler(enabled = portraitDrawerBackEnabled) {
            closePortraitDrawer()
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

    // FAB - Create Habit Selection Dialog
    if (showCreateHabitDialog) {
        CreateHabitSelectionDialog(
            onDismiss = { showCreateHabitDialog = false },
            onManualCreate = {
                showCreateHabitDialog = false
                scope.launch {
                    clickHandler.processClick {
                        onCreateHabit()
                    }
                }
            },
            onAICreate = {
                showCreateHabitDialog = false
                scope.launch {
                    clickHandler.processClick {
                        onAICreateHabit()
                    }
                }
            }
        )
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
                AnimatedNavIcon(
                    isSelected = isSelected,
                    section = section,
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
                )
            }
        }
    }
}

@Composable
fun CreateHabitSelectionDialog(
    onDismiss: () -> Unit,
    onManualCreate: () -> Unit,
    onAICreate: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.create_habit_selection_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ElevatedCard(
                    onClick = onAICreate,
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.create_habit_selection_ai),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = stringResource(id = R.string.create_habit_selection_ai_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onManualCreate)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(id = R.string.create_habit_selection_manual),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = stringResource(id = R.string.create_habit_selection_manual_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

// ============= Preview =============

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HabitPulseTheme {
        HomeScreen(
            onCreateHabit = {},
            onNavigateToSettings = {},
            onEditHabit = {},
            onAICreateHabit = {},
            application = null
        )
    }
}
