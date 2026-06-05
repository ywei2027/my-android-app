package com.example.myandroidapp

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myandroidapp.ui.news.NewsDetailScreen
import com.example.myandroidapp.ui.news.NewsListScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // 系统 SplashScreen — 保持显示直到 Compose splash 就绪
        val splashScreen = installSplashScreen()
        var keepSplash = true
        splashScreen.setKeepOnScreenCondition { keepSplash }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme {
                AppWithAnimatedSplash(
                    onSplashReady = { keepSplash = false }
                )
            }
        }
    }
}

@Composable
fun AppWithAnimatedSplash(onSplashReady: () -> Unit) {
    var showSplash by remember { mutableStateOf(true) }

    // 通知系统 SplashScreen 可以关闭了
    LaunchedEffect(Unit) {
        onSplashReady()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 主内容（先渲染，等 splash 动画结束后显示）
        AnimatedVisibility(
            visible = !showSplash,
            enter = fadeIn(animationSpec = tween(400, delayMillis = 200)),
            modifier = Modifier.fillMaxSize()
        ) {
            NewsAppNavHost()
        }

        // 自定义动画 Splash
        AnimatedVisibility(
            visible = showSplash,
            exit = fadeOut(animationSpec = tween(500))
        ) {
            AnimatedSplashContent(
                onFinished = { showSplash = false }
            )
        }
    }
}

@Composable
fun AnimatedSplashContent(onFinished: () -> Unit) {
    var visible by remember { mutableStateOf(false) }

    // Step 1: Logo 弹入（scale + fade）
    LaunchedEffect(Unit) {
        visible = true
        delay(1200) // Logo 展示 1.2s
        onFinished() // 渐变切换到主内容
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A73E8)),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(
                initialScale = 0.3f,
                animationSpec = tween(600)
            ) + fadeIn(animationSpec = tween(600))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 报纸图标
                Text(
                    text = "📰",
                    fontSize = 72.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "新闻",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "热点资讯 一键掌握",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun NewsAppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "news_list"
    ) {
        composable("news_list") {
            NewsListScreen(
                onArticleClick = { articleId ->
                    val encoded = Uri.encode(articleId)
                    navController.navigate("news_detail/$encoded")
                }
            )
        }
        composable(
            route = "news_detail/{articleId}",
            arguments = listOf(navArgument("articleId") { type = NavType.StringType }))
        { backStackEntry ->
            val articleId = backStackEntry.arguments?.getString("articleId") ?: ""
            NewsDetailScreen(
                articleId = articleId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
