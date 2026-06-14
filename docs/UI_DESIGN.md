# 用户注册 — UI 设计方案

> **版本:** v0.1-draft
> **创建日期:** 2026-06-14
> **来源:** PRD §9 UI 设计输入（v1.2-confirmed）
> **生成方式:** 快速通道（手动构建）
> **页面数量:** 2 个核心页面（RegisterScreen, AgreementScreen）
> **设计语言:** Material 3 (M3)

---

## 1. 页面清单

| 页面 | 文件名 | 用途 | 状态 |
|------|--------|------|:----:|
| RegisterScreen | `RegisterScreen.kt` | 注册表单主页面：手机号+验证码+密码+协议 | ✅ 新增 |
| AgreementScreen | `AgreementScreen.kt` | 用户协议 WebView 页面 | ✅ 新增 |
| LoginScreen | `LoginScreen.kt` | 已有页面，注册完成后导航目标 | 🔗 复用 |

---

## 2. RegisterScreen 线框图

```
┌─── StatusBar (24dp) ───────────────────────────┐
│                                                  │
│  ┌─ CenterAlignedTopAppBar ───────────────────┐ │
│  │  ← 返回         注册                        │ │
│  └─────────────────────────────────────────────┘ │
│                                                  │
│  ┌─ Column (16dp pad, verticalScroll) ─────────┐ │
│  │                                              │ │
│  │  ┌─ OutlinedTextField ────────────────────┐ │ │
│  │  │  手机号                                  │ │ │
│  │  │  138 1234 5678                          │ │ │
│  │  └────────────────────────────────────────┘ │ │
│  │                                              │ │
│  │  ┌─ Row ──────────────────────────────────┐ │ │
│  │  │ ┌─ OutlinedTextField ────┐             │ │ │
│  │  │ │  验证码                  │  [获取验证码] │ │ │
│  │  │ └────────────────────────┘  (Outlined)  │ │ │
│  │  └─────────────────────────────────────────┘ │ │
│  │                                              │ │
│  │  ┌─ OutlinedTextField ────────────────────┐ │ │
│  │  │  设置密码                         👁   │ │ │
│  │  │  ●●●●●●●●                             │ │ │
│  │  └────────────────────────────────────────┘ │ │
│  │                                              │ │
│  │  ┌─ OutlinedTextField ────────────────────┐ │ │
│  │  │  确认密码                         👁   │ │ │
│  │  │  ●●●●●●●●                             │ │ │
│  │  │  ⚠ 两次密码不一致                      │ │ │
│  │  └────────────────────────────────────────┘ │ │
│  │                                              │ │
│  │  ┌─ Row (协议) ───────────────────────────┐ │ │
│  │  │  [✓] 我已阅读并同意《用户协议》           │ │ │
│  │  └────────────────────────────────────────┘ │ │
│  │                                              │ │
│  │  ┌─ FilledButton ────────────────────────┐ │ │
│  │  │              注  册                     │ │ │
│  │  └────────────────────────────────────────┘ │ │
│  │                                              │ │
│  │         已有账号？去登录                      │ │
│  │                                              │ │
│  └──────────────────────────────────────────────┘ │
│                                                    │
│  遮罩层 (Loading 态):                               │
│  ┌─ Box(fillMaxSize, bg=半透明黑色60%) ──────────┐ │
│  │                                                │ │
│  │              ⟳ 注册提交中...                    │ │
│  │              (返回键可取消)                     │ │
│  │                                                │ │
│  └────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────┘
```

## 3. 组件层级树

