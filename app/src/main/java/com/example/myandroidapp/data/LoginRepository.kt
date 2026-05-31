package com.example.myandroidapp.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoginRepository @Inject constructor() {

    /**
     * 发送验证码到指定手机号
     * 返回 true 表示发送成功
     */
    suspend fun sendVerificationCode(phone: String): Boolean {
        return withContext(Dispatchers.IO) {
            // 模拟网络请求 - 实际项目中调用 Retrofit API
            kotlinx.coroutines.delay(1000)
            phone.length == 11
        }
    }
}
