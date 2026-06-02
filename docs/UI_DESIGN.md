# 关于页面版本号显示 — UI 设计方案

> **版本:** v0.1-draft
> **功能名称:** 关于页面 & 版本号显示（AboutScreen + AboutVersionLabel）
> **创建日期:** 2026-06-02
> **基于:** PRD v1.0-confirmed §9 | DECISIONS.md D-12~D-29

---

## 页面清单

| 页面标识 | 页面名称 | 路由 | 类型 | 说明 |
|---------|---------|------|------|------|
| `AboutScreen` | 关于页面 | `/about` | 新建 | Scaffold + TopAppBar + 版本号行 + SnackbarHost |
| `NavHost` | 导航容器 | 根路由 | 新建 | 替换 MainActivity 直接渲染 LoginScreen 的硬编码 |
| `SettingsScreen` | 设置页入口 | — | 新建/轻量 | 提供 "关于" 导航入口，触发 `navController.navigate("/about")` |

---

## P1 — AboutScreen（关于页面）

### 线框图

```
┌──────────────────────────────────────┐  375dp × 812dp (Phone)
│  ← 关于                    TopAppBar │  headlineSmall, 64dp
├──────────────────────────────────────┤
│                                      │
│            ┌──────┐                  │
│            │ App  │  (品牌图标)       │  48dp × 48dp
│            │ Icon │                  │
│            └──────┘                  │
│         我的应用                      │  titleLarge
│                                      │
│   ─────── 信息区域 ───────            │
│                                      │
│    App 名称    我的应用               │  bodyLarge / onSurface
│    版本号       v1.0.0(1)release 📋  │  bodyMedium 14sp / onSurfaceVariant
│                                      │
├──────────────────────────────────────┤
│        ════ SnackbarHost ════        │  Scaffold 内置
└──────────────────────────────────────┘
```

#### 版本号行放大详图

```
┌───────────────────────────────────┐
│                                   │
│    ┌─────────────────────────┐    │
│    │  v1.0.0(1)release  📋  │    │  Row(Center), 48dp min height
│    │  └── 14sp ──┘  └ 16dp ┘│    │  alpha 0.6, onSurfaceVariant
│    └─────────────────────────┘    │  clickable + ripple
│                                   │
└───────────────────────────────────┘
         ↑ bottom 8dp spacing
```

### 组件层级树

```
AboutScreen (Scaffold)
├── TopAppBar
│   ├── title: Text("关于")
│   └── navigationIcon: IconButton(Icons.AutoMirrored.Filled.ArrowBack) → navController.popBackStack()
├── Content: Box(Modifier.fillMaxSize())
│   ├── Column(center horizontally, padding 16dp)
│   │   ├── AppIcon (48dp, optional branding)
│   │   ├── Text("我的应用", style = titleLarge)
│   │   ├── Spacer(24dp)
│   │   ├── HorizontalDivider
│   │   ├── Spacer(16dp)
│   │   ├── InfoRow("App 名称", "我的应用")
│   │   ├── InfoRow("版本号", ...) ← AboutVersionLabel 嵌入此处
│   │   └── ... (其他信息项)
│   └── AboutVersionLabel (Box → Alignment.BottomCenter)
│       └── Row(
│             horizontalArrangement = Center,
│             modifier = Modifier
│               .defaultMinSize(minHeight = 48.dp)
│               .clickable(onClick = onCopy, ripple)
│               .semantics { contentDescription = talkbackDesc }
│               .windowInsetsPadding(WindowInsets.navigationBars)
│               .padding(bottom = 8.dp)
│           )
│           ├── Text(
│           │     text = versionString,
│           │     style = MaterialTheme.typography.bodyMedium,
│           │     color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
│           │     maxLines = 1,
│           │     overflow = TextOverflow.Ellipsis,
│           │     textAlign = TextAlign.Center
│           │   )
│           └── Icon(
│                 imageVector = Icons.Outlined.ContentCopy,
│                 contentDescription = null,  // decorative
│                 modifier = Modifier.size(16.dp).padding(start = 4.dp),
│                 tint = onSurfaceVariant.copy(alpha = 0.6f)
│               )
└── snackbarHost: SnackbarHost(snackbarHostState)
```

### 交互状态机

