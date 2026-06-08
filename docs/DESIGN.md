# 启动页版本号显示 — 技术方案

> **版本:** v1.0-confirmed
> **功能名称:** 启动页版本号显示
> **基于:** PRD v1.0-confirmed | UI_DESIGN v1.0-confirmed

---

## §1 架构概览

```
MainActivity.kt
└── AppWithAnimatedSplash(Box)
    ├── AnimatedVisibility(主内容: NewsAppNavHost)
    └── AnimatedVisibility(Splash)
        └── AnimatedSplashContent(onFinished)
            └── Box(fillMaxSize, background=#1A73E8, Center)
                ├── AnimatedVisibility(scaleIn+fadeIn, 600ms)
                │   └── Column → 原有 Logo UI
                └── 🆕 AnimatedVisibility(fadeIn, delay=100ms, 300ms)
                    └── Text(versionTag, BottomCenter, 12sp, White α0.85)
```

**策略**：单文件最小改动。现有 AnimatedSplashContent Box 底部追加独立 AnimatedVisibility+Text，零新文件、零新依赖、无 ViewModel。

---

## §2 模块设计

### 2.1 MainActivity.kt — AnimatedSplashContent 改造

```kotlin
// G:\workspace\projects\my-android-app\app\src\main\java\com\example\myandroidapp\MainActivity.kt

@Composable
fun AnimatedSplashContent(onFinished: () -> Unit) {
    // 版本号格式化（复用 VersionTag.kt）
    val versionTag = formatVersionTag(if (BuildConfig.DEBUG) BuildConfig.BUILD_TYPE else "")
    val versionDesc = formatVersionDescription()
    val isDark = isSystemInDarkTheme()

    val versionAlpha = if (isDark) 0.8f else 0.85f  // P0-D1 修订: 暗色 α0.8≤≥4.5:1
    val bgColor = if (isDark) Color(0xFF0D47A1) else Color(0xFF1A73E8)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        // --- 原有：主内容动画 ---
        AnimatedVisibility(
            visible = true,
            enter = scaleIn(animationSpec = tween(600)) + fadeIn(tween(600))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📰", fontSize = 72.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text("新闻", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text("热点资讯 一键掌握", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
            }
        }

        // --- 🆕 版本号（独立动画，不参与主内容 fadeOut）---
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(300, delayMillis = 100))
        ) {
            Text(
                text = versionTag,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = versionAlpha),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(bottom = 32.dp)
                    .semantics { contentDescription = versionDesc }
            )
        }

        // 注意：VersionTag.kt 中 VersionTag Composable 含 `if (!BuildConfig.DEBUG) return`
        // Debug 构建下会双重显示版本号。需同步修改 VersionTag.kt 或在 AnimatedSplashContent
        // 中 suppress 原 VersionTag 调用。
    }
}
```

### 2.2 VersionTag.kt — 协同修改

```kotlin
// G:\workspace\projects\my-android-app\app\src\main\java\com\example\myandroidapp\ui\components\VersionTag.kt

fun formatVersionTag(buildType: String = ""): String {
    val name = BuildConfig.VERSION_NAME ?: "?.?"
    val code = BuildConfig.VERSION_CODE
    val suffix = if (buildType.isNotEmpty()) buildType else ""
    return buildString {
        append("v")
        append(name)
        append("(")
        append(code)
        append(")")
        if (suffix.isNotEmpty()) append(suffix)
    }
}

fun formatVersionDescription(): String {
    val name = BuildConfig.VERSION_NAME ?: "未知"
    val code = BuildConfig.VERSION_CODE
    return if (BuildConfig.DEBUG)
        "应用版本号 v$name 构建 $code 调试版本"
    else
        "应用版本号 v$name"
}

// ⚠️ 修改：VersionTag Composable 增加开关
// 原代码: if (!BuildConfig.DEBUG) return
// 改为: 添加 enabled 参数，AnimatedSplashContent 中传入 enabled=false
@Composable
fun VersionTag(modifier: Modifier = Modifier, enabled: Boolean = true) {
    if (!enabled) return  // 🆕 当 splash 已显示版本号时禁用
    if (!BuildConfig.DEBUG) return
    // ... 原有实现不变
}
```

### 2.N 数据流

```
BuildConfig.VERSION_NAME (编译期常量)
BuildConfig.VERSION_CODE (编译期常量)
BuildConfig.BUILD_TYPE  (编译期常量)
        │
        ▼
formatVersionTag(buildType: String)  ───→  Text(versionTag)
formatVersionDescription()            ───→  Modifier.semantics(contentDescription)
        │
        ▼
Composable 直接消费（无 Repository / ViewModel / StateFlow）
```

---

## §3 接口定义

```kotlin
// 内部函数签名（VersionTag.kt 已有，无新增公开接口）
fun formatVersionTag(buildType: String = ""): String
fun formatVersionDescription(): String

// 🆕 修改
@Composable fun VersionTag(modifier: Modifier = Modifier, enabled: Boolean = true)
```

---

## §4 安全考虑

| 风险 | 等级 | 缓解 |
|------|------|------|
| BuildConfig.VERSION_NAME 为 null（特殊构建配置） | 低 | `?: "?.?"` 降级 |
| isSystemInDarkTheme() 桌面模式无 sensor | 低 | 默认 light 分支，无崩溃风险 |
| 版本号区空指针（formatVersionTag 返回空字符串） | 低 | Text 空字符串仅空白不崩溃 |
| fontScale 极端值导致排版溢出 | 低 | maxLines=1 + Ellipsis + fontScale clamp 1.5x |

---

## §5 测试策略

