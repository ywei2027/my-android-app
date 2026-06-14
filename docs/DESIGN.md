# 用户注册 — 技术方案 DESIGN.md

> 版本: v1.0
> 创建日期: 2026-06-14
> 阶段: STAGE_DESIGN
> 来源: PRD v1.2-confirmed + UI_DESIGN v0.2-review + DECISIONS v1.3
> 作者: Hermes 智能研发工作流

---

## 1. 架构概览

### 1.1 分层架构图

```
┌──────────────────────────────────────────────────────────────┐
│  UI Layer (Compose + Material3)                              │
│                                                              │
│  RegisterScreen ──── RegisterViewModel                       │
│       │                    │                                 │
│       │  StateFlow<        │                                 │
│       │  RegisterUiState>  │                                 │
│       │                    │                                 │
│  AgreementScreen           │                                 │
│  (WebView)                 │                                 │
└────────────────────────────┼─────────────────────────────────┘
                             │
┌────────────────────────────┼─────────────────────────────────┐
│  Domain Layer              │                                 │
│                            │                                 │
│  ValidatePhoneUseCase      │                                 │
│  ValidatePasswordUseCase   │                                 │
│  SendSmsCodeUseCase        │                                 │
│  RegisterUseCase           │                                 │
└────────────────────────────┼─────────────────────────────────┘
                             │
┌────────────────────────────┼─────────────────────────────────┐
│  Data Layer                │                                 │
│                            │                                 │
│  AuthRepository ───────────┤                                 │
│  ├─ SmsApiService (Retrofit)                                 │
│  ├─ AuthApiService (Retrofit)                                │
│  └─ TokenStorage (EncryptedSharedPreferences)                │
│                                                              │
│  UserPreferences (DataStore, 仅存非敏感偏好)                   │
└──────────────────────────────────────────────────────────────┘
```

### 1.2 数据流（一次完整注册）

```
┌─ 用户输入手机号 ─────────────────────────────────────────────┐
│  ViewModel.onPhoneChanged(value)                             │
│  → PhoneVisualTransformation 实时格式化 (3-4-4)               │
│  → 更新 UiState.phone + phoneValid                           │
└─────────────────────────────────────────────────────────────┘
                         ↓
┌─ 用户点击「获取验证码」 ──────────────────────────────────────┐
│  ViewModel.requestSmsCode()                                  │
│  1. ValidatePhoneUseCase → 格式校验 (1开头+11位)              │
│  2. AuthRepository.checkPhone(phone)                         │
│     ├─ 200 OK → 手机号可用                                    │
│     │   → AuthRepository.sendSms(phone)                      │
│     │   → 收到 expiresAt + retryAfter                        │
│     │   → 启动 countdownStateFlow(expiresAt)                  │
│     └─ 409 Conflict → UiState.phoneAlreadyRegistered = true │
│     ├─ 其他 4xx/5xx → UiState.error + Snackbar               │
└─────────────────────────────────────────────────────────────┘
                         ↓
┌─ 用户输入验证码+密码+确认密码+勾选协议 ────────────────────────┐
│  各字段触发实时的客户端校验：                                   │
│  - smsCode: ValidateSmsCodeUseCase (6位数字)                  │
│  - password: ValidatePasswordUseCase (8-20位+字母+数字)       │
│  - confirmPassword: 实时比对 password                         │
│  - agreementAccepted: Checkbox 联动                          │
│                                                              │
│  注册按钮 enabled 条件：                                       │
│  phoneValid ∧ smsCodeValid ∧ passwordValid                    │
│  ∧ confirmMatch ∧ agreementAccepted ∧ !isSubmitting          │
└─────────────────────────────────────────────────────────────┘
                         ↓
┌─ 用户点击「注册」 ───────────────────────────────────────────┐
│  ViewModel.register()                                        │
│  1. UpdateState → isSubmitting = true（遮罩 + loading）       │
│  2. AuthRepository.register(phone, smsCode, password)        │
│     ├─ 200 OK { token, user }                                │
│     │   → TokenStorage.save(token) — EncryptedSP 加密写入     │
│     │   → UiState.registrationSuccess = true                 │
│     │   → NavController.navigate("home") 清空回退栈           │
│     ├─ 400 → Snackbar("参数错误")                             │
│     ├─ 422 → Snackbar("验证码错误/已过期")                    │
│     ├─ 429 → Snackbar("请求过于频繁")                         │
│     ├─ 500 → Snackbar("服务器错误")                           │
│     ├─ ConnectException → Snackbar("网络超时，请重试")         │
│     └─ IOException → Snackbar("网络连接失败，请重试")          │
│  3. 错误时：isSubmitting = false，表单数据保留                 │
│                                                              │
│  异常降级：注册成功但 auto-login 失败                          │
│  → Token 已持久化 → Snackbar("注册成功，请手动登录")            │
│  → NavController.navigate("login")                           │
│  → App 下次启动时检测 Token 已存在 → 自动登录跳转主页           │
└─────────────────────────────────────────────────────────────┘
```

