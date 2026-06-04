package com.example.myandroidapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myandroidapp.ui.components.VersionTag
import com.example.myandroidapp.ui.login.LoginViewModel
import com.example.myandroidapp.ui.util.isKeyboardVisible
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val keyboardVisible = isKeyboardVisible()
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .imePadding() // §4: IME 避让，键盘弹出时内容自动收起
                    ) {
                        LoginScreen(modifier = Modifier.fillMaxSize())
                        // §3 交互状态机: 键盘弹出 → 隐藏版本号，键盘收起 → 淡入显示
                        AnimatedVisibility(visible = !keyboardVisible) {
                            VersionTag(
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel()
) {
    var phone by remember { mutableStateOf("") }

    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = "登录",
            style = MaterialTheme.typography.headlineMedium
        )
        TextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("手机号") },
            modifier = Modifier.padding(top = 8.dp)
        )
        Button(
            onClick = { viewModel.login(phone) },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("获取验证码")
        }
    }
}
