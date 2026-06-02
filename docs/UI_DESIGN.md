# 版本号显示 — UI 设计方案

> **版本:** v0.1-draft
> **功能名称:** 版本号显示（主界面底部 VersionTag + 关于页面 AboutScreen）
> **创建日期:** 2026-06-02
> **基于:** PRD v1.0-confirmed §9 | DECISIONS.md R-01~R-09

---

## §1 设计总览

### 设计目标
- 主界面底部（仅 Debug 构建）显示版本号，供测试/客服快速确认版本
- 「设置→关于」页面（所有构建）展示完整版本信息，支持长按复制
- 全面遵循 Material3 Token 体系，兼容浅色/深色主题，通过 WCAG AA 对比度标准

### 设计语言
- Material3 (M3) 设计系统
- 375dp 基准视口（Phone）
- 颜色 Token：`onSurfaceVariant` 系（非硬编码）

### 核心原则
- **最小视觉噪音**：12sp 小字，onSurfaceVariant × 0.6 半透明，不喧宾夺主
- **安全区适配**：WindowInsets.navigationBars 防全面屏手势遮挡
- **可访问性优先**：TalkBack contentDescription、48dp 触摸目标、WCAG AA 对比度

---

## §2 页面清单与导航

| 页面标识 | 页面名称 | 路由 | 类型 | 说明 |
|---------|---------|------|------|------|
| `MainScreen` | 主界面（登录页） | `/`（根） | 已有 | Box 内 LoginScreen + VersionTag bottom-align |
| `AboutScreen` | 关于页面 | `/about` | 新建 | Scaffold + TopAppBar + 版本号行 + SnackbarHost |
| `SettingsScreen` | 设置页入口 | `/settings` | 新建/轻量 | 提供「关于」导航入口 |

```
导航关系：
  MainScreen ──(设置入口)──▶ SettingsScreen ──(关于)──▶ AboutScreen
       │                                                    
       └── VersionTag (底部常驻，仅 Debug)                  
```

---

## §3 P0 — 主界面底部版本号（VersionTag）

> **范围:** PRD §9 核心功能
> **实现状态:** 已实现（`ui/components/VersionTag.kt`）

### 3.1 线框图

```
┌──────────────────────────────────────┐  375dp × 812dp (Phone)
│                                      │
│   ┌──────────────────────────────┐   │
│   │        登录                   │   │  headlineMedium
│   │   ┌──────────────────────┐   │   │
│   │   │ 手机号               │   │   │  TextField
│   │   └──────────────────────┘   │   │
│   │   ┌──────────────────────┐   │   │
│   │   │     获取验证码        │   │   │  Button
│   │   └──────────────────────┘   │   │
│   └──────────────────────────────┘   │
│                                      │
│                                      │
│            v1.0(1)debug              │  12sp, onSurfaceVariant
│            └── 居中 ──┘              │  padding bottom=8dp
│   ═══════ 导航栏区域 ═══════         │  WindowInsets.navigationBars
└──────────────────────────────────────┘
```

#### 版本号区域放大详图

```
┌───────────────────────────────────┐
│          v1.0(1)debug             │  Text, 12sp
│          ←── 水平居中 ──→         │  onSurfaceVariant
│                                   │  maxLines=1, Ellipsis
│   ═══ navigationBars inset ═══   │  8dp bottom padding
└───────────────────────────────────┘
```

### 3.2 组件层级树

```
MainActivity (ComponentActivity)
└── setContent
    └── MaterialTheme
        └── Surface(Modifier.fillMaxSize())
            └── Box(Modifier.fillMaxSize())
                ├── LoginScreen(Modifier.fillMaxSize())
                │   └── Column(padding=16dp)
                │       ├── Text("登录", headlineMedium)
                │       ├── TextField(label="手机号")
                │       └── Button("获取验证码")
                └── VersionTag(Modifier.align(BottomCenter))    ← [PRD §9 新增]
                    └── Text(
                          text = "v1.0(1)debug",
                          fontSize = 12.sp,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                          modifier = Modifier
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(bottom = 8.dp)
                            .semantics { contentDescription = "应用版本号 v1.0" }
                        )
```

### 3.3 交互状态机

```
                    ┌──────────────┐
                    │   构建类型    │
                    └──────┬───────┘
               Debug ╱       ╲ Release
                    ╱         ╲
              ┌─────────┐   ┌──────────┐
              │ 可见    │   │ 不渲染   │
              │ 静态文本 │   │ (空节点) │
              └─────────┘   └──────────┘
```

VersionTag 为纯静态组件，无交互、无动画、无状态转换。

### 3.4 状态覆盖表

