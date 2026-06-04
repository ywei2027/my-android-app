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
    /**
     * 按分类拉取头条新闻（Cache-Fallback 模式）。
     * 异步缓存不阻塞响应 — 缓存写入失败不影响主流程（D-54 决议）。
     */
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
                        // 异步写入，fire-and-forget — 缓存写入失败不影响主流程
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
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
                            } catch (_: Exception) { /* 缓存写入失败不影响主流程 */ }
                        }
                        Timber.d("Network success, cached ${articles.size} articles")
                        Result.success(articles)
                    } else {
                        Timber.w("API status=${response.status}, fallback to cache")
                        fallbackToCache(category.apiValue, ApiException(response.status))
                    }
                } catch (e: HttpException) {
                    Timber.e(e, "HTTP ${e.code()} failed, fallback to cache")
                    fallbackToCache(category.apiValue, e)
                } catch (e: IOException) {
                    Timber.e(e, "Network unavailable, fallback to cache")
                    fallbackToCache(category.apiValue, e)
                } catch (e: Exception) {
                    Timber.e(e, "Unexpected error")
                    Result.failure(e)
                }
            }
        }
    }

    /**
     * 从 Room 缓存按 URL 查找单篇文章。
     * 用于详情页加载（B1-P0-1 修复：从本地缓存查询而非 API 盲搜）。
     */
    suspend fun getArticleByUrl(url: String): Result<NewsArticle> {
        return withContext(Dispatchers.IO) {
            try {
                val entity = newsDao.getByUrl(url)
                if (entity != null) {
                    Result.success(entity.toNewsArticle())
                } else {
                    Result.failure(ArticleNotFoundException("文章未找到: $url"))
                }
            } catch (e: Exception) {
                Timber.e(e, "getArticleByUrl failed")
                Result.failure(e)
            }
        }
    }

    private suspend fun fallbackToCache(
        category: String,
        originalError: Exception
    ): Result<List<NewsArticle>> {
        val cached = newsDao.getByCategory(category).map { it.toNewsArticle() }
        return if (cached.isNotEmpty()) {
            Result.success(cached)
        } else {
            Result.failure(originalError)
        }
    }
}

class ApiException(status: String) : Exception("API returned status: $status")
class ArticleNotFoundException(message: String) : Exception(message)
