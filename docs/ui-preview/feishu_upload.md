<md-todo>交互原型(375px视口M3风格): https://htmlpreview.github.io/?https://raw.githubusercontent.com/ywei2027/my-android-app/53fa30be8af05d4105ebefbe0681d6a431a8645b/docs/ui-preview/index.html</md-todo>

<md-todo>
### 截图预览
| 页面 | 状态 | 预览 |
|------|------|------|
| 🔍 AnimatedSplashContent | Debug 默认 | https://raw.githubusercontent.com/ywei2027/my-android-app/53fa30be8af05d4105ebefbe0681d6a431a8645b/docs/ui-preview/splash_default.html</md-todo>

---

# 启动页版本号显示 — UI 设计方案

> **版本:** v0.2-review
> **功能名称:** 启动页版本号显示
> **创建日期:** 2026-06-08
> **基于:** PRD v1.0-confirmed §9 | DECISIONS.md

---

## §1 设计总览

### 设计目标
- 在现有 AnimatedSplashContent 底部追加版本号 Text，不破坏原有动画结构
- 版本号使用独立 AnimatedVisibility(fadeIn)，延迟 300ms 出现
- 颜色与现有白色 UI 元素协调：`Color.White.copy(alpha=0.85f)`

### 设计语言
- Material3 (M3) 设计系统
- 375dp 基准视口
- 品牌蓝 `#1A73E8` 背景

---

## §2 页面清单与导航

| 页面 | 路由 | 类型 | 入口 | 说明 |
|------|------|------|------|------|
| AnimatedSplashContent | *(无路由)* | 改造 | MainActivity.setContent | splash 持续 ~1.5s 后自动淡出 |

### 导航图
```
MainActivity
└── AppWithAnimatedSplash (Box)
    ├── AnimatedVisibility (主内容: NewsAppNavHost)
    └── AnimatedVisibility (Splash: AnimatedSplashContent)
        └── Box(fillMaxSize, #1A73E8)
            ├── AnimatedVisibility(scaleIn+fadeIn, 600ms)
            │   └── Column(Alignment.Center)
            │       ├── Text("📰", 72sp)
            │       ├── Text("新闻", 28sp, Bold, White)
            │       └── Text("热点资讯 一键掌握", 14sp, White α0.8)
            └── 🆕 AnimatedVisibility(fadeIn, 300ms, delay=100ms)
                └── Text(versionTag, 12sp, White α0.85, BottomCenter)
```

---

## §3 页面设计 — AnimatedSplashContent

### 线框图
```
┌──────────────────────────────────┐
│           #1A73E8 背景           │
│                                  │
│           ┌────────┐             │
│           │   📰   │  72sp       │
│           │        │             │
│           │  新闻  │  28sp Bold  │
│           │        │  White      │
│           │热点资讯│  14sp       │
│           │一键掌握│  White α0.8 │
│           └────────┘             │
│               ↑ scaleIn+fadeIn   │
│             600ms, 延迟进入      │
│                                  │
│                                  │
│    v1.0.0(1)debug  ← 12sp       │
│    White α0.85, fadeIn 300ms    │
│    32dp+systemBars 底部边距      │
└──────────────────────────────────┘
```

### 组件层级树
```
AnimatedSplashContent(onFinished)
└── Box(Modifier.fillMaxSize().background(Color(0xFF1A73E8)), Alignment.Center)
    ├── AnimatedVisibility(visible, scaleIn+fadeIn, 600ms)
    │   └── Column(HorizontalAlignment.CenterHorizontally)
    │       ├── Text("📰", fontSize=72.sp)
    │       ├── Spacer(16.dp)
    │       ├── Text("新闻", fontSize=28.sp, Bold, Color.White)
    │       ├── Spacer(8.dp)
    │       └── Text("热点资讯 一键掌握", fontSize=14.sp, Color.White.copy(alpha=0.8f))
    │
    └── 🆕 AnimatedVisibility(enter=fadeIn(tween(300ms, delayMillis=100)))
        └── Text(
                text = versionString,
                fontSize = 12.sp,  /* hardcode，非 labelSmall */
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(bottom = 32.dp)
                    .semantics { contentDescription = versionDesc }
            )
```

