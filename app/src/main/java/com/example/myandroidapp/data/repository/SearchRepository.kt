package com.example.myandroidapp.data.repository

import com.example.myandroidapp.data.local.NewsDao
import com.example.myandroidapp.data.local.toNewsArticle
import com.example.myandroidapp.domain.model.NewsArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepository @Inject constructor(
    private val newsDao: NewsDao
) {
    suspend fun search(query: String): List<NewsArticle> {
        return withTimeout(10_000L) {
            withContext(Dispatchers.IO) {
                try {
                    newsDao.searchByTitleAndDescription("%$query%")
                        .map { it.toNewsArticle() }
                } catch (e: Exception) {
                    Timber.e(e, "Search failed for query=$query")
                    emptyList()
                }
            }
        }
    }
}
