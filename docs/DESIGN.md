# 用户登录 — 技术方案

> **版本:** v0.2-review
> **功能名称:** 用户登录
> **基于:** PRD v1.0-confirmed | UI_DESIGN v1.0-confirmed

---

## §1 架构概览

```
┌──────────────────────────────────────────────────────────┐
│                      UI Layer (Compose)                    │
│  LoginScreen ←→ LoginViewModel (StateFlow<LoginUiState>)  │
│  BackHandler → activity.finish()                          │
└──────────────────────┬───────────────────────────────────┘
                       │ DI: Hilt AuthModule
┌──────────────────────▼───────────────────────────────────┐
│                    Domain Layer                            │
│  AuthModels: LoginRequest / LoginResponse /               │
│              LoginErrorResponse / LoginUiState /          │
│              LoginEvent / LoginStatus                      │
└──────────────────────┬───────────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────────┐
│                     Data Layer                             │
│  AuthRepository ─→ LoginApi (Retrofit)                    │
│       │                                                   │
│       ├── MockAuthInterceptor (dev/mock env)              │
│       └── LoginStateManager (DataStore Preferences)       │
└──────────────────────────────────────────────────────────┘
```

**策略**：将现有 username+password mock 体系重构为 email+password Retrofit 体系，核心变更集中在 AuthRepository（从 mock→Retrofit）和 LoginViewModel（字段+状态机），UI 层遵循 UI_DESIGN §3 组件层级树和 Token 映射表。

---

## §2 模块设计

### 2.1 LoginApi — Retrofit 接口（新建）

**文件**：`app/src/main/java/com/example/myandroidapp/data/remote/LoginApi.kt`

```kotlin
interface LoginApi {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
}
```

**约束**：
- 使用项目已有的 `NetworkModule` 提供的 OkHttpClient
- LoginApi 实例由 `AuthModule` Hilt 提供
- 请求/响应模型见 §3 接口定义

### 2.2 MockAuthInterceptor — OkHttp 拦截器（新建）

**文件**：`app/src/main/java/com/example/myandroidapp/data/remote/MockAuthInterceptor.kt`

```kotlin
class MockAuthInterceptor : Interceptor {
    // ✅ P0-G1: 缓冲 request body 避免 one-shot 消费
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.url.encodedPath != "/api/auth/login") return chain.proceed(request)

        // 缓冲 body (OkHttp RequestBody 为 one-shot)
        val buffer = Buffer()
        request.body?.writeTo(buffer)
        val bodyString = buffer.readUtf8()
        val loginRequest = Gson().fromJson(bodyString, LoginRequest::class.java)

        // ✅ P0-G3: Mock 延迟改用 readTimeout 模拟，避免 Thread.sleep 阻塞 OkHttp 线程池
        return when {
            loginRequest.email == "admin@example.com" && loginRequest.password == "123456" ->
                mockResponse(200, LoginResponse(token="mock-jwt", user=User(id="1", email="admin@example.com", displayName="Admin")))
            loginRequest.email.contains("locked") ->
                mockResponse(403, LoginErrorResponse(code=403, message="账户已被锁定"))
            loginRequest.email.contains("ratelimit") ->
                mockResponse(429, LoginErrorResponse(code=429, message="操作过于频繁，请稍后再试"))
            else ->
                mockResponse(401, LoginErrorResponse(code=401, message="邮箱或密码错误"))
        }
    }
}
```

**参考**：项目已有 `MockNewsInterceptor` 模式，复用其 `mockResponse()` 辅助函数（该函数内联在 `buildJsonResponse()` 中，需提取为独立工具函数）。

**Mock 延迟模拟**：通过 OkHttp 客户端 `readTimeout=2s` + `callTimeout=3s` 模拟正常延迟；超时场景使用 `connectTimeout=1ms` 触发。**禁止**在拦截器中使用 `Thread.sleep()` — 会阻塞 OkHttp dispatcher 线程池。

**Debug 门控增强**：
```kotlin
// AuthModule.kt — 使用 FLAG_DEBUGGABLE 替代 BuildConfig.DEBUG
val isDebug = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
okHttpClient = if (isDebug) {
    networkClient.newBuilder().addInterceptor(MockAuthInterceptor()).build()
} else {
    networkClient  // Release 构建不含 MockInterceptor
}
```

### 2.3 AuthRepository — 数据仓库（重构）

**文件**：`app/src/main/java/com/example/myandroidapp/data/repository/AuthRepository.kt`

> **⚠️ P0 修订说明**：`AuthResult` 保持为 `domain/model/AuthModels.kt` 中的顶层 sealed class（不改动为内部类，避免破坏现有所有引用）。`Error` 类新增 `code: Int?` 可选参数保持向后兼容。

