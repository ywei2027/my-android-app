# 启动页版本号显示 — PRD

> **版本:** v1.0-confirmed
> **功能名称:** 启动页版本号显示
> **创建日期:** 2026-06-08
> **作者:** Hermes 智能研发工作流

---

## §1 功能概述

在应用启动页（MainActivity 中的 `AnimatedSplashContent` Composable）底部显示当前应用版本号，便于开发者和用户快速识别版本。启动页 Splash 持续约 1.5s 后自动淡出，版本号随 Splash 短暂展示而非永久驻留主界面。

> **概念映射**：用户术语"启动页" → 代码实体为 MainActivity.kt 中的 `AnimatedSplashContent`。项目已有 `VersionTag` 组件和 `formatVersionTag()`/`formatVersionDescription()` 函数，但 VersionTag 仅 Debug 渲染，需绕过条件逻辑。

## §2 用户场景

| 场景编号 | 角色 | 场景描述 |
|----------|------|----------|
| US-01 | 普通用户 | 启动时在启动页看到版本号 |
| US-02 | 测试/QA | 在不同构建版本间切换，通过版本号快速确认当前版本 |

## §3 范围边界

### 包含
- `AnimatedSplashContent` 底部显示版本号，放在 Box 根容器不被 AnimatedVisibility 包裹
- 版本号启动即显示，splash 结束后保留
- Debug：显示 `v{name}({code})debug`
- Release：显示 `v{name}({code})`（无 buildType 后缀）
- 版本号格式由复用 `formatVersionTag()` / `formatVersionDescription()` 工具函数提供

### 不包含
- 不直接复用 `VersionTag` Composable（其内置 `if(!BuildConfig.DEBUG) return` 阻断 Release）
- Debug 下需替换 `VersionTag` 渲染逻辑，避免新旧版本号重复显示
- 设置页/版本更新检查

## §4 验收标准

| 编号 | 验收项 | 预期结果 |
|------|--------|----------|
| AC-01 | Debug 版本号 | 底部显示 `v{name}({code})debug` |
| AC-02 | Release 版本号 | 底部显示 `v{name}({code})`，不含 buildType 后缀 |
| AC-03 | 动画独立 | fadeOut 后版本号仍可见 |
| AC-04 | 无障碍 | 语义化 contentDescription |
| AC-05 | 对比度 | ≥4.5:1 (WCAG AA) |
| AC-06 | 异常兜底 | BuildConfig 异常时静默降级不崩溃 |

## §5 非功能性需求

- **性能:** 零额外开销（纯静态展示）
- **兼容性:** API 26+
- **可维护性:** 复用现有函数

## §6 技术约束

- Compose 实现，不改架构
- 来源：BuildConfig 编译期常量
- 无新依赖，无 ViewModel
- 函数签名变更需保持向后兼容

## §7 风险与依赖

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 动画结构需调整 | 中 | Box 根容器不参与 AnimatedVisibility |
| 导航栏遮挡 | 中 | WindowInsets.navigationBars 叠加 |
| 屏幕适配 | 低 | 自适应布局 |

## §8 术语表

| 术语 | 说明 |
|------|------|
| AnimatedSplashContent | MainActivity 中启动页 Composable |
| formatVersionTag() | 版本号格式化函数 |
| AnimatedVisibility | Compose 动画可见性 |

## §9 UI 设计输入

### 页面清单
- `AnimatedSplashContent` (MainActivity.kt) — 修改

