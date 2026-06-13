# 用户注册 — PRD

> **版本:** v1.0-confirmed
> **澄清依据:** docs/PRD_CLARIFICATION_user_registration.md（3 轮 11 问，2026-06-13）
> **创建日期:** 2026-06-13
> **作者:** Hermes 智能研发工作流
> **状态:** 已冻结（v1.0-confirmed，2026-06-13 多视角评审通过后人工确认）

---

## §1 功能概述

### 一句话描述
为未注册用户提供手机号 + 短信验证码 + 密码的注册能力，注册成功后自动登录并进入主页。

### 核心价值
- 打通新用户从"首次接触"到"进入主页"的最短路径，减少流失
- 以手机号作为唯一身份标识，为后续社交/通讯功能提供用户锚点

### 当前问题诊断
应用当前无注册入口，所有用户必须通过预设账号或第三方渠道获取访问权限，新用户无法自助 onboarding。注册功能是用户增长的第一道门。

### 涉及页面

| 页面 | 角色 |
|------|------|
| RegisterScreen | 注册表单主页面 |
| AgreementScreen | 用户协议详情（WebView） |
| LoginScreen | 已有页面，注册页提供"去登录"入口，已注册拦截后引导至此处 |
| HomeScreen | 注册成功后自动跳转目标 |

### 概念对齐表

| 业务概念 | 英文标识 | 代码实体（预期） |
|----------|----------|------------------|
| 注册 | Register | RegisterViewModel, RegisterScreen |
| 验证码 | Verification Code | SmsVerificationCode |
| 用户协议 | User Agreement | UserAgreementUrl |
| 自动登录 | Auto Login | AuthRepository.autoLogin(token) |
| 手机号 | Phone Number | PhoneNumber (value class) |

---

## §2 用户场景

| 场景编号 | 角色 | 场景描述 |
|----------|------|----------|
| US-01 | 新用户 | 通过手机号 + 验证码 + 密码完成注册，自动登录进入主页 |
| US-02 | 新用户 | 在注册过程中查看用户协议全文后返回继续注册 |
| US-03 | 已注册用户 | 输入已注册手机号并点击获取验证码，被告知已注册并被引导去登录 |
| US-04 | 新用户 | 输入不一致的两次密码，收到实时校验提示后修正 |
| US-05 | 新用户 | 网络异常情况下提交注册失败，看到 Toast 提示后留在页面重试 |

---

## §3 范围边界

### 包含
- 手机号输入 + 自动格式化（3-4-4）
- 短信验证码发送 + 60 秒倒计时 + 5 分钟有效期
- 密码设置 + 确认密码（双框，实时比对）
- 用户协议勾选框 + 可点击查看
- 手机号已注册提前拦截（发送验证码时校验）
- 注册成功自动登录 → 跳转主页
- 注册失败 Toast + 留在当前页

### 不包含
- 邮箱注册
- 第三方账号注册（微信/QQ/Apple ID）
- 注册时设置头像/昵称等额外信息
- 图形验证码
- 国际手机号
- 注册邀请码机制
- 注册后的引导页/兴趣选择页
- 注销账号功能

---

## §4 验收标准

| 编号 | 优先级 | 验收项 | 预期结果 |
|------|:------:|--------|----------|
| AC-01 | P0 | 手机号输入自动格式化 | 输入 11 位数字后显示为 `138-0000-0000` 格式 |
| AC-02 | P0 | 无效手机号格式拦截 | 非 1 开头或不足 11 位的手机号，获取验证码按钮不可点击或提示格式错误 |
| AC-03 | P0 | 验证码发送成功 | 点击获取验证码后按钮变灰并显示 60 秒倒计时 |
| AC-04 | P0 | 已注册手机号提前拦截 | 已注册手机号点击获取验证码时，提示"该手机号已注册，请直接登录"并提供"去登录"链接 |
| AC-05 | P0 | 密码规则校验 | 密码 8-20 位且包含数字和字母方可通过，不满足时给出明确提示 |
| AC-06 | P0 | 确认密码一致性校验 | 两次密码不一致时，确认密码框下方显示红色提示"两次输入的密码不一致" |
| AC-07 | P0 | 协议未勾选禁用注册按钮 | 协议未勾选时注册按钮置灰不可点击 |
| AC-08 | P0 | 注册成功自动登录跳转 | 注册接口返回成功后，自动完成登录并跳转到主页 |
| AC-09 | P1 | 网络异常/服务端错误处理 | 注册失败时 Toast 提示"注册失败，请检查网络后重试"，留在当前页，用户可重新提交 |
| AC-10 | P1 | 验证码过期处理 | 5 分钟后验证码失效，再次提交时提示验证码过期，需重新获取 |
| AC-11 | P1 | 注册按钮防重复提交 | 注册过程中按钮显示 loading 状态，禁止重复点击 |
| AC-12 | P2 | 协议链接可点击 | 点击"《用户协议》"打开协议详情页（WebView 加载远程 URL） |
| AC-13 | P2 | "已有账号？去登录"入口 | 注册页提供跳转登录页的入口 |
| AC-14 | P0 | 短信验证码发送失败提示 | 发送验证码接口返回失败时，Toast 提示"验证码发送失败，请重试"，不启动倒计时，用户可再次点击 |
| AC-15 | P0 | 倒计时归零可重新获取 | 60s 倒计时归零后，按钮文案变为"重新获取"，恢复可点击状态 |
| AC-16 | P0 | 注册成功但自动登录失败降级 | 注册成功但自动登录接口失败时，Toast 提示"注册成功，请手动登录"，跳转至登录页 |
| AC-17 | P0 | 密码可见性切换 | 两个密码框各有独立的显示/隐藏切换按钮，图标状态正确切换，contentDescription 同步更新 |
| AC-18 | P1 | 横竖屏旋转状态保留 | 旋转屏幕后手机号、密码、验证码已发送状态、倒计时进度、协议勾选状态全部保留 |
| AC-19 | P1 | 杀进程后倒计时失效 | 进程被杀后重新进入注册页，倒计时失效，按钮显示"获取验证码"可点击 |
| AC-20 | P1 | 密码含特殊字符放行 | 密码包含合法特殊字符（如 @#$%等）时不拦截，正常通过校验 |
| AC-21 | P1 | 所有交互元素无障碍支持 | 每个输入框、按钮、链接设置正确的 contentDescription，触摸目标 ≥ 48dp |

