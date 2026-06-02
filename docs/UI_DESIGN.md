# 关于页面增加版本号显示功能 — UI 设计方案

> **版本:** v0.1-draft
> **功能名称:** 关于页面增加版本号显示功能
> **创建日期:** 2026-06-02
> **基于:** PRD v1.0-confirmed §9 | DECISIONS.md

---

## §1 设计总览

### 设计目标
- 在「关于」页面以清晰、轻量的方式展示版本号信息
- 版本号格式 `v{name}({code}){buildType}`，统一管理
- 仅展示，无交互（对齐 D-24）

### 设计语言
- Material3 (M3) 设计系统
- 375dp 基准视口
- 颜色 Token: onSurfaceVariant + opacity 0.6 (D-13 / D-19)

---

## §2 页面清单与导航

| 页面 | 路由 | 类型 | 入口 | 说明 |
|------|------|------|------|------|
| AboutScreen | `/about` | 新建 | 登录页「关于」TextButton | 展示应用信息 + 版本号 |

### 导航图
```
NavHost(startDestination = "login")
├── composable("login")  → LoginScreen
└── composable("about")  → AboutScreen
```

---

## §3 页面设计 — AboutScreen

### 线框图
```
┌──────────────────────────────────┐
│                                  │
│           ← 返回                  │
│                                  │
│           📱 应用图标              │
│                                  │
│         应用名称                   │
│         应用简述                   │
│                                  │
│     ─────────────────────         │
│                                  │
│     v1.0.0(42)release            │  ← 14sp, onSurfaceVariant, 0.6 opacity
│                                  │
│     ─────────────────────         │
│                                  │
│     [关于我们]                     │
│     [用户协议]                     │
│     [隐私政策]                     │
│     [开源许可]                     │
│                                  │
└──────────────────────────────────┘
```

### 组件层级树
```
AboutScreen
└── Scaffold
    ├── topBar: TopAppBar (title="关于", navigationIcon=← back)
    └── content: Column
        ├── Spacer(32dp)
        ├── AppIcon (Image, 80×80dp)
        ├── Spacer(16dp)
        ├── AppName (Text, headlineMedium, center)
        ├── Spacer(4dp)
        ├── AppDescription (Text, bodyMedium, onSurfaceVariant)
        ├── Spacer(24dp)
        ├── Divider
        ├── Spacer(16dp)
        ├── VersionText (Text, 14sp, onSurfaceVariant, opacity 0.6)
        ├── Spacer(16dp)
        ├── Divider
        ├── Spacer(16dp)
        ├── InfoItem("关于我们")
        ├── InfoItem("用户协议")
        ├── InfoItem("隐私政策")
        └── InfoItem("开源许可")
```

### 交互状态机
```
┌─────────────┐
│    Idle     │ ← 进入页面 → PackageManager 读取版本信息
└──────┬──────┘
       │ 版本信息获取成功
       ▼
┌─────────────┐
│   Display   │ ← 显示格式化版本号 `v{name}({code}){buildType}`
└─────────────┘
       │ 版本信息获取失败
       ▼
┌─────────────┐
│   Unknown   │ ← 显示 "版本未知"（降级态）
└─────────────┘
```

### 状态覆盖表

| 状态 | UI 表现 | 说明 |
|------|---------|------|
| 默认（Display） | 显示 `v1.0.0(42)release` | 版本信息正常读取 |
| 降级（Unknown） | 显示 "版本未知" | PackageManager 异常时 |
| 深色模式 | onSurfaceVariant 自动切换暗色值 | M3 Theme 自动适配 |

---

## §4 Token 映射表

| 设计属性 | M3 Token | 值 |
|----------|----------|-----|
| 页面背景 | `background` | #FFFBFC / #1C1B1F |
| 应用名称 | `onSurface` headlineMedium | #1C1B1F / #E6E1E5 |
| 版本号文字 | `onSurfaceVariant` bodySmall | #49454F / #CAC4D0 |
| 分隔线 | `outlineVariant` | #CAC4D0 / #938F99 |
| 返回图标 | `onSurface` | #1C1B1F / #E6E1E5 |
| 信息项文字 | `onSurface` bodyLarge | #1C1B1F / #E6E1E5 |

---

## §5 组件复用分析

| 组件 | 来源 | 复用方式 | 状态 |
|------|------|----------|------|
| TopAppBar | M3 内置 | 直接使用 | ✅ |
| AppIcon | 项目已有 | 直接复用 | ✅ |
| InfoItem | 新组件 | 新增 — 图标+文本列表项 | ❌ 低复杂度 |
| VersionText | 新组件 | 新增 — 格式化版本号文本 | ❌ 低复杂度 |

### 新增文件

| 文件 | 说明 | 复杂度 |
|------|------|--------|
| AboutScreen.kt | 关于页面 Composable | 低 |
| AboutViewModel.kt | 版本信息获取逻辑 | 低 |
| InfoItem.kt | 信息行复用组件（图标+文字+箭头） | 低 |
| VersionText.kt | 版本号展示组件 | 低 |

---

## §6 架构协调设计

### 导航事件
```
LoginScreen."关于"点击 → navController.navigate("about")
AboutScreen ← 返回 → navController.popBackStack()
```

### ViewModel
不需要独立 ViewModel。版本信息为静态数据，通过 `LocalContext.current.packageManager` 一次性读取即可。使用 `remember` + `LaunchedEffect(Unit)` 在 Composable 内完成。

### BackHandler
AboutScreen 无特殊 BackHandler 需求，系统返回键按导航栈 popBackStack 即可。

---

## §7 无障碍适配

| 元素 | contentDescription | 触控目标 |
|------|-------------------|----------|
| 返回按钮 | "返回" | ≥48dp |
| 应用图标 | "应用图标" | ≥48dp |
| 版本号文字 | "当前版本 v{name}({code}){buildType}" | ≥48dp |
| 信息项 | 对应文本内容 | ≥48dp |
| 分隔线 | null（装饰性） | — |

---

## §8 前置依赖

| 依赖 | 说明 | 状态 |
|------|------|------|
| Material3 Theme | Color.kt / Type.kt / Theme.kt | ⚠️ 编码阶段完成 |
| PackageManager | 系统 API，无需额外依赖 | ✅ 内置 |

---

> **版本:** v0.1-draft
> **状态:** 待评审 — 3-Agent 评审后升级 v0.2-review