```
Scaffold(topBar, snackbarHost)
├── CenterAlignedTopAppBar
│   └── navigationIcon: IconButton("返回")
│       └── title: Text("注册")
│
├── Box(fillMaxSize)  // 主内容 + 遮罩层
│   ├── Column(verticalScroll, padding=16dp, imePadding)
│   │   │
│   │   ├── [1] OutlinedTextField("手机号")
│   │   │       keyboardType=Phone, singleLine
│   │   │       visualTransformation=PhoneFormat(3-4-4)
│   │   │       supportingText=phoneError
│   │   │
│   │   ├── [2] Row(verificationCode + sendSmsButton)
│   │   │   ├── OutlinedTextField("验证码")
│   │   │   │       keyboardType=Number, singleLine
│   │   │   │       modifier=weight(1f)
│   │   │   │
│   │   │   └── OutlinedButton("获取验证码")
│   │   │           enabled=phoneValid && !countingDown
│   │   │           text=countdownText  // 动态切换
│   │   │
│   │   ├── [3] OutlinedTextField("设置密码")
│   │   │       keyboardType=Password
│   │   │       visualTransformation=PasswordVT
│   │   │       trailingIcon=togglePasswordVisibility
│   │   │       supportingText=passwordError
│   │   │
│   │   ├── [4] OutlinedTextField("确认密码")
│   │   │       keyboardType=Password
│   │   │       visualTransformation=PasswordVT
│   │   │       trailingIcon=togglePasswordVisibility
│   │   │       supportingText=confirmError
│   │   │
│   │   ├── [5] Row(协议)
│   │   │   ├── Checkbox(checked, onCheck)
│   │   │   └── ClickableText("我已阅读并同意《用户协议》")
│   │   │           annotatedString(clickable "《用户协议》")
│   │   │
│   │   ├── [6] FilledButton("注册")
│   │   │       enabled=formValid && !submitting
│   │   │       loading=submitting (显示 "注册中...")
│   │   │       width=fillMaxWidth, height=56dp
│   │   │
│   │   └── [7] TextButton("已有账号？去登录")
│   │           onClick=navigateToLogin
│   │
│   └── [Overlay] AnimatedVisibility(submitting)
│       └── Box(fillMaxSize, bg=Black60%)
│           └── Column(center)
│               ├── CircularProgressIndicator
│               └── Text("注册提交中，请稍候")
│
└── SnackbarHost(hostState)
```

## 4. 交互状态机

### RegisterScreen 全态转移图

```
                    ┌─────────────────────────────────┐
                    │          Idle (默认态)            │
                    │ 所有字段空，按钮 disabled          │
                    └──────┬────────────────┬─────────┘
           phone输入       │                │
                    ┌──────▼──────┐         │
                    │ Inputting   │         │ 点击"去登录"
                    │ 字段有焦点   │         │
                    └──┬───┬───┬──┘         ▼
         phone合法     │   │   │       LoginScreen
                    ┌──▼┐  │   │
                    │Sms │  │   │
                    │Btn │  │   │
                    │En  │  │   │
                    └──┬┘  │   │
              点击获取  │   │   │
                    ┌──▼──────────┐
                    │ SendingSms   │
                    │ 按钮 loading │
                    └──┬───────┬──┘
             成功      │       │ 失败
                    ┌──▼──┐  ┌─▼──────────┐
                    │Count│  │ SmsError    │
                    │down │  │ Snackbar    │
                    │ 60s │  │ 可重试      │
                    └──┬──┘  └─────────────┘
           填入验证码  │
                    ┌──▼──────────┐
                    │ FormFilling │
                    │ 密码/确认密码 │
                    └──┬──────┬───┘
          表单完整     │      │ 字段校验失败
                    ┌──▼──┐  ┌─▼──────────┐
                    │Sub- │  │ Validation │
                    │mit  │  │ Error提示   │
                    │BtnEn│  │ supporting │
                    └──┬──┘  │ Text       │
              点击注册 │      └─────────────┘
                    ┌──▼───────────┐
                    │ Submitting   │
                    │ 遮罩+loading │
                    └──┬───┬───┬──┘
             成功     │   │   │ 失败/超时
                    ┌▼┐  │   └─────────┐
                    │S │  │ 取消        │
                    │u │  │ (返回键)    │
                    │c │  ┌▼──────────┐│
                    │c │  │Idle(Keep  ││
                    │e │  │FormData)  ││
                    │s │  └───────────┘│
                    │s │               │
                    └┬─┘      ┌────────▼──────────┐
                     │        │ Error              │
               自动登录│        │ Snackbar+表单保留  │
                  ┌──▼──┐    │ 已注册→去登录       │
                  │Home │    │ 网络→重试           │
                  │主页  │    │ 超时→重试           │
                  └─────┘    │ 服务端→错误信息     │
                             └─────────────────────┘
```

## 5. 状态覆盖表

