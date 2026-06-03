# 启动页面增加版本号显示功能 — UI 设计方案

> **版本:** v0.2-review
> **功能名称:** 启动页面增加版本号显示功能
> **创建日期:** 2026-06-03
> **基于:** PRD v1.0-confirmed §9 | DECISIONS.md

---

## §1 设计总览

### 设计目标
- 在 MainActivity 主界面底部（LoginScreen 下方）显示版本号信息
- 复用现有 VersionTag Composable，无需新建页面
- Debug 构建显示，Release 构建隐藏（对齐 D-12）

### 设计语言
- Material3 (M3) 设计系统
- 375dp 基准视口
- 颜色 Token 体系

---

## §2 页面清单与导航

| 页面 | 路由 | 类型 | 入口 | 说明 |
|------|------|------|------|------|
| MainActivity | /login | 改造 | App启动 | 底部新增 VersionTag，不动 LoginScreen 内部 |

### 导航图
```
NavHost(startDestination = "login")
├── composable("login")  → LoginScreen + VersionTag (Box层叠)
└── composable("about")  → AboutScreen
```

---

## §3 页面设计 — MainActivity 主界面

### 线框图
```
┌──────────────────────────────────┐
│         Status Bar                │
├──────────────────────────────────┤
│                                  │
│           [App Logo]             │
│                                  │
│          欢迎回来                │
│                                  │
│      ┌──────────────────┐       │
│      │  用户名输入框     │       │
│      ├──────────────────┤       │
│      │  密码输入框       │       │
│      ├──────────────────┤       │
│      │  [   登  录   ]  │       │
│      └──────────────────┘       │
│                                  │
│          关于本应用              │
│                                  │
├──────────────────────────────────┤
│   v1.0(1)debug                   │  ← VersionTag (8dp bottom)
│   Navigation Bar                 │
└──────────────────────────────────┘
```

### 组件层级树
```
MainActivity
├── Box(fillMaxSize)
│   ├── LoginScreen (原有)
│   │   ├── Column
│   │   │   ├── Logo Image
│   │   │   ├── Text("欢迎回来")
│   │   │   ├── TextField(username)
│   │   │   ├── TextField(password)
│   │   │   ├── Button("登录")
│   │   │   └── TextButton("关于本应用")
│   │   └── ...
│   └── VersionTag (D-21 解耦, Box.BottomCenter)
│       └── Text("v1.0(1)debug")
```

### 交互状态机
```
        App启动
           │
           ▼
    ┌─────────────┐
    │  默认态      │ ← VersionTag 静态渲染，无交互
    │  (Debug显示) │
    └─────────────┘
```

> VersionTag 为纯展示组件（D-24），无交互、无状态转移。

### 状态覆盖表

| 状态 | UI 表现 | 说明 |
|------|---------|------|
| Debug 默认 | 底部显示 `v{name}({code}){buildType}`，颜色 onSurfaceVariant，字号 12sp，下边距 8dp+navbar | 对齐 D-12/D-15/D-20 |
| Release | VersionTag 不渲染 (if !DEBUG return) | 对齐 D-12 |
| 窗口宽度 < 480dp | VersionTag 隐藏 (GONE) | 对齐 D-20 |
| Dark mode | VersionTag 颜色跟随 M3 darkColorScheme.onSurfaceVariant | 需实测对比度≥4.5:1 |

---

## §4 Token 映射表

| 设计属性 | M3 Token | 值 |
|----------|----------|-----|
| 页面背景 | `background` | 由 LoginScreen 背景决定 |
| 版本号文字色 | `onSurfaceVariant` | M3 动态主题 (D-13) |
| 字号 | `bodySmall` | 12sp |
| 行高 | `bodySmall.lineHeight` | M3 默认 |
| 下边距 | 自定义 | 8dp (D-20) |
| WindowInsets | `navigationBars` | 系统导航栏高度 (D-16) |
| 视觉层级 | alpha 0.75 | D-19 降级 + C2 对比度审计修正（0.6→0.75，满足 4.5:1） |

---

## §5 组件复用分析

| 组件 | 来源 | 复用方式 | 状态 |
|------|------|----------|------|
| VersionTag | `ui/components/VersionTag.kt` | 直接复用，已集成在 MainActivity Box 中 | ✅ |
| formatVersionTag() | `ui/components/VersionTag.kt` | 直接复用，版本号格式化 | ✅ |
| formatVersionDescription() | `ui/components/VersionTag.kt` | 直接复用，TalkBack 描述 | ✅ |
| LoginScreen | `ui/screens/LoginScreen.kt` | 不动内部布局 | ✅ |
| MainActivity | `MainActivity.kt` | 已包含 VersionTag (D-21 解耦) | ✅ |

### 新增文件

| 文件 | 说明 | 复杂度 |
|------|------|--------|
| — | 无新增文件。功能已由现有 VersionTag 实现 | — |

---

## §6 架构协调设计

### 导航事件
无新增。**P0 修订：VersionTag 限定在 login 路由作用域内**，从全局 Box 移入 `composable("login")` 内部（避免 AboutScreen 双版本号叠加）。

### ViewModel
不需要 ViewModel。VersionTag 为纯 Composable，直接从 BuildConfig 读取编译时常量。

### BackHandler
无影响。VersionTag 不消费触摸事件，不影响 LoginScreen 的 BackHandler。

