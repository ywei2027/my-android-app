package com.example.myandroidapp.ui.auth

import app.cash.turbine.test
import com.example.myandroidapp.data.repository.AuthRepository
import com.example.myandroidapp.domain.model.AuthResult
import com.example.myandroidapp.domain.model.LoginEvent
import com.example.myandroidapp.domain.model.LoginResponse
import com.example.myandroidapp.domain.model.LoginStatus
import com.example.myandroidapp.domain.model.LoginUiState
import com.example.myandroidapp.domain.model.User
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: AuthRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // T1: email 校验 — 合法邮箱 isEmailValid=true，非法 false
    @Test
    fun `T1 valid email sets isEmailValid true`() = runTest(testDispatcher) {
        val viewModel = LoginViewModel(authRepository)
        viewModel.uiState.test {
            val initial = awaitItem()
            assertEquals(LoginStatus.Idle, initial.status)
            assertFalse(initial.isEmailValid)

            viewModel.onEvent(LoginEvent.EmailChanged("user@example.com"))
            val afterValid = awaitItem()
            assertTrue(afterValid.isEmailValid)
            assertEquals("user@example.com", afterValid.email)
            assertEquals(LoginStatus.Editing, afterValid.status)
        }
    }

    @Test
    fun `T1b invalid email sets isEmailValid false`() = runTest(testDispatcher) {
        val viewModel = LoginViewModel(authRepository)
        viewModel.uiState.test {
            awaitItem() // initial Idle
            viewModel.onEvent(LoginEvent.EmailChanged("invalid-email"))
            val state = awaitItem()
            assertFalse(state.isEmailValid)
        }
    }

    // T2: 按钮联动 — isEmailValid=false -> Editing 但按钮不起用；两者满足 -> 按钮可用
    @Test
    fun `T2 button enabled condition requires valid email and non-empty password`() = runTest(testDispatcher) {
        val viewModel = LoginViewModel(authRepository)
        viewModel.uiState.test {
            awaitItem() // Idle

            // 仅邮箱有效
            viewModel.onEvent(LoginEvent.EmailChanged("user@example.com"))
            val withEmail = awaitItem()
            assertTrue(withEmail.isEmailValid)
            assertTrue(withEmail.password.isEmpty())
            // 按钮判断：isEmailValid && passwordNotEmpty -> false

            // 加密码
            viewModel.onEvent(LoginEvent.PasswordChanged("pass"))
            val withBoth = awaitItem()
            assertTrue(withBoth.isEmailValid)
            assertTrue(withBoth.password.isNotEmpty())
            // 按钮判断：isEmailValid && passwordNotEmpty -> true
        }
    }

    // T3: login 成功 -> status=Success
    @Test
    fun `T3 login success sets status to Success`() = runTest(testDispatcher) {
        coEvery { authRepository.login("user@example.com", "password123") } returns AuthResult.Success(
            LoginResponse("jwt-token", User("1", "user@example.com", "User"))
        )

        val viewModel = LoginViewModel(authRepository)
        viewModel.uiState.test {
            awaitItem() // Idle
            viewModel.onEvent(LoginEvent.EmailChanged("user@example.com"))
            awaitItem() // Editing
            viewModel.onEvent(LoginEvent.PasswordChanged("password123"))
            awaitItem() // Editing

            viewModel.onEvent(LoginEvent.SubmitLogin)
            val loadingState = awaitItem()
            assertEquals(LoginStatus.Loading, loadingState.status)
            assertTrue(loadingState.isEmailValid)

            val successState = awaitItem()
            assertEquals(LoginStatus.Success, successState.status)
        }
    }

    // T4: login 401 -> status=Error, errorMessage
    @Test
    fun `T4 login 401 sets Error status with message`() = runTest(testDispatcher) {
        coEvery { authRepository.login("wrong@example.com", "wrong") } returns AuthResult.Error(401, "邮箱或密码错误")

        val viewModel = LoginViewModel(authRepository)
        viewModel.uiState.test {
            awaitItem() // Idle
            viewModel.onEvent(LoginEvent.EmailChanged("wrong@example.com"))
            awaitItem()
            viewModel.onEvent(LoginEvent.PasswordChanged("wrong"))
            awaitItem()

            viewModel.onEvent(LoginEvent.SubmitLogin)
            awaitItem() // Loading
            val errorState = awaitItem()
            assertEquals(LoginStatus.Error, errorState.status)
            assertEquals("邮箱或密码错误", errorState.errorMessage)
        }
    }

    // T5: login 403 -> Error
    @Test
    fun `T5 login 403 sets Error status`() = runTest(testDispatcher) {
        coEvery { authRepository.login("locked@example.com", "pass") } returns AuthResult.Error(403, "账户已被锁定")

        val viewModel = LoginViewModel(authRepository)
        viewModel.uiState.test {
            awaitItem() // Idle
            viewModel.onEvent(LoginEvent.EmailChanged("locked@example.com"))
            awaitItem()
            viewModel.onEvent(LoginEvent.PasswordChanged("pass"))
            awaitItem()

            viewModel.onEvent(LoginEvent.SubmitLogin)
            awaitItem() // Loading
            val errorState = awaitItem()
            assertEquals(LoginStatus.Error, errorState.status)
            assertEquals("账户已被锁定", errorState.errorMessage)
        }
    }

    // T6: login 429 -> Cooldown -> Idle
    @Test
    fun `T6 login 429 sets Cooldown with 30s countdown`() = runTest(testDispatcher) {
        coEvery { authRepository.login("ratelimit@example.com", "pass") } returns AuthResult.Error(429, "操作过于频繁，请稍后再试")

        val viewModel = LoginViewModel(authRepository)
        viewModel.uiState.test {
            awaitItem() // Idle
            viewModel.onEvent(LoginEvent.EmailChanged("ratelimit@example.com"))
            awaitItem()
            viewModel.onEvent(LoginEvent.PasswordChanged("pass"))
            awaitItem()

            viewModel.onEvent(LoginEvent.SubmitLogin)
            awaitItem() // Loading
            val cooldownState = awaitItem()
            assertEquals(LoginStatus.Cooldown, cooldownState.status)
            assertEquals(30, cooldownState.cooldownSeconds)
            assertEquals("操作过于频繁，请稍后再试", cooldownState.errorMessage)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // T7: login 超时 -> Timeout
    @Test
    fun `T7 login timeout sets Timeout status`() = runTest(testDispatcher) {
        coEvery { authRepository.login("user@example.com", "pass") } returns AuthResult.Error(-1, "网络请求超时，请重试")

        val viewModel = LoginViewModel(authRepository)
        viewModel.uiState.test {
            awaitItem() // Idle
            viewModel.onEvent(LoginEvent.EmailChanged("user@example.com"))
            awaitItem()
            viewModel.onEvent(LoginEvent.PasswordChanged("pass"))
            awaitItem()

            viewModel.onEvent(LoginEvent.SubmitLogin)
            awaitItem() // Loading
            val timeoutState = awaitItem()
            assertEquals(LoginStatus.Timeout, timeoutState.status)
            assertEquals("网络请求超时，请重试", timeoutState.errorMessage)
        }
    }

    // T8: Loading 中不可重入
    @Test
    fun `T8 login not re-entrant during Loading`() = runTest(testDispatcher) {
        coEvery { authRepository.login("user@example.com", "password123") } returns
            AuthResult.Success(LoginResponse("token", User("1", "user@example.com", "User")))

        val viewModel = LoginViewModel(authRepository)
        viewModel.uiState.test {
            awaitItem() // Idle
            viewModel.onEvent(LoginEvent.EmailChanged("user@example.com"))
            awaitItem()
            viewModel.onEvent(LoginEvent.PasswordChanged("password123"))
            awaitItem()

            viewModel.onEvent(LoginEvent.SubmitLogin)
            val loadingState = awaitItem()
            assertEquals(LoginStatus.Loading, loadingState.status)

            // 尝试再次登录 — 应被原子检查拦截
            viewModel.onEvent(LoginEvent.SubmitLogin)

            // 消费结果
            val result = awaitItem()
            assertEquals(LoginStatus.Success, result.status)

            coVerify(exactly = 1) { authRepository.login("user@example.com", "password123") }
        }
    }

    // T9: 输入变更清除错误
    @Test
    fun `T9 input change clears error and returns to Editing`() = runTest(testDispatcher) {
        coEvery { authRepository.login("wrong@example.com", "wrong") } returns AuthResult.Error(401, "邮箱或密码错误")

        val viewModel = LoginViewModel(authRepository)
        viewModel.uiState.test {
            awaitItem() // Idle
            viewModel.onEvent(LoginEvent.EmailChanged("wrong@example.com"))
            awaitItem()
            viewModel.onEvent(LoginEvent.PasswordChanged("wrong"))
            awaitItem()

            viewModel.onEvent(LoginEvent.SubmitLogin)
            awaitItem() // Loading
            val errorState = awaitItem()
            assertEquals(LoginStatus.Error, errorState.status)
            assertTrue(errorState.errorMessage != null)

            // 修改邮箱 -> 清除错误
            viewModel.onEvent(LoginEvent.EmailChanged("new@example.com"))
            val clearedState = awaitItem()
            assertEquals(LoginStatus.Editing, clearedState.status)
            assertNull(clearedState.errorMessage)
        }
    }

    // logout test
    @Test
    fun `logout clears state`() = runTest(testDispatcher) {
        coEvery { authRepository.login("user@example.com", "pass") } returns AuthResult.Success(
            LoginResponse("token", User("1", "user@example.com", "User"))
        )

        val viewModel = LoginViewModel(authRepository)
        viewModel.onEvent(LoginEvent.EmailChanged("user@example.com"))
        viewModel.onEvent(LoginEvent.PasswordChanged("pass"))
        viewModel.onEvent(LoginEvent.SubmitLogin)

        viewModel.logout()
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals("", state.email)
        assertEquals("", state.password)
        assertEquals(LoginStatus.Idle, state.status)
        assertFalse(state.isEmailValid)
    }
}
