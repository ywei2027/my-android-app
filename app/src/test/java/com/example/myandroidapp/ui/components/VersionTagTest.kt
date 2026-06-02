package com.example.myandroidapp.ui.components

import com.example.myandroidapp.BuildConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * VersionTag 编译期常量验证 — 对齐 DESIGN.md §5 测试策略。
 *
 * T1: 版本号格式验证 (v{name}({code}){buildType})
 * T2: BuildConfig 常量非空验证
 * T3: DEBUG 标志正确性
 */
class VersionTagTest {

    // ── T1: 版本号拼接格式 ──
    @Test
    fun t1_versionStringFormat() {
        val versionText = "v${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})${BuildConfig.BUILD_TYPE}"
        // 格式: v1.0(1)debug
        assertTrue("Version text should start with 'v'", versionText.startsWith("v"))
        assertTrue("Version text should contain VERSION_NAME", versionText.contains(BuildConfig.VERSION_NAME))
        assertTrue("Version text should contain VERSION_CODE", versionText.contains(BuildConfig.VERSION_CODE.toString()))
        assertTrue("Version text should contain BUILD_TYPE", versionText.contains(BuildConfig.BUILD_TYPE))
    }

    // ── T2: BuildConfig 常量非空 ──
    @Test
    fun t2_buildConfigConstantsNotEmpty() {
        assertFalse("VERSION_NAME should not be empty", BuildConfig.VERSION_NAME.isEmpty())
        assertTrue("VERSION_CODE should be >= 1", BuildConfig.VERSION_CODE >= 1)
        assertTrue("BUILD_TYPE should be debug or release", 
            BuildConfig.BUILD_TYPE == "debug" || BuildConfig.BUILD_TYPE == "release")
    }

    // ── T3: Debug flag ──
    @Test
    fun t3_debugFlag() {
        // testDebugUnitTest 运行在 debug variant，DEBUG 应为 true
        assertTrue("DEBUG should be true in debug variant", BuildConfig.DEBUG)
    }
}
