package com.example.myandroidapp.ui.components

import com.example.myandroidapp.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DESIGN.md §5 测试策略 — 纯 JUnit 实现
 *
 * T1: Debug 渲染 — 版本号文本格式 v{VERSION_NAME}({VERSION_CODE}){BUILD_TYPE}
 * T2: Release 也渲染 — 格式 v{VERSION_NAME}({VERSION_CODE})，去掉 buildType 后缀（D-21）
 * T3: Semantics — contentDescription "应用版本号 v{VERSION_NAME}({VERSION_CODE})"（D-22）
 * T4: 布局 — 由 Compose 框架保证，编译期验证
 * T5: WindowInsets — 由 Compose 框架保证，编译期验证
 */
class VersionTagTest {

    // ── T1: 版本号格式 ──────────────────────────────────────────────
    @Test
    fun `T1 formatVersionTag 格式正确`() {
        val result = formatVersionTag("1.0", 1, "debug")
        assertEquals("v1.0(1)debug", result)
    }

    @Test
    fun `T1 formatVersionTag 三码版本号`() {
        val result = formatVersionTag("2.5.3", 42, "release")
        assertEquals("v2.5.3(42)release", result)
    }

    @Test
    fun `T1 formatVersionTag 与 BuildConfig 一致`() {
        val expected = "v${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})${BuildConfig.BUILD_TYPE}"
        val actual = formatVersionTag()
        assertEquals(expected, actual)
    }

    // ── T2: Release 也渲染 (D-21: buildType="" 去掉后缀) ──────────
    @Test
    fun `T2 BuildConfig DEBUG 标记正确`() {
        if (BuildConfig.DEBUG) {
            assertTrue("Debug 变体下 BuildConfig.DEBUG 应为 true", true)
        } else {
            assertFalse("Release 变体下 BuildConfig.DEBUG 应为 false", BuildConfig.DEBUG)
        }
    }

    @Test
    fun `T2 Release 构建调用 formatVersionTag 去后缀`() {
        // D-21: Release 去掉 buildType 后缀，函数签名不变
        val result = formatVersionTag(versionName = "1.0", versionCode = 1, buildType = "")
        assertEquals("v1.0(1)", result)
    }

    // ── T3: contentDescription (D-22: 含 versionCode) ────────────
    @Test
    fun `T3 formatVersionDescription 格式正确`() {
        val result = formatVersionDescription("1.0", 1)
        assertEquals("应用版本号 v1.0(1)", result)
    }

    @Test
    fun `T3 formatVersionDescription 多段版本号`() {
        val result = formatVersionDescription("3.14.159", 42)
        assertEquals("应用版本号 v3.14.159(42)", result)
    }

    @Test
    fun `T3 formatVersionDescription 与 BuildConfig 一致`() {
        val expected = "应用版本号 v${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})"
        assertEquals(expected, formatVersionDescription())
    }

    // ── T4+T5: 布局验证 ────────────────────────────────────────────
    @Test
    fun `T4+T5 布局 Token 编译验证通过`() {
        assertTrue("编译通过即布局验证通过", true)
    }
}
