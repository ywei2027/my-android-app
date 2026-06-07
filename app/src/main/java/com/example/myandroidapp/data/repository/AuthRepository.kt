package com.example.myandroidapp.data.repository

import com.example.myandroidapp.data.local.LoginStateManager
import com.example.myandroidapp.data.remote.LoginApi
import com.example.myandroidapp.domain.model.AuthResult
import com.example.myandroidapp.domain.model.LoginErrorResponse
import com.example.myandroidapp.domain.model.LoginRequest
import com.example.myandroidapp.domain.model.LoginResponse
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.withTimeout
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val loginApi: LoginApi,
    private val stateManager: LoginStateManager
) {

    suspend fun login(email: String, password: String): AuthResult<LoginResponse> {
        return try {
            withTimeout(10_000L) {
                val response = loginApi.login(LoginRequest(email, password))
                if (response.isSuccessful) {
                    val body = response.body()
                        ?: return@withTimeout AuthResult.Error(-1, "服务器返回数据异常")
                    stateManager.saveUser(body.token, body.user)
                    AuthResult.Success(body)
                } else {
                    val errorBody = parseError(response)
                    AuthResult.Error(errorBody.code, errorBody.message)
                }
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            AuthResult.Error(-1, "网络请求超时，请重试")
        } catch (e: HttpException) {
            AuthResult.Error(e.code(), e.message())
        } catch (e: SocketTimeoutException) {
            AuthResult.Error(-1, "网络请求超时，请重试")
        } catch (e: IOException) {
            AuthResult.Error(-1, "网络连接失败，请检查网络设置")
        } catch (e: JsonSyntaxException) {
            AuthResult.Error(-1, "数据解析失败")
        } catch (e: Exception) {
            AuthResult.Error(-1, "未知错误: ${e.message}")
        }
    }

    private fun parseError(response: Response<*>): AuthResult.Error {
        return try {
            val errorBody = response.errorBody()?.string() ?: "{}"
            val parsed = Gson().fromJson(errorBody, LoginErrorResponse::class.java)
                ?: LoginErrorResponse(response.code(), "未知错误")
            AuthResult.Error(parsed.code, parsed.message)
        } catch (e: Exception) {
            AuthResult.Error(response.code(), "请求失败 (${response.code()})")
        }
    }

    fun isLoggedIn(): Boolean = stateManager.getToken() != null

    fun logout() = stateManager.clear()
}
