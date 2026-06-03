# 启动页面增加版本号显示功能 — 技术方案

> **版本:** v0.1-draft
> **功能名称:** 启动页面增加版本号显示功能
> **基于:** PRD v1.0-confirmed | UI_DESIGN v1.0-confirmed

---

## §1 架构概览

```
MainActivity (Box)
  ├── LoginScreen (现有, fillMaxSize)
  └── VersionTag (修改, BottomCenter)
        └── Text( formatVersionTag(), 12sp, onSurfaceVariant )

无新增模块。仅修改 1 个组件 + 1 个纯函数。
```

**策略**：最小改动 — 移除 VersionTag 的 DEBUG 守卫 + formatVersionTag() 内部增加条件分支，Release 渲染但省略 buildType 后缀。

---

## §2 模块设计

### 2.1 VersionTag 组件修改

**文件:** `app/src/main/java/com/example/myandroidapp/ui/components/VersionTag.kt`

```kotlin
// 变更 1: formatVersionTag() 增加条件分支
internal fun formatVersionTag(
    versionName: String = BuildConfig.VERSION_NAME,
    versionCode: Int = BuildConfig.VERSION_CODE,
    buildType: String = BuildConfig.BUILD_TYPE
): String {
    val suffix = if (BuildConfig.DEBUG) buildType else ""  // ← 修改
    return "v$versionName($versionCode)$suffix"
}

// 变更 2: VersionTag Composable 移除 DEBUG 守卫
@Composable
fun VersionTag(modifier: Modifier = Modifier) {
    // 移除: if (!BuildConfig.DEBUG) return
    
    val versionText = formatVersionTag()
    Text(
        text = versionText,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(bottom = 8.dp)
            .imePadding()  // ← UR-01: 防键盘重叠
            .semantics {
                contentDescription = formatVersionDescription()
            }
    )
}
```

### 2.2 无其他模块变更

- **MainActivity.kt**: 无需修改（VersionTag 已在 Box 中调用）
- **LoginScreen**: 无需修改
- **无新增文件**

### 2.3 数据流

```
BuildConfig (编译时常量)
  └── VERSION_NAME / VERSION_CODE / BUILD_TYPE / DEBUG
        └── formatVersionTag() → String
              └── VersionTag Text → 渲染到屏幕
```

无网络/IO/数据库依赖，零运行时开销。

---

## §3 接口定义

无新增接口。修改现有内部函数：

```kotlin
// 修改前: "v$versionName($versionCode)$buildType"
// 修改后: "v$versionName($versionCode)" + if (BuildConfig.DEBUG) buildType else ""

// formatVersionDescription() 不变
internal fun formatVersionDescription(versionName: String = BuildConfig.VERSION_NAME): String =
    "应用版本号 v$versionName"
```

---

## §4 安全考虑

| 风险 | 等级 | 缓解 |
|------|------|------|
| versionName 为空字符串 | 低 | BuildConfig 保障非空；若为空则显示 `v(42)` |
| versionCode 为负值 | 低 | Android 构建系统保障 ≥ 0 |
| R8 误删间接引用 | 低 | `formatVersionTag()` 由 `VersionTag` Composable 直接引用，R8 不会误删；实测 Release 构建验证 |

---

## §5 测试策略

| # | 测试类型 | 用例 | 断言 |
|---|----------|------|------|
| T1 | 单元测试 | formatVersionTag Debug | `assertEquals("v1.0.0(42)debug", formatVersionTag("1.0.0", 42, "debug"))` |
| T2 | 单元测试 | formatVersionTag Release | `assertEquals("v1.0.0(42)", formatVersionTag("1.0.0", 42, "release"))` |
| T3 | 单元测试 | VersionTag Composable Debug 渲染 | 验证文本节点 `v1.0.0(1)debug` 存在 |
| T4 | 单元测试 | VersionTag Composable Release 渲染 | 验证文本节点 `v1.0.0(1)` 存在（不含 buildType） |
| T5 | 单元测试 | 空 versionName | `assertEquals("v(42)", formatVersionTag("", 42, "release"))` |

**CI 策略**：`./gradlew testDebugUnitTest testReleaseUnitTest`（双变体；Release 精确排除 `*VersionTagComposeTest*`，保留纯函数测试）

---

## §6 文件变更清单

| 操作 | 文件 | 说明 |
|------|------|------|
| 修改 | `app/.../ui/components/VersionTag.kt` | 移除 DEBUG 守卫 + formatVersionTag 条件分支 |
| 修改 | `app/.../ui/components/VersionTag.kt` | `VersionTag` 接受可选参数 `versionText`/`contentDesc` 用于测试注入 |
| 修改 | `app/src/test/.../VersionTagTest.kt` | 删除 T2/T4 永真断言；T2 验证 Release 省略 buildType；新增 T5 空 versionName |
| 修改 | `app/src/test/.../VersionTagComposeTest.kt` | 预期文本用 `formatVersionTag()` 动态构造（去硬编码）；新增 Release 渲染验证 |
| 修改 | `app/build.gradle.kts` | `testReleaseUnitTest` 精确排除 `*VersionTagComposeTest*`（保留纯函数测试） |
| 修改 | `app/.../MainActivity.kt` | 给 Box 容器追加 `Modifier.imePadding()`（父容器层） |

