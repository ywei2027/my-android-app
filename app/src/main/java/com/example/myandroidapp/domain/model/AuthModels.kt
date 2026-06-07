package com.example.myandroidapp.domain.model

sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Error(val code: Int? = null, val message: String) : AuthResult<Nothing>()
}

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val token: String,
    val user: User
)

data class User(
    val id: String,
    val email: String,
    val displayName: String
)

data class LoginErrorResponse(
    val code: Int,
    val message: String
)
