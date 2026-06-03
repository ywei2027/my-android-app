# 关于页面增加版本号显示功能 — 技术方案

> **版本:** v1.0-confirmed
> **功能名称:** 关于页面增加版本号显示功能
> **基于:** PRD v1.0-confirmed | UI_DESIGN v1.0-confirmed

---

## §1 架构概览

```
AboutScreen (Composable)
└── Scaffold
    ├── TopAppBar (返回 + "关于")
    └── Column
        ├── AppIcon
        ├── AppName
        ├── AppDescription
        ├── Divider
        ├── VersionText ← 复用 formatVersionTag() 逻辑
        ├── Divider
        └── InfoItem × 4

版本号获取: BuildConfig → formatVersionTag() → 纯展示
导航: LoginScreen → navController.navigate("about") → popBackStack()
```

**策略**: 纯 UI 组件，无业务逻辑，无 ViewModel，版本号通过 `BuildConfig` 编译时常量直接渲染。

---

## §2 模块设计

### 2.1 AboutScreen

```kotlin
// app/src/main/java/.../ui/screen/about/AboutScreen.kt
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(title = { Text("关于") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Default.ArrowBack, "返回") } })
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp), horizontalAlignment = CenterHorizontally) {
            Spacer(32.dp)
            Image(painterResource(R.drawable.ic_launcher), "应用图标", modifier = Modifier.size(80.dp))
            Spacer(16.dp)
            Text("我的应用", style = MaterialTheme.typography.headlineMedium)
            Spacer(4.dp)
            Text("智能笔记助手", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(24.dp)
            HorizontalDivider()
            Spacer(16.dp)
            VersionText()
            Spacer(16.dp)
            HorizontalDivider()
            Spacer(16.dp)
            InfoItem("关于我们") { /* 跳转 */ }
            InfoItem("用户协议") { /* 跳转 */ }
            InfoItem("隐私政策") { /* 跳转 */ }
            InfoItem("开源许可") { /* 跳转 */ }
        }
    }
}
```

### 2.2 VersionText

```kotlin
// app/src/main/java/.../ui/screen/about/VersionText.kt
@Composable
fun VersionText(modifier: Modifier = Modifier) {
    val version = formatVersionTag() // 复用已有格式化逻辑，对齐 VersionTag.kt
    SelectionContainer {
        Text(
            text = version,
            modifier = modifier,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}
```

### 2.3 InfoItem

```kotlin
// app/src/main/java/.../ui/screen/about/InfoItem.kt
@Composable
fun InfoItem(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp).clickable(onClick = onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Default.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
```

### 2.4 导航接线

```kotlin
// app/src/main/java/.../navigation/AppNavHost.kt — 已有路由表追加
composable("about") { AboutScreen(onBack = { navController.popBackStack() }) }

// LoginScreen 中"关于"按钮
TextButton(onClick = { navController.navigate("about") }) { Text("关于") }
```

---

## §3 接口定义

无公开 API 接口。所有组件为内部 Composable，事件通过 lambda 回调（onClick、onBack）传递。

---

## §4 安全考虑

| 风险 | 等级 | 缓解 |
|------|------|------|
| 无用户输入 | 低 | 版本号只读，无注入风险 |
| 无网络请求 | 低 | 离线功能 |
| 无敏感数据 | 低 | 版本号为公开信息 |

---

## §5 测试策略

| # | 测试类型 | 用例 | 断言 |
|---|----------|------|------|
| T1 | Unit | VersionText 显示格式 | 精确匹配 `v{name}({code}){buildType}` 格式（参数化测试覆盖多个版本号场景） |
| T2 | UI | AboutScreen 内容完整性 | 页面包含"关于"标题 + 版本号文本 + 4 个 InfoItem |
| T3 | UI | 返回操作 | 点击返回 → popBackStack |
| T4 | Visual | @Preview dark/light 不崩溃 | 无渲染异常 |
| T5 | Unit | InfoItem 渲染与交互 | text 显示正确 + onClick 触发回调 |
| T6 | Unit | NavHost about 路由注册 | navigate("about") → 到达 AboutScreen |
| T7 | UI | AboutScreen 滚动到底部 | 末尾 InfoItem("开源许可") 在滚动后可见可点击 |

