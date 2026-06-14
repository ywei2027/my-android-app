package com.example.app.feature.register

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.app.feature.register.components.AgreementRow
import com.example.app.feature.register.components.PasswordField
import com.example.app.feature.register.components.PhoneNumberField
import com.example.app.feature.register.components.SmsCodeRow
import com.example.app.feature.register.components.SubmitOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onNavigateToLogin: (String) -> Unit,
    onNavigateToAgreement: () -> Unit,
    onRegistrationSuccess: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // 注册成功 → 跳转主页
    LaunchedEffect(uiState.registrationSuccess) {
        if (uiState.registrationSuccess) {
            onRegistrationSuccess()
        }
    }

    // 错误提示
    LaunchedEffect(uiState.registrationError) {
        uiState.registrationError?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    // 手机号已注册提示
    LaunchedEffect(uiState.phoneAlreadyRegistered) {
        if (uiState.phoneAlreadyRegistered) {
            snackbarHostState.showSnackbar("该手机号已注册")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("注册") },
                navigationIcon = {
                    IconButton(onClick = { onNavigateToLogin(uiState.phone) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.imePadding()
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(16.dp))

                // 手机号输入
                PhoneNumberField(
                    value = uiState.phone,
                    onValueChange = viewModel::onPhoneChanged,
                    error = uiState.phoneError
                )

                Spacer(Modifier.height(12.dp))

                // 验证码输入
                SmsCodeRow(
                    value = uiState.smsCode,
                    onValueChange = viewModel::onSmsCodeChanged,
                    phoneValid = uiState.isPhoneValid,
                    onRequestSms = viewModel::requestSmsCode,
                    countdownSeconds = uiState.smsCountdownSeconds,
                    isSending = uiState.isSendingSms
                )

                Spacer(Modifier.height(12.dp))

                // 密码输入
                PasswordField(
                    value = uiState.password,
                    onValueChange = viewModel::onPasswordChanged,
                    label = "设置密码",
                    error = uiState.passwordError,
                    modifier = Modifier.semantics { contentDescription = "设置密码" }
                )

                Spacer(Modifier.height(12.dp))

                // 确认密码输入
                PasswordField(
                    value = uiState.confirmPassword,
                    onValueChange = viewModel::onConfirmPasswordChanged,
                    label = "确认密码",
                    testTag = "field_confirm_password",
                    error = uiState.confirmPasswordError
                )

                Spacer(Modifier.height(8.dp))

                // 协议勾选
                AgreementRow(
                    accepted = uiState.agreementAccepted,
                    onToggle = viewModel::onAgreementToggled,
                    onNavigateAgreement = onNavigateToAgreement
                )

                Spacer(Modifier.height(16.dp))

                // 注册按钮
                Button(
                    onClick = viewModel::register,
                    enabled = uiState.canSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .semantics { testTag = "btn_register" }
                ) {
                    Text("注册")
                }

                Spacer(Modifier.height(12.dp))

                // 去登录
                TextButton(
                    onClick = { onNavigateToLogin(uiState.phone) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("已有账号？去登录")
                }

                Spacer(Modifier.height(16.dp))
            }

            // 提交遮罩层
            SubmitOverlay(
                isVisible = uiState.isSubmitting,
                onCancel = viewModel::cancelRequest
            )
        }
    }
}