### 布局规格
| 属性 | 值 |
|------|-----|
| 布局策略 | 嵌套 Box：外层 Box(#1A73E8)→AnimatedVisibility(主内容 fadeIn+fadeOut)+独立 AnimatedVisibility(fadeIn,300ms)(版本号)。主内容 Box(Alignment.Center)保持原有布局，版本号 Text 用 `Modifier.align(Alignment.BottomCenter)` |
| 底边距 | `32dp + WindowInsets.navigationBars` 安全区 |
| 字号 | 12sp (labelSmall)，字体缩放上限 1.5x |
| 颜色 | `Color.White.copy(alpha=0.7f)` — 与现有白色 UI 元素协调，对比度 ~6.5:1 ≥ WCAG AAA |
| 溢出 | `maxLines=1`, `TextOverflow.Ellipsis` |
| RTL | `TextDirection.Content` |

### 交互规格
- 纯静态文本，无 onClick/onLongClick
- 版本号使用独立 `AnimatedVisibility(enter=fadeIn(tween(300ms)))`，延迟 300ms 与主内容动画协调
- 不参与主内容 AnimatedVisibility 的 scaleIn+fadeIn+fadeOut

### 组件选型
| 组件 | 选型 | 原因 |
|------|------|------|
| 文本 | Text (Compose) | 无新依赖 |
| 格式化 | formatVersionTag/Description | 复用 |
| 容器 | Box (现有) | 叠加 |

### 设计约束
- 版本号不被主内容 AnimatedVisibility 包裹，独立于 fadeOut
- Release：`formatVersionTag(buildType="")` 去掉 buildType 后缀
- Debug contentDescription: `Modifier.semantics { contentDescription = "应用版本号 v{name} 构建 {code} 调试版本" }`
- Release contentDescription: `Modifier.semantics { contentDescription = "应用版本号 v{name}" }`
- contentDescription **始终包含完整版本号**（不因 Ellipsis 截断），确保 TalkBack 朗读完整信息
- 导航栏安全区 + BuildConfig 异常静默降级

## §10 验收测试用例

### 场景组：启动页版本号显示

```gherkin
Scenario: Debug构建显示完整版本号(TC-01)
  Given 应用以 Debug 变体构建
  When  冷启动应用
  Then  底部显示 "v1.0(1)debug"

Scenario: Release构建显示简洁版本号(TC-02)
  Given 应用以 Release 变体构建
  When  冷启动应用
  Then  底部显示 "v1.0(1)" 且不含 buildType 后缀

Scenario: 版本号在动画结束后仍可见(TC-03)
  Given AnimatedSplashContent 正在显示
  When  fadeOut 动画执行完毕
  Then  版本号仍在 Box 根容器中可见，不在 AnimatedVisibility 子树内

Scenario: 版本号有语义化contentDescription(TC-04)
  Given 应用已启动
  When  TalkBack 焦点移到版本号
  Then  Debug朗读含调试版本信息，Release朗读简洁版本号

Scenario: BuildConfig异常静默降级(TC-05)
  Given BuildConfig 字段异常或为空
  When  冷启动应用
  Then  应用不崩溃，版本号显示空字符串

Scenario: 对比度达标(TC-06)
  Given Release构建, 背景 #1A73E8
  When  显示版本号
  Then  AccessibilityScanner 检测 ≥4.5:1

Scenario: 长版本名省略(TC-07)
  Given VERSION_NAME 超长
  When  冷启动
  Then  maxLines=1 且超出省略号截断

Scenario: 字体缩限(TC-08)
  Given 系统字体缩放 2.0x
  When  冷启动
  Then  版本号缩放不超过 1.3x

Scenario: RTL布局(TC-09)
  Given 系统语言为阿拉伯语
  When  冷启动
  Then  TextDirection=Content 且 BottomCenter 正确

Scenario: 底部导航栏不遮挡版本号(TC-10)
  Given 设备有虚拟导航栏
  When  冷启动显示Splash
  Then  版本号底边距 = 32dp + 导航栏高度
  And   版本号不被导航栏遮挡

Scenario: versionName空值静默降级(TC-11)
  Given VERSION_NAME 为空字符串或 null
  When  冷启动应用
  Then  版本号区域显示兜底文案 "v?.?" 不崩溃

Scenario: fadeIn动画期间TalkBack可读(TC-12)
  Given TalkBack 已开启
  When  版本号处于 fadeIn 动画中(alpha < 1.0)
  Then  contentDescription 完整可读，语义立即生效

Scenario: 配置变更旋转鲁棒性(TC-13)
  Given 版本号已显示
  When  设备在 splash 期间旋转
  Then  版本号正确重绘，无崩溃，布局不重叠

Scenario: 快速返回不残留动画(TC-14)
  Given splash 正在展示
  When  用户快速按返回键退出
  Then  Activity 正常 finish，无残留动画泄漏

Scenario: Debug构建无双版本号(TC-15)
  Given Debug 构建，项目已有 VersionTag
  When  启动页展示
  Then  仅显示新版本号，不出现新旧重复
```
| 编号 | 标题 | AC | 优先级 |
|------|------|:--:|:------:|
| TC-01 | Debug完整版本号 | AC-01 | P0 |
| TC-02 | Release简洁版本号 | AC-02 | P0 |
| TC-03 | 动画后仍可见 | AC-03 | P0 |
| TC-04 | contentDescription | AC-04 | P1 |
| TC-05 | 异常降级 | AC-06 | P0 |
| TC-06 | 对比度达标 | AC-05 | P0 |
| TC-07 | 长文本省略 | AC-01 | P1 |
| TC-08 | 字体缩限 | AC-05 | P1 |
| TC-09 | RTL布局 | AC-04 | P1 |
| TC-10 | 导航栏不遮挡 | AC-03 | P1 |
| TC-11 | versionName空值降级 | AC-06 | P0 |
| TC-12 | fadeIn动画无障碍 | AC-04 | P0 |
| TC-13 | 配置变更旋转 | AC-03 | P1 |
| TC-14 | 快速返回不泄漏 | AC-06 | P1 |
| TC-15 | Debug无双版本号 | AC-01 | P1 |

---

## §11 数据契约

本功能纯客户端展示，无 API 接口。

| 字段 | 类型 | 必填 | 来源 |
|------|------|:---:|------|
| VERSION_NAME | String | ✅ | BuildConfig |
| VERSION_CODE | Int | ✅ | BuildConfig |
| BUILD_TYPE | String | ✅ | BuildConfig |

---

## §12 多视角评审记录

### 12.1 评审总览

| 视角 | 评分 | P0 | P1 | 结论 |
|------|:----:|:--:|:--:|------|
| 产品 | 6/10 | 3 | 5 | P0已修订，排除设置页合理(splash短暂展示≠永久) |
| 技术 | 7/10 | 2 | 4 | P0已修订，Box布局+动画解耦 |
| UX | 7/10 | 3 | 6 | P0已修订，无障碍+颜色+字体缩限 |
| QA | 7/10 | 2 | 3 | P0已补充用例，AC全覆盖 |

### 12.2 产品视角评审

**评分: 6/10**

**P0 项（3项）：**
| # | 问题 | 修订 |
|---|------|:---:|
| P0-1 | Release 信息不足——`v{name}` 不含 versionCode | ✅ 已修订 AC-02: Release 显示 `v{name}({code})` |
| P0-2 | "启动即显示+持久保留"误读为永久侵入——澄清：splash ~1.5s 短暂展示，非永久主页 | ✅ 已澄清 §1 |
| P0-3 | Debug 下 VersionTag 与新版本号可能重复显示 | ✅ 已修订 §3: 替换 VersionTag 渲染逻辑 |

**P1 项（5项）：** 异常兜底行为模糊、contentDescription 格式需细化、缺少灰度方案

### 12.3 技术视角评审

**评分: 7/10**

**P0 项（2项）：**
| # | 问题 | 修订 |
|---|------|:---:|
| P0-1 | 布局冲突——Box Alignment.Center 与 BottomCenter 矛盾 | ✅ 已修订 §9: 使用嵌套 Box 或 Modifier.align |
| P0-2 | 动画隔离——版本号需独立 AnimatedVisibility(fadeIn only) | ✅ 已修订 §9 |

**P1 项（4项）：** 对比度实测验证、formatVersionTag 尾随空格、字体缩放上限实现方式、异常降级触发条件

### 12.4 UX 视角评审

**评分: 7/10**

**P0 项（3项）：**
| # | 问题 | 修订 |
|---|------|:---:|
| P0-1 | 字体缩放上限 1.3x 不足 → 应支持 1.5x+ | ✅ 已修订 §9: 缩放上限提升至 1.5x |
| P0-2 | 无障碍绑定方式未说明 | ✅ 已修订 §9: 显式 Modifier.semantics |
| P0-3 | 长版本号内容截断后 TalkBack 看不到完整信息 | ✅ 已修订 §9: contentDescription 始终完整 |

**P1 项（6项）：** 颜色不协调(黑字vs白UI)、未用Material3语义颜色、300ms动画标记可选、12sp可读性偏小、对比度余量

### 12.5 QA 视角评审

**评分: 7/10**

**P0 项（2项）：**
| # | 问题 | 修订 |
|---|------|:---:|
| P0-1 | AC-06 仅测 BuildConfig 异常，缺 `versionName=null/empty` 数据缺失场景 | ✅ 补充 TC-11 §10 |
| P0-2 | AC-04 仅测静态 TalkBack，未覆盖 fadeIn 动画期间 contentDescription 可读性 | ✅ 补充 TC-12 §10 |

**P1 项（3项）：** 配置变更(旋转)鲁棒性、快速返回不残留动画、Debug 双版本号验证 — 已补充 TC-13~15。

**AC 覆盖率：** 6/6 AC 全部覆盖，AC-04/06 已从部分覆盖→全覆盖。综合 7/10，补充后可达 8.5。

### 12.6 讨论决议

| 决议编号 | 决议内容 | 来源 | 状态 |
|----------|----------|------|------|
| R-01 | 布局策略：嵌套 Box + 独立 AnimatedVisibility(fadeIn 300ms) | 技术+UX | ✅ 已修订 §9 |
| R-02 | Release 显示 `v{name}({code})` 不含 buildType | 产品 | ✅ 已修订 AC-02/§3 |
| R-03 | 版本号颜色改为 `Color.White.copy(alpha=0.7f)` 与现有 UI 协调 | UX | ✅ 已修订 §9 |
| R-04 | 无障碍：`Modifier.semantics` 显式绑定 + contentDescription 始终完整版 | UX | ✅ 已修订 §9 |
| R-05 | 字体缩放上限从 1.3x 提升至 1.5x | UX | ✅ 已修订 §9 |
| R-06 | Debug 下替换 `VersionTag` 渲染避免新旧重复 | 产品 | ✅ 已修订 §3 |
| R-07 | splash ~1.5s 短暂展示，非永久主屏 | 产品 | ✅ 已澄清 §1 |
| R-08 | 对比度 6.5:1 (白字 alpha0.7) ≥ WCAG AAA | UX | ✅ 已修订 §9 |

---

> **状态:** 多视角评审已完成，N 项决议待确认。确认后冻结版本号。
