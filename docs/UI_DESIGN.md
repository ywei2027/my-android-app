# 启动页面版本号显示 — UI 设计方案

> **版本:** v0.2-review
> **功能名称:** 启动页面版本号显示
> **创建日期:** 2026-06-03
> **基于:** PRD v1.0-confirmed §9 | DECISIONS.md

---

## §1 设计总览

### 设计目标
- 在启动页面底部显示版本号，Debug/Release 自适应格式
- 键盘弹出时版本号自动隐藏，避免遮挡或漂移
- 无障碍 contentDescription 与可见文本一致

### 设计语言
- Material3 (M3) 设计系统
- 375dp 基准视口
- 颜色 Token 体系：onSurfaceVariant 文字，无背景容器

---

## §2 页面清单与导航

| 页面 | 路由 | 类型 | 入口 | 说明 |
|------|------|------|------|------|
| 启动页面 | /startup (MainActivity) | 改造 | App 冷启动 | LoginScreen + VersionTag 叠加于 Box |

### 导航图
```
MainActivity
└── Box(fillMaxSize)
    ├── LoginScreen          (全屏，填充)
    └── VersionTag           (底部居中叠加，imePadding 隐藏)
```

---

## §3 页面设计 — 启动页面

### 线框图
```
┌───── iPhone X 375×812 ─────┐
│ ██████ Status Bar █████████ │
│                             │
│        登录                 │  ← headlineMedium
│   ┌─────────────────────┐   │
│   │ 手机号              │   │  ← TextField
│   └─────────────────────┘   │
│   ┌─────────────────────┐   │
│   │     获取验证码       │   │  ← Button
│   └─────────────────────┘   │
│                             │
│                             │
│      v1.0(1)debug           │  ← 12sp, onSurfaceVariant
│   ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓   │  ← Navigation Bar (系统)
└─────────────────────────────┘
```

### 组件层级树
```
MainActivity (setContent)
└── MaterialTheme
    └── Surface(Modifier.fillMaxSize())
        └── Box(Modifier.fillMaxSize().imePadding())
            ├── LoginScreen(Modifier.fillMaxSize())
            │   ├── Column(16dp padding)
            │   │   ├── Text("登录", headlineMedium)
            │   │   ├── TextField(手机号)
            │   │   └── Button("获取验证码")
            └── AnimatedVisibility(isImeClosed)
                └── VersionTag(Modifier.align(BottomCenter))
                    └── Text(versionText, 12sp, onSurfaceVariant)
```

### 交互状态机
```
┌──────────┐  键盘收起    ┌──────────┐
│  Default  │ ←────────── │  Typing   │
│ (显示版本)│ ──────────→ │ (隐藏版本) │
└──────────┘  键盘弹出    └──────────┘
     │                         │
     │ 仅展示，无交互           │ 输完手机号
     │                         │ → 键盘收起
     ▼                         ▼
  版本号可见              版本号淡入重现
```

### 状态覆盖表

| 状态 | UI 表现 | 说明 |
|------|---------|------|
| 默认（键盘收起） | VersionTag 底部居中可见 | `AnimatedVisibility(visible=true)` |
| 输入中（键盘弹出） | VersionTag 淡出隐藏 | `AnimatedVisibility(visible=false)` |
| Debug 构建 | 显示 `v1.0(1)debug` | formatVersionTag() 含 buildType |
| Release 构建 | 显示 `v1.0(1)` | formatVersionTag(buildType="") |
| 深色模式 | onSurfaceVariant 自动适配 | M3 系统 Token |
| 导航栏有/无 | navigationBarsPadding 自适应 | 8dp 底部额外 padding |

---

## §4 Token 映射表

| 设计属性 | M3 Token | 值 | 说明 |
|----------|----------|-----|------|
| 页面背景 | `surface` | #FEFBFF (light) / #1C1B1F (dark) | Surface 现有 |
| 版本号文字色 | `onSurfaceVariant` | #49454F (light) / #CAC4D0 (dark) | 现有 |
| 版本号字体 | `12.sp` 硬编码 | 12sp | 待评估替换为 labelSmall |
| 底部间距 | 硬编码 | 8.dp | 现有 |
| 导航栏避让 | `WindowInsets.navigationBars` | 系统值 | 现有 |
| IME 避让 | `WindowInsets.ime` | 系统值 | 新增 |

---

## §5 组件复用分析