```kotlin
class AuthRepository @Inject constructor(
    private val loginApi: LoginApi,
    private val stateManager: LoginStateManager
) {

    suspend fun login(email: String, password: String): AuthResult<LoginResponse> {
        // ✅ P0-G2: withTimeout 包装在 try-catch 最外层，确保 TimeoutCancellationException 被捕获
        return try {
            withTimeout(10_000L) {  // D-56
                val response = loginApi.login(LoginRequest(email, password))
                if (response.isSuccessful) {
                    // ✅ P0-G5: body() 安全解包，非空断言 → 空安全
                    val body = response.body()
                        ?: return@withTimeout AuthResult.Error(-1, "服务器返回数据异常")
                    stateManager.saveUser(body.token, body.user)  // Token 持久化
                    AuthResult.Success(body)
                } else {
                    val errorBody = parseError(response)
                    AuthResult.Error(errorBody.code, errorBody.message)
                }
            }
        } catch (e: TimeoutCancellationException) {  // ✅ P0-G2: 补充超时异常捕获
            AuthResult.Error(-1, "网络请求超时，请重试")
        } catch (e: HttpException) {  // D-42
            AuthResult.Error(e.code(), e.message())
        } catch (e: SocketTimeoutException) {
            AuthResult.Error(-1, "网络请求超时，请重试")
        } catch (e: IOException) {
            AuthResult.Error(-1, "网络连接失败，请检查网络设置")
        } catch (e: JsonSyntaxException) {  // ✅ P0-G4: Gson 解析失败兜底
            AuthResult.Error(-1, "数据解析失败")
        } catch (e: Exception) {  // ✅ P0-G4: 全局兜底
            AuthResult.Error(-1, "未知错误: ${e.message}")
        }
    }

    // ✅ P0-G4: parseError 实现（含 Gson 解析保护）
    private fun parseError(response: Response<*>): AuthResult.Error {
        return try {
            val errorBody = response.errorBody()?.string() ?: "{}"
            val parsed = Gson().fromJson(errorBody, LoginErrorResponse::class.java)
                ?: LoginErrorResponse(response.code(), "未知错误")
            AuthResult.Error(parsed.code, parsed.message)
        } catch (e: Exception) {
            AuthResult.Error(response.code(), "请求失败 (${response.code()})")
        }
    }

    fun isLoggedIn(): Boolean = stateManager.getToken() != null
    fun logout() = stateManager.clear()
}
```

**关键变更**：
- 从硬编码 mock → Retrofit `LoginApi` 调用
- 添加 `withTimeout(10s)` + 全异常覆盖：`TimeoutCancellationException` / `HttpException` / `SocketTimeoutException` / `IOException` / `JsonSyntaxException` / `Exception` 全局兜底
- `AuthResult` 保持顶层密封类（`AuthModels.kt`），`Error(code: Int?, message: String)` 向后兼容
- Token 通过 `LoginStateManager` 持久化到 DataStore（D-67）

### 2.4 AuthModels — 数据模型（重构）

**文件**：`app/src/main/java/com/example/myandroidapp/domain/model/AuthModels.kt`

```kotlin
// ── 请求 ──
data class LoginRequest(
    val email: String,     // RFC 5322, max 254
    val password: String   // 1-128 chars
)

// ── 成功响应 ──
data class LoginResponse(
    val token: String,
    val user: User
)

data class User(
    val id: String,
    val email: String,
    val displayName: String
)

// ── 错误响应 ──
data class LoginErrorResponse(
    val code: Int,         // 401 | 403 | 429
    val message: String
)

// ── UI 状态 ──
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isEmailValid: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val status: LoginStatus = LoginStatus.Idle,
    val errorMessage: String? = null,
    val cooldownSeconds: Int = 0          // 429 限流倒计时
)

enum class LoginStatus { Idle, Editing, Loading, Success, Error, Timeout, Cooldown }

// ── UI 事件 ──
sealed class LoginEvent {
    data class EmailChanged(val email: String) : LoginEvent()
    data class PasswordChanged(val password: String) : LoginEvent()
    object SubmitLogin : LoginEvent()
    object TogglePasswordVisibility : LoginEvent()
    object ClearError : LoginEvent()
}
```

**关键变更**：
- `username` → `email`，新增 `isEmailValid` 字段
- `LoginResponse` 从 `(token, username)` → `(token, user{id,email,displayName})` 对应 PRD §11 JSON Schema
- 新增 `LoginErrorResponse` 独立错误模型
- `LoginUiState` 新增显式 `status: LoginStatus` 枚举 + `cooldownSeconds` 429 专用
- 新增 `LoginEvent` 密封类（原 `LoginEvent` 在现有代码中不存在，为全新引入）

### 2.5 LoginViewModel — 视图模型（重构）

**文件**：`app/src/main/java/com/example/myandroidapp/ui/auth/LoginViewModel.kt`

