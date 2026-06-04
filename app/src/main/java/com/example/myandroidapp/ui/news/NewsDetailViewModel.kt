package com.example.myandroidapp.ui.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myandroidapp.data.repository.NewsRepository
import com.example.myandroidapp.domain.model.NewsArticle
import com.example.myandroidapp.domain.model.NewsCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
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

    fun loadDetail(articleId: String) {
        viewModelScope.launch {
            _uiState.value = DetailUiState.Loading
            Timber.d("loadDetail articleId=$articleId")
            try {
                withTimeout(30_000L) {
                    // 获取第一页数据（任意分类）然后按 URL 过滤
                    val result = newsRepository.getTopHeadlines(
                        category = NewsCategory.RECOMMENDED,
                        page = 1,
                        pageSize = 1
                    )
                    result.onSuccess { articles ->
                        val article = articles.find { it.url == articleId }
                        if (article != null) {
                            _uiState.value = DetailUiState.Success(article)
                            Timber.d("loadDetail SUCCESS: ${article.title}")
                        } else {
                            _uiState.value = DetailUiState.Error("文章未找到")
                            Timber.w("loadDetail article not found: $articleId")
                        }
                    }.onFailure { e ->
                        _uiState.value = DetailUiState.Error("加载失败，请检查网络")
                        Timber.e(e, "loadDetail FAILED")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = DetailUiState.Error("请求超时，请重试")
                Timber.e(e, "loadDetail TIMEOUT")
            }
        }
    }
}
