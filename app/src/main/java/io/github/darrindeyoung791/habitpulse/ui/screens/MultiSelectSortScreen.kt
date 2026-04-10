package io.github.darrindeyoung791.habitpulse.ui.screens

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.darrindeyoung791.habitpulse.HabitPulseApplication
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.data.model.Habit
import io.github.darrindeyoung791.habitpulse.ui.theme.HabitPulseTheme
import io.github.darrindeyoung791.habitpulse.ui.utils.rememberDebounceClickHandler
import io.github.darrindeyoung791.habitpulse.ui.utils.rememberNavigationGuard
import io.github.darrindeyoung791.habitpulse.viewmodel.HabitViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.util.UUID

/**
 * 多选与排序界面
 *
 * 功能：
 * - 长按习惯卡片进入此界面
 * - 支持多选习惯
 * - 支持拖拽排序
 * - 支持批量删除
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiSelectSortScreen(
    onNavigateBack: () -> Unit,
    viewModel: HabitViewModel,
    application: HabitPulseApplication? = null,
    navController: androidx.navigation.NavHostController? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null
) {
    val hapticFeedback = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // Collect state - use habitsFlow to get habits in their current order
    val allHabits by viewModel.habitsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val selectedHabitIds by viewModel.selectedHabitIds.collectAsStateWithLifecycle()

    // Track drag reordering - initialize with allHabits and update on drag
    val draggedOrder = remember { mutableStateOf<List<Habit>>(emptyList()) }

    // Track whether the initial staggered animation phase has completed
    // Once true, newly composed items (from scrolling) should skip animation
    var initialAnimationComplete by remember { mutableStateOf(false) }
    
    // Estimate max delay needed for initial staggered animation
    // We'll use this to know when all initial animations are done
    val estimatedMaxAnimationTime = remember(allHabits) {
        val maxDistance = allHabits.size
        (maxDistance * 30L + 500L) // max delay + spring animation duration
    }

    // Create lazy list state
    val lazyListState = rememberLazyListState()
    // Track whether we've already scrolled to the initial habit
    var hasScrolledToInitialHabit by remember { mutableStateOf(false) }
    // Get density for scroll offset calculation
    val density = LocalDensity.current

    // Initialize draggedOrder when allHabits is loaded or changed
    LaunchedEffect(allHabits) {
        if (allHabits != draggedOrder.value) {
            draggedOrder.value = allHabits.toList()
        }
    }

    // Mark initial animation as complete after estimated time
    // This ensures newly composed items (from scrolling) skip the animation
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(estimatedMaxAnimationTime)
        initialAnimationComplete = true
    }

    // Scroll to the initially selected habit (from long-press)
    // Only trigger once when the screen is first displayed
    LaunchedEffect(Unit) {
        // Wait for the list to be populated
        if (draggedOrder.value.isEmpty()) {
            // Wait for the next frame where draggedOrder will be populated
            snapshotFlow { draggedOrder.value }
                .first { it.isNotEmpty() }
        }

        if (hasScrolledToInitialHabit) return@LaunchedEffect

        val selectedIds = selectedHabitIds
        // Only scroll if exactly one habit is selected (the long-pressed one)
        if (selectedIds.size == 1 && draggedOrder.value.isNotEmpty()) {
            val targetHabitId = selectedIds.first()
            val index = draggedOrder.value.indexOfFirst { it.id == targetHabitId }
            if (index != -1) {
                // Scroll to the item with offset to position it in upper half
                // -100dp offset to place it in upper portion of screen
                val scrollOffset = with(density) { (-100.dp).toPx().toInt() }
                lazyListState.scrollToItem(index, scrollOffset)
                hasScrolledToInitialHabit = true
            }
        }
    }

    // Create reorderable state - update list on every move to trigger recomposition
    val reorderableState = rememberReorderableLazyListState(lazyListState = lazyListState) { from, to ->
        if (draggedOrder.value.isNotEmpty()) {
            // NOTE: from.index/to.index may include non-reorderable header items,
            // so use key-based lookup against draggedOrder to get correct indexes.
            val fromIndex = draggedOrder.value.indexOfFirst { it.id == from.key }
            val toIndex = draggedOrder.value.indexOfFirst { it.id == to.key }

            if (fromIndex != -1 && toIndex != -1) {
                val newList = draggedOrder.value.toMutableList()
                val item = newList.removeAt(fromIndex)
                newList.add(toIndex, item)
                draggedOrder.value = newList

                // Clear all selections when user reorders - indicates intent to save positions, not delete
                if (selectedHabitIds.isNotEmpty()) {
                    viewModel.clearAllSelections()
                }
            }
        }

        // Just trigger haptic feedback
        hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }

    // Dialog state for delete confirmation
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Debounce click handler to prevent rapid consecutive clicks
    val clickHandler = rememberDebounceClickHandler()
    // Navigation guard to prevent navigating back beyond home screen
    val navigationGuard = navController?.let { rememberNavigationGuard(it) }

    // Track the index of the initially selected habit (from long-press)
    var initiallySelectedHabitId by remember { mutableStateOf<UUID?>(null) }
    
    LaunchedEffect(selectedHabitIds) {
        // Capture the initially selected habit ID when first entering multi-select
        if (initiallySelectedHabitId == null && selectedHabitIds.size == 1) {
            initiallySelectedHabitId = selectedHabitIds.first()
        }
    }

    val displayHabits = if (draggedOrder.value.isNotEmpty()) draggedOrder.value else allHabits
    
    // Find the index of the initially selected habit for staggered animation
    val initiallySelectedIndex = initiallySelectedHabitId?.let { selectedId ->
        displayHabits.indexOfFirst { it.id == selectedId }
    } ?: -1

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.multi_select_sort_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                clickHandler.processClick {
                                    // Exit multi-select mode without scrolling to top
                                    viewModel.exitMultiSelectMode()
                                    // Use navigation guard for safe back navigation
                                    if (navigationGuard != null) {
                                        navigationGuard.safePopBackStack()
                                    } else {
                                        onNavigateBack()
                                    }
                                }
                            }
                        }
                    ) {
                        Text(
                            text = stringResource(id = R.string.multi_select_sort_cancel),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                actions = {
                    // Show "Delete" when items are selected, "Save" when no items selected
                    TextButton(
                        onClick = {
                            scope.launch {
                                clickHandler.processClick {
                                    if (selectedHabitIds.isNotEmpty()) {
                                        showDeleteDialog = true
                                    } else {
                                        // Save the current order to database using draggedOrder.value
                                        draggedOrder.value.forEachIndexed { index, habit ->
                                            viewModel.updateHabitSortOrder(habit.id, index)
                                        }
                                        viewModel.exitMultiSelectMode()
                                        // Scroll to top only when saving
                                        viewModel.requestScrollToTop()
                                        // Use navigation guard for safe back navigation
                                        if (navigationGuard != null) {
                                            navigationGuard.safePopBackStack()
                                        } else {
                                            onNavigateBack()
                                        }
                                    }
                                }
                            }
                        },
                        enabled = true
                    ) {
                        Text(
                            text = if (selectedHabitIds.isNotEmpty()) {
                                stringResource(id = R.string.multi_select_sort_delete)
                            } else {
                                stringResource(id = R.string.multi_select_sort_save)
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (allHabits.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DragHandle,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(id = R.string.multi_select_sort_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Select All item - sticky header that stays at top while scrolling
                    stickyHeader {
                        SelectAllItem(
                            isSelected = selectedHabitIds.size == draggedOrder.value.size && draggedOrder.value.isNotEmpty(),
                            isIndeterminate = selectedHabitIds.isNotEmpty() && selectedHabitIds.size < draggedOrder.value.size,
                            selectedCount = selectedHabitIds.size,
                            totalCount = draggedOrder.value.size,
                            onClick = { viewModel.toggleSelectAll(draggedOrder.value.map { it.id }) }
                        )
                    }

                    // Habit items - use displayHabits for stable display while reorder state同步中
                    items(
                        items = displayHabits,
                        key = { it.id }
                    ) { habit ->
                        val currentIndex = displayHabits.indexOf(habit)
                        
                        ReorderableItem(
                            state = reorderableState,
                            key = habit.id
                        ) { isDragging ->
                            val elevation by animateDpAsState(if (isDragging) 4.dp else 0.dp)

                            MultiSelectHabitItem(
                                habit = habit,
                                isSelected = selectedHabitIds.contains(habit.id),
                                onToggleSelection = { viewModel.toggleHabitSelection(habit.id) },
                                isDragging = isDragging,
                                elevation = elevation,
                                reorderableItemScope = this,
                                selectedIndex = initiallySelectedIndex,
                                currentIndex = currentIndex,
                                initialAnimationComplete = initialAnimationComplete,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedContentScope = animatedContentScope,
                                isSharedElementTarget = selectedHabitIds.size == 1 && selectedHabitIds.contains(habit.id)
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = stringResource(id = R.string.multi_select_sort_delete_confirm_title),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = stringResource(
                        id = R.string.multi_select_sort_delete_confirm_message,
                        selectedHabitIds.size.toString()
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            clickHandler.processClick {
                                viewModel.deleteSelectedHabits()
                                // 批量删除习惯后刷新记录界面
                                application?.recordsViewModel?.refreshRecords()
                                showDeleteDialog = false
                                // Scroll to top only when deleting
                                viewModel.requestScrollToTop()
                                // Use navigation guard for safe back navigation
                                if (navigationGuard != null) {
                                    navigationGuard.safePopBackStack()
                                } else {
                                    onNavigateBack()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(text = stringResource(id = R.string.multi_select_sort_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text(text = stringResource(id = R.string.multi_select_sort_delete_cancel))
                }
            }
        )
    }
}

/**
 * 全选复选框项目
 */