---

## §7 架构决策

| 编号 | 决策 | 原因 |
|------|------|------|
| AD-01 | 在 formatVersionTag() 内使用编译期 `if (DEBUG)` 分支而非运行时参数 | R8 在 Release 消除死分支，零运行时开销 |
| AD-02 | `Modifier.imePadding()` 作用于 MainActivity Box 父容器层 | UR-01 决议，防键盘弹出时全局避让（非单一 Text） |
| AD-03 | 不新增抽象层/接口 | 改动极小（~5行），引入抽象层得不偿失 |
| AD-04 | `VersionTag` 接受可选参数 `versionText`/`contentDesc` 支持测试注入 | B3 P1-4，提升 Composable 可测试性 |
| AD-05 | CI 双变体覆盖 `testDebugUnitTest testReleaseUnitTest` | B3 P0-1/P0-2，保证 Release 行为被验证 |

---

## §8 依赖关系

```
VersionTag.kt
  └── BuildConfig (compile-time, Gradle 生成)
  └── MaterialTheme.colorScheme (runtime, Material3)
  └── WindowInsets (runtime, Compose UI)
```

无新增依赖。

---

## §9 多视角评审记录

> **评审日期:** 2026-06-03 | **方式:** delegate_task 三视角并行 (B1 工程师/B2 安全/B3 可测试性)

### 评审总览

| 视角 | 评分 | P0 | P1 | P2 | 关键发现 |
|------|------|----|----|-----|---------|
| B1 资深工程师 | 7/10 | 0 | 3 | 2 | imePadding 放错位置、测试断裂 |
| B2 安全/稳定性 | 9/10 | 0 | 0 | 3 | 极低风险，仅缺防御性空串检查 |
| B3 可测试性 | 4/10 | 3 | 4 | 4 | CI 不覆盖 Release + 永真断言 |

### P0 修订记录（已自动修订 ✅）

| # | 来源 | 问题 | 修订 |
|---|------|------|------|
| D1 | B3 P0-1 | CI 仅 `testDebugUnitTest`，Release 行为零覆盖 | §5 CI 策略改为双变体 `testDebugUnitTest testReleaseUnitTest` |
| D2 | B3 P0-2 | `build.gradle.kts` afterEvaluate 排除 Release Compose 测试 | §6 精确排除 `*VersionTagComposeTest*` 保留纯函数测试 |
| D3 | B3 P0-3 | `assertTrue(true)` 永真断言无验证价值 | §6 VersionTagTest.kt 删除永真断言并替换为实际验证 |
| D4 | B1 P1-3 | `imePadding()` 仅作用于 VersionTag Text 而非整体布局 | AD-02 改为 MainActivity Box 父容器层；§6 MainActivity.kt 追加父容器 imePadding |
| D5 | B3 P1-4 | Composable 不可注入测试数据 | AD-04 VersionTag 接受可选参数 `versionText`/`contentDesc` |
| D6 | B3 P1-1 | T5 空 versionName 未实现 | §5 T5 保留，§6 新增实现 |

### P1 待确认项（编码阶段消化）

| # | 问题 |
|---|------|
| P1-B1-1 | formatVersionTag 测试断裂：T1/T2 在 Release 变体期望值不匹配（已在 §6 列入修改范围） |
| P1-B1-2 | Release Compose 测试缺失（已在 §6 列入修改范围） |
| P1-B2-1 | 防御性空串检查 `require(versionName.isNotBlank())`（编者注：BuildConfig 保障非空，P2 级别） |
| P1-B3-2 | Compose 测试硬编码 `"v1.0(1)debug"`（已在 §6 改为 `formatVersionTag()` 动态构造） |
| P1-B3-3 | T3/T4 归类错误（标记为单元测试，实为集成测试 — 已在 §5 标注，不影响实现） |

### P2 建议项

| # | 问题 |
|---|------|
| P2-1 | `formatVersionTag()` 职责分离：BuildConfig.DEBUG 分支上移调用侧（编者注：纯函数更干净但当前方案简单可行） |
| P2-2 | `proguard-rules.pro` 文件缺失但 `build.gradle.kts` 引用 |
| P2-3 | 缺少 `imePadding()` 专项测试 |
| P2-4 | 颜色验证形同虚设（注释自认"验证节点存在即确认 Token 生效"） |

---

> **状态:** 待确认 (v0.1-draft) — 3 项 P0 + 3 项 P1 已自动修订。请审阅后回复「确认」冻结进入编码。