| # | 状态名称 | 手机号 | 验证码 | 密码 | 确认密码 | 协议 | 注册按钮 | 遮罩层 | Snackbar |
|---|---------|:------:|:------:|:----:|:--------:|:----:|:--------:|:------:|:--------:|
| 1 | Idle(默认) | 空 | 空 | 空 | 空 | ☐ | disabled | - | - |
| 2 | PhoneFilling | 输入中 | 空 | 空 | 空 | ☐ | disabled | - | - |
| 3 | PhoneValid | 合法 | 空 | 空 | 空 | ☐ | disabled | - | - |
| 4 | SendingSms | 合法 | 空 | 空 | 空 | ☐ | disabled | - | - |
| 5 | CountingDown | 合法 | 空/有值 | * | * | * | 联动 | - | - |
| 6 | SmsFilled | 合法 | 已填 | * | * | * | 联动 | - | - |
| 7 | PasswordMismatch | 合法 | 已填 | 已填 | 不匹配 | * | disabled | - | - |
| 8 | FormValid | 合法 | 已填 | 合法 | 匹配 | ✓ | enabled | - | - |
| 9 | Submitting | 合法 | 已填 | 已填 | 已填 | ✓ | loading | ✓ 60%黑 | - |
| 10 | Success | - | - | - | - | - | - | - | "注册成功" 绿 |
| 11 | Error-Registered | 合法 | 已填 | 已填 | 已填 | ✓ | enabled | - | "已注册→去登录" |
| 12 | Error-Network | 合法 | 已填 | 已填 | 已填 | ✓ | enabled | - | "网络错误→重试" |
| 13 | Error-Server | 合法 | 已填 | 已填 | 已填 | ✓ | enabled | - | 后端错误信息 |
| 14 | KeyboardUp | * | * | * | * | * | * | - | - |
| 15 | DarkMode | - | - | - | - | - | - | - | - |
| 16 | Landscape | - | - | - | - | - | - | - | - |

> * = 状态保持当前值，-- = 不适用

**额外边缘状态：**
- **SmsError**: 获取验证码失败，Snackbar 提示"验证码发送失败，请重试"，不启动倒计时
- **CodeExpired**: 5分钟过期，提交时 Snackbar"验证码已过期，请重新获取"
- **CodeWrong3x**: 连续3次错误，Snackbar"验证码错误次数过多，请重新获取"
- **Timeout**: 注册请求15s超时，Snackbar"网络超时，请重试"，表单保留
- **SmsRetrieverAuto**: SMS Retriever API 自动填入验证码
- **SmsManualInput**: 无GMS设备，手动输入验证码（支持粘贴）

## 6. Token 映射表

由于项目无现有 Theme 文件，采用标准 M3 默认色板：

| 设计Token | Light值 | Dark值 | 用途 | 来源 |
|-----------|---------|--------|------|------|
| `primary` | `#6750A4` | `#D0BCFF` | 主色按钮背景/Activated | M3 Default |
| `onPrimary` | `#FFFFFF` | `#381E72` | 主色按钮文字 | M3 Default |
| `primaryContainer` | `#EADDFF` | `#4F378B` | 浅主色容器 | M3 Default |
| `onPrimaryContainer` | `#21005D` | `#EADDFF` | 浅主色文字 | M3 Default |
| `secondary` | `#625B71` | `#CCC2DC` | 辅助色 | M3 Default |
| `surface` | `#FEF7FF` | `#1C1B1F` | 页面背景 | M3 Default |
| `surfaceVariant` | `#E7E0EC` | `#49454F` | 组件容器 | M3 Default |
| `onSurface` | `#1C1B1E` | `#E6E1E5` | 主文字 | M3 Default |
| `onSurfaceVariant` | `#49454F` | `#CAC4D0` | 辅助文字/label | M3 Default |
| `outline` | `#79747E` | `#938F99` | 边框/分割线 | M3 Default |
| `outlineVariant` | `#CAC4D0` | `#49454F` | 输入框 outline | M3 Default |
| `error` | `#BA1A1A` | `#FFB4AB` | 错误色 | M3 Default |
| `onError` | `#FFFFFF` | `#690005` | 错误文字 | M3 Default |
| `errorContainer` | `#FFDAD6` | `#93000A` | 错误容器 | M3 Default |
| `onErrorContainer` | `#410002` | `#FFDAD6` | 错误容器文字 | M3 Default |
| `background` | `#FEF7FF` | `#1C1B1F` | 根背景 | M3 Default |

