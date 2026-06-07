# 用户登录 — PRD

> **版本:** v0.1-draft
> **功能名称:** 用户登录
> **创建日期:** 2026-06-07
> **作者:** Hermes 智能研发工作流

---

## §1 功能概述

为用户提供邮箱+密码方式的身份认证入口。用户输入注册邮箱和密码后，系统验证凭据有效性，验证通过后进入主界面，验证失败则展示明确的错误提示。本次实现重点覆盖登录成功和校验失败的完整交互闭环，暂不包含注册、找回密码、第三方登录等功能。

> **注意：** 原 DECISIONS.md 中「登录方式采用手机号+验证码，否决邮箱+密码」的决策将被本次需求覆盖。重新评估邮箱+密码登录作为主要认证方式。

## §2 用户场景

| 场景编号 | 角色 | 场景描述 |
|----------|------|----------|
| US-01 | 普通用户 | 打开应用后看到登录页面，输入已注册的邮箱和密码，点击登录按钮，验证通过后进入应用主界面 |
| US-02 | 普通用户 | 输入错误的邮箱或密码，点击登录后看到错误提示，可重新输入 |
| US-03 | 普通用户 | 未填写邮箱或密码时点击登录按钮，按钮处于禁用状态，无法提交 |
| US-04 | 普通用户 | 输入邮箱格式不正确时，邮箱输入框显示格式校验错误提示 |
| US-05 | 普通用户 | 登录请求发送中时，登录按钮显示加载状态且不可重复点击 |

## §3 范围边界

### 包含
- 登录页面 UI（邮箱输入框 + 密码输入框 + 登录按钮）
- 邮箱格式客户端校验（正则表达式验证）
- 密码非空校验
- 登录按钮联动启用/禁用（邮箱格式正确 + 密码非空 → 启用）
- 登录 API 调用（POST 邮箱+密码）
- 登录成功 → 导航到主界面
- 登录失败 → 展示服务端返回的错误信息（Snackbar/内联提示）
- 登录中的 Loading 状态（按钮转圈 + 禁止重复提交）
- 密码输入框支持显示/隐藏切换

### 不包含
- 用户注册功能
- 忘记密码 / 密码找回
- 第三方登录（Google/微信/Apple）
- 手机号+验证码登录
- "记住我" / 自动登录
- Biometric（指纹/面部）认证
- Token 刷新 / Session 管理（由后续模块负责）

## §4 验收标准

| 编号 | 验收项 | 预期结果 |
|------|--------|----------|
| AC-01 | 邮箱格式校验 | 输入不合规邮箱（缺少@、无域名等）时输入框显示错误提示，登录按钮禁用 |
| AC-02 | 密码非空校验 | 密码为空时登录按钮禁用 |
| AC-03 | 登录按钮联动 | 仅当邮箱格式正确 AND 密码非空时登录按钮可点击 |
| AC-04 | 登录成功跳转 | 输入有效凭据后成功导航到主界面（HomeScreen） |
| AC-05 | 登录失败提示 | 凭据错误时展示服务端错误信息（如"邮箱或密码错误"），用户可重新输入 |
| AC-06 | Loading 态 | 登录请求进行中时按钮显示 CircularProgressIndicator，不可重复点击 |
| AC-07 | 密码显隐切换 | 点击密码输入框的可见性图标可切换密码显示/隐藏 |
| AC-08 | 返回键行为 | 在登录页按系统返回键退出应用（不回到闪屏页） |

## §5 非功能性需求

- **性能:** 登录 API 响应时间 P95 < 2s，客户端校验 < 16ms（不阻塞 UI 线程）
- **安全性:** 密码传输使用 HTTPS，客户端不做明文持久化存储；登录按钮防重复提交
- **兼容性:** Android 8.0 (API 26) 及以上，支持手机和平板竖屏布局
- **可维护性:** 登录逻辑封装在 LoginViewModel 中，UI 仅消费 UiState；错误处理集中在 Repository 层

## §6 技术约束

- MVVM 架构：LoginViewModel 暴露 StateFlow<LoginUiState>
- DI 框架：Hilt
- 网络层：Retrofit + OkHttp
- UI：Jetpack Compose + Material3
- 所有协程绑定 viewModelScope
- 输入框使用 OutlinedTextField（M3 风格）

## §7 风险与依赖

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 后端登录 API 尚未就绪 | 无法端到端验证 | 使用 Mock WebServer (OkHttp MockInterceptor) 先行开发和 UI 测试 |
| 邮箱+密码与 DECISIONS.md 冲突 | 需重新评估登录方案 | 本次 PRD 评审中专题讨论，更新 DECISIONS.md |
| Token 存储方案未定 | 登录成功后 Token 无处存放 | 本次简化：Token 暂存内存，Token 持久化由后续模块负责 |

## §8 术语表

| 术语 | 说明 |
|------|------|
| LoginUiState | 登录页面的 UI 状态数据类，包含邮箱/密码/错误信息/加载状态 |
| LoginEvent | 用户操作事件密封类（邮箱变更/密码变更/登录提交/密码可见性切换） |
| AuthRepository | 认证数据仓库，封装登录 API 调用和错误处理 |
| LoginApi | Retrofit 接口，定义 POST /api/auth/login |

## §9 UI 设计输入

> **状态:** 结构化规格（UX 评审后修订版）
> **基准视口:** 360×640dp (手机竖屏) / 兼容横屏滚动

### 9.1 页面清单

| 页面 | 路由 | 类型 | 入口 | 说明 |
|------|------|------|------|------|
| LoginScreen | `/login` (NavHost startDestination) | 新建 | App 冷启动 → Splash 后 | 单页全屏，无 BottomBar/AppBar |

### 9.2 布局规格（精确 dp 值）

```
┌────────── 360dp ──────────┐
│  statusBarsPadding        │
│                           │
│      [图标 64×64dp]       │  ← 距顶部 80dp（含状态栏）
│      欢迎回来             │  ← headlineMedium (24sp), Bold
│   请使用邮箱和密码登录     │  ← bodyMedium (14sp), onSurfaceVariant
│                           │  ← 间距 32dp
│  ┌─────────────────────┐  │
│  │ 👤 邮箱            │  │  ← OutlinedTextField, height 56dp
│  │    placeholder      │  │     leadingIcon: Icons.Filled.Email
│  └─────────────────────┘  │     keyboardType: Email, imeAction: Next
│                           │  ← 间距 16dp
│  ┌─────────────────────┐  │
│  │ 🔒 密码        👁  │  │  ← OutlinedTextField, height 56dp
│  │    ••••••••         │  │     leadingIcon: Icons.Filled.Lock
│  └─────────────────────┘  │     trailingIcon: Visibility/VisibilityOff
│                           │     keyboardType: Password, imeAction: Done
│  ┌─────────────────────┐  │  ← 内联错误文字 12sp error 色（input下方4dp）
│  │ ⚠ 邮箱或密码错误    │  │     AnimatedVisibility fadeIn/Out
│  └─────────────────────┘  │
│                           │  ← 间距 24dp
│  ┌─────────────────────┐  │
│  │     登  录           │  │  ← Button(filled), height 48dp, fullWidth
│  └─────────────────────┘  │     cornerRadius 24dp (50% pill)
│     ◌ 登录中...          │  ← loading态：CircularProgressIndicator(20dp)
│                           │       + "登录中..." text inside button
│                           │  ← 间距 24dp
│  测试账号: admin/123456   │  ← labelSmall (11sp), alpha 0.6
│                           │
│      1.0(1)debug          │  ← VersionTag 底部居中 (如存在)
│  navigationBarsPadding    │
└───────────────────────────┘
```

