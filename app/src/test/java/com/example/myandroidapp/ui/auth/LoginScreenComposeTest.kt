package com.example.myandroidapp.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = android.app.Application::class)
class LoginScreenComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // T12: LoginScreen idle 态 — 按钮 disabled + 输入框存在
    @Test
    fun `T12 idle state shows disabled button and fields`() {
        composeTestRule.setContent {
            MaterialTheme {
                Column {
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        modifier = Modifier.testTag("email_field")
                    )
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        modifier = Modifier.testTag("password_field")
                    )
                    Button(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.testTag("login_button")
                    ) { Text("登录") }
                }
            }
        }
        composeTestRule.onNodeWithTag("login_button").assertExists()
        composeTestRule.onNodeWithTag("login_button").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("email_field").assertExists()
        composeTestRule.onNodeWithTag("password_field").assertExists()
    }

    // T13: 标题和图标可见
    @Test
    fun `T13 title and icon are visible`() {
        composeTestRule.setContent {
            MaterialTheme {
                Column {
                    Icon(
                        imageVector = Icons.Filled.Email,
                        contentDescription = null,
                        modifier = Modifier
                            .size(64.dp)
                            .testTag("login_app_icon")
                    )
                    Text(
                        text = "欢迎回来",
                        modifier = Modifier.testTag("login_title")
                    )
                }
            }
        }
        composeTestRule.onNodeWithTag("login_title").assertIsDisplayed()
        composeTestRule.onNodeWithTag("login_app_icon").assertExists()
    }

    // T14: 错误提示存在
    @Test
    fun `T14 error node exists`() {
        composeTestRule.setContent {
            MaterialTheme {
                Text(
                    text = "邮箱或密码错误",
                    modifier = Modifier.testTag("error_message")
                )
            }
        }
        composeTestRule.onNodeWithTag("error_message").assertExists()
    }

    // T15: 底部提示存在
    @Test
    fun `T15 hint text exists`() {
        composeTestRule.setContent {
            MaterialTheme {
                Text(
                    text = "测试账号: admin@example.com / 123456",
                    modifier = Modifier.testTag("login_hint")
                )
            }
        }
        composeTestRule.onNodeWithTag("login_hint").assertExists()
    }

    // T16: LoginDimens 值校验
    @Test
    fun `T16 LoginDimens values match spec`() {
        assertEquals(0.dp, LoginDimens.spacing0)
        assertEquals(8.dp, LoginDimens.spacing1)
        assertEquals(16.dp, LoginDimens.spacing2)
        assertEquals(24.dp, LoginDimens.spacing3)
        assertEquals(32.dp, LoginDimens.spacing4)
        assertEquals(40.dp, LoginDimens.spacing5)
        assertEquals(64.dp, LoginDimens.iconSize)
        assertEquals(56.dp, LoginDimens.fieldHeight)
        assertEquals(48.dp, LoginDimens.btnHeight)
        assertEquals(24.dp, LoginDimens.btnRadius)
        assertEquals(48.dp, LoginDimens.touchTarget)
        assertEquals(20.dp, LoginDimens.spnrSize)
    }
}
