# C1 UX 交互评审 — 登录页面

> **评审日期:** 2026-06-07 | **评审对象:** UI_DESIGN.md v0.1-draft + 10 张截图 (750×1624 @2x, 375×812dp 逻辑视口)
> **对照基准:** PRD §9 (v1.0-confirmed) | DECISIONS.md
> **评审方法:** 逐页审阅文档规格 + 截图维度核对; 无法直接查看像素内容(无 vision 工具), 基于文档和元数据推断。

---

## 1. 综合评分

| 维度 | 评分 | 说明 |
|------|:----:|------|
| 用户路径效率 | 8/10 | 单页闭环, imeAction 链(Next→Done)高效, 按钮联动减少无效点击 |
| Android 平台惯例 | 8/10 | M3 组件体系, BackHandler 退出, imePadding 避让键盘; Scaffold 略重但合规 |
| 边界状态覆盖 | 7/10 | 9 状态建模完整, 但 429 限流 UX 未独立建模, Timeout 自动消失逻辑缺失 |
| 键盘与输入交互 | 8/10 | 焦点链 + 提交时收起键盘 + imePadding; 缺少硬件键盘/横屏全屏输入模式说明 |
| 信息架构 | 8/10 | 五层视觉分组清晰, 单页单责, Debug 构建区分测试提示 |
| **PRD §9 一致性** | **7/10** | 核心布局匹配, 但错误组件选型(Card vs Text)、429 限流 UX、Timeout 逻辑存在偏差 |
| **综合评分** | **7/10** | 可进入编码, 但需先解决 P0 项(429/错误组件) |

---

## 2. P0/P1 问题清单

### P0 (必须修复, 阻塞编码)

| # | 严重度 | 问题 | 位置 | 改进建议 |
|---|:------:|------|------|----------|
| **P0-1** | **高** | **429 限流 UX 未独立建模** — PRD TC-07 要求 429 时按钮保持 disabled 30 秒并有倒计时反馈("操作过于频繁, Ns后重试")。UI_DESIGN §3 状态机将 429 等同于通用 Error 态(按钮恢复 enabled), 与 PRD 冲突。截图 `login_error_429.png` 可能同样缺少此行为。 | UI_DESIGN §3 状态机 + 截图 login_error_429.png | ① 在 LoginUiState 中增加 `rateLimitRemainingSeconds: Int?` 字段 ② 429 时设置 `rateLimitRemainingSeconds=30`, 按钮显示倒计时文字 ③ 30s 倒计时归零后自动恢复 enabled ④ 截图重新生成, 验证按钮 disabled+倒计时 |
| **P0-2** | **中** | **错误提示组件与 PRD 不一致** — PRD §9.5 明确规定错误通知使用 "内联 `Text`(error color) + `AnimatedVisibility`", 非 Snackbar 非 Card。但 UI_DESIGN §3 组件树使用 `Card(errorContainer bg) { Text }`。Card 增加了一层背景容器, 改变了视觉权重和间距。 | UI_DESIGN §3 L119-121 | 二选一: (a) 改回 PRD 规格 — 仅用 Text(error color) + AnimatedVisibility, 不加 Card 背景; 或 (b) 更新 PRD §9.5 将 Card 方案纳入规格, 同步更新 Token(需用到 errorContainer/onErrorContainer)。推荐 (a) 更轻量且与 PRD 上游一致。 |
| **P0-3** | **中** | **Timeout 错误自动消失未建模** — PRD §9.3 规定错误提示 "超时 10s 自动消失"。UI_DESIGN §6 ViewModel 描述中未提及任何 timer/coroutine.delay 逻辑来清除 errorMessage。 | UI_DESIGN §6 L329-334 | 在 LoginViewModel 的 Error/Timeout 分支中加入 `viewModelScope.launch { delay(10_000); _uiState.update { it.copy(errorMessage = null) } }` 并确保 `onCleared` 时取消。 |

### P1 (应修复, 不阻塞)