| 元素 | 精确规格 | M3 Token |
|------|----------|----------|
| 页面容器 | `Column` fillMaxSize, verticalScroll, horizontalPadding=24dp, verticalArrangement=Center | — |
| 应用图标 | `Icon` 64×64dp, painterResource, tint=primary | `primary` |
| 标题文字 | `Text` "欢迎回来", 24sp, Bold | `headlineMedium` |
| 副标题文字 | `Text` "请使用邮箱和密码登录", 14sp, regular | `bodyMedium`, `onSurfaceVariant` |
| 图标→标题间距 | `Spacer` 16dp | — |
| 标题→副标题间距 | `Spacer` 8dp | — |
| 副标题→邮箱输入框间距 | `Spacer` 32dp | — |
| 邮箱输入框 | `OutlinedTextField`, height=56dp, singleLine, shape=4dp | — |
| 邮箱 leadingIcon | `Icons.Filled.Email`, contentDescription="邮箱图标" | — |
| 邮箱 placeholder | "请输入邮箱地址" | — |
| 邮箱错误文案 | "请输入有效的邮箱地址", 12sp, error color | `error`, `labelSmall` |
| 邮箱↔密码间距 | `Spacer` 16dp | — |
| 密码输入框 | `OutlinedTextField`, height=56dp, singleLine, shape=4dp | — |
| 密码 leadingIcon | `Icons.Filled.Lock`, contentDescription="密码图标" | — |
| 密码 trailingIcon | `IconButton` 48×48dp touch target, `Icons.Filled.Visibility`/`VisibilityOff` | — |
| 密码 placeholder | "请输入密码" | — |
| 密码→错误提示间距 | 0dp（内联错误在输入框 `supportingText` 或下方 4dp） | — |
| 服务端错误提示 | `Text` 14sp, error color, AnimatedVisibility fadeIn/Out | `error`, `bodySmall` |
| 错误→按钮间距 | `Spacer` 24dp | — |
| 登录按钮 | `Button`(filled), height=48dp, fullWidth, shape=RoundedCornerShape(24dp) | `primary` / `onPrimary` |
| 按钮 loading | `CircularProgressIndicator` 20dp, strokeWidth=2dp, color=onPrimary | `onPrimary` |
| 按钮文字(默认) | "登录", 16sp, Medium | `labelLarge` |
| 按钮文字(loading) | "登录中...", 16sp | `labelLarge` |
| 按钮→底部提示间距 | `Spacer` 24dp | — |
| 底部测试提示 | "测试账号: admin / 123456", 11sp, onSurfaceVariant alpha 0.6 | `labelSmall` |

### 9.3 交互规格

| 触发元素 | 交互行为 | 键盘/焦点管理 |
|----------|----------|---------------|
| 邮箱输入框 | 输入时实时校验 RFC 5322 格式。格式错误 → `supportingText` 显示 "请输入有效的邮箱地址" + 输入框变 error 态。格式正确 → 清除 error 态。 | `keyboardType=Email`, `imeAction=Next` → 焦点跳转密码框 |
| 密码输入框 | 输入时联动按钮启用状态。`trailingIcon` 点击切换 PasswordVisualTransformation ↔ None。`imeAction=Done` 触发登录。 | `keyboardType=Password`, `imeAction=Done` → 收起键盘+触发登录 |
| 登录按钮 | **enable 条件**: `isEmailValid AND password.isNotEmpty() AND !isLoading`。点击 → 键盘收起 → loading 态(按钮内 CircularProgressIndicator + "登录中...") → 按钮 disabled。成功 → navigate HomeScreen。失败 → AnimatedVisibility 显示错误 + 按钮恢复 enabled。 | 点击时 `focusManager.clearFocus()` + `keyboardController?.hide()` |
| 错误提示 | 内联显示在按钮上方。服务端错误/网络错误均在此区域。下次输入任意字段时自动清除。超时 10s 自动消失。 | 无焦点影响 |
| 返回键 | `BackHandler` 拦截 → `activity.finish()` 退出应用（不返回 Splash）。 | — |

### 9.4 状态覆盖

| 状态 | 触发条件 | UI 表现 | 测试标识 |
|------|----------|---------|----------|
| **默认 (Idle)** | 页面首次加载 | 邮箱/密码为空，按钮 disabled，无错误文字 | `testTag="login_screen_idle"` |
| **输入中 (Editing)** | 用户输入邮箱/密码 | 按钮联动启用/禁用。邮箱格式错误时输入框变 error 态 | `testTag="login_screen_editing"` |
| **加载中 (Loading)** | 点击登录按钮 → API 请求进行中 | 按钮 disabled + 显示 `CircularProgressIndicator`(20dp) + "登录中..."；输入框 disabled | `testTag="login_screen_loading"` |
| **成功 (Success)** | API 返回 200 | 短暂展示成功态（可选）→ navigate HomeScreen | `testTag="login_screen_success"` |
| **错误 (Error)** | API 返回 401/403/429 或网络异常 | 按钮恢复 enabled；按钮上方显示错误 Card/text；输入框 enabled | `testTag="login_screen_error"` |
| **空状态 (Empty)** | 用户清空所有输入 | 按钮 disabled，格式错误清除，服务端错误清除 | `testTag="login_screen_empty"` |
| **网络超时 (Timeout)** | 请求 > 10s 无响应 | 显示 "网络请求超时，请重试"；按钮恢复 enabled | `testTag="login_screen_timeout"` |
| **深色模式** | 系统 dark theme 激活 | 所有颜色通过 M3 Token 自动适配 | — |
| **横屏** | 设备旋转 landscape | `verticalScroll` 确保内容可滚动到按钮 | — |

### 9.5 组件选型（含推荐 M3 组件）

| UI 元素 | 推荐 M3 组件 | 备选 | 选择理由 |
|----------|-------------|------|----------|
| 邮箱/密码输入框 | `OutlinedTextField` | `TextField`(filled) | M3 默认输入框风格，项目现有组件一致 |
| 输入框错误态 | `OutlinedTextField(isError=true, supportingText={...})` | 独立 `Text` | M3 原生错误态内置红色边框+错误文字，语义化更好 |
| 登录按钮 | `Button` (filled) | `FilledTonalButton` | 登录为页面核心 CTA，filled 层级最高 |
| 加载指示器 | `CircularProgressIndicator`(indeterminate, size=20dp) | `LinearProgressIndicator` | 按钮内小尺寸转圈，标准模式 |
| 密码显隐图标 | `IconButton` + `Icons.Filled.Visibility`/`VisibilityOff` | `IconToggleButton` | 标准做法，48dp 触控目标 |
| 错误/成功通知 | 内联 `Text`(error color) + `AnimatedVisibility` | `Snackbar` | 内联不遮挡输入框，视觉归属明确。Snackbar 仅用于全局操作反馈 |
| 键盘避让 | `Modifier.imePadding()` | `WindowInsets.ime` 手动处理 | Compose 官方 API，简洁可靠 |
| 横屏滚动 | `Modifier.verticalScroll(rememberScrollState())` | — | 防止小屏横屏内容截断 |
| 页面容器 | `Scaffold`（仅用 padding）或 `Box`+`fillMaxSize` | — | 无 AppBar/BottomBar 时 `Box` 更轻量 |

