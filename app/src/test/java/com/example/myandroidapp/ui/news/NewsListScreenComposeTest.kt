package com.example.myandroidapp.ui.news

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.myandroidapp.domain.model.NewsCategory
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = android.app.Application::class)
class NewsListScreenComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun newsListScreen_showsFirstLoadingState() {
        val mockViewModel = mockk<NewsListViewModel>(relaxed = true)
        every { mockViewModel.uiState } returns MutableStateFlow(NewsListUiState.FirstLoading)
        every { mockViewModel.searchQuery } returns MutableStateFlow("")
        every { mockViewModel.isSearchActive } returns MutableStateFlow(false)
        every { mockViewModel.selectedTab } returns MutableStateFlow(NewsCategory.RECOMMENDED)
        every { mockViewModel.searchResults } returns MutableStateFlow(emptyList())

        composeTestRule.setContent {
            MaterialTheme {
                NewsListScreen(
                    onArticleClick = {},
                    viewModel = mockViewModel
                )
            }
        }

        // LazyColumn 存在，首个 ShimmerCard 可见
        // (LazyColumn 仅组合可见项，Robolectric 视口可能只渲染首项)
        composeTestRule
            .onNodeWithTag("newsCardList")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag("shimmerCard_0")
            .assertIsDisplayed()
    }

    @Test
    fun newsListScreen_showsEmptyState() {
        val mockViewModel = mockk<NewsListViewModel>(relaxed = true)
        every { mockViewModel.uiState } returns MutableStateFlow(NewsListUiState.Empty)
        every { mockViewModel.searchQuery } returns MutableStateFlow("")
        every { mockViewModel.isSearchActive } returns MutableStateFlow(false)
        every { mockViewModel.selectedTab } returns MutableStateFlow(NewsCategory.RECOMMENDED)
        every { mockViewModel.searchResults } returns MutableStateFlow(emptyList())

        composeTestRule.setContent {
            MaterialTheme {
                NewsListScreen(
                    onArticleClick = {},
                    viewModel = mockViewModel
                )
            }
        }

        composeTestRule
            .onNodeWithTag("emptyState")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("暂无新闻")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("下拉刷新试试")
            .assertIsDisplayed()
    }

    @Test
    fun newsListScreen_showsErrorState() {
        val mockViewModel = mockk<NewsListViewModel>(relaxed = true)
        every { mockViewModel.uiState } returns MutableStateFlow(
            NewsListUiState.Error("加载失败，请检查网络")
        )
        every { mockViewModel.searchQuery } returns MutableStateFlow("")
        every { mockViewModel.isSearchActive } returns MutableStateFlow(false)
        every { mockViewModel.selectedTab } returns MutableStateFlow(NewsCategory.RECOMMENDED)
        every { mockViewModel.searchResults } returns MutableStateFlow(emptyList())

        composeTestRule.setContent {
            MaterialTheme {
                NewsListScreen(
                    onArticleClick = {},
                    viewModel = mockViewModel
                )
            }
        }

        composeTestRule
            .onNodeWithTag("errorState")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("加载失败，请检查网络")
            .assertIsDisplayed()

        // 重试按钮可见
        composeTestRule
            .onNodeWithTag("errorRetryButton")
            .assertIsDisplayed()
    }

    @Test
    fun newsListScreen_hasSearchBar() {
        val mockViewModel = mockk<NewsListViewModel>(relaxed = true)
        every { mockViewModel.uiState } returns MutableStateFlow(NewsListUiState.FirstLoading)
        every { mockViewModel.searchQuery } returns MutableStateFlow("")
        every { mockViewModel.isSearchActive } returns MutableStateFlow(false)
        every { mockViewModel.selectedTab } returns MutableStateFlow(NewsCategory.RECOMMENDED)
        every { mockViewModel.searchResults } returns MutableStateFlow(emptyList())

        composeTestRule.setContent {
            MaterialTheme {
                NewsListScreen(
                    onArticleClick = {},
                    viewModel = mockViewModel
                )
            }
        }

        composeTestRule
            .onNodeWithTag("searchBar")
            .assertIsDisplayed()
    }
}