**间距Token：**

| Token | 值 | 用途 |
|-------|-----|------|
| `spacing-horizontal` | 16dp | 页面水平 padding |
| `spacing-field` | 16dp | 字段间距 |
| `spacing-btn-top` | 32dp | 注册按钮距上方字段 |
| `height-input` | 56dp | 输入框高度 |
| `height-btn` | 56dp | 按钮高度 |
| `touch-target` | ≥48dp | 最小触摸目标 |
| `max-width-landscape` | 480dp | 横屏最大宽度 |

**对比度检查清单：**

| 元素 | 前景色 | 背景色 | Light WCAG | Dark WCAG |
|------|--------|--------|:----------:|:---------:|
| 正文 | `onSurface` | `surface` | ~13:1 ✅ | ~13:1 ✅ |
| 辅助文字 | `onSurfaceVariant` | `surface` | ~7:1 ✅ | ~7:1 ✅ |
| 主按钮文字 | `onPrimary` | `primary` | 5.26:1 ✅ | 5.96:1 ✅ |
| 错误文字 | `error` | `surface` | 5.55:1 ✅ | 5.69:1 ✅ |
| disabled 按钮 | 38% opacity | - | N/A (disabled) | N/A |
| 遮罩文字 | `#FFFFFF` | 60%黑 | ≥7:1 ✅ | ≥7:1 ✅ |

## 7. 组件复用分析 + 新增组件清单

### 复用分析

| 组件 | 复用状态 | 路径/来源 | 说明 |
|------|:--------:|-----------|------|
| OutlinedTextField | 🔗 M3 组件 | `androidx.compose.material3` | 标准组件，直接使用 |
| FilledButton | 🔗 M3 组件 | `androidx.compose.material3` | 标准组件，直接使用 |
| OutlinedButton | 🔗 M3 组件 | `androidx.compose.material3` | 标准组件，直接使用 |
| TextButton | 🔗 M3 组件 | `androidx.compose.material3` | 标准组件，直接使用 |
| Checkbox | 🔗 M3 组件 | `androidx.compose.material3` | 标准组件，直接使用 |
| TopAppBar | 🔗 M3 组件 | `androidx.compose.material3` | 标准组件，直接使用 |
| CircularProgressIndicator | 🔗 M3 组件 | `androidx.compose.material3` | 标准组件，直接使用 |
| Snackbar | 🔗 M3 组件 | `androidx.compose.material3` | 标准组件，直接使用 |

### 新增组件清单

| 组件 | 复杂度 | 预估工时 | 描述 |
|------|:------:|:--------:|------|
| `PhoneNumberField` | 低 | 0.5h | 封装手机号 OutlinedTextField + 3-4-4 格式化 + 光标保持 |
| `SmsCodeRow` | 中 | 1h | Row(验证码输入框 + 获取验证码按钮) 带倒计时状态联动 |
| `PasswordField` | 低 | 0.5h | 封装密码 OutlinedTextField + 可见性切换 trailingIcon + 校验 |
| `AgreementRow` | 低 | 0.5h | Row(Checkbox + ClickableText) 协议勾选+链接 |
| `SubmitOverlay` | 中 | 1h | 提交遮罩层：AnimatedVisibility + 半透明背景 + 返回键取消 |
| `CountdownState` | 中 | 1.5h | 倒计时逻辑：ViewModel 协程 + StateFlow<Long> + 基于服务器 timestamp |
| `PhoneVisualTransformation` | 中 | 1h | 3-4-4 格式化的 VisualTransformation，保持光标位置 |
| `RegisterScreen` | 高 | 4h | 注册页面 Composable，组装以上组件 |
| `AgreementScreen` | 低 | 1h | WebView 加载用户协议 + 返回按钮 |
| **总计** | | **11h** | |

## 8. AgreementScreen 设计

### 线框图

