# 启动页面增加版本号显示功能 — PRD

> **版本:** v1.0-confirmed
> **功能名称:** 启动页面增加版本号显示功能
> **创建日期:** 2026-06-03
> **作者:** Hermes 智能研发工作流

---

## §1 功能概述

在App主界面（MainActivity的LoginScreen区域）底部显示当前应用的版本号信息。版本号从 BuildConfig 动态读取，格式遵循 D-15 决策（`v{name}({code}){buildType}`），Debug 构建始终显示，Release 构建仅在「设置→关于」显示。该功能实质是对已有 `VersionTag` 组件的定位/样式微调，无需新建 Splash Screen。

> **关联决策:** D-12 (Debug/Release 策略), D-15 (版本号格式), D-20 (间距), D-21 (VersionTag 解耦)

## §2 用户场景

| 场景编号 | 角色 | 场景描述 |
|----------|------|----------|
| US-01 | 开发者/Debug用户 | Debug构建启动时，在主界面底部看到版本号（含 name+code+buildType），快速确认构建版本 |
| US-02 | 测试人员 | 启动App时快速确认当前安装版本和构建类型，无需进入设置页面 |

## §3 范围边界

### 包含
- 主界面底部显示版本号文本（复用现有 VersionTag 组件）
- 版本号格式：`v{versionName}({versionCode}){buildType}`（对齐 D-15）
- Debug 构建显示，Release 构建不在此处显示（对齐 D-12）
- 版本号间距：距底部 8dp（对齐 D-20 8dp 网格）
- WindowInsets 导航栏适配（对齐 D-16）
- TalkBack contentDescription「应用版本号」（对齐 D-17）
- 横屏/分屏 <480dp 宽度时隐藏版本号（对齐 D-20）

### 不包含
- 新建独立 Splash Screen 页面（现有 MainActivity→LoginScreen 已满足需求，D-21 已解耦）
- 版本更新检查功能
- 点击版本号交互（对齐 D-24 静态展示策略）
- 多语言适配
- Git SHA 短码（标记为 v1.1 特性）
- Release 构建的启动版本号显示

## §4 验收标准

| 编号 | 验收项 | 预期结果 |
|------|--------|----------|
| AC-01 | 版本号格式 | 显示格式为 `v{versionName}({versionCode}){buildType}`（对齐 D-15）；Release 构建省略 buildType 后缀 |
| AC-02 | 版本号来源 | 从 BuildConfig.VERSION_NAME / VERSION_CODE / BUILD_TYPE 动态读取 |
| AC-03 | Debug/Release 区分 | Debug：主界面底部显示；Release：不显示（对齐 D-12，仅「设置→关于」显示） |
| AC-04 | 显示位置 | 版本号位于主界面底部、水平居中，不遮挡 Logo 和核心内容 |
| AC-05 | 下边距 | 距底部 8dp（对齐 D-20 8dp 网格），叠加 WindowInsets.navigationBars |
| AC-06 | 横屏/小屏隐藏 | 窗口宽度 < 480dp 时隐藏版本号文本（对齐 D-20） |
| AC-07 | 无障碍 | TalkBack contentDescription 为「应用版本号」（对齐 D-17） |
| AC-08 | 对比度 | 版本号文本与背景对比度 ≥ 4.5:1（WCAG AA normal text 标准） |
| AC-09 | 视觉层级 | 版本号使用较低视觉层级（onSurfaceVariant @ alpha 0.6），与导航文字区分（对齐 D-19） |

## §5 非功能性需求

- **性能:** 版本号读取为零开销（BuildConfig 编译时常量），不影响启动速度
- **兼容性:** Android 8.0+ (API 26+)，与现有项目 minSdk 保持一致（对齐 D-14）
- **可维护性:** 复用现有 `formatVersionTag()` 函数，版本号单一来源

## §6 技术约束

