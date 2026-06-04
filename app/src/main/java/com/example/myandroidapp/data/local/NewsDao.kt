package com.example.myandroidapp.data.local

import androidx.room.*

@Dao
interface NewsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(articles: List<NewsArticleEntity>)

    @Query("SELECT * FROM news_articles WHERE category = :category ORDER BY publishedAt DESC")
    suspend fun getByCategory(category: String): List<NewsArticleEntity>

    @Query("SELECT * FROM news_articles WHERE url = :url LIMIT 1")
    suspend fun getByUrl(url: String): NewsArticleEntity?

    @Query("""
        SELECT * FROM news_articles
        WHERE title LIKE :query OR description LIKE :query
        ORDER BY publishedAt DESC
        LIMIT 50
    """)
    suspend fun searchByTitleAndDescription(query: String): List<NewsArticleEntity>

    @Query("DELETE FROM news_articles WHERE cachedAt < :expireTime")
    suspend fun deleteExpired(expireTime: Long)

    @Query("SELECT COUNT(*) FROM news_articles")
    suspend fun getCount(): Int
}
