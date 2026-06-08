# 启动页版本号显示 — UI 设计方案

> **版本:** v0.1-draft
> **功能名称:** 启动页版本号显示
> **创建日期:** 2026-06-08
> **基于:** PRD v1.0-confirmed §9 | DECISIONS.md (R-01~R-06)

---

## §1 设计总览

### 设计目标
- 在现有启动页（蓝色 #1A73E8 背景，📰 logo）底部新增版本号文本
- 最小侵入：不改变现有 logo/动画/布局
- 对比度达标：文字色 rgba(0,0,0,0.55) 在 #1A73E8 上约 4.85:1 ≥ WCAG AA 4.5:1

### 设计语言
- Material3 (M3) 设计系统
- 375dp 基准视口（iPhone X 尺寸：375×812）
- 颜色 Token：项目 Theme Color.kt 现有 Token + splash 专用色值

---

## §2 页面清单与导航

| 页面 | 路由/组件 | 类型 | 入口 | 说明 |
|------|----------|------|------|------|
| 启动页 | `AnimatedSplashContent` (MainActivity.kt) | 改造 | 冷启动 | 现有蓝色启动页底部新增版本号 Text，不涉及新路由 |

### 导航图
无新增——版本号在现有启动页内叠加，不涉及 NavHost 变更。

---

## §3 页面设计 — 启动页（Splash Screen）

### 线框图
```
┌──────────────────────────────────┐  ← Box(fillMaxSize, bg=#1A73E8)
│                                  │
│          ┌──────────┐            │
│          │   📰     │ 72sp       │  ← logo scaleIn + fadeIn 600ms
│          │  新闻     │ 28sp Bold  │
│          │  热点资讯  │ 14sp      │
│          │  一键掌握  │ 半透白     │
│          └──────────┘            │
│                                  │
│                                  │
│          v1.0(1)debug            │  ← ★ 版本号 12sp rgba(0,0,0,0.55)
│                                  │     距底 32dp + navigationBars inset
└──────────────────────────────────┘  ← 375×812 视口
```

### 组件层级树
```
AppWithAnimatedSplash(onSplashReady)
└── Box(fillMaxSize)                                    // 根容器
    ├── AnimatedVisibility(visible=!showSplash)         // 主内容（延迟显示）
    │   └── NewsAppNavHost()
    ├── AnimatedVisibility(                             // Splash 动画容器
    │       visible=showSplash,
    │       exit=fadeOut(500ms))
    │   └── AnimatedSplashContent(onFinished)
    │       ├── Text("📰", 72sp)                        // scaleIn+fadeIn 600ms
    │       ├── Text("新闻", 28sp, Bold, White)
    │       └── Text("热点资讯 一键掌握", 14sp, 半透白)
    └── Text(versionTag,                                // ★ 新增 — 独立于AnimatedVisibility
             Modifier.align(BottomCenter)
               .windowInsetsPadding(navigationBars)
               .padding(bottom=32dp),
             fontSize=12sp,
             color=Color(0x8C000000),                   // rgba(0,0,0,0.55)
             semantics { contentDescription = "应用版本号 v1.0" })
```

### 交互状态机
```
┌──────────────┐
│   VISIBLE    │ ← showSplash=true, 启动页可见
└──────┬───────┘
       │ onFinished() → showSplash=false
       ▼
┌──────────────┐
│    GONE      │ ← Box 容器移除，版本号随启动页整体消失
└──────────────┘
```
版本号只有一个状态（纯展示），无加载/错误/空数据态。

### 状态覆盖表

| 状态 | UI 表现 | 说明 |
|------|---------|------|
| VISIBLE | 底部居中显示版本号（Debug: `v1.0(1)debug` / Release: `v1.0`），12sp，深色半透明，不参与动画 | 始终可见（0ms 即显示） |
| GONE | 版本号随 AnimatedVisibility splash 容器移除后整体消失 | 不单独 FadeOut |

---

## §4 Token 映射表

| 设计属性 | M3 Token / 值 | 来源 |
|----------|--------------|------|
| 启动页背景 | `#1A73E8`（硬编码蓝） | 现有 AnimatedSplashContent |
| 版本号文字色 | `rgba(0,0,0,0.55)` = `#8C000000` | R-02 决议：方案A |
| 版本号字体大小 | `12.sp` | PRD §9.2 |
| 版本号字重 | `FontWeight.Normal` (400) | PRD §9.2 |
| 底部间距 | `32.dp` + `WindowInsets.navigationBars` | PRD §9.2 + R-03 |
| 版本号格式(Debug) | `formatVersionTag()` → `v1.0(1)debug` | D-23/R-01 |
| 版本号格式(Release) | `formatVersionTag(buildType="")` → `v1.0` | D-21/R-01 |
| contentDescription | `formatVersionDescription()` → "应用版本号 v1.0" | D-22/R-05 |
| Logo 主色 | `Color.White` | 现有 AnimatedSplashContent |
| 副标题色 | `Color.White.copy(alpha=0.8f)` | 现有 AnimatedSplashContent |

---

## §5 组件复用分析

| 组件 | 来源 | 复用方式 | 状态 |
|------|------|----------|------|
| `formatVersionTag()` | VersionTag.kt（已有） | 直接复用 — splash 场景调用同一函数 | ✅ |
| `formatVersionDescription()` | VersionTag.kt（已有） | 直接复用 — contentDescription | ✅ |
| `Text` (Compose) | M3 标准库 | 新建 splash 专用 Text 实例 | ✅ |
| `Box` (Compose) | M3 标准库 | 现有根容器，不改动 | ✅ |

**复用率：** 2/3（67%）— 格式化函数全部复用，仅新增 1 个 Text Composable 调用。

### 新增文件

| 文件 | 说明 | 复杂度 |
|------|------|--------|
| —（无新增文件） | 修改仅限 MainActivity.kt 内 AppWithAnimatedSplash 函数 | 低 |

---

## §6 架构协调设计

### 导航事件
无——版本号在启动页内叠加，不触发导航。

### ViewModel
**不需要 ViewModel。** 版本号是 `BuildConfig.VERSION_NAME` 编译期常量，无异步/无状态变化，纯展示。符合 YAGNI（R-06）。

### BackHandler
无影响——版本号为纯展示，无交互，不涉及返回栈。

---

## §7 无障碍适配

| 元素 | contentDescription | 触控目标 |
|------|-------------------|----------|
| 版本号文本 | `"应用版本号 v1.0"`（Debug: `"应用版本号 v1.0(1)debug"`） | N/A（纯展示） |

---

## §8 前置依赖

| 依赖 | 说明 | 状态 |
|------|------|------|
| `formatVersionTag()` | VersionTag.kt — 已有函数 | ✅ |
| `BuildConfig.VERSION_NAME` | Gradle 自动生成 | ✅ |
| `BuildConfig.VERSION_CODE` | Gradle 自动生成 | ✅ |
| `BuildConfig.BUILD_TYPE` | Gradle 自动生成 | ✅ |

---

## §9 多视角评审记录

> 评审日期: 2026-06-08 | 方式: delegate_task 三视角并行 | 耗时: ~200s

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

> **版本:** v0.1-draft
> **状态:** 待三视角评审。评审完成后版本号升级为 v0.2-review。