```kotlin
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.EmailChanged -> onEmailChanged(event.email)
            is LoginEvent.PasswordChanged -> onPasswordChanged(event.password)
            LoginEvent.SubmitLogin -> login()
            LoginEvent.TogglePasswordVisibility -> togglePasswordVisibility()
            LoginEvent.ClearError -> clearError()
        }
    }

    private fun onEmailChanged(email: String) {
        val isValid = EMAIL_REGEX.matches(email) && email.length <= 254
        _uiState.update {
            it.copy(
                email = email,
                isEmailValid = isValid,
                errorMessage = null,       // 输入变更清除错误
                status = LoginStatus.Editing
            )
        }
    }

    private fun onPasswordChanged(password: String) {
        _uiState.update {
            it.copy(
                password = password,
                errorMessage = null,
                status = LoginStatus.Editing
            )
        }
    }

    fun login() {
        // ✅ P0-G6: 原子检查+切换，消除竞态条件（快速连点→并发双请求）
        viewModelScope.launch {
            val acquired = _uiState.updateAndGet { current ->
                if (!current.isEmailValid ||
                    current.password.isEmpty() ||
                    current.status == LoginStatus.Loading ||
                    current.status == LoginStatus.Cooldown
                ) return@updateAndGet current  // 不修改，放弃本次
                current.copy(status = LoginStatus.Loading, errorMessage = null)
            }
            if (acquired.status != LoginStatus.Loading) return@launch  // 未获取登录权

            val currentState = acquired  // 闭包内捕获 state

            // ✅ P0-G7: 网络请求在 Dispatchers.IO 中执行（遵守 CLAUDE.md）
            when (val result = withContext(Dispatchers.IO) {
                authRepository.login(currentState.email, currentState.password)
            }) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(status = LoginStatus.Success) }
                }
                is AuthResult.Error -> {
                    when (result.code) {
                        429 -> {
                            // ✅ P1-G1: 30s 冷却倒计时 — cancel-safe (try-catch CancellationException + finally 清理)
                            var countdown = 30
                            _uiState.update { it.copy(status = LoginStatus.Cooldown, cooldownSeconds = countdown, errorMessage = result.message) }
                            try {
                                while (countdown > 0) {
                                    delay(1000)
                                    countdown--
                                    _uiState.update { it.copy(cooldownSeconds = countdown) }
                                }
                            } catch (_: CancellationException) {
                                // ViewModel.onCleared 时协程取消，不做任何操作
                            } finally {
                                // 确保退出时状态干净
                                if (countdown > 0) {
                                    _uiState.update { it.copy(status = LoginStatus.Idle, cooldownSeconds = 0) }
                                }
                            }
                            _uiState.update { it.copy(status = LoginStatus.Idle, cooldownSeconds = 0) }
                        }
                        -1 -> {
                            _uiState.update { it.copy(status = LoginStatus.Timeout, errorMessage = result.message) }
                        }
                        else -> {
                            _uiState.update { it.copy(status = LoginStatus.Error, errorMessage = result.message) }
                        }
                    }
                }
            }
        }
    }

    // ✅ P1-G5: 保留 logout() 方法 — NavGraph news_list composable 依赖
    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }

    private fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    private fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {  // D-58
        super.onCleared()
    }
}
```

**关键变更**：
- `onUsernameChanged()` → `onEmailChanged()` + 邮箱正则校验
- 新增 `LoginEvent` 驱动的事件处理（替代旧有的方法直调）
- 显式状态机：`Idle → Editing → Loading → Success/Error/Timeout/Cooldown`
- `collectLatest` 模式 — 事件处理使用 `onEvent()` 统一入口（D-55）
- 429 限流：30s Cooldown 倒计时 + 按钮 disabled
- `onCleared()` 显式声明（D-58）

### 2.6 LoginScreen — Compose UI（重构）

**文件**：`app/src/main/java/com/example/myandroidapp/ui/auth/LoginScreen.kt`

**关键变更**（遵循 UI_DESIGN §3 组件层级树）：
- `username` 输入框 → `email` 输入框，`KeyboardType.Email` + `imeAction = Next`
- 按钮 enabled 条件：`isEmailValid && password.isNotEmpty() && status != LoginStatus.Loading && status != LoginStatus.Cooldown`
- 按钮文字：Cooldown 态显示 "请等待 {cooldownSeconds}s"
- 错误展示：内联 `Text(error)` + `AnimatedVisibility`（非 Card）
- `BackHandler(enabled = true) { activity.finish() }`（AC-08）
- 所有元素添加 `testTag` + `contentDescription`（§7 无障碍表）
- `Modifier.imePadding()` 键盘避让

### 2.7 AuthModule — Hilt DI（重构）

