package com.example.myandroidapp.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.myandroidapp.domain.search.SearchQuery

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchHistorySection(
    history: List<SearchQuery>,
    isLoading: Boolean,
    onHistoryClick: (SearchQuery) -> Unit,
    onHistoryDelete: (String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showClearDialog by remember { mutableStateOf(false) }
    var showDropdownFor by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current

    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = "最近搜索",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp)),
            color = MaterialTheme.colorScheme.outline,
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn {
            items(history, key = { it.query }) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (isLoading) Modifier.alpha(0.5f) else Modifier)
                        .combinedClickable(
                            enabled = !isLoading,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onHistoryClick(item) },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showDropdownFor = item.query
                            },
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = item.query,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = androidx.compose.ui.unit.TextUnit(16f, androidx.compose.ui.unit.TextUnitType.Sp)),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    IconButton(
                        onClick = { onHistoryDelete(item.query) },
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { contentDescription = "删除此搜索记录" },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "删除此搜索记录",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }

                    DropdownMenu(
                        expanded = showDropdownFor == item.query,
                        onDismissRequest = { showDropdownFor = null },
                    ) {
                        DropdownMenuItem(
                            text = { Text("删除") },
                            onClick = {
                                onHistoryDelete(item.query)
                                showDropdownFor = null
                            },
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = { showClearDialog = true },
            enabled = history.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .then(if (history.isEmpty()) Modifier.alpha(0.38f) else Modifier),
        ) {
            Text(
                "清除全部搜索历史",
                color = MaterialTheme.colorScheme.error,
            )
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("确定清除全部搜索历史？") },
            text = { Text("此操作不可撤销") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAll()
                        showClearDialog = false
                    },
                ) {
                    Text("确定", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("取消")
                }
            },
        )
    }
}
