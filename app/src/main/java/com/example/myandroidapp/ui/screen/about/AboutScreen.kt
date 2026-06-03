package com.example.myandroidapp.ui.screen.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 关于页面。
 * 展示应用图标、名称、描述、版本号及四个信息项。
 *
 * @param onBack 返回回调，通常为 navController.popBackStack()
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("关于") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Spacer 32dp
            Spacer(modifier = Modifier.height(32.dp))

            // 2. AppIcon
            Icon(
                imageVector = Icons.Filled.Android,
                contentDescription = "应用图标",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            // 3. Spacer 16dp
            Spacer(modifier = Modifier.height(16.dp))

            // 4. AppName
            Text(
                text = "我的应用",
                style = MaterialTheme.typography.headlineMedium
            )

            // 5. Spacer 4dp
            Spacer(modifier = Modifier.height(4.dp))

            // 6. AppDescription
            Text(
                text = "智能笔记助手",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 7. Spacer 24dp
            Spacer(modifier = Modifier.height(24.dp))

            // 8. Divider
            Divider()

            // 9. Spacer 16dp
            Spacer(modifier = Modifier.height(16.dp))

            // 10. VersionText
            VersionText()

            // 11. Spacer 16dp
            Spacer(modifier = Modifier.height(16.dp))

            // 12. Divider
            Divider()

            // 13. Spacer 16dp
            Spacer(modifier = Modifier.height(16.dp))

            // 14-17. InfoItem x 4
            InfoItem("关于我们") { /* TODO: 跳转关于我们页面 */ }
            InfoItem("用户协议") { /* TODO: 跳转用户协议页面 */ }
            InfoItem("隐私政策") { /* TODO: 跳转隐私政策页面 */ }
            InfoItem("开源许可") { /* TODO: 跳转开源许可页面 */ }
        }
    }
}
