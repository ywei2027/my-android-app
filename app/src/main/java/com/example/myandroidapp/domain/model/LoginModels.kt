package com.example.myandroidapp.domain.model

sealed class LoginEvent {
    data class EmailChanged(val email: String) : LoginEvent()
    data class PasswordChanged(val password: String) : LoginEvent()
    object SubmitLogin : LoginEvent()
    object TogglePasswordVisibility : LoginEvent()
    object ClearError : LoginEvent()
}

enum class LoginStatus {
    Idle, Editing, Loading, Success, Error, Timeout, Cooldown
}

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isEmailValid: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val status: LoginStatus = LoginStatus.Idle,
    val errorMessage: String? = null,
    val cooldownSeconds: Int = 0
)
