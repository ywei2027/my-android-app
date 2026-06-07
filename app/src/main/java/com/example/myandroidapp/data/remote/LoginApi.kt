package com.example.myandroidapp.data.remote

import com.example.myandroidapp.domain.model.LoginRequest
import com.example.myandroidapp.domain.model.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface LoginApi {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
}