| 状态 | 视觉表现 | 条件 |
|------|---------|------|
| Debug 可见 | `v1.0(1)debug`，12sp，onSurfaceVariant | `BuildConfig.DEBUG == true` |
| Release 隐藏 | 空（Composable 提前 return） | `BuildConfig.DEBUG == false` |
| 深色模式 | onSurfaceVariant 自动切换至深色 Token | 系统深色主题启用 |
| 字体缩放 | maxLines=1 + Ellipsis 截断 | 系统字体 > 100% |
| 超长版本号 | Ellipsis 截断 | 版本字符串过长 |
| 全面屏手势 | windowInsetsPadding(navigationBars) 避开导航栏 | 手势导航设备 |

---

## §4 P1 — AboutScreen（关于页面）

> **范围:** DECISIONS.md R-01 扩展需求（所有构建可见）
> **实现状态:** 设计方案（尚未编码）

### 4.1 线框图

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

### 4.2 组件层级树

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

### 4.3 交互状态机

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

### 4.4 状态覆盖表

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

---

## §5 Token 映射表

### 5.1 主界面 VersionTag (P0)

| 设计属性 | M3 Token | 浅色值 | 深色值 | 来源 |
|----------|----------|--------|--------|------|
| 版本号文字色 | `onSurfaceVariant` | `#49454F` | `#CAC4D0` | D-13 / PRD §9 |
| 版本号字号 | — | 12sp | 12sp | PRD §9 |
| 版本号字重 | — | 400 (Normal) | 400 (Normal) | 默认 |
| 底部间距 | — | 8dp | 8dp | PRD §9 |
| 导航栏 inset | `WindowInsets.navigationBars` | — | — | D-16 |
| 最大行数 | — | 1 | 1 | overflow 安全 |

### 5.2 AboutScreen (P1)

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
| Snackbar 背景 | `inverseSurface` | `#313033` | `#E6E1E5` | M3 默认 |
| Snackbar 文字 | `inverseOnSurface` | `#F4EFF4` | `#313033` | M3 默认 |

---

## §6 交互细节

### 6.1 VersionTag (P0)
- **无交互**：纯静态展示，不可点击，不可聚焦
- **构建条件渲染**：`if (!BuildConfig.DEBUG) return` — Release 构建不产生任何 Composition 节点

### 6.2 AboutVersionLabel (P1)
- **点击复制**：点击版本号行 → 复制到剪贴板 → Snackbar 反馈
- **防抖**：300ms debounce，防止快速双击触发多次复制
- **Ripple**：Material3 标准涟漪效果， bounded within 48dp touch target
- **长按**：系统默认文本选择行为（预留）

### 6.3 页面过渡
- **进入动画**：AboutScreen fadeIn 300ms
- **返回动画**：navController.popBackStack()，系统默认过渡

---

## §7 可访问性

| 检查项 | VersionTag (P0) | AboutScreen (P1) | 标准 |
|--------|:---:|:---:|------|
| WCAG AA 对比度 (≥3:1 大文本) | ✅ | ✅ | onSurfaceVariant 满足 |
| TalkBack contentDescription | ✅ "应用版本号 v1.0" | ✅ 含完整版本信息 | D-17 |
| 触摸目标 ≥ 48dp | N/A（无交互） | ✅ | AC-10 |
| 字体缩放 200% | ✅ Ellipsis 截断 | ✅ maxLines=1 + Ellipsis | — |
| 装饰性图标 | N/A | ✅ contentDescription=null | — |
| 横屏适配 | ✅ | ⚠️ 窄屏隐藏版本号行 | — |

---

## §8 适配策略

| 场景 | VersionTag (P0) | AboutScreen (P1) |
|------|:---:|:---:|
| 浅色主题 | onSurfaceVariant 默认映射 | onSurfaceVariant × 0.6 |
| 深色主题 | darkColorScheme onSurfaceVariant | darkColorScheme onSurfaceVariant × 0.6 |
| 全面屏手势 | windowInsetsPadding(navigationBars) | 同上 |
| 字体缩放 ≤ 200% | maxLines=1 + Ellipsis | 同上 |
| 横屏 | 底部区域变窄，Ellipsis 截断 | 窄屏隐藏版本号行 |
| 分屏/多窗口 | Box bottom-align 自适应 | Scaffold 自适应 |
| 多 DPI | 12sp 相对单位自适应 | 14sp 相对单位自适应 |
| minSdk 26 | ✅ | ✅ |

---

## §9 组件复用分析

### 9.1 已有组件

| 组件 | 位置 | 类型 | 复用评估 |
|------|------|------|----------|
| `VersionTag` | `ui/components/VersionTag.kt` | Composable | **P0 已实现**，静态底部版本号 |
| `formatVersionTag()` | `ui/components/VersionTag.kt` | 纯函数 | **可直接复用**于 AboutScreen |
| `formatVersionDescription()` | `ui/components/VersionTag.kt` | 纯函数 | **可直接复用**于 AboutScreen contentDescription |
| `MainActivity` | `MainActivity.kt` | Activity | **已集成** VersionTag(BottomCenter) |
| `LoginScreen` | `MainActivity.kt` | Composable | 现有登录页，与版本号解耦 |

### 9.2 P0 复用决策

