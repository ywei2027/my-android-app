package com.example.myandroidapp.ui.news

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myandroidapp.data.remote.Http429Exception
import com.example.myandroidapp.data.repository.NewsRepository
import com.example.myandroidapp.data.repository.SearchRepository
import com.example.myandroidapp.domain.model.NewsArticle
import com.example.myandroidapp.domain.model.NewsCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import javax.inject.Inject

sealed interface NewsListUiState {
    data object FirstLoading : NewsListUiState
    data class Loading(val currentArticles: List<NewsArticle> = emptyList()) : NewsListUiState
    data class Success(val articles: List<NewsArticle>) : NewsListUiState
    data object Empty : NewsListUiState
    data class Error(val message: String) : NewsListUiState
    data class PagingLoading(val currentArticles: List<NewsArticle>) : NewsListUiState
}

@HiltViewModel
class NewsListViewModel @Inject constructor(
    private val newsRepository: NewsRepository,
    private val searchRepository: SearchRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<NewsListUiState>(NewsListUiState.FirstLoading)
    val uiState: StateFlow<NewsListUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    private val _selectedTab = MutableStateFlow(NewsCategory.RECOMMENDED)
    val selectedTab: StateFlow<NewsCategory> = _selectedTab.asStateFlow()

    private val _searchResults = MutableStateFlow<List<NewsArticle>>(emptyList())
    val searchResults: StateFlow<List<NewsArticle>> = _searchResults.asStateFlow()

    private val _snackbarEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val snackbarEvent: SharedFlow<String> = _snackbarEvent.asSharedFlow()

    private var loadJob: Job? = null
    private var currentPage = 1
    private var currentData: List<NewsArticle> = emptyList()

    // 客户端分页上限 — 防止无限累积 OOM
    private val MAX_CACHED_ARTICLES = 200

    init {
        // 从 SavedStateHandle 恢复 Tab 状态
        _selectedTab.value = savedStateHandle.get<NewsCategory>("selectedTab") ?: NewsCategory.RECOMMENDED
        currentPage = savedStateHandle.get<Int>("currentPage") ?: 1
        loadNews()
        observeSearchQuery()
        Timber.d("NewsListViewModel initialized, tab=${_selectedTab.value}")
    }

    fun loadNews(category: NewsCategory = _selectedTab.value, isRefresh: Boolean = false) {
        loadJob?.cancel() // 取消前一个 tab 加载防止竞态
        loadJob = viewModelScope.launch {
            Timber.d("loadNews category=$category isRefresh=$isRefresh")
            _uiState.value = if (isRefresh) _uiState.value else NewsListUiState.FirstLoading
            _selectedTab.value = category
            savedStateHandle["selectedTab"] = category // 持久化 Tab
            currentPage = 1
            savedStateHandle["currentPage"] = 1
            newsRepository.getTopHeadlines(category, page = 1)
                .onSuccess { articles ->
                    currentData = articles
                    _uiState.value = if (articles.isEmpty()) NewsListUiState.Empty
                                    else NewsListUiState.Success(articles)
                    Timber.d("loadNews SUCCESS, count=${articles.size}")
                }
                .onFailure { e ->
                    Timber.e(e, "loadNews FAILED")
                    _uiState.value = when (e) {
                        is Http429Exception -> NewsListUiState.Error("请求太频繁，请稍后再试")
                        else -> NewsListUiState.Error("加载失败，请检查网络")
                    }
                }
        }
    }

    fun loadMore() {
        if (_uiState.value is NewsListUiState.PagingLoading) return
        val oldData = currentData
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = NewsListUiState.PagingLoading(oldData)
            Timber.d("loadMore page=${currentPage + 1}")
            newsRepository.getTopHeadlines(_selectedTab.value, page = ++currentPage)
                .onSuccess { moreArticles ->
                    // 客户端分页上限保护
                    currentData = (oldData + moreArticles).takeLast(MAX_CACHED_ARTICLES)
                    _uiState.value = NewsListUiState.Success(currentData)
                    savedStateHandle["currentPage"] = currentPage
                    Timber.d("loadMore SUCCESS, total=${currentData.size}")
                }
                .onFailure {
                    // 保留已加载数据
                    _uiState.value = NewsListUiState.Success(oldData)
                    _snackbarEvent.tryEmit("加载失败，请检查网络")
                    Timber.e("loadMore FAILED, keeping ${oldData.size} items")
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query.take(100) // 搜索词长度上限
        _isSearchActive.value = query.length >= 3
    }

    @OptIn(FlowPreview::class)
    private fun observeSearchQuery() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .filter { it.length >= 3 }
                .collectLatest { query ->
                    // collectLatest 自动取消前一个 lambda
                    Timber.d("search triggered: query=$query")
                    try {
                        val results = withTimeout(10_000L) { // 搜索超时保护
                            searchRepository.search(query)
                        }
                        _searchResults.value = results
                        Timber.d("search completed: ${results.size} results")
                    } catch (e: Exception) {
                        Timber.e(e, "search failed")
                        _searchResults.value = emptyList()
                    }
                }
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _isSearchActive.value = false
    }

    override fun onCleared() {
        super.onCleared()
        loadJob?.cancel()
        Timber.d("NewsListViewModel cleared")
    }
}
