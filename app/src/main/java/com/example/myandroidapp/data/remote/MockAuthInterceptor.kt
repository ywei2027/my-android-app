package com.example.myandroidapp.data.remote

import com.example.myandroidapp.domain.model.LoginErrorResponse
import com.example.myandroidapp.domain.model.LoginRequest
import com.example.myandroidapp.domain.model.LoginResponse
import com.example.myandroidapp.domain.model.User
import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer

class MockAuthInterceptor : Interceptor {

    companion object {
        private val JSON_MEDIA = "application/json".toMediaType()
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // 仅拦截登录请求
        if (request.url.encodedPath != "/api/auth/login") {
            return chain.proceed(request)
        }

        // Buffer 缓冲 body（one-shot 安全读取）
        val buffer = Buffer()
        request.body?.writeTo(buffer)
        val bodyString = buffer.readUtf8()
        val loginRequest = Gson().fromJson(bodyString, LoginRequest::class.java)

        return when {
            loginRequest.email == "admin@example.com" && loginRequest.password == "123456" ->
                buildResponse(request, 200, LoginResponse(
                    token = "mock-jwt-token",
                    user = User(id = "1", email = "admin@example.com", displayName = "Admin")
                ))
            loginRequest.email.contains("locked") ->
                buildResponse(request, 403, LoginErrorResponse(code = 403, message = "账户已被锁定"))
            loginRequest.email.contains("ratelimit") ->
                buildResponse(request, 429, LoginErrorResponse(code = 429, message = "操作过于频繁，请稍后再试"))
            else ->
                buildResponse(request, 401, LoginErrorResponse(code = 401, message = "邮箱或密码错误"))
        }
    }

    private fun buildResponse(request: okhttp3.Request, code: Int, body: Any): Response {
        val json = Gson().toJson(body)
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code == 200) "OK" else "Error")
            .body(json.toResponseBody(JSON_MEDIA))
            .build()
    }
}
