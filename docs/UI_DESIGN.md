# 启动页面增加版本号显示功能 — UI 设计方案

> **版本:** v0.1-draft
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
| 视觉层级 | alpha 0.6 | 次级弱化 (D-19) |

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
无新增。VersionTag 作为 LoginScreen 的 Box 同层组件，不参与导航。

### ViewModel
不需要 ViewModel。VersionTag 为纯 Composable，直接从 BuildConfig 读取编译时常量。

### BackHandler
无影响。VersionTag 不消费触摸事件，不影响 LoginScreen 的 BackHandler。

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

> 评审日期: 2026-06-03 | 方式: delegate_task 三视角并行

### 评审总览

| 视角 | 评分 | P0 | P1 | 核心发现 |
|------|------|----|----|----------|
| C1 UX 交互 | | | | |
| C2 视觉审美 | | | | |
| C3 前端实现 | | | | |

### P0 修订记录（已在正文自动修订）

| 编号 | 问题 | 修订内容 |
|------|------|----------|
| | | |

### C2 视觉评审 10 维度

| # | 维度 | 评分 | 关键评语 |
|---|------|:---:|------|
| 1 | 格式塔 | | |
| 2 | 视觉层级 | | |
| 3 | 色彩 | | |
| 4 | 字体 | | |
| 5 | 空间 | | |
| 6 | 布局 | | |
| 7 | 可感知性 | | |
| 8 | 一致性 | | |
| 9 | 情感品牌 | | |
| 10 | 平台适配 | | |

**综合: X/50**

---

> **版本:** v0.2-review
> **状态:** 三视角评审完成，P0 已自动修订。请审阅后回复「确认」冻结进入技术方案。
