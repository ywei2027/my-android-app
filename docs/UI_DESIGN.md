# 关于页面版本号显示 — UI 设计方案

> **版本:** v0.2-review
> **功能名称:** 关于页面版本号显示
> **创建日期:** 2026-06-02
> **基于:** PRD v1.0-confirmed §9 | DECISIONS.md R2-D-22~R2-D-26 | C2 视觉 41/50

---

## §1 设计总览

### 设计目标
- 登录页底部「关于」入口 → 导航至关于页面
- 关于页面展示应用图标、名称、版本号等基本信息
- 所有构建类型（Debug/Release）均可见

### 设计语言
- Material3 (M3) 设计系统
- 375dp 基准视口
- 颜色 Token：`onSurfaceVariant` + alpha 0.6

---

## §2 页面清单与导航

| 页面 | 路由 | 类型 | 入口 | 说明 |
|------|------|------|------|------|
| 登录页（入口改造） | `/login` | 改造 | 启动默认 | 底部增加「关于」TextButton |
| 关于页面 | `/about` | 新建 | 登录页「关于」按钮 | 展示应用信息+版本号 |

### 导航图
```
NavHost(startDestination = "login")
├── composable("login")  → LoginScreen（底部含「关于」按钮）
└── composable("about")  → AboutScreen（TopAppBar + 内容）
```

---

## §3 登录页入口改造

### 线框图
```
┌──────────────────────────────────┐
│                                  │
│          📱 应用图标              │
│          应用名称                 │
│                                  │
│  ┌────────────────────────────┐  │
│  │  手机号                      │  │
│  └────────────────────────────┘  │
│  [        获取验证码        ]   │
│                                  │
│                                  │
│           关于                   │  ← TextButton, onSurfaceVariant
└──────────────────────────────────┘
```

### 组件层级
```
LoginScreen (Column, fillMaxSize)
├── 应用图标 + 名称 (Column, center)
├── 手机号输入框 (TextField)
├── 获取验证码按钮 (Button)
└── 关于 (TextButton, Modifier.align(BottomCenter)
    .defaultMinSize(minHeight=48.dp, minWidth=48.dp))
    └── onClick → navController.navigate("about")
```

---

## §4 关于页面设计

### 线框图
```
┌──────────────────────────────────┐
│  ←  关于                          │  ← CenterAlignedTopAppBar
├──────────────────────────────────┤
│                                  │
│           [应用图标]              │  ← 48×48dp, 圆角12dp
│          我的应用                 │  ← titleLarge 22sp
│          ─────────               │  ← HorizontalDivider
│          版本号                   │  ← label
│  v1.0.0(42)debug                 │  ← bodyMedium 14sp
│                                  │      onSurfaceVariant, alpha=0.6
│                                  │      maxWidth=240dp, maxLines=1
│ ← 底部 8dp + WindowInsets        │
└──────────────────────────────────┘
```

### 组件层级树
```
AboutScreen
├── Scaffold
│   ├── topBar: CenterAlignedTopAppBar
│   │   ├── title: Text("关于")
│   │   └── navigationIcon: IconButton(ArrowBack)
│   │       └── onClick → navController.popBackStack()
│   └── content: Column (center, padding 16dp)
│       ├── Icon/Image  (48×48dp, rounded 12dp, contentDescription="应用图标")
│       ├── Spacer(16dp)
│       ├── Text(appName, titleLarge)
│       ├── Spacer(16dp)
│       ├── HorizontalDivider  (outlineVariant, importantForAccessibility=no)
│       ├── Spacer(16dp)
│       ├── Row("版本号" label)
│       └── Text(formatVersionTag(), bodyMedium,
│           color=onSurfaceVariant.copy(alpha=0.6f),
│           maxLines=1, overflow=Ellipsis,
│           modifier=Modifier.widthIn(max=240.dp)
│               .windowInsetsPadding(WindowInsets.navigationBars)
│               .padding(bottom=8.dp)
│               .semantics { contentDescription = formatVersionDescription() })
```

---

## §5 交互状态机

```
┌─────────────┐
│    Idle     │ ← 进入页面
│  显示正常    │
└──────┬──────┘
       │ BuildConfig 异常
       ▼
┌─────────────┐
│  Fallback    │
│  版本未知    │ ← alpha=1.0, 不可点击,
│              │    contentDescription="版本信息暂时不可用"
└─────────────┘
```

### 状态覆盖表

| 状态 | 图标 | 名称 | 版本号 | 说明 |
|------|------|------|--------|------|
| 默认 | ✅ 应用图标 | ✅ 应用名称 | ✅ v{name}({code}){buildType} | BuildConfig 正常 |
| 降级 | ✅ 应用图标 | ✅ 应用名称 | 版本未知 | BuildConfig 字段为空/null |

---

## §6 Token 映射表

| 设计属性 | M3 Token | 值 |
|----------|----------|-----|
| 页面背景 | `background` | light: #FFFBFE / dark: #1C1B1F |
| TopAppBar 标题色 | `onSurface` | light: #1C1B1F / dark: #E6E1E5 |
| TopAppBar 背景 | `surface` | light: #FFFBFE / dark: #1C1B1F |
| 标题文字色 | `onSurface` | light: #1C1B1F / dark: #E6E1E5 |
| 版本号文字色 | `onSurfaceVariant` × 0.6 | light: #49454F / dark: #CAC4D0 |
| 分隔线 | `outlineVariant` | light: #CAC4D0 / dark: #49454F |
| 返回箭头 | `onSurface` | light: #1C1B1F / dark: #E6E1E5 |

