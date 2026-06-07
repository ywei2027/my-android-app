package com.example.myandroidapp.ui.auth

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myandroidapp.domain.model.LoginEvent
import com.example.myandroidapp.domain.model.LoginStatus

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    BackHandler {
        (context as? Activity)?.finish()
    }

    LaunchedEffect(uiState.status) {
        if (uiState.status == LoginStatus.Success) {
            onLoginSuccess()
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .testTag("login_screen"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier
                .weight(1f)
                .then(Modifier.height(0.dp)))

            // 应用图标
            Icon(
                imageVector = Icons.Filled.Email,
                contentDescription = "应用图标",
                modifier = Modifier
                    .size(LoginDimens.iconSize)
                    .testTag("login_app_icon"),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(LoginDimens.spacing2)) // 16dp

            // 标题
            Text(
                text = "欢迎回来",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("login_title")
            )

            Spacer(modifier = Modifier.height(LoginDimens.spacing1)) // 8dp

            // 副标题
            Text(
                text = "请使用邮箱和密码登录",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("login_subtitle")
            )

            Spacer(modifier = Modifier.height(LoginDimens.spacing4)) // 32dp

            // 邮箱输入框
            val isEmailError = !uiState.isEmailValid && uiState.email.isNotEmpty()
            OutlinedTextField(
                value = uiState.email,
                onValueChange = { viewModel.onEvent(LoginEvent.EmailChanged(it)) },
                placeholder = { Text("请输入邮箱地址") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Email,
                        contentDescription = "邮箱图标"
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                isError = isEmailError,
                supportingText = if (isEmailError) {
                    { Text("请输入有效的邮箱地址") }
                } else null,
                enabled = uiState.status != LoginStatus.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(LoginDimens.fieldHeight)
                    .padding(horizontal = LoginDimens.spacing3)
                    .testTag("email_field")
            )

            Spacer(modifier = Modifier.height(LoginDimens.spacing2)) // 16dp

            // 密码输入框
            OutlinedTextField(
                value = uiState.password,
                onValueChange = { viewModel.onEvent(LoginEvent.PasswordChanged(it)) },
                placeholder = { Text("请输入密码") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "密码图标"
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = { viewModel.onEvent(LoginEvent.TogglePasswordVisibility) },
                        modifier = Modifier.size(LoginDimens.touchTarget)
                    ) {
                        Icon(
                            imageVector = if (uiState.isPasswordVisible)
                                Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (uiState.isPasswordVisible) "隐藏密码" else "显示密码"
                        )
                    }
                },
                visualTransformation = if (uiState.isPasswordVisible)
                    VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        viewModel.onEvent(LoginEvent.SubmitLogin)
                    }
                ),
                singleLine = true,
                enabled = uiState.status != LoginStatus.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(LoginDimens.fieldHeight)
                    .padding(horizontal = LoginDimens.spacing3)
                    .testTag("password_field")
            )

            Spacer(modifier = Modifier.height(LoginDimens.spacing0)) // 0dp

            // 内联错误提示
            AnimatedVisibility(
                visible = uiState.errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = uiState.errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = LoginDimens.spacing3)
                        .testTag("error_message")
                )
            }

            Spacer(modifier = Modifier.height(LoginDimens.spacing3)) // 24dp

            // 登录按钮
            val isLoading = uiState.status == LoginStatus.Loading
            val isCooldown = uiState.status == LoginStatus.Cooldown
            val buttonEnabled = uiState.isEmailValid &&
                uiState.password.isNotEmpty() &&
                !isLoading &&
                !isCooldown

            Button(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.onEvent(LoginEvent.SubmitLogin)
                },
                enabled = buttonEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(LoginDimens.btnHeight)
                    .padding(horizontal = LoginDimens.spacing3)
                    .testTag("login_button"),
                shape = RoundedCornerShape(LoginDimens.btnRadius)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(LoginDimens.spnrSize),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.height(LoginDimens.spacing1))
                    Text(text = "登录中...")
                } else if (isCooldown) {
                    Text(text = "请等待 ${uiState.cooldownSeconds}s")
                } else {
                    Text(
                        text = "登录",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(LoginDimens.spacing3)) // 24dp

            // 测试账号提示
            Text(
                text = "测试账号: admin@example.com / 123456",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.testTag("login_hint")
            )

            Spacer(modifier = Modifier
                .weight(1f)
                .then(Modifier.height(0.dp)))
        }
    }
}
