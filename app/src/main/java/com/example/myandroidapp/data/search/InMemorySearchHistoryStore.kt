package com.example.myandroidapp.data.search

import com.example.myandroidapp.domain.search.SearchHistoryStore
import com.example.myandroidapp.domain.search.SearchQuery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemorySearchHistoryStore @Inject constructor() : SearchHistoryStore {

    private val _history = MutableStateFlow<List<SearchQuery>>(emptyList())

    override val history: Flow<List<SearchQuery>> = _history.asStateFlow()

    override suspend fun addQuery(query: String) {
        val trimmed = query.trim()
        val normalized = trimmed.lowercase()
        val current = _history.value.toMutableList()
        val existingIdx = current.indexOfFirst {
            it.query.trim().lowercase() == normalized
        }
        if (existingIdx >= 0) {
            current.removeAt(existingIdx)
        }
        current.add(0, SearchQuery(query = trimmed, timestamp = System.currentTimeMillis()))
        _history.value = current.take(5)
    }

    override suspend fun removeQuery(query: String) {
        _history.value = _history.value.filter { it.query != query }
    }

    override suspend fun clearAll() {
        _history.value = emptyList()
    }
}
