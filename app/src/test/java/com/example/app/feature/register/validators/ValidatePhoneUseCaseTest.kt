package com.example.app.feature.register.validators

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ValidatePhoneUseCase 单元测试
 *
 * 测试规则：手机号必须为 1 开头的 11 位数字
 * 对应 DESIGN.md §4.3 和 §5.2
 *
 * 注意：这是 TDD RED 阶段——被测类 ValidatePhoneUseCase 尚不存在
 */
class ValidatePhoneUseCaseTest {

    private val useCase = ValidatePhoneUseCase()

    @Test
    fun `valid 11-digit phone starting with 1 returns success`() {
        val result = useCase("13812345678")
        assertTrue(result.isSuccess)
        assertEquals("13812345678", result.getOrNull())
    }

    @Test
    fun `phone not starting with 1 returns error`() {
        val result = useCase("23312345678")
        assertTrue(result.isFailure)
        assertEquals("请输入正确的手机号", result.exceptionOrNull()?.message)
    }

    @Test
    fun `phone shorter than 11 digits returns error`() {
        val result = useCase("1381234567")
        assertTrue(result.isFailure)
        assertEquals("请输入正确的手机号", result.exceptionOrNull()?.message)
    }

    @Test
    fun `empty input returns error`() {
        val result = useCase("")
        assertTrue(result.isFailure)
        assertEquals("请输入正确的手机号", result.exceptionOrNull()?.message)
    }

    @Test
    fun `phone with spaces but valid digits returns success`() {
        // 含空格的手机号可能来自格式化后的粘贴，应先去空格再校验
        val result = useCase("138 1234 5678")
        assertTrue(result.isSuccess)
        assertEquals("13812345678", result.getOrNull())
    }
}
