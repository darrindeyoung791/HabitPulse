package io.github.darrindeyoung791.habitpulse.ui.screens.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.LocalIndication
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.data.model.Habit
import io.github.darrindeyoung791.habitpulse.data.model.HabitWithStatus
import io.github.darrindeyoung791.habitpulse.data.search.SettingsIcon
import io.github.darrindeyoung791.habitpulse.data.search.SettingsSearchEntry
import io.github.darrindeyoung791.habitpulse.ui.screens.ContactCard
import io.github.darrindeyoung791.habitpulse.ui.screens.HabitCard
import io.github.darrindeyoung791.habitpulse.ui.screens.emptyStateOmniboxClearance
import io.github.darrindeyoung791.habitpulse.ui.screens.highlightText
import io.github.darrindeyoung791.habitpulse.ui.utils.PressVibrationFeedback
import io.github.darrindeyoung791.habitpulse.viewmodel.ContactsViewModel
import java.util.UUID

/**
 * Omnibox 统一搜索结果的类型筛选。
 *
 * 习惯 / 联系人两个 Section 输入查询词时展示统一分组结果页，
 * 顶部切换控件在四个取值间筛选要显示的结果类别；
 * 记录 Section 不参与统一搜索（保持本地记录过滤）。
 */
enum class SearchResultType { ALL, HABITS, CONTACTS, SETTINGS }

/**
 * 统一搜索结果内容（习惯 + 联系人 + 系统设置分组展示）。
 *
 * 布局：顶部 pinned 的类型切换控件（MD3 FilterChip 行）+ 下方分组 LazyColumn。
 * 空态时切换控件仍然保留，便于切到其他有结果的类型。
 *
 * - 「全部」模式下按组显示组标题；切到具体类型时隐藏组标题。
 * - 习惯/联系人保持富卡片渲染（习惯可打卡、联系人可弹详情），
 *   设置条目为简化行（图标 + 标题 + 副标题 + 箭头）。
 * - 宽屏（isWideLayout）下习惯/联系人卡片沿用现有 chunked(2) 双列。
 */