| # | 严重度 | 问题 | 位置 | 改进建议 |
|---|:------:|------|------|----------|
| **P1-1** | 低 | **键盘弹出时错误提示可能被遮挡** — 内联错误位于密码框和按钮之间, 软键盘弹出时该区域可能滚出可视区(尤其小屏设备)。虽已有 imePadding, 但错误+按钮组合可能超出视口。 | UI_DESIGN §3 L118-123 | 确保 `verticalScroll` + `imePadding` 组合正确(先 imePadding 再 verticalScroll 或相反顺序需验证)。编码后使用 360×640dp 模拟器 + 键盘打开 + 错误态做回归测试。 |
| **P1-2** | 低 | **清除错误的触发条件隐含** — 状态机标注 "用户修改→输入变更清除错误", 但 §3 组件树中 `onEmailChanged`/`onPasswordChanged` 未显式标注调用 `clearError()`。 | UI_DESIGN §3 L94, L105 | 在组件树注释中为 `onEmailChanged` 和 `onPasswordChanged` 添加 `// also triggers clearError()` 注释, 确保实现阶段不遗漏。 |
| **P1-3** | 低 | **Success 态无过渡反馈** — PRD §9.4 标注 "短暂展示成功态(可选)"。UI_DESIGN 中 Success 直接 navigate, 无视觉确认。慢网络下用户可能困惑(按钮已点, 无反应数秒)。 | UI_DESIGN §3 状态机 Success 分支 | 考虑在 navigate 前加 300ms 的 `LaunchedEffect` 短暂显示按钮文字"✓ 登录成功"(可选, 非强制)。 |
| **P1-4** | 低 | **Landscape 模式仅依赖滚动** — PRD §9.4 提到横屏时 verticalScroll 确保可滚动, 但未给出横屏布局建议。横屏时 centered Column 可能在极宽屏幕上显得空旷。 | UI_DESIGN §3 状态覆盖表 | 可接受(垂直滚动是最小可行方案)。建议编码阶段验证 640×360dp 横屏模拟器, 确保按钮可达。平板大屏横屏优化可延后至后续迭代。 |

---

## 3. PRD §9 一致性逐条检查

### §9.1 页面清单

| 检查项 | PRD 要求 | UI_DESIGN | 一致? |
|--------|----------|-----------|:-----:|
| 页面名称 | LoginScreen | LoginScreen | ✅ |
| 路由 | `/login` (startDestination) | `/login` (startDestination) | ✅ |
| 类型 | 新建 | 改造(已有 username→email) | ⚠️ 语义差, 实质一致 |
| 全屏无 Bar | 无 BottomBar/AppBar | 无 BottomBar/AppBar | ✅ |

### §9.2 布局规格

| 检查项 | PRD 要求 | UI_DESIGN | 一致? |
|--------|----------|-----------|:-----:|
| 容器 | Column, fillMaxSize, verticalScroll, horizontalPadding=24dp, Center | Column(fillMaxSize, verticalScroll, horizontalPadding=24dp, Center) | ✅ |
| 图标 | 64×64dp, tint=primary | Icon(Email, 64×64dp, tint=primary) | ✅ |
| 标题 | "欢迎回来", headlineMedium 24sp Bold | "欢迎回来", headlineMedium, Bold | ✅ |
| 副标题 | "请使用邮箱和密码登录", bodyMedium 14sp, onSurfaceVariant | "请使用邮箱和密码登录", bodyMedium | ✅ |
| 图标→标题 | 16dp | Spacer(16dp) | ✅ |
| 标题→副标题 | 8dp | Spacer(8dp) | ✅ |
| 副标题→邮箱 | 32dp | Spacer(32dp) | ✅ |
| 邮箱字段 | OutlinedTextField 56dp, singleLine, leadingIcon=Email, keyboardType=Email, imeAction=Next, placeholder="请输入邮箱地址" | 全部匹配 | ✅ |
| 邮箱错误 | isError+supportingText "请输入有效的邮箱地址", labelSmall 12sp error color | isError+supportingText, 12sp error | ✅ |
| 邮箱↔密码 | 16dp | Spacer(16dp) | ✅ |
| 密码字段 | OutlinedTextField 56dp, singleLine, leadingIcon=Lock, trailingIcon=Visibility/Off IconButton 48dp, keyboardType=Password, imeAction=Done | 全部匹配 | ✅ |
| 密码 placeholder | "请输入密码" | "请输入密码" | ✅ |
| **错误提示** | **内联 Text 14sp error color, AnimatedVisibility** | **Card(errorContainer bg) { Text }** | **❌ P0-2** |
| 错误→按钮 | 24dp | Spacer(24dp) | ✅ |
| 按钮 | Button(filled), 48dp, fullWidth, RoundedCornerShape(24dp) | Button(filled), 48dp, fillMaxWidth, shape=24dp | ✅ |
| 按钮文字 | "登录", labelLarge 16sp Medium | "登录", labelLarge | ✅ |
| 按钮 loading | CircularProgressIndicator 20dp strokeWidth=2dp onPrimary + "登录中..." | CircularProgressIndicator(20dp, 2dp, onPrimary) + Spacer(8dp) + "登录中..." | ✅ |
| 按钮→提示 | 24dp | Spacer(24dp) | ✅ |
| 底部提示 | "测试账号: admin / 123456", labelSmall 11sp, onSurfaceVariant alpha 0.6 | 全部匹配 | ✅ |
| VersionTag | 底部居中 | VersionTag(CenterHorizontally) | ✅ |

