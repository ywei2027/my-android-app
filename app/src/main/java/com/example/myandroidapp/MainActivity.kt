package com.example.myandroidapp

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myandroidapp.ui.auth.LoginScreen
import com.example.myandroidapp.BuildConfig
import com.example.myandroidapp.ui.components.formatVersionTag
import com.example.myandroidapp.ui.components.formatVersionDescription
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

    // 🆕 版本号
    val isDark = isSystemInDarkTheme()
    val versionAlpha = if (isDark) 0.8f else 0.85f   // AD-03: P0-D1 修订暗色 α0.8
    val bgColor = if (isDark) Color(0xFF0D47A1) else Color(0xFF1A73E8)
    val versionTag = formatVersionTag(
        buildType = if (BuildConfig.DEBUG) BuildConfig.BUILD_TYPE else ""
    )
    val versionDesc = formatVersionDescription()

    // Step 1: Logo 弹入（scale + fade）
    LaunchedEffect(Unit) {
        visible = true
        delay(900) // Logo 展示 ~0.9s + 600ms scaleIn = ~1.5s 总时长
        onFinished() // 渐变切换到主内容
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
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

        // 🆕 版本号 — AD-04: 独立 AnimatedVisibility，不参与主内容 fadeOut
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(300, delayMillis = 100))
        ) {
            Text(
                text = versionTag,
                fontSize = 12.sp, // AD-02: hardcode 非 labelSmall
                color = Color.White.copy(alpha = versionAlpha),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(bottom = 32.dp)
                    .semantics { contentDescription = versionDesc }
            )
        }
    }
}

@Composable
fun NewsAppNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("news_list") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
            BackHandler {
                (context as? Activity)?.finish()
            }
        }
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
