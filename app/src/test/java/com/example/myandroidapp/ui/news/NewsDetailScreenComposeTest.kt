package com.example.myandroidapp.ui.news

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.myandroidapp.domain.model.NewsArticle
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
class NewsDetailScreenComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleArticle = NewsArticle(
        url = "https://example.com/news/1",
        title = "详情页测试标题",
        description = "这是一条测试新闻的详细描述",
        sourceName = "测试来源",
        publishedAt = "2024-01-01",
        urlToImage = null,
        category = NewsCategory.TECHNOLOGY
    )

    @Test
    fun newsDetailScreen_showsLoadingState() {
        val mockViewModel = mockk<NewsDetailViewModel>(relaxed = true)
        every { mockViewModel.uiState } returns MutableStateFlow(DetailUiState.Loading)

        composeTestRule.setContent {
            MaterialTheme {
                NewsDetailScreen(
                    articleId = "https://example.com/news/1",
                    onBack = {},
                    viewModel = mockViewModel
                )
            }
        }

        // ShimmerCard 可见
        composeTestRule
            .onNodeWithTag("shimmerDetail")
            .assertIsDisplayed()
    }

    @Test
    fun newsDetailScreen_showsSuccessState() {
        val mockViewModel = mockk<NewsDetailViewModel>(relaxed = true)
        every { mockViewModel.uiState } returns MutableStateFlow(
            DetailUiState.Success(sampleArticle)
        )

        composeTestRule.setContent {
            MaterialTheme {
                NewsDetailScreen(
                    articleId = "https://example.com/news/1",
                    onBack = {},
                    viewModel = mockViewModel
                )
            }
        }

        // 标题可见
        composeTestRule
            .onNodeWithText("详情页测试标题")
            .assertIsDisplayed()

        // 来源可见
        composeTestRule
            .onNodeWithText("测试来源")
            .assertIsDisplayed()

        // 阅读原文按钮可见
        composeTestRule
            .onNodeWithTag("readOriginalButton")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("阅读原文")
            .assertIsDisplayed()
    }

    @Test
    fun newsDetailScreen_showsErrorState() {
        val mockViewModel = mockk<NewsDetailViewModel>(relaxed = true)
        every { mockViewModel.uiState } returns MutableStateFlow(
            DetailUiState.Error("文章未找到，请返回重试")
        )

        composeTestRule.setContent {
            MaterialTheme {
                NewsDetailScreen(
                    articleId = "https://example.com/news/1",
                    onBack = {},
                    viewModel = mockViewModel
                )
            }
        }

        // 错误信息可见
        composeTestRule
            .onNodeWithText("文章未找到，请返回重试")
            .assertIsDisplayed()

        // 重试按钮可见
        composeTestRule
            .onNodeWithTag("errorRetryButton")
            .assertIsDisplayed()
    }

    @Test
    fun newsDetailScreen_hasBackButton() {
        val mockViewModel = mockk<NewsDetailViewModel>(relaxed = true)
        every { mockViewModel.uiState } returns MutableStateFlow(DetailUiState.Loading)

        composeTestRule.setContent {
            MaterialTheme {
                NewsDetailScreen(
                    articleId = "https://example.com/news/1",
                    onBack = {},
                    viewModel = mockViewModel
                )
            }
        }

        composeTestRule
            .onNodeWithTag("detailBackButton")
            .assertIsDisplayed()
    }
}