**CI 策略**: `./gradlew testDebugUnitTest`

---

## §6 文件变更清单

| 操作 | 文件 | 说明 |
|------|------|------|
| 新增 | `AboutScreen.kt` | 关于页面 Composable |
| 新增 | `VersionText.kt` | 版本号展示组件 |
| 新增 | `InfoItem.kt` | 信息行复用组件 |
| 修改 | `AppNavHost.kt` | 追加 `about` 路由 |
| 修改 | 入口页面（LoginScreen） | 添加「关于」TextButton |

---

## §7 架构决策

| 编号 | 决策 | 原因 |
|------|------|------|
| AD-01 | 使用 BuildConfig 替代 PackageManager | 编译时常量，零运行时开销，对齐已有 VersionTag.kt 方案 |
| AD-02 | 不引入 ViewModel | 版本号为静态数据，LaunchedEffect(Unit) 一次性读取即可 |
| AD-03 | textIsSelectable 使用 SelectionContainer | Compose 中通过 `SelectionContainer { Text(...) }` 而非 Android View 的 textIsSelectable 实现 |
| AD-04 | onSurfaceVariant + 0.6 opacity | 视觉层级对齐 D-19，与现有其他页面次级信息风格一致 |
| AD-05 | 复用 formatVersionTag() 函数 | 直接调用已有函数，避免版本格式化逻辑重复维护 |

---

## §8 依赖关系

```
app module
  └── Material3 (compile) — Scaffold, TopAppBar, HorizontalDivider
  └── BuildConfig (compile-time) — VERSION_NAME, VERSION_CODE
```

无额外第三方依赖引入。

---

## §9 多视角评审记录

> **评审日期:** 2026-06-02 | **方式:** delegate_task 三视角并行 (B1 工程师/B2 安全/B3 可测试性)

### 评审总览

| 视角 | 评分 | P0 | P1 | P2 | 关键发现 |
|------|:----:|:--:|:--:|:--:|----------|
| B1 资深工程师 | **8.5/10** | 0 | 3 | 3 | 方案合理，SelectionContainer 替代 textIsSelectable、InfoItem 空 lambda |
| B2 安全/稳定性 | **通过 ✅** | 0 | 0 | 0 | 零风险，已逐项排查（ProGuard/导航/竞态/兼容性） |
| B3 可测试性 | — | 3 | 4 | 0 | 复用 formatVersionTag()、测试断言精确化、新增 T5-T7 |

### P0 修订记录（已自动修订 ✅）

| # | 来源 | 问题 | 修订 |
|---|------|------|------|
| P0-1 | B3 | VersionText 重写 format 逻辑而非复用 formatVersionTag() | 改为调用 `formatVersionTag()` + `SelectionContainer` 包裹 |
| P0-2 | B3 | T1 断言过弱 | 改为精确格式匹配 + 参数化测试 |
| P0-3 | B3 | 测试用例不足 | 新增 T5(InfoItem)、T6(路由注册)、T7(滚动到底部) |

### P1 待确认项（编码阶段消化）

| # | 问题 |
|---|------|
| P1-1 | BuildConfig.DEBUG 在 AGP 8+ 已废弃，确认项目 AGP 版本后考虑用 BUILD_TYPE |
| P1-2 | InfoItem × 4 点击回调需填入真实导航路由 |
| P1-3 | CI 策略考虑 Release 变体 `testReleaseUnitTest` |

---

> **版本:** v1.0-confirmed
> **状态:** 已冻结，进入编码阶段。GitHub Issues 已创建。