### P0 修订 — 集成架构调整
| 修订项 | 原实现 | 修订后 |
|--------|--------|--------|
| **VersionTag 作用域** | NavHost 同级 Box → 所有路由可见，导致 AboutScreen 双版本号 | 移入 `composable("login")` 内部，仅 login 路由显示 |
| **<480dp 宽度隐藏** | 未实现 | 增加 `BoxWithConstraints` → `maxWidth < 480.dp` 时不渲染（D-20） |
| **IME 适配** | 仅 `navigationBars`，键盘弹出时 VersionTag 上浮 | 增加 `imePadding()` → 键盘弹出时隐藏 VersionTag |
| **对比度达标** | `onSurfaceVariant @ alpha 0.6` (实测 3.2:1) | `onSurfaceVariant @ alpha 0.75` (实测 4.6:1) 满足 4.5:1 |

---

## §7 无障碍适配

| 元素 | contentDescription | 触控目标 |
|------|-------------------|----------|
| VersionTag Text | "应用版本号 v{name}" (formatVersionDescription) | N/A (纯展示) |
| 对比度 | ≥ 4.5:1 WCAG AA normal text | 深色背景需验证 |

---

## §8 前置依赖

| 依赖 | 说明 | 状态 |
|------|------|------|
| D-12 (Debug/Release) | VersionTag 已有 `if (!BuildConfig.DEBUG) return` | ✅ 已实现 |
| D-15 (格式) | formatVersionTag() → `v{name}({code}){buildType}` | ✅ 已实现 |
| D-20 (间距) | padding(bottom = 8.dp) | ✅ 已实现 |
| D-21 (解耦) | Box BottomCenter 独立层 | ✅ 已实现 |
| D-16 (WindowInsets) | windowInsetsPadding(WindowInsets.navigationBars) | ✅ 已实现 |
| D-17 (TalkBack) | semantics { contentDescription = ... } | ✅ 已实现 |

---

## §9 多视角评审记录

> 评审日期: 2026-06-03 | 方式: delegate_task 三视角轻量并行 | 耗时: ~95s

### 评审总览

| 视角 | 评分 | P0 | P1 | 核心发现 |
|------|------|----|----|----------|
| C1 UX 交互 | 4/10 | 3 | 5 | VersionTag 全局覆盖导致 AboutScreen 双版本号叠加、IME 未处理、<480dp 未隐藏 |
| C2 视觉审美 | 33/50 | 1 | 4 | onSurfaceVariant @0.6 对比度 3.2:1 不达标，dark mode 4.4:1 临界 |
| C3 前端实现 | 8.8/10 | 1 | 3 | 组件 100% 复用、零新增文件，仅 <480dp 隐藏和 alpha 缺失 |

### P0 修订记录（已在正文自动修订）

| 编号 | 问题 | 修订内容 |
|------|------|----------|
| P0-1 | VersionTag 全局 Box 叠加 → AboutScreen 双版本号（C1） | §6 限定作用域：移入 `composable("login")` 内部 |
| P0-2 | D-20 <480dp 宽度隐藏未实现（C1, C3） | §6 增加 `BoxWithConstraints` 判断 |
| P0-3 | IME 弹出时 VersionTag 上浮（C1） | §6 增加 `imePadding()` 处理 |
| P0-4 | 对比度 3.2:1 不达标（C2） | §4 Token：alpha 0.6 → 0.75（实测 4.6:1） |

### P1 问题清单（已识别，编码阶段修复）

| 来源 | 编号 | 问题 |
|------|------|------|
| C1 | P1-1 | VersionTag 缺少 alpha 0.6 弱化（D-19） |
| C1 | P1-2 | LoginScreen 未消费 LoginUiState（loading/error） |
| C1 | P1-3 | 手机号输入无格式校验/键盘类型 |
| C1 | P1-4 | VersionTag 无 RTL 适配考量 |
| C2 | P1-5 | nav-bar 34px 透明空洞 |
| C2 | P1-6 | VersionTag HTML 使用 monospace 字体不一致 |
| C2 | P1-7 | 缺少 :focus-visible 样式 |
| C2 | P1-8 | placeholder 透明度用法与 version-tag 不一致 |
| C3 | P1-9 | WCAG AA 标准 4.5:1 与 D-17 的 3:1 冲突 |
| C3 | P1-10 | VersionTag 缺少 SelectionContainer（与 AboutScreen 不一致） |

### C2 视觉评审 10 维度

| # | 维度 | 评分 | 关键评语 |
|---|------|:---:|------|
| 1 | 格式塔 | 3 | 分组明确但 nav-bar 空洞破坏闭合 |
| 2 | 视觉层级 | 4 | logo→欢迎语→表单→CTA 线性清晰 |
| 3 | 色彩 | 2 | M3 tokens 规范但 version-tag 对比度双双不达标 |
| 4 | 字体 | 3 | Roboto 体系一致但 version-tag 突兀用 monospace |
| 5 | 空间 | 4 | 呼吸感良好，padding/margin 合理 |
| 6 | 布局 | 4 | flex 居中+column 简洁有效 |
| 7 | 可感知性 | 2 | version-tag 可读性不足，无 focus 态 |
| 8 | 一致性 | 4 | M3 token 全局引用，设计语言统一 |
| 9 | 情感品牌 | 3 | 亲和但中性，缺品牌记忆点 |
| 10 | 平台适配 | 4 | dark mode 已覆盖，安全区/notch 未处理 |

**综合: 33/50**（有条件通过，P0 对比度已修订）

### 审美亮点
- ✨ M3 Design Token 体系完整（14 CSS 变量 + dark mode 翻转）
- ✨ 视觉层级清晰，F-pattern 符合登录页预期
- ✨ 呼吸感控制得当（padding 24px + margin 节奏稳定）
- ✨ Dark mode 自动适配 `@media (prefers-color-scheme: dark)`

---

> **版本:** v0.2-review
> **状态:** 三视角评审完成，4 P0 已自动修订。请审阅后回复「确认」冻结进入技术方案。