### 1.3 导航图

```
LoginScreen ←── TextButton("已有账号？去登录")
    ↑                        │
    │ 自动填入phone           │
    │                        ↓
    │              RegisterScreen
    │                        │
    │ 点击«用户协议»          ↓ 注册成功
    │              AgreementScreen ──→ HomeScreen
    │              (WebView)         (清空回退栈)
    │                        │
    └──────── 注册失败降级 ───┘
```

### 1.4 依赖清单

| 依赖 | 版本 | 用途 |
|------|------|------|
| Jetpack Compose BOM | 2024.x | UI 框架 |
| Material3 | 1.2.x | 组件库 |
| Navigation Compose | 2.7.x | 路由 & 传参 |
| Hilt | 2.50+ | 依赖注入 |
| Retrofit + OkHttp | 2.9+ / 4.12+ | HTTP 客户端 |
| kotlinx.serialization | 1.6+ | JSON 序列化 |
| EncryptedSharedPreferences | 1.1.1 | Token 安全存储 |
| DataStore | 1.0+ | 非敏感数据持久化 |
| AndroidX Lifecycle | 2.7+ | ViewModel + SavedStateHandle |
| Coil | 2.5+ | 图片加载（如需要） |

---

## 2. 模块设计

### 2.1 项目结构

```
app/src/main/java/com/example/app/
├── feature/
│   ├── register/
│   │   ├── RegisterScreen.kt          // 注册主页面 Composable
│   │   ├── RegisterViewModel.kt       // 注册 ViewModel
│   │   ├── RegisterUiState.kt         // UI 状态数据类
│   │   ├── AgreementScreen.kt         // 协议 WebView 页面
│   │   ├── components/
│   │   │   ├── PhoneNumberField.kt    // 手机号输入 + 自动格式化
│   │   │   ├── SmsCodeRow.kt          // 验证码输入行 + 获取按钮
│   │   │   ├── PasswordField.kt       // 密码输入 + 可见性切换
│   │   │   ├── AgreementRow.kt        // 协议勾选 + 链接
│   │   │   ├── SubmitOverlay.kt       // 提交遮罩层
│   │   │   └── PhoneVisualTransformation.kt // 3-4-4 格式化
│   │   └── validators/
│   │       ├── PhoneValidator.kt      // 手机号格式校验
│   │       └── PasswordValidator.kt   // 密码强度校验
│   │
│   └── auth/
│       └── LoginScreen.kt             // 已有，注册后导航目标
│
├── domain/
│   ├── usecase/
│   │   ├── ValidatePhoneUseCase.kt
│   │   ├── ValidatePasswordUseCase.kt
│   │   ├── SendSmsCodeUseCase.kt
│   │   └── RegisterUseCase.kt
│   └── model/
│       ├── PhoneNumber.kt             // value class
│       ├── Password.kt                // value class
│       └── SmsVerificationCode.kt     // value class
│
├── data/
│   ├── repository/
│   │   └── AuthRepository.kt          // 注册+登录+Token 持久化
│   ├── remote/
│   │   ├── SmsApiService.kt           // Retrofit SMS 接口
│   │   └── AuthApiService.kt          // Retrofit 注册登录接口
│   ├── local/
│   │   └── TokenStorage.kt            // EncryptedSP 封装
│   └── model/
│       ├── SendSmsRequest.kt
│       ├── RegisterRequest.kt
│       ├── ApiResponse.kt
│       └── RegisterData.kt
│
├── di/
│   └── RegisterModule.kt              // Hilt 模块
│
└── navigation/
    └── NavGraph.kt                     // 注册相关路由注册
```

### 2.2 RegisterUiState 设计