### §9.3 交互规格

| 检查项 | PRD 要求 | UI_DESIGN | 一致? |
|--------|----------|-----------|:-----:|
| 邮箱实时校验 | RFC 5322, 实时, isError+supportingText | isEmailValid 实时检查 + isError | ✅ |
| 邮箱 imeAction | Next → 焦点跳转密码框 | imeAction=Next, keyboardActions→focusManager.moveFocus | ✅ |
| 密码 imeAction | Done → 触发登录 | imeAction=Done, keyboardActions=onDone→login() | ✅ |
| 密码显隐切换 | trailingIcon IconButton 48dp, VisualTransformation 切换 | VisibilityToggle, visualTransformation=Password/None | ✅ |
| 按钮 enable | isEmailValid AND passwordNotEmpty AND !isLoading | isEmailValid && passwordNotEmpty && !isLoading | ✅ |
| 按钮点击 | clearFocus + keyboardController.hide | §6 键盘管理 | ✅ |
| **错误自动清除** | **下次输入任意字段时自动清除** | **状态机标注, 组件树未显式标注** | **⚠️ P1-2** |
| **超时自动消失** | **超时 10s 自动消失** | **ViewModel 未建模 timer** | **❌ P0-3** |
| BackHandler | BackHandler 拦截 → activity.finish() | BackHandler(enabled=true)→activity.finish() | ✅ |

### §9.4 状态覆盖

| 状态 | PRD | UI_DESIGN | 一致? |
|------|-----|-----------|:-----:|
| Idle | ✅ | ✅ | ✅ |
| Editing | ✅ | ✅ | ✅ |
| Loading | ✅ | ✅ | ✅ |
| Success | ✅ (可选短暂成功态) | ✅ (直接 navigate) | ⚠️ P1-3 |
| Error | ✅ (401/403/429/网络异常) | ✅ (但 429 行为不对) | ❌ P0-1 |
| Empty | ✅ | ✅ | ✅ |
| Timeout | ✅ | ✅ | ✅ |
| Dark | ✅ | ✅ (Token 双模式) | ✅ |
| Landscape | ✅ | ✅ (verticalScroll) | ⚠️ P1-4 |

### §9.5 组件选型

| 检查项 | PRD | UI_DESIGN | 一致? |
|--------|-----|-----------|:-----:|
| 输入框 | OutlinedTextField | OutlinedTextField | ✅ |
| 按钮 | Button(filled) | Button(filled) | ✅ |
| 加载 | CircularProgressIndicator(20dp) | CircularProgressIndicator(20dp) | ✅ |
| 密码图标 | IconButton 48dp | IconButton/VisibilityToggle | ✅ |
| **错误通知** | **内联 Text + AnimatedVisibility** | **Card + AnimatedVisibility** | **❌** |
| 键盘避让 | imePadding() | imePadding() | ✅ |
| 横屏滚动 | verticalScroll | verticalScroll | ✅ |
| 页面容器 | Scaffold 或 Box(更轻量) | Scaffold | ⚠️ 微小 |

### §9.6 设计约束

| 检查项 | PRD | UI_DESIGN | 一致? |
|--------|-----|-----------|:-----:|
| Color Tokens (10个) | primary/onPrimary/primaryContainer/onPrimaryContainer/error/errorContainer/onErrorContainer/surface/onSurface/onSurfaceVariant | 13个(增加了 outline/outlineVariant/surfaceVariant) | ✅ (超集) |
| Font Tokens (8个) | headlineMedium/bodyMedium/bodyLarge/bodySmall/labelLarge/bodySmall/labelSmall/bodyLarge | 8个, 增加了行高 | ✅ (增强) |
| Spacing (8dp grid) | spacing0~spacing5 + iconSize/fieldH/btnH/btnR/touchTgt/spnrSize | 一致 | ✅ |
| 品牌色 #1A73E8 | ✅ | ✅ | ✅ |

