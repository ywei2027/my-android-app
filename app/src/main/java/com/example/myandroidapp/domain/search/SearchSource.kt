package com.example.myandroidapp.domain.search

interface SearchSource {
    val name: String

    suspend fun search(query: String): List<SearchResultItem>
}