### 9.6 设计约束

#### 品牌色 / 色彩 Token

| Token | Light 值 | Dark 值 | 用途 |
|-------|----------|---------|------|
| `primary` | `#1A73E8` | `#8AB4F8` | 按钮背景、图标、链接 |
| `onPrimary` | `#FFFFFF` | `#003A75` | 按钮文字、loading 指示器 |
| `primaryContainer` | `#D3E3FD` | `#004A77` | 输入框焦点背景（可选） |
| `onPrimaryContainer` | `#001D36` | `#D3E3FD` | — |
| `error` | `#B3261E` | `#F2B8B5` | 输入框错误边框、错误文字 |
| `errorContainer` | `#F9DEDC` | `#8C1D18` | 错误提示卡片背景 |
| `onErrorContainer` | `#410E0B` | `#F9DEDC` | 错误卡片文字 |
| `surface` | `#FEFBFF` | `#1C1B1F` | 页面背景 |
| `onSurface` | `#1C1B1F` | `#E6E1E5` | 标题文字 |
| `onSurfaceVariant` | `#49454F` | `#CAC4D0` | 副标题、placeholder |

> **来源:** M3 默认 ColorScheme 基础 + `primary` 品牌色 `#1A73E8`（与现有 SplashScreen 背景一致）。建议在 `Theme.kt` 中通过 `lightColorScheme()` / `darkColorScheme()` 显式定义。

#### 字体层级

| 角色 | M3 Token | Size | Weight | LetterSpacing |
|------|----------|------|--------|---------------|
| 页面标题 | `headlineMedium` | 24sp | Bold (700) | 0 |
| 副标题/提示 | `bodyMedium` | 14sp | Regular (400) | 0.25sp |
| 输入框文字 | `bodyLarge` | 16sp | Regular (400) | 0.5sp |
| 输入框标签 | `bodySmall` → `labelSmall` (折叠后) | 12sp | Regular | 0.5sp |
| 按钮文字 | `labelLarge` | 16sp | Medium (500) | 0.1sp |
| 错误提示 | `bodySmall` | 14sp | Regular | 0.25sp |
| 测试账号提示 | `labelSmall` | 11sp | Regular | 0.5sp |
| 输入框 placeholder | `bodyLarge` | 16sp | Regular, alpha 0.6 | 0.5sp |

#### 间距网格（8dp 基础）

```
基准: 1x = 8dp
┌──────────┬──────┬───────────────────────────────┐
│ Token    │ 值   │ 用途                          │
├──────────┼──────┼───────────────────────────────┤
│ spacing0 │  0dp │ 内联错误紧贴输入框             │
│ spacing1 │  8dp │ 标题↔副标题，按钮内图标↔文字  │
│ spacing2 │ 16dp │ 标题↔图标，输入框之间，边距    │
│ spacing3 │ 24dp │ 水平 padding，错误↔按钮        │
│ spacing4 │ 32dp │ 副标题↔邮箱输入框              │
│ spacing5 │ 40dp │ (预留) 大段间距                │
├──────────┼──────┼───────────────────────────────┤
│ iconSize │ 64dp │ 应用图标                       │
│ fieldH   │ 56dp │ OutlinedTextField 高度         │
│ btnH     │ 48dp │ Button 高度                    │
│ btnR     │ 24dp │ Button 圆角半径 (pill)         │
│ touchTgt │ 48dp │ IconButton 最小触摸目标         │
│ spnrSize │ 20dp │ CircularProgressIndicator      │
└──────────┴──────┴───────────────────────────────┘
```

## §10 验收测试用例

> **产出方:** QA Agent（PRD 评审阶段并行产出）
> **格式:** Gherkin（Given/When/Then）
> **用途:** 编码阶段直接执行红绿循环
> **修订:** v1.0-qa-review — 新增超时/403/429/状态枚举覆盖

### 场景组 A：登录核心流程（P0）

```gherkin
Scenario: TC-01 成功登录 — 完整闭环
  Given 用户在登录页面，登录状态为 Idle
  And   邮箱输入框和密码输入框均为空
  And   登录按钮处于 disabled 状态
  When  用户输入有效邮箱 "user@example.com"
  And   邮箱格式校验通过，输入框无 error 态
  And   用户输入非空密码 "password123"
  And   登录按钮变为 enabled 状态（邮箱正确 AND 密码非空）
  When  用户点击登录按钮
  Then  登录按钮立即变为 disabled，显示 CircularProgressIndicator(20dp) 和文字 "登录中..."
  And   邮箱输入框和密码输入框变为 disabled
  And   服务端返回 HTTP 200 { token, user }
  Then  登录状态切换为 Success
  And   导航到 HomeScreen（不经过 Splash）

Scenario: TC-02 邮箱格式实时校验 — 输入中校验
  Given 用户在登录页面，登录状态为 Idle
  When  用户在邮箱输入框输入 "invalid-email"
  Then  每输入一个字符，实时校验 RFC 5322 格式
  And   输入框变为 isError=true，supportingText 显示 "请输入有效的邮箱地址"
  And   登录按钮保持 disabled（不满足"邮箱格式正确"条件）
  When  用户将邮箱修改为 "user@example.com"
  Then  邮箱输入框 error 态清除，supportingText 消失
  And   若密码非空，登录按钮变为 enabled

Scenario: TC-03 按钮联动 — 正向启用条件
  Given 用户在登录页面，登录状态为 Idle
  And   邮箱输入框为空，密码输入框为空
  And   登录按钮为 disabled
  When  用户输入有效邮箱 "user@example.com"
  Then  登录按钮仍为 disabled（密码为空）
  When  用户输入非空密码 "123456"
  Then  登录按钮变为 enabled（邮箱有效 AND 密码非空）
  When  用户清空密码
  Then  登录按钮恢复 disabled
  When  用户重新输入密码 "123456"
  And   用户将邮箱改为 "bad-email"
  Then  邮箱输入框显示格式错误
  And   登录按钮恢复 disabled

Scenario: TC-04 登录失败 — 401 凭据错误
  Given 用户在登录页面
  And   用户已输入有效邮箱 "wrong@example.com" 和非空密码 "wrongpwd"
  When  用户点击登录按钮
  Then  按钮进入 Loading 态（disabled + CircularProgressIndicator）
  And   服务端返回 HTTP 401 { code: 401, message: "邮箱或密码错误" }
  Then  登录状态切换为 Error
  And   按钮上方显示内联错误 Card：文字 "邮箱或密码错误"，errorContainer 背景色
  And   登录按钮恢复 enabled（退出 Loading 态）
  And   邮箱和密码输入框恢复 enabled
  When  用户开始修改邮箱或密码输入
  Then  错误提示自动清除（AnimatedVisibility fadeOut）

Scenario: TC-05 网络超时
  Given 用户在登录页面
  And   用户已输入有效邮箱 "user@example.com" 和非空密码 "password123"
  When  用户点击登录按钮
  And   服务端 10 秒内无任何响应（OkHttp timeout 触发 SocketTimeoutException）
  Then  登录状态切换为 Error
  And   按钮上方显示内联错误 Card：文字 "网络请求超时，请重试"
  And   登录按钮恢复 enabled
  And   邮箱和密码输入框恢复 enabled
  And   用户可重新点击登录按钮发起重试
```

