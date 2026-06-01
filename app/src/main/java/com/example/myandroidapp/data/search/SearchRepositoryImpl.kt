package com.example.myandroidapp.data.search

import com.example.myandroidapp.domain.search.SearchRepository
import com.example.myandroidapp.domain.search.SearchResultItem
import com.example.myandroidapp.domain.search.SearchSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val sources: Set<@JvmSuppressWildcards SearchSource>,
) : SearchRepository {

    override suspend fun search(query: String): List<SearchResultItem> =
        withContext(Dispatchers.IO) {
            sources.flatMap { it.search(query) }.take(200)
        }
}