```
                    ┌──────────────────────────┐
                    │                          │
       enter        ▼         click            │
  ┌─────────┐  ┌─────────┐  ┌──────────┐      │
  │ Entering │─▶│  Idle   │─▶│ Copying  │      │
  │ fadeIn  │  │ 正常显示 │  │ debounce │      │
  │ 300ms   │  └─────────┘  │  300ms   │      │
  └─────────┘       ▲       └────┬─────┘      │
                    │            │             │
                    │   ┌────────┴────────┐    │
                    │   ▼                 ▼    │
                    │ ┌────────┐   ┌──────────┐│
                    │ │Success │   │  Failure ││
                    │ │Snackbar│   │ Snackbar ││
                    │ │"已复制"│   │"复制失败" ││
                    │ │+ 关闭  │   │ + 重试──▶││——retry→Copying
                    │ └────────┘   └──────────┘│
                    │      │             │     │
                    └──────┴─────────────┘     │
                          dismiss(2s)          │
                          ┌────────────────────┘
                          ▼
                      ┌───────┐
                      │ Empty │  (versionName 为空/null)
                      │"版本未知"│  alpha=1.0, 不可点击
                      └───────┘
```

### 状态覆盖表

| 状态 | 视觉表现 | 交互 | 截图文件 |
|------|---------|:----:|----------|
| 正常 (Debug) | `v1.0.0(1)release`，14sp，onSurfaceVariant × 0.6，含 📋 图标 | ✅ | `about_normal_debug.png` |
| 正常 (Release) | `v1.0.0(1)`（无 buildType 后缀），14sp，onSurfaceVariant × 0.6 | ✅ | `about_normal_release.png` |
| 按下 | Ripple bounded，48dp 触摸目标 | ✅ | — |
| 复制成功 | Snackbar "版本号已复制" + "关闭" Action，2s 自动消失 | — | `about_copy_success.png` |
| 复制失败 | Snackbar "复制失败，请重试" + "重试" Action | ✅ | `about_copy_failure.png` |
| 版本号未知 | "版本未知"，14sp，alpha 1.0，无图标 | ❌ | `about_unknown_version.png` |
| 宽度 < 480dp | 版本号行 `visibility = GONE` | ❌ | — |
| 深色模式 | `darkColorScheme.onSurfaceVariant × 0.6`，WCAG AA 对比度 ≥ 3:1 | ✅ | `about_dark_mode.png` |
| 字体 200% | `maxLines=1` + `Ellipsis` 截断，48dp 触摸区域保持 | ✅ | — |
| TalkBack | 朗读 "应用版本号 v1.0.0，版本代码 1，构建类型 release，点按两次即可复制" | ✅ | — |

### Token 映射表

| 设计属性 | M3 Token | 浅色值 | 深色值 | 来源 |
|----------|----------|--------|--------|------|
| 页面背景 | `surface` | `#FFFBFE` | `#1C1B1F` | M3 默认 |
| TopAppBar 背景 | `surface` | `#FFFBFE` | `#1C1B1F` | M3 默认 |
| TopAppBar 标题色 | `onSurface` | `#1C1B1F` | `#E6E1E5` | M3 默认 |
| 标题文字色 | `onSurface` | `#1C1B1F` | `#E6E1E5` | M3 默认 |
| 信息标签色 | `onSurfaceVariant` | `#49454F` | `#CAC4D0` | M3 默认 |
| 版本号文字色 | `onSurfaceVariant × 0.6` | `#49454F` × 0.6 → 合成色 | `#CAC4D0` × 0.6 → 合成色 | D-13 + D-19 |
| 图标色 | `onSurfaceVariant × 0.6` | 同版本号文字 | 同版本号文字 | R2-D-29 |
| 分隔线色 | `outlineVariant` | `#CAC4D0` | `#49454F` | M3 默认 |
| 版本号字号 | — | 14sp | 14sp | D-22 |
| 版本号字重 | — | `BodyMedium` (400) | `BodyMedium` (400) | M3 |
| 底部间距 | — | 8dp | 8dp | D-20 |
| 触摸区域 | — | ≥ 48dp × 48dp | ≥ 48dp × 48dp | AC-10 |
| 图标尺寸 | — | 16dp × 16dp | 16dp × 16dp | R2-D-29 |
| 图标间距 | — | 4dp | 4dp | PRD §9.2 |
| 导航栏 inset | `WindowInsets.navigationBars` | — | — | D-16 |

### 组件复用分析