```kotlin
data class RegisterUiState(
    // 输入字段
    val phone: String = "",
    val smsCode: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val agreementAccepted: Boolean = false,

    // 校验状态
    val phoneError: String? = null,           // "请输入正确的手机号"
    val passwordError: String? = null,        // "密码需8-20位，含字母和数字"
    val confirmPasswordError: String? = null, // "两次输入的密码不一致"
    val smsCodeError: String? = null,         // "请输入6位数字验证码"

    // 验证码状态
    val smsCountdownSeconds: Int = 0,         // >0 倒计时中，==0 可获取
    val isSendingSms: Boolean = false,         // 验证码发送中
    val smsSentTimestamp: Long = 0,           // 服务器返回的发送时间戳
    val smsCodeWrongAttempts: Int = 0,        // 连续错误次数(0-3)
    val phoneAlreadyRegistered: Boolean = false,

    // 注册状态
    val isSubmitting: Boolean = false,         // 注册提交中
    val registrationSuccess: Boolean = false,  // 注册成功
    val registrationError: String? = null,     // 注册失败信息

    // 密码可见性（局部状态，由 Composable rememberSaveable 管理）
    // 不在此处定义，避免 SavedStateHandle 泄露明文

    // 派生属性
    val isPhoneValid: Boolean get() = phoneError == null && phone.length == 11
    val isPasswordValid: Boolean get() = passwordError == null && password.length in 8..20
    val isConfirmMatch: Boolean get() = confirmPasswordError == null && confirmPassword.isNotEmpty()
    val isFormValid: Boolean get() = isPhoneValid && smsCode.length == 6 &&
        isPasswordValid && isConfirmMatch && agreementAccepted
    val canRequestSms: Boolean get() = isPhoneValid && !isSendingSms
    val canSubmit: Boolean get() = isFormValid && !isSubmitting
)
```

### 2.3 RegisterViewModel 核心事件

```kotlin
@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val validatePhone: ValidatePhoneUseCase,
    private val validatePassword: ValidatePasswordUseCase,
    private val sendSmsCode: SendSmsCodeUseCase,
    private val register: RegisterUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    // 倒计时 Job，绑定 viewModelScope
    private var countdownJob: Job? = null

    init {
        // 恢复 SavedStateHandle 中的持久化字段
        savedStateHandle.get<String>("phone")?.let { phone ->
            _uiState.update { it.copy(phone = phone) }
        }
        savedStateHandle.get<Long>("smsSentTimestamp")?.let { timestamp ->
            restoreCountdown(timestamp)
        }
        savedStateHandle.get<Boolean>("agreementAccepted")?.let { accepted ->
            _uiState.update { it.copy(agreementAccepted = accepted) }
        }
    }

    fun onPhoneChanged(value: String) { /* ... */ }
    fun onSmsCodeChanged(value: String) { /* ... */ }
    fun onPasswordChanged(value: String) { /* ... */ }
    fun onConfirmPasswordChanged(value: String) { /* ... */ }
    fun onAgreementToggled(accepted: Boolean) { /* ... */ }
    fun requestSmsCode() { /* ... */ }
    fun register() { /* ... */ }
    fun cancelRequest() { /* 取消当前注册协程 */ }

    override fun onCleared() {
        countdownJob?.cancel()
        super.onCleared()
    }
}
```

### 2.4 核心组件规格

#### PhoneVisualTransformation

```
功能: 3-4-4 手机号格式化 (如 "13812345678" → "138 1234 5678")
实现: VisualTransformation + OffsetMapping
  - 插入: 在 index 3 和 7 后插入空格
  - 删除: 光标遇到空格时，实际删除前一位数字
  - 全选/替换: 正确映射 offset
  - 仅保留原始 11 位数字，空格仅在展示层存在
```

#### CountdownState

```
功能: 60 秒倒计时，基于服务器返回的 expiresAt 计算
实现:
  1. ViewModel 保存 smsSentTimestamp (millis)
  2. countdownJob = viewModelScope.launch(Dispatchers.Default) {
       while (remaining > 0) {
         _uiState.update { it.copy(smsCountdownSeconds = remaining) }
         delay(1000)
       }
     }
  3. 配置变更恢复: ViewModel init{} 中检测 smsSentTimestamp 非零 → restoreCountdown()
  4. 杀进程后: smsSentTimestamp 从 SavedStateHandle 读取但已过期（基于 server 时间），
     恢复为 0 — 按钮恢复可点击
  5. 按钮文案: countdown > 0 → "NNs后重试"; countdown == 0 且已发送过 → "重新获取";
     未发送过 → "获取验证码"
```

#### SubmitOverlay

