package io.github.darrindeyoung791.habitpulse.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.lerp
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
import io.github.darrindeyoung791.habitpulse.ui.utils.rememberHapticsEnabled
import io.github.darrindeyoung791.habitpulse.ui.utils.rememberPressVibrationParams
import io.github.darrindeyoung791.habitpulse.ui.utils.vibrateShort
import io.github.darrindeyoung791.habitpulse.ui.screens.DateFilterButton
import io.github.darrindeyoung791.habitpulse.ui.screens.ai.AIChatScreen
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

/**
 * 主页内容底部为 Omnibox 预留的额外净空：列表可滚动范围在搜索框上方结束，
 * 不从其下滑动（各列表自带的尾部 Spacer 之外再叠加）。
 */
private val OmniboxContentBottomClearance = 24.dp

/** 渐变过渡层从 Omnibox 上沿再往上延伸的高度（过渡起点）。 */
private val OmniboxGradientHeadroom = 64.dp

/** Omnibox 距屏幕底边（导航栏/键盘之上）的外边距。 */
private val OmniboxBottomMargin = 20.dp

/**
 * 底部 Omnibox + MD3 drag handle + 背景渐变过渡的完整装饰，需在 BoxScope 中调用。
 *
 * 把手位于搜索框上方：点按直接展开 AI 对话页；纵向拖拽由主页根层的
 * 常驻手势条（aiSheetStripDragModifier）驱动，可无极擦洗形变进度。
 * 渐变层自把手上方 [OmniboxGradientHeadroom]
 * 处开始向下不透明度加深，直至屏幕底边完全变为背景色；整体随 ime insets 抬升。
 */
@Composable
private fun BoxScope.BottomOmniboxWithFade(
    query: String,
    onQueryChange: (String) -> Unit,
    onHandleTap: () -> Unit,
    showHandle: Boolean = true,
    progress: Float = 0f,
    onPillBounds: (Rect) -> Unit = {},
    dragModifier: Modifier = Modifier
) {
    val surfaceColor = MaterialTheme.colorScheme.surface

    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
    ) {
        // 透明 → 背景色 的垂直渐变（绘制相位执行，零重组开销）
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(surfaceColor.copy(alpha = 0f), surfaceColor)
                        )
                    )
                }
        )
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                .padding(
                    top = OmniboxGradientHeadroom,
                    bottom = OmniboxBottomMargin,
                    start = 16.dp,
                    end = 16.dp
                )
                .then(dragModifier)
                .graphicsLayer { alpha = (1f - progress).coerceIn(0f, 1f) },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 拖拽把手：点按展开 AI 对话页；拖拽由外层容器手势处理。
            // 形变启动后隐藏视觉（幽灵把手在面片上缘同位接替），
            // 但保留固定占位尺寸，避免布局跳动
            val handleDescription = stringResource(R.string.ai_omnibox_handle_talkback)
            Box(
                modifier = Modifier
                    .size(width = 64.dp, height = 24.dp)
                    .clickable(enabled = showHandle) { onHandleTap() }
                    .semantics {
                        contentDescription = handleDescription
                    },
                contentAlignment = Alignment.Center
            ) {
                if (showHandle) {
                    OmniboxDragHandle()
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            HomeOmnibox(
                query = query,
                onQueryChange = onQueryChange,
                onUpdateWindowBounds = onPillBounds
            )
        }
    }
}