```
┌─ CenterAlignedTopAppBar ────────────┐
│  ← 返回         用户协议              │
└─────────────────────────────────────┘

┌─ WebView ──────────────────────────┐
│                                      │
│  (加载远程URL: /api/v1/agreement)     │
│  + 本地 fallback: assets/agreement  │
│                                      │
│                                      │
│                                      │
│                                      │
└──────────────────────────────────────┘
```

### 交互规格

| # | 触发条件 | 行为 |
|---|---------|------|
| 1 | 页面加载 | WebView 加载远程 URL |
| 2 | URL 不可达 | 自动降级到本地 assets/agreement.html |
| 3 | 点击返回 | 返回 RegisterScreen，表单数据保留 |
| 4 | 加载中 | WebView 显示 ProgressIndicator |
| 5 | 加载失败 | Snackbar"协议加载失败，请重试" + 重试按钮 |

### 组件

- `WebView` (AndroidView包裹)
- `CenterAlignedTopAppBar`
- `CircularProgressIndicator` (加载态)

## 9. 架构协调设计

### 跨 Screen 通信

```
RegisterScreen                 AgreementScreen
     │                               │
     │ navigate("agreement")         │
     ├──────────────────────────────>│
     │                               │ onBack:
     │<──────────────────────────────┤
     │ (RegisterViewModel 存活)      │
     │ 表单数据保留在 ViewModel       │
     │                               │
```

- **导航方式**: Navigation Compose `NavController.navigate()`
- **回退策略**: AgreementScreen popBackStack 回 RegisterScreen
- **数据保留**: RegisterViewModel 绑定 NavBackStackEntry 生命周期，返回时不被销毁

### 去登录跳转（自动填入手机号）

```kotlin
// 已注册拦截 → 跳转登录页 + 自动填入手机号
navController.navigate("login?phone=${phone.value}") {
    popUpTo("register") { inclusive = true }
}
```

### 注册成功清空回退栈

```kotlin
navController.navigate("home") {
    popUpTo(0) { inclusive = true }  // 清除全部回退栈
}
```

## 10. 设计决策

| # | 决策 | 原因 | 否决方案 |
|---|------|------|---------|
| UD-01 | 所有组件使用 M3 标准组件，不自定义 View | 维护成本低，暗色/无障碍自动支持 | 自定义 Canvas 绘制（过于复杂） |
| UD-02 | 遮罩层使用 AnimatedVisibility + 半透明背景 | 平滑过渡 + 可被返回键取消 | Modal (Dialog) — 返回键行为不直观 |
| UD-03 | 倒计时使用 ViewModel 协程 + StateFlow | 首次稳定，配置变更不重置 | LaunchedEffect — 组合树变更时会重置 |
| UD-04 | 密码 visible 状态用 remembered 局部状态 | 不需要跨页面保留，ViewModel 最小化 | ViewModel 管理 — 增加不必要的状态复杂度 |
| UD-05 | 键盘适配使用 imePadding() | M3 推荐方式，自动处理 | 手动 WindowInsets — 冗余代码 |
| UD-06 | 手机号格式化使用 VisualTransformation | 光标位置可控制 | TextFieldValue 直接修改 — 光标跳动 |

---

## 11. 多视角评审记录

> 评审方式: delegate_task 并行三视角（C1 UX / C2 视觉 / C3 前端）
> 评审时间: 2026-06-14
> 评审模型: deepseek-v4-flash

### 11.1 C1 UX 交互评审 (34/50)

