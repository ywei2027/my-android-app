package com.example.app.feature.register.validators

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ValidatePhoneUseCase @Inject constructor() {

    operator fun invoke(phone: String): Result<String> {
        val cleaned = phone.replace(" ", "")
        if (cleaned.isEmpty() || cleaned.length != 11 || cleaned[0] != '1' || !cleaned.all { it.isDigit() }) {
            return Result.failure(IllegalArgumentException("请输入正确的手机号"))
        }
        return Result.success(cleaned)
    }
}
