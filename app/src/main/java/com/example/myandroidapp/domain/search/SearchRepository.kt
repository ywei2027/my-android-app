package com.example.myandroidapp.domain.search

interface SearchRepository {
    suspend fun search(query: String): List<SearchResultItem>
}
