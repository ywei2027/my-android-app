package com.example.myandroidapp.data.remote

import com.example.myandroidapp.domain.model.NewsArticle
import com.example.myandroidapp.domain.model.NewsCategory

fun NewsApiArticle.toNewsArticle(category: NewsCategory): NewsArticle = NewsArticle(
    url = url,
    title = title ?: "",
    description = description,
    sourceName = source?.name ?: "Unknown",
    publishedAt = publishedAt ?: "",
    urlToImage = urlToImage,
    category = category
)