| # | 问题 | 严重度 | 说明 |
|---|------|:------:|------|
| C1-01 | 协议勾选框触摸目标仅18×18px | P0 | 远低于48dp规范，违反D-09无障碍基线。建议整行Row可点击 |
| C1-02 | Loading遮罩层未覆盖TopAppBar | P0 | HTML原型.overlay仅覆盖.scroll-area，返回按钮裸露。需全屏覆盖 |
| C1-03 | SMS Retriever自动填充无UI规约 | P1 | 无填充成功/失败视觉反馈，建议添加supportingText提示 |
| C1-04 | 密码可见性toggle独立性需确认 | P1 | UD-04写"remembered局部状态"但两个密码框共享状态？ |
| C1-05 | 获取验证码按钮无SendingSms中间态 | P1 | 缺少API调用中的loading spinner过渡态 |
| C1-06 | 验证码3次错误降级无视觉呈现 | P1 | CodeWrong3x状态仅文本定义，HTML原型缺失 |
| C1-07 | 服务器错误与网络错误未区分 | P1 | 原型中仅一种错误Snackbar，应区分文案和action |
| C1-08 | 缺少IME Action导航链定义 | P1 | Next/Done键盘动作未指定字段焦点跳转链 |
| C1-09 | 键盘弹起时验证码按钮被遮挡风险 | P1 | 小屏设备OutlinedButton可能被键盘完全遮挡 |
| C1-10 | 横屏布局未定义具体方案 | P2 | 仅状态列表有Landscape无布局说明 |
| C1-11 | 协议页"本地备份"徽标暴露实现细节 | P2 | 对终端用户无意义 |
| C1-12 | "去登录"自动填入手机号无视觉提示 | P2 | 跳转后无告知用户手机号已填入 |
| C1-13 | 注册成功Snackbar缺少action按钮 | P2 | 仅文字无"进入主页"链接 |
| C1-14 | TopAppBar标题居中方式脆弱 | P2 | absolute居中在有更多右侧icon时会偏移 |

### 11.2 C2 视觉审美评审 (30.5/50) — 基于HTML源码推断，非截图验证

| # | 维度 | 评分(1-5) | 关键问题 |
|---|------|:--------:|---------|
| 一 | 格式塔感知 | 3.5 | 分组清晰但协议行间距断裂8dp节奏 |
| 二 | 视觉层级 | 3.5 | title字重偏轻，"去登录"primary色与主按钮竞争 |
| 三 | 色彩系统 | 3.5 | Snackbar硬编码颜色未走token |
| 四 | 字体排版 | 3.0 | 无显式行高声明，label 12px偏小 |
| 五 | 空间与网格 | 3.0 | 多处未对齐8dp(sms gap=12dp, agreement gap=4dp) |
| 六 | 布局与比例 | 4.0 | 比例合理，横屏约束得当 |
| 七 | 可感知性与可操作性 | 3.5 | aria-label全覆盖(优)但checkbox热区不达标 |
| 八 | 一致性 | 2.5 | CSS类名碎片化，跨文件样式不一致 |
| 九 | 情感与品牌 | 2.5 | 无品牌差异化，M3默认紫色无辨识度 |
| 十 | 平台与适配 | 3.5 | Dark token正确但缺safe-area/notch处理 |

**P0/P1问题:** checkbox触摸目标不达标(P0)；CSS类名碎片化+P1间距偏离+Snackbar硬编码+无line-height声明(P1)

**审美亮点:** 暗色模式Token完整、aria-label全覆盖、状态机设计细致、遮罩层决策合理、微交互到位、对比度预演

### 11.3 C3 前端实现评审 (33/50)

| # | 问题 | 严重度 | 维度 |
|---|------|:------:|------|
| C3-01 | 提交遮罩取消未定义网络层Job取消 | P0 | Compose陷阱：BackHandler仅改UI状态，未cancel() Retrofit Call |
| C3-02 | WebView缺少DisposableEffect销毁 | P0 | AndroidView包裹WebView无onDispose，内存泄漏风险 |
| C3-03 | ClickableText协议链接TalkBack不可独立聚焦 | P0 | 无障碍：AnnotatedString链接无独立焦点节点 |
| C3-04 | 倒计时恢复未覆盖协程重启逻辑 | P1 | ViewModel init{}检测SavedStateHandle并重启协程逻辑缺失 |
| C3-05 | 倒计时按钮文案变化导致布局跳变 | P1 | "获取验证码"→"60s后重试"→"重新获取"宽度跳变 |
| C3-06 | Snackbar与键盘弹起层级冲突 | P1 | imePadding与SnackbarHost协调未定义 |
| C3-07 | PhoneVisualTransformation删除操作光标偏移风险 | P1 | OffsetMapping需覆盖插入/删除/全选/替换 |
| C3-08 | 暗色模式CircularProgressIndicator对比度不足 | P1 | primary #D0BCFF在75%黑遮罩上对比度≈3:1 |
| C3-09 | WebView重试策略未定义 | P1 | 重试远程序列vs直接本地fallback |
| C3-10 | remember vs rememberSaveable歧义 | P1 | AC-28要求配置变更保留，UD-04措辞"remembered" |
| C3-11 | 注册按钮contentDescription动态切换未标注 | P2 | loading时contentDescription应同步更新 |
| C3-12 | verticalScroll键盘焦点自动滚动未定义 | P2 | 大字体1.5x下焦点字段可能遮挡 |
| C3-13 | 间距16dp硬编码未提取Token | P2 | 影响Design-to-Code质量 |
| C3-14 | 系统时间手动修改导致倒计时跳变 | P2 | 边缘场景但应记录 |