| 组件 | 复用/新增 | 来源 | 说明 |
|------|-----------|------|------|
| `Text` | 复用 | Material3 | 标准 Composable |
| `MaterialTheme.colorScheme.onSurfaceVariant` | 复用 | M3 Token | 颜色来源 |
| `WindowInsets.navigationBars` | 复用 | Compose Foundation | 全面屏适配 |
| `Modifier.semantics` | 复用 | Compose UI | 无障碍 |
| `VersionTag` | **已有** | 本项目 | 已完成编码+测试 |

### 9.3 P1 新增组件

| 组件 | 复用/新增 | 来源 | 修改说明 |
|------|-----------|------|----------|
| `Scaffold` | 复用 | Material3 | 无修改，标准组件 |
| `TopAppBar` | 复用 | Material3 | 配置 title="关于", navigationIcon |
| `SnackbarHost` | 复用 | Material3 | 挂载于 Scaffold.snackbarHost |
| `SnackbarHostState` | 复用 | Material3 | ViewModel 或 remember 管理 |
| `Text` | 复用 | Material3 | bodyMedium 14sp |
| `Icon` | 复用 | `material-icons-extended` | `Icons.Outlined.ContentCopy` 16dp |
| `Row` / `Column` / `Box` | 复用 | Compose Foundation | 标准布局 |
| `HorizontalDivider` | 复用 | Material3 | 分隔线 |
| `formatVersionTag()` | **复用** | `VersionTag.kt` (现有) | 纯函数，无副作用，可直接复用 |
| `formatVersionDescription()` | **复用** | `VersionTag.kt` (现有) | 纯函数，用于 contentDescription |
| `VersionTag` Composable | **不复用** | `VersionTag.kt` (现有) | `if (!BuildConfig.DEBUG) return` 阻止 Release 渲染；12sp 硬编码不匹配 14sp 需求 |
| `AboutVersionLabel` | **新增** | — | 独立实现，14sp，含 ContentCopy 图标，支持点击复制 |
| `AboutScreen` | **新增** | — | 完整页面，Scaffold + TopAppBar + 信息区域 |
| `NavHost` | **新增** | `navigation-compose` | 路由 `/about`，替换 MainActivity 硬编码 |
| `darkColorScheme` | **新增** | Theme.kt | D-18/R2-P1-3：显式定义深色配色方案 |

### 9.4 预估工时

| 组件 | 复杂度 | 预估工时 | 说明 |
|------|--------|----------|------|
| VersionTag (P0) | ✅ 已完成 | — | 已编码 + 测试完成 |
| `AboutVersionLabel` | 低 | 1h | Composable: Row(Text + Icon) + clickable + Snackbar 回调 |
| `AboutScreen` | 中 | 2h | Scaffold + TopAppBar + 信息区域布局 + AboutVersionLabel 集成 |
| `NavHost` + 路由 | 中 | 1.5h | 替换 MainActivity 硬编码，配置 `/about` 路由 |
| `darkColorScheme` | 低 | 0.5h | Theme.kt 中定义 darkColorScheme |
| Compose UI 测试 | 中 | 2h | 渲染/点击/Snackbar/深色模式/窄屏隐藏（5 个用例） |
| **P1 合计** | | **7h** | |

---

## §10 架构协调设计

- **跨 Screen 通信**: AboutScreen 通过 `SnackbarHostState` 自管理反馈，无需跨 Screen 事件通道
- **BackHandler**: TopAppBar navigationIcon 调用 `navController.popBackStack()`，系统返回键由 NavHost 自动处理
- **WindowInsets**: Scaffold 自动处理 `imePadding`；版本号行额外 `windowInsetsPadding(WindowInsets.navigationBars)` 防手势导航遮挡
- **ViewModel**: 版本号 BuildConfig 为编译期常量，无需 ViewModel；SnackbarHostState 由 `remember` 管理即可
- **暗色模式**: 需要显式定义 `darkColorScheme`（当前缺失），否则深色模式下 Token 依赖默认值可能不准确
- **P0/P1 关系**: P0 VersionTag 与 P1 AboutVersionLabel 互不依赖。formatVersionTag()/formatVersionDescription() 纯函数作为共享逻辑层

---

## §11 多视角评审记录

> **评审日期:** 2026-06-02
> **评审方式:** ui-design-review 综合评审（基于 PRD v1.0-confirmed §9 + DECISIONS.md）

### 评审总览

| 视角 | 评分 | P0 | P1 | 关键发现 |
|------|------|----|----|----------|
| 设计输入完整性 | — | 0 | 0 | PRD §9 + DECISIONS.md 覆盖完整 |
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

## §12 变更记录

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| v0.1-draft | 2026-06-02 | 初始生成，覆盖 PRD §9 (P0) + AboutScreen (P1)，基于 PRD v1.0-confirmed §9 + DECISIONS.md R-01~R-09 |

---

> **状态:** v0.1-draft — 未冻结，请审阅后回复「确认」冻结进入技术方案阶段。
