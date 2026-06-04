package com.example.myandroidapp.ui.news

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myandroidapp.domain.model.NewsArticle
import com.example.myandroidapp.domain.model.NewsCategory
import com.example.myandroidapp.ui.components.EmptyState
import com.example.myandroidapp.ui.components.ErrorState
import com.example.myandroidapp.ui.components.NewsCard
import com.example.myandroidapp.ui.components.NewsDimens
import com.example.myandroidapp.ui.components.ShimmerCard
import androidx.compose.runtime.derivedStateOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsListScreen(
    onArticleClick: (String) -> Unit,
    viewModel: NewsListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isSearchActive by viewModel.isSearchActive.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()

    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    // 下拉刷新状态
    val pullToRefreshState = rememberPullToRefreshState()

    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.loadNews(selectedTab, isRefresh = true)
            pullToRefreshState.endRefresh()
        }
    }

    // 收集 Snackbar 事件
    LaunchedEffect(Unit) {
        viewModel.snackbarEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    BackHandler(enabled = isSearchActive) {
        viewModel.clearSearch()
        focusManager.clearFocus()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("新闻") },
                actions = {
                    IconButton(onClick = { focusManager.clearFocus() }) {
                        Icon(Icons.Default.Search, contentDescription = "搜索")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .nestedScroll(pullToRefreshState.nestedScrollConnection)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = NewsDimens.CardPadding)
            ) {
                // 搜索栏 — PRD §9: OutlinedTextField, imeAction=Search
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("searchBar"),
                    placeholder = { Text("搜索新闻标题或描述...") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { focusManager.clearFocus() }
                    ),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.clearSearch() }) {
                                Icon(Icons.Default.SearchOff, contentDescription = "清除搜索")
                            }
                        }
                    }
                )

                // Tab 栏
                if (!isSearchActive) {
                    androidx.compose.material3.ScrollableTabRow(
                        selectedTabIndex = NewsCategory.entries.indexOf(selectedTab),
                        modifier = Modifier.testTag("categoryTabRow"),
                        edgePadding = 0.dp
                    ) {
                        NewsCategory.entries.forEach { category ->
                            androidx.compose.material3.Tab(
                                selected = selectedTab == category,
                                onClick = { viewModel.loadNews(category) },
                                text = { Text(category.displayName) }
                            )
                        }
                    }
                }

                // 内容区
                Box(modifier = Modifier.weight(1f)) {
                    when {
                        isSearchActive -> SearchResultsView(
                            results = searchResults,
                            query = searchQuery,
                            onArticleClick = onArticleClick
                        )
                        else -> ContentView(
                            uiState = uiState,
                            onArticleClick = onArticleClick,
                            onRetry = { viewModel.loadNews(selectedTab) },
                            onLoadMore = { viewModel.loadMore() },
                            listState = listState
                        )
                    }
                }
            }

            // 下拉刷新指示器
            PullToRefreshContainer(
                state = pullToRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
private fun SearchResultsView(
    results: List<NewsArticle>,
    query: String,
    onArticleClick: (String) -> Unit
) {
    if (query.length < 3) {
        EmptyState(
            title = "输入关键词搜索",
            subtitle = "至少输入3个字符",
            testTag = "searchEmptyState"
        )
    } else if (results.isEmpty()) {
        EmptyState(
            title = "未找到\"$query\"相关新闻",
            subtitle = "试试其他关键词",
            testTag = "searchEmptyState"
        )
    } else {
        LazyColumn(modifier = Modifier.testTag("searchOverlay")) {
            items(results, key = { it.url }) { article ->
                NewsCard(
                    article = article,
                    onClick = { onArticleClick(article.url) },
                    modifier = Modifier.padding(vertical = NewsDimens.CardSpacing)
                )
            }
        }
    }
}

@Composable
private fun ContentView(
    uiState: NewsListUiState,
    onArticleClick: (String) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    when (uiState) {
        is NewsListUiState.FirstLoading -> {
            LazyColumn(modifier = Modifier.testTag("newsCardList")) {
                items(3) { index ->
                    ShimmerCard(
                        modifier = Modifier.padding(vertical = NewsDimens.CardSpacing),
                        testTag = "shimmerCard_$index"
                    )
                }
            }
        }
        is NewsListUiState.Loading -> {
            Column(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                ArticleList(
                    articles = uiState.currentArticles,
                    onArticleClick = onArticleClick,
                    listState = listState,
                    onLoadMore = onLoadMore
                )
            }
        }
        is NewsListUiState.Success -> {
            ArticleList(
                articles = uiState.articles,
                onArticleClick = onArticleClick,
                listState = listState,
                onLoadMore = onLoadMore
            )
        }
        is NewsListUiState.Empty -> {
            EmptyState(title = "暂无新闻", subtitle = "下拉刷新试试", testTag = "emptyState")
        }
        is NewsListUiState.Error -> {
            ErrorState(message = uiState.message, onRetry = onRetry, testTag = "errorState")
        }
        is NewsListUiState.PagingLoading -> {
            ArticleList(
                articles = uiState.currentArticles,
                onArticleClick = onArticleClick,
                listState = listState,
                onLoadMore = onLoadMore,
                isPagingLoading = true
            )
        }
    }
}

/**
 * 无限滚动文章列表。
 * 使用 derivedStateOf 检测滚动到底部（距底 ≤3 项）自动触发分页。
 */
@Composable
private fun ArticleList(
    articles: List<NewsArticle>,
    onArticleClick: (String) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onLoadMore: () -> Unit,
    isPagingLoading: Boolean = false
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.testTag("newsCardList")
    ) {
        items(articles, key = { it.url }) { article ->
            NewsCard(
                article = article,
                onClick = { onArticleClick(article.url) },
                modifier = Modifier.padding(vertical = NewsDimens.CardSpacing)
            )
        }
        if (isPagingLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(NewsDimens.CardPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.testTag("pagingLoading")
                    )
                }
            }
        }
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleIndex >= totalItems - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !isPagingLoading) {
            onLoadMore()
        }
    }
}
