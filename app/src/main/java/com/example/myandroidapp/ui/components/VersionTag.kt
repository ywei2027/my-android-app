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
 * 格式化无障碍描述为 "应用版本号 v{name}({code})"。
 * D-22: contentDescription 与可见文本保持一致。
 */
internal fun formatVersionDescription(
    versionName: String = BuildConfig.VERSION_NAME,
    versionCode: Int = BuildConfig.VERSION_CODE
): String =
    "应用版本号 v$versionName($versionCode)"

/**
 * 主界面底部版本号标签。
 *
 * Debug 构建显示 "v{VERSION_NAME}({VERSION_CODE}){BUILD_TYPE}"，
 * Release 构建显示 "v{VERSION_NAME}({VERSION_CODE})"（D-21: buildType="" 去掉后缀）。
 *
 * @param modifier 外部传入的 Modifier
 */
@Composable
fun VersionTag(modifier: Modifier = Modifier) {
    // D-21: Release 构建去掉 buildType 后缀，函数签名保持不变
    val versionText = if (BuildConfig.DEBUG) {
        formatVersionTag()
    } else {
        formatVersionTag(buildType = "")
    }

    // D-22: contentDescription 与可见文本一致（含 versionCode）
    val description = if (BuildConfig.DEBUG) {
        formatVersionDescription()
    } else {
        formatVersionDescription()
    }

    Text(
        text = versionText,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant, // D-13
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars) // D-16
            .padding(bottom = 8.dp) // D-20
            .semantics {
                // D-17: TalkBack contentDescription
                contentDescription = description
            }
    )
}