/** Omnibox 顶部的横向拖拽把手（自绘，避免依赖版本不稳定的 m3 DragHandle）。 */
@Composable
private fun OmniboxDragHandle() {
    Box(
        modifier = Modifier
            .padding(vertical = 10.dp, horizontal = 16.dp)
            .size(width = 32.dp, height = 4.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
    )
}

/**
 * Omnibox → AI 对话页 形变层（文件级独立组件）。
 *
 * 性能关键设计：
 * - 对 [progress] 值的全部读取都发生在本组件的最小重组作用域内——动画每帧
 *   只重组本组件，不波及 HomeScreen 主树。
 * - 传给内嵌 AIChatScreen 的回调经 remember 固定为稳定实例（rememberUpdatedState
 *   转发最新值），使其在纯进度帧中可以被跳过重组；浮现 alpha 在 graphicsLayer
 *   的绘制相位读取，同样不触发重组。
 * - 显示/隐藏门用 derivedStateOf 布尔阈值，避免逐帧重组子树。
 */
@Composable
private fun BoxScope.AiSheetOverlay(
    progress: Animatable<Float, AnimationVector1D>,
    omniboxBounds: Rect,
    screenWidthPx: Float,
    screenHeightPx: Float,
    cornerRadius: Dp,
    application: HabitPulseApplication?,
    habits: List<Habit>,
    onCollapse: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onEditHabitById: (UUID) -> Unit,
    onManualCreateHabit: () -> Unit,
    draftText: String
) {
    val density = LocalDensity.current
    val p = progress.value

    // 稳定回调包装：实例跨帧不变，内部经 State 转发最新闭包
    val collapseState = rememberUpdatedState(onCollapse)
    val settingsState = rememberUpdatedState(onNavigateToSettings)
    val editByIdState = rememberUpdatedState(onEditHabitById)
    val manualState = rememberUpdatedState(onManualCreateHabit)
    val chatOnBack = remember { { collapseState.value() } }
    val chatOnSettings = remember { { settingsState.value() } }
    val chatOnEdit = remember { { id: UUID -> editByIdState.value(id) } }
    val chatOnManual = remember { { manualState.value() } }

    // 门控布尔：只有跨越阈值时才重组对应子树
    val scrimVisible by remember { derivedStateOf { progress.value > 0.01f } }
    val chatVisible by remember { derivedStateOf { progress.value > 0.3f } }
    val handleVisible by remember { derivedStateOf { progress.value < 0.25f } }

    // 几何插值
    val screenRect = Rect(0f, 0f, screenWidthPx, screenHeightPx)
    // 起点矩形与真实胶囊完全重合（不含把手区 headroom）：拖拽启动的首帧
    // 面片与 Omnibox 像素级重合、零跳变，随进度连续向上/向四周生长
    val startRect = Rect(
        omniboxBounds.left,
        omniboxBounds.top,
        omniboxBounds.right,
        omniboxBounds.bottom
    )
    val curRect = lerp(startRect, screenRect, p)
    // 半径从真实胶囊的半高开始，保证首帧圆角与 Omnibox 一致
    val startRadiusPx = startRect.height / 2f
    val endRadiusPx = with(density) { cornerRadius.toPx() }
    val curRadiusPx = startRadiusPx + (endRadiusPx - startRadiusPx) * p
    // 颜色在组合期取好，绘制相位只做插值（drawBehind 中不可读 CompositionLocal）
    val sheetStartColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val sheetEndColor = MaterialTheme.colorScheme.surface

    // 页面压暗 + 点击空白处收回
    if (scrimVisible) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    drawRect(color = Color.Black, alpha = 0.32f * progress.value)
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = p > 0.05f
                ) { onCollapse() }
        )
    }

    // 形变面片：矩形/圆角/颜色随进度连续插值；可继续拖拽擦洗
    Box(
        modifier = Modifier
            .offset {
                IntOffset(curRect.left.roundToInt(), curRect.top.roundToInt())
            }
            .size(
                width = with(density) { curRect.width.toDp() },
                height = with(density) { curRect.height.toDp() }
            )
            .graphicsLayer {
                shape = RoundedCornerShape(with(density) { curRadiusPx.toDp() })
                clip = true
            }
            .drawBehind {
                drawRect(color = lerp(sheetStartColor, sheetEndColor, p))
            }
    ) {
        // 对话页元素在后半程浮现（alpha 绘制相位读取，不逐帧重组）
        if (chatVisible && application != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = ((progress.value - 0.45f) / 0.35f).coerceIn(0f, 1f)
                    }
            ) {
                AIChatScreen(
                    onNavigateBack = chatOnBack,
                    onNavigateToSettings = chatOnSettings,
                    onCollapse = chatOnBack,
                    onEditHabit = chatOnEdit,
                    application = application,
                    initialInputText = draftText,
                    onManualCreateHabit = chatOnManual,
                    progress = progress
                )
            }
        }
    }

    // 把手幽灵层：随拖拽上移并渐隐，至屏幕上 1/4 进度处完全透明
    if (handleVisible) {
        val handleAlpha = (1f - p / 0.25f).coerceIn(0f, 1f)
        val handleBoxWidthDp = 64.dp
        val handleBoxHeightDp = 24.dp
        val handleBoxWidthPx = with(density) { handleBoxWidthDp.toPx() }
        val handleBoxHeightPx = with(density) { handleBoxHeightDp.toPx() }
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (((curRect.left + curRect.right) / 2f) - handleBoxWidthPx / 2f)
                            .roundToInt(),
                        (curRect.top - handleBoxHeightPx).roundToInt()
                    )
                }
                .size(handleBoxWidthDp, handleBoxHeightDp)
                .graphicsLayer { alpha = handleAlpha },
            contentAlignment = Alignment.Center
        ) {
            OmniboxDragHandle()
        }
    }
}

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
    val focusManager = LocalFocusManager.current

    // 抽屉完全展开到位时的一次性触感反馈（时长/强度读调试页设置，
    // 并遵循「关闭应用内全部震动」总开关）
    val hapticsEnabled = rememberHapticsEnabled()
    val (vibrationDurationMs, vibrationAmplitude) = rememberPressVibrationParams()

    // Track which habit is transitioning to MultiSelect (for shared element)
    var multiSelectTargetHabitId by remember { mutableStateOf<UUID?>(null) }

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

    // Omnibox（主页底部常驻搜索框）：单一查询词，三个 Section 共用一个输入框。
    // Habits / Contacts 由各自 ViewModel 内部做 debounce 过滤（此处单向同步），
    // Records 本期仅占位不过滤；AI 与语音识别均为预留位，无逻辑。
    var homeOmniboxText by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(homeOmniboxText) {
        viewModel.setSearchQuery(homeOmniboxText)
        application?.contactsViewModel?.setSearchQuery(homeOmniboxText)
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
    var sheetScreenWidthPx by remember { mutableFloatStateOf(0f) }
    var sheetScreenHeightPx by remember { mutableFloatStateOf(0f) }
    val deviceCornerRadius = getDeviceCornerRadius()
    // Drawer occupies 3/4 of the screen width.
    val portraitDrawerWidthPx = sheetScreenWidthPx * 3f / 4f
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
        // 若焦点在 Omnibox 输入框上，打开抽屉时取消聚焦并收起键盘
        focusManager.clearFocus()
        val wasFullyOpen = portraitDrawerFraction.value >= 0.999f
        portraitDrawerOpen = true
        scope.launch {
            portraitDrawerFraction.animateTo(
                1f,
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            )
            // 完全展开到位后震动一次（已在全开状态时跳过；收起不震）
            if (!wasFullyOpen && portraitDrawerFraction.value >= 0.999f && hapticsEnabled) {
                vibrateShort(context, vibrationDurationMs, vibrationAmplitude)
            }
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
        if (open) {
            // 滑动展开路径同样需要联动：若焦点在 Omnibox 上则取消聚焦收起键盘
            focusManager.clearFocus()
        }
        val wasFullyOpen = portraitDrawerFraction.value >= 0.999f
        portraitDrawerOpen = open
        scope.launch {
            val widthPx = sheetScreenWidthPx * 3f / 4f
            val initialVelocity = if (widthPx > 1f) releaseVelocityPxPerSec / widthPx else 0f
            portraitDrawerFraction.animateTo(
                targetValue = if (open) 1f else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                initialVelocity = initialVelocity
            )
            // 手势展开到位后震动一次（已在全开状态或收起时跳过）
            if (open && !wasFullyOpen && portraitDrawerFraction.value >= 0.999f && hapticsEnabled) {
                vibrateShort(context, vibrationDurationMs, vibrationAmplitude)
            }
        }
    }

    // ------------------------------------------------------------------
    // Omnibox → AI 对话页 连续形变（拖拽把手无极擦洗，进度驱动）
    // 进度 0 = 主页底部 Omnibox 原位；1 = AIChatScreen 全屏。
    // 形变的是背景面片（与输入框同形状同色），而非输入框本身延展全屏。
    // ------------------------------------------------------------------
    // 使用 rememberSaveable 让形变进度在旋转后保持，对话内容在 ViewModel 中不丢失
    var aiSheetOpen by rememberSaveable { mutableStateOf(false) }
    val aiSheetProgress = rememberSaveable(saver = Saver(
        save = { animatable: Animatable<Float, AnimationVector1D> ->
            if (animatable.isRunning) null else animatable.value
        },
        restore = { value: Float -> Animatable(value) }
    )) { Animatable(0f) }
    var aiDraftText by rememberSaveable { mutableStateOf("") }
    // Omnibox 胶囊在窗口坐标系中的边界（形变起点矩形）
    var omniboxWindowBounds by remember { mutableStateOf(Rect.Zero) }

    val isAiSheetActive by remember {
        derivedStateOf { aiSheetOpen || aiSheetProgress.value > 0.001f }
    }
    // 形变启动后隐藏装饰层把手的视觉（幽灵把手同位接替），避免双把手并存
    val showOmniboxHandle by remember {
        derivedStateOf { aiSheetProgress.value <= 0.001f && !aiSheetOpen }
    }

    fun beginAiSheetHandoff() {
        if (!aiSheetOpen && aiSheetProgress.value <= 0.001f) {
            // 文本草稿交接：Omnibox 已输入内容移交给对话页输入框
            aiDraftText = homeOmniboxText
            homeOmniboxText = ""
        }
    }

    /**
     * 形变收尾动画（三条入口统一走此函数）：
     * - 弹性弹簧产生端点回弹动效；手势路径注入释放初速，末端跟随动量
     *   （初速钳制在 ±8 progress/s，防止极端数值）
     * - 到达端点落定后按全局震动设置震动一次；起点已在端点则跳过
     */
    fun animateAiSheetTo(
        open: Boolean,
        initialVelocityProgressPerSec: Float = 0f
    ) {
        // 收起时将草稿文本归还搜索框（换行替换为空格，搜索框不支持换行）
        if (!open && aiDraftText.isNotEmpty() && homeOmniboxText.isEmpty()) {
            homeOmniboxText = aiDraftText.replace("\n", " ")
        }
        val startValue = aiSheetProgress.value
        val alreadyAtTarget = if (open) startValue >= 0.999f else startValue <= 0.001f
        scope.launch {
            aiSheetProgress.animateTo(
                targetValue = if (open) 1f else 0f,
                animationSpec = if (open) {
                    // 展开：适度回弹 + 末端缓动（比收起 400ms 稍长）
                    spring(
                        dampingRatio = 0.8f,
                        stiffness = 280f
                    )
                } else {
                    // 收起：夸张的 ease-out —— 中段极快、末段缓慢拖尾
                    tween(
                        durationMillis = 400,
                        easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
                    )
                },
                initialVelocity = if (open) initialVelocityProgressPerSec.coerceIn(-8f, 8f) else 0f
            )
            // 回弹落定后震动一次（完全展开与完全收起都触发）
            val endValue = aiSheetProgress.value
            if (!alreadyAtTarget && hapticsEnabled &&
                ((open && endValue >= 0.999f) || (!open && endValue <= 0.001f))
            ) {
                vibrateShort(context, vibrationDurationMs, vibrationAmplitude)
            }
        }
    }

    fun settleAiSheet(open: Boolean, releaseVelocityProgressPerSec: Float) {
        if (open) beginAiSheetHandoff()
        focusManager.clearFocus()
        animateAiSheetTo(open, releaseVelocityProgressPerSec)
    }

    fun expandAiSheetAnimated() {
        beginAiSheetHandoff()
        focusManager.clearFocus()
        animateAiSheetTo(open = true)
    }

    fun collapseAiSheetAnimated() {
        animateAiSheetTo(open = false)
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
            // ⚠️ 守卫必须放在 awaitFirstDown 之后（见 AI 手势条同款注释）：
            // block 在挂起前 return 且无按下指针时，awaitEachGesture 会不经
            // 挂起立即重启下一轮 —— 形变激活期间这里曾因此热自旋死循环。
            val down = awaitFirstDown(requireUnconsumed = false)
            // Omnibox→AI 形变激活期间不参与抽屉手势
            if (isAiSheetActive) return@awaitEachGesture
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
                        val widthPx = sheetScreenWidthPx * 3f / 4f
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
                val fifthOfScreen = sheetScreenWidthPx / 5f
                val shouldOpen = if (abs(totalX) >= fifthOfScreen) {
                    totalX > 0f
                } else {
                    val widthPx = sheetScreenWidthPx * 3f / 4f
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

    // 拖拽全程位移：把手到屏幕顶部的实测距离；边界未上报时以屏高比例兜底，
    // 避免 travelPx≈1 导致任意微小位移把进度瞬间顶满（ANR 根因之一）
    fun aiSheetTravelPx(): Float {
        val boundsTop = omniboxWindowBounds.top
        return if (boundsTop > 1f) boundsTop else sheetScreenHeightPx * 0.55f
    }

    // ------------------------------------------------------------------
    // AI 形变唯一纵向驱动：常驻于主页根层的「底部手势条」。
    // 不挂在装饰/形变面片上——那些节点会随 p 越阈而挂载/卸载，
    // 挂在上面会导致：① 双检测器短暂共存同时消费同一指针流（进度翻倍、
    // 瞬间满屏）；② 拖拽中途节点被卸载、手势协程被取消，松手 settle
    // 永不执行。常驻条生命周期贯穿全程，单一驱动源。
    // ------------------------------------------------------------------
    val aiSheetStripDragModifier = Modifier.pointerInput(Unit) {
        val touchSlop = viewConfiguration.touchSlop
        val velocityTracker = VelocityTracker()
        awaitEachGesture {
            // ⚠️ 守卫必须在 awaitFirstDown 之后：awaitEachGesture 在 block 不经
            // 挂起就返回时会立即重启下一轮迭代（无指针按下时 finally 直接放行），
            // 形成主线程热自旋 —— 这正是完全展开态 ANR 的根因。
            // awaitFirstDown 本身是挂起点，其后 return 是安全的（finally 会等抬手）。
            val down = awaitFirstDown(requireUnconsumed = false)
            // 完全展开后不接管任何拖拽（避免劫持聊天页底部区域）
            if (aiSheetProgress.value >= 0.999f) return@awaitEachGesture
            // 抽屉开启期间不参与
            if (portraitDrawerFraction.value > 0.01f) return@awaitEachGesture
            velocityTracker.resetTracking()
            var totalY = 0f
            var engaged = false

            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break

                val delta = change.positionChange()
                totalY += delta.y

                if (engaged) {
                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                }
                if (!change.pressed) break

                if (!engaged) {
                    if (change.isConsumed) break
                    // 仅上滑意图才接管（下拉收回走遮罩点击/返回键）
                    if (abs(totalY) > touchSlop && totalY < 0f &&
                        aiSheetProgress.value < 0.999f
                    ) {
                        engaged = true
                        beginAiSheetHandoff()
                        velocityTracker.resetTracking()
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                    } else if (abs(totalY) > touchSlop) {
                        break
                    }
                }

                if (engaged) {
                    change.consume()
                    val dy = delta.y
                    scope.launch {
                        val travelPx = aiSheetTravelPx()
                        aiSheetProgress.snapTo(
                            (aiSheetProgress.value - dy / travelPx).coerceIn(0f, 1f)
                        )
                    }
                }
            }

            if (engaged) {
                val vy = velocityTracker.calculateVelocity().y // 上滑为负
                val travelPx = aiSheetTravelPx()
                val velocityProgressPerSec = -vy / travelPx
                val projected =
                    aiSheetProgress.value + velocityProgressPerSec * DrawerFlingProjectionSeconds
                // 触发规则（与抽屉一致的两级判定）：
                // ① 本次手势位移 ≥ 1/5 屏宽时无论速度按方向直接展开（上滑）
                // ② 更短手势用动量投影取最近锚点——快速轻扫短距即触发，
                //    慢速短拖保持原位，速度读数失真时仅退化为就近吸附
                val fifthOfScreen = sheetScreenWidthPx / 5f
                val shouldOpen = if (abs(totalY) >= fifthOfScreen) {
                    totalY < 0f
                } else {
                    projected > 0.5f
                }
                settleAiSheet(
                    open = shouldOpen,
                    releaseVelocityProgressPerSec = velocityProgressPerSec
                )
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
                        onCreateHabitSelection = { expandAiSheetAnimated() },
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
                        searchQuery = homeOmniboxText,
                        onClearSearch = { homeOmniboxText = "" }
                    )
                }
                HomeSection.Contacts -> {
                    ContactsScreenContent(
                        modifier = modifier,
                        application = application,
                        scrollBehavior = contactsScrollBehavior,
                        listState = contactsScrollState
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

    // 新建习惯 FAB 本期隐藏（omnibox 迭代）：手动创建入口改为
    // 习惯页顶部下拉释放展开搜索框（AI 形变）

    if (effectiveIsPermanentDrawer) {
        // 外层 Box 让 AiSheetOverlay 能覆盖全屏（包括 Drawer）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged {
                    sheetScreenWidthPx = it.width.toFloat()
                    sheetScreenHeightPx = it.height.toFloat()
                }
        ) {
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
                    homeBody(
                        Modifier
                            .fillMaxSize()
                            // 列表可滚动范围在 omnibox 上方结束
                            .padding(bottom = OmniboxContentBottomClearance)
                            // 键盘弹出时列表显示高度随之抬升，习惯不被遮挡
                            .imePadding()
                    )

                    // Omnibox（常驻搜索框 + 背景渐变过渡 + AI 形变拖拽源）
                    // 注意：形变期间保持挂载不卸载——展开后被全屏面片覆盖不可见，
                    // 但节点存活使拖拽手势协程贯穿全程；同时保证输入框可正常聚焦
                        BottomOmniboxWithFade(
                            query = homeOmniboxText,
                            onQueryChange = { homeOmniboxText = it },
                            onHandleTap = { expandAiSheetAnimated() },
                            showHandle = showOmniboxHandle,
                            progress = aiSheetProgress.value,
                            onPillBounds = { omniboxWindowBounds = it },
                            dragModifier = aiSheetStripDragModifier
                        )
                }
            }
        }

        // AI 形变覆盖层（与手机竖屏一致的覆盖方案，覆盖全屏含 Drawer）
        if (isAiSheetActive && omniboxWindowBounds.width > 0f && application != null) {
            AiSheetOverlay(
                progress = aiSheetProgress,
                omniboxBounds = omniboxWindowBounds,
                screenWidthPx = sheetScreenWidthPx,
                screenHeightPx = sheetScreenHeightPx,
                cornerRadius = deviceCornerRadius,
                application = application,
                habits = habits,
                onCollapse = { collapseAiSheetAnimated() },
                onNavigateToSettings = onNavigateToSettings,
                onEditHabitById = { habitId: UUID ->
                    val habit = habits.firstOrNull { it.id == habitId }
                    if (habit != null) {
                        collapseAiSheetAnimated()
                        scope.launch {
                            clickHandler.processClick { onEditHabit(habit) }
                        }
                    }
                },
                onManualCreateHabit = {
                    collapseAiSheetAnimated()
                    scope.launch {
                        clickHandler.processClick { onCreateHabit() }
                    }
                },
                draftText = aiDraftText
            )
        }
        } // outer Box
    } else if (effectiveUseRail) {
        // NavigationRail layout for landscape phones
        // Rail occupies full height on left, content area on right
        // 外层 Box 让 AiSheetOverlay 能覆盖全屏（包括 Rail）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .onSizeChanged {
                    sheetScreenWidthPx = it.width.toFloat()
                    sheetScreenHeightPx = it.height.toFloat()
                }
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
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
                        ).padding(bottom = OmniboxContentBottomClearance)
                        // 键盘弹出时列表显示高度随之抬升
                        .imePadding()
                    )

                    // Omnibox（常驻搜索框 + 背景渐变过渡，替代原 FAB 位置带）；形变激活期间由形变层接管
                        BottomOmniboxWithFade(
                            query = homeOmniboxText,
                            onQueryChange = { homeOmniboxText = it },
                            onHandleTap = { expandAiSheetAnimated() },
                            showHandle = showOmniboxHandle,
                            progress = aiSheetProgress.value,
                            onPillBounds = { omniboxWindowBounds = it },
                            dragModifier = aiSheetStripDragModifier
                        )
                }
            }
            }

            // AI 形变覆盖层（与手机竖屏一致的覆盖方案）
            if (isAiSheetActive && omniboxWindowBounds.width > 0f && application != null) {
                AiSheetOverlay(
                    progress = aiSheetProgress,
                    omniboxBounds = omniboxWindowBounds,
                    screenWidthPx = sheetScreenWidthPx,
                    screenHeightPx = sheetScreenHeightPx,
                    cornerRadius = deviceCornerRadius,
                    application = application,
                    habits = habits,
                    onCollapse = { collapseAiSheetAnimated() },
                    onNavigateToSettings = onNavigateToSettings,
                    onEditHabitById = { habitId: UUID ->
                        val habit = habits.firstOrNull { it.id == habitId }
                        if (habit != null) {
                            collapseAiSheetAnimated()
                            scope.launch {
                                clickHandler.processClick { onEditHabit(habit) }
                            }
                        }
                    },
                    onManualCreateHabit = {
                        collapseAiSheetAnimated()
                        scope.launch {
                            clickHandler.processClick { onCreateHabit() }
                        }
                    },
                    draftText = aiDraftText
                )
            }
        }
    } else {
        // Portrait mode: reveal drawer (same-plane slide).
        // Drawer and main page are laid out side by side on ONE virtual plane:
        // opening translates both right by drawerWidth * fraction, so the
        // drawer slides in from off-screen while the page exits right and
        // gets dimmed. Offsets read fraction in the placement phase, so the
        // animation causes no recomposition.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .onSizeChanged {
                    sheetScreenWidthPx = it.width.toFloat()
                    sheetScreenHeightPx = it.height.toFloat()
                }
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
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            // 列表可滚动范围在 omnibox 上方结束，不在其下滑动
                            .padding(bottom = OmniboxContentBottomClearance)
                            // 键盘弹出时列表显示高度随之抬升，习惯不被遮挡
                            .imePadding()
                    ) {
                        homeBody(Modifier.fillMaxSize())
                    }
                }

                // Omnibox（常驻搜索框 + 背景渐变过渡，随页面一同滑动、被遮罩压暗 + AI 形变拖拽源）
                // 形变期间保持挂载不卸载（同上）
                    BottomOmniboxWithFade(
                        query = homeOmniboxText,
                        onQueryChange = { homeOmniboxText = it },
                        onHandleTap = { expandAiSheetAnimated() },
                        showHandle = showOmniboxHandle,
                        progress = aiSheetProgress.value,
                        onPillBounds = { omniboxWindowBounds = it },
                        dragModifier = aiSheetStripDragModifier
                    )

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

            // ------------------------------------------------------------------
            // Omnibox → AI 对话页 形变层：实现整体隔离在文件级 AiSheetOverlay 内，
            // 进度读取不进入 HomeScreen 组合作用域——动画帧只重组形变层自身，
            // 绝不波及主树（否则整屏逐帧重组 + 聊天树不可跳过 = 松手 ANR）。
            // 几何未就绪（胶囊边界尚未上报）时不渲染，避免坏帧
            // ------------------------------------------------------------------
            if (isAiSheetActive && omniboxWindowBounds.width > 0f && application != null) {
                AiSheetOverlay(
                    progress = aiSheetProgress,
                    omniboxBounds = omniboxWindowBounds,
                    screenWidthPx = sheetScreenWidthPx,
                    screenHeightPx = sheetScreenHeightPx,
                    cornerRadius = deviceCornerRadius,
                    application = application,
                    habits = habits,
                    onCollapse = { collapseAiSheetAnimated() },
                    onNavigateToSettings = onNavigateToSettings,
                    onEditHabitById = { habitId ->
                        val habit = habits.firstOrNull { it.id == habitId }
                        if (habit != null) {
                            collapseAiSheetAnimated()
                            scope.launch {
                                clickHandler.processClick { onEditHabit(habit) }
                            }
                        }
                    },
                    onManualCreateHabit = {
                        collapseAiSheetAnimated()
                        scope.launch {
                            clickHandler.processClick { onCreateHabit() }
                        }
                    },
                    draftText = aiDraftText
                )
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
 * 主页底部常驻 Omnibox（Google 搜索框风格）。
 *
 * 100% 圆角胶囊形：左侧放大镜图标，中间提示文本「搜索与AI」/输入内容，
 * 右侧麦克风图标（语音识别预留位，本期无行为）；有输入内容时麦克风
 * 替换为清除按钮。点击任意处聚焦并弹出输入法，调用方通过 ime insets
 * padding 适配键盘高度。
 *
 * 无阴影；填充色用 surfaceContainerHighest，与习惯卡片（surfaceContainer）
 * 区分。
 */
@Composable
private fun HomeOmnibox(
    query: String,
    onQueryChange: (String) -> Unit,
    onUpdateWindowBounds: (Rect) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    Surface(
        modifier = modifier
            .onGloballyPositioned { coords ->
                // 上报胶囊在窗口坐标系中的边界，作为 AI 形变层的起点矩形
                onUpdateWindowBounds(
                    Rect(coords.positionInWindow(), coords.size.toSize())
                )
            },
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clickable { focusRequester.requestFocus() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp)
            )
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 14.dp)
                    .focusRequester(focusRequester),
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                text = stringResource(id = R.string.main_omnibox_hint),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                        innerTextField()
                    }
                }
            )
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(id = R.string.accessibility_omnibox_clear),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // 语音识别预留位：本期无行为
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 20.dp)
                )
            }
        }
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
