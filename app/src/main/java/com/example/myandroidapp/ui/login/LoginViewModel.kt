package com.example.myandroidapp.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myandroidapp.data.local.LoginStateManager
import com.example.myandroidapp.data.remote.AuthRepository
import com.example.myandroidapp.data.remote.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 登录页 UI 状态。
 */
data class LoginUiState(
    val phoneNumber: String = "",
    val verificationCode: String = "",
    val isCodeSent: Boolean = false,
    val countdownSeconds: Int = 0,
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val errorMessage: String? = null
)

/**
 * 登录 ViewModel。
 *
 * 职责：
 * - 管理手机号 + 验证码登录流程
 * - 发送验证码（带 60 秒倒计时）
 * - 验证码登录
 * - 登录状态持久化（通过 LoginStateManager）
 *
 * 架构要点：
 * - ✅ 不持有 Activity/Context 引用 —— 无内存泄漏
 * - ✅ 使用 viewModelScope 管理协程生命周期
 * - ✅ 通过 Hilt 注入依赖（AuthRepository, LoginStateManager）
 * - ✅ 所有 nullable 安全处理，无 !! 强制解包
 * - ✅ 错误信息通过 UiState 暴露，不吞没异常
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val loginStateManager: LoginStateManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    /**
     * 更新手机号。
     */
    fun onPhoneNumberChanged(phone: String) {
        _uiState.update { it.copy(phoneNumber = phone, errorMessage = null) }
    }

    /**
     * 更新验证码。
     */
    fun onVerificationCodeChanged(code: String) {
        _uiState.update { it.copy(verificationCode = code, errorMessage = null) }
    }

    /**
     * 发送验证码。
     * 校验手机号格式 → 调用 API → 启动 60 秒倒计时。
     */
    fun sendVerificationCode() {
        val phone = _uiState.value.phoneNumber

        // 格式校验
        if (phone.length != 11 || !phone.all { it.isDigit() }) {
            _uiState.update { it.copy(errorMessage = "请输入正确的 11 位手机号") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = authRepository.sendVerificationCode(phone)) {
                is AuthResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isCodeSent = true,
                            isLoading = false,
                            countdownSeconds = 60
                        )
                    }
                    startCountdown()
                }
                is AuthResult.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
            }
        }
    }

    /**
     * 用验证码登录。
     */
    fun login() {
        val phone = _uiState.value.phoneNumber
        val code = _uiState.value.verificationCode

        // 校验
        if (phone.isBlank()) {
            _uiState.update { it.copy(errorMessage = "请输入手机号") }
            return
        }
        if (code.length < 4) {
            _uiState.update { it.copy(errorMessage = "请输入完整验证码") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = authRepository.loginWithCode(phone, code)) {
                is AuthResult.Success -> {
                    // 持久化登录状态
                    loginStateManager.saveLoginState(
                        token = result.data.token,
                        phone = result.data.phone
                    )
                    _uiState.update {
                        it.copy(isLoading = false, isLoggedIn = true)
                    }
                }
                is AuthResult.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
            }
        }
    }

    /**
     * 清除错误提示。
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * 60 秒倒计时。
     */
    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (i in 60 downTo 1) {
                delay(1000L)
                _uiState.update { it.copy(countdownSeconds = i - 1) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}
