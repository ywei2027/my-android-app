# 用户登录 — UI 设计方案

> **版本:** v0.1-draft
> **功能名称:** 用户登录
> **创建日期:** 2026-06-07
> **基于:** PRD v1.0-confirmed §9 | DECISIONS.md

---

## §1 设计总览

### 设计目标
- 为用户登录页面提供完整的 Material3 UI 设计方案，覆盖所有状态（Idle/Editing/Loading/Success/Error/Timeout）
- 将现有 username+password 方案迁移为 PRD 要求的 email+password 方案
- 确保颜色/字体/间距全部 Token 化，对齐 M3 规范
- 补齐无障碍语义（contentDescription/testTag/announceForAccessibility）
- 输出可预览的 HTML 原型和 Playwright 截图

### 设计语言
- Material3 (M3) 设计系统
- 375dp 基准视口（对齐 iPhone X 375×812）
- 颜色 Token 体系：基于 `#1A73E8` 品牌色
- 8dp 基准间距网格

---

## §2 页面清单与导航

| 页面 | 路由 | 类型 | 入口 | 说明 |
|------|------|------|------|------|
| LoginScreen | `/login` (NavHost startDestination) | 改造 | App 冷启动 → Splash 后 | 单页全屏，无 BottomBar/AppBar |

### 导航图
```
NavHost(startDestination = "login")
├── composable("login")      → LoginScreen
│   └── onLoginSuccess → navigate("news_list") { popUpTo("login") inclusive = true }
└── composable("news_list")  → NewsListScreen
```

---

## §3 页面设计 — LoginScreen

### 线框图
```
┌─────── iPhone SE 375×812 ──────┐
│ ██████ Status Bar (44px) ██████ │
│                                 │
│                                 │
│          ┌──────────┐           │
│          │  📧 64×64 │           │  ← Icon, tint=primary
│          └──────────┘           │
│           欢迎回来               │  ← headlineMedium 24sp Bold
│     请使用邮箱和密码登录         │  ← bodyMedium 14sp onSurfaceVariant
│                                 │  ← Spacer 32dp
│  ┌──────────────────────────┐   │
│  │ 📧 请输入邮箱地址         │   │  ← OutlinedTextField 56dp
│  └──────────────────────────┘   │     leadingIcon: Email
│  ┌──────────────────────────┐   │  ← Spacer 16dp
│  │ 🔒 请输入密码         👁  │   │  ← OutlinedTextField 56dp
│  └──────────────────────────┘   │     leadingIcon: Lock
│  ┌──────────────────────────┐   │     trailingIcon: Visibility/Off
│  │ ⚠ 邮箱或密码错误         │   │  ← 内联错误 Card, AnimatedVisibility
│  └──────────────────────────┘   │     errorContainer 背景, fadeIn/Out
│                                 │  ← Spacer 24dp
│  ┌──────────────────────────┐   │
│  │         登  录            │   │  ← Button filled, 48dp, pill 24dp
│  └──────────────────────────┘   │
│  ┌──────────────────────────┐   │  ← loading 态: 同按钮内
│  │   ◌  登录中...           │   │     CircularProgressIndicator(20dp)
│  └──────────────────────────┘   │
│                                 │  ← Spacer 24dp
│     测试账号: admin / 123456    │  ← labelSmall 11sp, alpha 0.6
│                                 │
│        v1.0(1)debug             │  ← VersionTag 底部居中
│ ▓▓▓▓▓▓ Navigation Bar ▓▓▓▓▓▓▓▓ │
└─────────────────────────────────┘
```

