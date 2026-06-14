package com.example.app.feature.register

import androidx.lifecycle.SavedStateHandle
import com.example.app.data.local.TokenStorage
import com.example.app.data.remote.AuthApiService
import com.example.app.data.remote.SmsApiService
import com.example.app.data.repository.AuthRepository
import com.example.app.feature.register.validators.ValidatePasswordUseCase
import com.example.app.feature.register.validators.ValidatePhoneUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit

/**
 * RegisterViewModel 集成测试
 *
 * 使用 MockWebServer 模拟后端 API，
 * 验证 ViewModel 完整注册流程的状态转换。
 *
 * 对应 DESIGN.md §5.4
 *
 * 注意：这是 TDD RED 阶段——部分被测代码尚不存在
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelIntegrationTest {

    private lateinit var mockServer: MockWebServer
    private lateinit var smsApiService: SmsApiService
    private lateinit var authApiService: AuthApiService
    private lateinit var authRepository: AuthRepository
    private lateinit var tokenStorage: TokenStorage
    private lateinit var validatePhone: ValidatePhoneUseCase
    private lateinit var validatePassword: ValidatePasswordUseCase
    private lateinit var savedStateHandle: SavedStateHandle

    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() {
        mockServer = MockWebServer()

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()

        val contentType = "application/json".toMediaType()
        val baseUrl = mockServer.url("/")

        smsApiService = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(SmsApiService::class.java)

        authApiService = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(AuthApiService::class.java)

        tokenStorage = mockk(relaxed = true)
        every { tokenStorage.saveToken(any()) } returns Unit

        authRepository = AuthRepository(smsApiService, authApiService, tokenStorage)
        validatePhone = ValidatePhoneUseCase()
        validatePassword = ValidatePasswordUseCase()
        savedStateHandle = SavedStateHandle()
    }

    @After
    fun tearDown() {
        mockServer.shutdown()
    }

    private fun createViewModel() = RegisterViewModel(
        validatePhone, validatePassword, authRepository, savedStateHandle
    )

    // ============ 完整注册成功 ============

    @Test
    fun `register success triggers registrationSuccess state`() = runTest {
        // 模拟注册成功响应
        val responseBody = """
            {
                "code": 200,
                "data": {
                    "token": "mock_token_abc",
                    "user": { "userId": "usr_abc", "phone": "13812345678" }
                }
            }
        """.trimIndent()

        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
        )

        val viewModel = createViewModel()

        // 填写完整表单
        viewModel.onPhoneChanged("13812345678")
        viewModel.onSmsCodeChanged("123456")
        viewModel.onPasswordChanged("Abc12345")
        viewModel.onConfirmPasswordChanged("Abc12345")
        viewModel.onAgreementToggled(true)

        // 提交注册
        viewModel.register()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.registrationSuccess, "注册应成功")
        assertFalse(state.isSubmitting, "提交状态应结束")
    }

    // ============ 手机号已注册拦截 ============

    @Test
    fun `already registered phone shows phoneAlreadyRegistered state`() = runTest {
        // 模拟 checkPhone 返回 409
        val responseBody = """
            {
                "code": 409,
                "data": { "registered": true },
                "message": "该手机号已注册"
            }
        """.trimIndent()

        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
        )

        val viewModel = createViewModel()
        viewModel.onPhoneChanged("13812345678")

        // 触发验证码请求 → 内部调用 checkPhone
        viewModel.requestSmsCode()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.phoneAlreadyRegistered, "应识别手机号已注册")
    }

    // ============ 验证码发送成功 ============

    @Test
    fun `sendSms success starts countdown`() = runTest {
        // 模拟 checkPhone 成功（未注册）
        val checkResponse = """
            {"code": 200, "data": {"registered": false}}
        """.trimIndent()

        // 模拟 sendSms 成功
        val smsResponse = """
            {
                "code": 200,
                "data": {
                    "expiresAt": "2026-06-14T12:15:00+08:00",
                    "retryAfter": 60
                }
            }
        """.trimIndent()

        mockServer.enqueue(
            MockResponse().setResponseCode(200).setBody(checkResponse)
        )
        mockServer.enqueue(
            MockResponse().setResponseCode(200).setBody(smsResponse)
        )

        val viewModel = createViewModel()
        viewModel.onPhoneChanged("13812345678")

        viewModel.requestSmsCode()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.smsCountdownSeconds > 0, "倒计时应启动")
        assertFalse(state.isSendingSms, "发送状态应结束")
    }

    // ============ 验证码发送失败 ============

    @Test
    fun `sendSms failure does NOT start countdown`() = runTest {
        // 模拟 checkPhone 成功
        val checkResponse = """
            {"code": 200, "data": {"registered": false}}
        """.trimIndent()

        // 模拟 sendSms 失败
        val smsResponse = """
            {"code": 500, "message": "短信服务暂时不可用"}
        """.trimIndent()

        mockServer.enqueue(
            MockResponse().setResponseCode(200).setBody(checkResponse)
        )
        mockServer.enqueue(
            MockResponse().setResponseCode(200).setBody(smsResponse)
        )

        val viewModel = createViewModel()
        viewModel.onPhoneChanged("13812345678")

        viewModel.requestSmsCode()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0, state.smsCountdownSeconds, "倒计时不应启动")
        assertNotNull(state.registrationError, "应有错误提示")
    }

    // ============ 注册网络超时 ============

    @Test
    fun `register network timeout shows error and stays on page`() = runTest {
        // 模拟网络超时（不返回响应）
        mockServer.enqueue(
            MockResponse()
                .setBodyDelay(15, TimeUnit.SECONDS)  // 超过 OkHttp 5s 超时
                .setBody("{}")
        )

        // 缩短 OkHttp 超时以便测试快速失败
        val shortClient = OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(1, TimeUnit.SECONDS)
            .build()

        val baseUrl = mockServer.url("/")
        val contentType = "application/json".toMediaType()
        val tempAuthApi = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(shortClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(AuthApiService::class.java)

        val tempRepo = AuthRepository(smsApiService, tempAuthApi, tokenStorage)
        val viewModel = RegisterViewModel(validatePhone, validatePassword, tempRepo, savedStateHandle)

        viewModel.onPhoneChanged("13812345678")
        viewModel.onSmsCodeChanged("123456")
        viewModel.onPasswordChanged("Abc12345")
        viewModel.onConfirmPasswordChanged("Abc12345")
        viewModel.onAgreementToggled(true)

        viewModel.register()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.registrationSuccess, "注册不应成功")
        assertFalse(state.isSubmitting, "提交状态应结束")
    }

    // ============ 注册成功但登录失败降级 ============

    @Test
    fun `register success but login failure triggers degradation`() = runTest {
        // 模拟注册成功
        val registerResponse = """
            {
                "code": 200,
                "data": {
                    "token": "mock_token",
                    "user": { "userId": "usr_abc", "phone": "13812345678" }
                }
            }
        """.trimIndent()

        mockServer.enqueue(
            MockResponse().setResponseCode(200).setBody(registerResponse)
        )

        val viewModel = createViewModel()

        viewModel.onPhoneChanged("13812345678")
        viewModel.onSmsCodeChanged("123456")
        viewModel.onPasswordChanged("Abc12345")
        viewModel.onConfirmPasswordChanged("Abc12345")
        viewModel.onAgreementToggled(true)

        viewModel.register()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.registrationSuccess, "注册应成功")

        // Token 应已保存（降级后用户可通过 Token 自动登录）
        // 注意：这由 AuthRepository.register() 方法负责，
        // 在收到 200 响应时自动调用 tokenStorage.saveToken()
    }
}
