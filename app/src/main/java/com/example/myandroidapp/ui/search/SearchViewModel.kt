package com.example.myandroidapp.ui.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myandroidapp.domain.search.SearchHistoryStore
import com.example.myandroidapp.domain.search.SearchQuery
import com.example.myandroidapp.domain.search.SearchRepository
import com.example.myandroidapp.domain.search.SearchResultItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import java.util.concurrent.TimeoutException
import javax.inject.Inject

sealed interface SearchEvent {
    data class OnQueryChange(val query: String) : SearchEvent
    data object OnImeSearch : SearchEvent
    data class OnHistoryClick(val query: SearchQuery) : SearchEvent
    data class OnHistoryDelete(val query: String) : SearchEvent
    data object OnClearAllHistory : SearchEvent
    data class OnResultClick(val itemId: String) : SearchEvent
    data class OnResultLongPress(val item: SearchResultItem) : SearchEvent
    data object OnRetry : SearchEvent
    data object OnDismiss : SearchEvent
    data object OnClearInput : SearchEvent
    data class OnExpand(val isExpanded: Boolean) : SearchEvent
    data class CopyToClipboard(val text: String, val label: String) : SearchEvent
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val historyStore: SearchHistoryStore,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Docked)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SearchEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<SearchEvent> = _events.asSharedFlow()

    private val _snackbarMessages = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 3,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    private var searchJob: Job? = null
    private var lastQuery = ""

    init {
        loadSavedState()
    }

    private fun loadSavedState() {
        viewModelScope.launch {
            val history = historyStore.history.first()
            val savedQuery = savedStateHandle.get<String>("query")
            if (!savedQuery.isNullOrBlank()) {
                _uiState.value = SearchUiState.Loading(savedQuery)
                performSearch(savedQuery)
            } else if (history.isNotEmpty()) {
                _uiState.value = SearchUiState.History(query = "", history = history)
            }
        }
    }

    fun onEvent(event: SearchEvent) {
        when (event) {
            is SearchEvent.OnQueryChange -> onQueryChange(event.query)
            is SearchEvent.OnImeSearch -> onImeSearch()
            is SearchEvent.OnHistoryClick -> onHistoryClick(event.query)
            is SearchEvent.OnHistoryDelete -> onHistoryDelete(event.query)
            is SearchEvent.OnClearAllHistory -> onClearAllHistory()
            is SearchEvent.OnResultClick -> onResultClick(event.itemId)
            is SearchEvent.OnResultLongPress -> onResultLongPress(event.item)
            is SearchEvent.OnRetry -> onRetry()
            is SearchEvent.OnDismiss -> onDismiss()
            is SearchEvent.OnClearInput -> onClearInput()
            is SearchEvent.OnExpand -> onExpand(event.isExpanded)
            is SearchEvent.CopyToClipboard -> onCopyToClipboard(event)
        }
    }

    private fun onQueryChange(query: String) {
        lastQuery = query
        savedStateHandle["query"] = query

        if (query.isBlank()) {
            searchJob?.cancel()
            searchJob = null
            viewModelScope.launch {
                val history = historyStore.history.first()
                _uiState.value = SearchUiState.History(query = query, history = history)
            }
            return
        }

        if (query.length > 100) {
            viewModelScope.launch { _snackbarMessages.emit("最多输入100字") }
            return
        }

        val previousState = _uiState.value
        _uiState.value = SearchUiState.Typing(query = query, previousState = previousState)

        searchJob?.cancel()
        Timber.d("Search cancelled: newQuery=%s", query)

        searchJob = viewModelScope.launch {
            delay(300)
            performSearch(query)
        }
    }

    private fun onImeSearch() {
        if (lastQuery.isNotBlank()) {
            searchJob?.cancel()
            searchJob = viewModelScope.launch {
                performSearch(lastQuery)
            }
        }
    }

    private fun onHistoryClick(searchQuery: SearchQuery) {
        lastQuery = searchQuery.query
        savedStateHandle["query"] = searchQuery.query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            performSearch(searchQuery.query)
        }
    }

    private fun onHistoryDelete(query: String) {
        viewModelScope.launch {
            historyStore.removeQuery(query)
            _snackbarMessages.emit("已删除")
            val history = historyStore.history.first()
            _uiState.value = SearchUiState.History(query = lastQuery, history = history)
        }
    }

    private fun onClearAllHistory() {
        viewModelScope.launch {
            historyStore.clearAll()
            val history = historyStore.history.first()
            _uiState.value = SearchUiState.History(query = lastQuery, history = history)
        }
    }

    private fun onResultClick(itemId: String) {
        viewModelScope.launch {
            _events.emit(SearchEvent.OnResultClick(itemId))
        }
    }

    private fun onResultLongPress(item: SearchResultItem) {
        viewModelScope.launch {
            _events.emit(SearchEvent.OnResultLongPress(item))
        }
    }

    private fun onRetry() {
        if (lastQuery.isNotBlank()) {
            _uiState.value = SearchUiState.Loading(lastQuery)
            searchJob?.cancel()
            searchJob = viewModelScope.launch {
                performSearch(lastQuery)
            }
        }
    }

    private fun onDismiss() {
        searchJob?.cancel()
        searchJob = null
        _uiState.value = SearchUiState.Docked
    }

    private fun onClearInput() {
        lastQuery = ""
        savedStateHandle["query"] = ""
        searchJob?.cancel()
        searchJob = null
        viewModelScope.launch {
            val history = historyStore.history.first()
            _uiState.value = SearchUiState.History(query = "", history = history)
        }
    }

    private fun onExpand(isExpanded: Boolean) {
        if (isExpanded) {
            viewModelScope.launch {
                val history = historyStore.history.first()
                _uiState.value = if (history.isEmpty()) {
                    SearchUiState.History(query = "", history = history)
                } else {
                    SearchUiState.History(query = "", history = history)
                }
            }
        }
    }

    private fun onCopyToClipboard(event: SearchEvent.CopyToClipboard) {
        viewModelScope.launch {
            _snackbarMessages.emit("已复制到剪贴板")
        }
    }

    private suspend fun performSearch(query: String) {
        if (query.isBlank()) return
        Timber.d("Search started: query=%s", query)
        _uiState.value = SearchUiState.Loading(query)
        val startMs = System.currentTimeMillis()

        try {
            val results = withContext(Dispatchers.IO) {
                withTimeout(5_000) {
                    searchRepository.search(query)
                }
            }
            val latencyMs = System.currentTimeMillis() - startMs
            Timber.i("Search done: %d results, %dms", results.size, latencyMs)

            _uiState.value = if (results.isEmpty()) {
                SearchUiState.Empty(query)
            } else {
                SearchUiState.Results(query = query, items = results, totalCount = results.size)
            }

            // Save to search history
            historyStore.addQuery(query)
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Timber.e(e, "Search timeout: query=%s", query)
            _uiState.value = SearchUiState.Error(query = query, message = "搜索超时，请重试")
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Timber.e(e, "Search failed: query=%s", query)
            val previousItems = (_uiState.value as? SearchUiState.Results)?.items ?: emptyList()
            _uiState.value = SearchUiState.Error(
                query = query,
                message = e.message ?: "搜索失败",
                previousItems = previousItems,
            )
        }
    }
}
