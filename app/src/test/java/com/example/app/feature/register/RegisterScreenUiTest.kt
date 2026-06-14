package com.example.app.feature.register

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * RegisterScreen Compose UI 测试
 *
 * 测试 UI 渲染、交互行为和可访问性
 * 对应 DESIGN.md §5.3
 *
 * 注意：这是 TDD RED 阶段——被测 Composable 组件尚不存在
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class RegisterScreenUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ============ 手机号格式化 ============

    @Test
    fun `phoneNumberField shows formatted text 3-4-4 pattern`() {
        composeTestRule.setContent {
            // TODO: STAGE_CODE 阶段替换为 RegisterScreen
            // 当前测试 PhoneNumberField 组件独立渲染
            // PhoneNumberField(value = "13812345678", onValueChange = {})
        }

        // 输入 "13812345678" 后应显示 "138 1234 5678"
        composeTestRule
            .onNodeWithText("138 1234 5678")
            .assertIsDisplayed()
    }

    // ============ 验证码按钮联动 ============

    @Test
    fun `smsCodeButton enabled when phone is valid`() {
        composeTestRule.setContent {
            // 手机号完整时，获取验证码按钮 should be enabled
            // SmsCodeRow(phoneValid = true, ...)
        }

        composeTestRule
            .onNodeWithTag("btn_get_sms_code")
            .assertIsEnabled()
    }

    @Test
    fun `smsCodeButton disabled when phone is invalid`() {
        composeTestRule.setContent {
            // 手机号不完整时，获取验证码按钮 should be disabled
            // SmsCodeRow(phoneValid = false, ...)
        }

        composeTestRule
            .onNodeWithTag("btn_get_sms_code")
            .assertIsNotEnabled()
    }

    // ============ 密码可见性切换 ============

    @Test
    fun `password visibility toggle switches between hidden and shown`() {
        composeTestRule.setContent {
            // PasswordField 包含独立的可见性切换按钮
            // PasswordField(...)
        }

        // 点击显示密码按钮
        composeTestRule
            .onNodeWithContentDescription("显示密码")
            .performClick()

        // 按钮文案应变为"隐藏密码"
        composeTestRule
            .onNodeWithContentDescription("隐藏密码")
            .assertIsDisplayed()
    }

    @Test
    fun `confirmPassword has independent visibility toggle`() {
        composeTestRule.setContent {
            // 确认密码框有独立 visibility toggle
            // PasswordField(..., testTag = "field_confirm_password")
        }

        composeTestRule
            .onNodeWithTag("field_confirm_password")
            .onNodeWithContentDescription("显示密码")
            .assertIsDisplayed()
    }

    // ============ 确认密码校验 ============

    @Test
    fun `confirmPassword mismatch shows error text`() {
        composeTestRule.setContent {
            // 两次密码不一致时显示红色提示
            // PasswordField(confirmPasswordError = "两次输入的密码不一致", ...)
        }

        composeTestRule
            .onNodeWithText("两次输入的密码不一致")
            .assertIsDisplayed()
    }

    // ============ 提交遮罩层 ============

    @Test
    fun `submit overlay shows when isSubmitting is true`() {
        composeTestRule.setContent {
            // 提交中显示遮罩层
            // SubmitOverlay(isVisible = true, onCancel = {})
        }

        composeTestRule
            .onNodeWithTag("overlay_submitting")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("注册提交中，请稍候")
            .assertIsDisplayed()
    }

    // ============ 无障碍 ============

    @Test
    fun `all interactive elements have contentDescription`() {
        composeTestRule.setContent {
            // RegisterScreen(...)
        }

        // 手机号输入框
        composeTestRule
            .onNodeWithContentDescription("手机号")
            .assertIsDisplayed()

        // 验证码输入框
        composeTestRule
            .onNodeWithContentDescription("短信验证码")
            .assertIsDisplayed()

        // 密码输入框
        composeTestRule
            .onNodeWithContentDescription("设置密码")
            .assertIsDisplayed()

        // 协议勾选
        composeTestRule
            .onNodeWithContentDescription("同意用户协议")
            .assertIsDisplayed()
    }

    // ============ 触摸目标 ============

    @Test
    fun `touch targets meet minimum 48dp size`() {
        composeTestRule.setContent {
            // RegisterScreen(...)
        }

        // 注册按钮 — 验证存在、启用，且在 STAGE_CODE 后验证 minHeight ≥ 48dp
        composeTestRule
            .onNodeWithTag("btn_register")
            .assertExists()

        // 获取验证码按钮
        composeTestRule
            .onNodeWithTag("btn_get_sms_code")
            .assertExists()

        // 协议勾选框
        composeTestRule
            .onNodeWithTag("checkbox_agreement")
            .assertExists()
    }

    // ============ 注册按钮联动 ============

    @Test
    fun `register button enabled when form is valid and agreement accepted`() {
        composeTestRule.setContent {
            // 全部字段有效 + 协议勾选 → 注册按钮 enabled
            // RegisterScreen(...)
        }

        composeTestRule
            .onNodeWithTag("btn_register")
            .assertIsEnabled()
    }
}