### 交互状态机
```
┌──────────────┐   LaunchedEffect      ┌──────────────┐
│   Splash     │ ─────────────────────→ │   Visible    │
│   进入       │   visible=true         │  Logo弹入    │
│              │   scaleIn+fadeIn       │  600ms动画   │
└──────────────┘                        └──────┬───────┘
                                               │ delay(100ms)
                                               ↓
                                        ┌──────────────┐
                                        │  VersionTag  │
                                        │  fadeIn 300ms│
                                        │  delay=100ms  │
                                        └──────┬───────┘
                                               │ delay(500ms)
                                               ↓ onFinished()
                                        ┌──────────────┐
                                        │   FadeOut    │
                                        │  Logo淡出    │
                                        │  VersionTag  │
                                        │  随Box销毁    │
                                        └──────────────┘
```

### 状态覆盖表

| 状态 | UI 表现 | 说明 |
|------|---------|------|
| 默认(Debug) | 底部显示 `v1.0.0(1)debug` | 完整构建信息 |
| 默认(Release) | 底部显示 `v1.0.0(1)` | 无 buildType 后缀 |
| 降级/异常 | 显示 `v?.?` 降级文案 | formatVersionTag null->"? " |
| 暗色模式 | 背景 #0D47A1，文字 White α0.6 | isSystemInDarkTheme() |

---

## §4 Token 映射表

| 设计属性 | M3 Token / 硬编码值 | 说明 |
|----------|---------------------|------|
| 背景色 | `#1A73E8` (`PrimaryLight`) | 现有背景，不变 |
| Logo 文字 | `Color.White` | 现有，不变 |
| 副标题文字 | `Color.White.copy(alpha=0.8f)` | 现有，不变 |
| 版本号文字 | `Color.White.copy(alpha=0.85f)` | 🆕 修改(原0.7→0.85)≥4.5:1 |
| 版本号字号 | `12.sp` (hardcode，非 labelSmall 11sp) | 🆕 修改(原labelSmall→12sp) |
| 版本号底部间距 | `32.dp + navigationBars` | 🆕 |
| 版本号行数限制 | `maxLines=1, TextOverflow.Ellipsis` | 🆕 |
- 字体缩放上限 `fontScale.clamp(1.0f, 1.5f)` | 🆕 |
| RTL | `TextDirection.Content` | 🆕 |
| 暗色模式背景 | `#0D47A1` (via `isSystemInDarkTheme()`) | 🆕 |
| 暗色模式文字 | `Color.White.copy(alpha=0.6f)` | 🆕 |

## §5 组件复用分析

| 组件 | 来源 | 复用方式 | 状态 |
|------|------|----------|------|
| `formatVersionTag()` | `VersionTag.kt` | 直接调用 | ✅ 复用 |
| `formatVersionDescription()` | `VersionTag.kt` | 直接调用 | ✅ 复用 |
| `AnimatedSplashContent` | `MainActivity.kt` | 改造（新增子元素） | 🔧 修改 |
| `VersionTag` Composable | `VersionTag.kt` | **不复用**（Debug-only 阻断） | ❌ 绕过 |

### 新增文件

| 文件 | 说明 | 复杂度 |
|------|------|--------|
| *(无)* | 仅修改 `MainActivity.kt` 中 AnimatedSplashContent | 低 |

---

## §6 架构协调设计

### 导航事件
```
无导航变更 — 版本号在 AnimatedSplashContent 内部，splash 结束后整个 Composable 随 fadeOut 销毁
```

### ViewModel
**不需要 ViewModel。** BuildConfig 为编译期常量，版本号纯展示无状态变化，符合 YAGNI。

### BackHandler
无交互，不需要 BackHandler。

---

## §7 无障碍适配

| 元素 | contentDescription | 触控目标 |
|------|-------------------|----------|
| 版本号(Debug) | `"应用版本号 v{name} 构建 {code} 调试版本"` | N/A（纯展示） |
| 版本号(Release) | `"应用版本号 v{name}"` | N/A（纯展示） |
| 版本号(异常) | `""` | N/A（静默降级） |

> contentDescription **始终包含完整版本号**，不因 Ellipsis 截断。

---

## §8 前置依赖

