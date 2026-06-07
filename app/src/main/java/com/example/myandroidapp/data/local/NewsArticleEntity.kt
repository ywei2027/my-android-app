package com.example.myandroidapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "news_articles", indices = [androidx.room.Index(value = ["category"])])
data class NewsArticleEntity(
    @PrimaryKey val url: String,
    val title: String,
    val description: String?,
    val sourceName: String,
    val publishedAt: String,
    val urlToImage: String?,
    val category: String,
    val cachedAt: Long = System.currentTimeMillis()
)