```
功能: 注册提交时的全屏遮罩层
实现:
  AnimatedVisibility(visible = isSubmitting) {
      Box(
          modifier = Modifier.fillMaxSize()
              .background(Color.Black.copy(alpha = 0.6f)),
          contentAlignment = Alignment.Center
      ) {
          Column(horizontalAlignment = CenterHorizontally) {
              CircularProgressIndicator(color = Color.White)
              Spacer(Modifier.height(16.dp))
              Text("注册提交中，请稍候", color = Color.White)
          }
      }
  }
位置: Scaffold 内容区 Box 的最上层，覆盖 TopAppBar
返回键: BackHandler { viewModel.cancelRequest() }
     → 取消 Retrofit Call + ViewModel 协程
     → isSubmitting = false，表单数据保留
```

#### AgreementRow

```
功能: 协议勾选 + 可点击链接
实现 (修正 P0 UR-01/UR-05):
  Row(Modifier.fillMaxWidth().clickable { toggle() }, verticalAlignment = CenterVertically) {
      Checkbox(
          checked = accepted,
          onCheckedChange = null,  // 不独立响应，由整行 clickable 处理
          modifier = Modifier.minimumTouchTargetSize() // 48dp
      )
      Text("我已阅读并同意")
      TextButton(onClick = { navigateToAgreement() }) {
          Text("《用户协议》")
      }
  }
无障碍: 整行 Row contentDescription = "同意用户协议"; TextButton 独立可聚焦
```

#### AgreementScreen

```
功能: 加载用户协议
实现:
  AndroidView(factory = { WebView(context).apply {
      settings.javaScriptEnabled = false  // 安全：关闭 JS
      loadUrl(AGREEMENT_REMOTE_URL)
      webViewClient = AgreementWebViewClient() // 包含降级逻辑
  } })
  DisposableEffect(webView) {
      onDispose { webView.destroy() }  // 内存泄漏修复 (P0 UR-04)
  }

降级策略:
  1. 尝试远程 URL (AGREEMENT_REMOTE_URL = "/api/v1/agreement")
  2. 5s 超时 → 降级到本地 assets/agreement.html
  3. 本地也失败 → Snackbar "协议加载失败，请重试" + 重试按钮
```

---

## 3. 接口定义

### 3.1 API 接口

所有路径使用 `/api/v1/` 前缀。Base URL 在 BuildConfig 中配置。

#### 3.1.1 检查手机号是否已注册

```
POST /api/v1/auth/check-phone
Request:
  Content-Type: application/json
  {
    "phone": "13812345678"   // 11位数字，无格式符
  }

Response 200 — 未注册:
  {
    "code": 200,
    "data": { "registered": false }
  }

Response 409 — 已注册:
  {
    "code": 409,
    "message": "该手机号已注册"
  }
```

#### 3.1.2 发送短信验证码

```
POST /api/v1/auth/send-sms
Request:
  {
    "phone": "13812345678"
  }

Response 200:
  {
    "code": 200,
    "data": {
      "expiresAt": "2026-06-14T12:15:00+08:00",
      "retryAfter": 60
    }
  }

Response 429 — 频率限制:
  {
    "code": 429,
    "message": "请求过于频繁，请稍后再试"
  }

Response 500 — 服务端错误:
  {
    "code": 500,
    "message": "短信服务暂时不可用"
  }
```

#### 3.1.3 注册

```
POST /api/v1/auth/register
Request:
  {
    "phone": "13812345678",
    "smsCode": "123456",
    "password": "Abc12345"
  }

Response 200 — 成功:
  {
    "code": 200,
    "data": {
      "token": "eyJhbGciOi...",
      "user": {
        "userId": "usr_abc123",
        "phone": "13812345678"
      }
    }
  }

Response 400 — 参数错误:
  { "code": 400, "message": "手机号格式不正确" }

Response 409 — 手机号已注册 (race condition):
  { "code": 409, "message": "该手机号已注册" }

Response 422 — 验证码错误/过期:
  { "code": 422, "message": "验证码已过期，请重新获取" }

Response 429 — 频率限制:
  { "code": 429, "message": "请求过于频繁" }

Response 500 — 服务端错误:
  { "code": 500, "message": "服务器内部错误" }
```

#### 3.1.4 自动登录（注册成功后调用）

```
POST /api/v1/auth/login
Request:
  {
    "phone": "13812345678",
    "password": "Abc12345"
  }
  // 或使用 token 免密登录:
  // Authorization: Bearer {token}

Response 200:
  {
    "code": 200,
    "data": {
      "token": "eyJhbGciOi...",
      "user": { "userId": "usr_abc123", "phone": "13812345678" }
    }
  }
```

### 3.2 Kotlin 数据模型

