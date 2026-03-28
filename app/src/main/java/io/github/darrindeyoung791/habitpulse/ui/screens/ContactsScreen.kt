package io.github.darrindeyoung791.habitpulse.ui.screens

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.darrindeyoung791.habitpulse.HabitPulseApplication
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.data.model.Habit
import io.github.darrindeyoung791.habitpulse.data.model.HabitCompletion
import io.github.darrindeyoung791.habitpulse.data.model.RepeatCycle
import io.github.darrindeyoung791.habitpulse.data.model.SupervisionMethod
import io.github.darrindeyoung791.habitpulse.data.repository.HabitRepository
import io.github.darrindeyoung791.habitpulse.ui.theme.HabitPulseTheme
import io.github.darrindeyoung791.habitpulse.viewmodel.ContactsViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.UUID

/**
 * 联系人页面内容组件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreenContent(
    modifier: Modifier = Modifier,
    application: HabitPulseApplication? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    listState: LazyListState = remember { LazyListState() }
) {
    val viewModel: ContactsViewModel = if (application != null) {
        application.contactsViewModel
    } else {
        remember {
            val fakeHabitDao = FakeHabitDaoForContacts()
            val fakeCompletionDao = FakeHabitCompletionDaoForContacts()
            val fakeRepository = HabitRepository(fakeHabitDao, fakeCompletionDao)
            ContactsViewModel(fakeRepository)
        }
    }

    // Collect ViewModel states
    val contacts by viewModel.allContactsFlow.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle(initialValue = true)
    val selectedContact by viewModel.selectedContact.collectAsStateWithLifecycle()
    val showBottomSheet by viewModel.showBottomSheet.collectAsStateWithLifecycle()
    val showDeleteConfirmDialog by viewModel.showDeleteConfirmDialog.collectAsStateWithLifecycle()
    val deleteConfirmContext by viewModel.deleteConfirmContext.collectAsStateWithLifecycle()

    // Get habits for displaying in bottom sheet
    val habits = if (application != null) {
        runBlocking { application.repository.getAllHabits() }
    } else {
        emptyList()
    }

    // Apply nested scroll
    val nestedScrollModifier = scrollBehavior?.let { modifier.nestedScroll(it.nestedScrollConnection) } ?: modifier

    // Reset loading state when entering the screen
    LaunchedEffect(Unit) {
        viewModel.resetLoadingState()
    }

    Column(
        modifier = nestedScrollModifier.fillMaxSize()
    ) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            contacts.isEmpty() -> {
                EmptyContactsContent(
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = contacts,
                        key = { "${it.type}_${it.value}" }
                    ) { contact ->
                        ContactCard(
                            contact = contact,
                            habits = habits.filter { it.id in contact.habitIds },
                            onClick = { viewModel.selectContact(contact) },
                            onDeleteFromAll = {
                                viewModel.showDeleteConfirmDialog(
                                    ContactsViewModel.DeleteConfirmType.FROM_ALL_HABITS,
                                    contact = contact
                                )
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }
    }

    // Bottom Sheet - Show habits using this contact
    if (showBottomSheet && selectedContact != null) {
        val sheetState = rememberModalBottomSheetState()
        val scope = rememberCoroutineScope()

        ModalBottomSheet(
            onDismissRequest = { viewModel.closeBottomSheet() },
            sheetState = sheetState
        ) {
            ContactBottomSheetContent(
                contact = selectedContact!!,
                habits = habits.filter { it.id in selectedContact!!.habitIds },
                onDeleteFromHabit = { habitId ->
                    scope.launch {
                        sheetState.hide()
                        viewModel.showDeleteConfirmDialog(
                            ContactsViewModel.DeleteConfirmType.FROM_HABIT,
                            habitId = habitId,
                            contact = selectedContact
                        )
                    }
                },
                onDeleteFromAll = {
                    scope.launch {
                        sheetState.hide()
                        viewModel.showDeleteConfirmDialog(
                            ContactsViewModel.DeleteConfirmType.FROM_ALL_HABITS,
                            contact = selectedContact
                        )
                    }
                }
            )
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        val isFromAllHabits = deleteConfirmContext?.type == ContactsViewModel.DeleteConfirmType.FROM_ALL_HABITS
        val habitId = deleteConfirmContext?.habitId
        val contactForDeletion = deleteConfirmContext?.contact ?: selectedContact

        AlertDialog(
            onDismissRequest = { viewModel.closeDeleteConfirmDialog() },
            title = {
                Text(text = stringResource(id = R.string.contacts_delete_confirm_title))
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isFromAllHabits) {
                            stringResource(id = R.string.contacts_delete_from_all_habits_confirm)
                        } else {
                            stringResource(id = R.string.contacts_delete_from_habit_confirm)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Show warning if this is the last contact for a habit
                    if (!isFromAllHabits && habitId != null && contactForDeletion != null) {
                        val habit = habits.find { it.id == habitId }
                        val isLastContact = habit?.let { h ->
                            val contactsInHabit = when (contactForDeletion.type) {
                                ContactsViewModel.ContactType.EMAIL ->
                                    h.getSupervisorEmailsList().size
                                ContactsViewModel.ContactType.PHONE ->
                                    h.getSupervisorPhonesList().size
                            }
                            contactsInHabit == 1
                        } == true

                        if (isLastContact) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.contacts_last_supervisor_warning),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (isFromAllHabits) {
                            viewModel.deleteContactFromAllHabits(contactForDeletion)
                        } else if (habitId != null) {
                            viewModel.deleteContactFromHabit(habitId, contactForDeletion)
                        }
                        viewModel.closeDeleteConfirmDialog()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(text = stringResource(id = R.string.contacts_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.closeDeleteConfirmDialog() }
                ) {
                    Text(text = stringResource(id = R.string.contacts_delete_cancel))
                }
            }
        )
    }
}

/**
 * 联系人卡片
 */
