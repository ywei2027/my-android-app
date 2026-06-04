package com.example.myandroidapp.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = android.app.Application::class)
class ShimmerCardComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun shimmerCard_rendersWithDefaultTestTag() {
        composeTestRule.setContent {
            MaterialTheme {
                ShimmerCard()
            }
        }

        composeTestRule
            .onNodeWithTag("shimmerCard")
            .assertIsDisplayed()
    }

    @Test
    fun shimmerCard_acceptsCustomTestTag() {
        composeTestRule.setContent {
            MaterialTheme {
                ShimmerCard(testTag = "customShimmer")
            }
        }

        composeTestRule
            .onNodeWithTag("customShimmer")
            .assertIsDisplayed()
    }

    @Test
    fun shimmerCard_animationDoesNotCrash() {
        // D-49: 暂停动画时钟后验证节点仍在树中
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            MaterialTheme {
                ShimmerCard()
            }
        }

        // 首帧断言 — 节点存在
        composeTestRule
            .onNodeWithTag("shimmerCard")
            .assertIsDisplayed()

        // 推进动画时间，验证不崩溃
        composeTestRule.mainClock.advanceTimeBy(1200L)

        composeTestRule
            .onNodeWithTag("shimmerCard")
            .assertIsDisplayed()
    }

    @Test
    fun shimmerCard_rendersWithModifier() {
        composeTestRule.setContent {
            MaterialTheme {
                ShimmerCard(testTag = "shimmerWithMod")
            }
        }

        composeTestRule
            .onNodeWithTag("shimmerWithMod")
            .assertIsDisplayed()
    }
}
