package com.example.app.feature.register.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun SmsCodeRow(
    value: String,
    onValueChange: (String) -> Unit,
    phoneValid: Boolean,
    onRequestSms: () -> Unit,
    countdownSeconds: Int = 0,
    isSending: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                val digits = newValue.filter { it.isDigit() }.take(6)
                onValueChange(digits)
            },
            label = { Text("短信验证码") },
            placeholder = { Text("请输入验证码") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = "短信验证码" }
        )

        Button(
            onClick = onRequestSms,
            enabled = phoneValid && countdownSeconds == 0 && !isSending,
            modifier = Modifier.semantics { testTag = "btn_get_sms_code" }
        ) {
            val text = when {
                isSending -> "发送中..."
                countdownSeconds > 0 -> "${countdownSeconds}s后重试"
                else -> "获取验证码"
            }
            Text(text)
        }
    }
}
