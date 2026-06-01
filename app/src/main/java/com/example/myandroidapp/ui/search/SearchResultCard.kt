package com.example.myandroidapp.ui.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.myandroidapp.domain.search.SearchResultItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchResultCard(
    item: SearchResultItem,
    query: String,
    isLoading: Boolean,
    onClick: () -> Unit,
    onCopyExpression: (String) -> Unit,
    onCopyResult: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDropdown by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (isLoading) Modifier.alpha(0.5f) else Modifier)
            .combinedClickable(
                enabled = !isLoading,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showDropdown = true
                },
            )
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .semantics {
                contentDescription = "${item.history.expression}=${item.history.result}, ${relativeTimestamp(item.history.timestamp)}"
            },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = buildHighlightedText(
                        text = item.history.expression,
                        query = query,
                        ranges = item.highlightRanges,
                    ),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = androidx.compose.ui.unit.TextUnit(16f, androidx.compose.ui.unit.TextUnitType.Sp),
                        lineHeight = androidx.compose.ui.unit.TextUnit(24f, androidx.compose.ui.unit.TextUnitType.Sp),
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "= ${item.history.result}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = relativeTimestamp(item.history.timestamp),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = androidx.compose.ui.unit.TextUnit(11f, androidx.compose.ui.unit.TextUnitType.Sp),
                    lineHeight = androidx.compose.ui.unit.TextUnit(16f, androidx.compose.ui.unit.TextUnitType.Sp),
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        DropdownMenu(
            expanded = showDropdown,
            onDismissRequest = { showDropdown = false },
        ) {
            DropdownMenuItem(
                text = { Text("复制结果") },
                onClick = {
                    onCopyResult(item.history.result)
                    showDropdown = false
                },
            )
            DropdownMenuItem(
                text = { Text("复制表达式") },
                onClick = {
                    onCopyExpression(item.history.expression)
                    showDropdown = false
                },
            )
        }
    }
}

private val now24hFormat = SimpleDateFormat("今天 HH:mm", Locale.getDefault())
private val yesterdayFormat = SimpleDateFormat("昨天 HH:mm", Locale.getDefault())
private val daysAgoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

private fun relativeTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val hours24 = TimeUnit.HOURS.toMillis(24)
    val hours48 = TimeUnit.HOURS.toMillis(48)
    val days7 = TimeUnit.DAYS.toMillis(7)

    return when {
        diff < hours24 -> now24hFormat.format(Date(timestamp))
        diff < hours48 -> yesterdayFormat.format(Date(timestamp))
        diff < days7 -> "${TimeUnit.MILLISECONDS.toDays(diff) + 1}天前"
        else -> daysAgoFormat.format(Date(timestamp))
    }
}
