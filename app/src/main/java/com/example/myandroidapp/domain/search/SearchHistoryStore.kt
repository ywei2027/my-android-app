package com.example.myandroidapp.domain.search

import kotlinx.coroutines.flow.Flow

data class SearchQuery(
    val query: String,
    val timestamp: Long,
)

interface SearchHistoryStore {
    val history: Flow<List<SearchQuery>>

    suspend fun addQuery(query: String)
    suspend fun removeQuery(query: String)
    suspend fun clearAll()
}
