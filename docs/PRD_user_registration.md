# 用户注册 — PRD

> **版本:** v0.1-draft
> **澄清依据:** docs/PRD_CLARIFICATION_user_registration.md（3 轮 11 问，2026-06-13）
> **创建日期:** 2026-06-13
> **作者:** Hermes 智能研发工作流
> **状态:** 待评审冻结

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
| 手机号已注册校验接口在校验阶段依赖后端 | 后端不可用时校验功能失效 | 前端仅做格式校验，不自行判断是否已注册；后端返回 5xx 时按"暂时不可用"处理，放行到最终注册接口再校验 |
| 注册成功但自动登录失败 | 用户注册了但停留在登录页，体验断裂 | 注册接口返回 token 后先持久化，登录失败时回退到登录页并 Toast 提示"注册成功，请登录" |
| 倒计时期间应用退到后台或进程被杀 | 前端倒计时可能不准确 | 使用基于服务器返回的发送时间戳计算剩余秒数，客户端倒计时仅做 UI 展示，提交时后端校验验证码时效性 |
| 用户协议 URL 不可达 | 用户无法查看协议 | 提供本地 fallback 静态文本（打包进 assets） |

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

### 页面清单

| 页面 | 路由 | 用途 |
|------|------|------|
| RegisterScreen | `register` | 注册表单主页面 |
| AgreementScreen | `register/agreement` | WebView 加载用户协议 |
| LoginScreen | `login` | 已有页面，作为导航目标 |

### 布局规格

| 属性 | 值 |
|------|-----|
| 页面标题 | "注册"（居中，TopAppBar） |
| 手机号输入框 | 单行，placeholder "请输入手机号"，inputType phone，自动格式化 3-4-4 |
| 验证码输入框 | 单行，placeholder "请输入验证码"，inputType number，右侧"获取验证码"按钮 |
| 密码输入框 | 单行，placeholder "请输入密码"，inputType textPassword，trailing icon 显示/隐藏 |
| 确认密码输入框 | 单行，placeholder "请再次输入密码"，inputType textPassword，trailing icon 显示/隐藏 |
| 协议行 | Row：Checkbox + "我已阅读并同意《用户协议》"（链接颜色 accent） |
| 注册按钮 | 全宽 filled button，文字"注册"，disabled 时置灰 |
| 底部导航 | "已有账号？去登录"（TextButton） |

### 交互规格

| 交互 | 触发条件 | 行为 |
|------|----------|------|
| 手机号自动格式化 | 用户输入数字 | 实时格式化为 `xxx-xxxx-xxxx` |
| 获取验证码按钮 | 手机号格式合法 + 未注册 | 发送请求，按钮变灰 + 60s 倒计时 |
| 获取验证码按钮 | 手机号已注册 | Toast/内联提示，不发送请求 |
| 密码实时校验 | 输入框失焦或提交时 | 检查长度 + 字符组合，密码框下方显示错误文字 |
| 确认密码实时校验 | 确认密码框内容变化 | 实时与密码框比对，不一致时下方显示红色提示 |
| 协议勾选 | 点击 Checkbox | 切换勾选状态，影响注册按钮 enabled |
| 协议链接点击 | 点击"《用户协议》" | 导航到 AgreementScreen |
| 注册按钮点击 | 所有字段合法 + 协议已勾选 | 按钮变 loading，提交 API |
| 注册成功 | API 返回 200 | 自动登录 → 导航到 HomeScreen，清除返回栈 |
| 注册失败 | API 返回 4xx/5xx | Toast"注册失败，请检查网络后重试"，恢复按钮状态 |
| 去登录 | 点击底部链接 | 导航到 LoginScreen |
| 已注册拦截 | 获取验证码返回 409 | 显示内联提示 + "去登录"链接 |

### 状态覆盖

| 状态 | 页面表现 |
|------|----------|
| 默认 | 所有输入框为空，注册按钮置灰，获取验证码按钮可用（手机号为空时置灰） |
| 输入中 | 各字段实时校验反馈 |
| 验证码发送成功 | 获取验证码按钮变灰 + 60s 倒计时（如 "59s 后重新获取"） |
| 验证码倒计时归零 | 按钮恢复"重新获取"文案和可点击状态 |
| 手机号已注册 | 获取验证码按钮下方出现内联提示，输入框不动 |
| 密码不一致 | 确认密码框下方红色文字"两次输入的密码不一致" |
| 密码不足 8 位 | 密码框下方提示"密码需要至少 8 位" |
| 密码不含字母 | 密码框下方提示"密码需包含字母" |
| 密码不含数字 | 密码框下方提示"密码需包含数字" |
| 注册提交中 | 注册按钮显示 loading spinner，文字变"注册中..."，不可点击 |
| 注册失败 | Toast 弹出，按钮恢复可点击，输入内容保留 |
| 键盘弹起 | 页面内容可滚动，注册按钮不被键盘遮挡，输入框保持可见 |
| 深色模式 | 所有颜色使用 M3 主题色 token，自动适配 |
| 横屏/平板 | 内容居中，最大宽度 480dp |