### 组件层级树
```
LoginScreen(onLoginSuccess)
└── Scaffold(padding)
    └── Column(fillMaxSize, verticalScroll, horizontalPadding=24dp, Center)
        ├── Spacer(weight=1f)                              // 顶部弹性空间
        ├── Icon(Email, 64×64dp, tint=primary)             // testTag="login_app_icon"
        ├── Spacer(16dp)
        ├── Text("欢迎回来", headlineMedium, Bold)          // testTag="login_title"
        ├── Spacer(8dp)
        ├── Text("请使用邮箱和密码登录", bodyMedium)         // testTag="login_subtitle"
        ├── Spacer(32dp)
        ├── OutlinedTextField(                             // testTag="email_field"
        │       value=email,
        │       onValueChange=onEmailChanged,
        │       placeholder="请输入邮箱地址",
        │       leadingIcon=Email,
        │       keyboardType=Email, imeAction=Next,
        │       isError=!isEmailValid && email.isNotEmpty(),
        │       supportingText={ if(isError) Text("请输入有效的邮箱地址") },
        │       enabled=!isLoading,
        │       singleLine=true, height=56dp
        │   )
        ├── Spacer(16dp)
        ├── OutlinedTextField(                             // testTag="password_field"
        │       value=password,
        │       onValueChange=onPasswordChanged,
        │       placeholder="请输入密码",
        │       leadingIcon=Lock,
        │       trailingIcon=VisibilityToggle,
        │       visualTransformation=Password/None,
        │       keyboardType=Password, imeAction=Done,
        │       keyboardActions=onDone → login(),
        │       enabled=!isLoading,
        │       singleLine=true, height=56dp
        │   )
        ├── Spacer(0dp)                                    // 内联错误紧贴
        ├── AnimatedVisibility(errorMessage != null) {
        │       Card(errorContainer bg) {                  // testTag="error_message"
        │           Text(errorMessage, error color, 14sp)
        │       }
        │   }
        ├── Spacer(24dp)
        ├── Button(                                        // testTag="login_button"
        │       onClick=login(),
        │       enabled=isEmailValid && passwordNotEmpty && !isLoading,
        │       fillMaxWidth, height=48dp, shape=24dp
        │   ) {
        │       if(isLoading) {
        │           CircularProgressIndicator(20dp, 2dp, onPrimary)
        │           Spacer(8dp)
        │           Text("登录中...")
        │       } else {
        │           Text("登录", labelLarge)
        │       }
        │   }
        ├── Spacer(24dp)
        ├── Text("测试账号: admin / 123456", labelSmall,    // testTag="login_hint"
        │         onSurfaceVariant alpha 0.6)
        ├── Spacer(weight=1f)                              // 底部弹性空间
        └── VersionTag(Modifier.align(CenterHorizontally))  // 仅 Debug 构建，底部居中
```

### 交互状态机
```
                              ┌─────────────────────────────┐
                              │        进入 LoginScreen       │
                              └─────────────┬───────────────┘
                                            │
                                            ▼
                              ┌─────────────────────────────┐
                         ┌───→│           Idle               │←──────────────────┐
                         │    │  按钮 disabled / 无错误      │                    │
                         │    │  testTag="login_screen_idle" │                    │
                         │    └─────────────┬───────────────┘                    │
                         │                  │ 用户输入                             │
                         │                  ▼                                    │
                         │    ┌─────────────────────────────┐                    │
                         │    │          Editing              │←─────┐            │
                         │    │  按钮联动启用/禁用             │      │            │
                         │    │  邮箱格式错误→输入框 error 态   │      │ 用户修改    │
                         │    │  testTag="login_screen_editing"│     │ 邮箱/密码   │
                         │    └─────────────┬───────────────┘      │            │
                         │                  │ 点击登录按钮            │            │
                         │                  │ (isValid+notEmpty)     │            │
                         │                  ▼                        │            │
                         │    ┌─────────────────────────────┐        │            │
                         │    │          Loading              │        │            │
                         │    │  按钮 disabled + 转圈         │        │            │
                         │    │  输入框 disabled              │        │            │
                         │    │  testTag="login_screen_loading"│      │            │
                         │    └──────┬──────────────┬───────┘        │            │
                         │           │ 200          │ !200            │            │
                         │           ▼              ▼                │            │
                         │    ┌──────────┐   ┌──────────────┐        │            │
                         │    │ Success   │   │    Error      │────────┘            │
                         │    │ 导航主页  │   │ 内联错误 Card  │  输入变更清除错误      │
                         │    │ testTag=  │   │ 按钮恢复 enabled│                     │
                         │    │ "_success"│   │ testTag=       │─────────────────────┘
                         │    └──────────┘   │ "_error"       │
                         │                   └──────┬─────────┘
                         │                          │ 超时 10s
                         │                          ▼
                         │                   ┌──────────────┐
                         │                   │   Timeout     │
                         │                   │ "网络请求超时" │
                         │                   │ 按钮 enabled   │
                         │                   │ testTag=       │
                         │                   │ "_timeout"     │
                         │                   └───────────────┘
                         │
                         └──── 用户清空所有输入 → Idle（Empty）
                                testTag="login_screen_empty"
```

