package com.example.myandroidapp.ui.screen.about

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.myandroidapp.ui.components.formatVersionDescription
import com.example.myandroidapp.ui.components.formatVersionTag

/**
 * 关于页面版本号展示组件。
 *
 * 始终显示版本号（不区分 Debug/Release），格式为 v{name}({code}){buildType}。
 * 文本支持长按选中（通过 [SelectionContainer] 实现）。
 *
 * @param modifier 外部传入的 Modifier
 */
@Composable
fun VersionText(modifier: Modifier = Modifier) {
    val version = formatVersionTag()

    SelectionContainer {
        Text(
            text = version,
            modifier = modifier.semantics {
                contentDescription = formatVersionDescription()
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}