@Composable
fun ContactCard(
    contact: ContactsViewModel.ContactInfo,
    habits: List<Habit>,
    onClick: () -> Unit,
    onDeleteFromAll: () -> Unit
) {
    var showDropdown by remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    showDropdown = true
                }
            ),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Contact type icon
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = when (contact.type) {
                    ContactsViewModel.ContactType.EMAIL ->
                        MaterialTheme.colorScheme.primaryContainer
                    ContactsViewModel.ContactType.PHONE ->
                        MaterialTheme.colorScheme.secondaryContainer
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when (contact.type) {
                            ContactsViewModel.ContactType.EMAIL -> Icons.Outlined.Email
                            ContactsViewModel.ContactType.PHONE -> Icons.Outlined.Phone
                        },
                        contentDescription = when (contact.type) {
                            ContactsViewModel.ContactType.EMAIL -> stringResource(id = R.string.contacts_email_type)
                            ContactsViewModel.ContactType.PHONE -> stringResource(id = R.string.contacts_phone_type)
                        },
                        tint = when (contact.type) {
                            ContactsViewModel.ContactType.EMAIL -> MaterialTheme.colorScheme.onPrimaryContainer
                            ContactsViewModel.ContactType.PHONE -> MaterialTheme.colorScheme.onSecondaryContainer
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Contact info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = contact.value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(
                        id = R.string.contacts_used_in_habits,
                        habits.size
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // More options button
            IconButton(
                onClick = { showDropdown = true }
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "More options"
                )
            }

            // Dropdown menu
            DropdownMenu(
                expanded = showDropdown,
                onDismissRequest = { showDropdown = false }
            ) {
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(id = R.string.contacts_delete_from_all_habits),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    onClick = {
                        showDropdown = false
                        onDeleteFromAll()
                    }
                )
            }
        }
    }
}

/**
 * 联系人 Bottom Sheet 内容
 */