---

## §7 组件复用分析

| 组件 | 来源 | 复用方式 | 状态 |
|------|------|----------|------|
| `formatVersionTag()` | `ui/components/VersionTag.kt` | 直接调用 | ✅ 复用 |
| `formatVersionDescription()` | `ui/components/VersionTag.kt` | 直接调用 | ✅ 复用 |
| `VersionTag` Composable | `ui/components/VersionTag.kt` | ❌ 不复用 | Debug-only 限制与需求冲突 |
| `Scaffold` | material3 | 直接使用 | ✅ 标准组件 |
| `CenterAlignedTopAppBar` | material3 | 直接使用 | ✅ 标准组件 |

### 新增文件

| 文件 | 说明 | 复杂度 |
|------|------|--------|
| `ui/about/AboutScreen.kt` | 关于页面 Composable | 低 |
| 导航修改 `MainActivity.kt` | 接入 NavHost | 中 |

---

## §8 架构协调设计

### 导航事件
```
LoginScreen "关于" onClick → navController.navigate("about")
AboutScreen 返回箭头 → navController.popBackStack()
```

### ViewModel（不需要）
关于页面为纯静态展示页面，无业务逻辑，不需要 ViewModel。

### BackHandler
- 登录页：无特殊处理（默认行为）
- 关于页面：TopAppBar 返回箭头 + 系统返回手势 → popBackStack()

---

## §9 无障碍适配

| 元素 | contentDescription | 触控目标 |
|------|-------------------|----------|
| 返回按钮 | 「返回」 | ≥48dp |
| 应用图标 | 「应用图标」 | ≥48dp |
| 版本号 | 「应用版本号 v{name}，版本代码 {code}，构建类型 {buildType}」 | 静态文本 |
| 「关于」按钮 | 「关于此应用」 | ≥48dp |

---

## §10 前置依赖

| 依赖 | 说明 | 状态 |
|------|------|------|
| darkColorScheme | D-18 决议，需在 Theme.kt 中补建 | ⚠️ 编码阶段一并完成 |
| NavHost | navigation-compose 已引入，需在 MainActivity 接线 | ⚠️ 编码阶段一并完成 |

---

## §11 多视角评审记录

> 评审日期: 2026-06-02 | 方式: delegate_task 三视角并行 | 耗时: ~120s

### 评审总览

| 视角 | 评分 | P0 | P1 | 核心发现 |
|------|------|----|----|----------|
| C1 UX 交互 | 6/10 | 4 | 8 | 下边距/WindowInsets 缺失、maxWidth 未约束、Fallback 缺 contentDescription |
| C2 视觉审美 | 41/50 ✅ | 0 | 2 | 通过(≥40)，跨文件一致性 3/5 需关注 |
| C3 前端实现 | 4.2/5 | 2 | 4 | darkColorScheme+NavHost 前置依赖、工时 1.75-2h |

### P0 修订记录（已在正文自动修订）

| 编号 | 问题 | 修订内容 |
|------|------|----------|
| P0-UX-01 | 下边距+WindowInsets 缺失 | §4 线框图+组件树增加 padding(bottom=8.dp)+windowInsetsPadding |
| P0-UX-02 | maxWidth 240dp 未约束 | §4 组件树增加 widthIn(max=240.dp) |
| P0-UX-03 | Fallback 缺 contentDescription | §5 状态机增加"版本信息暂时不可用" |
| P0-UX-04 | 按钮触摸目标未声明 | §3 组件树增加 defaultMinSize(48.dp) |
| P0-IMPL-01 | darkColorScheme 未建 | §10 前置依赖纳管，编码阶段补建 |
| P0-IMPL-02 | NavHost 未接线 | §10 前置依赖纳管，编码阶段一并实现 |

### C2 视觉评审 10 维度

| # | 维度 | 评分 | 关键评语 |
|---|------|:---:|------|
| 1 | 格式塔 | 4 | 垂直流清晰，信息密度一致 |
| 2 | 视觉层级 | 4 | 四级递减合理(22sp→14sp→opacity 0.6) |
| 3 | 色彩 | 5 | M3 Token 完整，对比度 ≥3:1 ✅ |
| 4 | 字体 | 4 | M3 type scale + Noto Sans SC |
| 5 | 空间 | 4 | 48px padding-top, 8dp 底部网格对齐 |
| 6 | 布局 | 4 | 375×812 手机模拟，flex column 居中 |
| 7 | 可感知性 | 5 | aria-label 完整，≥48dp 触控 |
| 8 | 一致性 | 3 | 跨文件细节差异（字号/字重/Token 命名） |
| 9 | 情感品牌 | 4 | 紫色调专业温暖，品牌辨识度可提升 |
| 10 | 平台适配 | 4 | 深浅色+safe-area+窄屏 ✅ |

**综合: 41/50** — 基于源码推断（快速通道无截图）

---

> **版本:** v0.2-review
> **状态:** 三视角评审完成，P0 已自动修订。请审阅后回复「确认」冻结进入技术方案。
