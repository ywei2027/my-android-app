# 用户登录 — UI 设计方案

> **版本:** v0.2-review
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

> 评审日期: 2026-06-07 | 方式: delegate_task 三视角并行 | 耗时: ~406s

### 评审总览

| 视角 | 评分 | P0 | P1 | 核心发现 |
|------|------|----|----|----------|
| C1 UX 交互 | 7/10 | 2 | 2 | 429 限流未独立建模；错误展示组件与 PRD 不一致 |
| C2 视觉审美 | 34/50 | 2 | 4 | 暗色模式完全失效；按钮文字渲染存疑 |
| C3 前端实现 | 8/10 | 3 | 3 | 字段迁移 8 文件遗漏风险；Theme.kt 需新建；DECISIONS 决议缺失 |

**综合: 一票保留（C2 34/50 ≥30 免重设计线，但接近临界）**

### P0 修订记录（已在正文自动修订）

| 编号 | 问题 | 来源 | 修订内容 |
|------|------|:--:|----------|
| P0-C1-1 | 错误提示使用 `Card(errorContainer bg)` 与 PRD 要求 `Text(error color)` 不一致 | C1 | §3 组件层级树 + §9.5 组件选型改为内联 `Text(error)` + `AnimatedVisibility`；**删除 errorCard 用法** |
| P0-C1-2 | 429 限流无独立 UX 建模——PRD TC-07 要求按钮 disabled 30s + 倒计时反馈，设计将 429 等同通用 Error(按钮 enabled) | C1 | §3 交互状态机新增「429 Cooldown」子状态：按钮文字显示倒计时 "请等待 Ns" + disabled；Snackbar 显示限流提示 |
| P0-C2-1 | 暗色模式完全失效——`login_dark.png` 背景色 `#FEFBFF` 与亮色模式无差异，HTML `.dark` class 无对应 CSS 规则 | C2 | §4 Token 表中 Dark 值已定义但 HTML 未绑定。**标记为 HTML 预览问题，编码阶段 Theme.kt 将正确映射 darkColorScheme** |
| P0-C2-2 | 按钮文字像素扫描未检测到 `#FFFFFF`——可能 <16sp 字号 + 图层的渲染问题 | C2 | §4 按钮文字保持 `onPrimary=#FFFFFF`。**编码时使用 `MaterialTheme.colorScheme.onPrimary` 自动获取正确值** |
| P0-C3-1 | username→email 字段重命名涉及 8 文件交叉依赖，遗漏一个即编译失败 | C3 | §5 组件复用分析中已有逐文件变更清单（8 组件表） |
| P0-C3-2 | Theme.kt 不存在——当前项目无此文件，`#1A73E8` 品牌色需在编码前手动创建 M3 Theme | C3 | §8 前置依赖表新增 Theme.kt 新建任务（标注 P0 编码前完成） |
| P0-C3-3 | BackHandler 需在 NavGraph 中处理 Activity.finish()，当前无实现 | C3 | §6 架构协调中已明确 BackHandler 设计，标注为 P0 编码项 |

### C1 UX 交互评审

**评分: 7/10**

**亮点:**
- 状态机 6 态全链路覆盖（Idle→Editing→Loading→Success/Error→Timeout），无遗漏状态
- imeAction 键盘链设计完整：Email→Next 跳转密码框，Password→Done 触发登录
- 无障碍 17 元素逐项标注（contentDescription + 触控目标 + LiveRegion）
- 弹性垂直居中兼容横屏

**P0 问题:**
| # | 问题 | 说明 | 改进建议 |
|---|------|------|----------|
| P0-1 | 429 限流未独立 UX | 设计将 429 等同通用 Error(按钮 enabled)，但 PRD TC-07 要求按钮 disabled 30s + 倒计时 | 新增 Cooldown 子状态：按钮显示 "请等待 Ns" + disabled |
| P0-2 | 错误组件与 PRD 不一致 | 设计用 `Card(errorContainer bg)`，PRD 要求 `Text(error color)` | 改用内联 Text + AnimatedVisibility |

**P1 问题:**
- 键盘弹出时错误提示可能被遮挡（imePadding 处理但需实测验证）
- Success 态无过渡反馈（建议 300ms 绿色 ✓ + "登录成功" → 导航）

**PRD §9 一致性检查: 12/14 完全一致** ✅
核心布局间距/字体/按钮规格与 PRD 严格对齐。Token 映射比 PRD 更细致。

### C2 视觉审美评审（deepseek-v4-pro · 10 维度）

**综合: 34/50**（≥30 免重设计线）

#### 维度评分总览

