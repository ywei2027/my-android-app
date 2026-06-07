package com.example.myandroidapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myandroidapp.data.repository.AuthRepository
import com.example.myandroidapp.domain.model.AuthResult
import com.example.myandroidapp.domain.model.LoginEvent
import com.example.myandroidapp.domain.model.LoginStatus
import com.example.myandroidapp.domain.model.LoginUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.EmailChanged -> onEmailChanged(event.email)
            is LoginEvent.PasswordChanged -> onPasswordChanged(event.password)
            LoginEvent.SubmitLogin -> login()
            LoginEvent.TogglePasswordVisibility -> togglePasswordVisibility()
            LoginEvent.ClearError -> clearError()
        }
    }

    private fun onEmailChanged(email: String) {
        val isValid = EMAIL_REGEX.matches(email) && email.length <= 254
        _uiState.update {
            it.copy(
                email = email,
                isEmailValid = isValid,
                errorMessage = null,
                status = if (email.isEmpty() && it.password.isEmpty()) LoginStatus.Idle else LoginStatus.Editing
            )
        }
    }

    private fun onPasswordChanged(password: String) {
        _uiState.update {
            it.copy(
                password = password,
                errorMessage = null,
                status = if (it.email.isEmpty() && password.isEmpty()) LoginStatus.Idle else LoginStatus.Editing
            )
        }
    }

    fun login() {
        viewModelScope.launch {
            // 原子竞态检查：使用 update 内部的 CAS 保证只有一次成功从 Editing 转到 Loading
            var acquired = false
            _uiState.update { current ->
                if (!current.isEmailValid ||
                    current.password.isEmpty() ||
                    current.status == LoginStatus.Loading ||
                    current.status == LoginStatus.Cooldown
                ) {
                    current
                } else {
                    acquired = true
                    current.copy(status = LoginStatus.Loading, errorMessage = null)
                }
            }
            if (!acquired) return@launch

            val currentState = _uiState.value
            Timber.d("login attempt: email=${currentState.email}")

            when (val result = authRepository.login(currentState.email, currentState.password)) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(status = LoginStatus.Success) }
                    Timber.d("login success: email=${result.data.user.email}")
                }
                is AuthResult.Error -> {
                    when (result.code) {
                        429 -> {
                            var countdown = 30
                            _uiState.update {
                                it.copy(
                                    status = LoginStatus.Cooldown,
                                    cooldownSeconds = countdown,
                                    errorMessage = result.message
                                )
                            }
                            try {
                                while (countdown > 0) {
                                    delay(1000)
                                    countdown--
                                    _uiState.update { it.copy(cooldownSeconds = countdown) }
                                }
                            } catch (_: CancellationException) {
                                // ViewModel.onCleared 时协程取消
                            } finally {
                                if (countdown > 0) {
                                    _uiState.update {
                                        it.copy(status = LoginStatus.Idle, cooldownSeconds = 0)
                                    }
                                }
                            }
                            _uiState.update { it.copy(status = LoginStatus.Idle, cooldownSeconds = 0) }
                        }
                        -1 -> {
                            _uiState.update {
                                it.copy(status = LoginStatus.Timeout, errorMessage = result.message)
                            }
                            Timber.d("login timeout or network error: ${result.message}")
                        }
                        else -> {
                            _uiState.update {
                                it.copy(status = LoginStatus.Error, errorMessage = result.message)
                            }
                            Timber.d("login failed: ${result.message}")
                        }
                    }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.update { LoginUiState() }
            Timber.d("logout completed")
        }
    }

    private fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    private fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
    }
}