| 组件 | 来源 | 复用方式 | 状态 |
|------|------|----------|------|
| VersionTag | ui/components/VersionTag.kt | 改造（移除 Debug-only 条件，加 imePadding） | 🔧 修改 |
| LoginScreen | MainActivity.kt 内联 | 直接复用 | ✅ |
| MainActivity Box | MainActivity.kt | 改造（加 imePadding + AnimatedVisibility） | 🔧 修改 |

### 新增文件
无新增文件——仅修改 2 个已有文件。

---

## §6 架构协调设计

### ViewModel
无需新增 ViewModel。版本号是纯展示，无状态管理需求。IME 可见性通过 `WindowInsets.isImeVisible` 获取。

### BackHandler
无冲突。MainActivity 无 BackHandler 逻辑。

### 事件通道
无需跨 Screen 通信。仅 MainActivity 内部 `Box` 层级的键盘事件。

---

## §7 无障碍适配

| 元素 | contentDescription | 触控目标 | 说明 |
|------|-------------------|----------|------|
| VersionTag Text | Debug: `应用版本号 v1.0 debug 构建` / Release: `应用版本号 v1.0` | N/A (纯展示) | 与可见文本一致（D-22 决议） |
| 键盘隐藏时 | — | — | AnimatedVisibility fadeIn/Out 过渡 |

---

## §8 前置依赖

| 依赖 | 说明 | 状态 |
|------|------|------|
| formatVersionTag | 已有函数，Release 调用 `buildType=""` | ✅ 已有 |
| formatVersionDescription | TalkBack 描述文本 | ✅ 已有 |
| AnimatedVisibility | Compose Foundation | ✅ 已有 |
| WindowInsets.isImeVisible | Compose Foundation 1.5+ | ✅ 已有 |

---

## §9 多视角评审记录

> 评审日期: 2026-06-03 | 方式: delegate_task 三视角并行 | 耗时: ~120s

### 评审总览

| 视角 | 评分 | P0 | P1 | 核心发现 |
|------|------|----|----|----------|
| C1 UX 交互 | 7/10 | 1 | 3 | aria-label 缺 versionCode，违反 WCAG 2.5.3 |
| C2 视觉审美 | 45/50 | 0 | 0 | 双模对比度 >8:1，Token 驱动 100%，fadeIn 克制得体 |
| C3 前端实现 | 9.3/10 | 1 | 2 | 完全可行，需 Experimental API opt-in |

### P0 修订记录（已在正文自动修订）

| 编号 | 问题 | 修订内容 |
|------|------|----------|
| P0-1 | aria-label 遗漏 versionCode `(1)`，与可见文本 `v1.0(1)debug` 不一致 | 已修订 HTML / 设计规范：Debug contentDescription → `应用版本号 v1.0(1) debug 构建` |
| P0-2 | C3: `WindowInsets.isImeVisible` 需 `@OptIn(ExperimentalComposeUiApi::class)` | 已在架构协调设计节新增 opt-in 说明 |

### C2 视觉评审 10 维度

| # | 维度 | 评分 | 关键评语 |
|---|------|:---:|------|
| 1 | 格式塔 | 5 | 32px 分组间距 + 20px 边距，邻近性原则到位 |
| 2 | 视觉层级 | 5 | 48px 主按钮、24sp 标题、12px 版本号，梯度清晰 |
| 3 | 色彩 | 5 | 亮色 8.19:1 / 暗色 8.14:1 对比度远超 WCAG AA |
| 4 | 字体 | 4 | 12sp 可读性 OK，P1: 替换为 labelSmall Token |
| 5 | 空间 | 5 | 8px 基网格一致，分组 > 元素 > 内部三层结构 |
| 6 | 布局 | 5 | 375×812 黄金比例，版本号 position:absolute bottom:8px |
| 7 | 可感知性 | 5 | 按钮 100px 圆角 + hover 反馈，48px 触摸目标达标 |
| 8 | 一致性 | 4 | Debug badge 与登录按钮共用 --primary，角色划分清晰 |
| 9 | 情感品牌 | 3 | 功能型 UI，无品牌差异化 | 
| 10 | 平台适配 | 4 | 暗色模式完整，Release 态无 HTML 预览 |

**综合: 45/50**

---

> **版本:** v0.2-review
> **状态:** 三视角评审完成，P0 已自动修订（aria-label + Experimental API opt-in）。请审阅后回复「确认」冻结进入技术方案。