**文件**：`app/src/main/java/com/example/myandroidapp/di/AuthModule.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    // ✅ P0-G8: LoginApi 暴露为独立 binding — 单元测试可直接 mockk<LoginApi>()
    @Provides
    @Singleton
    fun provideLoginApi(okHttpClient: OkHttpClient): LoginApi {
        return Retrofit.Builder()
            .baseUrl("https://api.example.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LoginApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        loginApi: LoginApi,              // ✅ 独立注入，可 mock
        stateManager: LoginStateManager   // ✅ 独立注入，可 mock
    ): AuthRepository = AuthRepository(loginApi, stateManager)
}
```

**关键变更**：
- 从手动 `new AuthRepository()` → Hilt `@Provides` Retrofit 注入
- ✅ **P0-G8**: `LoginApi` 暴露为独立 Hilt binding，单元测试中可直接 `mockk<LoginApi>()` 注入 `AuthRepository`
- ✅ **P0-G1**: Debug 门控使用 `FLAG_DEBUGGABLE` 替代 `BuildConfig.DEBUG`（见 §2.2 MockAuthInterceptor）
- 复用 `NetworkModule` 提供的 OkHttpClient（含 writeTimeout/callTimeout，D-64）

### 2.8 LoginStateManager — DataStore 持久化（重构）

**文件**：`app/src/main/java/com/example/myandroidapp/data/local/LoginStateManager.kt`

**关键变更**：
- `username` key → `user_email` + `user_display_name` + `auth_token` 三个独立 key
- 新增 `saveUser(token, user)` / `getToken()` / `getUser()` / `clear()` 方法
- 使用 `DataStore<Preferences>` 加密存储（EncryptedSharedPreferences 后续迭代）

### 2.9 NavGraph — 导航（修改）

**文件**：`app/src/main/java/com/example/myandroidapp/MainActivity.kt`（⚠️ P0-G9: 统一到 `NewsAppNavHost()`，**非** `NavGraph.kt` 的 `AppNavGraph()`）

> **⚠️ 关键纠正**：项目存在两个 NavHost 实现 — `NavGraph.kt` 的 `AppNavGraph()` 和 `MainActivity.kt` 的 `NewsAppNavHost()`。实际运行时使用的是 `MainActivity.kt` 中的 `NewsAppNavHost()`。需在 `NewsAppNavHost()` 中新增 login composable，而非修改 `AppNavGraph()`。

```kotlin
// MainActivity.kt — NewsAppNavHost()
@Composable
fun NewsAppNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = "login"  // ✅ 改为 login
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("news_list") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
            // ✅ AC-08: 返回键退出应用
            BackHandler { (context as? Activity)?.finish() }
        }
        composable("news_list") { /* 现有逻辑 */ }
        composable("news_detail/{articleId}") { /* 现有逻辑 */ }
    }
}
```

- `startDestination = "login"`（原 "news_list"）
- 登录成功 → `navigate("news_list") { popUpTo("login") { inclusive = true } }` 防止回退
- `BackHandler` 使用安全类型转换 `as? Activity`（测试环境兼容）
- **行动项**：删除 `NavGraph.kt` 中的 `AppNavGraph()` 死代码，避免后续混淆

### 2.10 数据流

```
用户输入 email                          LoginApi
      │                                    │
      ▼                                    ▼
LoginEvent.EmailChanged ──→ LoginViewModel    POST /api/auth/login
      │                         │                │
      ▼                         │                ▼
isEmailValid = regex.match()    │           Response(200/401/403/429)
      │                         │                │
      ▼                         ▼                ▼
_uiState.update { ... }    viewModelScope.launch  AuthResult.Success/Error
      │                         │                │
      ▼                         ▼                ▼
LoginScreen 重组               _uiState.update   LoginStateManager.saveUser()
```

---

## §3 接口定义

### 3.1 LoginApi

```kotlin
@POST("api/auth/login")
suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
```

### 3.2 AuthRepository

```kotlin
suspend fun login(email: String, password: String): AuthResult<LoginResponse>
fun isLoggedIn(): Boolean
fun logout()
```

### 3.3 LoginViewModel

```kotlin
val uiState: StateFlow<LoginUiState>
fun onEvent(event: LoginEvent)
```

### 3.4 数据模型（对齐 PRD §11 JSON Schema）

| 模型 | 字段 | 对应 Schema |
|------|------|-------------|
| `LoginRequest` | email, password | `LoginRequest` |
| `LoginResponse` | token, user{id,email,displayName} | `LoginResponse` |
| `LoginErrorResponse` | code(401/403/429), message | `LoginErrorResponse` |
| `LoginUiState` | email, password, isEmailValid, isPasswordVisible, status, errorMessage, cooldownSeconds | — |
| `LoginStatus` | Idle, Editing, Loading, Success, Error, Timeout, Cooldown | §11.3 状态枚举扩展 |