---

## §5 非功能性需求

- **性能:** 注册接口响应时间 < 3s（P95），短信验证码到达时间 < 10s（P95）
- **兼容性:** Android API 26+（minSdk 26），全面屏/折叠屏适配，深色模式支持
- **可维护性:** 表单校验逻辑独立为 Validator 类，不与 ViewModel 耦合；验证码倒计时逻辑封装为独立 Composable 或 StateFlow
- **安全:** 密码传输必须 HTTPS + 加盐哈希（bcrypt/scrypt），验证码发送频率由后端限制（同手机号每分钟最多 1 次、同 IP 每小时最多 10 次）

---

## §6 技术约束

### 架构位置

```
┌─────────────────────────────────────────────┐
│ UI Layer (Compose)                          │
│  RegisterScreen ← RegisterViewModel         │
│       ↑              ↑                      │
│       │   StateFlow<RegisterUiState>        │
│       │              │                      │
│  AgreementScreen     │                      │
└──────────────────────┼──────────────────────┘
                       │
┌──────────────────────┼──────────────────────┐
│ Domain Layer         │                      │
│  ValidatePhoneUseCase                       │
│  ValidatePasswordUseCase                    │
│  SendSmsUseCase                             │
│  RegisterUseCase                            │
└──────────────────────┼──────────────────────┘
                       │
┌──────────────────────┼──────────────────────┐
│ Data Layer           │                      │
│  AuthRepository ─────┤                      │
│  SmsApiService (Retrofit)                   │
│  UserPreferences (DataStore)                │
└─────────────────────────────────────────────┘
```

### 数据流（注册流程）

```
1. 用户输入手机号 → ViewModel.onPhoneChanged()
2. 点击"获取验证码" → ViewModel.requestSmsCode()
   → Validate phone format (client-side)
   → POST /api/auth/check-phone (check if registered)
     ├─ 200 OK → 手机号可用
     │   → POST /api/auth/send-sms (发送验证码)
     │   → 启动 60s 倒计时
     └─ 409 Conflict → 已注册
         → UiState.phoneAlreadyRegistered = true
3. 用户输入验证码 + 密码 + 确认密码 → 填完点击"注册"
4. ViewModel.register()
   → Validate password & confirm match (client-side)
   → POST /api/auth/register { phone, smsCode, password }
     ├─ 200 OK → { token, userInfo }
     │   → AuthRepository.saveToken(token)
     │   → Navigate to HomeScreen
     └─ 4xx/5xx → UiState.error = "注册失败，请检查网络后重试"
```

### 关键实现点

| 实现点 | 约束 |
|--------|------|
| 手机号格式化 | 输入时实时格式化 `###-####-####`，存储和提交时去除格式符 |
| 验证码倒计时 | 使用 StateFlow<Long> + LaunchedEffect 实现 countdown，不依赖系统时钟 |
| 密码可见性切换 | 两个密码框各带独立的"显示/隐藏"切换按钮（trailing icon） |
| 注册按钮状态 | enabled = 手机号合法 ∧ 验证码非空 ∧ 密码合法 ∧ 确认密码一致 ∧ 协议已勾选 ∧ 未在加载中 |
| 异常处理 | 区分网络超时（ConnectException）与服务端错误（非 2xx），统一 Toast 提示 |
| 状态保存 | 注册过程中页面旋转不丢失输入内容（通过 ViewModel + SavedStateHandle） |
| Token 安全存储 | AuthToken 使用 EncryptedSharedPreferences 加密持久化，禁止 DataStore 明文存储 |
| API 版本化 | 所有 API 路径使用 /api/v1/ 前缀（如 POST /api/v1/auth/send-sms），为后续兼容预留 |
| SavedStateHandle 字段 | 需持久化：phone（手机号）、password（密码）、smsSentTimestamp（发送时间戳）、agreementAccepted（协议勾选） |

### 依赖

- Retrofit + OkHttp（网络请求）
- Hilt（DI）
- Jetpack Compose + Material3
- Navigation Compose（路由）
- DataStore（本地 Token 持久化）
- kotlinx.serialization（JSON 解析）

---

