package com.example.app.feature.register.validators

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ValidatePasswordUseCase @Inject constructor() {

    operator fun invoke(password: String): Result<String> {
        if (password.length !in 8..20) {
            return Result.failure(IllegalArgumentException("密码需8-20位，含字母和数字"))
        }
        val hasLetter = password.any { it.isLetter() }
        val hasDigit = password.any { it.isDigit() }
        if (!hasLetter || !hasDigit) {
            return Result.failure(IllegalArgumentException("密码需8-20位，含字母和数字"))
        }
        return Result.success(password)
    }
}
