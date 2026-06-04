package com.example.myandroidapp.data.repository

import com.example.myandroidapp.data.local.NewsDao
import com.example.myandroidapp.data.local.toNewsArticle
import com.example.myandroidapp.data.remote.NewsApiService
import com.example.myandroidapp.data.remote.toNewsArticle
import com.example.myandroidapp.domain.model.NewsArticle
import com.example.myandroidapp.domain.model.NewsCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsRepository @Inject constructor(
    private val newsApiService: NewsApiService,
    private val newsDao: NewsDao
) {
    suspend fun getTopHeadlines(
        category: NewsCategory,
        page: Int = 1,
        pageSize: Int = 20
    ): Result<List<NewsArticle>> {
        return withTimeout(30_000L) {
            withContext(Dispatchers.IO) {
                try {
                    val response = newsApiService.getTopHeadlines(
                        country = "cn",
                        category = category.apiValue,
                        page = page,
                        pageSize = pageSize
                    )
                    if (response.status == "ok") {
                        val articles = response.articles.map { it.toNewsArticle(category) }
                        // 异步缓存不阻塞响应
                        CoroutineScope(Dispatchers.IO).launch {
                            newsDao.insertAll(articles.map { article ->
                                com.example.myandroidapp.data.local.NewsArticleEntity(
                                    url = article.url,
                                    title = article.title,
                                    description = article.description,
                                    sourceName = article.sourceName,
                                    publishedAt = article.publishedAt,
                                    urlToImage = article.urlToImage,
                                    category = article.category.apiValue
                                )
                            })
                        }
                        Timber.d("Network success, cached ${articles.size} articles")
                        Result.success(articles)
                    } else {
                        Timber.w("API status=${response.status}, fallback to cache")
                        val cached = newsDao.getByCategory(category.apiValue).map { it.toNewsArticle() }
                        if (cached.isNotEmpty()) Result.success(cached)
                        else Result.failure(ApiException(response.status))
                    }
                } catch (e: HttpException) {
                    Timber.e(e, "HTTP ${e.code()} failed, fallback to cache")
                    val cached = newsDao.getByCategory(category.apiValue).map { it.toNewsArticle() }
                    if (cached.isNotEmpty()) Result.success(cached)
                    else Result.failure(e)
                } catch (e: IOException) {
                    Timber.e(e, "Network unavailable, fallback to cache")
                    val cached = newsDao.getByCategory(category.apiValue).map { it.toNewsArticle() }
                    if (cached.isNotEmpty()) Result.success(cached)
                    else Result.failure(e)
                } catch (e: Exception) {
                    Timber.e(e, "Unexpected error")
                    Result.failure(e)
                }
            }
        }
    }
}

class ApiException(status: String) : Exception("API returned status: $status")