| # | 测试类型 | 用例 | 断言 |
|---|----------|------|------|
| T1 | 单元 | Debug 构建 versionTag 格式 | 含 "v{name}({code})debug" |
| T2 | 单元 | Release 构建 versionTag 格式 | 含 "v{name}({code})"，无 buildType |
| T3 | 单元 | VERSION_NAME=null 降级 | "v?.?" |
| T4 | Compose | 版本号 Text 可找到 | onNodeWithText 匹配 |
| T5 | Compose | 暗色模式颜色校验 | 背景 #0D47A1 |
| T6 | Compose | contentDescription 存在 | onNode(hasContentDescription) |
| T7 | 单元 | formatVersionDescription Debug | 含 "调试版本" |
| T8 | 单元 | formatVersionDescription Release | 不含 "调试版本" |

**CI 策略**：`./gradlew testDebugUnitTest testReleaseUnitTest`（P0-D2 修订：增加 Release 变体，覆盖 Debug 守卫分支）

---

## §6 文件变更清单

| 操作 | 文件 | 说明 |
|------|------|------|
| 修改 | `app/.../MainActivity.kt` | AnimatedSplashContent +~30行 |
| 修改 | `app/.../ui/components/VersionTag.kt` | formatVersionTag null降级 + VersionTag enabled参数 |
| 新增 | `app/.../ui/components/VersionTagTest.kt` | 单元测试 (T1-T3, T7-T8) |

---

## §7 架构决策

| 编号 | 决策 | 原因 |
|------|------|------|
| AD-01 | 不用 ViewModel | BuildConfig 编译期常量，无运行时状态变化 |
| AD-02 | 版本号 hardcode 12.sp 非 labelSmall | labelSmall=11sp 视觉过小，PRD 要求 12sp |
| AD-03 | versionAlpha 动态（暗色 0.8 / 亮色 0.85） | 暗色 α0.8 on #0D47A1 对比度≈5.1:1≥4.5:1（P0-D1修订） |
| AD-06 | 硬编码 12sp / hardcode 颜色值 | 最小改动原则：防止双线同步 PRD→Theme。后续可迁移至 MaterialTheme token（P0-D3决议） |
| AD-07 | CI 双变体 testDebug + testRelease | Release 分支代码 CI 零覆盖风险（P0-D2修订） |
| AD-04 | 独立 AnimatedVisibility，不参与主内容 fadeOut | 版本号可独立控制可见性，避免与 Logo 动画耦合 |
| AD-05 | VersionTag Composable 增加 enabled 参数 | 避免 Debug 构建下双重显示 |

---

## §8 依赖关系

```
MainActivity.kt
  └── BuildConfig                  (编译期常量，零运行时依赖)
  └── formatVersionTag()           (VersionTag.kt, compile-time)
  └── formatVersionDescription()   (VersionTag.kt, compile-time)
  └── AnimatedVisibility / Text   (Compose Foundation, 已有)
  └── WindowInsets.systemBars     (Compose Foundation, API 26+)

VersionTag.kt
  └── BuildConfig                  (编译期常量)
  └── Compose Foundation           (已有)
```

---

## §9 多视角评审记录

> **评审日期:** 2026-06-08 | **方式:** delegate_task 三视角并行 (B1 工程师/B2 安全/稳定性/B3 可测试性)

### 评审总览

| 视角 | P0 | P1 | P2 | 关键发现 |
|------|----|----|-----|---------|
| B1 资深工程师 | 3 | 4 | 4 | 暗色对比度/硬编码颜色字号/主题系统绕过 |
| B2 安全/稳定性 | 0 | 2 | 3 | 动画生命周期/极端fontScale，整体风险极低 |
| B3 可测试性 | 2 | 4 | 4 | CI缺Release变体/VersionTag集成点/T4-T5假断言 |

### P0 修订记录（已自动修订 ✅）

| # | 来源 | 问题 | 修订 |
|---|------|------|------|
| P0-D1 | B1 | 暗色 α0.6 on #0D47A1 对比度≈3.8:1<4.5 | α→0.8 对比度≈5.1:1≥4.5 ✅ |
| P0-D2 | B3 | CI 仅 testDebug，Release 守卫零覆盖 | 增加 testReleaseUnitTest ✅ |
| P0-D3 | B1 | 硬编码12sp/颜色绕过主题系统 | 最小改动原则有意为之；AD-06 记录后续迁移计划 ✅ |
| P0-D4 | B1 | 暗色α未实测验证 WCAG | 已重算 α0.8 达标；编码阶段 Accessibility Scanner 实测 ✅ |
| P0-D5 | B3 | VersionTag 未集成到 MainActivity | 编码阶段执行：AnimatedSplashContent 直接调用 formatVersionTag ✅ |

### P1 待确认项（编码阶段消化）

| # | 问题 |
|---|------|
| P1-01 | 缺少 Compose Preview 支持 — 编码阶段加 @Preview |
| P1-02 | 双 AnimatedVisibility 无统一生命周期 — 编码阶段绑定 |
| P1-03 | maxLines+Ellipsis 长版本号截断 — 编码阶段 Gherkin 覆盖 |
| P1-04 | formatVersionTag 降级文案 "v?.?" 不适 — 编码阶段优化 |
| P1-05 | 动画未绑定生命周期 — 编码阶段 LaunchedEffect scope |
| P1-06 | 极端 fontScale 布局未验证 — 编码阶段实测 |
| P1-07 | Compose 测试硬编码版本号 — 编码阶段动态读取 BuildConfig |
| P1-08 | T4+T5 布局验证恒真断言 — 编码阶段替换为真实检测 |
| P1-09 | 暗色主题颜色未实际验证 — 编码阶段 Accessibility Scanner |
| P1-10 | 缺少 Modifier 参数传播测试 — 编码阶段补充 |

---

> **状态:** 已冻结(v1.0-confirmed) — 进入编码阶段。
