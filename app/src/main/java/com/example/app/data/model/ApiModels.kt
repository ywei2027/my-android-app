package com.example.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val code: Int,
    val data: T? = null,
    val message: String? = null
)

@Serializable
data class CheckPhoneRequest(val phone: String)

@Serializable
data class CheckPhoneData(val registered: Boolean)

@Serializable
data class SendSmsRequest(val phone: String)

@Serializable
data class SendSmsData(
    val expiresAt: String,
    val retryAfter: Int
)

@Serializable
data class RegisterRequest(
    val phone: String,
    val smsCode: String,
    val password: String
)

@Serializable
data class RegisterData(
    val token: String,
    val user: UserBrief
)

@Serializable
data class UserBrief(
    val userId: String,
    val phone: String
)

@Serializable
data class LoginRequest(
    val phone: String,
    val password: String
)