```kotlin
// request
@Serializable
data class CheckPhoneRequest(val phone: String)

@Serializable
data class SendSmsRequest(val phone: String)

@Serializable
data class RegisterRequest(
    val phone: String,
    val smsCode: String,
    val password: String
)

// response
@Serializable
data class ApiResponse<T>(
    val code: Int,
    val data: T? = null,
    val message: String? = null
)

@Serializable
data class CheckPhoneData(val registered: Boolean)

@Serializable
data class SendSmsData(
    val expiresAt: String,
    val retryAfter: Int
)

@Serializable
data class RegisterData(
    val token: String,
    val user: UserBrief
)

@Serializable
data class UserBrief(
    val userId: String,
    val phone: String
)
```

### 3.3 Retrofit Service 定义

```kotlin
interface SmsApiService {
    @POST("api/v1/auth/check-phone")
    suspend fun checkPhone(@Body request: CheckPhoneRequest): ApiResponse<CheckPhoneData>

    @POST("api/v1/auth/send-sms")
    suspend fun sendSms(@Body request: SendSmsRequest): ApiResponse<SendSmsData>
}

interface AuthApiService {
    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<RegisterData>

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<RegisterData>
}
```

### 3.4 AuthRepository 接口

```kotlin
class AuthRepository @Inject constructor(
    private val smsApi: SmsApiService,
    private val authApi: AuthApiService,
    private val tokenStorage: TokenStorage
) {
    suspend fun checkPhone(phone: String): Result<CheckPhoneData>
    suspend fun sendSms(phone: String): Result<SendSmsData>
    suspend fun register(phone: String, smsCode: String, password: String): Result<RegisterData>
    suspend fun login(phone: String, password: String): Result<RegisterData>
    suspend fun loginWithToken(token: String): Result<RegisterData>
    fun saveToken(token: String)
    fun getToken(): String?
    fun clearToken()
}
```

### 3.5 导航路由定义

```kotlin
// NavGraph.kt
sealed class Route(val path: String) {
    object Register : Route("register")
    object Agreement : Route("agreement")
    data class Login(val phone: String = "") : Route("login?phone={phone}") {
        companion object {
            const val ROUTE_PATTERN = "login?phone={phone}"
            val arguments = listOf(navArgument("phone") {
                type = NavType.StringType; defaultValue = ""
            })
        }
    }
    object Home : Route("home")
}

// RegisterScreen.kt
composable(Route.Register.path) {
    RegisterScreen(
        onNavigateToLogin = { phone ->
            navController.navigate("login?phone=$phone") {
                popUpTo("register") { inclusive = true }
            }
        },
        onNavigateToAgreement = { navController.navigate("agreement") },
        onRegistrationSuccess = {
            navController.navigate("home") {
                popUpTo(0) { inclusive = true }
            }
        }
    )
}
```

---

## 4. 安全考虑

### 4.1 数据传输安全

| 措施 | 实现 |
|------|------|
| 网络传输 | 全量 HTTPS（TLS 1.2+），OkHttp 配置 certificatePinner |
| 敏感数据 | 密码、验证码、Token 仅通过 POST Body 传输，不放入 URL |
| 日志脱敏 | OkHttp LoggingInterceptor 对 `password`、`smsCode`、`token` 字段脱敏（替换为 `***`） |

```kotlin
// OkHttpClient 配置
OkHttpClient.Builder()
    .certificatePinner(CertificatePinner.Builder()
        .add("api.example.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAA=")
        .build())
    .addInterceptor(SensitiveDataMaskingInterceptor()) // 自定义脱敏
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .build()
```

### 4.2 本地存储安全

| 数据 | 存储方式 | 原因 |
|------|---------|------|
| AuthToken | `EncryptedSharedPreferences` | Token 可代表用户身份，泄露=账号劫持 |
| 手机号 | `SavedStateHandle` (内存+Bundle) | 非敏感数据，仅跨配置变更保留 |
| smsSentTimestamp | `SavedStateHandle` | 仅倒计时恢复用，无安全风险 |
| agreementAccepted | `SavedStateHandle` | 无安全风险 |
| 密码 | 不在任何持久化存储中保留 | 明文密码泄露风险（P0 安全红线） |

Token 存储实现:

```kotlin
@Singleton
class TokenStorage @Inject constructor(@ApplicationContext context: Context) {
    private val sharedPrefs = EncryptedSharedPreferences.create(
        "auth_tokens",
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveToken(token: String) {
        sharedPrefs.edit().putString("auth_token", token).apply()
    }

    fun getToken(): String? = sharedPrefs.getString("auth_token", null)

    fun clearToken() {
        sharedPrefs.edit().remove("auth_token").apply()
    }
}
```