@Composable
fun ContactBottomSheetContent(
    contact: ContactsViewModel.ContactInfo,
    habits: List<Habit>,
    onDeleteFromHabit: (UUID) -> Unit,
    onDeleteFromAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(id = R.string.contacts_bottom_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = contact.value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Habits list
        if (habits.isEmpty()) {
            Text(
                text = "该联系人未用于任何习惯",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                habits.forEach { habit ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = habit.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = when (contact.type) {
                                        ContactsViewModel.ContactType.EMAIL ->
                                            stringResource(id = R.string.contacts_email_type)
                                        ContactsViewModel.ContactType.PHONE ->
                                            stringResource(id = R.string.contacts_phone_type)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            TextButton(
                                onClick = { onDeleteFromHabit(habit.id) },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(id = R.string.contacts_delete_from_habit),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Delete from all habits button
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        ) {
            TextButton(
                onClick = onDeleteFromAll,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(id = R.string.contacts_delete_from_all_habits),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

/**
 * 空联系人状态
 */
@Composable
fun EmptyContactsContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Person,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(id = R.string.contacts_no_contacts),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(id = R.string.contacts_no_contacts_description),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

// ============= Fake DAOs for Preview =============

@Suppress("unused")
private class FakeHabitDaoForContacts : io.github.darrindeyoung791.habitpulse.data.database.dao.HabitDao {
    private val habits = listOf(
        Habit(
            id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
            title = "每天喝水",
            repeatCycle = RepeatCycle.DAILY,
            supervisionMethod = SupervisionMethod.EMAIL,
            supervisorEmails = """["supervisor@example.com","manager@example.com"]""",
            completionCount = 15,
            createdDate = System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L
        ),
        Habit(
            id = UUID.fromString("00000000-0000-0000-0000-000000000002"),
            title = "晨跑锻炼",
            repeatCycle = RepeatCycle.WEEKLY,
            supervisionMethod = SupervisionMethod.SMS,
            supervisorPhones = """["+8613800138000","+8613900139000"]""",
            completionCount = 8,
            createdDate = System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L
        ),
        Habit(
            id = UUID.fromString("00000000-0000-0000-0000-000000000003"),
            title = "阅读书籍",
            repeatCycle = RepeatCycle.DAILY,
            supervisionMethod = SupervisionMethod.EMAIL,
            supervisorEmails = """["supervisor@example.com"]""",
            completionCount = 20,
            createdDate = System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L
        )
    )

    override fun getAllHabitsFlow(): kotlinx.coroutines.flow.Flow<List<Habit>> =
        kotlinx.coroutines.flow.flowOf(habits)

    override suspend fun getAllHabits(): List<Habit> = habits
    override fun getHabitByIdFlow(id: UUID): kotlinx.coroutines.flow.Flow<Habit?> =
        kotlinx.coroutines.flow.flowOf(habits.find { it.id == id })
    override suspend fun getHabitById(id: UUID): Habit? = habits.find { it.id == id }
    override fun getIncompleteHabitsFlow(): kotlinx.coroutines.flow.Flow<List<Habit>> =
        kotlinx.coroutines.flow.flowOf(emptyList())
    override fun getCompletedHabitsFlow(): kotlinx.coroutines.flow.Flow<List<Habit>> =
        kotlinx.coroutines.flow.flowOf(habits)
    override suspend fun insert(habit: Habit): Long = 0
    override suspend fun update(habit: Habit) {}
    override suspend fun delete(habit: Habit) {}
    override suspend fun deleteAll() {}
    override suspend fun updateCompletionStatus(id: UUID, completed: Boolean, timestamp: Long) {}
    override suspend fun undoCompletionStatus(id: UUID, timestamp: Long) {}
    override suspend fun incrementCompletionCount(id: UUID, timestamp: Long) {}
    override suspend fun resetAllCompletionStatus(timestamp: Long) {}
    override fun getHabitCount(): kotlinx.coroutines.flow.Flow<Int> =
        kotlinx.coroutines.flow.flowOf(habits.size)

    override fun searchHabitsFlow(query: String): kotlinx.coroutines.flow.Flow<List<Habit>> {
        val searchQuery = query.trim('%')
        return kotlinx.coroutines.flow.flowOf(
            habits.filter { habit ->
                habit.title.contains(searchQuery, ignoreCase = true)
            }
        )
    }
}

@Suppress("unused")
private class FakeHabitCompletionDaoForContacts : io.github.darrindeyoung791.habitpulse.data.database.dao.HabitCompletionDao {
    override fun getCompletionsByHabitIdFlow(habitId: UUID): kotlinx.coroutines.flow.Flow<List<io.github.darrindeyoung791.habitpulse.data.model.HabitCompletion>> =
        kotlinx.coroutines.flow.flowOf(emptyList())

    override suspend fun getCompletionsByHabitId(habitId: UUID): List<io.github.darrindeyoung791.habitpulse.data.model.HabitCompletion> = emptyList()

    override suspend fun getCompletionsByHabitIdAndDate(
        habitId: UUID,
        date: String
    ): List<io.github.darrindeyoung791.habitpulse.data.model.HabitCompletion> = emptyList()

    override suspend fun getCompletionsByHabitIdAndDateRange(
        habitId: UUID,
        startDate: String,
        endDate: String
    ): List<io.github.darrindeyoung791.habitpulse.data.model.HabitCompletion> = emptyList()

    override suspend fun getCompletionsByDate(date: String): List<io.github.darrindeyoung791.habitpulse.data.model.HabitCompletion> = emptyList()

    override suspend fun getTodayCompletionCount(habitId: UUID, date: String): Int = 0

    override suspend fun insert(completion: io.github.darrindeyoung791.habitpulse.data.model.HabitCompletion): Long = 0

    override suspend fun insertAll(completions: List<io.github.darrindeyoung791.habitpulse.data.model.HabitCompletion>) {}

    override suspend fun delete(completion: io.github.darrindeyoung791.habitpulse.data.model.HabitCompletion) {}

    override suspend fun deleteByHabitId(habitId: UUID) {}

    override suspend fun deleteByDate(date: String) {}

    override suspend fun deleteAll() {}

    override fun getCompletionCount(): kotlinx.coroutines.flow.Flow<Int> =
        kotlinx.coroutines.flow.flowOf(0)

    override suspend fun getCompletionCountByHabitId(habitId: UUID): Int = 0
}

// ============= Previews =============

@Preview(showBackground = true)
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ContactsScreenContentPreview() {
    HabitPulseTheme {
        ContactsScreenContent()
    }
}

@Preview(showBackground = true)
@Composable
fun ContactCardPreview() {
    HabitPulseTheme {
        ContactCard(
            contact = ContactsViewModel.ContactInfo(
                type = ContactsViewModel.ContactType.EMAIL,
                value = "supervisor@example.com",
                habitIds = listOf(UUID.randomUUID())
            ),
            habits = listOf(
                Habit(
                    id = UUID.randomUUID(),
                    title = "每天喝水",
                    repeatCycle = RepeatCycle.DAILY
                )
            ),
            onClick = {},
            onDeleteFromAll = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EmptyContactsContentPreview() {
    HabitPulseTheme {
        EmptyContactsContent()
    }
}
