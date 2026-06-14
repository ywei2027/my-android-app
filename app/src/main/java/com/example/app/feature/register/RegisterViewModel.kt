package com.example.app.feature.register

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.data.repository.AuthRepository
import com.example.app.feature.register.validators.ValidatePasswordUseCase
import com.example.app.feature.register.validators.ValidatePhoneUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val validatePhone: ValidatePhoneUseCase,
    private val validatePassword: ValidatePasswordUseCase,
    private val authRepository: AuthRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null
    private var registerJob: Job? = null

    init {
        savedStateHandle.get<String>("phone")?.let { phone ->
            _uiState.update { it.copy(phone = phone) }
            validatePhoneField(phone)
        }
        savedStateHandle.get<Long>("smsSentTimestamp")?.let { timestamp ->
            restoreCountdown(timestamp)
        }
        savedStateHandle.get<Boolean>("agreementAccepted")?.let { accepted ->
            _uiState.update { it.copy(agreementAccepted = accepted) }
        }
    }

    fun onPhoneChanged(value: String) {
        _uiState.update { it.copy(phone = value, phoneAlreadyRegistered = false, registrationError = null) }
        validatePhoneField(value)
        savedStateHandle["phone"] = value
    }

    fun onSmsCodeChanged(value: String) {
        _uiState.update { it.copy(smsCode = value, smsCodeError = null, registrationError = null) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(password = value, registrationError = null) }
        validatePasswordField(value)
        if (_uiState.value.confirmPassword.isNotEmpty()) {
            validateConfirmPassword(_uiState.value.confirmPassword, value)
        }
    }

    fun onConfirmPasswordChanged(value: String) {
        _uiState.update { it.copy(confirmPassword = value, registrationError = null) }
        validateConfirmPassword(value, _uiState.value.password)
    }

    fun onAgreementToggled(accepted: Boolean) {
        _uiState.update { it.copy(agreementAccepted = accepted) }
        savedStateHandle["agreementAccepted"] = accepted
    }

    fun requestSmsCode() {
        val phone = _uiState.value.phone
        val result = validatePhone(phone)
        if (result.isFailure) {
            _uiState.update { it.copy(phoneError = result.exceptionOrNull()?.message) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSendingSms = true, registrationError = null) }

            // 先检查手机号是否已注册
            val checkResult = authRepository.checkPhone(phone)
            if (checkResult.isSuccess && checkResult.getOrNull()?.registered == true) {
                _uiState.update {
                    it.copy(
                        isSendingSms = false,
                        phoneAlreadyRegistered = true
                    )
                }
                return@launch
            }

            // 发送短信验证码
            when (val smsResult = authRepository.sendSms(phone)) {
                is Result.Success -> {
                    val data = smsResult.getOrNull()!!
                    val timestamp = System.currentTimeMillis()
                    _uiState.update {
                        it.copy(
                            isSendingSms = false,
                            smsCountdownSeconds = data.retryAfter,
                            smsSentTimestamp = timestamp
                        )
                    }
                    savedStateHandle["smsSentTimestamp"] = timestamp
                    startCountdown(data.retryAfter)
                }
                is Result.Failure -> {
                    _uiState.update {
                        it.copy(
                            isSendingSms = false,
                            registrationError = smsResult.exceptionOrNull()?.message ?: "发送失败"
                        )
                    }
                }
            }
        }
    }

    fun register() {
        val state = _uiState.value

        registerJob = viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, registrationError = null) }

            when (val result = authRepository.register(
                state.phone, state.smsCode, state.password
            )) {
                is Result.Success -> {
                    // 注册成功后尝试自动登录
                    try {
                        val loginResult = authRepository.login(state.phone, state.password)
                        if (loginResult.isSuccess) {
                            _uiState.update {
                                it.copy(isSubmitting = false, registrationSuccess = true)
                            }
                        } else {
                            // 注册成功但登录失败，降级处理：Token 已保存
                            _uiState.update {
                                it.copy(
                                    isSubmitting = false,
                                    registrationSuccess = true,
                                    registrationError = "注册成功，请手动登录"
                                )
                            }
                        }
                    } catch (e: Exception) {
                        // 注册成功但自动登录异常，Token 已在 register() 中保存
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                registrationSuccess = true,
                                registrationError = "注册成功，请手动登录"
                            )
                        }
                    }
                }
                is Result.Failure -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            registrationError = result.exceptionOrNull()?.message ?: "注册失败"
                        )
                    }
                }
            }
        }
    }

    fun cancelRequest() {
        registerJob?.cancel()
        countdownJob?.cancel()
        _uiState.update { it.copy(isSubmitting = false, isSendingSms = false) }
    }

    private fun validatePhoneField(phone: String) {
        val result = validatePhone(phone)
        _uiState.update {
            it.copy(
                phoneError = if (result.isFailure) result.exceptionOrNull()?.message else null
            )
        }
    }

    private fun validatePasswordField(password: String) {
        val result = validatePassword(password)
        _uiState.update {
            it.copy(
                passwordError = if (result.isFailure) result.exceptionOrNull()?.message else null
            )
        }
    }

    private fun validateConfirmPassword(confirmPassword: String, password: String) {
        val error = when {
            confirmPassword.isEmpty() -> null
            confirmPassword != password -> "两次输入的密码不一致"
            else -> null
        }
        _uiState.update { it.copy(confirmPasswordError = error) }
    }

    private fun startCountdown(totalSeconds: Int) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            var remaining = totalSeconds
            while (remaining > 0) {
                delay(1000)
                remaining--
                _uiState.update { it.copy(smsCountdownSeconds = remaining) }
            }
        }
    }

    private fun restoreCountdown(timestamp: Long) {
        val elapsed = (System.currentTimeMillis() - timestamp) / 1000
        val totalSeconds = 60
        val remaining = (totalSeconds - elapsed).toInt().coerceAtLeast(0)
        if (remaining > 0) {
            _uiState.update { it.copy(smsCountdownSeconds = remaining, smsSentTimestamp = timestamp) }
            startCountdown(remaining)
        }
    }

    override fun onCleared() {
        countdownJob?.cancel()
        registerJob?.cancel()
        super.onCleared()
    }
}