- 使用 Jetpack Compose 实现
- 遵循现有 MVVM 架构
- Material3 主题系统
- 复用现有 `VersionTag.kt` Composable（D-21 已与 LoginScreen 解耦，Box bottom-align）
- 复用现有 `formatVersionTag()` 函数（D-15 格式）

## §7 风险与依赖

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| LoginScreen 布局改动可能影响现有动画 | 低 | VersionTag 已通过 Box 独立层解耦(D-21)，不改变 LoginScreen 内部布局 |
| onSurfaceVariant @ alpha 0.6 在深色背景对比度不足 | 中 | 实测验证对比度 ≥ 4.5:1，不达标时提升至 onSurface |
| Release 构建 buildType 后缀用户困惑 | 低 | Release 构建省略 buildType 后缀，格式降为 `v{name}({code})` |

## §8 术语表

| 术语 | 说明 |
|------|------|
| VersionTag | 现有 Composable，在 MainActivity Box 底部居中显示版本号 |
| MainActivity | 现有主 Activity，Compose 渲染 LoginScreen + VersionTag |
| LoginScreen | 现有登录页面 Composable |
| BuildConfig | Android 编译时自动生成的配置类 |

## §9 UI 设计输入

### 页面清单
- MainActivity 主界面（现有）- VersionTag 已在 Box 底部，仅调整样式/验收

### 布局规格
| 属性 | 值 |
|------|-----|
| 版本号位置 | 主界面底部，水平居中（Box BottomCenter） |
| 版本号距底 | 8dp（对齐 D-20 8dp 网格）+ WindowInsets.navigationBars |
| 版本号字号 | 12sp |
| 版本号颜色 | Material3 onSurfaceVariant @ alpha 0.6（对齐 D-19） |
| 版本号字体 | MaterialTheme.typography.bodySmall |
| 可见断点 | 窗口宽度 ≥ 480dp 时显示，否则 GONE（对齐 D-20） |

### 交互规格
- 无交互（纯展示文本，对齐 D-24）
- 无动画（随界面同步渲染）

### 组件选型
| 组件 | 选型 | 原因 |
|------|------|------|
| 版本号文本 | 复用 VersionTag.kt (Text) | 已存在，D-21 已解耦 |
| 版本号格式 | 复用 formatVersionTag() | D-15 格式，已有实现 |

### 设计约束
- 不改变 LoginScreen 内部布局（VersionTag 在 Box 独立层）
- 版本号文本不可交互
- 对比度 ≥ 4.5:1 (WCAG AA normal text)
- Debug 显示 / Release 隐藏（对齐 D-12）
- Dark mode：若背景 #000000 则强制提亮至对比度达标

## §10 附录

### 参考资料
- 项目 CLAUDE.md
- DECISIONS.md D-12~D-21
- Material3 Design Guidelines
- 现有 VersionTag.kt / formatVersionTag() 实现

---

> **状态:** 待评审 (v0.1-draft) — 请审阅后回复「确认」冻结。

---

## §11 多视角评审记录

> 评审日期: 2026-06-03
> 评审方式: 3-Agent 并行轻量评审（产品视角 / 技术视角 / UX 视角）

### 11.1 评审总览

| 视角 | 评分 | P0 项 | P1 项 | 结论 |
|------|------|-------|-------|------|
| 产品视角 | 4/10 | 3 | 5 | ❌ 退回修订 |
| 技术视角 | 4/10 | 5 | 6 | ❌ 退回修订 |
| UX 视角 | 4/10 | 3 | 6 | ❌ 退回修订 |

### 11.2 产品视角评审

**P0 问题（已自动修订）：**
| # | 问题 | 修订 |
|---|------|------|
| P0-1 | PRD 与 D-12 冲突（未区分 Debug/Release） | §3 增加 Debug/Release 策略，AC-03 补充 |
| P0-2 | 版本号格式与 D-15 冲突 | §3 对齐为 `v{name}({code}){buildType}`，AC-01 修正 |
| P0-3 | "启动页面"不存在（实为 LoginScreen） | §1/§8 修正概念，改为 MainActivity+LoginScreen |