### 4.3 客户端校验

| 校验项 | 规则 | 位置 |
|--------|------|------|
| 手机号格式 | `^1[0-9]{10}$`（11 位，1 开头） | ValidatePhoneUseCase |
| 验证码格式 | `^[0-9]{6}$`（仅 6 位数字） | 输入框 keyboardType=Number + maxLength=6 |
| 密码规则 | 8-20 位，含字母+数字 | ValidatePasswordUseCase |
| 特殊字符 | 不拦截（@#$% 等放行） | 前端仅校验上述规则 |

**重要:** 客户端校验是 UX 辅助，不可作为安全边界。所有校验必须在服务端再次执行。

### 4.4 防刷策略

| 措施 | 位置 | 说明 |
|------|------|------|
| 验证码发送限频 | 后端 | 同手机号 1 次/分钟；同 IP 10 次/小时 |
| 验证码错误上限 | 客户端/后端 | 连续 3 次错误 → 提示重新获取 |
| 注册按钮防重 | 客户端 | isSubmitting 状态 + 按钮 disabled |
| 请求去重 | Retrofit | 同一请求未完成前不发起新请求 |

### 4.5 ProGuard 规则

```proguard
# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class com.example.app.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# 不混淆敏感数据类（便于安全审计）
-keep class com.example.app.data.remote.** { *; }
-keep class com.example.app.data.local.TokenStorage { *; }
```

### 4.6 WebView 安全

```kotlin
// AgreementScreen WebView 安全配置
webView.settings.apply {
    javaScriptEnabled = false          // 协议页禁用 JS
    allowFileAccess = false            // 禁止文件系统访问
    allowContentAccess = false         // 禁止 content:// 访问
    mixedContentMode = MIXED_CONTENT_NEVER_ALLOW  // 仅 HTTPS
    cacheMode = LOAD_NO_CACHE          // 不缓存敏感内容
}
```

---

## 5. 测试策略

### 5.1 测试金字塔

```
           ┌─────────┐
           │  E2E    │  2 场景: 完整注册流程 × 已注册拦截
           │ (0.5h)  │
          ┌─┴─────────┴─┐
          │ 集成测试     │  6 场景: API mock 验证 ViewModel 状态转换
          │  (1.5h)     │
        ┌─┴─────────────┴─┐
        │  单元测试        │  18 场景: Validator / UseCase / Repository
        │   (3h)          │
       ┌┴─────────────────┴┐
       │  UI 测试          │  8 场景: Compose testing + 无障碍检查
       │  (1h)            │
      └────────────────────┘
             总计: ~6h
```

### 5.2 单元测试 (18 场景)

| 被测对象 | 场景数 | 关键场景 |
|---------|:-----:|---------|
| ValidatePhoneUseCase | 5 | 合法11位 / 非1开头 / 不足11位 / 空输入 / 含空格 |
| ValidatePasswordUseCase | 5 | 8位含字母数字 / 纯数字 / 不足8位 / 超20位 / 含特殊字符放行 |
| AuthRepository | 5 | 注册200 / 注册409 / 网络异常 / Sms发送成功 / Sms发送失败 |
| ViewModel 状态转换 | 3 | 输入手机号后状态更新 / 密码不一致 / 协议未勾选禁用按钮 |

```kotlin
// 示例: ValidatePhoneUseCase 测试
@Test
fun `valid 11-digit phone starting with 1 returns success`() {
    val result = validatePhone("13812345678")
    assertTrue(result.isSuccess)
}

@Test
fun `phone not starting with 1 returns error`() {
    val result = validatePhone("23312345678")
    assertTrue(result.isFailure)
    assertEquals("请输入正确的手机号", result.exceptionOrNull()?.message)
}
```

### 5.3 UI 测试 (8 场景)

| 场景 | 验证点 |
|------|--------|
| 手机号输入格式化 | 输入 "13812345678" → 显示 "138 1234 5678" |
| 获取验证码按钮联动 | 手机号完整 → enabled; 不完整 → disabled |
| 密码可见性切换 | 两次点击切换明文/密文，两个密码框独立 |
| 确认密码校验 | 不一致时显示红色提示 |
| 注册按钮联动 | 全部有效 + 协议勾选 → enabled |
| 提交遮罩层 | 点击注册 → 遮罩出现；系统返回 → 取消 |
| 无障碍 contentDescription | 所有交互元素存在且正确 |
| 触摸目标尺寸 | Checkbox/Button/TextButton ≥ 48dp |