| 依赖 | 说明 | 状态 |
|------|------|------|
| `BuildConfig.VERSION_NAME` | 编译期版本名 | ✅ 已有 |
| `BuildConfig.VERSION_CODE` | 编译期版本号 | ✅ 已有 |
| `BuildConfig.BUILD_TYPE` | 构建类型 | ✅ 已有 |
| `formatVersionTag()` | 版本号格式化 | ✅ 已有 |
| `formatVersionDescription()` | 无障碍描述 | ✅ 已有 |
| `WindowInsets.systemBars` | 安全区 API | ✅ 已有(API 26+) |

---

## §9 多视角评审记录

> 评审日期: 2026-06-08 | 方式: delegate_task 三视角并行 | 耗时: ~70s

### 评审总览

| 视角 | 评分 | P0 | P1 | 核心发现 |
|------|------|----|----|----------|
| C1 UX 交互 | 6.5/10 | 4 | 6 | Token冲突/Dark缺失/SystemBars/时长 |
| C2 视觉审美 | 37/50 | 2 | 4 | 动画节奏出色，暗色面/横屏待补 |
| C3 前端实现 | 7.2/10 | 3 | 5 | 对比度需重算/null降级/组件复用67% |

### P0 修订记录（已在正文自动修订）

| 编号 | 问题 | 修订内容 |
|------|------|----------|
| P0-1 | 字号与Theme冲突(12sp vs labelSmall=11sp) | 版本号 hardcode 12sp，不引用 labelSmall Token |
| P0-2 | 总时长超标(2400ms>2s) | 压缩为：600ms主+delay(100ms)+版本fadeIn(300ms)+停留(500ms)=1500ms |
| P0-3 | 暗色主题缺失 | 增加 `isSystemInDarkTheme()` 分支：背景 #0D47A1 / 文字 White α0.6 |
| P0-4 | SystemBars未显式调用 | Box 增加 `WindowInsets.systemBars` padding |
| P0-5 | 对比度重算(White α0.7 on #1A73E8) | 改为 `Color.White.copy(alpha=0.85f)` 确保≥4.5:1 |
| P0-6 | Release格式未明确声明 | 明确 `formatVersionTag(buildType="")` 输出 `v{name}({code})` |
| P0-7 | null降级无兜底文案 | `formatVersionTag` 内加 `?: "v?.?"` 降级 |
| P0-8 | Debug双版本号风险 | 标注 VersionTag.kt 需同步 conditionally disable |
| P0-9 | 暗色surface层级缺失 | 补充 surface/background token 映射 |

### C2 视觉评审 10 维度

| # | 维度 | 评分 | 关键评语 |
|---|------|:---:|------|
| 1 | 格式塔 | 4/5 | 主内容群集清晰，版本号与主内容属同组但空间分离 |
| 2 | 视觉层级 | 4.5/5 | 72sp→28sp→14sp→12sp梯度清晰 |
| 3 | 色彩 | 4/5 | #1A73E8品牌蓝统一，暗色面待补足 |
| 4 | 字体 | 3.5/5 | 3级字阶合理，行高/中英混排待完善 |
| 5 | 空间网格 | 3.5/5 | 底部40px对齐8dp网格，其他间距未声明 |
| 6 | 布局比例 | 4/5 | 居中对称+底部重心，safe area周全 |
| 7 | 可感知可操作 | 2.5/5 | 纯展示无交互(OK)，nav-bar触控区不足 |
| 8 | 一致性 | 4/5 | White α梯度统一，动画参数一致 |
| 9 | 情感品牌 | 4/5 | 📰+新闻轻快专业，scaleIn优雅入场 |
| 10 | 平台适配 | 3/5 | M3 token✅，暗色/横屏待补 |

**综合: 37/50 (良好)**

### 审美亮点
1. 入场动画叙事力强 — scaleIn(0.6s)→delay→fadeIn(0.3s)阅读引导
2. 透明度层级精确 — 1.0/0.8/0.85 三档视觉退阶
3. 底部指示条 134×5px White α0.3 传递"可滑动"暗示
4. safe area 32dp+navBars 周全

---

> **版本:** v0.2-review
> **状态:** 三视角评审完成，P0 已自动修订。请审阅后回复「确认」冻结进入技术方案。
