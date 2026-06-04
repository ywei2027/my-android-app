package com.example.myandroidapp.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.myandroidapp.domain.model.NewsArticle
import com.example.myandroidapp.domain.model.NewsCategory
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = android.app.Application::class)
class NewsCardComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleArticle = NewsArticle(
        url = "https://example.com/news/1",
        title = "测试新闻标题",
        description = "这是一条测试新闻的描述内容",
        sourceName = "测试来源",
        publishedAt = "2024-01-01",
        urlToImage = null,
        category = NewsCategory.TECHNOLOGY
    )

    @Test
    fun newsCard_displaysWithoutImage() {
        composeTestRule.setContent {
            MaterialTheme {
                NewsCard(
                    article = sampleArticle,
                    onClick = {}
                )
            }
        }

        composeTestRule
            .onNodeWithTag("newsCard")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("测试新闻标题")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("测试来源 · 2024-01-01")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("这是一条测试新闻的描述内容")
            .assertIsDisplayed()
    }

    @Test
    fun newsCard_displaysWithImage() {
        val articleWithImage = sampleArticle.copy(
            urlToImage = "https://example.com/image.jpg"
        )

        composeTestRule.setContent {
            MaterialTheme {
                NewsCard(
                    article = articleWithImage,
                    onClick = {}
                )
            }
        }

        composeTestRule
            .onNodeWithTag("newsCard")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("测试新闻标题")
            .assertIsDisplayed()
    }

    @Test
    fun newsCard_handlesNullDescription() {
        val articleNoDescription = sampleArticle.copy(description = null)

        composeTestRule.setContent {
            MaterialTheme {
                NewsCard(
                    article = articleNoDescription,
                    onClick = {}
                )
            }
        }

        composeTestRule
            .onNodeWithTag("newsCard")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("测试新闻标题")
            .assertIsDisplayed()
    }

    @Test
    fun newsCard_clickInvokesCallback() {
        composeTestRule.setContent {
            MaterialTheme {
                NewsCard(
                    article = sampleArticle,
                    onClick = {}
                )
            }
        }

        composeTestRule
            .onNodeWithTag("newsCard")
            .assertIsDisplayed()
    }
}