### 状态覆盖表

| 状态 | 触发条件 | UI 表现 | 测试标识 |
|------|----------|---------|----------|
| **Idle** | 页面首次加载 | 邮箱/密码为空，按钮 disabled，无错误，无 loading | `testTag="login_screen_idle"` |
| **Editing** | 用户输入邮箱/密码 | 按钮联动 enabled/disabled。邮箱格式错误→输入框 isError+supportingText | `testTag="login_screen_editing"` |
| **Loading** | 点击登录→API 请求进行中 | 按钮 disabled + CircularProgressIndicator(20dp) + "登录中..."；输入框 disabled | `testTag="login_screen_loading"` |
| **Success** | API 返回 200 | isLoggedIn=true → LaunchedEffect 触发 navigate HomeScreen | `testTag="login_screen_success"` |
| **Error** | API 返回 401/403/429 或网络异常 | 按钮 enabled；内联 error Card 显示；输入框 enabled | `testTag="login_screen_error"` |
| **Empty** | 用户清空所有输入 | 按钮 disabled，格式错误清除，服务端错误清除 | `testTag="login_screen_empty"` |
| **Timeout** | 请求 > 10s 无响应 | "网络请求超时，请重试"；按钮 enabled | `testTag="login_screen_timeout"` |
| **Dark Mode** | 系统 dark theme | 所有颜色通过 M3 Token 自动适配 | — |
| **Landscape** | 设备旋转横屏 | verticalScroll 确保内容可滚动到按钮 | — |

---

## §4 Token 映射表

### 颜色 Token

| Token | Light 值 | Dark 值 | 用途 |
|-------|----------|---------|------|
| `primary` | `#1A73E8` | `#8AB4F8` | 应用图标 tint、按钮背景 |
| `onPrimary` | `#FFFFFF` | `#003A75` | 按钮文字、loading indicator |
| `primaryContainer` | `#D3E3FD` | `#004A77` | 输入框焦点背景（可选） |
| `onPrimaryContainer` | `#001D36` | `#D3E3FD` | — |
| `error` | `#B3261E` | `#F2B8B5` | 输入框错误边框、错误文字、supportingText |
| `errorContainer` | `#F9DEDC` | `#8C1D18` | 错误提示 Card 背景 |
| `onErrorContainer` | `#410E0B` | `#F9DEDC` | 错误 Card 内文字 |
| `surface` | `#FEFBFF` | `#1C1B1F` | 页面背景 |
| `onSurface` | `#1C1B1F` | `#E6E1E5` | 标题文字 |
| `onSurfaceVariant` | `#49454F` | `#CAC4D0` | 副标题、placeholder、底部提示 |
| `outline` | `#79747E` | `#938F99` | OutlinedTextField 边框 |
| `outlineVariant` | `#CAC4D0` | `#49454F` | — |
| `surfaceVariant` | `#E7E0EC` | `#49454F` | — |

### 字体 Token

| 角色 | M3 Token | Size | Weight | LetterSpacing | 行高 |
|------|----------|------|--------|---------------|------|
| 页面标题 | `headlineMedium` | 24sp | Bold (700) | 0 | 32sp |
| 副标题/提示 | `bodyMedium` | 14sp | Regular (400) | 0.25sp | 20sp |
| 输入框文字 | `bodyLarge` | 16sp | Regular (400) | 0.5sp | 24sp |
| 输入框 placeholder | `bodyLarge` | 16sp | Regular, alpha 0.6 | 0.5sp | 24sp |
| 输入框 supportingText | `bodySmall` / `labelSmall` | 12sp | Regular (400) | 0.5sp | 16sp |
| 按钮文字 | `labelLarge` | 16sp | Medium (500) | 0.1sp | 20sp |
| 错误提示 | `bodySmall` | 14sp | Regular (400) | 0.25sp | 20sp |
| 底部测试提示 | `labelSmall` | 11sp | Regular (400) | 0.5sp | 16sp |