| # | 维度 | 评分 | 关键评语 |
|---|------|:---:|------|
| 1 | 格式塔感知 | 4 | 五层视觉分组清晰（图标→标题→副标题→输入框→按钮），8dp 网格对齐 |
| 2 | 视觉层级 | 4 | 标题 24sp Bold→副标题 14sp→按钮 16sp→提示 11sp，四层梯度分明 |
| 3 | 色彩系统 | 3 | 品牌色 #1A73E8 一致，但暗色模式未生效（P0）+ 副标题对比度 4.4:1 < WCAG AA |
| 4 | 字体排版 | 4 | 全部映射 M3 Token，行高/字重规范 |
| 5 | 空间与网格 | 4 | 8dp 基准 + 弹性 Space 垂直居中，24dp 水平 padding 统一 |
| 6 | 布局与比例 | 4 | 375dp 基准视口，输入框/按钮 fillMaxWidth，48dp 触控达标 |
| 7 | 可感知可操作 | 4 | 按钮 loading 态明确（转圈+文字变化），错误有视觉区分 |
| 8 | 一致性 | 3 | 品牌色与 SplashScreen 统一，但 NewsDimens 与登录 Token 不共享（合理但需文档说明） |
| 9 | 情感与品牌 | 2 | 功能型页面无品牌差异化，图标 64×64dp 可作为 Logo 占位 |
| 10 | 平台与适配 | 2 | 暗色模式完全失效（P0）→ 拖低平台适配评分 |

#### 详细问题清单

| # | 严重度 | 维度 | 问题 | 改进建议 |
|---|:------:|------|------|----------|
| 1 | P0 | 色彩/平台 | 暗色模式 HTML 未生效，`login_dark.png` 与亮色模式无差异 | 编码阶段 Theme.kt 用 darkColorScheme() 正确映射 |
| 2 | P0 | 可感知 | 按钮文字渲染存疑，像素扫描未检测到 #FFFFFF | 用 `MaterialTheme.colorScheme.onPrimary` 自动获取 |
| 3 | P1 | 色彩 | 副标题对比度 4.4:1 < WCAG AA 4.5:1（#79747E on #FEFBFF） | 改用 `onSurfaceVariant` Token（#49454F=7.2:1） |
| 4 | P1 | 色彩 | Loading 态副标题对比度 1.9:1（disabled opacity 扩散到非输入元素） | Loading 态仅对输入框应用 alpha，文字保持原对比度 |
| 5 | P1 | 可感知 | Idle/Loading 态标题对比度降至 3.17-3.33（应为 16.69） | 检查 loading 状态下是否误用了 alpha 修饰符 |
| 6 | P1 | 情感 | 功能型页面缺乏品牌差异化元素 | 图标 64×64dp 可作为品牌 Logo 占位，后续替换 |

#### 审美亮点
- M3 Token 体系完整（颜色/字体/间距/形状四项齐全）
- 8dp 网格严格对齐
- 四层视觉梯度分明
- 6 状态机设计完整
- 无障碍标注 17 元素
- Phone frame 渲染精致

### C3 前端实现评审

**评分: 8/10**

#### 组件复用率

| 类别 | 数量 | 比例 | 详情 |
|------|:---:|:---:|------|
| 直接复用 | 1 | 8.3% | VersionTag |
| 改造 | 8 | 66.7% | LoginScreen/LoginViewModel/LoginUiState/AuthModels/AuthRepository/AuthModule/LoginStateManager/NavGraph |
| 新建 | 3 | 25.0% | LoginApi/MockAuthInterceptor/LoginEvent |

#### P0 阻塞项

| # | 问题 | 说明 |
|---|------|------|
| P0-1 | 字段迁移遗漏风险 | username→email 涉及 8 文件交叉依赖，逐文件勾兑清单已列在 §5 |
| P0-2 | Theme.kt 需新建 | 当前项目无 Theme.kt，需在编码前创建含 darkColorScheme/lightColorScheme 的 M3 Theme |
| P0-3 | BackHandler 空实现 | NavGraph 需添加 Activity.finish() 逻辑 |

#### P1 重要项
- LoginDimens 对象缺失（应创建 6 个间距 Token 常量）
- passwordVisible 状态归属不明确（建议放入 LoginUiState）
- Email 图标需确认 `Icons.Filled.Email` 在 material-icons-extended 依赖中

#### 工时校准

| 任务 | 工时 | 说明 |
|------|:---:|------|
| Theme.kt 新建 | 1.5h | 定义 lightColorScheme + darkColorScheme + Typography |
| 8 组件重构 | 10h | 逐文件字段迁移 + 逻辑改造 |
| 3 新组件 | 3h | LoginApi + MockAuthInterceptor + LoginEvent |
| 单元测试 | 3h | LoginViewModelTest + LoginScreen preview test |
| 集成调试 | 2.5h | 编译验证 + Mock 联调 |
| **合计** | **20h** | ≈2.5 工作日 |

#### DECISIONS.md 决议对齐

| 决议 | 状态 | 说明 |
|------|:---:|------|
| D-42 HttpException | ⚠️ | AuthRepository 重构时补齐 |
| D-46 testTag | ⚠️ | §3 状态覆盖表已定义 testTag |
| D-55 collectLatest | ⚠️ | LoginViewModel 改造时应用 |
| D-56 withTimeout | ✅ | §6 架构已要求，编码实现 |
| D-58 onCleared | ⚠️ | 编码阶段添加 |
| D-61 preview test | ⚠️ | 编码阶段新建 |
| D-64 writeTimeout+callTimeout | ✅ | 复用 NetworkModule 配置 |


