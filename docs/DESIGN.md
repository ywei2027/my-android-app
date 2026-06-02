# 主界面底部版本号显示 — 技术方案

> **版本:** v0.1-draft
> **功能名称:** 主界面底部版本号显示
> **基于:** PRD v1.0-confirmed | UI_DESIGN v1.0-confirmed

---

## §1 架构概览

```
MainActivity
  └── Box(fillMaxSize)
      ├── LoginScreen (fills area, top-aligned)    ← 现有
      └── VersionTag (BottomCenter)                 ← [新增]
          └── Text("v1.0(1)debug")
```

**策略**：最小化变更。在 MainActivity 的 `Box` 中 bottom-align VersionTag，与 LoginScreen 解耦。不引入 Scaffold 架构重组。

---

## §2 模块设计

### 2.1 VersionTag 组件

```kotlin
// app/src/main/java/com/example/myandroidapp/ui/components/VersionTag.kt
@Composable
fun VersionTag(modifier: Modifier = Modifier) {
    // D-12: Release 构建不显示
    if (!BuildConfig.DEBUG) return

    // D-15: v{name}({code}){buildType}
    val versionText = "v${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})${BuildConfig.BUILD_TYPE}"

    Text(
        text = versionText,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,  // D-13
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)  // D-16
            .padding(bottom = 8.dp)                             // D-20
            .semantics { contentDescription = "应用版本号 v${BuildConfig.VERSION_NAME}" }  // D-17
    )
}
```

### 2.2 MainActivity 集成

```kotlin
// MainActivity.kt — 变更: Column → Box + 追加 VersionTag
setContent {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                LoginScreen(
                    modifier = Modifier.fillMaxSize()
                )
                VersionTag(
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
```

| 模块 | 类型 | 路径 |
|------|------|------|
| VersionTag | 新增 Composable | `ui/components/VersionTag.kt` |
| MainActivity | 修改 | `MainActivity.kt` — Column→Box + 追加 VersionTag |

### 2.3 数据流

```
BuildConfig (编译时常量: VERSION_NAME, VERSION_CODE, BUILD_TYPE, DEBUG)
        │
        ▼
   VersionTag.kt (纯静态渲染，Debug 条件 + 格式组装)
        │
        ▼
   Text("v1.0(1)debug", 12sp, onSurfaceVariant)
```

---

## §3 接口定义

```kotlin
@Composable fun VersionTag(modifier: Modifier = Modifier)
```

无额外接口。所有输入来自编译时常量，零外部依赖。

---

## §4 安全考虑

| 风险 | 等级 | 缓解 |
|------|------|------|
| Release 包暴露版本信息 | 低 | `if (!BuildConfig.DEBUG) return` |
| VERSION_CODE 暴露 | 已接受 | D-15 要求包含 CODE，仅 Debug 可见 |
| BuildConfig 反编译可读 | 固有 | 编译时常量在字节码中可读，非方案缺陷 |

---

## §5 测试策略

| # | 测试类型 | 用例 | 断言 |
|---|----------|------|------|
| T1 | Compose Rule | Debug 渲染 | Text("v1.0(1)debug") 存在 |
| T2 | Compose Rule | Release 不渲染 | 无 Text 节点（独立 Variant 运行） |
| T3 | Compose semantics | contentDescription | `onNodeWithContentDescription("应用版本号 v1.0")` 存在 |
| T4 | Compose layout | 底部定位 | `assertPositionInRoot` 验证 BottomCenter |
| T5 | Compose layout | WindowInsets | 底部 padding ≥ 导航栏高度 + 8dp |

**CI 策略**：`./gradlew testDebugUnitTest testReleaseUnitTest` 双 Variant 覆盖。

---

## §6 文件变更清单

| 操作 | 文件 | 行数变化 |
|------|------|----------|
| 新增 | `app/.../ui/components/VersionTag.kt` | +18 |
| 修改 | `app/.../MainActivity.kt` | +5（Column→Box + VersionTag） |

**总变更：2 文件，~23 行。**

---

## §7 架构决策

| 编号 | 决策 | 原因 |
|------|------|------|
| AD-01 | VersionTag 与 LoginScreen 解耦（Box bottom-align） | 避免 LoginScreen 携带版本号逻辑 |
| AD-02 | `BuildConfig.DEBUG` 条件编译 | 对齐 D-12 |
| AD-03 | `onSurfaceVariant` Token | 对齐 D-13 |
| AD-04 | 12sp + 8dp | 对齐 PRD §9 + D-20 |
| AD-05 | 无 ViewModel | 纯静态组件 |
| AD-06 | `v{name}({code}){buildType}` 格式 | 对齐 D-15 |
| AD-07 | `WindowInsets.navigationBars` | 对齐 D-16，防手势导航遮挡 |
| AD-08 | `maxLines=1 + TextOverflow.Ellipsis` | 超长版本号安全 |

---

## §8 依赖关系

```
VersionTag
  └── BuildConfig.VERSION_NAME/CODE/BUILD_TYPE/DEBUG (compile-time)
  └── MaterialTheme.colorScheme.onSurfaceVariant (runtime)
  └── WindowInsets.navigationBars (runtime)
  └── Modifier.semantics (Compose UI)
```

---

## §9 多视角评审记录

> **评审日期:** 2026-06-02 | **方式:** delegate_task 三视角并行 (B1 工程师/B2 安全/B3 可测试性)

### 评审总览

| 视角 | P0 | P1 | P2 | 关键发现 |
|------|----|----|-----|---------|
| B1 资深工程师 | 2 | 4 | 4 | D-15 格式缺失、D-16 WindowInsets 缺失 |
| B2 安全/稳定性 | 1 | 3 | 3 | WindowInsets 遮挡、无溢出处理 |
| B3 可测试性 | 4 | 5 | 5 | BuildConfig 直接依赖导致测试隔离崩溃 |

### P0 修订记录（已自动修订 ✅）

| # | 来源 | 问题 | 修订 |
|---|------|------|------|
| P0-01 | B1/B2 | D-15: 版本格式缺少 VERSION_CODE+buildType | §2: `"v${name}(${code})${buildType}"` |
| P0-02 | B1/B2 | D-16: WindowInsets 适配缺失 | §2: `.windowInsetsPadding(WindowInsets.navigationBars)` |
| P0-03 | B1 | VersionTag 与 LoginScreen 耦合 | §2: Box bottom-align 解耦 |
| P0-04 | B2 | 长版本号无溢出处理 | §2: `maxLines=1, TextOverflow.Ellipsis` |
| P0-05 | B3 | 测试策略空壳化 | §5: 5 项具体测试用例 + CI 策略 |

### P1 待确认项（编码阶段消化）

| # | 问题 |
|---|------|
| P1-01 | darkColorScheme 未配置（深色模式 Token 依赖默认值） |
| P1-02 | Release 构建 `return` 空导致 Composition 树不一致 |
| P1-03 | `contentDescription` 不含具体版本号（已修正为含版本号） |

---

> **状态:** 待确认 (v0.1-draft) — 5 项 P0 已自动修订，请审阅后回复「确认」冻结进入编码。
