package com.example.myandroidapp.ui.news

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // HorizontalPager 状态
    val categories = NewsCategory.entries
    val pagerState = rememberPagerState(
        initialPage = categories.indexOf(selectedTab),
        pageCount = { categories.size }
    )

    // 滑动时同步 ViewModel Tab + 触发加载
    LaunchedEffect(pagerState.currentPage) {
        val category = categories[pagerState.currentPage]
        if (category != selectedTab) {
            viewModel.loadNews(category)
        }
    }

    // 下拉刷新状态
    val pullToRefreshState = rememberPullToRefreshState()

    LaunchedEffect(pullToRefreshState.isRefreshing) {
        if (pullToRefreshState.isRefreshing) {
            viewModel.loadNews(categories[pagerState.currentPage], isRefresh = true)
        }
    }

    // 刷新完成后关闭指示器
    LaunchedEffect(uiState) {
        if (pullToRefreshState.isRefreshing) {
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
                // 搜索栏
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

                // 搜索激活 → 显示搜索结果
                if (isSearchActive) {
                    Box(modifier = Modifier.weight(1f)) {
                        SearchResultsView(
                            results = searchResults,
                            query = searchQuery,
                            onArticleClick = onArticleClick
                        )
                    }
                } else {
                    // Tab 栏 — 点击/滑动联动
                    TabRow(
                        selectedTabIndex = pagerState.currentPage,
                        modifier = Modifier.testTag("categoryTabRow")
                    ) {
                        categories.forEachIndexed { index, category ->
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index, animationSpec = tween(300))
                                    }
                                },
                                text = { Text(category.displayName) }
                            )
                        }
                    }

                    // HorizontalPager — 左右滑动切换分类
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) { page ->
                        val category = categories[page]
                        // 每个 Page 独立的内容视图
                        if (category == selectedTab) {
                            // 当前 Tab 已加载数据
                            CategoryContentView(
                                uiState = uiState,
                                onArticleClick = onArticleClick,
                                onRetry = { viewModel.loadNews(category) },
                                onLoadMore = { viewModel.loadMore() }
                            )
                        } else {
                            // 滑动到的 Tab 正在加载中
                            FirstLoadingView()
                        }
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
private fun FirstLoadingView() {
    LazyColumn(modifier = Modifier.testTag("newsCardList")) {
        items(3) { index ->
            ShimmerCard(
                modifier = Modifier.padding(vertical = NewsDimens.CardSpacing),
                testTag = "shimmerCard_$index"
            )
        }
    }
}

@Composable
private fun CategoryContentView(
    uiState: NewsListUiState,
    onArticleClick: (String) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit
) {
    when (uiState) {
        is NewsListUiState.FirstLoading -> {
            FirstLoadingView()
        }
        is NewsListUiState.Loading -> {
            Column(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                ArticleList(
                    articles = uiState.currentArticles,
                    onArticleClick = onArticleClick,
                    onLoadMore = onLoadMore
                )
            }
        }
        is NewsListUiState.Success -> {
            ArticleList(
                articles = uiState.articles,
                onArticleClick = onArticleClick,
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
                onLoadMore = onLoadMore,
                isPagingLoading = true
            )
        }
    }
}

@Composable
private fun ArticleList(
    articles: List<NewsArticle>,
    onArticleClick: (String) -> Unit,
    onLoadMore: () -> Unit,
    isPagingLoading: Boolean = false
) {
    val listState = remember { LazyListState() }
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