**组件复用率:** 82%（M3直用14个 / 自定义封装7个）

**工时校准:** 原11h → 校准18.5h（+68%），主因集成测试2h + 运行时逻辑+2.5h + 无障碍+0.5h

### 11.4 P0 共识与冲突调和

**三方共识P0（必须修复）:**

| # | P0问题 | C1 | C2 | C3 | 决议 |
|---|--------|:--:|:--:|:--:|------|
| UR-01 | 协议Checkbox触摸目标<48dp | ✅ | ✅ | — | AgreementRow改为整行Row可点击，Checkbox仅视觉指示 |
| UR-02 | Loading遮罩全屏覆盖 | ✅ | — | — | 遮罩从Scaffold层覆盖，TopAppBar返回按钮在loading期间禁用 |
| UR-03 | 取消请求需cancel协程Job | — | — | ✅ | BackHandler中持有Deferred引用执行cancel() |
| UR-04 | WebView DisposableEffect销毁 | — | — | ✅ | onDispose中调用webView.destroy() |
| UR-05 | ClickableText链接无障碍 | — | — | ✅ | 改为Row(Checkbox+Text+TextButton)方案 |

**无冲突项 — 自动采纳（各方评价一致）：**

- 暗色模式Token体系完整保留 ✅
- aria-label全覆盖保留 ✅
- 倒计时ViewModel协程方案保留 ✅
- 遮罩层AnimatedVisibility+半透明方案保留 ✅
- 获取验证码SendingSms中间态补充 (C1+P1) → 纳入修订
- IME Action链补充 (C1+P1) → 纳入修订
- SMS Retriever自动填充视觉反馈补充 (C1+P1) → 纳入修订

### 11.5 质量门禁检查

| 检查项 | 状态 | 说明 |
|--------|:----:|------|
| 暗色主题色板定义 | ✅ | 所有HTML含@media dark完整Token |
| WindowInsets systemBars | ⚠️ | UI_DESIGN提及imePadding()，但未显式调用systemBars |
| 对比度验证 WCAG AA | ✅ | Token映射表含7组对比度预演，全部达标 |
| fontScale上限clamp | ❌ | 未定义fontScale.clamp()或coerceIn()策略 |
| 动画总时长≤2s | ✅ | AnimatedVisibility默认300ms |
| 字号与Theme Token一致性 | ⚠️ | M3默认值使用中，无项目自定义Token |
| 异常降级策略 | ⚠️ | 部分覆盖(SMS fallback)，泛化不足 |
| 暗色surface层级 | ✅ | token值正确(surface<surfaceVariant暗色) |
| CSS Token自包含 | ✅ | grep验证通过，每文件自包含全部Token |
| SearchBar focus-within primary | N/A | 无搜索栏 |

## 12. 评审决议

| 状态 | 说明 |
|:----:|------|
| ⚠️ | 有条件通过 (C1:34, C2:30.5, C3:33, 综合32.5/50) |
| P0必须修复 | 5项UR-01~05，需在编码阶段启动前关闭 |
| P1建议修复 | 17项跨三视角，在编码过程中逐项解决 |
| P2可延后 | 14项，不影响核心功能交付 |

下一阶段准入条件：5项P0决议在DECISIONS.md确认后，人工批准进入STAGE_DESIGN。

---

## 变更记录

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| v0.2-review | 2026-06-14 | 三视角评审完成 (C1 34/50 + C2 30.5/50 + C3 33/50)；5项P0决议；HTML原型6个文件 |
| v0.1-draft | 2026-06-14 | 初始版本，基于 PRD §9 (v1.2-confirmed) 生成 |
