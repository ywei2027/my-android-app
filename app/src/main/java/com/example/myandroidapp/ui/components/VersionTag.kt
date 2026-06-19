package com.example.myandroidapp.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myandroidapp.BuildConfig

/**
 * 格式化版本号字符串为 "v{name}({code}){buildType}"。
 * 提取为独立函数以支持单元测试。
 */
internal fun formatVersionTag(
    versionName: String = BuildConfig.VERSION_NAME,
    versionCode: Int = BuildConfig.VERSION_CODE,
    buildType: String = BuildConfig.BUILD_TYPE
): String = "v$versionName($versionCode)$buildType"

/**
 * 格式化无障碍描述为 "应用版本号 v{name}"。
 */
internal fun formatVersionDescription(versionName: String = BuildConfig.VERSION_NAME): String =
    "应用版本号 v$versionName"

/**
 * 主界面底部版本号标签。
 *
 * Debug 构建显示 "v{VERSION_NAME}({VERSION_CODE}){BUILD_TYPE}"，
 * Release 构建不渲染任何内容。
 *
 * @param modifier 外部传入的 Modifier
 */
@Composable
fun VersionTag(modifier: Modifier = Modifier) {
    // D-12: Release 构建不显示
    if (!BuildConfig.DEBUG) return

    // D-15: v{name}({code}){buildType}
    val versionText = formatVersionTag()

    Text(
        text = versionText,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant, // D-13
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars) // D-16
            .padding(bottom = 12.dp) // D-20: 调整底部间距
            .semantics {
                // D-17: TalkBack contentDescription
                contentDescription = formatVersionDescription()
            }
    )
}
