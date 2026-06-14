# 项目决策记录
> 本文档记录架构选型、技术决策及变更历史。

## 决策列表

| 编号 | 日期 | 决策内容 | 状态 |
|------|------|----------|------|
| D-01 | 2026-06-13 | Token 存储方案：EncryptedSharedPreferences 加密持久化，禁止 DataStore 明文存储 | 已确认 |
| D-02 | 2026-06-13 | API 版本化：所有路径使用 /api/v1/ 前缀 | 已确认 |
| D-03 | 2026-06-13 | 密码特殊字符策略：前端不拦截合法特殊字符（@#$%等），仅校验长度+字母+数字组合 | 已确认 |
| D-04 | 2026-06-13 | 错误反馈策略：字段级错误用 supportingText，全局错误用 Snackbar + action | 已确认 |
| D-05 | 2026-06-13 | 提交防重策略：遮罩层 + 按钮 disabled + 文字变为"注册中..." | 已确认 |
| D-06 | 2026-06-13 | 倒计时持久化：基于服务器返回 timestamp 计算，杀进程后失效恢复可点击 | 已确认 |
| D-07 | 2026-06-13 | SavedStateHandle 持久化字段：phone, password, smsSentTimestamp, agreementAccepted | 已确认 |
| D-08 | 2026-06-13 | 目标市场：中国大陆手机号（11 位 1 开头），不含国际号码 | 已确认 |
| D-09 | 2026-06-13 | 无障碍 baseline：所有交互元素设置 contentDescription，触摸目标 ≥ 48dp | 已确认 |
| D-10 | 2026-06-13 | PRD 冻结：v1.0-confirmed，多视角评审（产品/技术/UX/QA）通过后人工确认 | 已确认 |
| D-11 | 2026-06-13 | 验证码发送方式：后端统一短信发送 | 已确认 |
| D-12 | 2026-06-13 | 协议链接形式：in-app WebView 加载远程 URL，含本地 fallback | 已确认 |
| D-13 | 2026-06-13 | "去登录"自动填入手机号：从已注册拦截跳转时自动填入 | 已确认 |
| D-14 | 2026-06-13 | 注册页底部入口：TextButton "已有账号？去登录" | 已确认 |
| R2-D-15 | 2026-06-13 | 协议优先级修正：AC-12 P2→P0（法律合规），AC-13 P2→P1（核心逃逸路径） | 第2轮已修订 |
| R2-D-16 | 2026-06-13 | 注册成功反馈：Snackbar "注册成功"绿色，短暂停留后自动登录 | 第2轮已修订 |
| R2-D-17 | 2026-06-13 | 验证码格式/防暴力：仅6位数字，连续3次错误提示重新获取 | 第2轮已修订 |
| R2-D-18 | 2026-06-13 | Loading 可取消：遮罩期间系统返回键取消请求，表单保留 | 第2轮已修订 |
| R2-D-19 | 2026-06-13 | 提交失败表单保留：所有错误路径保留已填数据 | 第2轮已修订 |
| R2-D-20 | 2026-06-13 | 网络超时独立处理：15s 超时提示，区别于服务端错误 | 第2轮已修订 |
| R2-D-21 | 2026-06-13 | SMS 自动填充降级：SMS Retriever 优先，不可用时手动输入+粘贴 | 第2轮已修订 |
| R2-D-22 | 2026-06-13 | 密码可见状态保留：配置变更后可见/隐藏状态不变 | 第2轮已修订 |
| R2-D-23 | 2026-06-13 | SavedStateHandle 安全：移除 password 字段，仅存 phone/smsSentTimestamp/agreementAccepted | 第2轮已修订（覆盖 D-07） |
| R2-D-24 | 2026-06-13 | 协程生命周期：所有协程绑定 viewModelScope，倒计时用 ViewModel 协程替代 LaunchedEffect | 第2轮已修订 |
| R2-D-25 | 2026-06-13 | Token 持久化后导航恢复：App 启动检测 Token 已存但未进主页，自动登录跳转 | 第2轮已修订 |
| R2-D-26 | 2026-06-13 | 深色模式对比度：error supportingText 满足 WCAG AA 4.5:1，遮罩 60% 不透明度 | 第2轮已修订 |

## 变更历史

| 日期 | 版本 | 变更内容 |
|------|------|----------|
| 2026-06-13 | v1.0 | 初始决策集（PRD 第 1 轮多视角评审产出） |
| 2026-06-13 | v1.1 | 4 项假设（R-01~R-04）全部确认，PRD 重新冻结 |
| 2026-06-13 | v1.2 | 第2轮深度评审：新增 AC 22~28（7条）、Gherkin TC-19~23（5个）、决议 R2-D-15~26（12项）；修复 14 个 P0 运行时行为定义空白 |
| 2026-06-14 | v1.3 | UI 设计阶段（v0.2-review）：三视角并行评审完成；决议 UD-01~06 + UR-01~05；HTML 原型 6 个文件 |

