package com.example.app.data.remote

import com.example.app.data.model.ApiResponse
import com.example.app.data.model.CheckPhoneData
import com.example.app.data.model.CheckPhoneRequest
import com.example.app.data.model.RegisterData
import com.example.app.data.model.RegisterRequest
import com.example.app.data.model.SendSmsData
import com.example.app.data.model.SendSmsRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface SmsApiService {
    @POST("api/v1/auth/check-phone")
    suspend fun checkPhone(@Body request: CheckPhoneRequest): ApiResponse<CheckPhoneData>

    @POST("api/v1/auth/send-sms")
    suspend fun sendSms(@Body request: SendSmsRequest): ApiResponse<SendSmsData>
}

interface AuthApiService {
    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<RegisterData>

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: com.example.app.data.model.LoginRequest): ApiResponse<RegisterData>
}