## §7 风险与依赖

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 短信服务商不稳定导致验证码发送延迟/失败 | 用户无法完成注册 | 后端实现短信服务商主备切换；前端设置合理的超时时间并给出友好提示 |
| 短信接口被滥用（无防刷） | 产生资损、服务商封禁 | 后端实现 IP/设备维度频率限制（同号 1次/分钟、同 IP 10次/小时）；客户端仅 UX 辅助倒计时；后续版本考虑加入图形验证码兜底 |
| 手机号已注册校验接口在校验阶段依赖后端 | 后端不可用时校验功能失效 | 前端仅做格式校验，不自行判断是否已注册；后端返回 5xx 时按"暂时不可用"处理，放行到最终注册接口再校验 |
| 注册成功但自动登录失败 | 用户注册了但停留在登录页，体验断裂 | 注册接口返回 token 后先持久化，登录失败时回退到登录页并 Toast 提示"注册成功，请登录" |
| 倒计时期间应用退到后台或进程被杀 | 前端倒计时可能不准确 | 使用基于服务器返回的发送时间戳计算剩余秒数，客户端倒计时仅做 UI 展示，提交时后端校验验证码时效性；杀进程后倒计时失效，按钮恢复可点击 |
| 用户协议 URL 不可达 | 用户无法查看协议 | 提供本地 fallback 静态文本（打包进 assets） |
| Token 明文存储 | root 设备可读取 Token | 使用 EncryptedSharedPreferences 加密持久化 |

---

## §8 术语表

| 术语 | 代码实体（预期） | 说明 |
|------|------------------|------|
| 注册页 | RegisterScreen | Compose Composable，注册表单主页面 |
| 注册 ViewModel | RegisterViewModel | 持有 RegisterUiState，处理业务逻辑 |
| 注册 UI 状态 | RegisterUiState | 包含 phone、smsCode、password、confirmPassword、agreementAccepted 等字段及其校验状态 |
| 短信验证码 | SmsVerificationCode | 6 位数字字符串 |
| 手机号 | PhoneNumber | value class，内部存储原始 11 位数字，展示时格式化 |
| 密码 | Password | 8-20 位，含数字和字母 |
| 用户协议 | UserAgreement | 远程 URL + 本地 fallback |
| 认证仓库 | AuthRepository | 统一管理注册、登录、Token 持久化 |
| 验证码 API | SmsApiService | Retrofit interface，sendSms / checkPhone |
| 自动登录 Token | AuthToken | 注册成功后服务端返回的 Bearer Token |
| 倒计时 | smsCountdown | StateFlow<Long>，剩余秒数 |
| 去登录链接 | onNavigateToLogin | 回调函数，触发导航到 LoginScreen |

---

## §9 UI 设计输入

> **产出方:** UX Agent（PRD 评审阶段产出）
> **评审日期:** 2026-06-13

### 9.1 页面清单

| 页面 | 路径 | 用途 |
|------|------|------|
| RegisterScreen | `feature/register/RegisterScreen.kt` | 注册表单主页面 |
| AgreementScreen | `feature/register/AgreementScreen.kt` | 用户协议 WebView 页面 |
| LoginScreen | `feature/auth/LoginScreen.kt` | 导航目标-登录页 |

### 9.2 RegisterScreen 布局规格

```
┌─ CenterAlignedTopAppBar ───────────────────────┐
│  title: "注册"                                   │
│  navigationIcon: 返回箭头 (contentDescription: "返回") │
└─────────────────────────────────────────────────┘

┌─ Column (verticalScroll, 16dp padding, imePadding) ──┐
│                                                        │
│  OutlinedTextField (手机号)                             │
│    label: "手机号"                                      │
│    placeholder: "请输入手机号"                            │
│    keyboardType: Phone                                 │
│    visualTransformation: 3-4-4 格式化（光标位置不变）      │
│    singleLine: true                                    │
│    contentDescription: "手机号输入框"                    │
│    height: 56dp                                        │
│                                                        │
│  Row ────────────────────────────────────────┐         │
│  │ OutlinedTextField (验证码)  flex(1)       │         │
│  │   label: "验证码"                         │         │
│  │   placeholder: "6位数字"                  │         │
│  │   keyboardType: Number                   │         │
│  │   singleLine: true                       │         │
│  │   contentDescription: "短信验证码输入框"    │         │
│  │                                          │         │
│  │ OutlinedButton (获取验证码)  width=wrap    │         │
│  │   text: "获取验证码" / "NNs后重试"         │         │
│  │   enabled: 手机号有效 ∧ 未倒计时            │         │
│  │   contentDescription: 跟随文字动态更新      │         │
│  └──────────────────────────────────────────┘         │
│                                                        │
│  OutlinedTextField (设置密码)                           │
│    label: "设置密码"                                    │
│    placeholder: "8-20位，含字母+数字"                    │
│    keyboardType: Password                              │
│    visualTransformation: PasswordVisualTransformation   │
│    trailingIcon: 眼睛开/关 (toggle 密码可见)              │
│    trailingIcon contentDescription: "显示密码"/"隐藏密码"  │
│    singleLine: true                                    │
│    contentDescription: "密码输入框"                      │
│    height: 56dp                                        │
│                                                        │
│  OutlinedTextField (确认密码)                           │
│    label: "确认密码"                                    │
│    placeholder: "再次输入密码"                           │
│    keyboardType: Password                              │
│    visualTransformation: PasswordVisualTransformation   │
│    trailingIcon: 眼睛开/关 (独立控制)                     │
│    trailingIcon contentDescription: "显示密码"/"隐藏密码"  │
│    singleLine: true                                    │
│    contentDescription: "确认密码输入框"                   │
│    supportingText: "两次密码不一致" (不匹配时红色)          │
│    height: 56dp                                        │
│                                                        │
│  Row (协议) ──────────────────────────────────┐        │
│  │ Checkbox                                   │        │
│  │   contentDescription: "同意用户协议复选框"    │        │
│  │   minTouchTarget: 48dp                     │        │
│  │                                            │        │
│  │ ClickableText (AnnotatedString)            │        │
│  │   text: "我已阅读并同意《用户协议》"          │        │
│  │   clickableRange: "《用户协议》"             │        │
│  │   minTouchTarget: 48dp                     │        │
│  └────────────────────────────────────────────┘        │
│                                                        │
│  Button (注册)  ← 32dp margin-top                       │
│    style: FilledButton                                 │
│    text: "注册" / "注册中..." (loading)                  │
│    enabled: 所有字段有效 ∧ 协议已勾选 ∧ 未提交中           │
│    width: fillMaxWidth                                 │
│    height: 56dp                                        │
│    contentDescription: "提交注册" / "注册提交中"           │
│    loading: CircularProgressIndicator (替换文字)          │
│                                                        │
│  TextButton ("已有账号？去登录")                          │
│    onClick: navigateToLogin                            │
│    contentDescription: "已有账号，去登录"                  │
│    minTouchTarget: 48dp                                │
│                                                        │
└────────────────────────────────────────────────────────┘

遮罩层 (提交中):
  Box(fillMaxSize, backgroundColor=半透明黑色)
    CircularProgressIndicator (居中)
    contentDescription: "注册提交中，请稍候"
```

