package com.example.myandroidapp.data.remote

import com.google.gson.annotations.SerializedName

data class NewsApiResponse(
    val status: String,
    val totalResults: Int,
    val articles: List<NewsApiArticle>
)

data class NewsApiArticle(
    val title: String?,
    val description: String?,
    val url: String,
    @SerializedName("urlToImage") val urlToImage: String?,
    @SerializedName("publishedAt") val publishedAt: String?,
    @SerializedName("source") val source: NewsApiSource?
)

data class NewsApiSource(
    val name: String?
)
