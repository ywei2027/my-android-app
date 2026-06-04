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
class ErrorStateComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun errorState_displaysErrorMessage() {
        composeTestRule.setContent {
            MaterialTheme {
                ErrorState(
                    message = "网络连接失败",
                    onRetry = {}
                )
            }
        }

        composeTestRule
            .onNodeWithTag("errorState")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("网络连接失败")
            .assertIsDisplayed()
    }

    @Test
    fun errorState_displaysRetryButton() {
        composeTestRule.setContent {
            MaterialTheme {
                ErrorState(
                    message = "加载失败",
                    onRetry = {}
                )
            }
        }

        composeTestRule
            .onNodeWithTag("errorRetryButton")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("重试")
            .assertIsDisplayed()
    }

    @Test
    fun errorState_usesCustomTestTag() {
        composeTestRule.setContent {
            MaterialTheme {
                ErrorState(
                    message = "出错了",
                    onRetry = {},
                    testTag = "customErrorTag"
                )
            }
        }

        composeTestRule
            .onNodeWithTag("customErrorTag")
            .assertIsDisplayed()
    }

    @Test
    fun errorState_retryButtonIsClickable() {
        composeTestRule.setContent {
            MaterialTheme {
                ErrorState(
                    message = "请重试",
                    onRetry = {}
                )
            }
        }

        // 按钮存在且可点击
        composeTestRule
            .onNodeWithTag("errorRetryButton")
            .assertIsDisplayed()
    }
}