### 9.3 交互规格

| # | 触发条件 | 行为 | 反馈 |
|---|---------|------|------|
| 1 | 输入手机号 | 实时格式化为 3-4-4（如 138 1234 5678）；光标位置同步不跳变 | 文本即时变化 |
| 2 | 手机号格式完整 | 获取验证码按钮变为 enabled | 按钮从灰色→可点击 |
| 3 | 点击获取验证码 | 调用发送验证码 API；按钮变为倒计时 "NNs后重试"（60s） | 按钮 disabled + 文字倒计时 |
| 4 | 倒计时中 | 按钮不可点击 | 文字递减，到 0 恢复"重新获取" |
| 5 | 输入验证码 | 仅允许 6 位数字，支持粘贴 | 自动填入 |
| 6 | 收到 SMS | SMS Retriever API 自动填充验证码 | 验证码框自动填入 |
| 7 | 输入密码 | 实时校验：≥8位，含字母+数字 | 不符合时 supportingText 提示 |
| 8 | 输入确认密码 | 实时比对两次密码 | 不一致时红色 supportingText |
| 9 | 点击密码可见切换 | 切换 PasswordVisualTransformation ↔ 明文 | trailingIcon 眼睛开/关 |
| 10 | 勾选协议 Checkbox | 注册按钮 enabled 条件之一 | 按钮状态联动 |
| 11 | 点击《用户协议》链接 | 导航到 AgreementScreen (in-app WebView) | 新页面，注册页保留在回退栈 |
| 12 | 所有字段有效+勾选 | 注册按钮 fully enabled | 按钮可点击 |
| 13 | 点击注册按钮 | 按钮 loading；遮罩层；调用注册 API | 全屏遮罩防误触 |
| 14 | 注册成功 | Snackbar "注册成功"；自动登录→跳转主页 | Snackbar → 页面跳转 |
| 15 | 注册失败 | 视错误类型：已注册→Snackbar + "去登录" action；网络→Snackbar + "重试" action | Snackbar + action |
| 16 | 点击"去登录" | 导航到 LoginScreen | 保留回退栈 |

### 9.4 状态覆盖

| # | 状态 | 视觉表现 |
|---|------|---------|
| 1 | 默认 | 所有字段空，获取验证码按钮 disabled，注册按钮 disabled |
| 2 | 手机号输入中 | 手机号字段有焦点，实时格式化，获取验证码按钮随完整性切换 |
| 3 | 手机号已填完整 | 获取验证码按钮 enabled |
| 4 | 验证码发送中 | 获取验证码按钮 loading 状态 |
| 5 | 倒计时中 | 按钮显示 "NNs后重试"，disabled |
| 6 | 验证码已输入 | 验证码字段已填充 |
| 7 | 密码不一致 | 确认密码框 supportingText 红色错误提示 |
| 8 | 表单完整有效 | 所有字段有效+勾选，注册按钮 enabled |
| 9 | 提交中 (loading) | 注册按钮 loading；遮罩层覆盖全屏 |
| 10 | 注册成功 | Snackbar 绿色提示；自动登录→跳转主页 |
| 11 | 注册失败-已注册 | Snackbar 提示 + "去登录" action |
| 12 | 注册失败-网络 | Snackbar 提示 + "重试" action |
| 13 | 注册失败-服务端 | Snackbar 展示后端错误信息 |
| 14 | 键盘弹起 | imePadding() 自适应，表单可滚动到焦点字段 |
| 15 | 深色模式 | Material3 darkColorScheme token 全局适配 |
| 16 | 横屏/宽屏 | Column maxWidth 480dp 居中，内部 verticalScroll |

### 9.5 M3 组件选型

