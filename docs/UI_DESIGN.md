# 启动页面版本号显示 — UI 设计方案

> **版本:** v0.1-draft
> **功能名称:** 启动页面增加版本号显示功能
> **创建日期:** 2026-06-03
> **来源PRD:** docs/PRD.md (v1.0-confirmed, §9)
> **模式:** 快速通道（工作流验证）

---

## 1. 页面清单

| 页面 | 路由/Activity | 说明 | 变更范围 |
|------|---------------|------|----------|
| 主页面 | MainActivity | 现有登录页面，Box 底部已有 VersionTag 组件 | 仅修改 VersionTag 行为 |

## 2. 线框图

```
┌─────────────────────────────────┐ 375dp
│  Status Bar                     │
├─────────────────────────────────┤
│                                 │
│         ┌──────────┐            │
│         │   Logo   │ (现有)     │
│         └──────────┘            │
│                                 │
│       ┌─────────────────┐       │
│       │   登录  (headline)│      │
│       └─────────────────┘       │
│                                 │
│    ┌──────────────────────┐     │
│    │  手机号输入框          │     │
│    └──────────────────────┘     │
│    ┌──────────────────────┐     │
│    │   获取验证码 (Button)  │     │
│    └──────────────────────┘     │
│                                 │
│                                 │
│         v1.0.0(42)              │ ← 版本号
│         ^^^^^^^^                │    onSurfaceVariant
│    (8dp + navBars inset)        │    12sp, 单行, ellipsis
├─────────────────────────────────┤
│  Navigation Bar                 │
└─────────────────────────────────┘
```

## 3. 组件层级树

```
MainActivity
└── MaterialTheme
    └── Surface(fillMaxSize)
        └── Box(fillMaxSize)
            ├── LoginScreen(fillMaxSize)          ← 现有
            │   └── Column(padding=16dp)
            │       ├── Text("登录", headlineMedium)
            │       ├── TextField(手机号)
            │       └── Button("获取验证码")
            └── VersionTag(BottomCenter)          ← 修改
                └── Text(
                        text = formatVersionTag(),
                        fontSize = 12.sp,
                        color = onSurfaceVariant,
                        maxLines = 1,
                        overflow = Ellipsis
                    )
```

## 4. 交互状态机

```
                     ┌──────────────────────────────┐
                     │          Default              │
                     │  静态展示版本号文本             │
                     │  (无交互)                     │
                     └──────────────────────────────┘
                              │ 构建变体分支
              ┌───────────────┴───────────────┐
              ▼                               ▼
     ┌────────────────┐              ┌────────────────┐
     │  Debug 构建     │              │  Release 构建   │
     │ v1.0.0(42)debug│              │ v1.0.0(42)     │
     │ 含 buildType    │              │ 省略 buildType  │
     └────────────────┘              └────────────────┘
```

> **说明**：VersionTag 为纯展示组件，无交互状态切换。唯一的行为差异由编译时常量 `BuildConfig.DEBUG` 决定。

## 5. 状态覆盖表

| 状态 | Debug 输出 | Release 输出 | 说明 |
|------|-----------|-------------|------|
| 正常 | `v1.0.0(42)debug` | `v1.0.0(42)` | 编译期分支 |
| 长版本名 | `v1.2.3-alpha.rc1(12345)debug` → ellipsis 截断 | `v1.2.3-alpha.rc1(12345)` → ellipsis 截断 | 单行截断 |
| 深色主题 | 颜色自动适配 `onSurfaceVariant` | 同左 | M3 token 自动处理 |
| 浅色主题 | 颜色自动适配 `onSurfaceVariant` | 同左 | M3 token 自动处理 |
| 空 versionName | `v(42)debug` | `v(42)` | BuildConfig 保障非空 |

## 6. Token 映射表

| Token | PRD §9 值 | Compose 实现 | 备注 |
|-------|----------|-------------|------|
| 字体大小 | 12sp | `12.sp` | 保持现有 |
| 字体颜色 | `onSurfaceVariant` | `MaterialTheme.colorScheme.onSurfaceVariant` | M3 自适应深浅色 |
| 对齐 | 水平居中 | `Alignment.BottomCenter` | Box 布局 |
| 行数 | 单行 | `maxLines = 1` | 保持现有 |
| 溢出 | ellipsis | `TextOverflow.Ellipsis` | 保持现有 |
| 底部内边距 | 8dp | `.padding(bottom = 8.dp)` | 保持现有 |
| 系统栏适配 | navigationBars | `windowInsetsPadding(WindowInsets.navigationBars)` | 保持现有 |
| 无障碍 | `"应用版本号 v{name}"` | `semantics { contentDescription = formatVersionDescription() }` | 保持现有 |

## 7. 组件复用分析

| 组件 | 文件 | 类型 | 说明 |
|------|------|------|------|
| VersionTag | `ui/components/VersionTag.kt` | 修改 | 移除 `if (!BuildConfig.DEBUG) return` |
| formatVersionTag() | `ui/components/VersionTag.kt` | 修改 | 增加 Debug/Release 分支 |
| formatVersionDescription() | `ui/components/VersionTag.kt` | 复用 | 不变 |

**复用率：1/3 直接复用，2/3 需修改。新增组件：0。**

## 8. 新增组件清单

无。仅修改现有组件，无需新增文件。

## 9. 架构协调设计

- **无跨 Screen 通信**：VersionTag 位于 MainActivity 底部，独立运行
- **无 BackHandler 冲突**：VersionTag 无交互，不消费返回事件
- **无 z-order 碰撞**：VersionTag 作为 Box 最顶层子元素，但无 FAB/Snackbar 等底部交互控件（当前 MainActivity 不含此等组件）
- **编译期安全**：`BuildConfig.DEBUG` 为编译时常量，R8/ProGuard 在 Release 构建时自动消除 dead code 分支

---

## 10. 变更记录

| 版本 | 日期 | 变更 |
|------|------|------|
| v0.1-draft | 2026-06-03 | 初始生成（快速通道） |

---

## 11. 多视角评审记录

> 评审日期: 待评审
> 评审方式: 3-Agent 并行自评审（UX/视觉/前端）

（将在阶段4汇总后填充）

---

## 12. 评审决议

（将在阶段4汇总后填充）

---

> **状态:** 待评审 (v0.1-draft) — 自动进入三视角评审
