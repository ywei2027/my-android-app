package com.example.myandroidapp.ui.search

import com.example.myandroidapp.domain.search.SearchQuery
import com.example.myandroidapp.domain.search.SearchResultItem

sealed interface SearchUiState {
    data object Docked : SearchUiState

    data class History(
        val query: String,
        val history: List<SearchQuery>,
    ) : SearchUiState

    data class Typing(
        val query: String,
        val previousState: SearchUiState,
    ) : SearchUiState

    data class Loading(
        val query: String,
    ) : SearchUiState

    data class Results(
        val query: String,
        val items: List<SearchResultItem>,
        val totalCount: Int,
    ) : SearchUiState

    data class Empty(
        val query: String,
    ) : SearchUiState

    data class Error(
        val query: String,
        val message: String,
        val previousItems: List<SearchResultItem> = emptyList(),
    ) : SearchUiState
}
