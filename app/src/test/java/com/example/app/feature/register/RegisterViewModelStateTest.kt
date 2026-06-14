package com.example.app.feature.register

import androidx.lifecycle.SavedStateHandle
import com.example.app.data.repository.AuthRepository
import com.example.app.feature.register.validators.ValidatePasswordUseCase
import com.example.app.feature.register.validators.ValidatePhoneUseCase
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RegisterViewModel 状态转换单元测试
 *
 * 测试 ViewModel 对用户输入的状态管理逻辑：
 * - 手机号变更 → UiState 更新
 * - 密码不一致 → confirmPasswordError
 * - 协议未勾选 → 注册按钮禁用
 *
 * 对应 DESIGN.md §5.2
 *
 * 注意：这是 TDD RED 阶段——被测类 RegisterViewModel 尚不存在
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelStateTest {

    private val validatePhone: ValidatePhoneUseCase = ValidatePhoneUseCase()
    private val validatePassword: ValidatePasswordUseCase = ValidatePasswordUseCase()
    private val authRepository: AuthRepository = mockk()
    private val savedStateHandle = SavedStateHandle()

    private fun createViewModel(): RegisterViewModel {
        return RegisterViewModel(validatePhone, validatePassword, authRepository, savedStateHandle)
    }

    @Test
    fun `onPhoneChanged updates phone field and validates`() = runTest {
        val viewModel = createViewModel()

        viewModel.onPhoneChanged("13812345678")

        val state = viewModel.uiState.value
        assertEquals("13812345678", state.phone)
        assertTrue(state.isPhoneValid)
        assertTrue(state.canRequestSms)
    }

    @Test
    fun `mismatched passwords sets confirmPasswordError`() = runTest {
        val viewModel = createViewModel()

        // 输入合法密码
        viewModel.onPasswordChanged("Abc12345")
        // 输入不一致的确认密码
        viewModel.onConfirmPasswordChanged("Abc12346")

        val state = viewModel.uiState.value
        assertEquals("两次输入的密码不一致", state.confirmPasswordError)
        assertFalse(state.isConfirmMatch)
        assertFalse(state.isFormValid)
    }

    @Test
    fun `all fields valid but agreement not accepted disables submit`() = runTest {
        val viewModel = createViewModel()

        viewModel.onPhoneChanged("13812345678")       // 合法手机号
        viewModel.onSmsCodeChanged("123456")           // 6位验证码
        viewModel.onPasswordChanged("Abc12345")        // 合法密码
        viewModel.onConfirmPasswordChanged("Abc12345") // 一致密码
        // 不勾选协议

        val state = viewModel.uiState.value
        assertFalse(state.agreementAccepted)
        assertTrue(state.isPhoneValid)
        assertTrue(state.isPasswordValid)
        assertTrue(state.isConfirmMatch)
        assertTrue(state.isFormValid)  // 字段级校验通过
        assertFalse(state.canSubmit)    // 但协议未勾选，不可提交
    }

    @Test
    fun `phone validation error sets phoneError`() = runTest {
        val viewModel = createViewModel()

        viewModel.onPhoneChanged("23312345678") // 非 1 开头

        val state = viewModel.uiState.value
        assertEquals("请输入正确的手机号", state.phoneError)
        assertFalse(state.isPhoneValid)
    }

    @Test
    fun `password too short sets passwordError`() = runTest {
        val viewModel = createViewModel()

        viewModel.onPasswordChanged("Abc123") // 不足 8 位

        val state = viewModel.uiState.value
        assertEquals("密码需8-20位，含字母和数字", state.passwordError)
        assertFalse(state.isPasswordValid)
    }
}
