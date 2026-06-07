package com.example.myandroidapp.domain.model

data class NewsArticle(
    val url: String,
    val title: String,
    val description: String?,
    val sourceName: String,
    val publishedAt: String,
    val urlToImage: String?,
    val category: NewsCategory
)

enum class NewsCategory(val displayName: String, val apiValue: String) {
    RECOMMENDED("推荐", "general"),
    TECHNOLOGY("科技", "technology"),
    BUSINESS("财经", "business"),
    SPORTS("体育", "sports"),
    ENTERTAINMENT("娱乐", "entertainment")
}
