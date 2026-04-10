package io.github.darrindeyoung791.habitpulse.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
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
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.runtime.LaunchedEffect
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
    listState: LazyListState = remember { LazyListState() },
    // Search parameters
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    isSearchActive: Boolean = false,
    onSearchActiveChange: (Boolean) -> Unit = {}
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
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle(initialValue = false)
    val hasLoadedDataOnce by viewModel.hasLoadedDataOnce.collectAsStateWithLifecycle(initialValue = false)
    val lastNonEmptyData by viewModel.lastNonEmptyData.collectAsStateWithLifecycle(initialValue = emptyList())
    val selectedContact by viewModel.selectedContact.collectAsStateWithLifecycle()

    // 使用最后一次的非空数据，避免切换页面时闪现空状态
    // 但只在加载中时使用缓存，加载完成后显示真实数据（包括空状态）
    val displayContacts = if (contacts.isNotEmpty()) {
        contacts
    } else if (isLoading) {
        // 加载中：使用缓存避免闪现空状态
        lastNonEmptyData
    } else {
        // 加载完成：显示真实数据（可能是空列表）
        contacts
    }
    
    // 使用过滤后的联系人列表（如果搜索激活）
    // 当搜索词为空时，直接使用 displayContacts，避免等待 Flow 收集导致闪现空列表
    val filteredContactsFlowValue by viewModel.filteredContactsFlow.collectAsStateWithLifecycle(initialValue = displayContacts)
    val filteredContacts = if (isSearchActive && searchQuery.isNotEmpty()) {
        filteredContactsFlowValue
    } else if (isSearchActive) {
        // 搜索激活但搜索词为空，显示所有联系人
        displayContacts
    } else {
        displayContacts
    }
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

    // Get screen configuration for two-column layout
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    // Use two-column layout for tablets in landscape (≥840dp), same as HomeScreen
    val useTwoColumnLayout = isLandscape && screenWidthDp >= 840

    Column(
        modifier = nestedScrollModifier.fillMaxSize()
    ) {
        // 搜索框 - 使用 AnimatedVisibility 带滑入/滑出动画
        // 搜索框的高度变化会自动推动下方内容
        val searchFocusRequester = remember { FocusRequester() }
        
        // 请求焦点到搜索框
        LaunchedEffect(isSearchActive) {
            if (isSearchActive) {
                searchFocusRequester.requestFocus()
            }
        }
        
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
                onSearchQueryChange = onSearchQueryChange,
                onClearSearch = { onSearchQueryChange("") },
                onBackClick = { onSearchActiveChange(false) },
                placeholder = stringResource(id = R.string.search_contacts_hint),
                accessibilityLabel = stringResource(id = R.string.accessibility_search_contacts),
                focusRequester = searchFocusRequester,
                isFocused = true,
                onFocusedChange = { },
                isSearchActive = isSearchActive
            )
        }

        when {
            // 只在首次加载时显示加载指示器，切换页面时不显示
            isLoading && !hasLoadedDataOnce -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            // 搜索时显示搜索结果（包括空结果），不搜索时显示正常状态
            isSearchActive -> {
                if (filteredContacts.isEmpty()) {
                    // 搜索但无结果
                    EmptyContactsContent(
                        modifier = Modifier.fillMaxSize(),
                        title = stringResource(id = R.string.contacts_no_search_results),
                        description = stringResource(id = R.string.contacts_no_search_results_description)
                    )
                } else if (useTwoColumnLayout) {
                    // Two-column layout for tablet landscape
                    ScrollableLazyColumnWithScrollbar(
                        modifier = Modifier.fillMaxSize(),
                        listState = listState,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val contactRows = filteredContacts.chunked(2)

                        items(
                            items = contactRows,
                            key = { row ->
                                "${row.getOrNull(0)?.type}_${row.getOrNull(0)?.value ?: ""}-${row.getOrNull(1)?.type}_${row.getOrNull(1)?.value ?: ""}"
                            }
                        ) { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                row.getOrNull(0)?.let { contact ->
                                    ContactCard(
                                        contact = contact,
                                        habits = habits.filter { it.id in contact.habitIds },
                                        onClick = { viewModel.selectContact(contact) },
                                        onDeleteFromAll = {
                                            viewModel.showDeleteConfirmDialog(
                                                ContactsViewModel.DeleteConfirmType.FROM_ALL_HABITS,
                                                contact = contact
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                } ?: Spacer(modifier = Modifier.weight(1f))

                                row.getOrNull(1)?.let { contact ->
                                    ContactCard(
                                        contact = contact,
                                        habits = habits.filter { it.id in contact.habitIds },
                                        onClick = { viewModel.selectContact(contact) },
                                        onDeleteFromAll = {
                                            viewModel.showDeleteConfirmDialog(
                                                ContactsViewModel.DeleteConfirmType.FROM_ALL_HABITS,
                                                contact = contact
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                } ?: Spacer(modifier = Modifier.weight(1f))
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(100.dp))
                        }
                    }
                } else {
                    // 显示搜索结果 - single column
                    ScrollableLazyColumnWithScrollbar(
                        modifier = Modifier.fillMaxSize(),
                        listState = listState,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = filteredContacts,
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
            // 非搜索状态
            filteredContacts.isEmpty() -> {
                // 所有联系人为空
                EmptyContactsContent(
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                if (useTwoColumnLayout) {
                    // Two-column layout for tablet landscape
                    ScrollableLazyColumnWithScrollbar(
                        modifier = Modifier.fillMaxSize(),
                        listState = listState,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val contactRows = filteredContacts.chunked(2)

                        items(
                            items = contactRows,
                            key = { row ->
                                "${row.getOrNull(0)?.type}_${row.getOrNull(0)?.value ?: ""}-${row.getOrNull(1)?.type}_${row.getOrNull(1)?.value ?: ""}"
                            }
                        ) { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                row.getOrNull(0)?.let { contact ->
                                    ContactCard(
                                        contact = contact,
                                        habits = habits.filter { it.id in contact.habitIds },
                                        onClick = { viewModel.selectContact(contact) },
                                        onDeleteFromAll = {
                                            viewModel.showDeleteConfirmDialog(
                                                ContactsViewModel.DeleteConfirmType.FROM_ALL_HABITS,
                                                contact = contact
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                } ?: Spacer(modifier = Modifier.weight(1f))

                                row.getOrNull(1)?.let { contact ->
                                    ContactCard(
                                        contact = contact,
                                        habits = habits.filter { it.id in contact.habitIds },
                                        onClick = { viewModel.selectContact(contact) },
                                        onDeleteFromAll = {
                                            viewModel.showDeleteConfirmDialog(
                                                ContactsViewModel.DeleteConfirmType.FROM_ALL_HABITS,
                                                contact = contact
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                } ?: Spacer(modifier = Modifier.weight(1f))
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(100.dp))
                        }
                    }
                } else {
                    // Single column layout for phones and portrait mode
                    ScrollableLazyColumnWithScrollbar(
                        modifier = Modifier.fillMaxSize(),
                        listState = listState,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = filteredContacts,
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
    }

    // Bottom Sheet - Show habits using this contact
    if (showBottomSheet && selectedContact != null) {
        val sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )
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
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(id = R.string.contacts_last_supervisor_warning),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.fillMaxWidth()
                            )
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
    onDeleteFromAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDropdown by remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    showDropdown = true
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
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
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
            .padding(horizontal = 16.dp)
            .windowInsetsPadding(WindowInsets.navigationBars),
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
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Habits list - scrollable when there are many habits
        if (habits.isEmpty()) {
            Text(
                text = stringResource(id = R.string.contacts_bottom_sheet_no_habits),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(habits.size) { index ->
                    val habit = habits[index]
                    val hapticFeedback = LocalHapticFeedback.current
                    var showDropdown by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onDeleteFromHabit(habit.id) },
                                onLongClick = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showDropdown = true
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
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = habit.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
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

                            IconButton(
                                onClick = { onDeleteFromHabit(habit.id) }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = stringResource(id = R.string.contacts_delete_from_habit),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }

        // Delete from all habits button - Keep as Surface (not outlined)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.error
        ) {
            TextButton(
                onClick = onDeleteFromAll,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onError
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(id = R.string.contacts_delete_from_all_habits),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

/**
 * 空联系人状态
 */
@Composable
fun EmptyContactsContent(
    modifier: Modifier = Modifier,
    title: String = stringResource(id = R.string.contacts_no_contacts),
    description: String = stringResource(id = R.string.contacts_no_contacts_description)
) {
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
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

// ============= Fake DAOs for Preview =============

@Suppress("unused")
private class FakeHabitDaoForContacts : io.github.darrindeyoung791.habitpulse.data.database.dao.HabitDao {
    private val habits = mutableListOf(
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

    override fun getHabitsBySortOrderFlow(): kotlinx.coroutines.flow.Flow<List<Habit>> {
        return kotlinx.coroutines.flow.flowOf(habits.sortedBy { it.sortOrder })
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

@Suppress("unused")
private class FakeHabitCompletionDaoForContacts : io.github.darrindeyoung791.habitpulse.data.database.dao.HabitCompletionDao {
    override fun getCompletionsByHabitIdFlow(habitId: UUID): kotlinx.coroutines.flow.Flow<List<io.github.darrindeyoung791.habitpulse.data.model.HabitCompletion>> =
        kotlinx.coroutines.flow.flowOf(emptyList())

    override fun getAllCompletionsFlow(): kotlinx.coroutines.flow.Flow<List<io.github.darrindeyoung791.habitpulse.data.model.HabitCompletion>> =
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