### Material3 组件选型

| 组件 | 选型 | 原因 |
|------|------|------|
| 输入框 | OutlinedTextField | 清晰边界，符合 Material3 规范 |
| 注册按钮 | Button (filled) | 主要 CTA，全宽 |
| 协议勾选 | Checkbox + ClickableText | 标准二元选择 + 内联链接 |
| 倒计时按钮 | OutlinedButton (disabled 态) | 非主操作，置灰表达不可用 |
| 错误提示 | Supporting text (error colored) | 内联提示，不打断用户 |
| Toast | Snackbar (preferred) / Toast | 短暂反馈，不打断 |
| 顶部栏 | CenterAlignedTopAppBar | M3 规范 |
| 加载指示 | CircularProgressIndicator (button 内) | 表达处理中 |

### 设计约束

- 表单垂直间距 16dp，section 间距 24dp
- 输入框高度 56dp，全宽
- 协议行水平排列，Checkbox 24dp
- 注册按钮距最后一个输入框 32dp
- 页面内容超出时整体可滚动（verticalScroll）
- 手机号输入框仅接受数字，maxLength = 11（存储），显示为 13 字符（含两个 `-`）
- 深色模式：所有颜色使用 M3 colorScheme token，禁止硬编码

---

## §10 验收测试用例

> **产出方:** QA Agent（PRD 评审阶段并行产出）
> **格式:** Gherkin（Given/When/Then）
> **用途:** 编码阶段红绿循环

### 场景组：注册主流程

```gherkin
Scenario: 新用户成功注册
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
  Then  按钮显示 loading，变为"注册中..."
  And   注册接口返回成功
  Then  用户被自动登录
  And   跳转到主页，且无法通过返回键回到注册页
```

```gherkin
Scenario: 已注册手机号提前拦截
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
Scenario: 两次密码不一致
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
Scenario: 注册网络异常
  Given 用户打开注册页面，所有字段合法填写完毕，协议已勾选
  When  网络断开
  And   点击"注册"按钮
  Then  按钮短暂显示 loading
  And   Toast 提示"注册失败，请检查网络后重试"
  And   按钮恢复可点击状态
  And   所有输入内容保留
```

```gherkin
Scenario: 验证码过期后重新获取
  Given 用户打开注册页面并已获取验证码
  And   5 分钟后验证码已过期
  When  输入过期验证码并填写完所有信息，点击注册
  Then  API 返回验证码过期错误
  And   Toast 提示"验证码已过期，请重新获取"
  And   获取验证码按钮恢复可点击
```

```gherkin
Scenario: 协议未勾选禁用注册
  Given 用户打开注册页面，所有字段合法填写完毕
  And   协议未勾选
  Then  注册按钮置灰不可点击
  When  勾选协议
  Then  注册按钮变为可点击
```

```gherkin
Scenario: 查看用户协议
  Given 用户打开注册页面
  When  点击"《用户协议》"链接
  Then  跳转到协议详情页（WebView 加载远程 URL）
  And   用户可返回注册页，表单内容保留
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

---

## §11 数据契约

> **产出方:** 技术 Agent（PRD 评审阶段产出）
> **格式:** JSON Schema
> **用途:** spec_validator.py 编码前门控验证

### 11.1 API 接口定义

#### POST /api/auth/send-sms

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

#### POST /api/auth/register

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

> 评审日期: 待执行
> 评审方式: 4-Agent 并行评审（产品视角 / 技术视角 / UX 视角 / QA 视角）

### 12.1 评审总览

| 视角 | 评分 | P0 项 | P1 项 | 结论 |
|------|------|-------|-------|------|
| 产品视角 | — | — | — | 待评审 |
| 技术视角 | — | — | — | 待评审 |
| UX 视角 | — | — | — | 待评审 |
| QA 视角 | — | — | — | 待评审 |

### 12.2–12.5 待评审产出

### 12.6 讨论决议

| 决议编号 | 决议内容 | 来源 | 状态 |
|----------|----------|------|------|
| R-01 | 验证码发送方式：后端统一发送 | 假设，待确认 | 待确认 |
| R-02 | 协议链接：WebView 加载远程 URL | 假设，待确认 | 待确认 |
| R-03 | "去登录"自动填入手机号：是 | 假设，待确认 | 待确认 |
| R-04 | 注册页"已有账号？去登录"入口：有 | 假设，待确认 | 待确认 |

---

> **状态:** 初稿完成，4 项假设（R-01 ~ R-04）待确认。待触发多视角评审后冻结版本号。