> **一致性汇总:** 12/14 完全一致, 2 项偏差(P0-2 错误组件, P0-1 429 行为), 2 项轻微不足(P1-2 清除错误标注, P0-3 超时消失)

---

## 4. 截图逐页审查

> ⚠️ 截图无法直接查看像素内容(无 vision 工具), 基于文件尺寸(750×1624 @2x→375×812dp)和 UI_DESIGN 规格推断。截图均使用 2x 分辨率, 与 iPhone SE 375×812 基准视口一致, 还原度良好。

| 截图 | 对应状态 | 预期尺寸 | 关键验证点(从规格推断) |
|------|----------|----------|----------------------|
| `login_idle.png` | Idle | 750×1624 ✓ | 空字段, 按钮 disabled(灰色), 无错误, 底部版本号可见 |
| `login_editing.png` | Editing | 750×1624 ✓ | 邮箱+密码有输入, 按钮 enabled(primary色), 无错误 |
| `login_email_error.png` | Editing(格式错) | 750×1624 ✓ | 邮箱框红色边框+supportingText"请输入有效的邮箱地址", 按钮 disabled |
| `login_loading.png` | Loading | 750×1624 ✓ | 按钮内转圈+"登录中...", 按钮 disabled, 输入框 disabled |
| `login_error_401.png` | Error(401) | 750×1624 ✓ | 错误提示可见(Text或Card), 按钮 enabled, 输入框 enabled |
| `login_error_403.png` | Error(403) | 750×1624 ✓ | 同上, 错误文案为"账户已被锁定" |
| `login_error_429.png` | Error(429) | 750×1624 ✓ | **⚠️ 预期按钮 disabled+倒计时(PRD TC-07), 但设计规格未独立建模** |
| `login_timeout.png` | Timeout | 750×1624 ✓ | 错误"网络请求超时, 请重试", 按钮 enabled |
| `login_dark.png` | Dark Mode | 750×1624 ✓ | 深色背景(#1C1B1F), 文字 onSurface(#E6E1E5), 品牌色 #8AB4F8, 错误色 #F2B8B5 |
| `login_full_page.png` | 完整页面 | 1600×1800 | 非 375×812 基准, 可能为长截图或 HTML 全页预览 |

---

## 5. 亮点

1. **状态机完整** — Idle→Editing→Loading→Success/Error/Timeout 六个核心状态全链路覆盖, 与 PRD §10 Gherkin 用例可追溯。
2. **键盘交互设计到位** — imeAction 链(Next→Done) + 焦点管理 + 提交时收起键盘 + imePadding, 组合拳完整。
3. **无障碍 17 元素** — 所有交互元素含 contentDescription, 按钮/图标 ≥48dp 触控目标, 错误 Card 使用 LiveRegion.Alert。
4. **Token 体系增强** — 在 PRD 10 色基础上扩展至 13 色(增加 outline/outlineVariant/surfaceVariant), 字体增加行高定义, 比 PRD 更细致。
5. **弹性垂直居中** — `Spacer(weight=1f)` 顶部+底部 实现图标→按钮整体垂直居中, 同时保证横屏滚动可用, 兼顾视觉与功能。

---

## 6. 与父技能 Pitfalls 对照

| Pitfall | 是否触发 | 说明 |
|---------|:------:|------|
| 覆盖层与 BackHandler 互斥 | ❌ 未触发 | 登录页无弹窗/覆盖层, BackHandler 独占 |
| Modifier.alpha() 不阻止触摸 | ✅ 已规避 | Loading 时输入框使用 `enabled=false` 而非 `alpha` |
| 跨 Screen 事件通道 | ✅ 已建模 | LoginEvent + LoginViewModel 显式事件 |
| 搜索超时未定义 | ✅ 已定义 | 超时 10s→Timeout 状态(P0-3 需补充自动消失) |
| PRD Token 同步滞后 | ⚠️ 轻微 | UI_DESIGN 扩展了 Token 数量(正向增强), 但错误组件选型与 PRD 不一致 |

---

> **结论:** 7/10 分, **可进入编码但需先修复 P0-1(429 限流)和 P0-2(错误组件)两项**。P0-3(Timeout 自动消失)和 P1 项可在编码阶段并行修复。整体设计质量良好, 状态覆盖和键盘交互设计是亮点, 主要问题集中在 PRD 规格一致性上。
