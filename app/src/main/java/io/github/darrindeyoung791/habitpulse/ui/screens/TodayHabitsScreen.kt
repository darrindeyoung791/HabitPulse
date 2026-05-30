package io.github.darrindeyoung791.habitpulse.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
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
import io.github.darrindeyoung791.habitpulse.data.model.RepeatCycle
import io.github.darrindeyoung791.habitpulse.data.repository.HabitRepository
import io.github.darrindeyoung791.habitpulse.utils.OnboardingPreferences
import io.github.darrindeyoung791.habitpulse.viewmodel.HabitViewModel
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import androidx.navigation.NavHostController
import io.github.darrindeyoung791.habitpulse.ui.utils.rememberDebounceClickHandler
import io.github.darrindeyoung791.habitpulse.ui.utils.rememberHideKeyboardAndNavigateBack
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayHabitsScreen(
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

    val allHabits by viewModel.habitsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val rewardSheetHabit by viewModel.rewardSheetHabit.collectAsStateWithLifecycle(initialValue = null)
    val showRewardSheet by viewModel.showRewardSheet.collectAsStateWithLifecycle(initialValue = false)

    val todayDayOfWeek = LocalDate.now().dayOfWeek
    val now = LocalTime.now()

    val todayHabits = remember(allHabits, todayDayOfWeek) {
        allHabits.filter { habit ->
            when (habit.repeatCycle) {
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
                    dayIndex in habit.getRepeatDaysList()
                }
            }
        }
    }

    val sections = remember(todayHabits, now) {
        val soonTitle = context.getString(R.string.today_section_soon)
        val laterTitle = context.getString(R.string.today_section_later)
        val alldayTitle = context.getString(R.string.today_section_allday)

        val sorted = todayHabits.sortedBy { habit ->
            val times = habit.getReminderTimesList()
            if (times.isEmpty()) "99:99" else times.min()
        }

        val soonList = mutableListOf<Habit>()
        val laterList = mutableListOf<Habit>()
        val alldayList = mutableListOf<Habit>()

        for (habit in sorted) {
            val times = habit.getReminderTimesList()
            if (times.isEmpty()) {
                alldayList.add(habit)
            } else {
                val parts = times.min().split(":")
                val reminderTime = LocalTime.of(parts[0].toInt(), parts[1].toInt())
                if (reminderTime <= now || Duration.between(now, reminderTime).toMinutes() < 60) {
                    soonList.add(habit)
                } else {
                    laterList.add(habit)
                }
            }
        }

        buildList {
            if (soonList.isNotEmpty()) add(soonTitle to soonList)
            if (laterList.isNotEmpty()) add(laterTitle to laterList)
            if (alldayList.isNotEmpty()) add(alldayTitle to alldayList)
        }
    }

    val listState = remember { LazyListState() }
    val scope = rememberCoroutineScope()
    val clickHandler = rememberDebounceClickHandler()
    val hideKeyboardAndNavigateBack = rememberHideKeyboardAndNavigateBack(navController)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.entry_zone_today_habits),
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
            if (sections.isEmpty()) {
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
                    sections.forEach { (title, habitList) ->
                        item(key = "header_$title") {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(
                            items = habitList,
                            key = { it.id.toString() }
                        ) { habit ->
                            HabitCard(
                                habit = habit,
                                onClick = { onEditHabit(habit) },
                                onCheckIn = {
                                    viewModel.incrementCompletionCount(habit)
                                    viewModel.showRewardSheet(habit)
                                },
                                onUndoCompletion = { viewModel.undoHabitCompletion(habit) },
                                onEditHabit = { onEditHabit(habit) },
                                onDeleteHabit = {
                                    viewModel.deleteHabit(habit)
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
}
