package com.example.myandroidapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import com.example.myandroidapp.data.local.LoginStateManager
import com.example.myandroidapp.navigation.AppNavGraph
import com.example.myandroidapp.ui.theme.MyAndroidAppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 主 Activity。
 *
 * 职责：
 * - 应用入口，设置 Compose 内容
 * - 根据登录状态决定显示的页面
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var loginStateManager: LoginStateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyAndroidAppTheme {
                AppNavGraph(loginStateManager = loginStateManager)
            }
        }
    }
}
