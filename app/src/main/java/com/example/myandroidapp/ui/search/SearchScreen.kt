package com.example.myandroidapp.ui.search

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    isExpanded: Boolean,
    viewModel: SearchViewModel,
    onNavigateToCalculator: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarMessages by viewModel.snackbarMessages.collectAsState(initial = null)
    val events by viewModel.events.collectAsState(initial = null)

    val snackbarHostState = remember { SnackbarHostState() }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    val currentQuery = when (val state = uiState) {
        is SearchUiState.History -> state.query
        is SearchUiState.Typing -> state.query
        is SearchUiState.Loading -> state.query
        is SearchUiState.Results -> state.query
        is SearchUiState.Empty -> state.query
        is SearchUiState.Error -> state.query
        else -> ""
    }

    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {
            }
        }
    }

    // Handle navigation events
    LaunchedEffect(events) {
        when (val event = events) {
            is SearchEvent.OnResultClick -> {
                onDismiss()
                onNavigateToCalculator(event.itemId)
            }
            is SearchEvent.CopyToClipboard -> {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText(event.label, event.text))
            }
            else -> {}
        }
    }

    // Handle snackbar messages
    LaunchedEffect(snackbarMessages) {
        snackbarMessages?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    // BackHandler: search expanded takes priority
    BackHandler(enabled = isExpanded) {
        focusManager.clearFocus()
        onDismiss()
    }

    AnimatedVisibility(
        visible = isExpanded,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically(),
        modifier = modifier,
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            modifier = Modifier.fillMaxSize(),
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding(),
            ) {
                // Expanded SearchBar
                SearchBar(
                    query = currentQuery,
                    onQueryChange = { query ->
                        if (query.length > 100) {
                            Toast.makeText(context, "最多输入100字", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.onEvent(SearchEvent.OnQueryChange(query))
                        }
                    },
                    onSearch = {
                        viewModel.onEvent(SearchEvent.OnImeSearch)
                    },
                    active = true,
                    onActiveChange = { /* always active in overlay */ },
                    placeholder = {
                        Text(
                            "搜索计算历史",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    leadingIcon = {
                        IconButton(
                            onClick = {
                                focusManager.clearFocus()
                                onDismiss()
                            },
                            modifier = Modifier.semantics {
                                contentDescription = "收起搜索"
                            },
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "收起搜索",
                            )
                        }
                    },
                    trailingIcon = {
                        if (currentQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.onEvent(SearchEvent.OnClearInput) },
                                modifier = Modifier.semantics {
                                    contentDescription = "清除搜索内容"
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "清除搜索内容",
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                        .focusRequester(focusRequester),
                    colors = SearchBarDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )

                // Content based on uiState
                Box(modifier = Modifier.fillMaxSize()) {
                    when (val state = uiState) {
                        is SearchUiState.Docked -> { /* not shown */ }

                        is SearchUiState.History -> {
                            if (state.history.isEmpty() && currentQuery.isEmpty()) {
                                EmptyHistoryGuide(modifier = Modifier.padding(16.dp))
                            } else {
                                SearchHistorySection(
                                    history = state.history,
                                    isLoading = false,
                                    onHistoryClick = { viewModel.onEvent(SearchEvent.OnHistoryClick(it)) },
                                    onHistoryDelete = { viewModel.onEvent(SearchEvent.OnHistoryDelete(it)) },
                                    onClearAll = { viewModel.onEvent(SearchEvent.OnClearAllHistory) },
                                )
                            }
                        }

                        is SearchUiState.Typing -> {
                            val prev = state.previousState
                            if (prev is SearchUiState.History || prev is SearchUiState.Docked) {
                                val hist = (prev as? SearchUiState.History)?.history
                                if (hist.isNullOrEmpty()) {
                                    EmptyHistoryGuide(modifier = Modifier.padding(16.dp))
                                } else {
                                    SearchHistorySection(
                                        history = hist,
                                        isLoading = false,
                                        onHistoryClick = { viewModel.onEvent(SearchEvent.OnHistoryClick(it)) },
                                        onHistoryDelete = { viewModel.onEvent(SearchEvent.OnHistoryDelete(it)) },
                                        onClearAll = { viewModel.onEvent(SearchEvent.OnClearAllHistory) },
                                    )
                                }
                            } else if (prev is SearchUiState.Results) {
                                ResultsContent(
                                    items = prev.items,
                                    totalCount = prev.totalCount,
                                    query = prev.query,
                                    isLoading = false,
                                    onResultClick = { viewModel.onEvent(SearchEvent.OnResultClick(it)) },
                                    onCopyExpression = { viewModel.onEvent(SearchEvent.CopyToClipboard(it, "表达式")) },
                                    onCopyResult = { viewModel.onEvent(SearchEvent.CopyToClipboard(it, "结果")) },
                                )
                            } else if (prev is SearchUiState.Error) {
                                val prevItems = prev.previousItems
                                if (prevItems.isNotEmpty()) {
                                    ResultsContent(
                                        items = prevItems,
                                        totalCount = prevItems.size,
                                        query = prev.query,
                                        isLoading = false,
                                        onResultClick = { viewModel.onEvent(SearchEvent.OnResultClick(it)) },
                                        onCopyExpression = { viewModel.onEvent(SearchEvent.CopyToClipboard(it, "表达式")) },
                                        onCopyResult = { viewModel.onEvent(SearchEvent.CopyToClipboard(it, "结果")) },
                                    )
                                }
                            }
                        }

                        is SearchUiState.Loading -> ShimmerSearchSkeleton()

                        is SearchUiState.Results -> {
                            ResultsContent(
                                items = state.items,
                                totalCount = state.totalCount,
                                query = state.query,
                                isLoading = false,
                                onResultClick = { viewModel.onEvent(SearchEvent.OnResultClick(it)) },
                                onCopyExpression = { viewModel.onEvent(SearchEvent.CopyToClipboard(it, "表达式")) },
                                onCopyResult = { viewModel.onEvent(SearchEvent.CopyToClipboard(it, "结果")) },
                            )
                        }

                        is SearchUiState.Empty -> EmptySearchView(query = state.query)

                        is SearchUiState.Error -> {
                            val prevItems = state.previousItems
                            if (prevItems.isNotEmpty()) {
                                ResultsContent(
                                    items = prevItems,
                                    totalCount = prevItems.size,
                                    query = state.query,
                                    isLoading = false,
                                    onResultClick = { viewModel.onEvent(SearchEvent.OnResultClick(it)) },
                                    onCopyExpression = { viewModel.onEvent(SearchEvent.CopyToClipboard(it, "表达式")) },
                                    onCopyResult = { viewModel.onEvent(SearchEvent.CopyToClipboard(it, "结果")) },
                                )
                            }
                            LaunchedEffect(state.message) {
                                snackbarHostState.showSnackbar(
                                    message = state.message,
                                    actionLabel = "重试",
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultsContent(
    items: List<com.example.myandroidapp.domain.search.SearchResultItem>,
    totalCount: Int,
    query: String,
    isLoading: Boolean,
    onResultClick: (String) -> Unit,
    onCopyExpression: (String) -> Unit,
    onCopyResult: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(16.dp)) {
        AnimatedVisibility(visible = true, enter = fadeIn()) {
            Text(
                text = "找到 $totalCount 条结果",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp),
                    lineHeight = androidx.compose.ui.unit.TextUnit(16f, androidx.compose.ui.unit.TextUnitType.Sp),
                ),
                color = MaterialTheme.colorScheme.outline,
            )
        }
        LazyColumn {
            items(items, key = { it.history.id }) { item ->
                SearchResultCard(
                    item = item,
                    query = query,
                    isLoading = isLoading,
                    onClick = { onResultClick(item.history.id) },
                    onCopyExpression = onCopyExpression,
                    onCopyResult = onCopyResult,
                )
            }
            if (totalCount > 200) {
                item {
                    Text(
                        text = "仅显示最近 200 条结果",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
        }
    }
}