**P1 问题（已识别）：**
- P1-1: 用户价值存疑（Splash 显示时长短） — 功能性质调整为主界面版本标记
- P1-2: 颜色规格模糊（alpha 未指定） — AC-09 补充 alpha 0.6
- P1-3: 未处理系统栏内边距 — AC-05 补充 WindowInsets
- P1-4: 无障碍规格缺失 — AC-07 补充 TalkBack
- P1-5: 无深色模式考量 — §9 补充 dark mode 约束

### 11.3 技术视角评审

**P0 问题（已自动修订）：**
| # | 问题 | 修订 |
|---|------|------|
| P0-1 | 版本号格式与 D-15 冲突 | AC-01 修正 |
| P0-2 | 未区分 Debug/Release (D-12) | AC-03 补充 |
| P0-3 | "Splash Screen" 概念不存在 | §1/§8 修正 |
| P0-4 | 间距与 D-20 冲突 (24dp vs 8dp) | §9 对齐 8dp |
| P0-5 | 缺少关键验收标准 | AC-05~AC-09 补充 |

**P1 问题（已识别）：**
- P1-1: PRD 未引用决策记录 — §1/§10 增加决策引用
- P1-2: Release buildType 后缀含混 — §3/§7 补充省略规则
- P1-3: 透明度策略不一致 — AC-09 对齐
- P1-4: Typography 不一致 — §9 统一为 bodySmall
- P1-5: D-15 Git SHA 未体现 — §3 标记为 v1.1
- P1-6: 风险表不全 — §7 补充

**结论：** 无新增组件。功能实质已由 `VersionTag.kt` 实现，PRD 需对齐描述而非新增代码。

### 11.4 UX 视角评审

**P0 问题（已自动修订）：**
| # | 问题 | 修订 |
|---|------|------|
| P0-1 | WCAG AA 合规断裂（12sp 需 4.5:1） | AC-08 修正为 ≥ 4.5:1 |
| P0-2 | 版本号格式矛盾 | AC-01 对齐 D-15 |
| P0-3 | 对比度风险未闭环 | §9 补充 dark mode 实测要求 |

**P1 问题（已识别）：**
- P1-1: 下边距不一致 — 已对齐
- P1-2: 缺少横屏/小屏隐藏 — AC-06 补充
- P1-3: 缺少无障碍完整规格 — AC-07 补充
- P1-4: Dark mode 未验证 — §9 补充约束
- P1-5: 视觉层级冲突（D-19 vs D-13） — AC-09 明确 onSurfaceVariant @ 0.6
- P1-6: 出现时机未定义 — §9 明确无动画同步渲染

### 11.5 讨论决议

| 决议编号 | 决议内容 | 来源 | 状态 |
|----------|----------|------|------|
| R-01 | 功能定位修正：从"新建 Splash Screen"改为"现有 MainActivity VersionTag 样式对齐" | 三方共识 | 已修订 |
| R-02 | 版本号格式统一为 D-15：`v{name}({code}){buildType}`，Release 省略 buildType | 三方共识 | 已修订 |
| R-03 | Debug/Release 策略对齐 D-12：Debug 显示，Release 隐藏 | 产品+技术 | 已修订 |
| R-04 | 下边距统一 8dp（对齐 D-20 8dp 网格） | 技术 | 已修订 |
| R-05 | 对比度标准修正为 WCAG AA normal text ≥ 4.5:1 | UX | 已修订 |
| R-06 | 补充 AC-05~AC-09 验收标准（WindowInsets/横屏/无障碍/对比度/视觉层级） | 技术+UX | 已修订 |

---

> **状态:** 多视角轻量评审已完成，共识 P0 已自动修订回填 PRD 正文。6 项决议已执行。
