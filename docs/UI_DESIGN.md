# 启动页面版本号显示 — UI 设计方案

> **版本:** v0.2-review
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
| 键盘适配 | 软键盘弹出时上移 | `Modifier.imePadding()` | 新增（P0修复） |

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
- **软键盘适配**：LoginScreen 含 TextField，键盘弹出时 VersionTag 可能重叠。方案：给 VersionTag 追加 `Modifier.imePadding()`，确保键盘弹出时随内容上移（或在 Box 中通过 `Modifier.offset` 动态调整）
- **编译期安全**：`BuildConfig.DEBUG` 为编译时常量，R8/ProGuard 在 Release 构建时自动消除 dead code 分支
- **设计-代码一致性**：当前代码 `if (!BuildConfig.DEBUG) return` 在 Release 完全不渲染。方案已明确：移除该守卫 + `formatVersionTag()` 内部通过 `if (DEBUG) buildType else ""` 控制后缀，Release 渲染但省略 buildType

---

## 10. 变更记录

| 版本 | 日期 | 变更 |
|------|------|------|
| v0.1-draft | 2026-06-03 | 初始生成（快速通道） |
| v0.2-review | 2026-06-03 | 三视角评审完成，P0 自动修订（软键盘适配 + 设计-代码一致声明） |

---

## 11. 多视角评审记录

> 评审日期: 2026-06-03
> 评审方式: 3-Agent 并行自评审（delegate_task）
> 模式: 快速通道（纯文本视觉评审，无 Playwright 截图）

### 11.1 评审总览

| 视角 | 评分 | P0 项 | P1 项 | 结论 |
|------|------|-------|-------|------|
| C1 UX 交互 | 5/10 | 2 | 5 | 骨架正确但关键细节缺失 |
| C2 视觉审美 | 36.5/50 | 2 | 6 | 有条件通过（≥30），Token 体系成熟 |
| C3 前端实现 | 8/10 | 0 | 1 | 可行，2 处代码改动 |

### 11.2 C1 UX 交互评审

| 编号 | 严重度 | 问题 | 修订状态 |
|------|--------|------|----------|
| P0-1 | 致命 | 设计-代码不一致：当前 `if (!BuildConfig.DEBUG) return` 在 Release 完全不渲染 | ✅ 已修订 §9 |
| P0-2 | 致命 | 软键盘遮挡：TextField 弹出时 VersionTag 可能重叠 | ✅ 已修订 §6/§9（追加 imePadding） |
| P1-1 | 重要 | 字体缩放未防御（200% 缩放 12sp→24sp 溢出） | 📋 记录 |
| P1-2 | 重要 | RTL 未覆盖 | 📋 记录 |
| P1-3 | 重要 | TalkBack 描述缺少 versionCode | 📋 记录 |
| P1-4 | 重要 | 12sp 低于可读性建议 | 📋 记录 |
| P1-5 | 重要 | 横屏/分屏/折叠屏未验证 | 📋 记录 |

### 11.3 C2 视觉审美评审

⚠️ 基于 HTML 源码纯文本推断，非截图验证。

| 评分维度 | 得分 |
|----------|:---:|
| 格式塔感知 | 4/5 |
| 视觉层级 | 4/5 |
| 色彩系统 | 5/5 |
| 字体排版 | 3/5 |
| 空间与网格 | 3/5 |
| 布局与比例 | 3/5 |
| 可感知可操作 | 4/5 |
| 一致性 | 4/5 |
| 情感与品牌 | 3/5 |
| 平台与适配 | 3.5/5 |
| **综合** | **36.5/50** |

| 严重度 | 问题 | 说明 |
|:---:|------|------|
| 🔴高 | 字体排版 | 仅声明字号，无字重/行高/letter-spacing |
| 🔴高 | 空间与网格 | 各组块间距、水平 padding 未定义 |
| 🟡中 | 布局比例 | logo 80dp 可能过小 |
| 🟡中 | 色彩系统 | #6750A4 非标准 M3 purple |
| 🟡中 | 品牌 | 纯色圆形无差异化 |
| 🟢低 | 响应式 | 仅 375dp 单断点 |

**审美亮点**：Token 体系成熟（明暗双模+ripple alpha 适配）、版本号定位考究（absolute 底部居中 + onSurfaceVariant）、手机框 375×812 模拟真实。

### 11.4 C3 前端实现评审

| 严重度 | 问题 | 说明 |
|------|------|------|
| P1 | formatVersionTag() R8 分支 | `if(DEBUG)` 编译时常量，R8 自动消除死分支，无运行时开销 |

**组件可行性**：✅ 简单（1 个 Text + 纯函数格式化器），低复杂度，零额外性能开销。
**架构适配**：Box 底部 8dp+navBars 提供 ≥48dp 安全区。无跨 Screen 通信/BH 冲突。

---

## 12. 评审决议

| 决议编号 | 决议内容 | 来源 | 状态 |
|----------|----------|------|------|
| UR-01 | VersionTag 追加 `Modifier.imePadding()` 防止键盘弹出时重叠 | C1 P0-2 | ✅ 已修订 §6/§9 |
| UR-02 | §9 增加设计-代码一致性声明（当前 Release 不渲染 → 需移除守卫） | C1 P0-1 | ✅ 已修订 §9 |
| UR-03 | HTML 预览补充字重/行高/间距声明（下一迭代） | C2 P0 | 📋 记录 |
| UR-04 | 字体缩放防御方案（下一迭代评估） | C1 P1-1 | 📋 记录 |

---

> **状态:** 三视角评审完成，P0 已自动修订。请审阅后回复「确认」冻结进入技术方案。