```kotlin
// 示例: 手机号格式化 UI 测试
@Test
fun phoneNumberField_showsFormattedText() {
    composeTestRule.setContent {
        PhoneNumberField(value = "13812345678", onValueChange = {})
    }
    composeTestRule
        .onNodeWithText("138 1234 5678")
        .assertIsDisplayed()
}
```

### 5.4 集成测试 (6 场景)

使用 MockWebServer 或 Hilt 测试替换，验证 ViewModel 完整流程。

| 场景 | 测试内容 |
|------|---------|
| 完整注册成功 | mock 200 → UiState.registrationSuccess = true |
| 手机号已注册拦截 | mock 409 → UiState.phoneAlreadyRegistered = true |
| 验证码发送成功 | mock 200 → smsCountdownSeconds = 60 |
| 验证码发送失败 | mock 500 → Snackbar 提示 + 不启动倒计时 |
| 注册网络超时 | mock ConnectException → 提示超时信息 |
| 注册成功登录失败降级 | mock 注册200 + 登录500 → 降级到 LoginScreen |

```kotlin
// 示例: ViewModel 集成测试
@Test
fun `register success triggers navigation to home`() = runTest {
    val viewModel = RegisterViewModel(
        validatePhone, validatePassword,
        sendSmsCode, registerUseCase, savedStateHandle
    )
    viewModel.onPhoneChanged("13812345678")
    viewModel.onSmsCodeChanged("123456")
    viewModel.onPasswordChanged("Abc12345")
    viewModel.onConfirmPasswordChanged("Abc12345")
    viewModel.onAgreementToggled(true)

    viewModel.register()

    advanceUntilIdle()
    val state = viewModel.uiState.value
    assertTrue(state.registrationSuccess)
    assertFalse(state.isSubmitting)
}
```

### 5.5 E2E 测试 (2 场景)

使用 Espresso + Compose 在真机/模拟器上执行:

1. 新用户完整注册 → 跳转主页
2. 已注册手机号 → 提示已注册 → 跳转登录页

### 5.6 测试覆盖目标

| 指标 | 目标 |
|------|:----:|
| 行覆盖率 | ≥ 80% |
| 分支覆盖率 | ≥ 70% |
| UseCase 覆盖率 | 100% |
| ViewModel 状态转换覆盖率 | 100% |
| API 错误路径覆盖 | 100% |

### 5.7 安全测试检查点

| 检查项 | 方法 |
|--------|------|
| Token 仅存 EncryptedSharedPreferences | 代码审查 + 单元测试验证 DataStore 不含 Token |
| 密码不写入 SavedStateHandle | 代码审查确认 password 字段不在 SavedStateHandle 初始化列表中 |
| HTTPS 证书固定 | 使用错误证书的服务端验证连接失败 |
| 日志脱敏 | grep 构建日志确认无明文密码/Token 输出 |
| ProGuard 规则 | 混淆后打包运行，确认 kotlinx.serialization 正常工作 |

---

## 6. ADR 决策记录

### ADR-001: Token 安全存储方案

**背景:**
注册成功后服务端返回 Bearer Token，用于后续所有认证请求。Token 泄露 = 账号劫持，必须安全存储。

**决策:**
使用 Android `EncryptedSharedPreferences`（AES-256 GCM）存储 AuthToken。禁止使用 DataStore 或普通 SharedPreferences 明文存储。

**后果:**
- 正面: 即使 root 设备也无法直接读取 Token 明文（需解密主密钥）
- 正面: 与 Android Keystore 集成，硬件级安全
- 负面: 需要 Android 6.0+（满足 minSdk 26）
- 负面: 首次创建 MasterKey 有微小延迟（~50ms）
- 约束: TokenStorage 必须在 Application 初始化后使用（依赖 Context）

**状态:** 已确认（PRD 第1轮评审 D-01，2026-06-13）

---

### ADR-002: 倒计时实现方案

**背景:**
发送验证码后需要 60 秒倒计时，用户旋转屏幕或退到后台后计时不能重置，杀进程后重新进入倒计时应失效。

**决策:**
使用 ViewModel 作用域协程 + StateFlow<Int> + 基于服务器返回的 `expiresAt` 时间戳计算剩余秒数。

**否决方案:** LaunchedEffect — Composable 组合树变更时（如横竖屏切换）会导致 LaunchedEffect 重启，倒计时重置。

