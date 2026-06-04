package com.example.myandroidapp.ui.util

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity

/**
 * 检查当前 IME（键盘）是否可见。
 * 通过计算 ime bottom inset > 0 判断，避免使用实验性 isImeVisible API。
 */
@Composable
fun isKeyboardVisible(): Boolean {
    val density = LocalDensity.current
    val imeBottom = with(density) { WindowInsets.ime.getBottom(density) }
    return imeBottom > 0
}
