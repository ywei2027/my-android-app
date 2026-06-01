package com.example.myandroidapp.domain.search

import com.example.myandroidapp.data.CalculatorHistoryDataSource
import com.example.myandroidapp.data.model.CalcHistory
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalculatorHistorySearchSource @Inject constructor(
    private val historyDataSource: CalculatorHistoryDataSource,
) : SearchSource {

    override val name: String = "calculator_history"

    override suspend fun search(query: String): List<SearchResultItem> {
        val allHistory = historyDataSource.allHistory.first()
        val normalizedQuery = query.trim().lowercase()

        return allHistory
            .mapNotNull { calc ->
                val combined = "${calc.expression}${calc.result}"
                val ranges = highlightRanges(normalizedQuery, combined)
                if (ranges.isEmpty()) null
                else SearchResultItem(
                    source = name,
                    history = calc,
                    highlightRanges = ranges.coerceWithinExpression(calc.expression),
                )
            }
            .take(200)
    }

    private fun highlightRanges(query: String, text: String): List<IntRange> {
        val lower = text.lowercase()
        val result = mutableListOf<IntRange>()
        var start = 0
        while (true) {
            start = lower.indexOf(query, start)
            if (start < 0) break
            result.add(start until (start + query.length))
            start += query.length
        }
        return result
    }

    private fun List<IntRange>.coerceWithinExpression(expression: String): List<IntRange> =
        map { range ->
            val start = range.first.coerceIn(0, expression.length)
            val end = range.last.coerceIn(0, expression.length)
            start..end
        }.distinct()
}
