package com.example.myandroidapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myandroidapp.data.local.LoginStateManager
import com.example.myandroidapp.ui.home.HomeScreen
import com.example.myandroidapp.ui.home.HomeViewModel
import com.example.myandroidapp.ui.login.LoginScreen

/**
 * 导航路由常量。
 */
object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
}

/**
 * 根导航图。
 *
 * 启动时根据 DataStore 中的登录状态决定初始页面：
 * - 已登录 → 首页
 * - 未登录 → 登录页
 */
@Composable
fun AppNavGraph(
    loginStateManager: LoginStateManager
) {
    val navController = rememberNavController()

    // 读取持久化的登录状态
    val isLoggedIn by loginStateManager.isLoggedIn.collectAsState(initial = null)

    // 登录状态未加载完成时显示空白（极短瞬间）
    if (isLoggedIn == null) return

    val startDestination = if (isLoggedIn == true) Routes.HOME else Routes.LOGIN

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        // 清除登录页回退栈，用户不能返回到登录页
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen()
        }
    }
}