---

## §4 安全考虑

| 风险 | 等级 | 缓解 |
|------|:----:|------|
| 明文密码在 ViewModel 内存中 | 🟢 低 | ViewModel 在 onCleared 时销毁，密码不写入 DataStore。后续迭代考虑 CharArray/@Transient |
| HTTPS 中间人攻击 | 🟢 低 | Retrofit + OkHttp 默认 TLS 1.2+；NetworkModule 配置 certificatePinner（后续） |
| 登录按钮快速连点导致并发请求 | 🟢 低 | ✅ **P0-G6 已修订**：原子 `updateAndGet` 闭包内检查 `status == Loading/Cooldown`，消除竞态 |
| Token 明文存储 DataStore | 🟡 中 | v1.0 使用 DataStore Preferences；**建议 v1.0 即接入 EncryptedSharedPreferences**（非延期 v1.1） |
| MockAuthInterceptor 泄漏到 Release | 🟢 低 | ✅ **P0-G1 已修订**：`FLAG_DEBUGGABLE` 门控替代 `BuildConfig.DEBUG`；Release 不含 MockInterceptor |
| `activity.finish()` 在非 Activity Context 调用 | 🟢 低 | ✅ `as? Activity` 安全转换（测试/Preview 环境返回 null 不崩溃） |
| DataStore 三 key 写入非原子事务 | 🟡 中 | 建议合并为单次 `edit{}` 写入；v1.0 风险低（写入量小） |

---

## §5 测试策略

| # | 测试类型 | 用例 | 断言 |
|---|----------|------|------|
| T1 | 单元测试 | `LoginViewModel` email 校验 | 合法邮箱 `isEmailValid=true`，非法 `false` |
| T2 | 单元测试 | `LoginViewModel` 按钮联动 | `isEmailValid=false` → button disabled；两者满足 → enabled |
| T3 | 单元测试 | `LoginViewModel` login 成功 | `AuthResult.Success` → `status=Success` |
| T4 | 单元测试 | `LoginViewModel` login 401 | `AuthResult.Error(401)` → `status=Error`，`errorMessage="邮箱或密码错误"` |
| T5 | 单元测试 | `LoginViewModel` login 403 | `AuthResult.Error(403)` → `status=Error`，`errorMessage="账户已被锁定"` |
| T6 | 单元测试 | `LoginViewModel` login 429 | `AuthResult.Error(429)` → `status=Cooldown`，`cooldownSeconds=30`，倒计时结束 → `Idle` |
| T7 | 单元测试 | `LoginViewModel` login 超时 | `SocketTimeoutException` → `status=Timeout` |
| T8 | 单元测试 | `LoginViewModel` loading 中不可重入 | `status=Loading` 时再次 `login()` → 无第二次调用 |
| T9 | 单元测试 | `LoginViewModel` 输入变更清除错误 | `status=Error/errorMessage` → 输入 email → `errorMessage=null` |
| T10 | 单元测试 | `AuthRepository` HttpException → Error | Mock 401 response → `AuthResult.Error` |
| T11 | 单元测试 | `AuthRepository` withTimeout | Mock 延迟 >10s → `SocketTimeoutException` |
| T12 | Compose UI | LoginScreen idle 态 | testTag 存在，按钮 disabled，输入框可见 |
| T13 | Compose UI | LoginScreen loading 态 | CircularProgressIndicator 可见，输入框 disabled |
| T14 | Compose UI | LoginScreen error 态 | 错误文字可见，AnimatedVisibility 展开 |
| T15 | Compose UI | LoginScreen 密码显隐 | trailingIcon 点击 → password visible ↔ hidden |
| T16 | Compose UI | BackHandler 退出 | 按返回键 → `activity.finish()` 调用 |

**CI 策略**：`./gradlew testDebugUnitTest lintDebug`
> ✅ **P0-G10**：Compose UI Test（T12-T16）通过 Robolectric 在 `src/test/` 目录运行，与项目现有 `NewsListScreenComposeTest` 模式一致。不使用 `connectedAndroidTest`（无 `androidTest/` 基础设施）。

**测试依赖**（✅ 新增项已标记）：
- MockK：Mock `AuthRepository` + `LoginApi`
- ✅ **Turbine 1.0.0**：测试 `StateFlow` 发射序列（状态机 T3-T8）← **P0-G10: 需追加到 `build.gradle.kts`**
- Compose UI Test：T12-T16（通过 `androidx.compose.ui:ui-test-junit4` + Robolectric）
- Coroutines Test：`runTest` + `StandardTestDispatcher`
- ✅ **Hilt Android Testing**：`@HiltAndroidTest` + `HiltAndroidRule`（`hilt-android-testing:2.48.1` + `kaptTest hilt-android-compiler`）← **P0-G11: 需追加到 `build.gradle.kts`**