### 场景组 B：错误码与边界场景（P1）

```gherkin
Scenario: TC-06 登录失败 — 403 账户锁定
  Given 用户在登录页面
  And   用户已输入有效邮箱 "locked@example.com" 和非空密码
  When  用户点击登录按钮
  And   服务端返回 HTTP 403 { code: 403, message: "账户已被锁定，请联系管理员" }
  Then  按钮上方显示内联错误 Card：文字 "账户已被锁定，请联系管理员"
  And   登录按钮恢复 enabled（用户可尝试其他账号）
  And   输入框恢复 enabled

Scenario: TC-07 登录失败 — 429 限流
  Given 用户在登录页面
  And   用户已输入有效邮箱和非空密码
  When  用户点击登录按钮
  And   服务端返回 HTTP 429 { code: 429, message: "操作过于频繁，请稍后再试" }
  Then  按钮上方显示内联错误 Card：文字 "操作过于频繁，请稍后再试"
  And   登录按钮恢复 disabled 并保持 disabled 至少 30 秒（防暴力破解）
  And   30 秒后登录按钮自动恢复 enabled

Scenario: TC-08 状态枚举全流转验证
  Given 用户在登录页面，初始状态为 Idle（邮箱空/密码空/按钮 disabled/无错误）
  When  用户输入有效邮箱和非空密码
  Then  状态进入 Editing（按钮 enabled/无 error 态）
  When  用户点击登录按钮
  Then  状态进入 Loading（按钮 disabled+转圈/输入框 disabled/无错误）
  And   服务端返回 HTTP 200
  Then  状态进入 Success（isLoggedIn=true）并导航
  # 第二个子流程：Error → Editing
  Given 用户在登录页面，刚收到 401 错误
  Then  状态为 Error（错误 Card 可见/按钮 enabled/输入框 enabled）
  When  用户在邮箱输入框输入一个新字符
  Then  状态回到 Editing（错误 Card 消失/按钮联动校验）
```

### 场景组 C：交互细节（P1）

```gherkin
Scenario: TC-09 密码显隐切换
  Given 用户在登录页面
  And   密码框内容为 "mypassword"，当前为密文显示（圆点）
  When  用户点击密码输入框 trailingIcon（VisibilityOff 图标）
  Then  密码以明文 "mypassword" 显示
  And   trailingIcon 变为 Visibility 图标，contentDescription="隐藏密码"
  When  用户再次点击 trailingIcon
  Then  密码恢复密文显示
  And   trailingIcon 恢复 VisibilityOff 图标，contentDescription="显示密码"

Scenario: TC-10 返回键退出应用
  Given 用户在登录页面
  When  用户按下系统返回键
  Then  BackHandler 拦截事件
  And   调用 Activity.finish()
  Then  应用退出到系统桌面，不返回 SplashScreen 或其他页面
```

### 场景覆盖矩阵

| 场景编号 | 场景标题 | 覆盖的 AC | §11.3 状态覆盖 | API 错误码覆盖 | 优先级 |
|----------|----------|:---------:|:-------------:|:-------------:|:------:|
| TC-01 | 成功登录 — 完整闭环 | AC-03, AC-04, AC-06 | Idle→Editing→Loading→Success | 200 | **P0** |
| TC-02 | 邮箱格式实时校验 | AC-01, AC-03 | Idle→Editing | — | **P0** |
| TC-03 | 按钮联动 — 正向启用条件 | AC-02, AC-03 | Idle↔Editing | — | **P0** |
| TC-04 | 登录失败 — 401 凭据错误 | AC-05, AC-06 | Editing→Loading→Error→Editing | 401 | **P0** |
| TC-05 | 网络超时 | AC-05, AC-06 | Editing→Loading→Error | SocketTimeoutException | **P0** |
| TC-06 | 登录失败 — 403 账户锁定 | AC-05 | Editing→Loading→Error | 403 | P1 |
| TC-07 | 登录失败 — 429 限流 | AC-05 | Editing→Loading→Error | 429 | P1 |
| TC-08 | 状态枚举全流转验证 | AC-03, AC-04, AC-05, AC-06 | Idle/Editing/Loading/Success/Error 全覆盖 | — | P1 |
| TC-09 | 密码显隐切换 | AC-07 | — | — | P1 |
| TC-10 | 返回键退出应用 | AC-08 | — | — | P1 |

> **AC 覆盖汇总:** AC-01 ✅ | AC-02 ✅ | AC-03 ✅ | AC-04 ✅ | AC-05 ✅ | AC-06 ✅ | AC-07 ✅ | AC-08 ✅ — 8/8 全覆盖
> **§11.3 状态枚举全覆盖:** Idle ✅ | Editing ✅ | Loading ✅ | Success ✅ | Error ✅ — 5/5 全覆盖
> **§11.1 错误码覆盖:** 401 ✅ | 403 ✅ | 429 ✅ — 3/3 全覆盖
> **网络异常覆盖:** SocketTimeoutException ✅

---

## §11 数据契约

> **产出方:** 技术 Agent（PRD 评审阶段产出）
> **格式:** JSON Schema
> **用途:** 编码前门控验证 + 编码 prompt 注入

