package com.example.myandroidapp.ui.search

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

fun buildHighlightedText(
    text: String,
    query: String,
    ranges: List<IntRange> = emptyList(),
): AnnotatedString {
    if (query.isBlank() || text.isBlank()) {
        return AnnotatedString(text)
    }

    val highlightColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)

    val normalizedText = text.lowercase()
    val normalizedQuery = query.trim().lowercase()

    val computedRanges = if (ranges.isNotEmpty()) {
        ranges.map { range ->
            val start = range.first.coerceIn(0, text.length)
            val end = range.last.coerceIn(0, text.length)
            start..end
        }.distinct()
    } else {
        val result = mutableListOf<IntRange>()
        var start = 0
        while (true) {
            start = normalizedText.indexOf(normalizedQuery, start)
            if (start < 0) break
            result.add(start until (start + normalizedQuery.length))
            start += normalizedQuery.length
        }
        result
    }

    return buildAnnotatedString {
        append(text)
        for (range in computedRanges) {
            addStyle(
                SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    background = highlightColor,
                ),
                range.first,
                range.last + 1,
            )
        }
    }
}
