package io.github.darrindeyoung791.habitpulse.ui.screens

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.darrindeyoung791.habitpulse.HabitPulseApplication
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.data.model.Habit
import io.github.darrindeyoung791.habitpulse.data.model.HabitStatus
import io.github.darrindeyoung791.habitpulse.data.model.HabitWithStatus
import io.github.darrindeyoung791.habitpulse.data.model.RepeatCycle
import io.github.darrindeyoung791.habitpulse.data.repository.HabitRepository
import io.github.darrindeyoung791.habitpulse.utils.OnboardingPreferences
import io.github.darrindeyoung791.habitpulse.viewmodel.HabitViewModel
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import androidx.navigation.NavHostController
import io.github.darrindeyoung791.habitpulse.ui.utils.rememberAnimationsFrozen
import io.github.darrindeyoung791.habitpulse.ui.utils.rememberDebounceClickHandler
import io.github.darrindeyoung791.habitpulse.ui.utils.rememberHideKeyboardAndNavigateBack
import io.github.darrindeyoung791.habitpulse.ui.utils.StaggeredListItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayHabitsScreen(
    filter: String = "today",
    onNavigateBack: () -> Unit,
    navController: NavHostController,
    onEditHabit: (Habit) -> Unit,
    application: HabitPulseApplication? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current

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

    val allWithStatus by viewModel.habitsWithStatusForDisplay.collectAsStateWithLifecycle()
    val rewardSheetHabit by viewModel.rewardSheetHabit.collectAsStateWithLifecycle(initialValue = null)
    val showRewardSheet by viewModel.showRewardSheet.collectAsStateWithLifecycle(initialValue = false)

    val todayDayOfWeek = LocalDate.now().dayOfWeek
    val now = LocalTime.now()

    val todayHabitsWithStatus = remember(allWithStatus, todayDayOfWeek) {
        allWithStatus.filter { ws ->
            when (ws.habit.repeatCycle) {
                RepeatCycle.DAILY -> true
                RepeatCycle.WEEKLY -> {
                    val dayIndex = when (todayDayOfWeek) {
                        DayOfWeek.MONDAY -> 0
                        DayOfWeek.TUESDAY -> 1
                        DayOfWeek.WEDNESDAY -> 2
                        DayOfWeek.THURSDAY -> 3
                        DayOfWeek.FRIDAY -> 4
                        DayOfWeek.SATURDAY -> 5
                        DayOfWeek.SUNDAY -> 6
                    }
                    dayIndex in ws.habit.getRepeatDaysList()
                }
            }
        }
    }

    val filteredHabits = remember(todayHabitsWithStatus, filter) {
        when (filter) {
            "about_to_start" -> todayHabitsWithStatus.filter { it.status.contains(HabitStatus.ABOUT_TO_START) }
            "overdue" -> todayHabitsWithStatus.filter { it.isCompletelyOverdue }
            else -> todayHabitsWithStatus
        }
    }

    val flatSortedList = remember(filteredHabits, now) {
        filteredHabits.sortedBy { ws ->
            val times = ws.habit.getReminderTimesList()
            if (times.isEmpty()) "99:99" else times.min()
        }.sortedBy { ws ->
            val times = ws.habit.getReminderTimesList()
            if (times.isEmpty()) 2 else {
                val parts = times.min().split(":")
                val reminderTime = LocalTime.of(parts[0].toInt(), parts[1].toInt())
                if (reminderTime <= now || Duration.between(now, reminderTime).toMinutes() < 60) 0 else 1
            }
        }
    }

    val listState = remember { LazyListState() }
    val scope = rememberCoroutineScope()
    val clickHandler = rememberDebounceClickHandler()
    val hideKeyboardAndNavigateBack = rememberHideKeyboardAndNavigateBack(navController)

    val animationsFrozen by rememberAnimationsFrozen(listState)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val titleRes = when (filter) {
                        "about_to_start" -> R.string.entry_zone_about_to_start
                        "overdue" -> R.string.entry_zone_overdue
                        else -> R.string.entry_zone_today_habits
                    }
                    Text(
                        text = stringResource(id = titleRes),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { clickHandler.processClick { hideKeyboardAndNavigateBack() } } }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.settings_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (flatSortedList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.main_no_habits),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                ScrollableLazyColumnWithScrollbar(
                    modifier = Modifier.fillMaxSize(),
                    listState = listState,
                    contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = flatSortedList,
                        key = { _, item -> item.habit.id.toString() }
                    ) { index, ws ->
                        StaggeredListItem(
                            index = index,
                            animationsFrozen = animationsFrozen
                        ) {
                            HabitCard(
                                habitWithStatus = ws,
                                onClick = { onEditHabit(ws.habit) },
                                onCheckIn = {
                                    viewModel.performSlotCheckIn(ws.habit)
                                },
                                onUndoCompletion = { viewModel.undoHabitCompletion(ws.habit) },
                                onEditHabit = { onEditHabit(ws.habit) },
                                onDeleteHabit = {
                                    viewModel.deleteHabit(ws.habit)
                                    application?.recordsViewModel?.refreshRecords()
                                },
                                onNavigateToMultiSelect = {},
                                modifier = Modifier.fillMaxWidth(),
                                showMultiSelectMenuItem = false
                            )
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
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

    val checkInFeedbackType by viewModel.checkInFeedbackType.collectAsStateWithLifecycle()
    val tooEarlyEarliestSlot by viewModel.tooEarlyEarliestSlot.collectAsStateWithLifecycle()
    if (checkInFeedbackType != HabitViewModel.CheckInFeedbackType.NONE) {
        CheckInFeedbackSheet(
            type = checkInFeedbackType,
            earliestSlot = tooEarlyEarliestSlot,
            onDismiss = { viewModel.dismissCheckInFeedback() }
        )
    }
}
