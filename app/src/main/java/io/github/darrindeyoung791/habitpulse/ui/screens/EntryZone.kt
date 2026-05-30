package io.github.darrindeyoung791.habitpulse.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

data class EntryItem(
    val id: String,
    val icon: ImageVector,
    val title: String,
    val badgeText: String?,
    val onClick: () -> Unit
)

@Composable
fun EntryZone(
    entries: List<EntryItem>,
    modifier: Modifier = Modifier
) {
    if (entries.isEmpty()) return

    val listState = rememberLazyListState()
    val density = LocalDensity.current
    var contentHeight by remember { mutableStateOf(0.dp) }

    val isAtStart = remember { mutableStateOf(true) }
    val isAtEnd = remember { mutableStateOf(false) }

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        val layoutInfo = listState.layoutInfo
        val totalCount = layoutInfo.totalItemsCount
        val visibleItems = layoutInfo.visibleItemsInfo

        isAtStart.value = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0

        if (totalCount > 0 && visibleItems.isNotEmpty()) {
            val lastVisibleItem = visibleItems.last()
            val isLastItemVisible = lastVisibleItem.index == totalCount - 1
            val viewportWidth = layoutInfo.viewportSize.width
            val itemEnd = lastVisibleItem.offset + lastVisibleItem.size
            isAtEnd.value = isLastItemVisible && itemEnd <= viewportWidth
        } else {
            isAtEnd.value = true
        }
    }

    val startGradientAlpha by animateFloatAsState(
        targetValue = if (isAtStart.value) 0f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "entryZoneStartGradientAlpha"
    )
    val endGradientAlpha by animateFloatAsState(
        targetValue = if (isAtEnd.value) 0f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "entryZoneEndGradientAlpha"
    )

    Box(modifier = modifier.fillMaxWidth()) {
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { size ->
                    contentHeight = with(density) { size.height.toDp() }
                },
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(
                items = entries,
                key = { it.id }
            ) { entry ->
                EntryCard(entry = entry)
            }
        }

        Box(
            modifier = Modifier
                .width(16.dp)
                .height(if (contentHeight > 0.dp) contentHeight else 1.dp)
                .align(Alignment.CenterStart)
                .alpha(startGradientAlpha)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            Color.Transparent
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .width(16.dp)
                .height(if (contentHeight > 0.dp) contentHeight else 1.dp)
                .align(Alignment.CenterEnd)
                .alpha(endGradientAlpha)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )
    }
}

@Composable
private fun EntryCard(
    entry: EntryItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .widthIn(min = 148.dp)
            .clickable { entry.onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = entry.icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (entry.badgeText != null) {
                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = entry.badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
