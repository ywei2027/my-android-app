package com.example.myandroidapp.ui.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myandroidapp.data.repository.NewsRepository
import com.example.myandroidapp.domain.model.NewsArticle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Success(val article: NewsArticle) : DetailUiState
    data class Error(val message: String) : DetailUiState
}

@HiltViewModel
class NewsDetailViewModel @Inject constructor(
    private val newsRepository: NewsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    /**
     * B1-P0-1 修复：从 Room 缓存按 URL 查询文章，而非 API 盲搜 pageSize=1。
     * Room 之前已通过列表页的 getTopHeadlines 异步缓存了各分类数据。
     */
    fun loadDetail(articleId: String) {
        viewModelScope.launch {
            _uiState.value = DetailUiState.Loading
            Timber.d("loadDetail articleId=$articleId")
            newsRepository.getArticleByUrl(articleId)
                .onSuccess { article ->
                    _uiState.value = DetailUiState.Success(article)
                    Timber.d("loadDetail SUCCESS: ${article.title}")
                }
                .onFailure { e ->
                    Timber.w(e, "loadDetail not found in cache: $articleId")
                    _uiState.value = DetailUiState.Error("文章未找到，请返回重试")
                }
        }
    }
}
