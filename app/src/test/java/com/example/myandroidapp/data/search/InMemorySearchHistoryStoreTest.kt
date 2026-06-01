package com.example.myandroidapp.data.search

import com.example.myandroidapp.domain.search.SearchQuery
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InMemorySearchHistoryStoreTest {

    private lateinit var store: InMemorySearchHistoryStore

    @Before
    fun setUp() {
        store = InMemorySearchHistoryStore()
    }

    @Test
    fun `add and retrieve history`() = runTest {
        store.addQuery("123")
        store.addQuery("456")

        val history = store.history.first()

        assertEquals(2, history.size)
        assertEquals("456", history[0].query) // newest first
        assertEquals("123", history[1].query)
    }

    @Test
    fun `deduplication moves to top`() = runTest {
        store.addQuery("abc")
        store.addQuery("def")
        store.addQuery("abc") // duplicate

        val history = store.history.first()

        assertEquals(2, history.size)
        assertEquals("abc", history[0].query) // moved to top
    }

    @Test
    fun `trim whitespace from query`() = runTest {
        store.addQuery("  123  ")

        val history = store.history.first()

        assertEquals(1, history.size)
        assertEquals("123", history[0].query)
    }

    @Test
    fun `FIFO eviction at 6 items`() = runTest {
        store.addQuery("a")
        store.addQuery("b")
        store.addQuery("c")
        store.addQuery("d")
        store.addQuery("e")
        store.addQuery("f") // should evict "a"

        val history = store.history.first()

        assertEquals(5, history.size)
        assertEquals("f", history[0].query)
        // "a" should be removed
        assertTrue(history.none { it.query == "a" })
    }

    @Test
    fun `remove specific query`() = runTest {
        store.addQuery("keep")
        store.addQuery("remove")
        store.removeQuery("remove")

        val history = store.history.first()

        assertEquals(1, history.size)
        assertEquals("keep", history[0].query)
    }

    @Test
    fun `clearAll empties history`() = runTest {
        store.addQuery("a")
        store.addQuery("b")
        store.clearAll()

        val history = store.history.first()

        assertEquals(0, history.size)
    }

    @Test
    fun `case insensitive deduplication`() = runTest {
        store.addQuery("ABC")
        store.addQuery("abc")

        val history = store.history.first()

        assertEquals(1, history.size)
    }
}