| 组件 | 复用/新增 | 来源 | 修改说明 |
|------|-----------|------|----------|
| `Scaffold` | 复用 | Material3 | 无修改，标准组件 |
| `TopAppBar` | 复用 | Material3 | 配置 title="关于", navigationIcon |
| `SnackbarHost` | 复用 | Material3 | 挂载于 Scaffold.snackbarHost |
| `SnackbarHostState` | 复用 | Material3 | ViewModel 或 remember 管理 |
| `Text` | 复用 | Material3 | bodyMedium 14sp |
| `Icon` | 复用 | `material-icons-extended` | `Icons.Outlined.ContentCopy` 16dp |
| `Row` / `Column` / `Box` | 复用 | Compose Foundation | 标准布局 |
| `formatVersionTag()` | **复用** | `VersionTag.kt` (现有) | 纯函数，无副作用，可直接复用 |
| `formatVersionDescription()` | **复用** | `VersionTag.kt` (现有) | 纯函数，用于 contentDescription |
| `VersionTag` Composable | **不复用** | `VersionTag.kt` (现有) | D-24: `if (!BuildConfig.DEBUG) return` 阻止 Release 渲染；12sp 硬编码不匹配 14sp 需求 |
| `AboutVersionLabel` | **新增** | — | 独立实现，14sp，含 ContentCopy 图标，支持点击复制 |
| `AboutScreen` | **新增** | — | 完整页面，Scaffold + TopAppBar + 信息区域 |
| `NavHost` | **新增** | `navigation-compose` | 路由 `/about`，替换 MainActivity 硬编码 |
| `darkColorScheme` | **新增** | Theme.kt | D-18/R2-P1-3：显式定义深色配色方案 |

### 新增组件清单

| 组件 | 复杂度 | 预估工时 | 说明 |
|------|--------|----------|------|
| `AboutVersionLabel` | 低 | 1h | Composable: Row(Text + Icon) + clickable + Snackbar 回调 |
| `AboutScreen` | 中 | 2h | Scaffold + TopAppBar + 信息区域布局 + AboutVersionLabel 集成 |
| `NavHost` + 路由 | 中 | 1.5h | 替换 MainActivity 硬编码，配置 `/about` 路由 |
| `darkColorScheme` | 低 | 0.5h | Theme.kt 中定义 darkColorScheme |
| Compose UI 测试 | 中 | 2h | 渲染/点击/Snackbar/深色模式/窄屏隐藏（5 个用例） |
| **合计** | | **7h** | |

### 架构协调设计

- **跨 Screen 通信**: AboutScreen 通过 `SnackbarHostState` 自管理反馈，无需跨 Screen 事件通道
- **BackHandler**: TopAppBar navigationIcon 调用 `navController.popBackStack()`，系统返回键由 NavHost 自动处理
- **WindowInsets**: Scaffold 自动处理 `imePadding`；版本号行额外 `windowInsetsPadding(WindowInsets.navigationBars)` 防手势导航遮挡
- **ViewModel**: 可选。版本号 BuildConfig 为编译期常量，无需 ViewModel；SnackbarHostState 由 `remember` 管理即可
- **暗色模式**: 需要显式定义 `darkColorScheme`（当前缺失），否则深色模式下 Token 依赖默认值可能不准确

---

## 多视角评审记录

> **评审日期:** 2026-06-02
> **评审方式:** ui-design-review 综合评审（基于 PRD v1.0-confirmed §9 + DECISIONS.md）

### 评审总览

| 视角 | 评分 | P0 | P1 | 关键发现 |
|------|------|----|----|----------|
| 设计输入完整性 | — | 0 | 0 | PRD §9 覆盖完整：布局/交互/状态/Token/自适应 |
| 组件复用 | — | 0 | 0 | `formatVersionTag()` / `formatVersionDescription()` 纯函数可直接复用 |
| 无障碍合规 | — | 0 | 0 | contentDescription / 48dp / WCAG AA ≥3:1 已在设计稿中覆盖 |
| 现有架构冲突 | — | 1 | 1 | P0-01 缺失 Theme.kt/darkColorScheme；P1-01 NavHost 替换工程 |

### P0 问题清单（必须修复）

| # | 问题 | 决议 |
|---|------|------|
| P0-01 | `darkColorScheme` 未配置：当前项目无 Theme.kt，深色模式 Token 依赖默认值不可靠 | 新建 `ui/theme/Theme.kt`，显式定义 `darkColorScheme` |

### P1 问题清单（建议修复）

| # | 问题 | 现状 |
|---|------|------|
| P1-01 | `NavHost` 替换需重新规划 MainActivity 布局（当前 Box 内直接挂 LoginScreen + VersionTag） | NavHost 作为根路由，VersionTag 移至 NavHost 外层 Box 或独立挂载 |

---

## 变更记录

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| v0.1-draft | 2026-06-02 | 初始生成，基于 PRD v1.0-confirmed §9 + DECISIONS.md D-12~D-29 |

---

> **状态:** v0.1-draft — 未冻结，请审阅后回复「确认」冻结进入技术方案阶段。
