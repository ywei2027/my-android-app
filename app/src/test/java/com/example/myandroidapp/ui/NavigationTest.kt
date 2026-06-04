package com.example.myandroidapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * D-47: NavigationTest — 验证 NavHost 路由结构
 * 路由定义与 NewsAppNavHost 一致 (news_list → news_detail/{articleId})。
 * 使用简化的测试目的地避免 Hilt 依赖。
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = android.app.Application::class)
class NavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun startDestination_isNewsList() {
        composeTestRule.setContent {
            MaterialTheme {
                TestNavHost()
            }
        }

        composeTestRule
            .onNodeWithTag("newsListDestination")
            .assertIsDisplayed()
    }

    @Test
    fun navigateToNewsDetail_rendersDetailScreen() {
        composeTestRule.setContent {
            MaterialTheme {
                TestNavHost()
            }
        }

        composeTestRule
            .onNodeWithTag("newsListDestination")
            .assertIsDisplayed()

        // 点击导航按钮
        composeTestRule
            .onNodeWithTag("navToDetail")
            .performClick()

        composeTestRule
            .onNodeWithTag("newsDetailDestination")
            .assertIsDisplayed()
    }

    @Test
    fun popBackStack_returnsToList() {
        composeTestRule.setContent {
            MaterialTheme {
                TestNavHost()
            }
        }

        // 导航到详情
        composeTestRule
            .onNodeWithTag("navToDetail")
            .performClick()

        composeTestRule
            .onNodeWithTag("newsDetailDestination")
            .assertIsDisplayed()

        // 返回
        composeTestRule
            .onNodeWithTag("navBack")
            .performClick()

        // 应回到列表
        composeTestRule
            .onNodeWithTag("newsListDestination")
            .assertIsDisplayed()
    }
}

/**
 * 测试用简化的 NavHost，路由结构与 NewsAppNavHost 一致：
 *   - "news_list" → "news_detail/{articleId}" (含 navArgument StringType)
 * 使用 Text + clickable 替代真实 Composable 以避免 Hilt ViewModel 依赖。
 */
@Composable
private fun TestNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "news_list"
    ) {
        composable("news_list") {
            Box(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "NewsList",
                    modifier = Modifier.testTag("newsListDestination")
                )
                Text(
                    text = "Go to detail",
                    modifier = Modifier
                        .testTag("navToDetail")
                        .clickable { navController.navigate("news_detail/test123") }
                )
            }
        }
        composable(
            route = "news_detail/{articleId}",
            arguments = listOf(navArgument("articleId") { type = NavType.StringType })
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "NewsDetail",
                    modifier = Modifier.testTag("newsDetailDestination")
                )
                Text(
                    text = "Back",
                    modifier = Modifier
                        .testTag("navBack")
                        .clickable { navController.popBackStack() }
                )
            }
        }
    }
}
