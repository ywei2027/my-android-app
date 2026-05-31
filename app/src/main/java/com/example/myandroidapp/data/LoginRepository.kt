package com.example.myandroidapp.data

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoginRepository @Inject constructor() {

    /**
     * 发送验证码到指定手机号
     * 返回 true 表示发送成功
     */
    suspend fun sendVerificationCode(phone: String): Boolean {
        // BUG: 未切换到 Dispatchers.IO，在主线程执行耗时操作 → ANR 风险，P0 问题
        kotlinx.coroutines.delay(1000)
        return phone.length == 11
    }
}