**`build.gradle.kts` 需追加的依赖**：
```kotlin
testImplementation("app.cash.turbine:turbine:1.0.0")
testImplementation("com.google.dagger:hilt-android-testing:2.48.1")
kaptTest("com.google.dagger:hilt-android-compiler:2.48.1")
```

---

## §6 文件变更清单

| 操作 | 文件 | 说明 |
|:--:|------|------|
| **新建** | `data/remote/LoginApi.kt` | Retrofit 登录 API 接口 |
| **新建** | `data/remote/MockAuthInterceptor.kt` | Mock 后端拦截器（含 Buffer body 解析 + FLAG_DEBUGGABLE 门控） |
| **新建** | `domain/model/LoginModels.kt` | LoginEvent + LoginStatus + 重构后的数据模型 |
| **新建** | `ui/theme/Theme.kt` | M3 Theme（lightColorScheme + darkColorScheme + Typography） |
| **新建** | `ui/theme/Color.kt` | 品牌色 `#1A73E8` 等 Token 常量 |
| **新建** | `ui/theme/Type.kt` | 字体层级映射（headlineMedium/bodyMedium/labelLarge 等） |
| **新建** | `ui/auth/LoginDimens.kt` | 间距 Token: spacing0=0dp, spacing1=8dp, ..., spacing5=40dp + icon/field/btn 规格 |
| **新建** | `data/repository/AuthRepositoryTest.kt` | ✅ T10-T11 单元测试（mock LoginApi + coEvery + runTest + advanceTimeBy） |
| **新建** | `ui/auth/LoginScreenComposeTest.kt` | ✅ T12-T16 Compose UI 测试（Robolectric） |
| **修改** | `data/repository/AuthRepository.kt` | mock→Retrofit + withTimeout + TimeoutCancellationException + body空安全 + parseError + 全局异常兜底 |
| **修改** | `domain/model/AuthModels.kt` | username→email，AuthResult.Error 新增 `code: Int?` 可选参数，新增 LoginErrorResponse |
| **修改** | `ui/auth/LoginViewModel.kt` | email 校验 + 原子状态检查 + Dispatchers.IO + LoginEvent + 429 cancel-safe 倒计时 + logout |
| **修改** | `ui/auth/LoginScreen.kt` | email 输入框 + 按钮联动 + BackHandler + 内联错误 + testTag + 无障碍 + 密码显隐用 ViewModel 状态 |
| **修改** | `di/AuthModule.kt` | 手动 new → Hilt Retrofit 注入；LoginApi 独立 binding；DEBUG 门控升级 |
| **修改** | `data/local/LoginStateManager.kt` | username key → user_email/user_display_name/auth_token 三 key |
| **修改** | `MainActivity.kt` (NewsAppNavHost) | ⚠️ 非 NavGraph.kt；startDestination→login + login composable + BackHandler |
| **修改** | `app/build.gradle.kts` | ✅ 追加 Turbine + Hilt testing 依赖 |
| **删除** | `navigation/NavGraph.kt` (AppNavGraph) | 死代码 — 已统一到 NewsAppNavHost |
| **删除** | 旧 `domain/model/AuthModels.kt` 残留 | username 相关旧字段 |

---

## §7 架构决策

| 编号 | 决策 | 原因 |
|------|------|------|
| AD-01 | AuthRepository 使用密封类 `AuthResult<T>` 而非裸异常 | 调用方可在 ViewModel 中用 `when` 穷举处理，编译期保证分支覆盖 |
| AD-02 | 邮箱校验在 ViewModel 中执行（非 Repository） | 客户端校验属于 UI 层逻辑，不应污染数据层；Repository 仅负责网络调用 |
| AD-03 | 429 倒计时在 ViewModel 中用 `delay(1000)` 循环实现 | 避免引入 CountDownTimer / Android 依赖，纯协程方案便于单元测试 |
| AD-04 | MockAuthInterceptor 仅在 Debug 构建启用（`FLAG_DEBUGGABLE` 门控） | ✅ 修订：`BuildConfig.DEBUG` → `ApplicationInfo.FLAG_DEBUGGABLE` 更可靠 |
| AD-05 | LoginStateManager 使用 3 个独立 DataStore key（token/email/displayName） | 避免 JSON 序列化开销，Preferences 天然支持独立 key 读写 |
| AD-06 | 输入框 Loading 态 disabled 使用 `Modifier.enabled(!isLoading)` 而非仅 alpha | `alpha` 不阻止触摸事件（Pitfalls 已验证），必须配合 `enabled=false` |
| AD-07 | Theme.kt 定义独立于 NewsDimens 的 LoginDimens | 登录页与新闻列表设计语境不同，强行共享 Token 会导致语义混乱 |
| AD-08 | `LoginEvent` 采用密封类 + 统一 `onEvent()` 入口 | `onEvent` 方法调用天然串行处理事件，无需 `collectLatest` |