### 11.1 API 接口定义

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "LoginRequest",
  "type": "object",
  "required": ["email", "password"],
  "properties": {
    "email": {
      "type": "string",
      "format": "email",
      "maxLength": 254
    },
    "password": {
      "type": "string",
      "minLength": 1,
      "maxLength": 128
    }
  }
}
```

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "LoginResponse",
  "type": "object",
  "required": ["token", "user"],
  "properties": {
    "token": {
      "type": "string",
      "description": "JWT access token"
    },
    "user": {
      "type": "object",
      "required": ["id", "email", "displayName"],
      "properties": {
        "id": { "type": "string" },
        "email": { "type": "string", "format": "email" },
        "displayName": { "type": "string" }
      }
    }
  }
}
```

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "LoginErrorResponse",
  "type": "object",
  "required": ["code", "message"],
  "properties": {
    "code": {
      "type": "integer",
      "enum": [401, 403, 429]
    },
    "message": {
      "type": "string",
      "description": "Human-readable error message"
    }
  }
}
```

### 11.2 数据模型

| 字段 | 类型 | 必填 | 约束 | 说明 |
|------|------|:---:|------|------|
| email | String | ✅ | RFC 5322 格式, max 254 | 用户邮箱 |
| password | String | ✅ | 1-128 字符 | 用户密码 |
| token | String | ✅ | JWT 格式 | 认证令牌 |
| user.id | String | ✅ | 非空 | 用户唯一 ID |
| user.email | String | ✅ | 邮箱格式 | 用户邮箱 |
| user.displayName | String | ✅ | 非空 | 用户展示名称 |
| error.code | Int | ✅ | 401/403/429 | 错误码 |
| error.message | String | ✅ | 非空 | 错误文案 |

### 11.3 状态枚举

| 状态 | 值 | 说明 |
|------|-----|------|
| Idle | idle | 初始状态，用户可输入 |
| Loading | loading | 登录请求进行中 |
| Success | success | 登录成功，待导航 |
| Error | error | 登录失败，展示错误信息 |

---

## §12 多视角评审记录

> 评审日期: 2026-06-07
> 评审方式: 4-Agent 并行评审（产品视角 / 技术视角 / UX 视角 / QA 视角）

### 12.1 评审总览

| 视角 | 评分 | P0 项 | P1 项 | 结论 |
|------|------|-------|-------|------|
| 产品视角 | 6 | 3 | 3 | ⚠️ 需修订 |
| 技术视角 | 7 | 8 | 8 | ⚠️ 需修订 |
| UX 视角 | 5 | 6 | 5 | ⚠️ 需修订 |
| QA 视角 | 5 | 5 | 3 | ⚠️ 需修订 |

### 12.2 产品视角评审

#### 产品评分：**6 / 10**

#### 评审意见

**亮点：**
- §2 用户场景覆盖了核心登录闭环（成功、失败、校验、防重复提交），颗粒度合理
- §3 范围边界清晰，明确列出包含/不包含，有利于控制首版 scope
- §10 Gherkin 用例可直接驱动 TDD 红绿循环，验收标准已对齐
- §11 数据契约完整，状态枚举（Idle/Loading/Success/Error）设计规范

**关键问题：**

1. **注册缺口 — 致命缺陷。** 登录页面是用户进入应用的第一个触点，但 PRD 明确不包含注册功能。如果这是一个新应用或者用户没有预分配账号，登录页面形同虚设。用户在登录页输入什么凭据？从哪里获得凭据？该问题不解决，整个模块无用户价值。

2. **Token 仅存内存 — 每次重启需重新登录。** §7 明确 Token 暂存内存、持久化由"后续模块负责"。这意味着用户每次冷启动都要重新输入邮箱密码，移动端高频使用场景下体验极差，留存率堪忧。Token 持久化应作为 P0 纳入登录模块职责，而非推给未定义的"后续模块"。

3. **网络异常场景缺失。** §2/US 场景和 §10/测试用例均未覆盖超时、断网、服务不可用等异常状态。用户在网络不稳定环境下仅看到加载转圈无任何反馈，会导致困惑和放弃。

4. **错误码覆盖不完整。** §11 定义了 401/403/429 三种错误码，但 §2 场景和 §10 用例仅覆盖了 401（密码错误）。403（账户锁定）和 429（限流）的交互设计缺失，用户面对这些错误时无明确指引。

5. **与 DECISIONS.md 决策冲突未充分论证。** 原决策明确否决邮箱+密码（理由：增加开发成本，试点阶段无必要），PRD 直接覆盖但未提供数据或场景分析支撑该变更。需补充：为什么邮箱+密码比手机号+验证码更适合当前阶段？用户调研或竞品分析依据是什么？

#### 改进建议

| # | 建议 | 优先级 | 说明 |
|---|------|:------:|------|
| S1 | 补充注册入口或明确账号来源 | **P0** | 可简化为：首版由管理员预分配账号/邀请链接，或增加最简注册页（邮箱+密码+确认密码） |
| S2 | Token 持久化纳入首版 | **P0** | 使用 DataStore 加密存储 Token，应用启动时自动校验有效性，避免重复登录 |
| S3 | 补充网络异常场景 | **P0** | US-06：网络超时→显示"网络连接失败，请重试"并允许重试 |
| S4 | 补充 403/429 错误处理 | **P1** | 403→"账户已被锁定，请联系管理员"；429→"操作过于频繁，请稍后再试"并禁用按钮 N 秒 |
| S5 | 决策冲突补充论证 | **P1** | 在 §1 增加决策变更说明：用户画像、竞品对标、团队讨论结论 |
| S6 | 补充"忘记密码"入口占位 | **P1** | 不实现功能，但 UI 预留"忘记密码？"文字链接（点击 Toast 提示"功能开发中"），降低用户卡死焦虑 |

#### 风险矩阵

| 风险 | 影响 | 概率 | 等级 | 缓解 |
|------|:--:|:--:|:--:|------|
| 无注册/账号来源，登录功能无用户可用 | 高 | 高 | 🔴 严重 | 补充 admin 预分配账号方案或最简注册页 |
| Token 不持久化，每次冷启动重登录致用户流失 | 高 | 高 | 🔴 严重 | DataStore 加密存 Token，启动自动验有效性 |
| 与 DECISIONS.md 决策冲突致团队分歧 | 中 | 高 | 🟡 高 | §1 补充决策变更论证，团队评审确认 |
| 后端 API 未就绪阻塞联调 | 高 | 中 | 🟡 高 | MockInterceptor 先行（已列在 §7，合理） |
| 网络异常无反馈致用户困惑放弃 | 中 | 中 | 🟢 中 | 增加超时/断网场景和重试机制 |

#### 结论

PRD 在登录交互闭环、数据契约、验收用例方面质量扎实，但存在两个致命产品缺口：**用户无账号来源**和**Token 不持久化**。建议将 S1/S2/S3 列为 P0 修订项，补全后 PRD 可达 8/10 水平，进入编码阶段。当前状态（6/10）不建议直接编码，需先解决注册缺口和 Token 持久化方案。

### 12.3 技术视角评审

#### 技术评分：**7 / 10**

**扣分项：**
- DECISIONS.md 明确否决过邮箱+密码（-1.5），PRD 虽声明覆盖但无详细论证
- 现有代码是用户名+密码，与 PRD 邮箱+密码存在系统性字段重命名冲突（-1）
- Token 存储方案完全延后，增加后续集成风险（-0.5）

**加分项：** MVVM+Compose 架构契合现有项目、范围边界清晰、Gherkin 测试用例完整、JSON Schema 已定义。

#### P0 阻塞项

| # | 问题 | 依据 |
|---|------|------|
| P0-1 | **DECISIONS.md 冲突未正式解决。** 原决策明确否决邮箱+密码（2026-05-31），PRD 仅加注释说要"重新评估"，但缺乏评估结论和更新条目 | DECISIONS.md L29-32 |
| P0-2 | **LoginApi (Retrofit) 缺失。** 现有 AuthRepository 是硬编码 mock，不通过 Retrofit+OkHttp 调用后端 API，违反 §6 技术约束 | 现有 AuthRepository.kt |
| P0-3 | **LoginResponse 数据模型不匹配。** 现有 `LoginResponse(token, username)` 只有2字段，PRD §11 要求 `LoginResponse(token, user{id, email, displayName})` 共4字段 | 现有 AuthModels.kt |
| P0-4 | **邮箱格式校验缺失。** LoginViewModel 没有邮箱格式验证逻辑（AC-01），LoginScreen 使用 username 字段而非 email | LoginViewModel.kt |
| P0-5 | **登录按钮启用条件错误。** 现有代码按钮仅判断 `!isLoading`，PRD 要求「邮箱格式正确 AND 密码非空」（AC-03） | LoginScreen.kt |
| P0-6 | **返回键退出应用未实现（AC-08）。** LoginScreen 和 NavGraph 没有处理系统返回键退出到桌面的逻辑 | PRD §4 AC-08 |
| P0-7 | **AuthRepository 缺少 HttpException 捕获。** DECISIONS.md D-42 决议要求捕获 HttpException + Exception，现有 mock 无异常处理 | DECISIONS.md D-42 |
| P0-8 | **AuthRepository 缺少 withTimeout 超时保护。** DECISIONS.md D-56 要求所有 Repository 方法添加超时 | DECISIONS.md D-56 |

#### P1 重要项

| # | 问题 | 依据 |
|---|------|------|
| P1-1 | LoginStateManager 仍存 `username` 键，需改为 user 对象（email + displayName） | PRD §11.2 |
| P1-2 | LoginViewModel 未使用 `collectLatest` 模式处理事件（D-55） | DECISIONS.md D-55 |
| P1-3 | LoginViewModel 无 `onCleared()` 显式清理（D-58） | DECISIONS.md D-58 |
| P1-4 | 缺少 LoginScreen 独立 preview test（D-61） | DECISIONS.md D-61 |
| P1-5 | 密码显隐切换图标缺少 testTag（D-46） | DECISIONS.md D-46 |
| P1-6 | AuthModule 手动 new AuthRepository()，应用 Hilt + Retrofit 注入代替 | 现有 AuthModule.kt |
| P1-7 | Token 仅存内存 + DataStore 但无 session 过期/刷新策略 | PRD §7 |
| P1-8 | `LoginUiState` 缺少显式状态枚举字段（Idle/Loading/Success/Error） | PRD §11.3 |

#### 新增/重构组件清单

| 组件 | 类型 | 当前状态 | PRD 要求 |
|------|------|----------|----------|
| `LoginApi` | Retrofit 接口 | **缺失** — 需新建 | `POST /api/auth/login` |
| `AuthRepository` | Repository | **需重构** — 现有 mock 改 Retrofit 调用 + HttpException 处理 + withTimeout | 调用 LoginApi |
| `LoginViewModel` | ViewModel | **需重构** — username→email，加邮箱校验，加按钮联动逻辑，加状态枚举 | StateFlow\<LoginUiState\> |
| `LoginScreen` | Composable | **需重构** — username→email，OutlinedTextField 加 email keyboardType，按钮启用条件 | M3 OutlinedTextField |
| `LoginUiState` | Data Class | **需重构** — username→email，增加显式 status 枚举字段 | 含 email/password/status/errorMessage |
| `LoginEvent` | Sealed Class | **需新建** | EmailChanged/PasswordChanged/SubmitLogin/TogglePasswordVisibility |
| `AuthModels` | Domain Model | **需重构** — LoginResponse 加 user{id,email,displayName} | PRD §11.1 |
| `AuthModule` | Hilt DI | **需重构** — 改 Retrofit 注入 | 提供 LoginApi + AuthRepository |
| `LoginStateManager` | DataStore | **需重构** — username→user 对象 | 存 token + user info |
| `MockAuthInterceptor` | OkHttp Interceptor | **需新建** — 参考 MockNewsInterceptor 模式 | Mock 登录 API 响应 |
| `NavGraph` | Navigation | **需修改** — 添加返回键退出处理 | AC-08 |

#### 技术风险表

| 风险 | 严重度 | 概率 | 缓解措施 |
|------|:------:|:----:|----------|
| DECISIONS.md 决策冲突未解决导致团队分歧 | 🔴 高 | 高 | §12.6 R-01 需产出正式决策：说明为何推翻手机号+验证码 |
| 后端 API 契约未确认 | 🔴 高 | 中 | §11 Schema 先与后端对齐；MockAuthInterceptor 按 Schema 模拟 |
| Token 持久化延后导致后续模块无法联调 | 🟡 中 | 高 | 明确 Token 内存+DataStore 接口已就绪，后续对接 refresh 逻辑 |
| 字段重命名影响已有 DataStore key | 🟡 中 | 中 | DataStore key 无 schema 约束，直接改 key 名 |
| LoginApi 与 NewsApiService 共用 OkHttpClient | 🟢 低 | 低 | Auth 请求发往自有后端，不带 NewsAPI Key 无影响 |

#### 与 DECISIONS.md 既有决议对齐检查

| 决议 | 登录模块状态 |
|------|:-----------:|
| D-40 Room LIKE 不用 FTS4 | N/A（登录不涉及 Room） |
| D-42 捕获 HttpException | ❌ 缺失 |
| D-46 testTag 标记 | ⚠️ 部分缺失 |
| D-48 Timber 日志 | ✅ ViewModel 已使用 |
| D-50 API Key Interceptor 注入 | N/A（Auth 用自有后端） |
| D-55 collectLatest | ❌ 缺失 |
| D-56 withTimeout | ❌ 缺失 |
| D-58 onCleared 清理 | ❌ 缺失 |
| D-61 preview test | ❌ 缺失 |
| D-64 writeTimeout+callTimeout | ✅ NetworkModule 已配置 |

#### 结论

PRD 范围清晰、技术栈匹配，但现有代码（用户名+密码 mock）与 PRD（邮箱+密码 Retrofit）存在系统性偏差，8 项 P0 阻塞项需在编码前全部解决。最大风险是 DECISIONS.md 决策冲突未正式更新，建议优先产出对比评估与决策更新条目。

### 12.4 UX 视角评审

#### UX 评分：**5 / 10**

#### 评审意见

**亮点：**
- 交互闭环完整：默认→输入→校验→提交→loading→成功/失败，状态覆盖意识好
- §9 原始稿已具备基础 UI 规格（组件选型、dp 值、交互描述）
- §10 Gherkin 用例覆盖了密码显隐、防重复提交等细节交互
- 横屏/键盘适配在 §9 约束中已有声明（imePadding + verticalScroll）

**关键问题：**

1. **M3 规范合规性严重不足。** 项目声称 Material3 但 PRD §9 未定义品牌色 hex 值、未指定字体 Token 层级、无间距网格标准。编码阶段无法产出统一的 Theme.kt，各 Composable 将使用硬编码值，导致视觉碎片化。原稿中「圆角 8dp」「错误提示 14sp」「按钮圆角 24dp」均为硬编码，未映射到 `RoundedCornerShape` / `MaterialTheme.typography` Token。

2. **无障碍 (A11Y) 完全缺位。** 全文无 `contentDescription`、`semantics`、`testTag` 的 UX 层面规范。TalkBack 用户无法获知：按钮当前是"登录"还是"登录中..."，错误提示出现时是否被朗读，密码显隐切换的状态描述。WCAG 2.5.3 (Label in Name) 和 4.1.2 (Name, Role, Value) 均不满足。

3. **错误展示方案内部矛盾。** §3(包含) 说「Snackbar/内联提示」，§9 组件选型推荐「Snackbar」但理由是"非阻断式"，而 §9 交互规格又说"按钮下方 Snackbar 或内联文字"。PRD 实际推荐 Snackbar，但 Snackbar 会遮挡底部输入框（键盘弹出时尤为严重），登录场景更合理的选择是内联错误提示（`supportingText` + `AnimatedVisibility`）。此矛盾必须在 §9 修订版中统一。

4. **按钮启用逻辑缺失前端校验。** AC-03 要求「邮箱格式正确 AND 密码非空 → 启用」，但现有代码 `LoginScreen.kt` 中 `enabled = !uiState.isLoading`，未联动校验结果。UiState 缺少 `isEmailValid` 字段，validation 逻辑未前移到 `onEmailChanged`。

5. **键盘类型与焦点管理未指定。** 邮箱输入框应使用 `KeyboardType.Email` + `imeAction=Next` 跳转密码框，密码框应 `imeAction=Done` 触发登录。PRD 原稿仅写"键盘收起"，未描述焦点链和键盘类型。

6. **边界状态覆盖缺失。** 缺失：网络超时 UI（请求 >10s 无响应，当前代码无 timeout 处理）、429 限流提示（§11 已定义错误码但无 UI 文案）、输入框最大字符数约束（email 254 / password 128 无截断反馈）、空状态与错误态的转换逻辑（清空输入是否清除服务端错误）。

#### P0 / P1 清单

| # | 问题 | 优先级 | 修订项 |
|---|------|:------:|--------|
| P0-1 | 品牌色 Token 未定义 | **P0** | §9.6 已补充 `primary=#1A73E8` 及完整 M3 ColorScheme Token 表 |
| P0-2 | 字体层级未映射 M3 Token | **P0** | §9.6 已补充 headlineMedium/bodyMedium/labelLarge 等完整层级表 |
| P0-3 | 间距未基于网格系统 | **P0** | §9.6 已定义 8dp 基准间距网格 Token（spacing0~spacing5） |
| P0-4 | 无障碍完全缺位 | **P0** | §9.2/§9.3 已补齐 contentDescription；需编码阶段补充 semantics/announceForAccessibility |
| P0-5 | 按钮启用逻辑缺失前端校验 | **P0** | LoginUiState 需新增 `isEmailValid`；`Button.enabled = isEmailValid && password.isNotEmpty() && !isLoading` |
| P0-6 | 键盘类型未指定 | **P0** | §9.3 已明确 `KeyboardType.Email`(邮箱) / `KeyboardType.Password`(密码) + `imeAction` 焦点链 |
| P1-1 | 错误展示方案内部矛盾 | **P1** | §9.5 已统一为内联 `Text` + `AnimatedVisibility`，放弃 Snackbar |
| P1-2 | 网络超时 UI 未设计 | **P1** | §9.4 已新增 Timeout 状态：「网络请求超时，请重试」+ 按钮恢复 enabled |
| P1-3 | 429 限流文案缺失 | **P1** | 需补充：「操作过于频繁，请稍后再试」 |
| P1-4 | 输入框字符上限无 UI 反馈 | **P1** | `OutlinedTextField` 配置 `maxLength` 属性，超限截断提示 |
| P1-5 | 暗色模式无显式验证 | **P1** | §9.6 已定义 Dark Token 值；编码后需截图对比双模式 |

