package com.example.myandroidapp.domain.search

import com.example.myandroidapp.data.model.CalcHistory

data class SearchResultItem(
    val source: String,
    val history: CalcHistory,
    val highlightRanges: List<IntRange>,
)
