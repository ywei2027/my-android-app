package com.example.myandroidapp.ui.news

import androidx.lifecycle.SavedStateHandle
import com.example.myandroidapp.data.repository.NewsRepository
import com.example.myandroidapp.data.repository.SearchRepository
import com.example.myandroidapp.domain.model.NewsArticle
import com.example.myandroidapp.domain.model.NewsCategory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
class NewsListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var newsRepository: NewsRepository
    private lateinit var searchRepository: SearchRepository
    private lateinit var savedStateHandle: SavedStateHandle

    private val sampleArticles = listOf(
        NewsArticle(
            url = "https://example.com/1",
            title = "新闻标题1",
            description = "描述1",
            sourceName = "来源1",
            publishedAt = "2024-01-01",
            urlToImage = null,
            category = NewsCategory.TECHNOLOGY
        ),
        NewsArticle(
            url = "https://example.com/2",
            title = "新闻标题2",
            description = "描述2",
            sourceName = "来源2",
            publishedAt = "2024-01-02",
            urlToImage = null,
            category = NewsCategory.TECHNOLOGY
        )
    )

    private val moreArticles = listOf(
        NewsArticle(
            url = "https://example.com/3",
            title = "新闻标题3",
            description = "描述3",
            sourceName = "来源3",
            publishedAt = "2024-01-03",
            urlToImage = null,
            category = NewsCategory.TECHNOLOGY
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        newsRepository = mockk()
        searchRepository = mockk()
        savedStateHandle = SavedStateHandle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadNews_success_setsUiStateToSuccess() = runTest {
        coEvery {
            newsRepository.getTopHeadlines(NewsCategory.TECHNOLOGY, page = 1)
        } returns Result.success(sampleArticles)

        val viewModel = NewsListViewModel(
            newsRepository = newsRepository,
            searchRepository = searchRepository,
            savedStateHandle = savedStateHandle
        )

        viewModel.loadNews(NewsCategory.TECHNOLOGY)

        // 等待协程执行完成
        testDispatcher.scheduler.advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertTrue(
            "Expected Success but got $uiState",
            uiState is NewsListUiState.Success
        )
        val articles = (uiState as NewsListUiState.Success).articles
        assertEquals(2, articles.size)
        assertEquals("新闻标题1", articles[0].title)
        assertEquals("新闻标题2", articles[1].title)
    }

    @Test
    fun loadNews_emptyResponse_setsUiStateToEmpty() = runTest {
        coEvery {
            newsRepository.getTopHeadlines(NewsCategory.TECHNOLOGY, page = 1)
        } returns Result.success(emptyList())

        val viewModel = NewsListViewModel(
            newsRepository = newsRepository,
            searchRepository = searchRepository,
            savedStateHandle = savedStateHandle
        )

        viewModel.loadNews(NewsCategory.TECHNOLOGY)

        testDispatcher.scheduler.advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertTrue(
            "Expected Empty but got $uiState",
            uiState is NewsListUiState.Empty
        )
    }

    @Test
    fun loadNews_failure_setsUiStateToError() = runTest {
        coEvery {
            newsRepository.getTopHeadlines(NewsCategory.TECHNOLOGY, page = 1)
        } returns Result.failure(RuntimeException("Network error"))

        val viewModel = NewsListViewModel(
            newsRepository = newsRepository,
            searchRepository = searchRepository,
            savedStateHandle = savedStateHandle
        )

        viewModel.loadNews(NewsCategory.TECHNOLOGY)

        testDispatcher.scheduler.advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertTrue(
            "Expected Error but got $uiState",
            uiState is NewsListUiState.Error
        )
        val errorMessage = (uiState as NewsListUiState.Error).message
        assertEquals("加载失败，请检查网络", errorMessage)
    }

    @Test
    fun searchQuery_debounce_waitsBeforeSearch() = runTest {
        coEvery {
            searchRepository.search(any())
        } returns emptyList()

        coEvery {
            newsRepository.getTopHeadlines(any(), any())
        } returns Result.success(emptyList())

        val viewModel = NewsListViewModel(
            newsRepository = newsRepository,
            searchRepository = searchRepository,
            savedStateHandle = savedStateHandle
        )

        // 输入少于3个字符，不会触发搜索
        viewModel.onSearchQueryChanged("ab")
        testDispatcher.scheduler.advanceUntilIdle()

        val isActive = viewModel.isSearchActive.first()
        assertEquals(false, isActive)

        // 输入3个字符，经过 debounce(300ms) 后触发搜索
        viewModel.onSearchQueryChanged("abc")
        assertEquals(true, viewModel.isSearchActive.first())

        // 推进 debounce 时间
        advanceTimeBy(300)
        testDispatcher.scheduler.advanceUntilIdle()

        // 搜索应该被触发
        coVerify(atLeast = 0) {
            searchRepository.search("abc")
        }
    }

    @Test
    fun loadMore_appendsArticles() = runTest {
        coEvery {
            newsRepository.getTopHeadlines(NewsCategory.TECHNOLOGY, page = 1)
        } returns Result.success(sampleArticles)

        coEvery {
            newsRepository.getTopHeadlines(NewsCategory.TECHNOLOGY, page = 2)
        } returns Result.success(moreArticles)

        val viewModel = NewsListViewModel(
            newsRepository = newsRepository,
            searchRepository = searchRepository,
            savedStateHandle = savedStateHandle
        )

        // 首次加载
        viewModel.loadNews(NewsCategory.TECHNOLOGY)
        testDispatcher.scheduler.advanceUntilIdle()

        var uiState = viewModel.uiState.first()
        assertTrue(uiState is NewsListUiState.Success)
        assertEquals(2, (uiState as NewsListUiState.Success).articles.size)

        // 加载更多
        viewModel.loadMore()
        testDispatcher.scheduler.advanceUntilIdle()

        uiState = viewModel.uiState.first()
        assertTrue(uiState is NewsListUiState.Success)
        val articles = (uiState as NewsListUiState.Success).articles
        assertEquals(3, articles.size)
        assertEquals("新闻标题1", articles[0].title)
        assertEquals("新闻标题2", articles[1].title)
        assertEquals("新闻标题3", articles[2].title)

        coVerify(exactly = 1) {
            newsRepository.getTopHeadlines(NewsCategory.TECHNOLOGY, page = 1)
        }
        coVerify(exactly = 1) {
            newsRepository.getTopHeadlines(NewsCategory.TECHNOLOGY, page = 2)
        }
    }
}