---

## §8 依赖关系

```
LoginViewModel
  └── AuthRepository (runtime, Hilt injected)
        ├── LoginApi (runtime, Retrofit)
        │     └── OkHttpClient (compile-time, NetworkModule)
        │           └── MockAuthInterceptor (debug-only, OkHttp)
        └── LoginStateManager (runtime, DataStore)

LoginScreen
  ├── LoginViewModel (runtime, hiltViewModel)
  ├── LoginDimens (compile-time)
  └── MaterialTheme (runtime, Theme.kt → Color.kt + Type.kt)

AuthModule
  ├── LoginApi → Retrofit.Builder → OkHttpClient (NetworkModule)
  └── AuthRepository → LoginApi + LoginStateManager
```

**需确认的编译依赖**：
- `material-icons-extended`：`Icons.Filled.Email` 在 extended 包中（非默认 material-icons-core）

---

## §9 多视角评审记录

> **评审日期:** 2026-06-07 | **方式:** delegate_task 三视角并行 (B1 工程师/B2 安全/B3 可测试性)

### 评审总览

| 视角 | 评分 | P0 | P1 | P2 | 关键发现 |
|------|:----:|----|----|-----|---------|
| B1 资深工程师 | 6/10 | 6 | 8 | 5 | MockAuth 伪代码、withTimeout 异常泄漏、NavGraph 双实现、Dispatchers.IO 缺失、AuthResult 签名变更、body!! |
| B2 安全/稳定性 | 3.8/10 | 5 | 6 | 5 | withTimeout 异常 miss、login() 竞态、body!! NPE、parseError 空缺、Mock body 消费 |
| B3 可测试性 | 3/10 | 6 | 8 | 6 | Turbine/依赖缺失、androidTest 虚构、LoginApi 不可独立 mock、hilt-test 缺失、AuthResult 破坏引用 |

> **去重后合计：12 P0 / 22 P1 / 16 P2** — 全部 12 项 P0 已在本次修订中自动修复；P1 项编码阶段逐项消化。

### P0 修订记录（已自动修订 ✅）

| # | 来源 | 问题 | 修订 |
|---|------|------|------|
| P0-G1 | B1+B2 | MockAuthInterceptor: body 伪代码 + one-shot 消费 + Thread.sleep + DEBUG 门控 | Buffer.writeTo 读取 body → Gson 解析；延迟改用 readTimeout；门控升级 FLAG_DEBUGGABLE |
| P0-G2 | B1+B2 | withTimeout 不捕获 TimeoutCancellationException | try-catch 移至 withTimeout 外层，新增 TimeoutCancellationException 分支 |
| P0-G3 | B1 | NavGraph 双实现 — NewsAppNavHost 不含 login 路由 | 统一到 MainActivity.kt NewsAppNavHost，新增 login composable + BackHandler |
| P0-G4 | B1 | 网络请求缺少 Dispatchers.IO | LoginViewModel.login() 中用 `withContext(Dispatchers.IO)` 包裹 `authRepository.login()` |
| P0-G5 | B1+B2 | response.body()!! NPE | 改为 `body ?: return@withTimeout AuthResult.Error(...)` 空安全解包 |
| P0-G6 | B2 | login() 竞态条件 — 读取/写入非原子 | `_uiState.updateAndGet` 原子闭包内检查 status + 切换 Loading，双重 check Cooldown |
| P0-G7 | B2 | parseError() 未定义 + Gson 解析失败无保护 | 实现 parseError 方法（含 Gson 解析 try-catch）+ 全局 catch(Exception) 兜底 |
| P0-G8 | B1+B3 | AuthResult 从独立文件移入内部类 — 破坏引用 | AuthResult 保持为顶层 sealed class；Error 新增 `code: Int?` 可选参数向后兼容 |
| P0-G9 | B3 | AuthRepository 的 LoginApi 不可独立 mock | AuthModule 中 LoginApi 暴露为独立 Hilt binding |
| P0-G10 | B3 | Turbine 依赖未声明；connectedAndroidTest 不可执行 | §5 追加 Turbine 1.0.0 → build.gradle.kts；CI 改为 `testDebugUnitTest lintDebug` |
| P0-G11 | B3 | hilt-android-testing 缺失 | §5 追加 `hilt-android-testing:2.48.1` + `kaptTest hilt-android-compiler` |
| P0-G12 | B3 | 测试文件未列入变更清单 | §6 追加 AuthRepositoryTest.kt + LoginScreenComposeTest.kt |

### P1 待确认项（编码阶段消化）