| 组件 | 用途 | 关键属性 |
|------|------|---------|
| CenterAlignedTopAppBar | 顶部导航 | title="注册", navigationIcon |
| OutlinedTextField | 手机号/验证码/密码/确认密码 | singleLine, keyboardType, supportingText, trailingIcon |
| FilledButton | 注册提交 | fillMaxWidth, height 56dp |
| OutlinedButton | 获取验证码 | enabled 联动, 文字动态切换 |
| Checkbox | 协议勾选 | checked 状态联动, minTouchTarget 48dp |
| ClickableText (AnnotatedString) | 协议链接 | 局部可点击, minTouchTarget 48dp |
| TextButton | "去登录" | 底部导航入口 |
| Snackbar (m3) | 成功/失败/错误反馈 | action 插槽支持重试/跳转 |
| CircularProgressIndicator | 提交 loading | 替换按钮文字，遮罩 |
| Surface/Column/Row/Box | 布局容器 | verticalScroll, padding, imePadding |

### 9.6 设计约束

| 约束 | 值 |
|------|-----|
| 页面水平 padding | 16dp |
| 字段间距 | 16dp |
| 输入框高度 | 56dp |
| 按钮距上方字段 | 32dp |
| 最小触摸目标 | 48dp (Checkbox, TextButton, 链接) |
| 横屏最大宽度 | 480dp (居中) |
| 滚动 | verticalScroll (Column 内) |
| 键盘适配 | imePadding() / WindowInsets.ime |
| 深色模式 | Material3 darkColorScheme + surface/onSurface tokens |
| 字体缩放 | 支持 1.5x 不截断（必要时滚动） |
| 验证码自动填充 | SMS Retriever API / SMS User Consent API |
| 协议页面 | in-app WebView（保留注册页 back stack） |
| 密码可见 | 每个密码框独立控制 |
| 手机号格式化 | 138 1234 5678（3-4-4，光标位置保持） |
| 倒计时 | 60s，精确到秒递减 |
| 提交防重 | 遮罩层 + 按钮 disabled |
| 内联错误 | supportingText 用于字段校验错误 |
| 全局错误 | Snackbar 用于 API 错误/网络错误 |

---

## §10 验收测试用例

> **产出方:** QA Agent（PRD 评审阶段产出）
> **评审日期:** 2026-06-13
> **格式:** Gherkin（Given/When/Then）
> **用途:** 编码阶段红绿循环

### 场景组：注册主流程

```gherkin
Scenario: TC-01 新用户成功注册
  Given 用户打开注册页面
  And   输入合法且未注册的手机号 "13812345678"
  When  点击"获取验证码"
  Then  按钮进入 60 秒倒计时，显示"60s 后重新获取"
  And   手机收到 6 位验证码
  When  输入收到的验证码 "123456"
  And   输入密码 "abc12345"
  And   再次输入相同的密码 "abc12345"
  And   勾选"我已阅读并同意《用户协议》"
  And   点击"注册"按钮
  Then  按钮显示 loading，变为"注册中..."，全屏遮罩出现
  And   注册接口返回成功
  Then  用户被自动登录
  And   跳转到主页，且无法通过返回键回到注册页
```

```gherkin
Scenario: TC-02 已注册手机号提前拦截
  Given 用户打开注册页面
  And   输入已注册的手机号 "13900001111"
  When  点击"获取验证码"
  Then  不发送验证码请求
  And   页面显示内联提示"该手机号已注册，请直接登录"
  And   显示"去登录"可点击链接
  When  点击"去登录"
  Then  跳转到登录页，并自动填入 "13900001111"
```

```gherkin
Scenario: TC-03 两次密码不一致
  Given 用户打开注册页面并已获取验证码
  And   输入密码 "abc12345"
  When  在确认密码框输入 "abc54321"
  Then  确认密码框下方显示红色提示"两次输入的密码不一致"
  And   注册按钮保持不可点击
  When  将确认密码改为 "abc12345"
  Then  红色提示消失
  And   注册按钮变为可点击（其余条件满足时）
```

```gherkin
Scenario: TC-04 注册网络异常
  Given 用户打开注册页面，所有字段合法填写完毕，协议已勾选
  When  网络断开
  And   点击"注册"按钮
  Then  按钮短暂显示 loading
  And   Snackbar 提示"注册失败，请检查网络后重试"，含"重试" action
  And   按钮恢复可点击状态
  And   所有输入内容保留
```

```gherkin
Scenario: TC-05 验证码过期后重新获取
  Given 用户打开注册页面并已获取验证码
  And   5 分钟后验证码已过期
  When  输入过期验证码并填写完所有信息，点击注册
  Then  API 返回验证码过期错误
  And   Snackbar 提示"验证码已过期，请重新获取"
  And   获取验证码按钮恢复可点击
```

```gherkin
Scenario: TC-06 协议未勾选禁用注册
  Given 用户打开注册页面，所有字段合法填写完毕
  And   协议未勾选
  Then  注册按钮置灰不可点击
  When  勾选协议
  Then  注册按钮变为可点击
```

```gherkin
Scenario: TC-07 查看用户协议
  Given 用户打开注册页面
  When  点击"《用户协议》"链接
  Then  跳转到协议详情页（in-app WebView 加载远程 URL）
  And   用户可返回注册页，表单内容保留
```

### 场景组：新增场景（评审驱动）

```gherkin
Scenario: TC-08 手机号格式校验-非1开头
  Given 用户打开注册页面
  When  输入非1开头的11位号码 "23312345678"
  Then  获取验证码按钮保持 disabled
  And   手机号输入框下方 supportingText 提示"请输入正确的手机号"
```

```gherkin
Scenario: TC-09 手机号格式校验-不足11位
  Given 用户打开注册页面
  When  输入10位号码 "1381234567"
  Then  获取验证码按钮保持 disabled
```

