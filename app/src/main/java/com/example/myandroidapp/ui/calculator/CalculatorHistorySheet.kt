package com.example.myandroidapp.ui.calculator

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.myandroidapp.data.model.CalcHistory
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorHistorySheet(
    historyFlow: Flow<List<CalcHistory>>,
    isExpanded: Boolean,
    highlightItemId: String?,
    onDismiss: () -> Unit,
    onItemClick: (String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()

    if (isExpanded) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            modifier = modifier,
        ) {
            HistoryContent(
                historyFlow = historyFlow,
                listState = listState,
                highlightItemId = highlightItemId,
                onItemClick = onItemClick,
                onClearAll = onClearAll,
            )
        }
    }
}

@Composable
fun HistoryContent(
    historyFlow: Flow<List<CalcHistory>>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    highlightItemId: String?,
    onItemClick: (String) -> Unit,
    onClearAll: () -> Unit,
) {
    val history by historyFlow.collectAsState(initial = emptyList())
    val sortedHistory = remember(history) {
        history.sortedByDescending { it.timestamp }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "计算历史",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "清除",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.clickable { onClearAll() },
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(state = listState) {
            items(sortedHistory, key = { it.id }) { item ->
                HistoryItem(
                    calcHistory = item,
                    isHighlighted = highlightItemId == item.id,
                    onClick = { onItemClick(item.id) },
                )
            }
        }
    }
}

@Composable
fun HistoryItem(
    calcHistory: CalcHistory,
    isHighlighted: Boolean,
    onClick: () -> Unit,
) {
    val highlightColor = MaterialTheme.colorScheme.primaryContainer
    val backgroundColor by animateColorAsState(
        targetValue = if (isHighlighted) highlightColor else Color.Transparent,
        animationSpec = tween(durationMillis = 2000),
        label = "highlightAnim",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = calcHistory.expression,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "= ${calcHistory.result}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
