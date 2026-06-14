package com.example.app.feature.register.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun PhoneNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    error: String? = null,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            // 只允许输入数字，最多 11 位
            val digits = newValue.filter { it.isDigit() }.take(11)
            onValueChange(digits)
        },
        label = { Text("手机号") },
        placeholder = { Text("请输入手机号") },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        visualTransformation = PhoneVisualTransformation(),
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "手机号" }
    )
}

// @Preview removed — ui-tooling-preview not in compile classpath. Preview in Android Studio.
