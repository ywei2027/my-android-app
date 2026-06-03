# 启动页面版本号显示 — PRD

> **版本:** v1.0-confirmed
> **功能名称:** 启动页面版本号显示
> **创建日期:** 2026-06-03
> **作者:** Hermes 智能研发工作流

---

## §1 功能概述

**概念映射:** 用户术语「启动页面」→ 代码实体 `MainActivity`（无独立 SplashScreen，MainActivity 直接渲染 LoginScreen + 底部 VersionTag）。

**现状分析:** 代码库已有 `VersionTag` Composable，在 `MainActivity` 底部通过 `Modifier.align(Alignment.BottomCenter)` 渲染，格式为 `v{versionName}({versionCode}){buildType}`（如 `v1.0(1)debug`）。**关键约束:** `VersionTag` 内置 `if (!BuildConfig.DEBUG) return`，仅在 Debug 构建显示。

**需求明确:** 本需求目标是在 Release 构建中同样显示版本号，同时保持 Debug 构建的完整格式（含 versionCode 和 buildType）。Release 构建仅显示 `v{versionName}`（无 buildType 后缀）。

## §2 用户场景

| 场景编号 | 角色 | 场景描述 |
|----------|------|----------|
| US-01 | 普通用户 | 启动应用时在底部看到版本号（如 `v1.0`），了解当前版本 |
| US-02 | 测试人员 | 启动应用时看到完整版本信息（如 `v1.0(1)debug`），快速确认构建 |

## §3 范围边界

### 包含
- Debug 构建：底部显示 `v{versionName}({versionCode}){buildType}`
- Release 构建：底部显示 `v{versionName}`
- 版本号从 BuildConfig 读取
- 无障碍 TalkBack 朗读 "应用版本号 v{versionName}"

### 不包含
- 版本更新检测/提示
- 版本号点击交互
- 从远程获取版本信息

## §4 验收标准

| 编号 | 验收项 | 预期结果 |
|------|--------|----------|
| AC-01 | Debug 版本号显示 | 显示 `v{versionName}({versionCode}){buildType}`（如 `v1.0(1)debug`） |
| AC-02 | Release 版本号显示 | 显示 `v{versionName}`（如 `v1.0`），无 buildType 后缀 |
| AC-03 | 显示位置 | 版本号位于界面底部，水平居中，导航栏安全区适配 |
| AC-04 | 视觉一致 | 版本号使用 onSurfaceVariant 颜色，12sp 字号 |
| AC-05 | 无障碍 | TalkBack 朗读 "应用版本号 v{versionName}" |
| AC-06 | 超长截断 | 版本号超长时单行省略号截断 |

## §5 非功能性需求

- **性能:** 无额外开销（静态读取 BuildConfig，初始化后不重组）
- **兼容性:** 不影响现有 LoginScreen 逻辑，继承项目 minSdk (API 26)
- **可维护性:** 版本号自动随 BuildConfig 更新
- **无障碍:** TalkBack contentDescription 已内建

## §6 技术约束

- 使用 Jetpack Compose
- 版本号从 BuildConfig 读取
- 遵循 MVVM 架构（VersionTag 为纯 UI 组件，无 ViewModel 依赖）
- 需处理 Debug/Release 构建行为差异

## §7 风险与依赖

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| VersionTag Debug-only 硬编码 | Release 不显示 | 重构 VersionTag 移除 `if (!DEBUG) return`，或新增无条件的 SimpleVersionLabel |

## §8 术语表

| 术语 | 说明 |
|------|------|
| 启动页面 → MainActivity | 用户术语"启动页面"在代码中对应 MainActivity |
| VersionTag | 现有版本号显示 Composable（Debug-only） |
| BuildConfig | Android 构建时自动生成的配置类 |

## §9 UI 设计输入

### 页面清单
| 页面 | 路由 | 说明 |
|------|------|------|
| 主界面版本号浮层 | 无路由（overlay） | 叠加在 MainActivity 内容之上的底部文本 |

### 布局规格
| 属性 | 值 |
|------|-----|
| 容器 | Box (fillMaxSize) |
| 对齐 | Alignment.BottomCenter |
| 底部间距 | 8dp |
| 导航栏适配 | WindowInsets.navigationBars |
| 字号 | 12sp (覆盖 labelSmall 默认 11sp) |
| 颜色 | MaterialTheme.colorScheme.onSurfaceVariant |
| 最大行数 | 1 (TextOverflow.Ellipsis) |

