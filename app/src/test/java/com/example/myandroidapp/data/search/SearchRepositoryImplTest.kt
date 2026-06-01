package com.example.myandroidapp.data.search

import com.example.myandroidapp.domain.search.SearchResultItem
import com.example.myandroidapp.domain.search.SearchSource
import com.example.myandroidapp.data.model.CalcHistory
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SearchRepositoryImplTest {

    private lateinit var repository: SearchRepositoryImpl
    private lateinit var source1: SearchSource
    private lateinit var source2: SearchSource

    @Before
    fun setUp() {
        source1 = mockk()
        source2 = mockk()
        repository = SearchRepositoryImpl(setOf(source1, source2))
    }

    @Test
    fun `single source returns its results`() = runTest {
        val repo = SearchRepositoryImpl(setOf(source1))
        val fakeResults = listOf(
            SearchResultItem(
                source = "test",
                history = CalcHistory("1", "1+1", "2", 0L),
                highlightRanges = emptyList(),
            ),
        )
        coEvery { source1.search("1+1") } returns fakeResults

        val results = repo.search("1+1")

        assertEquals(1, results.size)
    }

    @Test
    fun `multiple sources aggregate results`() = runTest {
        val result1 = listOf(
            SearchResultItem(
                source = "s1",
                history = CalcHistory("1", "1+1", "2", 0L),
                highlightRanges = emptyList(),
            ),
        )
        val result2 = listOf(
            SearchResultItem(
                source = "s2",
                history = CalcHistory("2", "2+2", "4", 0L),
                highlightRanges = emptyList(),
            ),
        )
        coEvery { source1.search("test") } returns result1
        coEvery { source2.search("test") } returns result2

        val results = repository.search("test")

        assertEquals(2, results.size)
    }

    @Test
    fun `results capped at 200`() = runTest {
        val manyResults = (1..250).map { i ->
            SearchResultItem(
                source = "test",
                history = CalcHistory("$i", "$i", "${i}", i.toLong()),
                highlightRanges = emptyList(),
            )
        }
        coEvery { source1.search("all") } returns manyResults

        val repo = SearchRepositoryImpl(setOf(source1))
        val results = repo.search("all")

        assertEquals(200, results.size)
    }

    @Test
    fun `empty sources returns empty results`() = runTest {
        val repo = SearchRepositoryImpl(emptySet())

        val results = repo.search("query")

        assertEquals(0, results.size)
    }
}