#### UX 改进建议

| # | 建议 | 说明 |
|---|------|------|
| S-UX1 | 输入框错误态使用 M3 原生 API | `OutlinedTextField(isError=true, supportingText={Text("请输入有效的邮箱地址")})`，自带红色边框+错误文字，无需额外 Text |
| S-UX2 | 登录成功添加短暂成功态 | 按钮变绿 ✓ 图标 + "登录成功" 文字 300ms，然后导航。避免突兀跳转 |
| S-UX3 | 按钮触发触觉反馈 | `performHapticFeedback(HapticFeedbackType.LongPress)` on click，增强操作确认感 |
| S-UX4 | 错误信息朗读 | TalkBack 用户：`LiveRegion.Alert` 或 `announceForAccessibility("登录失败：邮箱或密码错误")` |
| S-UX5 | 添加入场动画 | `AnimatedVisibility(enter=fadeIn+slideInVertically)` 页面首次加载时元素依次淡入，降低等待感 |
| S-UX6 | 按钮最小宽度约束 | `Modifier.fillMaxWidth()` 之外加 `defaultMinSize(minWidth=240.dp)`，大屏横屏时按钮不过宽 |
| S-UX7 | 密码框不自动填充建议 | 若后端无真正的密码数据库，关闭 `autofill` hint 避免 Android Autofill 弹窗干扰（开发阶段） |