### 交互规格
- 无点击/长按交互
- 不可获取无障碍焦点（纯装饰性辅助文本）
- 不拦截触摸事件

### 状态覆盖
| 状态 | 可见 | 显示内容 |
|------|------|----------|
| Debug 构建 | 是 | `v{name}({code}){buildType}` |
| Release 构建 | 是 | `v{name}` |
| 版本名为空 | 是 | 防御性 fallback |

### 组件选型
| 组件 | 选型 | 原因 |
|------|------|------|
| 文本 | Text | 简单文本显示 |
| 排版 | labelSmall (12sp 覆盖) | M3 最小文本层级 |
| 颜色 | onSurfaceVariant | M3 次级文本色 token |
| 容器 | Box | 底部对齐布局 |

### 设计约束
- 深色模式自动适配（onSurfaceVariant token）
- 响应系统字体缩放（.sp 单位）
- 惰性：静态文本无状态依赖，初始组合后不重组

## §10 附录

### 参考资料
- Material3 Typography: https://m3.material.io/styles/typography

---

> **状态:** 待评审 (v0.1-draft) — 请审阅后回复「确认」冻结。

---

## §11 多视角评审记录

> 评审日期: 2026-06-03
> 评审方式: 3-Agent 并行评审（产品视角 / 技术视角 / UX 视角）

### 11.1 评审总览

| 视角 | 评分 | P0 项 | P1 项 | 结论 |
|------|------|-------|-------|------|
| 产品视角 | 5/10 | 3 | 5 | P0 已自动修订 |
| 技术视角 | 4/10 | 3 | 3 | P0 已自动修订 |
| UX 视角 | 5/10 | 2 | 6 | P0 已自动修订 |

### 11.2 产品视角评审

**核心发现:** 产品 Agent 指出 PRD 结构与代码现状存在 3 处致命矛盾：(1) US-01 "普通用户看到版本号" 与 Debug-only 实现冲突；(2) AC-01 格式 `v{versionName}` 与代码 `v{name}({code}){buildType}` 不一致；(3) §7 "BuildConfig 未配置" 风险不成立（已配置 versionName="1.0"）。

**P1 项:** 样式硬编码 12.sp vs AC-04 的 labelSmall 需统一；底部间距偏差（PRD 24dp vs 代码 8dp）；缺少测试场景和 DECISIONS.md 条目。

### 11.3 技术视角评审

**核心发现:** 技术 Agent 确认 VersionTag 已完全实现 Debug 版本号显示。不可直接复用——`if (!BuildConfig.DEBUG) return` 硬编码阻止 Release 渲染。需新建无条件组件或重构 VersionTag 增加 `visibleInRelease` 参数。架构影响最小（仅 VersionTag.kt + MainActivity.kt，无数据层/ViewModel 改动）。

**P1 项:** 样式非 labelSmall（硬编码 12.sp）；无障碍已实现但 PRD 遗漏；格式规范需明确 Debug/Release 差异。

### 11.4 UX 视角评审

**核心发现:** UX Agent 确认"无交互"设计正确——版本号应为纯信息展示。指出间距矛盾（PRD 24dp vs 实际 8dp）、无障碍 contentDescription 遗漏、导航栏安全区未提及。输出完整 §9 UI 设计输入结构化规格（见 §9 已合并）。

**P1 项:** 缺少超长版本号截断边界、系统字体缩放适配、深色模式对比度验证。

### 11.5 讨论决议

| 决议编号 | 决议内容 | 来源 | 状态 |
|----------|----------|------|------|
| R-01 | Release 构建需显示版本号，格式为 `v{versionName}`（无 buildType 后缀） | 三方共识 | 已应用 |
| R-02 | 不可直接复用现有 VersionTag，需新建无条件组件或重构 | 技术 Agent | 待实现 |
| R-03 | 底部间距统一为 8dp（对齐代码现有值），非 PRD 原 24dp | UX Agent | 已应用 |
| R-04 | §7 删除 "BuildConfig 未配置" 无效风险项 | 产品+技术 | 已应用 |

---

> **状态:** 多视角评审已完成，4 项决议已自动修订到正文。确认后冻结版本号。