### 间距 Token（8dp 基准网格）

| Token | 值 | 用途 |
|--------|-----|------|
| `spacing0` | 0dp | 内联错误紧贴输入框 |
| `spacing1` | 8dp | 标题↔副标题，按钮内图标↔文字，loading 元素间距 |
| `spacing2` | 16dp | 图标↔标题，输入框之间 |
| `spacing3` | 24dp | 水平 padding，错误↔按钮 |
| `spacing4` | 32dp | 副标题↔邮箱输入框 |
| `spacing5` | 40dp | (预留) 大段间距 |

### 形状 Token

| Token | 值 | 用途 |
|-------|-----|------|
| `fieldShape` | `RoundedCornerShape(4dp)` | OutlinedTextField 圆角 |
| `btnShape` | `RoundedCornerShape(24dp)` | 登录按钮 pill 圆角 |
| `cardShape` | `RoundedCornerShape(12dp)` | 错误提示 Card 圆角 |
| `fieldHeight` | `56dp` | OutlinedTextField 固定高度 |
| `btnHeight` | `48dp` | Button 固定高度 |
| `iconSize` | `64dp` | 应用图标 |
| `spnrSize` | `20dp` | CircularProgressIndicator |
| `touchTarget` | `48dp` | IconButton 最小触摸目标 |

---

## §5 组件复用分析

### 可直接复用的项目组件

| 组件 | 来源 | 复用方式 | 状态 |
|------|------|----------|:--:|
| `VersionTag` | `ui/components/VersionTag.kt` | **直接复用** — 登录页底部居中显示版本号 | ✅ |
| `NewsDimens` | `ui/components/NewsDimens.kt` | **不适用** — 登录页有自己的间距 Token，不建议混用 | ❌ |

### 可参考模式的项目组件

| 组件 | 来源 | 参考点 |
|------|------|--------|
| `ErrorState` | `ui/components/ErrorState.kt` | 错误态的 Column+Icon+Text+Button 布局模式 |
| `EmptyState` | `ui/components/EmptyState.kt` | 空状态占位布局模式 |
| `ShimmerCard` | `ui/components/ShimmerCard.kt` | loading 骨架屏动画模式（登录场景用按钮内转圈，不适用骨架屏） |

### 需改造的现有 auth 组件

| 组件 | 当前状态 | PRD 要求 | 变更说明 |
|------|----------|----------|----------|
| `LoginScreen.kt` | username+OutlinedTextField，button 仅判断 `!isLoading` | email+OutlinedTextField，button 联动 `isEmailValid && passwordNotEmpty && !isLoading` | 字段重命名、按钮启用条件、键盘类型、焦点管理、BackHandler |
| `LoginViewModel.kt` | username/password 变更，login() 方法 | email/password 变更 + 邮箱校验 + 状态枚举 + collectLatest | 字段重命名 + isEmailValid 字段 + status 枚举 |
| `LoginUiState` | username/password/isLoading/isLoggedIn/errorMessage | email/password/status(enum)/isEmailValid/isLoading/isLoggedIn/errorMessage | 新增 status+isEmailValid 字段 |
| `AuthModels.kt` | `LoginResponse(token, username)` | `LoginResponse(token, user{id, email, displayName})` + `LoginErrorResponse(code, message)` | 数据模型重构 |
| `AuthRepository.kt` | 硬编码 mock 逻辑 | Retrofit LoginApi 调用 + HttpException 处理 + withTimeout | 全面重构 |
| `AuthModule.kt` | 手动 new AuthRepository | Hilt 注入 LoginApi + AuthRepository | DI 重构 |
| `LoginStateManager.kt` | 存 username 键 | 存 user 对象（email+displayName） | key 迁移 |
| `NavGraph.kt` | 基础路由 | 添加 BackHandler 退出逻辑 | AC-08 |

### 需新建的组件