#### 结论

PRD 在交互闭环、状态覆盖方面的意识值得肯定，但 M3 合规性严重不足——品牌色/字体/间距均未 Token 化，无障碍完全缺位，将直接导致编码阶段视觉碎片化和可用性缺陷。§9 已在本次评审中重写为结构化规格（含精确 dp 值、色彩 Token hex、字体层级表、8dp 间距网格、M3 组件选型理由、完整状态覆盖表），可作为阶段 3 AI 出图的直接输入。P0 六项需在编码前通过 DECISIONS.md 决议确认，P1 五项可在编码阶段渐进消化。

### 12.5 QA 视角评审

#### QA 评分：**5 / 10**

#### 评审意见

**亮点：**
- §10 现有 Gherkin 用例覆盖了 7 个基础场景，结构清晰，Given/When/Then 可读性好
- 场景覆盖矩阵将每个用例与 AC 编号一一映射，追溯性良好
- 密码显隐切换(TC-05)、防重复提交(TC-06)、返回键(TC-08)等交互细节已纳入

**关键问题：**

1. **网络超时场景完全缺失。** §9.4 明确定义了 Timeout 状态（请求>10s无响应 → 显示"网络请求超时，请重试"），但 §10 中无对应 Gherkin 用例。D-56 要求所有 Repository 方法添加 `withTimeout`，但无测试验证超时后 UI 状态是否正确恢复。

2. **403/429 错误码无测试覆盖。** §11 `LoginErrorResponse` 明确定义了 `enum: [401, 403, 429]` 三种错误码，但 §10 仅覆盖 401（TC-04 使用"邮箱或密码错误"）。403（账户锁定）和 429（限流）的错误文案和按钮恢复逻辑均未验证。

3. **状态枚举过渡未独立测试。** §11.3 定义 Idle → Loading → Success/Error 状态机，但现有测试未显式验证状态流转（如 Error 态下重新输入应回到 Editing，Idle 态按钮 disabled，Loading 态输入框 disabled）。

4. **AC-03 按钮联动条件测试不充分。** TC-02/TC-03 分别测试邮箱格式错误→禁用和密码为空→禁用，但缺少「邮箱正确+密码非空→按钮启用」的正向联动验证，以及「Loading 态下按钮禁用」与 AC-03 的交叉条件。现有 `LoginViewModelTest` 的 loading 测试实际验证的是 `assertFalse(isLoading)`（请求完成后），未在请求进行中捕获 `isLoading=true`。