```gherkin
Scenario: TC-10 密码弱校验-纯数字
  Given 用户打开注册页面
  When  输入纯数字密码 "12345678"
  Then  密码框下方 supportingText 提示"密码需包含字母和数字"
  And   注册按钮保持不可点击
```

```gherkin
Scenario: TC-11 密码弱校验-不足8位
  Given 用户打开注册页面
  When  输入7位密码 "Abc1234"
  Then  密码框下方 supportingText 提示"密码长度需8-20位"
```

```gherkin
Scenario: TC-12 短信发送失败提示重试
  Given 用户在注册页输入有效手机号
  When  用户点击"获取验证码"，短信接口返回发送失败
  Then  Snackbar 提示"验证码发送失败，请重试"
  And   按钮保持"获取验证码"文案，不启动60秒倒计时
  And   用户可再次点击获取
```

```gherkin
Scenario: TC-13 倒计时结束后重新获取验证码
  Given 用户已获取验证码，倒计时显示剩余1秒
  When  60秒倒计时归零
  Then  按钮文案变为"重新获取"
  And   按钮恢复可点击状态
  When  用户再次点击"重新获取"
  Then  发送新验证码并启动新的60秒倒计时
```

```gherkin
Scenario: TC-14 注册成功但自动登录失败降级到登录页
  Given 用户填写完整注册信息并提交
  When  注册接口返回成功，自动登录接口返回失败
  Then  Snackbar 提示"注册成功，请手动登录"
  And   页面跳转至登录页（非主页）
  And   不暴露任何 token 或敏感信息
```

```gherkin
Scenario: TC-15 切换密码可见性
  Given 用户已在密码输入框输入密码
  When  用户点击密码框右侧眼睛图标
  Then  密码以明文显示
  And   眼睛图标变为关闭状态，contentDescription 变为"隐藏密码"
  When  用户再次点击眼睛图标
  Then  密码恢复密文显示
  And   确认密码框独立拥有相同的可见性切换
```

```gherkin
Scenario: TC-16 密码包含特殊字符注册成功
  Given 用户在注册页
  When  用户输入密码 "Abc@1234#"（8-20位，含大写、小写、数字、特殊字符）
  And   两次密码一致
  Then  密码校验通过，无错误提示
  And   可正常提交注册（特殊字符不被前端拦截）
```

```gherkin
Scenario: TC-17 旋转屏幕后注册状态不丢失
  Given 用户已输入手机号 "13812345678" 并获取验证码，倒计时剩余42秒
  And   已输入密码和确认密码
  When  用户旋转设备（竖屏→横屏）
  Then  手机号、密码字段内容保留
  And   验证码已发送状态保留，倒计时继续不重置
  And   勾选框选中状态保留
```

```gherkin
Scenario: TC-18 杀进程重启后倒计时失效允许重新获取
  Given 用户已获取验证码，倒计时剩余30秒
  When  用户杀进程并重新打开 App 进入注册页
  Then  倒计时状态已失效
  And   按钮显示"获取验证码"且可点击
  And   不会恢复残留的倒计时
```

### 场景覆盖矩阵

| 场景编号 | 场景标题 | 覆盖的 AC | 优先级 |
|----------|----------|:---------:|:------:|
| TC-01 | 新用户成功注册 | AC-01, AC-03, AC-07, AC-08, AC-11 | P0 |
| TC-02 | 已注册手机号提前拦截 | AC-04 | P0 |
| TC-03 | 两次密码不一致 | AC-06 | P0 |
| TC-04 | 注册网络异常 | AC-09 | P1 |
| TC-05 | 验证码过期后重新获取 | AC-10 | P1 |
| TC-06 | 协议未勾选禁用注册 | AC-07 | P0 |
| TC-07 | 查看用户协议 | AC-12, AC-13 | P2 |
| TC-08 | 手机号格式校验-非1开头 | AC-02 | P0 |
| TC-09 | 手机号格式校验-不足11位 | AC-02 | P0 |
| TC-10 | 密码弱校验-纯数字 | AC-05 | P0 |
| TC-11 | 密码弱校验-不足8位 | AC-05 | P0 |
| TC-12 | 短信发送失败 | AC-14 | P0 |
| TC-13 | 倒计时归零可重新获取 | AC-15 | P0 |
| TC-14 | 注册成功自动登录失败降级 | AC-16 | P0 |
| TC-15 | 密码可见性切换 | AC-17 | P0 |
| TC-16 | 密码含特殊字符 | AC-20 | P1 |
| TC-17 | 横竖屏旋转状态保留 | AC-18 | P1 |
| TC-18 | 杀进程后倒计时失效 | AC-19 | P1 |

> AC-21（无障碍 contentDescription）为组件级约束，由代码审查和 UI 测试覆盖，不在功能 Gherkin 中单独设场景。

---

## §11 数据契约

> **产出方:** 技术 Agent（PRD 评审阶段产出）
> **格式:** JSON Schema
> **用途:** spec_validator.py 编码前门控验证

### 11.1 API 接口定义

#### POST /api/v1/auth/send-sms

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "SendSmsRequest",
  "description": "发送短信验证码请求",
  "type": "object",
  "required": ["phone"],
  "properties": {
    "phone": {
      "type": "string",
      "pattern": "^1[0-9]{10}$",
      "description": "11 位中国大陆手机号，不含格式符"
    }
  }
}
```

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "SendSmsResponse",
  "description": "发送短信验证码响应",
  "type": "object",
  "required": ["code", "data"],
  "properties": {
    "code": { "type": "integer", "const": 200 },
    "data": {
      "type": "object",
      "required": ["expiresAt", "retryAfter"],
      "properties": {
        "expiresAt": { "type": "string", "format": "date-time", "description": "验证码过期时间" },
        "retryAfter": { "type": "integer", "description": "下次可重新发送的秒数（默认 60）" }
      }
    }
  }
}
```

