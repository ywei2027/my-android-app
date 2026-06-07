package com.example.myandroidapp.data.local

import com.example.myandroidapp.domain.model.NewsArticle
import com.example.myandroidapp.domain.model.NewsCategory

fun NewsArticleEntity.toNewsArticle(): NewsArticle = NewsArticle(
    url = url,
    title = title,
    description = description,
    sourceName = sourceName,
    publishedAt = publishedAt,
    urlToImage = urlToImage,
    category = NewsCategory.entries.find { it.apiValue == category } ?: NewsCategory.RECOMMENDED
)