---

### 2026-06-14 — UI 设计决议：用户注册 UI 设计方案 v0.2-review

**评审概要：** C1 UX:34/50, C2 视觉:30.5/50(M3默认色板·无品牌差异), C3 前端:33/50(Compose可行性9/10·组件复用率82%)

**决策**：

| 编号 | 决策 | 原因 | 否决方案 |
|------|------|------|---------|
| UD-01 | 全部使用 M3 标准组件 | 维护成本低，暗色/无障碍自动支持 | 自定义 Canvas 绘制 |
| UD-02 | 遮罩层 AnimatedVisibility + 半透明背景 | 平滑过渡+返回键可取消 | Modal Dialog |
| UD-03 | 倒计时 ViewModel 协程 + StateFlow | 配置变更不重置 | LaunchedEffect |
| UD-04 | 密码visible rememberSaveable局部状态 | 按组件独立，配置变更保留 | ViewModel管理 |
| UD-05 | 键盘适配 imePadding() | M3推荐方式 | 手动WindowInsets |
| UD-06 | 手机号格式化 VisualTransformation | 光标位置可控 | TextFieldValue直接修改 |

**P0 修订决议（评审发现·必须关闭）：**

| 编号 | 决议 | 原因 | 实现要求 |
|------|------|------|---------|
| UR-01 | 协议Checkbox触摸目标≥48dp | C1/C2双视角指出18px不达标，违反D-09 | AgreementRow改为整行Row可点击，Checkbox仅视觉指示 |
| UR-02 | Loading遮罩全屏覆盖Scaffold层 | C1发现仅覆盖表单区，TopAppBar返回按钮裸露 | 遮罩覆盖TopAppBar，loading期间禁返回按钮 |
| UR-03 | 取消请求时cancel Retrofit Call/协程Job | C3发现BackHandler仅改UI状态未cancel网络层 | 持有Deferred引用执行cancel() |
| UR-04 | WebView DisposableEffect销毁 | C3发现AndroidView无onDispose，内存泄漏 | onDispose中webView.destroy() |
| UR-05 | 协议链接用Row(Checkbox+Text+TextButton) | C3发现ClickableText链接TalkBack不可独立聚焦 | 放弃ClickableText方案，改用独立TextButton |

**P1 建议修订：** 17项（SendingSms中间态、IME Action链、SMS Retriever反馈、倒计时按钮宽度锁定、Snackbar键盘协调、PhoneVisualTransformation删除光标、rememberSaveable措辞修正、WebView重试策略等）— 详见 UI_DESIGN.md §11

**影响范围：** RegisterScreen、AgreementScreen 两个新增页面；PhoneVisualTransformation、CountdownState、SubmitOverlay 三个核心组件

**关联：** UI_DESIGN.md §11 | PRD §9

---

### 2026-06-14 — 技术方案 DESIGN.md v1.0：用户注册设计决议

**交付物：** docs/DESIGN.md（架构概览、模块设计、接口定义、安全考虑、测试策略、6 项 ADR）

**新增决策：**

| 编号 | 决策 | 原因 | 否决方案 |
|------|------|------|---------|
| D-15 | Token 存储: EncryptedSharedPreferences (AES-256 GCM) | root 设备防读取，与 Keystore 集成 | DataStore / SharedPreferences 明文 |
| D-16 | 倒计时: ViewModel 协程 + StateFlow + 服务器 timestamp | 配置变更不重置，杀进程后自动失效 | LaunchedEffect（组合树变更时重置） |
| D-17 | 错误反馈: supportingText(字段级) + Snackbar(全局级) | 区分输入错误和网络错误，action 提供恢复路径 | 统一 Toast / 统一内联 |
| D-18 | SavedStateHandle 安全: 仅存 phone/smsSentTimestamp/agreementAccepted | password 明文禁入 Bundle，防 IPC 泄露 | 全字段持久化（安全红线） |
| D-19 | SMS 自动填充: Retriever API 优先 + 手动输入降级 | GMS 设备自动填充，无 GMS 设备粘贴 | 仅依赖自动填充（国内设备不可用） |
| D-20 | DI 作用域: Repository/Service @Singleton, ViewModel @HiltViewModel | 合理复用 + 正确绑定导航生命周期 | — |

**测试策略：** 18 单元测试 + 8 UI 测试 + 6 集成测试 + 2 E2E → 34 场景

**关联：** DESIGN.md | PRD §6 | DECISIONS.md D-01 ~ D-14