@Composable
private fun SelectAllItem(
    isSelected: Boolean,
    isIndeterminate: Boolean,
    selectedCount: Int,
    totalCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .combinedClickable(
                    onClick = onClick,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                modifier = Modifier.semantics {
                    contentDescription = if (isSelected) {
                        "取消全选"
                    } else {
                        "全选"
                    }
                }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.multi_select_sort_select_all),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = " ($selectedCount/$totalCount)",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 多选模式下的习惯卡片项目
 */
@Composable
private fun MultiSelectHabitItem(
    habit: Habit,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    isDragging: Boolean,
    elevation: Dp,
    reorderableItemScope: sh.calvin.reorderable.ReorderableCollectionItemScope,
    selectedIndex: Int,
    currentIndex: Int,
    initialAnimationComplete: Boolean,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null,
    isSharedElementTarget: Boolean = false
) {
    val hapticFeedback = LocalHapticFeedback.current
    val dragHandleContentDescription = stringResource(id = R.string.accessibility_drag_to_reorder)
    
    // Calculate animation delay based on distance from selected item
    val distance = if (selectedIndex >= 0) kotlin.math.abs(currentIndex - selectedIndex) else 0
    val animationDelayMs = distance * 30L
    
    // If initial animation phase is complete, skip animation entirely
    // This prevents newly composed items (from scrolling) from animating
    val shouldAnimate = !initialAnimationComplete
    
    // Staggered enter animation state
    var animationTriggered by remember { mutableStateOf(!shouldAnimate || selectedIndex == -1) }
    
    LaunchedEffect(Unit) {
        if (shouldAnimate && !animationTriggered) {
            kotlinx.coroutines.delay(animationDelayMs)
            animationTriggered = true
        }
    }
    
    // Use transition to animate alpha, scale, and translation
    val transition = updateTransition(targetState = animationTriggered, label = "staggeredEnter")
    
    val alpha by transition.animateFloat(
        transitionSpec = { 
            spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow) 
        },
        label = "alpha"
    ) { triggered ->
        if (triggered) 1f else 0f
    }
    
    val scale by transition.animateFloat(
        transitionSpec = { 
            spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow) 
        },
        label = "scale"
    ) { triggered ->
        if (triggered) 1f else 0.93f
    }
    
    val translationY by transition.animateFloat(
        transitionSpec = { 
            spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow) 
        },
        label = "translationY"
    ) { triggered ->
        if (triggered) 0f else 25f // ~25px slide from below (subtle)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha
                this.translationY = translationY
                this.scaleX = scale
                this.scaleY = scale
            }
            .then(
                if (isSharedElementTarget && sharedTransitionScope != null && animatedContentScope != null) {
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
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp)
                .combinedClickable(
                    onClick = {
                        onToggleSelection()
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelection() }
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Habit title
            Text(
                text = habit.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (isSharedElementTarget && sharedTransitionScope != null && animatedContentScope != null) {
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
                    ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Drag handle
            IconButton(
                modifier = with(reorderableItemScope) {
                    Modifier.draggableHandle()
                },
                onClick = { }
            ) {
                Icon(
                    imageVector = Icons.Outlined.DragHandle,
                    contentDescription = stringResource(id = R.string.accessibility_drag_handle),
                    modifier = Modifier.semantics {
                        contentDescription = dragHandleContentDescription
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Preview(showBackground = true, device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
private fun MultiSelectSortScreenPreview() {
    HabitPulseTheme {
        Surface {
            // Sample habits for preview
            val sampleHabits = listOf(
                Habit(
                    id = UUID.randomUUID(),
                    title = "Morning Meditation",
                    completedToday = false,
                    sortOrder = 0
                ),
                Habit(
                    id = UUID.randomUUID(),
                    title = "Exercise",
                    completedToday = true,
                    sortOrder = 1
                ),
                Habit(
                    id = UUID.randomUUID(),
                    title = "Read for 30 minutes",
                    completedToday = false,
                    sortOrder = 2
                ),
                Habit(
                    id = UUID.randomUUID(),
                    title = "Drink 8 glasses of water",
                    completedToday = true,
                    sortOrder = 3
                ),
                Habit(
                    id = UUID.randomUUID(),
                    title = "Practice gratitude journal",
                    completedToday = false,
                    sortOrder = 4
                ),
                Habit(
                    id = UUID.randomUUID(),
                    title = "No social media before noon",
                    completedToday = true,
                    sortOrder = 5
                ),
                Habit(
                    id = UUID.randomUUID(),
                    title = "Sleep by 10 PM",
                    completedToday = false,
                    sortOrder = 6
                )
            )

            // Use a preview-specific selected habit to demonstrate staggered animation
            val selectedHabitIndex = 2 // Third habit selected to show stagger effect
            val selectedHabitId = sampleHabits.getOrNull(selectedHabitIndex)?.id
            val selectedIds = if (selectedHabitId != null) setOf(selectedHabitId) else emptySet()

            // Preview the habit items with animations
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Select All item
                    item {
                        SelectAllItem(
                            isSelected = false,
                            isIndeterminate = true,
                            selectedCount = 1,
                            totalCount = sampleHabits.size,
                            onClick = {}
                        )
                    }

                    // Habit items with staggered animation
                    items(
                        items = sampleHabits,
                        key = { it.id }
                    ) { habit ->
                        val currentIndex = sampleHabits.indexOf(habit)

                        // Preview habit item without reorderable functionality
                        PreviewMultiSelectHabitItem(
                            habit = habit,
                            isSelected = selectedIds.contains(habit.id),
                            onToggleSelection = {},
                            isDragging = false,
                            elevation = 0.dp,
                            selectedIndex = selectedHabitIndex,
                            currentIndex = currentIndex,
                            initialAnimationComplete = false
                        )
                    }
                }
            }
        }
    }
}

/**
 * Preview-only habit item that doesn't require reorderable scope
 */
@Composable
private fun PreviewMultiSelectHabitItem(
    habit: Habit,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    isDragging: Boolean,
    elevation: Dp,
    selectedIndex: Int,
    currentIndex: Int,
    initialAnimationComplete: Boolean
) {
    val hapticFeedback = LocalHapticFeedback.current

    // Calculate animation delay based on distance from selected item
    val distance = if (selectedIndex >= 0) kotlin.math.abs(currentIndex - selectedIndex) else 0
    val animationDelayMs = distance * 30L

    // If initial animation phase is complete, skip animation entirely
    // This prevents newly composed items (from scrolling) from animating
    val shouldAnimate = !initialAnimationComplete

    // Staggered enter animation state
    var animationTriggered by remember { mutableStateOf(!shouldAnimate || selectedIndex == -1) }

    LaunchedEffect(Unit) {
        if (shouldAnimate && !animationTriggered) {
            kotlinx.coroutines.delay(animationDelayMs)
            animationTriggered = true
        }
    }

    // Use transition to animate alpha, scale, and translation
    val transition = updateTransition(targetState = animationTriggered, label = "staggeredEnter")

    val alpha by transition.animateFloat(
        transitionSpec = {
            spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
        },
        label = "alpha"
    ) { triggered ->
        if (triggered) 1f else 0f
    }

    val scale by transition.animateFloat(
        transitionSpec = {
            spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
        },
        label = "scale"
    ) { triggered ->
        if (triggered) 1f else 0.93f
    }

    val translationY by transition.animateFloat(
        transitionSpec = {
            spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
        },
        label = "translationY"
    ) { triggered ->
        if (triggered) 0f else 25f // ~25px slide from below (subtle)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha
                this.translationY = translationY
                this.scaleX = scale
                this.scaleY = scale
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp)
                .combinedClickable(
                    onClick = {
                        onToggleSelection()
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelection() }
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Habit title
            Text(
                text = habit.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Drag handle (non-functional in preview)
            IconButton(
                onClick = { }
            ) {
                Icon(
                    imageVector = Icons.Outlined.DragHandle,
                    contentDescription = "Drag to reorder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
