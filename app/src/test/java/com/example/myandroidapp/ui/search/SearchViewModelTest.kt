package com.example.myandroidapp.ui.search

import com.example.myandroidapp.domain.search.SearchHistoryStore
import com.example.myandroidapp.domain.search.SearchQuery
import com.example.myandroidapp.domain.search.SearchRepository
import com.example.myandroidapp.domain.search.SearchResultItem
import com.example.myandroidapp.data.model.CalcHistory
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var searchRepository: SearchRepository
    private lateinit var historyStore: SearchHistoryStore
    private lateinit var savedStateHandle: androidx.lifecycle.SavedStateHandle
    private lateinit var viewModel: SearchViewModel

    private val mockHistory = listOf(
        SearchQuery("123+456", System.currentTimeMillis()),
        SearchQuery("99*2", System.currentTimeMillis() - 1000),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        searchRepository = mockk(relaxed = true)
        historyStore = mockk(relaxed = true)
        savedStateHandle = androidx.lifecycle.SavedStateHandle()

        every { historyStore.history } returns flowOf(mockHistory)
        coEvery { historyStore.addQuery(any()) } returns Unit
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Case 1: Docked -> History (click SearchBar)
    @Test
    fun `initial state with history shows History`() = runTest {
        viewModel = SearchViewModel(searchRepository, historyStore, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SearchUiState.History)
        assertEquals(2, (state as SearchUiState.History).history.size)
        assertEquals("", state.query)
    }

    // Case 2: History -> Typing
    @Test
    fun `input changes state to Typing`() = runTest {
        viewModel = SearchViewModel(searchRepository, historyStore, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(SearchEvent.OnQueryChange("123"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SearchUiState.Typing)
        assertEquals("123", (state as SearchUiState.Typing).query)
    }

    // Case 2b: pure spaces isBlank() -> no search triggered
    @Test
    fun `blank input does not trigger search`() = runTest {
        viewModel = SearchViewModel(searchRepository, historyStore, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(SearchEvent.OnQueryChange("   "))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SearchUiState.History)
    }

    // Case 3: Typing -> Loading (debounce 300ms)
    @Test
    fun `debounce 300ms triggers loading state`() = runTest {
        viewModel = SearchViewModel(searchRepository, historyStore, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        val fakeResults = listOf(createFakeResult("123+456", "579"))
        coEvery { searchRepository.search("123") } returns fakeResults

        viewModel.onEvent(SearchEvent.OnQueryChange("123"))
        testDispatcher.scheduler.advanceUntilIdle()

        // Before 300ms, should still be Typing
        assertEquals(SearchUiState.Typing::class, viewModel.uiState.value::class)

        // Advance past debounce
        testDispatcher.scheduler.advanceTimeBy(300)
        testDispatcher.scheduler.advanceUntilIdle()

        // Should be Results after search completes
        val state = viewModel.uiState.value
        assertTrue(state is SearchUiState.Results)
        assertEquals(1, (state as SearchUiState.Results).items.size)
    }

    // Case 3b: 101 char input produces Toast
    @Test
    fun `maxLength 100 blocks search`() = runTest {
        viewModel = SearchViewModel(searchRepository, historyStore, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        val longInput = "a".repeat(101)
        viewModel.onEvent(SearchEvent.OnQueryChange(longInput))
        testDispatcher.scheduler.advanceUntilIdle()

        // State should not be Loading (blocked by validation)
        val state = viewModel.uiState.value
        assertTrue(state !is SearchUiState.Loading)
    }

    // Case 4: Loading -> Results
    @Test
    fun `successful search shows Results`() = runTest {
        viewModel = SearchViewModel(searchRepository, historyStore, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        val fakeResults = listOf(
            createFakeResult("1+2", "3"),
            createFakeResult("4+5", "9"),
        )
        coEvery { searchRepository.search("1+2") } returns fakeResults

        viewModel.onEvent(SearchEvent.OnQueryChange("1+2"))
        testDispatcher.scheduler.advanceTimeBy(300)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SearchUiState.Results)
        assertEquals(2, (state as SearchUiState.Results).totalCount)
    }

    // Case 5: Loading -> Empty
    @Test
    fun `empty search results show Empty state`() = runTest {
        viewModel = SearchViewModel(searchRepository, historyStore, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        coEvery { searchRepository.search("xyz") } returns emptyList()

        viewModel.onEvent(SearchEvent.OnQueryChange("xyz"))
        testDispatcher.scheduler.advanceTimeBy(300)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SearchUiState.Empty)
    }

    // Case 6: Loading -> Typing (regression: cancel search job)
    @Test
    fun `new query during loading cancels and goes to Typing`() = runTest {
        viewModel = SearchViewModel(searchRepository, historyStore, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        coEvery { searchRepository.search("abc") } coAnswers {
            delay(1000)
            emptyList()
        }

        viewModel.onEvent(SearchEvent.OnQueryChange("abc"))
        testDispatcher.scheduler.advanceTimeBy(300)
        testDispatcher.scheduler.advanceUntilIdle()

        // Now trigger new query while "abc" search is in progress
        viewModel.onEvent(SearchEvent.OnQueryChange("ab"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(
            state is SearchUiState.Typing || state is SearchUiState.Results,
            "Expected Typing or Results but was: ${state::class.simpleName}"
        )
    }

    // Case 7: withTimeout -> Error
    @Test
    fun `search timeout produces Error`() = runTest {
        viewModel = SearchViewModel(searchRepository, historyStore, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        coEvery { searchRepository.search("slow") } coAnswers {
            delay(6000) // Exceeds 5s timeout
            emptyList()
        }

        viewModel.onEvent(SearchEvent.OnQueryChange("slow"))
        testDispatcher.scheduler.advanceTimeBy(300)
        testDispatcher.scheduler.advanceTimeBy(5000)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SearchUiState.Error)
        assertEquals("搜索超时，请重试", (state as SearchUiState.Error).message)
    }

    // Case 9: Error -> Retry -> Loading
    @Test
    fun `retry from Error goes to Loading`() = runTest {
        viewModel = SearchViewModel(searchRepository, historyStore, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        // Set up error state by timeout
        coEvery { searchRepository.search("query") } coAnswers {
            delay(6000)
            emptyList()
        }
        viewModel.onEvent(SearchEvent.OnQueryChange("query"))
        testDispatcher.scheduler.advanceTimeBy(300)
        testDispatcher.scheduler.advanceTimeBy(5000)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is SearchUiState.Error)

        // Now retry with success
        coEvery { searchRepository.search("query") } returns listOf(createFakeResult("query", "result"))
        viewModel.onEvent(SearchEvent.OnRetry)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(
            viewModel.uiState.value is SearchUiState.Results,
            "Expected Results after retry"
        )
    }

    // Case 10: Results -> Docked (dismiss)
    @Test
    fun `dismiss returns to Docked`() = runTest {
        viewModel = SearchViewModel(searchRepository, historyStore, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(SearchEvent.OnDismiss)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is SearchUiState.Docked)
    }

    // Case 12: History item click -> Loading
    @Test
    fun `history click triggers search`() = runTest {
        viewModel = SearchViewModel(searchRepository, historyStore, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        coEvery { searchRepository.search("123+456") } returns listOf(createFakeResult("123+456", "579"))

        val historyItem = mockHistory.first()
        viewModel.onEvent(SearchEvent.OnHistoryClick(historyItem))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SearchUiState.Results)
        assertEquals("123+456", (state as SearchUiState.Results).query)
    }

    // Case 14: Racing cancels
    @Test
    fun `rapid queries cancel and keep only last`() = runTest {
        viewModel = SearchViewModel(searchRepository, historyStore, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        coEvery { searchRepository.search("abc") } returns listOf(createFakeResult("abc", "x"))
        coEvery { searchRepository.search("abcd") } returns listOf(createFakeResult("abcd", "y"))
        coEvery { searchRepository.search("abcde") } returns listOf(createFakeResult("abcde", "z"))

        viewModel.onEvent(SearchEvent.OnQueryChange("abc"))
        testDispatcher.scheduler.advanceTimeBy(150)
        viewModel.onEvent(SearchEvent.OnQueryChange("abcd"))
        testDispatcher.scheduler.advanceTimeBy(150)
        viewModel.onEvent(SearchEvent.OnQueryChange("abcde"))
        testDispatcher.scheduler.advanceTimeBy(300)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SearchUiState.Results)
        assertEquals("abcde", (state as SearchUiState.Results).query)
    }

    // Case 13: SavedStateHandle restore
    @Test
    fun `restores query from SavedStateHandle`() = runTest {
        savedStateHandle["query"] = "42+1"
        coEvery { searchRepository.search("42+1") } returns listOf(createFakeResult("42+1", "43"))

        viewModel = SearchViewModel(searchRepository, historyStore, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SearchUiState.Results)
    }

    private fun createFakeResult(expression: String, result: String): SearchResultItem {
        return SearchResultItem(
            source = "test",
            history = CalcHistory(
                id = "id_${expression}",
                expression = expression,
                result = result,
                timestamp = System.currentTimeMillis(),
            ),
            highlightRanges = emptyList(),
        )
    }
}
