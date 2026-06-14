package com.example.app.data.repository

import com.example.app.data.local.TokenStorage
import com.example.app.data.model.CheckPhoneRequest
import com.example.app.data.model.RegisterData
import com.example.app.data.model.RegisterRequest
import com.example.app.data.model.SendSmsData
import com.example.app.data.model.SendSmsRequest
import com.example.app.data.remote.AuthApiService
import com.example.app.data.remote.SmsApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val smsApi: SmsApiService,
    private val authApi: AuthApiService,
    private val tokenStorage: TokenStorage
) {

    suspend fun checkPhone(phone: String): Result<com.example.app.data.model.CheckPhoneData> {
        return try {
            val response = smsApi.checkPhone(CheckPhoneRequest(phone))
            if (response.code == 200) {
                Result.success(response.data!!)
            } else {
                Result.failure(IllegalArgumentException(response.message ?: "操作失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendSms(phone: String): Result<SendSmsData> {
        return try {
            val response = smsApi.sendSms(SendSmsRequest(phone))
            if (response.code == 200) {
                Result.success(response.data!!)
            } else {
                Result.failure(IllegalArgumentException(response.message ?: "操作失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(
        phone: String,
        smsCode: String,
        password: String
    ): Result<RegisterData> {
        return try {
            val response = authApi.register(
                RegisterRequest(phone, smsCode, password)
            )
            if (response.code == 200) {
                val data = response.data!!
                tokenStorage.saveToken(data.token)
                Result.success(data)
            } else {
                Result.failure(IllegalArgumentException(response.message ?: "操作失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(
        phone: String,
        password: String
    ): Result<RegisterData> {
        return try {
            val response = authApi.login(
                com.example.app.data.model.LoginRequest(phone, password)
            )
            if (response.code == 200) {
                val data = response.data!!
                tokenStorage.saveToken(data.token)
                Result.success(data)
            } else {
                Result.failure(IllegalArgumentException(response.message ?: "登录失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun saveToken(token: String) = tokenStorage.saveToken(token)

    fun getToken(): String? = tokenStorage.getToken()

    fun clearToken() = tokenStorage.clearToken()
}