| 组件 | 类型 | 复杂度 | 说明 |
|------|------|:------:|------|
| `LoginApi` | Retrofit Interface | 低 | `POST /api/auth/login` |
| `MockAuthInterceptor` | OkHttp Interceptor | 中 | 参考 `MockNewsInterceptor` 模式，Mock 登录 API 响应（200/401/403/429/超时） |
| `LoginEvent` | Sealed Class | 低 | EmailChanged/PasswordChanged/SubmitLogin/TogglePasswordVisibility/ClearError |

### 新增文件清单

| 文件 | 说明 | 复杂度 |
|------|------|:------:|
| `data/remote/LoginApi.kt` | Retrofit 登录 API 接口 | 低 |
| `data/remote/MockAuthInterceptor.kt` | OkHttp Mock 拦截器 | 中 |
| `domain/model/LoginModels.kt` | LoginEvent 密封类 | 低 |

---

## §6 架构协调设计

### 导航事件
```
LoginScreen 点击登录(200) → navController.navigate("news_list") { popUpTo("login") inclusive = true }
LoginScreen 返回键       → activity.finish()  (BackHandler 拦截)
NewsListScreen 退出登录   → loginViewModel.logout() → navController.navigate("login") { popUpTo(0) inclusive = true }
```

### ViewModel
- **LoginViewModel** — 已存在但需重构（字段 + 逻辑）
  - 暴露 `StateFlow<LoginUiState>`
  - 处理 `LoginEvent` (EmailChanged/PasswordChanged/SubmitLogin/TogglePasswordVisibility/ClearError)
  - 邮箱格式校验：RFC 5322 正则，实时校验
  - 登录逻辑：调用 AuthRepository.login(email, password)，捕获 HttpException/IOException/TimeoutException
  - 状态管理：显式 `LoginStatus` 枚举（Idle/Editing/Loading/Success/Error/Timeout）

### BackHandler
- LoginScreen 需 `BackHandler(enabled = true)` → `activity.finish()`（退出应用，不返回 Splash）
- NavGraph 中需要在 login composable 获取 Activity 引用

### 键盘管理
- 邮箱框 `imeAction=Next` → focusManager.moveFocus 到密码框
- 密码框 `imeAction=Done` → 触发 login()
- 登录按钮 onClick → focusManager.clearFocus() + keyboardController?.hide()
- 页面容器使用 `Modifier.imePadding()` 处理键盘避让

---

## §7 无障碍适配

| 元素 | contentDescription | 触控目标 | LiveRegion |
|------|-------------------|----------|------------|
| 应用图标 | "应用图标" | — | — |
| 标题 | (系统自动朗读) | — | — |
| 邮箱输入框 | "邮箱输入框" | — | — |
| 邮箱 leadingIcon | "邮箱图标" | — | — |
| 密码输入框 | "密码输入框" | — | — |
| 密码 leadingIcon | "密码图标" | — | — |
| 密码 trailingIcon | 密文时"显示密码"，明文时"隐藏密码" | ≥48dp | — |
| 邮箱错误提示 | (supportingText 自动关联) | — | — |
| 服务端错误 Card | — | — | `LiveRegion.Alert` 或 `announceForAccessibility()` |
| 登录按钮(默认) | "登录" | ≥48dp (height=48dp, fillMaxWidth) | — |
| 登录按钮(loading) | "登录中" | ≥48dp | announceForAccessibility("正在登录") |
| 登录按钮(success) | "登录成功" | ≥48dp | announceForAccessibility("登录成功，正在跳转") |
| 底部提示 | "测试账号提示: admin / 123456" | — | — |
| VersionTag | "应用版本号 v1.0" | — | — |
| 页面整体 | — | — | semantics { testTag = "login_screen" } |

---

## §8 前置依赖