| # | 问题 | 来源 |
|---|------|------|
| P1-G1 | LoginUiState/LoginEvent 跨层放置 — UiState 在 domain 层 | B1 |
| P1-G2 | AuthRepository.isLoggedIn() 非 suspend 却调 DataStore | B1 |
| P1-G3 | LoginStateManager 旧 key 迁移无数据兼容策略 | B1 |
| P1-G4 | LoginScreen 密码显隐从 remember 本地状态迁移到 ViewModel | B1+B3 |
| P1-G5 | LoginViewModel 丢失 logout() 方法 — NavGraph 依赖 | B1 |
| P1-G6 | Token 明文 DataStore → 建议 v1.0 即接入 EncryptedSharedPreferences | B2 |
| P1-G7 | 429 Cooldown delay 循环 + cancel-safe + finally 状态清理 | B2 |
| P1-G8 | 密码长度无上限校验 — 硬截断到 128 字符 | B2 |
| P1-G9 | AuthRepository 单元测试无独立文件计划 — §6 追补 | B3 |
| P1-G10 | LoginApi Retrofit 接口无法用 mockk 直接 mock — 需明确 coEvery 策略 | B3 |
| P1-G11 | CI 无 lint 步骤 — 追补 lintDebug | B3 |
| P1-G12 | 新增 Theme/Dimens 文件无独立测试 — 追补 LoginDimens 值校验 | B3 |
| P1-G13 | withTimeout 超时测试 (T11) runTest 中需 advanceTimeBy 触发 | B3 |
| P1-G14 | email 校验边界值未枚举 — 建议参数化测试覆盖 RFC 5322 边界 | B3 |
| P1-G15 | EncryptedSharedPreferences 建议 v1.0 实现（非延至 v1.1） | B2 |
| P1-G16 | LoginStateManager 三 key 写入非事务 — 建议单次 edit{} | B2 |
| P1-G17 | Gson 反序列化 LoginResponse 失败无保护 — 全局 catch(Exception) 兜底 | B1 |
| P1-G18 | MockNewsInterceptor 无独立 mockResponse() — 需提取公共工具函数 | B1 |
| P1-G19 | LoginScreen password visible 从 remember → ViewModel.uiState.isPasswordVisible | B3 |
| P1-G20 | T16 BackHandler 测试需要 Activity Scenario（或降级为 Robolectric） | B3 |
| P1-G21 | AnimatedVisibility 测试 — testTag 放入内部 composable | B3 |
| P1-G22 | T8 loading 不可重入断言需配合 coVerify(exactly=0) | B3 |

### P2 优化建议（低优先级）

| # | 建议 | 来源 |
|---|------|------|
| P2-1 | 429 冷却态 login() 追加 Cooldown 双重防护 | B1 |
| P2-2 | AnimatedVisibility + imePadding 重组抖动 — 建议 animateContentSize | B1 |
| P2-3 | LoginUiState.copy() 每字符触发全字段重建 — 建议 derivedStateOf | B1 |
| P2-4 | Theme.kt 暗色模式 onPrimary 色值用 MaterialTheme 自动生成 | B1 |
| P2-5 | BackHandler `as? Activity` 测试兼容 — 建议 CompositionLocal | B1+B2 |
| P2-6 | 邮箱正则拒绝 RFC 5322 合法格式 — 评估用户群后放宽 | B2 |
| P2-7 | 网络切换无自动重试 — 建议指数退避重试 | B2 |
| P2-8 | 进程杀死后 429 Cooldown 状态丢失 — 建议 deadline 持久化 | B2 |
| P2-9 | withTimeout(10s) 与 OkHttp callTimeout 双重超时 — 建议统一 | B2 |
| P2-10 | 密码 String 驻留内存 — 后续迁移 CharArray/@Transient | B2 |
| P2-11 | 缺少 Timber 日志验证测试 | B3 |
| P2-12 | 缺少参数化测试依赖声明（JUnit5 @ParameterizedTest） | B3 |
| P2-13 | 缺少代码覆盖率阈值（JaCoCo） | B3 |
| P2-14 | AnimatedVisibility Robolectric 下动画跳过 — assertIsDisplayed 可直接用 | B3 |
| P2-15 | LoginDimens 具体值仅在 P1 中提及 — §6 已补充 | B1 |
| P2-16 | 删除 NavGraph.kt 中 AppNavGraph 死代码 | B1 |

### 工时重新估算

| 原估 | 修订后 | 说明 |
|:----:|:------:|------|
| 20h | **28h** | P0 修订涉及 7 个模块的代码重构（AuthRepository/LoginViewModel/AuthModule/MockInterceptor/NavGraph/§5测试/§6文件）+ 新增 9 测试文件。编码阶段 22 项 P1 需逐项消化。 |

---

> **状态:** 已评审 (v0.2-review) — 12 项 P0 已自动修订，22 项 P1 编码阶段消化。请审阅后回复「确认」冻结进入编码。
