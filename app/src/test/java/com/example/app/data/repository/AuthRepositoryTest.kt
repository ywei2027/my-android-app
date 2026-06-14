package com.example.app.data.repository

import com.example.app.data.remote.AuthApiService
import com.example.app.data.remote.SmsApiService
import com.example.app.data.model.ApiResponse
import com.example.app.data.model.CheckPhoneData
import com.example.app.data.model.CheckPhoneRequest
import com.example.app.data.model.RegisterData
import com.example.app.data.model.RegisterRequest
import com.example.app.data.model.SendSmsData
import com.example.app.data.model.SendSmsRequest
import com.example.app.data.model.UserBrief
import com.example.app.data.local.TokenStorage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * AuthRepository 单元测试
 *
 * 测试 AuthRepository 对 API 调用的处理逻辑：
 * - 成功响应 → Result.success
 * - 业务错误码 → Result.failure
 * - 网络异常 → Result.failure
 *
 * 对应 DESIGN.md §5.2
 *
 * 注意：这是 TDD RED 阶段——被测类 AuthRepository 尚不存在
 */
class AuthRepositoryTest {

    private val smsApiService: SmsApiService = mockk()
    private val authApiService: AuthApiService = mockk()
    private val tokenStorage: TokenStorage = mockk()

    private val repository = AuthRepository(smsApiService, authApiService, tokenStorage)

    // ============ 注册测试 ============

    @Test
    fun `register with 200 response returns success`() = runTest {
        val token = "mock_token_abc123"
        val user = UserBrief(userId = "usr_abc123", phone = "13812345678")
        val registerData = RegisterData(token = token, user = user)
        val apiResponse = ApiResponse(code = 200, data = registerData, message = null)

        coEvery { authApiService.register(any()) } returns apiResponse
        every { tokenStorage.saveToken(token) } returns Unit

        val result = repository.register("13812345678", "123456", "Abc12345")

        assertTrue(result.isSuccess)
        assertEquals(registerData, result.getOrNull())

        coVerify(exactly = 1) { authApiService.register(any()) }
        verify(exactly = 1) { tokenStorage.saveToken(token) }
    }

    @Test
    fun `register with 409 conflict response returns failure`() = runTest {
        val apiResponse = ApiResponse<RegisterData>(
            code = 409,
            data = null,
            message = "该手机号已注册"
        )

        coEvery { authApiService.register(any()) } returns apiResponse

        val result = repository.register("13812345678", "123456", "Abc12345")

        assertTrue(result.isFailure)
        assertEquals("该手机号已注册", result.exceptionOrNull()?.message)
    }

    @Test
    fun `register with IOException returns network failure`() = runTest {
        coEvery { authApiService.register(any()) } throws IOException("网络连接失败")

        val result = repository.register("13812345678", "123456", "Abc12345")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
        assertEquals("网络连接失败", result.exceptionOrNull()?.message)
    }

    // ============ SMS 测试 ============

    @Test
    fun `sendSms with 200 response returns success`() = runTest {
        val smsData = SendSmsData(
            expiresAt = "2026-06-14T12:15:00+08:00",
            retryAfter = 60
        )
        val apiResponse = ApiResponse(code = 200, data = smsData, message = null)

        coEvery { smsApiService.sendSms(SendSmsRequest("13812345678")) } returns apiResponse

        val result = repository.sendSms("13812345678")

        assertTrue(result.isSuccess)
        assertEquals(smsData, result.getOrNull())
    }

    @Test
    fun `sendSms with 500 response returns failure`() = runTest {
        val apiResponse = ApiResponse<SendSmsData>(
            code = 500,
            data = null,
            message = "短信服务暂时不可用"
        )

        coEvery { smsApiService.sendSms(SendSmsRequest("13812345678")) } returns apiResponse

        val result = repository.sendSms("13812345678")

        assertTrue(result.isFailure)
        assertEquals("短信服务暂时不可用", result.exceptionOrNull()?.message)
    }
}