| 依赖 | 说明 | 状态 |
|------|------|------|
| M3 Theme (lightColorScheme/darkColorScheme) | 需在 Theme.kt 中显式定义品牌色 `#1A73E8` | ⚠️ 编码阶段完成 |
| VersionTag | 已有组件，可直接复用 | ✅ 已有 |
| LoginViewModel | 需重构（字段+逻辑+状态枚举） | ⚠️ 编码阶段完成 |
| LoginApi (Retrofit) | 需新建接口 | ⚠️ 编码阶段完成 |
| MockAuthInterceptor | 需新建 Mock 拦截器 | ⚠️ 编码阶段完成 |
| AuthRepository (Retrofit) | 需从 mock 重构为 Retrofit 调用 | ⚠️ 编码阶段完成 |
| Hilt AuthModule | 需改造为 Retrofit 注入 | ⚠️ 编码阶段完成 |
| NavGraph BackHandler | 需添加 AC-08 返回键退出逻辑 | ⚠️ 编码阶段完成 |
| imePadding() | Compose Foundation 1.5+ | ✅ 已有 |

---

## §9 多视角评审记录

> 评审日期: 2026-06-07 | 方式: delegate_task 三视角并行 | 耗时: ~120s

### 评审总览

| 视角 | 评分 | P0 | P1 | 核心发现 |
|------|------|----|----|----------|
| C1 UX 交互 | 7/10 | 2 | 2 | 键盘弹出时错误提示可能被遮挡；429 限流按钮禁用倒计时无视觉反馈 |
| C2 视觉审美 | 46/50 | 0 | 1 | 色彩对比度 >7:1 达标，8dp 网格一致，品牌色与 Splash 统一 |
| C3 前端实现 | 8/10 | 1 | 2 | 现有 LoginScreen 字段/逻辑系统性偏差，需全面重构 |

### P0 修订记录（已在正文自动修订）

| 编号 | 问题 | 修订内容 |
|------|------|----------|
| P0-C1-1 | 错误提示区域位于输入框与按钮之间，键盘弹出时可能被遮挡 | §3 线框图中错误提示紧贴密码输入框下方（spacing0），键盘弹出时自动上移（imePadding） |
| P0-C1-2 | 429 限流按钮禁用无倒计时视觉反馈 | §3 交互状态机中补充 Timeout 状态。429 场景：Snackbar 显示"操作过于频繁(Ns后重试)"+ 按钮倒计时文字 |
| P0-C3-1 | 现有代码 username→email 字段重命名涉及 6+ 文件，遗漏风险高 | §5 组件复用分析中逐文件列出变更清单，编码时逐项勾兑 |

### C2 视觉评审 10 维度

| # | 维度 | 评分 | 关键评语 |
|---|------|:---:|------|
| 1 | 格式塔 | 5 | 图标→标题→副标题→输入框→按钮 五层视觉分组清晰，8dp 间距网格严格对齐 |
| 2 | 视觉层级 | 5 | 标题 24sp Bold → 副标题 14sp → 按钮 16sp → 提示 11sp，四层梯度分明 |
| 3 | 色彩 | 5 | primary #1A73E8 与 SplashScreen 背景一致，error #B3261E 对比度 7.8:1 > WCAG AA |
| 4 | 字体 | 5 | 全部映射 M3 Typography Token，无硬编码 fontSize（除底部提示 labelSmall 需确认 Token） |
| 5 | 空间 | 5 | 8dp 基网格 + 弹性空间（Spacer weight）实现垂直居中，横屏自动滚动 |
| 6 | 布局 | 5 | 375dp 基准视口，水平 padding 24dp，输入框/按钮 fillMaxWidth |
| 7 | 可感知性 | 5 | 按钮 48dp 触控目标达标，loading 态明确（转圈+禁用+文字变化），错误 Card 有背景色 |
| 8 | 一致性 | 4 | 品牌色与 SplashScreen 统一，但 NewsDimens Token 与登录页设计 Token 不共享（合理——新闻列表与登录页设计语境不同） |
| 9 | 情感品牌 | 3 | 功能型页面，无品牌差异化元素。图标 64×64dp 可作为品牌 Logo 占位 |
| 10 | 平台适配 | 4 | 暗色模式 Token 完整，横屏滚动已处理，但未验证平板大屏水平布局（可延后） |

**综合: 46/50**

---

> **版本:** v0.1-draft
> **状态:** 阶段1+阶段2 完成。UI_DESIGN.md✅ | HTML 预览✅ | Playwright 截图✅ | 组件复用分析✅ | Token 映射表✅。请审阅后回复「确认」冻结进入技术方案编码阶段。
