package io.github.darrindeyoung791.habitpulse.ui.screens

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.SharedTransitionScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.darrindeyoung791.habitpulse.HabitPulseApplication
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.data.model.Habit
import io.github.darrindeyoung791.habitpulse.data.model.RepeatCycle
import io.github.darrindeyoung791.habitpulse.data.repository.HabitRepository
import io.github.darrindeyoung791.habitpulse.ui.theme.HabitPulseTheme
import io.github.darrindeyoung791.habitpulse.ui.utils.rememberDebounceClickHandler
import io.github.darrindeyoung791.habitpulse.utils.OnboardingPreferences
import io.github.darrindeyoung791.habitpulse.viewmodel.HabitViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitScreenContent(
    modifier: Modifier = Modifier,
    application: HabitPulseApplication? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    listState: LazyListState = remember { LazyListState() },
    waterfallScrollState: ScrollState = remember { ScrollState(0) },
    bringIntoViewRequester: BringIntoViewRequester = remember { BringIntoViewRequester() },
    forceTabletLandscape: Boolean = false,
    onCreateHabit: () -> Unit,
    onEditHabit: (Habit) -> Unit,
    onNavigateToMultiSelect: (UUID) -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null,
    nestedScrollConnection: androidx.compose.ui.input.nestedscroll.NestedScrollConnection? = null,
    multiSelectTargetHabitId: UUID? = null,
    isSearchActive: Boolean = false,
    onSearchActiveChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val clickHandler = rememberDebounceClickHandler()
    val scope = rememberCoroutineScope()

    val viewModel: HabitViewModel = if (application != null) {
        application.habitViewModel
    } else {
        remember {
            val fakeHabitDao = FakeHabitDao()
            val fakeCompletionDao = FakeHabitCompletionDao()
            val fakeRepository = HabitRepository(fakeHabitDao, fakeCompletionDao)
            val fakeOnboardingPreferences = OnboardingPreferences(context.applicationContext)
            HabitViewModel(fakeRepository, fakeOnboardingPreferences)
        }
    }

    val habits by viewModel.habitsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val filteredHabits by viewModel.filteredHabitsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle(initialValue = true)
    val newlyAddedHabitId by viewModel.newlyAddedHabitId.collectAsStateWithLifecycle(initialValue = null)
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle(initialValue = "")
    val rewardSheetHabit by viewModel.rewardSheetHabit.collectAsStateWithLifecycle(initialValue = null)
    val showRewardSheet by viewModel.showRewardSheet.collectAsStateWithLifecycle(initialValue = false)

    var hasLoadedHabits by remember { mutableStateOf(false) }
    var isSearchFocused by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    BackHandler(enabled = isSearchActive) {
        onSearchActiveChange(false)
        viewModel.clearSearch()
        focusManager.clearFocus()
    }

    LaunchedEffect(isLoading) {
        if (!isLoading) {
            hasLoadedHabits = true
        }
    }

    LaunchedEffect(isSearchActive) {
        if (!isSearchActive) {
            viewModel.clearSearch()
            focusManager.clearFocus()
        }
    }

    LaunchedEffect(newlyAddedHabitId) {
        if (newlyAddedHabitId != null) {
            delay(300)
            viewModel.resetNewlyAddedHabitId()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
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
                onClearSearch = { viewModel.setSearchQuery("") },
                onBackClick = {
                    onSearchActiveChange(false)
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

        Box(
            modifier = Modifier
                .weight(1f)
                .animateContentSize(
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 200f)
                )
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (isSearchActive && filteredHabits.isEmpty()) {
                SearchEmptyState(
                    modifier = Modifier.fillMaxSize(),
                    onClearSearch = {
                        onSearchActiveChange(false)
                        viewModel.clearSearch()
                    }
                )
            } else if (!isSearchActive && !hasLoadedHabits) {
                Box(modifier = Modifier.fillMaxSize()) {}
            } else if (!isSearchActive && habits.isEmpty()) {
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
                    habits = if (isSearchActive) filteredHabits else habits,
                    onHabitClick = { onEditHabit(it) },
                    onCheckIn = { habit ->
                        viewModel.incrementCompletionCount(habit)
                        viewModel.showRewardSheet(habit)
                    },
                    onUndoCompletion = { viewModel.undoHabitCompletion(it) },
                    onDeleteHabit = { habit ->
                        viewModel.deleteHabit(habit)
                        application?.recordsViewModel?.refreshRecords()
                    },
                    onNavigateToMultiSelect = onNavigateToMultiSelect,
                    nestedScrollConnection = nestedScrollConnection,
                    newlyAddedHabitId = newlyAddedHabitId,
                    listState = listState,
                    waterfallScrollState = waterfallScrollState,
                    bringIntoViewRequester = bringIntoViewRequester,
                    forceTabletLandscape = forceTabletLandscape,
                    searchQuery = searchQuery,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedContentScope = animatedContentScope,
                    multiSelectTargetHabitId = multiSelectTargetHabitId
                )
            }
        }
    }

    if (showRewardSheet && rewardSheetHabit != null) {
        val currentHabit = rewardSheetHabit!!
        val displayCompletionCount = currentHabit.completionCount + 1

        RewardBottomSheet(
            habit = currentHabit,
            completionCount = displayCompletionCount,
            onDismiss = { viewModel.dismissRewardSheet() },
            onComplete = { viewModel.dismissRewardSheet() },
            onNotifySupervisor = { viewModel.dismissRewardSheet() },
            onSkipNotification = { viewModel.dismissRewardSheet() }
        )
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
    listState: LazyListState = remember { LazyListState() },
    waterfallScrollState: ScrollState,
    bringIntoViewRequester: BringIntoViewRequester? = null,
    forceTabletLandscape: Boolean = false,
    searchQuery: String = "",
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null,
    multiSelectTargetHabitId: UUID? = null
) {
    val configuration = LocalConfiguration.current
    var screenWidthDp = configuration.screenWidthDp
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (forceTabletLandscape && isLandscape && screenWidthDp < 840) {
        screenWidthDp = 840
    }

    val useStaggeredGrid = isLandscape && screenWidthDp >= 840
    val horizontalPadding = 16.dp

    if (useStaggeredGrid) {
        val column1Habits = habits.filterIndexed { index, _ -> index % 2 == 0 }
        val column2Habits = habits.filterIndexed { index, _ -> index % 2 == 1 }

        val waterfallModifier = if (nestedScrollConnection != null) modifier.nestedScroll(nestedScrollConnection) else modifier

        ScrollableWaterfallWithScrollbar(
            modifier = waterfallModifier,
            scrollState = waterfallScrollState
        ) {
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
                            modifier = Modifier.fillMaxWidth(),
                            sharedTransitionScope = sharedTransitionScope,
                            animatedContentScope = animatedContentScope,
                            isMultiSelectTarget = (habit.id == multiSelectTargetHabitId)
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
                            modifier = Modifier.fillMaxWidth(),
                            sharedTransitionScope = sharedTransitionScope,
                            animatedContentScope = animatedContentScope,
                            isMultiSelectTarget = (habit.id == multiSelectTargetHabitId)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    } else {
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
                    searchQuery = searchQuery,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedContentScope = animatedContentScope,
                    isMultiSelectTarget = (habit.id == multiSelectTargetHabitId)
                )
            }
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

    val isAtTop = remember { mutableStateOf(true) }
    val isAtBottom = remember { mutableStateOf(false) }

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

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        val layoutInfo = listState.layoutInfo
        val totalCount = layoutInfo.totalItemsCount
        val visibleItems = layoutInfo.visibleItemsInfo

        isAtTop.value = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0

        if (totalCount > 0 && visibleItems.isNotEmpty()) {
            val lastVisibleItem = visibleItems.last()
            val isLastItemVisible = lastVisibleItem.index == totalCount - 1
            val viewportHeight = layoutInfo.viewportSize.height
            val itemEnd = lastVisibleItem.offset + lastVisibleItem.size
            isAtBottom.value = isLastItemVisible && itemEnd <= viewportHeight
        } else {
            isAtBottom.value = true
        }
    }

    val topGradientAlpha by animateFloatAsState(
        targetValue = if (isAtTop.value) 0f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "topGradientAlpha"
    )
    val bottomGradientAlpha by animateFloatAsState(
        targetValue = if (isAtBottom.value) 0f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "bottomGradientAlpha"
    )

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
                .fillMaxWidth()
                .height(16.dp)
                .align(Alignment.TopCenter)
                .alpha(topGradientAlpha)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            Color.Transparent
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .align(Alignment.BottomCenter)
                .alpha(bottomGradientAlpha)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )

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

                val viewportHeightPx = layoutInfo.viewportSize.height.toFloat()
                val sumVisibleHeights = visibleItems.sumOf { it.size }.toFloat()
                val averageItemHeightPx = (sumVisibleHeights / visibleCount).coerceAtLeast(1f)
                val remainingItems = (totalCount - visibleCount).coerceAtLeast(0)
                val estimatedTotalContentHeightPx = (sumVisibleHeights + remainingItems * averageItemHeightPx).coerceAtLeast(viewportHeightPx)

                val currentScrollPx = listState.firstVisibleItemIndex * averageItemHeightPx + listState.firstVisibleItemScrollOffset.toFloat()
                val totalScrollablePx = (estimatedTotalContentHeightPx - viewportHeightPx).coerceAtLeast(1f)
                val scrollFraction = (currentScrollPx / totalScrollablePx).coerceIn(0f, 1f)

                val indicatorHeightFraction = (viewportHeightPx / estimatedTotalContentHeightPx).coerceIn(0.03f, 1f)
                val indicatorHeightPx = viewportHeightPx * indicatorHeightFraction
                val offsetYPx = (viewportHeightPx - indicatorHeightPx) * scrollFraction

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

    val isAtTop = remember { mutableStateOf(true) }
    val isAtBottom = remember { mutableStateOf(false) }

    LaunchedEffect(scrollState.isScrollInProgress) {
        isScrollbarVisible.value = true
        if (!scrollState.isScrollInProgress) {
            delay(1200)
            if (!scrollState.isScrollInProgress) {
                isScrollbarVisible.value = false
            }
        }
    }

    LaunchedEffect(scrollState.value) {
        isAtTop.value = scrollState.value == 0
        isAtBottom.value = scrollState.value == scrollState.maxValue
    }

    val topGradientAlpha by animateFloatAsState(
        targetValue = if (isAtTop.value) 0f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "waterfallTopGradientAlpha"
    )
    val bottomGradientAlpha by animateFloatAsState(
        targetValue = if (isAtBottom.value) 0f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "waterfallBottomGradientAlpha"
    )

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
                .fillMaxWidth()
                .height(16.dp)
                .align(Alignment.TopCenter)
                .alpha(topGradientAlpha)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            Color.Transparent
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .align(Alignment.BottomCenter)
                .alpha(bottomGradientAlpha)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )

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
    onNavigateToMultiSelect: (habitId: UUID) -> Unit = {},
    modifier: Modifier = Modifier,
    isNewlyAdded: Boolean = false,
    searchQuery: String = "",
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null,
    isMultiSelectTarget: Boolean = false
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

    var isVisible by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    var isInitiallyVisible by remember { mutableStateOf(!isNewlyAdded) }

    LaunchedEffect(isNewlyAdded) {
        if (isNewlyAdded) {
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
            Card(
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
                    )
                    .then(
                        if (isMultiSelectTarget && sharedTransitionScope != null && animatedContentScope != null) {
                            with(sharedTransitionScope) {
                                Modifier.sharedElement(
                                    sharedContentState = rememberSharedContentState(key = "card-${habit.id}"),
                                    animatedVisibilityScope = animatedContentScope,
                                    boundsTransform = { _, _ ->
                                        tween(durationMillis = 350, easing = FastOutSlowInEasing)
                                    }
                                )
                            }
                        } else {
                            Modifier
                        }
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = highlightText(habit.title, searchQuery),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.then(
                                if (isMultiSelectTarget && sharedTransitionScope != null && animatedContentScope != null) {
                                    with(sharedTransitionScope) {
                                        Modifier.sharedElement(
                                            sharedContentState = rememberSharedContentState(key = "title-${habit.id}"),
                                            animatedVisibilityScope = animatedContentScope,
                                            boundsTransform = { _, _ ->
                                                tween(durationMillis = 350, easing = FastOutSlowInEasing)
                                            }
                                        )
                                    }
                                } else {
                                    Modifier
                                }
                            )
                        )

                        Text(
                            text = stringResource(id = R.string.habit_card_completed_count, habit.completionCount),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
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

                    CheckInButton(
                        onClick = onCheckIn,
                        contentDescription = stringResource(id = R.string.accessibility_habit_checkin, habit.title)
                    )
                }
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                if (habit.completionCount > 0) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(id = R.string.habit_card_menu_undo)) },
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

                DropdownMenuItem(
                    text = { Text(text = stringResource(id = R.string.habit_card_menu_edit)) },
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

                DropdownMenuItem(
                    text = { Text(text = stringResource(id = R.string.habit_context_menu_multi_select)) },
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

                DropdownMenuItem(
                    text = { Text(text = stringResource(id = R.string.habit_card_menu_delete)) },
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

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = {
                        Text(
                            text = stringResource(id = R.string.habit_card_delete_confirm_title),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start,
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
                                isVisible = false
                                scope.launch {
                                    delay(150)
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

                val hasSupervisors = supervisorEmails.isNotEmpty() || supervisorPhones.isNotEmpty()
                if (hasSupervisors) {
                    Spacer(modifier = Modifier.height(16.dp))

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

// ============= Search Bar Fixed =============

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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
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

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
    }
}

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

            if (matchIndex > startIndex) {
                append(text.substring(startIndex, matchIndex))
            }

            withStyle(style = SpanStyle(background = MaterialTheme.colorScheme.primaryContainer)) {
                append(text.substring(matchIndex, matchIndex + query.length))
            }

            startIndex = matchIndex + query.length
        }
    }

    return annotatedString
}

// ============= Preview Fake Data =============

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun HabitScreenPreview() {
    HabitPulseTheme(darkTheme = false, dynamicColor = false) {
        HabitScreenContent(
            application = null,
            onCreateHabit = {},
            onEditHabit = {},
            onNavigateToMultiSelect = {}
        )
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
 * Fake HabitDao implementation for Android Studio Preview.
 */
@Suppress("unused")
internal class FakeHabitDao : io.github.darrindeyoung791.habitpulse.data.database.dao.HabitDao {
    private val habits = mutableListOf<io.github.darrindeyoung791.habitpulse.data.model.Habit>()

    init {
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
        return flowOf(habits)
    }

    override suspend fun getAllHabits(): List<io.github.darrindeyoung791.habitpulse.data.model.Habit> = habits

    override fun getHabitByIdFlow(id: UUID): kotlinx.coroutines.flow.Flow<io.github.darrindeyoung791.habitpulse.data.model.Habit?> {
        return flowOf(habits.find { it.id == id })
    }

    override suspend fun getHabitById(id: UUID): io.github.darrindeyoung791.habitpulse.data.model.Habit? {
        return habits.find { it.id == id }
    }

    override fun getIncompleteHabitsFlow(): kotlinx.coroutines.flow.Flow<List<io.github.darrindeyoung791.habitpulse.data.model.Habit>> {
        return flowOf(habits.filter { !it.completedToday })
    }

    override fun getCompletedHabitsFlow(): kotlinx.coroutines.flow.Flow<List<io.github.darrindeyoung791.habitpulse.data.model.Habit>> {
        return flowOf(habits.filter { it.completedToday })
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

    override suspend fun updateCompletionStatus(id: UUID, completed: Boolean, timestamp: Long) {
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

    override suspend fun undoCompletionStatus(id: UUID, timestamp: Long) {
        val index = habits.indexOfFirst { it.id == id }
        if (index >= 0) {
            habits[index] = habits[index].copy(
                completedToday = false,
                completionCount = maxOf(0, habits[index].completionCount - 1),
                modifiedDate = timestamp
            )
        }
    }

    override suspend fun incrementCompletionCount(id: UUID, timestamp: Long) {
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
        return flowOf(habits.size)
    }

    override fun searchHabitsFlow(query: String): kotlinx.coroutines.flow.Flow<List<io.github.darrindeyoung791.habitpulse.data.model.Habit>> {
        val searchQuery = query.trim('%')
        return flowOf(
            habits.filter { habit ->
                habit.title.contains(searchQuery, ignoreCase = true) ||
                habit.notes.contains(searchQuery, ignoreCase = true)
            }
        )
    }

    override fun getHabitsBySortOrderFlow(): kotlinx.coroutines.flow.Flow<List<io.github.darrindeyoung791.habitpulse.data.model.Habit>> {
        return flowOf(habits.sortedBy { it.sortOrder })
    }

    override suspend fun updateSortOrder(id: UUID, sortOrder: Int, timestamp: Long) {
        val index = habits.indexOfFirst { it.id == id }
        if (index >= 0) {
            habits[index] = habits[index].copy(sortOrder = sortOrder, modifiedDate = timestamp)
        }
    }

    override suspend fun deleteHabitsByIds(habitIds: Set<UUID>) {
        habits.removeAll { it.id in habitIds }
    }
}

/**
 * Fake HabitCompletionDao implementation for Android Studio Preview.
 */
@Suppress("unused")
internal class FakeHabitCompletionDao : io.github.darrindeyoung791.habitpulse.data.database.dao.HabitCompletionDao {
    private val completions = mutableListOf<io.github.darrindeyoung791.habitpulse.data.model.HabitCompletion>()

    override fun getCompletionsByHabitIdFlow(habitId: UUID): kotlinx.coroutines.flow.Flow<List<io.github.darrindeyoung791.habitpulse.data.model.HabitCompletion>> {
        return flowOf(completions.filter { it.habitId == habitId })
    }

    override fun getAllCompletionsFlow(): kotlinx.coroutines.flow.Flow<List<io.github.darrindeyoung791.habitpulse.data.model.HabitCompletion>> {
        return flowOf(completions.toList())
    }

    override suspend fun getCompletionsByHabitId(habitId: UUID): List<io.github.darrindeyoung791.habitpulse.data.model.HabitCompletion> {
        return completions.filter { it.habitId == habitId }
    }

    override suspend fun getCompletionsByHabitIdAndDate(
        habitId: UUID,
        date: String
    ): List<io.github.darrindeyoung791.habitpulse.data.model.HabitCompletion> {
        return completions.filter { it.habitId == habitId && it.completedDateLocal == date }
    }

    override suspend fun getCompletionsByHabitIdAndDateRange(
        habitId: UUID,
        startDate: String,
        endDate: String
    ): List<io.github.darrindeyoung791.habitpulse.data.model.HabitCompletion> {
        return completions.filter { it.habitId == habitId && it.completedDateLocal in startDate..endDate }
    }

    override suspend fun getCompletionsByDate(date: String): List<io.github.darrindeyoung791.habitpulse.data.model.HabitCompletion> {
        return completions.filter { it.completedDateLocal == date }
    }

    override suspend fun getTodayCompletionCount(habitId: UUID, date: String): Int {
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

    override suspend fun deleteByHabitId(habitId: UUID) {
        completions.removeAll { it.habitId == habitId }
    }

    override suspend fun deleteByDate(date: String) {
        completions.removeAll { it.completedDateLocal == date }
    }

    override suspend fun deleteAll() {
        completions.clear()
    }

    override fun getCompletionCount(): kotlinx.coroutines.flow.Flow<Int> {
        return flowOf(completions.size)
    }

    override suspend fun getCompletionCountByHabitId(habitId: UUID): Int {
        return completions.count { it.habitId == habitId }
    }
}
