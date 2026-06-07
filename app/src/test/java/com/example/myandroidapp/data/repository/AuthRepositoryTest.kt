package com.example.myandroidapp.data.repository

import com.example.myandroidapp.data.local.LoginStateManager
import com.example.myandroidapp.data.remote.LoginApi
import com.example.myandroidapp.domain.model.AuthResult
import com.example.myandroidapp.domain.model.LoginErrorResponse
import com.example.myandroidapp.domain.model.LoginRequest
import com.example.myandroidapp.domain.model.LoginResponse
import com.example.myandroidapp.domain.model.User
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Response
import java.net.SocketTimeoutException

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var loginApi: LoginApi
    private lateinit var stateManager: LoginStateManager
    private lateinit var repository: AuthRepository

    @Before
    fun setUp() {
        loginApi = mockk()
        stateManager = mockk(relaxed = true)
        repository = AuthRepository(loginApi, stateManager)
    }

    // T10: AuthRepository HttpException -> Error — Mock 401 response
    @Test
    fun `login returns Error when API returns 401`() = runTest(testDispatcher) {
        val errorResponse = LoginErrorResponse(code = 401, message = "邮箱或密码错误")
        coEvery { loginApi.login(LoginRequest("wrong@example.com", "wrong")) } returns
            Response.error(
                401,
                okhttp3.ResponseBody.create(
                    "application/json".toMediaType(),
                    """{"code":401,"message":"邮箱或密码错误"}"""
                )
            )

        val result = repository.login("wrong@example.com", "wrong")

        assertTrue(result is AuthResult.Error)
        val error = result as AuthResult.Error
        assertEquals(401, error.code)
    }

    // T11: AuthRepository withTimeout — runTest advanceTimeBy(10_001) -> TimeoutCancellationException
    @Test
    fun `login returns Error on timeout`() = runTest(testDispatcher) {
        coEvery { loginApi.login(any()) } coAnswers {
            advanceTimeBy(10_001)
            throw SocketTimeoutException("timeout")
        }

        val result = repository.login("user@example.com", "password123")

        assertTrue(result is AuthResult.Error)
        val error = result as AuthResult.Error
        assertEquals(-1, error.code)
        assertEquals("网络请求超时，请重试", error.message)
    }
}
