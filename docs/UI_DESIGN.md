# 关于页面版本号显示 — UI 设计方案

> **版本:** v0.1-draft
> **功能名称:** 关于页面版本号显示
> **创建日期:** 2026-06-02
> **基于:** PRD v1.0-confirmed §9 | DECISIONS.md R2-D-22~R2-D-26

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
└── 关于 (TextButton, Modifier.align(BottomCenter))
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
│                                  │
│                                  │
│                                  │
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
│       ├── Icon/Image  (48×48dp, rounded 12dp)
│       ├── Spacer(16dp)
│       ├── Text(appName, titleLarge)
│       ├── Spacer(16dp)
│       ├── HorizontalDivider  (outlineVariant)
│       ├── Spacer(16dp)
│       ├── Row("版本号" label)
│       └── Text(formatVersionTag(), bodyMedium)
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
│  版本未知    │ ← alpha=1.0, 不可点击
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

> **状态:** 待评审 (v0.1-draft) — 三视角评审后将升级为 v0.2-review。