#### POST /api/v1/auth/register

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "RegisterRequest",
  "description": "注册请求",
  "type": "object",
  "required": ["phone", "smsCode", "password"],
  "properties": {
    "phone": {
      "type": "string",
      "pattern": "^1[0-9]{10}$"
    },
    "smsCode": {
      "type": "string",
      "pattern": "^[0-9]{6}$",
      "description": "6 位数字验证码"
    },
    "password": {
      "type": "string",
      "minLength": 8,
      "maxLength": 20,
      "description": "8-20 位，必须包含数字和字母"
    }
  }
}
```

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "RegisterResponse",
  "description": "注册成功响应",
  "type": "object",
  "required": ["code", "data"],
  "properties": {
    "code": { "type": "integer", "const": 200 },
    "data": {
      "type": "object",
      "required": ["token", "user"],
      "properties": {
        "token": { "type": "string", "description": "Bearer Token" },
        "user": {
          "type": "object",
          "required": ["userId", "phone"],
          "properties": {
            "userId": { "type": "string" },
            "phone": { "type": "string" }
          }
        }
      }
    }
  }
}
```

#### 错误响应

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "ErrorResponse",
  "type": "object",
  "required": ["code", "message"],
  "properties": {
    "code": {
      "type": "integer",
      "enum": [400, 409, 422, 429, 500],
      "description": "400 参数错误, 409 手机号已注册, 422 验证码过期/错误, 429 请求过于频繁, 500 服务端错误"
    },
    "message": { "type": "string" }
  }
}
```

### 11.2 Kotlin 数据模型映射

```kotlin
// request
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
data class ApiResponse<T>(val code: Int, val data: T?, val message: String?)

@Serializable
data class SendSmsData(val expiresAt: String, val retryAfter: Int)

@Serializable
data class RegisterData(val token: String, val user: UserBrief)