5. **AC-06 Loading 态测试断言错误。** 现有 TC-06「登录中防重复提交」测试与 TC-01 共用 `CircularProgressIndicator` 断言，但 `LoginViewModelTest.kt:134-147` 中 `login shows loading state during request` 的断言是 `assertFalse(state.isLoading)`——它在请求完成*后*才收集 state，从未真正验证过 `isLoading=true`。

6. **现有代码与 PRD 字段名不一致。** `LoginUiState` 使用 `username`，PRD 要求 `email`；`LoginViewModel` 暴露 `onUsernameChanged()`，PRD 要求 `onEmailChanged()`。现有测试验证的是 username 变更逻辑而非 email，PRD 重构后全部测试需改写。

| # | 问题 | 优先级 | 说明 |
|---|------|:------:|------|
| QA-P0-1 | 网络超时测试缺失 | **P0** | 新增 Gherkin 用例覆盖请求超时→错误提示→按钮恢复 |
| QA-P0-2 | 状态枚举过渡测试缺失 | **P0** | 新增 Idle→Loading→Error→Editing 状态流转验证 |
| QA-P0-3 | AC-06 Loading 中间态测试断言错误 | **P0** | 修复：在协程挂起期间收集 state，验证 `isLoading=true` |
| QA-P0-4 | AC-03 按钮联动正向验证缺失 | **P0** | 新增「邮箱正确+密码非空→按钮 enabled」用例 |
| QA-P0-5 | 字段重构（username→email）未反映到测试 | **P0** | 全部测试需同步重构为 email 字段 |
| QA-P1-1 | 403 账户锁定错误码无测试 | **P1** | 新增 403 响应→显示账户锁定文案 |
| QA-P1-2 | 429 限流错误码无测试 | **P1** | 新增 429 响应→显示限流提示+按钮禁用 N 秒 |
| QA-P1-3 | Loading 态输入框 disabled 未验证 | **P1** | 从 UI 层验证 loading 时输入框 `enabled=false` |

#### 结论

§10 现有用例骨架质量尚可（覆盖 7/8 AC），但存在两大致命缺口：**网络异常场景无覆盖**（超时/403/429）和**状态枚举过渡无显式测试**。此外 `LoginViewModelTest` 中 loading 测试实际未验证 loading=true，属于虚假通过。字段重构(username→email)将导致全部 9 个现有测试需重写。建议：P0 5 项在编码前补全 Gherkin 用例并同步更新 `LoginViewModelTest`，P1 3 项在编码阶段渐进覆盖。修订后 PRD 可达 7/10。

### 12.6 讨论决议

#### P0 致命项（4 视角共 22 项，需在确认前修订）

| 决议编号 | 决议内容 | 来源 | 修订状态 |
|----------|----------|------|:------:|
| R-01 | 覆盖 DECISIONS.md「否决邮箱+密码」决策，正式重新评估邮箱+密码为主要登录方式，产出评估对比表 | 产品+技术 | 已识别 |
| R-02 | 补充注册入口或明确账号来源（管理员预分配账号 或 最简注册页） | 产品 | 已识别 |
| R-03 | Token 持久化纳入首版（DataStore 加密存储，启动自动校验） | 产品 | 已识别 |
| R-04 | 补充网络异常场景（US-06 超时→错误提示→重试） | 产品+QA | 已识别 |
| R-05 | 补充 403/429 错误码交互和测试覆盖 | 产品+QA | 已识别 |
| R-06 | 新建 LoginApi (Retrofit) 接口，替换 AuthRepository 硬编码 mock | 技术 | 已识别 |
| R-07 | LoginResponse 数据模型重构：`token+username` → `token+user{id,email,displayName}` | 技术 | 已识别 |
| R-08 | 实现邮箱格式客户端校验（AC-01），LoginScreen 字段 username→email | 技术 | 已识别 |
| R-09 | 修复登录按钮启用条件：`!isLoading` → `isEmailValid && passwordNotEmpty && !isLoading` | 技术+UX | 已识别 |
| R-10 | 实现返回键退出应用（AC-08），NavGraph 添加 BackHandler | 技术 | 已识别 |
| R-11 | AuthRepository 添加 HttpException 捕获（D-42）+ withTimeout（D-56） | 技术 | 已识别 |
| R-12 | 定义品牌色 Token：primary=#1A73E8 + 完整 M3 ColorScheme（Light/Dark） | UX | 已识别 |
| R-13 | 字体层级映射 M3 Token（headlineMedium/bodyMedium/labelLarge 等） | UX | 已识别 |
| R-14 | 间距基于 8dp 网格系统（spacing0~spacing5 Token） | UX | 已识别 |
| R-15 | 补齐无障碍（contentDescription/semantics/announceForAccessibility） | UX | 已识别 |
| R-16 | 明确键盘类型：邮箱 KeyboardType.Email + imeAction=Next，密码 KeyboardType.Password + imeAction=Done | UX | 已识别 |
| R-17 | 补全 Gherkin 测试用例：网络超时(TC-05)、403(TC-06)、429(TC-07)、状态枚举全流转(TC-08) | QA | 已识别 |
| R-18 | 修复 AC-06 Loading 中间态测试断言（验证 isLoading=true 而非 false） | QA | 已识别 |
| R-19 | 补充 AC-03 按钮联动正向验证测试 | QA | 已识别 |
| R-20 | 字段重构 username→email 同步更新全部测试 | QA | 已识别 |
| R-21 | 统一错误展示方案为内联 `Text` + `AnimatedVisibility`（放弃 Snackbar） | UX | 已识别 |
| R-22 | 添加登录成功过渡态（✓ 图标 300ms → 导航） | UX | 已识别 |

#### P1 重要项（编码阶段消化，14 项）

| 决议编号 | 决议内容 | 来源 |
|----------|----------|------|
| R-23 | 决策冲突补充论证（用户画像/竞品对标） | 产品 |
| R-24 | UI 预留"忘记密码？"占位链接 | 产品 |
| R-25 | LoginStateManager key 从 `username` 迁移到 `user` 对象 | 技术 |
| R-26 | LoginViewModel 使用 `collectLatest`（D-55） | 技术 |
| R-27 | LoginViewModel 添加 `onCleared()` 清理（D-58） | 技术 |
| R-28 | 新增 LoginScreen Compose preview test（D-61） | 技术 |
| R-29 | 密码显隐图标添加 testTag（D-46） | 技术 |
| R-30 | AuthModule 改为 Hilt + Retrofit 注入 | 技术 |
| R-31 | `LoginUiState` 添加显式 `status: LoginStatus` 枚举字段 | 技术 |
| R-32 | Token refresh/session 管理标注为 v1.1 范围 | 技术 |
| R-33 | 网络超时 UI 设计（请求>10s→提示+按钮恢复） | UX |
| R-34 | 429 限流提示文案+按钮禁用 N 秒 | UX |
| R-35 | 输入框 `maxLength` 约束反馈 | UX |
| R-36 | 输入框 Loading 态 disabled 从 UI 层验证 | QA |

---

> **状态:** 4-Agent 并行评审完成，22 项 P0 + 14 项 P1 已识别。请审阅后回复「确认」冻结进入 UI 设计阶段。
