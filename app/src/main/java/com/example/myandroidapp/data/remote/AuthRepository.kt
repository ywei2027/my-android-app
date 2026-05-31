package com.example.myandroidapp.data.remote

import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 认证结果封装。
 */
sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Error(val message: String) : AuthResult<Nothing>()
}

/**
 * 登录响应数据。
 */
data class LoginResponse(
    val token: String,
    val phone: String,
    val userId: String
)

/**
 * 认证 Repository。
 * 负责手机号验证码登录的所有网络交互。
 *
 * 当前实现为模拟版本（替换为真实 API 时只需改此类）。
 */
@Singleton
class AuthRepository @Inject constructor() {

    /**
     * 发送验证码到指定手机号。
     * 返回发送结果。
     */
    suspend fun sendVerificationCode(phoneNumber: String): AuthResult<Unit> {
        // TODO: 替换为真实 API 调用
        // 模拟网络延迟
        delay(1500L)

        // 模拟验证：手机号必须是 11 位
        return if (phoneNumber.length == 11 && phoneNumber.all { it.isDigit() }) {
            AuthResult.Success(Unit)
        } else {
            AuthResult.Error("请输入正确的 11 位手机号")
        }
    }

    /**
     * 用手机号和验证码进行登录。
     * 成功返回 token 等信息。
     */
    suspend fun loginWithCode(
        phoneNumber: String,
        verificationCode: String
    ): AuthResult<LoginResponse> {
        // TODO: 替换为真实 API 调用
        delay(1000L)

        // 模拟验证码验证（演示用：验证码为 "123456" 即可通过）
        return if (verificationCode == "123456" && phoneNumber.length == 11) {
            AuthResult.Success(
                LoginResponse(
                    token = "mock_token_${System.currentTimeMillis()}",
                    phone = phoneNumber,
                    userId = "user_${phoneNumber.takeLast(4)}"
                )
            )
        } else {
            AuthResult.Error("验证码错误，请重试")
        }
    }
}