@Serializable
data class UserBrief(val userId: String, val phone: String)
```

**重要安全约束:** AuthToken 必须使用 EncryptedSharedPreferences 加密持久化，禁止使用 DataStore 明文存储 Token。DataStore 仅用于非敏感数据（如用户偏好）。

### 11.3 客户端状态枚举

| 状态 | 值 | 说明 |
|------|-----|------|
| Idle | — | 初始状态 / 操作完成后的静止状态 |
| Sending | — | 正在发送验证码 |
| Sent | smsCountdown > 0 | 验证码已发送，倒计时中 |
| CanResend | smsCountdown == 0 | 可重新发送验证码 |
| PhoneAlreadyRegistered | — | 手机号已注册 |
| Registering | — | 正在提交注册 |
| RegisterSuccess | — | 注册成功 |
| RegisterError | errorMessage | 注册失败，附带错误信息 |

---

## §12 多视角评审记录

> **评审日期:** 2026-06-13
> **评审方式:** 4-Agent 并行评审（产品视角 / 技术视角 / UX 视角 / QA 视角）
> **评审轮次:** 第 1 轮
> **结果:** 已自动修订共识 P0 问题至 PRD 正文

### 12.1 评审总览

| 视角 | 评分 | P0 项 | P1 项 | 结论 |
|------|:----:|:-----:|:-----:|------|
| 产品视角 | 7/10 | 5 | 6 | 核心场景覆盖充分，需补异常路径 AC |
| 技术视角 | 7/10 | 4 | 5 | 架构合理，Token 安全须强化 |
| UX 视角 | 6.5/10 | 4 | 6 | 交互流程闭合，无障碍细节缺失 |
| QA 视角 | 4/10 | 9 | 4 | 原 7 场景覆盖不足，扩至 18 场景 |

### 12.2 产品视角评审（Agent A）

**P0（已修订到正文）:**
- P0-01: 缺少短信发送失败 AC → 新增 AC-14
- P0-02: 倒计时归零行为未定义 → 新增 AC-15
- P0-03: 密码特殊字符规则不清 → 新增 AC-20（允许合法特殊字符放行）
- P0-04: 4 个待确认项阻塞 → 移至 R-01~R-04 跟踪，不阻塞进入编码
- P0-05: 注册成功但自动登录失败降级无 AC → 新增 AC-16

**P1:**
- P1-01: 密码不一致校验触发时机（建议确认密码框失焦时触发）
- P1-02: AC-12 协议链接优先级偏低（保持 P2，法律合规由应用商店审核负责）
- P1-03: SavedStateHandle 旋转恢复无 AC → 新增 AC-18
- P1-04: 杀进程恢复无 AC → 新增 AC-19
- P1-05: 非功能性 AC 缺失 → 性能目标在 §5 量化，编码另设 benchmark
- P1-06: 已注册拦截后缺少直达登录快捷操作 → 已包含"去登录"链接

**产品风险矩阵:** 7 项风险（短信服务商/校验依赖/自动登录失败/倒计时/协议 URL/密码规则/折叠屏），均有缓解措施。

### 12.3 技术视角评审（Agent B）

**P0（已修订到正文）:**
- P0-01: Token 明文存储 → §6 新增 EncryptedSharedPreferences，§11 追加安全约束
- P0-02: 短信接口无防刷 → §7 新增防刷风险及缓解（后端 IP/设备限频）
- P0-03: SavedStateHandle 字段未定义 → §6 列举 4 个字段（phone/password/smsSentTimestamp/agreementAccepted）
- P0-04: 手机号硬编码 11 位不兼容国际 → §3 明确仅中国大陆

**P1:**
- P1-01: RegisterError 粒度不足 → 建议 UiState 拆分 NetworkError/ServerError/ValidationError
- P1-02: 密码无客户端校验 → §4 AC-05/AC-10/AC-11 已覆盖
- P1-03: 倒计时持久化缺失 → §6 基于服务器时间戳方案已描述
- P1-04: 验证码自动填充（SMS Retriever API）→ §9.6 已纳入设计约束
- P1-05: API 无版本号 → §11 全部 API 路径改为 /api/v1/

**技术风险表:** 6 项（Token 明文/短信滥用/状态竞态/倒计时不同步/Process Death/Retrofit 拦截器）

### 12.4 UX 视角评审（Agent C）

**P0（已修订到正文）:**
- P0-01: 无障碍 contentDescription 全缺 → 新增 AC-21，§9 ASCII 布局中每个组件标注 contentDescription
- P0-02: 键盘类型未指定 → §9 布局规格显式标注 KeyboardType.Phone/Number/Password
- P0-03: 密码可见性切换规格缺失 → 新增 AC-17，§9 详细描述双框独立控制、图标切换、contentDescription
- P0-04: 键盘弹起策略未定义 → §9.6 设计约束加入 imePadding()/WindowInsets.ime

**P1:**
- P1-01: 手机号格式化光标跳变 → §9.3 交互 #1 明确"光标位置同步不跳变"
- P1-02: 倒计时按钮防重复 → §9 倒计时按钮 disabled + 文字动态切换
- P1-03: SMS 自动填充 → §9.3 #6 纳入 SMS Retriever API
- P1-04: 协议链接跳转方式 → §9.6 明确 in-app WebView 保留回退栈
- P1-05: 错误反馈策略混用 → §9.6 区分内联(supportingText)/全局(Snackbar)
- P1-06: Loading 状态视觉 → §9 遮罩层 + 按钮 loading

**§9 已重写:** ASCII 线框图布局 + 16 交互规格 + 16 状态覆盖 + 10 组件选型 + 18 设计约束

### 12.5 QA 视角评审（Agent D）

**覆盖提升:**
- 原 7 场景 → 扩至 18 场景
- AC 覆盖: 11/13 → 21/21（AC-21 由代码审查覆盖）
- 三方 P0 覆盖: 1/9 → 6/9（3 个非功能 P0 不适用 Gherkin）

**新增场景:**
| 场景 | 覆盖 |
|------|------|
| TC-08/09 | 手机号格式校验（AC-02） |
| TC-10/11 | 密码强度校验（AC-05） |
| TC-12 | 短信发送失败（AC-14） |
| TC-13 | 倒计时归零可重新获取（AC-15） |
| TC-14 | 自动登录失败降级（AC-16） |
| TC-15 | 密码可见性切换（AC-17） |
| TC-16 | 密码含特殊字符（AC-20） |
| TC-17 | 横竖屏旋转状态保留（AC-18） |
| TC-18 | 杀进程倒计时失效（AC-19） |

### 12.6 讨论决议

| 决议编号 | 决议内容 | 来源 | 状态 |
|----------|----------|------|------|
| R-01 | 验证码发送方式：后端统一短信发送 | 假设，待确认 | 待确认 |
| R-02 | 协议链接：in-app WebView 加载远程 URL，含本地 fallback | 假设，待确认 | 待确认 |
| R-03 | "去登录"自动填入手机号：是，从已注册拦截跳转时自动填入 | 假设，待确认 | 待确认 |
| R-04 | 注册页"已有账号？去登录"入口：底部 TextButton | 假设，待确认 | 待确认 |
| D-01 | Token 存储方案：EncryptedSharedPreferences 加密，禁止 DataStore 明文 | 技术 Agent P0 | 已确认 |
| D-02 | API 版本化：所有路径使用 /api/v1/ 前缀 | 技术 Agent P1 | 已确认 |
| D-03 | 密码特殊字符策略：前端不拦截合法特殊字符（@#$%等），仅校验长度+字母+数字组合 | 产品 P0 + QA TC-16 | 已确认 |
| D-04 | 错误反馈策略：字段级错误用 supportingText，全局错误用 Snackbar + action | UX P1 | 已确认 |
| D-05 | 提交防重策略：遮罩层 + 按钮 disabled + 文字变为"注册中..." | UX P1 | 已确认 |
| D-06 | 倒计时持久化：基于服务器返回 timestamp 计算，杀进程后失效恢复可点击 | 产品 P1 + QA TC-18 | 已确认 |
| D-07 | SavedStateHandle 字段：phone, password, smsSentTimestamp, agreementAccepted | 技术 P0 | 已确认 |
| D-08 | 目标市场：中国大陆手机号（11 位 1 开头），不含国际号码 | 技术 P0 + §3 | 已确认 |
| D-09 | 无障碍 baseline：所有交互元素设置 contentDescription，触摸目标 ≥ 48dp | UX P0 + AC-21 | 已确认 |

---

> **状态:** 初稿完成，4 项假设（R-01 ~ R-04）待确认。待触发多视角评审后冻结版本号。
