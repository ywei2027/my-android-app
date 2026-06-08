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
 *
 * @param versionName 版本名，null 降级为 "?.?"
 * @param versionCode 版本号
 * @param buildType 构建类型，空字符串不追加后缀
 */
internal fun formatVersionTag(
    versionName: String? = BuildConfig.VERSION_NAME,
    versionCode: Int = BuildConfig.VERSION_CODE,
    buildType: String = BuildConfig.BUILD_TYPE
): String = buildString {
    val safeName = versionName ?: "?.?"  // P0-D: null 降级
    append("v")
    append(safeName)
    append("(")
    append(versionCode)
    append(")")
    if (buildType.isNotEmpty()) append(buildType)
}

/**
 * 格式化无障碍描述。
 * Debug: "应用版本号 v{name} 构建 {code} 调试版本"
 * Release: "应用版本号 v{name}"
 */
internal fun formatVersionDescription(
    versionName: String? = BuildConfig.VERSION_NAME
): String {
    val safeName = versionName ?: "未知"  // P0-D: null 降级
    return if (BuildConfig.DEBUG) {
        "应用版本号 v$safeName 构建 ${BuildConfig.VERSION_CODE} 调试版本"
    } else {
        "应用版本号 v$safeName"
    }
}

/**
 * 主界面底部版本号标签。
 *
 * Debug 构建显示 "v{VERSION_NAME}({VERSION_CODE}){BUILD_TYPE}"，
 * Release 构建不渲染任何内容。
 *
 * @param modifier 外部传入的 Modifier
 * @param enabled 是否启用渲染。AD-05: splash 已独立显示版本号时可禁用
 */
@Composable
fun VersionTag(
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    // AD-05: 外部可禁用（避免 Debug 双重显示）
    if (!enabled) return

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
            .padding(bottom = 8.dp) // D-20
            .semantics {
                // D-17: TalkBack contentDescription
                contentDescription = formatVersionDescription()
            }
    )
}