**后果:**
- 正面: ViewModel 不随配置变更销毁，倒计时天然稳定
- 正面: 基于服务器时间戳恢复，杀进程后自动失效
- 正面: onCleared() 自动取消协程，无泄漏
- 负面: 需要额外处理 init{} 中恢复倒计时协程的逻辑
- 约束: ViewModel 必须持有 countdownJob 引用以支持取消

**状态:** 已确认（UI 设计决议 UD-03，PRD 第2轮 R2-D-24，2026-06-13/14）

---

### ADR-003: 错误处理与反馈策略

**背景:**
注册流程涉及多阶段交互（手机号校验 → 验证码获取 → 密码设置 → 提交），需要明确的错误分类和反馈机制。

**决策:**
采用双层错误反馈策略：
- 字段级校验错误 → `supportingText`（内联红色提示，如 "两次密码不一致"）
- 全局 API 错误 / 网络错误 → `Snackbar` + action 按钮（如 "重试"、"去登录"）

网络错误细分：
- `ConnectException` / `SocketTimeoutException(15s)` → Snackbar "网络超时，请检查网络后重试"
- `IOException` (非超时) → Snackbar "网络连接失败，请重试"
- 4xx/5xx → Snackbar 展示后端返回的 message

**后果:**
- 正面: 用户可明确区分输入错误和网络问题
- 正面: Snackbar action 提供快速恢复路径
- 负面: 需要维护 SnackbarHostState 与键盘的协调（imePadding + Snackbar 层级）
- 约束: 所有 API 调用统一通过 Repository 层包装为 Result<T>，ViewModel 按异常类型分发

**状态:** 已确认（PRD 第1轮 D-04，2026-06-13）

---

### ADR-004: SavedStateHandle 安全边界

**背景:**
Android 系统在进程被杀后会保存 SavedStateHandle 到 Bundle 中，恢复时反序列化。Bundle 数据以明文存储在系统进程中，可能被其他应用通过 IPC 读取。

**决策:**
SavedStateHandle 仅持久化非敏感字段：`phone`（手机号）、`smsSentTimestamp`（发送时间戳）、`agreementAccepted`（协议勾选状态）。`password` 明文严禁持久化。

**后果:**
- 正面: 用户密码从不在系统 Bundle 中明文存储
- 正面: 手机号、协议勾选状态在配置变更/进程恢复时保留，减少用户重复输入
- 负面: 进程恢复后用户需重新输入密码（但这是安全 vs 便利的正确取舍）
- 安全验证: 代码审查必须确认 ViewModel init{} 中无 password 字段恢复逻辑

**状态:** 已确认（PRD 第2轮 R2-D-23，覆盖 D-07，2026-06-13）

---

### ADR-005: SMS 自动填充降级策略

**背景:**
Android SMS Retriever API 依赖 Google Play Services，在华为/小米等无 GMS 设备上不可用。

**决策:**
优先尝试 SMS Retriever API 自动填充验证码，不可用时降级为手动输入模式（支持粘贴，利用 Android 自动提取 SMS 码到剪贴板的特性）。

**后果:**
- 正面: GMS 设备用户体验流畅（自动填充）
- 正面: 无 GMS 设备用户可通过系统 SMS 提取 + 粘贴完成验证码输入
- 负面: 国内品牌无 GMS 设备占据相当比例，多数用户走手动输入路径
- 约束: 需在代码中检测 GMS 可用性（GoogleApiAvailability.isGooglePlayServicesAvailable）

**状态:** 已确认（PRD 第2轮 R2-D-21 + AC-27，2026-06-13）

---

### ADR-006: 依赖注入架构

**背景:**
项目使用 Hilt 进行依赖注入，需确定注册模块的 DI 作用域。

**决策:**
- `AuthRepository` → `@Singleton`（全局唯一，管理 Token 存储）
- `SmsApiService` / `AuthApiService` → `@Singleton`（Retrofit 单例）
- `ValidatePhoneUseCase` / `ValidatePasswordUseCase` → `@Singleton`（无状态）
- `RegisterViewModel` → `@HiltViewModel`（绑定 NavBackStackEntry 生命周期）
- `TokenStorage` → `@Singleton`

**后果:**
- 正面: 单例化的 Repository 和 Service 避免重复创建
- 正面: ViewModel 作用域正确绑定导航生命周期
- 约束: Hilt 模块需在 Application 类中正确配置 `@HiltAndroidApp`

**状态:** 已确认（PRD §6 技术约束，2026-06-13）

---

## 7. 变更记录

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| v1.0 | 2026-06-14 | 初始版本：架构概览、模块设计、接口定义、安全考虑、测试策略、6 项 ADR |
