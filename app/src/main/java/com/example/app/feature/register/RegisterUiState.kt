package com.example.app.feature.register

data class RegisterUiState(
    // 输入字段
    val phone: String = "",
    val smsCode: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val agreementAccepted: Boolean = false,

    // 校验状态
    val phoneError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val smsCodeError: String? = null,

    // 验证码状态
    val smsCountdownSeconds: Int = 0,
    val isSendingSms: Boolean = false,
    val smsSentTimestamp: Long = 0,
    val smsCodeWrongAttempts: Int = 0,
    val phoneAlreadyRegistered: Boolean = false,

    // 注册状态
    val isSubmitting: Boolean = false,
    val registrationSuccess: Boolean = false,
    val registrationError: String? = null,

    // 派生属性
    val isPhoneValid: Boolean get() = phoneError == null && phone.length == 11
    val isPasswordValid: Boolean get() = passwordError == null && password.length in 8..20
    val isConfirmMatch: Boolean get() = confirmPasswordError == null && confirmPassword.isNotEmpty()
    val isFormValid: Boolean get() = isPhoneValid && smsCode.length == 6 &&
        isPasswordValid && isConfirmMatch && agreementAccepted
    val canRequestSms: Boolean get() = isPhoneValid && !isSendingSms
    val canSubmit: Boolean get() = isFormValid && !isSubmitting
)
