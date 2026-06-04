package com.example.myandroidapp.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = android.app.Application::class)
class EmptyStateComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyState_displaysDefaultTitle() {
        composeTestRule.setContent {
            MaterialTheme {
                EmptyState()
            }
        }

        composeTestRule
            .onNodeWithTag("emptyState")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("暂无内容")
            .assertIsDisplayed()
    }

    @Test
    fun emptyState_displaysCustomTitleAndSubtitle() {
        composeTestRule.setContent {
            MaterialTheme {
                EmptyState(
                    title = "自定义标题",
                    subtitle = "自定义副标题"
                )
            }
        }

        composeTestRule
            .onNodeWithTag("emptyState")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("自定义标题")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("自定义副标题")
            .assertIsDisplayed()
    }

    @Test
    fun emptyState_usesCustomTestTag() {
        composeTestRule.setContent {
            MaterialTheme {
                EmptyState(testTag = "customEmptyTag")
            }
        }

        composeTestRule
            .onNodeWithTag("customEmptyTag")
            .assertIsDisplayed()
    }

    @Test
    fun emptyState_doesNotDisplaySubtitleWhenEmpty() {
        composeTestRule.setContent {
            MaterialTheme {
                EmptyState(title = "仅标题")
            }
        }

        composeTestRule
            .onNodeWithText("仅标题")
            .assertIsDisplayed()

        // 副标题为空时不渲染
        composeTestRule
            .onNodeWithTag("emptyState")
            .assertIsDisplayed()
    }
}
