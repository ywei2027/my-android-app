package com.example.myandroidapp.domain.search

import com.example.myandroidapp.data.model.CalcHistory
import com.example.myandroidapp.data.CalculatorHistoryDataSource
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CalculatorHistorySearchSourceTest {

    private lateinit var dataSource: CalculatorHistoryDataSource
    private lateinit var searchSource: CalculatorHistorySearchSource

    private val sampleHistory = listOf(
        CalcHistory(id = "1", expression = "123 + 456", result = "579", timestamp = 1000L),
        CalcHistory(id = "2", expression = "99 * 2", result = "198", timestamp = 2000L),
        CalcHistory(id = "3", expression = "HELLO", result = "WORLD", timestamp = 3000L),
    )

    @Before
    fun setUp() {
        dataSource = mockk()
        searchSource = CalculatorHistorySearchSource(dataSource)
    }

    @Test
    fun `matches expression field`() = runTest {
        coEvery { dataSource.allHistory } returns flowOf(sampleHistory)

        val results = searchSource.search("123")

        assertEquals(1, results.size)
        assertEquals("123 + 456", results[0].history.expression)
        assertTrue(results[0].highlightRanges.isNotEmpty())
    }

    @Test
    fun `matches result field`() = runTest {
        coEvery { dataSource.allHistory } returns flowOf(sampleHistory)

        val results = searchSource.search("579")

        assertEquals(1, results.size)
        assertEquals("579", results[0].history.result)
    }

    @Test
    fun `case insensitive matching`() = runTest {
        coEvery { dataSource.allHistory } returns flowOf(sampleHistory)

        val results = searchSource.search("hello")

        assertEquals(1, results.size)
    }

    @Test
    fun `empty query returns empty results`() = runTest {
        coEvery { dataSource.allHistory } returns flowOf(sampleHistory)

        val results = searchSource.search("")

        assertEquals(0, results.size)
    }

    @Test
    fun `highlightRanges computed correctly`() = runTest {
        coEvery { dataSource.allHistory } returns flowOf(sampleHistory)

        val results = searchSource.search("123")

        assertEquals(1, results.size)
        val ranges = results[0].highlightRanges
        assertTrue(ranges.isNotEmpty())
        // Verify ranges are within bounds of expression
        for (range in ranges) {
            assertTrue(range.first >= 0)
            assertTrue(range.last < results[0].history.expression.length)
        }
    }

    @Test
    fun `searchSource name is set`() {
        assertEquals("calculator_history", searchSource.name)
    }

    @Test
    fun `no match returns empty list`() = runTest {
        coEvery { dataSource.allHistory } returns flowOf(sampleHistory)

        val results = searchSource.search("zzzzz")

        assertEquals(0, results.size)
    }
}
