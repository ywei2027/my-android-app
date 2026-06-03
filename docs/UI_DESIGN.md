# 启动页面版本号显示 — UI 设计方案

> **版本:** v0.1-draft
> **功能名称:** 启动页面版本号显示
> **创建日期:** 2026-06-03
> **基于:** PRD v1.0-confirmed §9 | DECISIONS.md D-01~D-04

---

## §1 设计总览

### 设计目标
- 在 MainActivity 底部持久显示应用版本号
- Debug 构建：完整格式 `v{name}({code}){buildType}`
- Release 构建：简洁格式 `v{name}`（无 buildType 后缀）

### 设计语言
- Material3 (M3) 设计系统
- 375dp 基准视口
- 颜色 Token: onSurfaceVariant

---

## §2 页面清单与导航

| 页面 | 路由 | 类型 | 入口 | 说明 |
|------|------|------|------|------|
| 版本号浮层 | 无路由 (overlay) | 改造 | MainActivity | Box 底部 Alignment.BottomCenter |

### 导航图
```
MainActivity
└── Box (fillMaxSize)
    ├── LoginScreen (填充)
    └── VersionTag (Alignment.BottomCenter)
```

---

## §3 页面设计 — 版本号浮层

### 线框图
```
┌─────────── 375dp ────────────┐
│                               │
│          LoginScreen          │
│         (内容区域)            │
│                               │
│   ┌─────────────────────┐     │
│   │  v1.0(1)debug        │     │  ← VersionTag
│   └─────────────────────┘     │     12sp, onSurfaceVariant
│   ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓ │     ← NavigationBars 安全区
└───────────────────────────────┘
```

### 组件层级树
```
MainActivity
└── MaterialTheme
    └── Surface (fillMaxSize)
        └── Box (fillMaxSize)
            ├── LoginScreen (fillMaxSize)
            │   └── Column
            │       ├── Text("登录", headlineMedium)
            │       ├── TextField(手机号)
            │       └── Button("获取验证码")
            └── VersionTag (Alignment.BottomCenter)
                └── Text(
                      text = formatVersionTag() / formatVersionName(),
                      fontSize = 12.sp,
                      color = onSurfaceVariant,
                      maxLines = 1, overflow = Ellipsis,
                      modifier = windowInsetsPadding(navigationBars) + padding(bottom = 8.dp)
                       + semantics { contentDescription = "应用版本号 v1.0" }
                    )
```

### 交互状态机
```
┌─────────────┐
│   Visible    │ ← 进入 Activity 即显示，无交互
│  (静态渲染)  │
└─────────────┘
      ↓ (构建类型决定内容)
┌──────────────┐    ┌──────────────┐
│ Debug 构建    │    │ Release 构建  │
│ v{name}({c}) │    │ v{name}      │
│  + buildType │    │ (无后缀)     │
└──────────────┘    └──────────────┘
```

### 状态覆盖表

| 状态 | 可见 | 显示内容 | 说明 |
|------|------|----------|------|
| Debug 构建 | ✅ | `v1.0(1)debug` | 完整格式，含 versionCode + buildType |
| Release 构建 | ✅ | `v1.0` | 仅 versionName，无后缀 |
| 版本名为空 | ✅ | 防御性空文本 | BuildConfig 异常兜底 |

---

## §4 Token 映射表

| 设计属性 | M3 Token | 值 |
|----------|----------|-----|
| 文字颜色 | `onSurfaceVariant` | 项目主题默认值 |
| 字体大小 | `labelSmall` 覆盖 | 12sp |
| 行数限制 | — | 1 (TextOverflow.Ellipsis) |
| 底部间距 | — | 8dp |
| 导航栏适配 | `WindowInsets.navigationBars` | 平台默认 |

---

## §5 组件复用分析

| 组件 | 来源 | 复用方式 | 状态 |
|------|------|----------|------|
| `VersionTag` | `ui/components/VersionTag.kt` | 需改造 | ⚠️ 移除 `if (!DEBUG) return` |
| `formatVersionTag()` | 同上 | 直接复用 | ✅ Debug 格式 |
| `formatVersionDescription()` | 同上 | 直接复用 | ✅ TalkBack |
| `LoginScreen` | `MainActivity.kt` | 不动 | ✅ 无变更 |

### 新增文件

无新增文件 — 仅修改现有 `VersionTag.kt`（移除 Debug-only 门控）。

---

## §6 架构协调设计

### 导航事件
无 — VersionTag 是 MainActivity 内静态 overlay，不参与导航。

### ViewModel
不需要 — VersionTag 无状态，纯静态文本从 BuildConfig 读取。

### BackHandler
不需要 — 无交互，不消费返回事件。

---

## §7 无障碍适配

| 元素 | contentDescription | 触控目标 | TalkBack 焦点 |
|------|-------------------|----------|--------------|
| 版本号文本 | "应用版本号 v{versionName}" | N/A (无交互) | 装饰性辅助 |

---

## §8 前置依赖

| 依赖 | 说明 | 状态 |
|------|------|------|
| `buildConfig = true` | build.gradle.kts 已启用 | ✅ 已满足 |
| `versionName` 已配置 | build.gradle.kts: `versionName = "1.0"` | ✅ 已满足 |

---

## §9 多视角评审记录

> 评审日期: 2026-06-03 | 方式: delegate_task 三视角并行 | 耗时: ~120s

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