@Composable
fun UnifiedSearchResultsContent(
    query: String,
    selectedType: SearchResultType,
    onTypeChange: (SearchResultType) -> Unit,
    habits: List<HabitWithStatus>,
    contacts: List<ContactsViewModel.ContactInfo>,
    settingsEntries: List<SettingsSearchEntry>,
    allHabits: List<Habit>,
    listState: LazyListState,
    onHabitClick: (Habit) -> Unit,
    onCheckIn: (Habit) -> Unit,
    onUndoCompletion: (Habit) -> Unit,
    onDeleteHabit: (Habit) -> Unit,
    onNavigateToMultiSelect: (habitId: UUID) -> Unit,
    onContactClick: (ContactsViewModel.ContactInfo) -> Unit,
    onContactDeleteFromAll: (ContactsViewModel.ContactInfo) -> Unit,
    onSettingsClick: (SettingsSearchEntry) -> Unit,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier,
    isWideLayout: Boolean = false,
    nestedScrollConnection: NestedScrollConnection? = null
) {
    val visibleHabits = if (selectedType == SearchResultType.ALL || selectedType == SearchResultType.HABITS) habits else emptyList()
    val visibleContacts = if (selectedType == SearchResultType.ALL || selectedType == SearchResultType.CONTACTS) contacts else emptyList()
    val visibleSettings = if (selectedType == SearchResultType.ALL || selectedType == SearchResultType.SETTINGS) settingsEntries else emptyList()
    val isEmpty = visibleHabits.isEmpty() && visibleContacts.isEmpty() && visibleSettings.isEmpty()
    // 组标题只在「全部」模式下显示；具体类型模式下切换控件已表明上下文
    val showGroupHeaders = selectedType == SearchResultType.ALL

    val rootModifier = if (nestedScrollConnection != null) {
        modifier.nestedScroll(nestedScrollConnection)
    } else {
        modifier
    }

    Column(modifier = rootModifier.fillMaxSize()) {
        SearchResultTypeSwitcher(
            selectedType = selectedType,
            onTypeChange = onTypeChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 4.dp)
        )

        if (isEmpty) {
            UnifiedSearchEmptyState(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                onClearSearch = onClearSearch
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ============ 习惯组（富卡片，保留打卡交互） ============
                if (visibleHabits.isNotEmpty()) {
                    if (showGroupHeaders) {
                        item(key = "header_habits", contentType = "header") {
                            GroupHeader(text = stringResource(id = R.string.main_tab_habits))
                        }
                    }
                    if (isWideLayout) {
                        visibleHabits.chunked(2).forEach { row ->
                            item(key = "habit_${row.first().habit.id}", contentType = "habit") {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row.forEach { ws ->
                                        HabitResultCard(
                                            ws = ws,
                                            query = query,
                                            onHabitClick = onHabitClick,
                                            onCheckIn = onCheckIn,
                                            onUndoCompletion = onUndoCompletion,
                                            onDeleteHabit = onDeleteHabit,
                                            onNavigateToMultiSelect = onNavigateToMultiSelect,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (row.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    } else {
                        visibleHabits.forEach { ws ->
                            item(key = "habit_${ws.habit.id}", contentType = "habit") {
                                HabitResultCard(
                                    ws = ws,
                                    query = query,
                                    onHabitClick = onHabitClick,
                                    onCheckIn = onCheckIn,
                                    onUndoCompletion = onUndoCompletion,
                                    onDeleteHabit = onDeleteHabit,
                                    onNavigateToMultiSelect = onNavigateToMultiSelect,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // ============ 联系人组（富卡片，点击弹详情） ============
                if (visibleContacts.isNotEmpty()) {
                    if (showGroupHeaders) {
                        item(key = "header_contacts", contentType = "header") {
                            GroupHeader(text = stringResource(id = R.string.main_tab_contacts))
                        }
                    }
                    if (isWideLayout) {
                        visibleContacts.chunked(2).forEach { row ->
                            item(key = "contact_${row.first().type}_${row.first().value}", contentType = "contact") {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row.forEach { contact ->
                                        ContactResultCard(
                                            contact = contact,
                                            allHabits = allHabits,
                                            onContactClick = onContactClick,
                                            onContactDeleteFromAll = onContactDeleteFromAll,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (row.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    } else {
                        visibleContacts.forEach { contact ->
                            item(key = "contact_${contact.type}_${contact.value}", contentType = "contact") {
                                ContactResultCard(
                                    contact = contact,
                                    allHabits = allHabits,
                                    onContactClick = onContactClick,
                                    onContactDeleteFromAll = onContactDeleteFromAll,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // ============ 设置组（简化行，点击深链直达） ============
                if (visibleSettings.isNotEmpty()) {
                    if (showGroupHeaders) {
                        item(key = "header_settings", contentType = "header") {
                            GroupHeader(text = stringResource(id = R.string.settings_title))
                        }
                    }
                    items(
                        count = visibleSettings.size,
                        key = { index -> "settings_${visibleSettings[index].title}" },
                        contentType = { "settings" }
                    ) { index ->
                        SettingsResultRow(
                            entry = visibleSettings[index],
                            query = query,
                            onClick = { onSettingsClick(visibleSettings[index]) }
                        )
                    }
                }

                item(key = "search_bottom_spacer", contentType = "spacer") {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

/** 结果页里的习惯卡片（接线与 HabitListContent 一致）。 */
@Composable
private fun HabitResultCard(
    ws: HabitWithStatus,
    query: String,
    onHabitClick: (Habit) -> Unit,
    onCheckIn: (Habit) -> Unit,
    onUndoCompletion: (Habit) -> Unit,
    onDeleteHabit: (Habit) -> Unit,
    onNavigateToMultiSelect: (habitId: UUID) -> Unit,
    modifier: Modifier = Modifier
) {
    HabitCard(
        habitWithStatus = ws,
        onClick = { onHabitClick(ws.habit) },
        onCheckIn = { onCheckIn(ws.habit) },
        onUndoCompletion = { onUndoCompletion(ws.habit) },
        onEditHabit = { onHabitClick(ws.habit) },
        onDeleteHabit = { onDeleteHabit(ws.habit) },
        onNavigateToMultiSelect = onNavigateToMultiSelect,
        searchQuery = query,
        modifier = modifier
    )
}

/** 结果页里的联系人卡片（接线与 ContactsScreenContent 一致）。 */
@Composable
private fun ContactResultCard(
    contact: ContactsViewModel.ContactInfo,
    allHabits: List<Habit>,
    onContactClick: (ContactsViewModel.ContactInfo) -> Unit,
    onContactDeleteFromAll: (ContactsViewModel.ContactInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    ContactCard(
        contact = contact,
        habits = allHabits.filter { it.id in contact.habitIds },
        onClick = { onContactClick(contact) },
        onDeleteFromAll = { onContactDeleteFromAll(contact) },
        modifier = modifier
    )
}

/**
 * 结果类型切换控件（MD3 FilterChip 行，顶部 pinned 不随列表滚动）。
 * 横向可滚动，长标签 locale（如 en 的 Contacts/Settings）不会被裁剪。
 */
@Composable
private fun SearchResultTypeSwitcher(
    selectedType: SearchResultType,
    onTypeChange: (SearchResultType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SearchResultType.entries.forEach { type ->
            val interactionSource = remember { MutableInteractionSource() }
            PressVibrationFeedback(interactionSource = interactionSource)
            FilterChip(
                selected = selectedType == type,
                onClick = { onTypeChange(type) },
                label = {
                    Text(
                        text = stringResource(id = type.labelRes()),
                        maxLines = 1,
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                interactionSource = interactionSource
            )
        }
    }
}

private fun SearchResultType.labelRes(): Int = when (this) {
    SearchResultType.ALL -> R.string.search_type_all
    SearchResultType.HABITS -> R.string.main_tab_habits
    SearchResultType.CONTACTS -> R.string.main_tab_contacts
    SearchResultType.SETTINGS -> R.string.settings_title
}

/** 分组标题（「全部」模式下区分习惯/联系人/设置三组）。 */
@Composable
private fun GroupHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

/** 设置结果简化行：图标 + 标题（关键词高亮）+ 副标题 + 右箭头。 */
@Composable
private fun SettingsResultRow(
    entry: SettingsSearchEntry,
    query: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    PressVibrationFeedback(interactionSource = interactionSource)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current
            ) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = highlightText(entry.title, query),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            supportingContent = entry.supportingText?.let { supporting ->
                {
                    Text(
                        text = supporting,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            leadingContent = {
                Icon(
                    imageVector = entry.icon.toImageVector(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingContent = {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
    }
}

/** 统一搜索空态：与各 Section 搜索空态一致，但在顶部保留类型切换控件。 */
@Composable
private fun UnifiedSearchEmptyState(
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            // 在 AppBar 与 Omnibox 之间整体垂直居中
            .emptyStateOmniboxClearance()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(id = R.string.search_unified_no_results),
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

/** 图标键 → Material 图标映射（索引数据保持纯 Kotlin，映射放在 UI 层）。 */
private fun SettingsIcon.toImageVector(): ImageVector = when (this) {
    SettingsIcon.AI -> Icons.Outlined.AutoAwesome
    SettingsIcon.LAN -> Icons.Outlined.Lan
    SettingsIcon.NOTIFICATIONS -> Icons.Outlined.Notifications
    SettingsIcon.GENERAL -> Icons.Outlined.Settings
    SettingsIcon.ABOUT -> Icons.Outlined.Info
    SettingsIcon.HELP -> Icons.AutoMirrored.Outlined.HelpOutline
    SettingsIcon.LANGUAGE -> Icons.Outlined.Translate
    SettingsIcon.FONT_SCALE -> Icons.Outlined.FormatSize
    SettingsIcon.TABLET -> Icons.Outlined.Tablet
    SettingsIcon.VIBRATION -> Icons.Outlined.Vibration
    SettingsIcon.CLEAN -> Icons.Outlined.DeleteSweep
    SettingsIcon.ALARM -> Icons.Outlined.Alarm
    SettingsIcon.BEDTIME -> Icons.Outlined.Bedtime
    SettingsIcon.VERSION -> Icons.Outlined.Info
    SettingsIcon.PERSON -> Icons.Outlined.Person
    SettingsIcon.DOCUMENT -> Icons.AutoMirrored.Outlined.Article
}
