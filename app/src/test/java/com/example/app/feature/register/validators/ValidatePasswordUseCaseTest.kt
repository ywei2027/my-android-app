package com.example.app.feature.register.validators

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ValidatePasswordUseCase 单元测试
 *
 * 测试规则：密码 8-20 位，必须同时包含字母和数字，特殊字符放行
 * 对应 DESIGN.md §4.3 和 §5.2
 *
 * 注意：这是 TDD RED 阶段——被测类 ValidatePasswordUseCase 尚不存在
 */
class ValidatePasswordUseCaseTest {

    private val useCase = ValidatePasswordUseCase()

    @Test
    fun `valid password with 8 chars containing letters and digits returns success`() {
        val result = useCase("Abc12345")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `password with only digits returns error`() {
        val result = useCase("12345678")
        assertTrue(result.isFailure)
        assertEquals("密码需8-20位，含字母和数字", result.exceptionOrNull()?.message)
    }

    @Test
    fun `password shorter than 8 chars returns error`() {
        val result = useCase("Abc123")
        assertTrue(result.isFailure)
        assertEquals("密码需8-20位，含字母和数字", result.exceptionOrNull()?.message)
    }

    @Test
    fun `password longer than 20 chars returns error`() {
        val result = useCase("Abc1234567890123456789")
        assertTrue(result.isFailure)
        assertEquals("密码需8-20位，含字母和数字", result.exceptionOrNull()?.message)
    }

    @Test
    fun `password with special chars but valid letters and digits returns success`() {
        val result = useCase("Abc@#$%123")
        assertTrue(result.isSuccess)
    }
}
