# 启动页版本号显示 — PRD

> **版本:** v1.0-confirmed
> **功能名称:** 启动页版本号显示
> **创建日期:** 2026-06-08
> **作者:** Hermes 智能研发工作流

---

## §1 功能概述

在应用启动页（Splash Screen）底部显示当前应用版本号，方便用户和测试人员快速识别安装的版本。

## §2 用户场景

| 场景编号 | 角色 | 场景描述 |
|----------|------|----------|
| US-01 | 普通用户 | 打开应用时在启动页看到版本号，确认应用是否为最新版本 |
| US-02 | 测试人员 | 安装测试包后在启动页直接确认版本号，无需进入关于页面 |
| US-03 | 开发者 | 快速验证 CI 构建产物的版本号与 Git tag 一致 |

## §3 范围边界

### 包含
- 启动页底部居中显示版本号文本
- Debug 构建显示完整格式 `v{versionName}({versionCode}){buildType}`（如 `v1.0(1)debug`）
- Release 构建显示简化格式 `v{versionName}`（如 `v1.0`，对齐 D-21/D-23 决议）
- 版本号从 BuildConfig 读取，与 Gradle versionName/versionCode 同步
- 复用项目已有 `formatVersionTag()` / `formatVersionDescription()` 函数
- 复用项目已有 `VersionTag` 组件的格式化函数，新增 splash 专用 Composable 调用

### 不包含
- 不修改启动页 logo 或动画
- 不增加手动刷新版本号功能
- 不在启动页显示 build 号或其他调试信息

## §4 验收标准

| 编号 | 验收项 | 预期结果 |
|------|--------|----------|
| AC-01 | 启动时显示版本号 | Debug: 启动页底部显示 `v1.0(1)debug`；Release: 显示 `v1.0`（对齐 D-21/D-23） |
| AC-02 | 版本号来源正确 | 显示值与 BuildConfig.VERSION_NAME/VERSION_CODE/BUILD_TYPE 一致 |
| AC-03 | 文字对比度达标 | 文字色 `rgba(0,0,0,0.55)` 在 #1A73E8 背景上对比度 ≥ 4.5:1（WCAG AA） |
| AC-04 | 无性能影响 | 不增加启动页显示延迟（增量 < 5ms，无额外 jank） |

## §5 非功能性需求

- **性能:** 启动时间增量 < 5ms
- **兼容性:** Android 8.0+，支持深色模式
- **可维护性:** 版本号读取逻辑封装为独立工具方法

## §6 技术约束

- 基于现有 SplashScreen API（androidx.core:core-splashscreen）
- 版本号从 BuildConfig.VERSION_NAME 获取
- 使用 Jetpack Compose 实现（与项目技术栈一致）

## §7 风险与依赖

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| Compose 与 SplashScreen API 兼容性 | 低 | Compose 视图嵌入已验证可行 |
| 版本号获取路径变更 | 低 | BuildConfig 为标准方案，如有异常回退到 PackageManager |

## §8 术语表

| 术语 | 说明 |
|------|------|
| Splash Screen | 应用启动时展示的过渡页面 |
| BuildConfig | Gradle 构建时自动生成的配置类，含 VERSION_NAME 等字段 |

## §9 UI 设计输入

> **状态:** v0.2-ux-reviewed | **基于:** UX 评审（5/10，4×P0 + 6×P1）

### §9.1 页面清单

| 页面 | 路由/组件 | 类型 | 说明 |
|------|----------|------|------|
| 启动页 | `AnimatedSplashContent` (MainActivity.kt) | 改造 | 现有蓝色启动页底部新增版本号文本。不涉及新页面/新路由，仅修改现有 Composable 布局层级。 |

### §9.2 布局规格

| 属性 | 原值（PRD v0.1） | **修订值（UX 评审后）** | 修订原因 |
|------|-----------------|------------------------|----------|
| 版本号位置 | 底部居中，距底部 32dp | 底部居中，距底部 32dp，叠加 `windowInsetsPadding(WindowInsets.navigationBars)` | P1-2：Edge-to-Edge 安全区适配 |
| 字体大小 | 12sp | 12sp | 不变 |
| 文字颜色 | `#80FFFFFF`（半透白） | **方案A：`rgba(0,0,0,0.55)`（深色半透明）** — 混合后 ~4.85:1 对比度，远超 WCAG AA 4.5:1 | P0-1：原方案对比度仅 1.64:1，方案A 低调可见不抢 logo 注意力 |
| 字重 | Normal (400) | Normal (400) | 不变 |
| 背景 | 透明 | 透明（方案B 例外：半透明深色 pill `RoundedCornerShape(4dp)`） | P0-1 取决于方案选型 |
| 显示格式 | `vX.Y.Z` | Debug: `v{name}({code}){buildType}`（如 `v1.0(1)debug`） / Release: `v{name}`（如 `v1.0`，无 versionCode/buildType） | P1-4 + P0-4：对齐 ADR-2026-001 + 项目 formatVersionTag() |
| 容器层级 | — | **Box → AnimatedVisibility(splash) → AnimatedSplashContent**（版本号 Text 独立于 AnimatedVisibility 外层，`Modifier.align(Alignment.BottomCenter)`） | P0-2：防止版本号随 FadeOut 消失 |

### §9.3 交互规格

| 属性 | 值 |
|------|-----|
| 交互类型 | 纯展示，无交互（无点击/焦点/手势） |
| 出现时机 | **启动页呈现瞬间即显示（0ms），不等待/不参与 logo scaleIn 动画**（P1-1） |
| 消失时机 | 随启动页整体消失（Box 容器被移除），不单独执行 FadeOut |
| 可见性约束 | 版本号在整个启动页生命周期内始终可见，不随 logo FadeOut 淡出（P0-2） |
| 无障碍 | `semantics { contentDescription = formatVersionDescription() }` — 格式 `"应用版本号 v{versionName}"`（P0-3，复用已有函数） |

### §9.4 组件选型

| 组件 | 选型 | 原因 |
|------|------|------|
| 版本号文本 | `Text` (Compose) | 与项目技术栈一致 |
| 版本号容器 | 直接置于 `Box`（启动页根容器）中，不包裹额外容器 | 保持透明叠加，最小侵入 |
| 版本号格式化 | **复用** `formatVersionTag()`（调试版本号） + `formatVersionDescription()`（无障碍描述） | P0-3/P1-4：避免重复造轮子，对齐现有 ADR |
| 颜色 Token | **[待决策]** 新增语义 Token `splashVersionColor` 或直接使用方案选定色值 | P1-3：避免硬编码，纳入 Theme 统一管理 |

### §9.5 设计约束

| # | 约束 | 说明 |
|---|------|------|
| C-1 | 不改变现有 logo/动画 | logo scaleIn(0.3→1, 600ms) + fadeIn(600ms) 保持原样 |
| C-2 | 版本号始终可见 | 不随 FadeOut 消失，独立于 AnimatedVisibility |
| C-3 | 对比度达标 | 文字与背景对比度 ≥ 4.5:1（WCAG AA）（P0-1 待方案选型） |
| C-4 | System bars 适配 | `windowInsetsPadding(WindowInsets.navigationBars)` + 底部 32dp（P1-2） |
| C-5 | 无障碍覆盖 | contentDescription 格式与可见文本一致（ADR-2026-002）（P0-3） |
| C-6 | Release 构建行为 | **[待决策]** 方案A: 显示 `v{name}` / 方案B: 不显示（P0-4） |
| C-7 | 刘海屏安全区 | 底部显示风险低，但建议标注 `displayCutout` 约束（P1-5） |
| C-8 | 横屏兼容 | 底部居中 + 32dp，不做特殊布局（P1-6） |
| C-9 | 性能 | 启动时间增量 < 5ms（PRD §5），纯 Text 渲染无额外开销 |

### §9.6 布局层级图（修订后）

```
AppWithAnimatedSplash
└── Box(fillMaxSize)                                    // 根容器
    ├── AnimatedVisibility(visible=!showSplash)         // 主内容（延迟显示）
    │   └── NewsAppNavHost()
    ├── AnimatedVisibility(visible=showSplash,          // Splash 动画容器
    │       exit=fadeOut(500ms))
    │   └── AnimatedSplashContent(onFinished)           // logo 动画（不变）
    │       ├── Text("📰", 72sp)                        // scaleIn + fadeIn 600ms
    │       ├── Text("新闻", 28sp, Bold, White)
    │       └── Text("热点资讯 一键掌握", 14sp, 半透白)
    └── Text(versionText,                               // ★ 新增：版本号
             Modifier.align(BottomCenter)
               .windowInsetsPadding(navigationBars)
               .padding(bottom=32dp),
             fontSize=12sp,
             color=[待决策],
             semantics { contentDescription = ... })
```

## §10 验收测试用例

> **产出方:** QA Agent（PRD 评审阶段并行产出）
> **评审日期:** 2026-06-08
> **格式:** Gherkin（Given/When/Then）
> **基础:** PRD §2 用户场景 + §4 验收标准 + 前三方评审 P0 发现
> **决议引用:** D-21 (Release 去 buildType 后缀), D-22 (contentDescription 与可见文本一致), D-23 (示例格式 v1.0(1)/v1.0(1)debug)

### §10.1 场景组：正常流程 — Debug/Release 双构建

```gherkin
Scenario: TC-01 Debug 构建启动页显示完整版本号
  Given 应用以 Debug 变体构建，versionName="1.0"，versionCode=1
  When 用户冷启动应用
  Then 启动页底部居中显示文本 "v1.0(1)debug"
  And 版本号字体大小为 12sp，文本单行不换行
  And 版本号位于 AnimatedVisibility splash 容器外部，不参与 logo scaleIn/fadeIn 动画
```

```gherkin
Scenario: TC-02 Release 构建启动页显示简化版本号
  Given 应用以 Release 变体构建，versionName="1.0"
  When 用户冷启动应用
  Then 启动页底部居中显示文本 "v1.0"（仅 versionName，无 versionCode 和 buildType 后缀）
  And 显示格式符合 D-21 决议：formatVersionTag(buildType="") 调用
  And 版本号在整个启动页生命周期中持续可见
```

### §10.2 场景组：动画生命周期 — 边界行为

```gherkin
Scenario: TC-03 版本号不随启动页淡出消失
  Given 应用已启动，启动页完全可见（showSplash=true）
  And 版本号文本在 Box 根容器中，独立于 AnimatedVisibility(exit=fadeOut(500ms))
  When 启动页开始退出过渡（showSplash 设为 false，触发 fadeOut(500ms)）
  Then 版本号文本在整个 fadeOut 动画期间始终完全可见
  And 版本号不参与淡出动画（opacity 保持 1.0）
  And 启动页完全消失后，版本号随 Box 容器移除而正常消失
```

### §10.3 场景组：视觉质量 — 对比度与无障碍

```gherkin
Scenario: TC-04 版本号文字与蓝色背景对比度达标
  Given 启动页背景色为 #1A73E8（硬编码蓝色）
  And 版本号文字颜色为非半透明白的方案色（不含 alpha 通道降低对比度）
  When 版本号文本渲染到屏幕上
  Then 文字与 #1A73E8 背景的相对亮度对比度 ≥ 4.5:1（WCAG 2.1 AA 标准）
  And 使用 APCA 或 WCAG 对比度计算工具验证（非目测）
```

```gherkin
Scenario: TC-05 TalkBack 朗读正确的无障碍描述
  Given 设备已启用 TalkBack（或等效屏幕阅读器）
  And 启动页当前可见
  When 用户将无障碍焦点移动到版本号文本区域
  Then 屏幕阅读器朗读内容为 "应用版本号 v{versionName}"（如 "应用版本号 v1.0"）
  And 朗读内容与 D-22 决议一致：contentDescription 与可见文本保持对应
  And semantics 节点包含 contentDescription 属性（非空）
```

### §10.4 场景组：非功能性 — 性能边界

```gherkin
Scenario: TC-06 版本号显示不增加启动延迟
  Given 同一设备上构建两个 APK：含版本号显示 vs 不含版本号显示（基线）
  When 对两个 APK 分别执行 5 次冷启动并测量 splash 完全可见时间
  Then 含版本号版本的平均启动增量 < 5ms
  And 无因版本号 Text Composable 导致的额外帧丢失（jank）
```

### §10.5 测试用例矩阵

| 场景编号 | 场景标题 | 覆盖的 AC | 优先级 | 类型 |
|----------|----------|:---------:|:------:|:----:|
| TC-01 | Debug 构建启动页显示完整版本号 | AC-01, AC-02 | **P0** | 正常流程 |
| TC-02 | Release 构建启动页显示简化版本号 | AC-01, AC-02 | **P0** | 正常流程 |
| TC-03 | 版本号不随启动页淡出消失 | AC-01 | **P0** | 边界行为 |
| TC-04 | 版本号文字与蓝色背景对比度达标 | AC-03 | **P0** | 边界-视觉 |
| TC-05 | TalkBack 朗读正确的无障碍描述 | AC-01 | **P1** | 边界-无障碍 |
| TC-06 | 版本号显示不增加启动延迟 | AC-04 | **P1** | 非功能性 |

### §10.6 覆盖率分析

| AC 编号 | 验收项 | 覆盖用例 | 覆盖状态 |
|---------|--------|----------|:--------:|
| AC-01 | 启动时显示版本号 | TC-01, TC-02, TC-03, TC-05 | ✅ 全覆盖 |
| AC-02 | 版本号来源正确（与 versionName 一致） | TC-01, TC-02 | ✅ 覆盖 |
| AC-03 | 视觉适配（深浅背景清晰可读） | TC-04 | ⚠️ 仅覆盖固定蓝色背景，深色模式场景见备注 |
| AC-04 | 无性能影响 | TC-06 | ✅ 覆盖 |

> **备注:** AC-03 当前仅覆盖固定蓝色背景 #1A73E8。若未来启动页支持深色模式主题换肤，需新增 TC-07 验证深色背景（如 #0D47A1）下的对比度。鉴于当前代码库 AnimatedSplashContent 硬编码蓝色背景且无深色模式分支，单场景覆盖充分。

### §10.7 已知测试债务（未纳入当前用例）

| # | 场景 | 原因 | 建议 |
|---|------|------|------|
| SKIP-01 | 超长版本号截断（如 v10.20.30-beta.1-rc2(9999)debug） | versionName 目前为 "1.0" 两段，超长场景概率极低 | P2，待实际 versionName 进入多段预发布后补充 |
| SKIP-02 | BuildConfig 不可用降级 | BuildConfig 为编译期生成，运行时不可用的概率≈0 | 不做测试 |
| SKIP-03 | 横屏布局验证 | D-25 决议 P1 项延后处理 | 后续迭代补充 |
| SKIP-04 | System bars 安全区适配（导航栏遮挡） | D-25 决议延后，且 TC-03 验证了 Box 根容器布局 | 后续迭代配合 navigationBars inset 测试 |
| SKIP-05 | 深色模式启动页对比度 | 当前硬编码 #1A73E8，无深色主题入口 | 主题化改造时新增 TC-07 |

---

## §11 数据契约

> **产出方:** 技术 Agent（PRD 评审阶段产出）
> **格式:** JSON Schema

### 11.1 版本信息模型

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "AppVersionInfo",
  "type": "object",
  "required": ["versionName", "versionCode"],
  "properties": {
    "versionName": {
      "type": "string",
      "pattern": "^v?\\d+\\.\\d+\\.\\d+(-[a-zA-Z0-9]+)?$",
      "description": "语义化版本号，如 1.0.0 或 2.0.0-beta"
    },
    "versionCode": {
      "type": "integer",
      "minimum": 1,
      "description": "内部构建号"
    }
  }
}
```

### 11.2 状态枚举

| 状态 | 值 | 说明 |
|------|-----|------|
| 启动页可见 | VISIBLE | 版本号文本渲染并可见 |
| 启动页消失 | GONE | 进入主界面后版本号随启动页消失 |

---

## §12 多视角评审记录

> 评审日期: 2026-06-08
> 评审方式: 4-Agent 并行评审（产品视角 / 技术视角 / UX 视角 / QA 视角）

### 12.1 评审总览

| 视角 | 评分 | P0 项 | P1 项 | 结论 |
|------|------|-------|-------|------|
| 产品视角 | 6/10 | 4 | 6 | P0:Debug/Release策略未定义、格式矛盾、与VersionTag关系未定义、背景非固定 |
| 技术视角 | 6/10 | 4 | 5 | P0:!DEBUG门控冲突、决议D-21/D-23格式冲突、AnimatedVisibility生命周期、VersionTag位置迁移 |
| UX 视角 | 5/10 | 4 | 6 | P0:对比度致命缺陷(1.64:1)、FadeOut冲突、缺contentDescription、Release行为未定义 |
| **QA 视角** | **3/10** | **4** | **4** | **P0:AC-01格式矛盾(vX.Y.Z vs v1.0(1)debug)、AC-03无量化阈值、AC-04缺测量协议、Debug/Release未区分** |

### 12.2 产品视角评审

> **评审日期:** 2026-06-08 | **评审模型:** deepseek-v4-flash | **评分: 6/10**

#### 用户价值清晰度 — 中等偏低

| 场景 | 评价 |
|------|------|
| US-01 普通用户 | ❌ **薄弱** — 普通用户几乎不在启动页停留1.2秒内主动阅读版本号，依赖应用商店更新机制 |
| US-02 测试人员 | ✅ **强价值** — 测试包安装后在启动页0操作确认版本，痛点真实 |
| US-03 开发者 | ⚠️ PRD中仍列出但DECISIONS.md已标记移除(D-24)；若保留则是内部工具场景而非产品功能 |

**结论：** 核心价值锚点是US-02（测试/QA效率），US-01作为"顺便可见"附加价值。

#### P0 清单（阻塞实现）

| # | 遗漏场景 | 风险 |
|---|---------|------|
| **P0-1** | Debug/Release显示策略未定义 | 现有代码Release不显示；PRD暗示始终显示。不明确无法编码 |
| **P0-2** | 版本号格式矛盾 | PRD写vX.Y.Z，数据契约§11含versionCode，已有代码含versionCode。AC-01无法验证 |
| **P0-3** | 与现有VersionTag关系未定义 | 代码库已有VersionTag（主界面底部，仅Debug）。新建还是复用？两处是否共存？ |
| **P0-4** | 启动页背景非固定 | 当前#1A73E8硬编码，AC-03要求"深浅背景均清晰可读"。若未来主题化，#80FFFFFF在白色背景上不可见 |

#### P1 清单

| # | 遗漏场景 | 建议 |
|---|---------|------|
| P1-1 | 版本号文本超长截断 | v2.0.0-beta.1-rc2(123)debug单行溢出行为未定义 |
| P1-2 | IME软键盘遮挡 | D-25已标记P1后续迭代 |
| P1-3 | TalkBack朗读内容 | formatVersionDescription()仅含versionName不含versionCode |
| P1-4 | RTL语言布局 | 阿拉伯语等RTL下v1.0(1)显示方向 |
| P1-5 | Landscape横屏适配 | 距底32dp在横屏下位置语义 |
| P1-6 | 版本号读取失败降级 | BuildConfig不可用时显示什么？

### 12.3 技术视角评审

> **评审日期:** 2026-06-08 | **评审模型:** deepseek-v4-flash | **评分: 6/10**

#### 技术可行性：✅ 可行，但有冲突需先解决

**PRD vs 现状差异：**

| 维度 | PRD 要求 | 现状代码 | 冲突级别 |
|------|----------|----------|:------:|
| 可见范围 | 所有构建（用户可见） | 仅 Debug（`if(!DEBUG) return`） | **P0** |
| 格式 | `vX.Y.Z`（三段语义化） | `v1.0(1)debug`（含code+buildType） | **P0** |
| 颜色 | `#80FFFFFF` 半透明白 | `onSurfaceVariant`（M3 Token） | P1 |
| 位置 | 启动页底部居中 32dp | 主界面底部 8dp + navigationBars inset | **P0** |
| 生命周期 | 启动页可见期间 | 主界面常驻 | **P0** |
| `versionName` | 三段如 `1.0.0` | 当前为 `1.0`（两段） | P1 |

#### 架构影响

| 组件 | 类型 | 影响范围 |
|------|------|----------|
| VersionTag.kt | **修改** | 移除 `!DEBUG` 门控；可能需要两个变体（splash版 vs 主界面版） |
| formatVersionTag() | **修改** | Release 去掉 `buildType` 后缀（D-21）；格式对齐 PRD |
| AnimatedSplashContent() | **修改** | Box 内底部追加 VersionTag 调用 |
| MainActivity.kt | **修改** | @OptIn 标记（D-27）；VersionTag 位置避开 AnimatedVisibility |

**UiState：不需要新增 ViewModel/UiState。** 版本号是编译期常量（BuildConfig），无异步/无状态变化。

#### P0 清单（阻塞开发）

| # | 事项 | 关联 |
|---|------|------|
| P0-1 | **移除 `if (!BuildConfig.DEBUG) return` 门控** — 版本号需在所有构建可见 | R1 |
| P0-2 | **决议 D-21/D-23 与 PRD 格式冲突** — `v1.0(1)debug` vs `v1.0.0` | R3 |
| P0-3 | **版本号放在 AnimatedVisibility 外部** — 确保不随 splash 动画消失 | R2 |
| P0-4 | **VersionTag 位置从主界面底部迁移到 splash 底部** — PRD明确要求启动页底部 | R6 |

#### P1 清单

| # | 事项 | 关联 |
|---|------|------|
| P1-1 | `versionName` 对齐三段语义化（`1.0` → `1.0.0`）或放宽数据契约 | R4 |
| P1-2 | 确认颜色方案：`#80FFFFFF` vs `onSurfaceVariant`（#1A73E8 背景对比度） | R5 |
| P1-3 | 补充 Compose UI 测试覆盖 splash 中版本号渲染 | — |
| P1-4 | MainActivity.kt 添加 @OptIn 标注（D-27） | D-27 |
| P1-5 | 已存在 VersionTagTest.kt 中 Release 断言需同步更新 | 测试回归 |

#### 依赖选型：零新依赖引入，纯现有技术栈

### 12.4 UX 视角评审

> **评审日期:** 2026-06-08 | **评审对象:** PRD v0.1-draft §1-§9 + MainActivity.kt AnimatedSplashContent
> **对照基准:** Material3 规范、WCAG 2.1 AA、Android 无障碍最佳实践、项目已有 VersionTag 组件

#### 综合评分: **5/10**

| 维度 | 评分 | 说明 |
|------|:----:|------|
| 交互流程合理性 | 6/10 | 纯展示无交互，流程简单；但版本号与动画生命周期关系未定义 |
| 边界情况覆盖 | 3/10 | 缺失：FadeOut 动画冲突、深浅模式对比度、Release 构建行为、刘海屏适配、横屏 |
| M3 设计规范 | 4/10 | 未使用 M3 Token 体系；硬编码颜色与项目现有 VersionTag 组件不一致 |
| 无障碍 | 3/10 | 无 contentDescription；对比度严重不达标（见 P0-1） |
| 适配性 | 3/10 | 无 navigationBars/statusBars 适配；无 cutout 安全区；无横屏说明 |

#### P0 清单（阻塞编码）

| # | 严重度 | 问题 | 位置 | 改进建议 |
|---|:------:|------|------|----------|
| **P0-1** | **致命** | **对比度严重不达标** — `#80FFFFFF`（50%半透白）在 `#1A73E8` 蓝色背景上混合后对比度仅约 **1.64:1**，远低于 WCAG AA **4.5:1** 最低要求。PRD 自身 TC-02 也要求 ≥4.5:1，自相矛盾。即使纯白 `#FFFFFF` 在 `#1A73E8` 上也仅 **2.28:1**。 | §9 布局规格 + §10 TC-02 | 方案A（推荐）：改版本号文字为深色 `rgba(0,0,0,0.55)` → 混合色约 `#0A406F` → 对比度 **~4.85:1** ✓。方案B：版本号加半透明深色背景 pill `rgba(0,0,0,0.35)` + 白字 → 保证对比度。方案C：降低启动页背景亮度（改用更深蓝 `#0D47A1`）后再用白字。 |
| **P0-2** | **致命** | **版本号随 FadeOut 消失** — PRD §9 约束要求"版本号必须始终可见（不随动画消失）"，但若将版本号放入 `AnimatedSplashContent` 内部，会被外层 `AnimatedVisibility(exit=fadeOut(500ms))` 一并淡出。 | §9 设计约束 + MainActivity.kt L92-99 | 版本号必须放在 `AnimatedVisibility` 外层的 `Box` 中，独立于 splash 动画容器。伪代码：`Box { AnimatedVisibility(splash) { AnimatedSplashContent } + VersionText(Modifier.align(BottomCenter)) }`。 |
| **P0-3** | **高** | **缺少 contentDescription** — PRD §9 交互规格标注"无交互"，但 TalkBack 仍会扫描静态文本。无 `semantics { contentDescription }` 会导致无障碍用户听到原始版本号字符串（如 `v1.0(1)debug`），语义不完整。 | §9 交互规格 | 参考已有 `formatVersionDescription()` 函数，为版本号 Text 添加 `semantics { contentDescription = "应用版本号 v1.0" }`。 |
| **P0-4** | **高** | **Release 构建行为未定义** — PRD 用户场景 US-01 明确"普通用户在启动页看到版本号"，但项目现有 VersionTag 组件（D-12 决议）在 Release 构建下完全不渲染（`if (!BuildConfig.DEBUG) return`）。PRD 未说明 Release 是否显示、显示格式是否与 Debug 一致。 | §2 US-01 + §3 范围边界 | 明确决策并写入 PRD：(a) Release 也显示，格式仅 `v1.0`（无 versionCode/buildType），或 (b) Release 不显示，用户场景改为仅限内部测试。推荐 (a)，因 ADR-2026-001 已决议 Release 去掉 buildType 后缀。 |

#### P1 清单（应修复，不阻塞）

| # | 严重度 | 问题 | 位置 | 改进建议 |
|---|:------:|------|------|----------|
| **P1-1** | 中 | **动画时间线未定义** — 版本号应与启动页背景同时出现（0ms），而非等待 logo scaleIn 动画 600ms 后才可见。当前 §9 未规定版本号的出现时机。 | §9 交互规格 | 明确：版本号在启动页呈现瞬间即显示，不受 logo 动画影响。实现上置于 `AnimatedVisibility` 外部。 |
| **P1-2** | 中 | **System bars 安全区缺失** — 未声明 `windowInsetsPadding(WindowInsets.navigationBars)` 和 statusBars。Edge-to-Edge 模式下版本号可能被导航栏遮挡。 | §9 布局规格 | 添加 `Modifier.windowInsetsPadding(WindowInsets.navigationBars)`，底部间距在 32dp 基础上叠加 inset。参考已有 VersionTag 组件的 D-16 做法。 |
| **P1-3** | 中 | **硬编码颜色与项目 Token 体系不一致** — 现有 VersionTag 组件使用 `MaterialTheme.colorScheme.onSurfaceVariant`（M3 Token），PRD §9 却用硬编码 `#80FFFFFF`。两套体系并存增加维护成本。 | §9 布局规格 + VersionTag.kt L53 | 如果需要半透明白色效果（蓝色启动页专用），建议定义为独立的语义 Token（如 `splashVersionColor`）并在 Theme 中统一管理，而非硬编码。 |
| **P1-4** | 低 | **版本号格式不一致** — PRD §9 仅描述 "vX.Y.Z" 格式，但代码库 `formatVersionTag()` 产出格式为 `v{name}({code}){buildType}`（如 `v1.0(1)debug`）。PRD 应明确最终显示格式。 | §9 布局规格 | 对齐格式：Debug 构建 `v1.0(1)debug`，Release 构建 `v1.0`（根据 ADR-2026-001）。更新 §9 布局规格描述。 |
| **P1-5** | 低 | **刘海屏/挖孔屏未适配** — 未声明 `displayCutout` 安全区。部分设备顶部状态栏区域可能遮盖版本号（若未来改为顶部显示）。当前底部居中风险较低，但建议标注。 | §9 设计约束 | 新增约束：版本号渲染区域避开 `WindowInsets.displayCutout`。 |
| **P1-6** | 低 | **横屏布局未定义** — 横屏时底部 32dp 可能过近于物理边缘，12sp 字号在小屏横屏设备上需验证可读性。 | §9 设计约束 | 补充：横屏下保持底部居中 + 32dp，不做特殊处理（与竖屏一致）。编码后 640×360dp 横屏模拟器验证。 |

#### 亮点

1. **最小侵入设计** — 不修改现有 logo/动画，设计约束明确，降低回归风险。
2. **纯展示无交互** — 简化交互复杂度，无需处理点击、焦点、手势等边界。
3. **PRD 自身识别了部分风险** — 已知 Pitfalls 已标注"深浅模式可见性"和"contentDescription 缺失"，有自查意识。
4. **VersionTag 组件可部分复用** — 项目已有成熟的版本号格式化函数（`formatVersionTag`/`formatVersionDescription`），避免重复造轮子。

#### PRD §9 一致性检查

| 检查项 | PRD 要求 | 代码库现状 | 一致? |
|--------|----------|-----------|:-----:|
| 位置：底部居中 | ✅ 明确 | — | — |
| 距底 32dp | ✅ 明确 | VersionTag 使用 8dp | ❌ 不一致 |
| 字体 12sp | ✅ 明确 | VersionTag 使用 12sp | ✅ |
| 颜色 #80FFFFFF | ✅ 硬编码 | VersionTag 使用 M3 Token | ❌ P0-1 + P1-3 |
| 字重 400 | ✅ 明确 | VersionTag 未指定 | ⚠️ |
| 背景透明 | ✅ 明确 | — | ✅ |
| 无交互 | ✅ 明确 | — | ✅ |
| 不改变动画 | ✅ 明确 | — | ⚠️ P0-2 |
| 始终可见 | ✅ 明确 | — | ❌ P0-2 |
| contentDescription | ❌ 缺失 | VersionTag 已实现 | ❌ P0-3 |

#### Pitfalls 对照

| Pitfall | 触发? | 说明 |
|---------|:-----:|------|
| 深浅模式可见性 | ✅ | P0-1：当前蓝色背景下白字对比度不足，深色模式下如果启动页背景不变则问题相同 |
| FadeOut 中消失 | ✅ | P0-2：版本号置于动画容器内会随 fadeOut 消失 |
| contentDescription 缺失 | ✅ | P0-3：无内容描述 |

> **结论:** 5/10 分，**不可进入编码**。需先修复 **4 项 P0**（对比度、FadeOut 冲突、contentDescription、Release 行为定义）。P0-1 为致命缺陷，PRD 自身的验收标准 TC-02 无法通过。建议优先解决对比度方案选型（A/B/C），联动更新 §9 颜色规格和 TC-02 验收标准。

### 12.5 QA 视角评审

> **评审日期:** 2026-06-08 | **评审模型:** deepseek-v4-flash | **评分: 3/10**
> **评审基础:** PRD §2 用户场景 + §4 验收标准 + 前三方评审发现 + 代码库 VersionTag/MainActivity 源码

#### 综合评分: 3/10

| 维度 | 评分 | 说明 |
|------|:----:|------|
| AC 可测试性 | 2/10 | AC-01 格式与代码格式矛盾（vX.Y.Z vs v1.0(1)debug）；AC-03 "清晰可读"无量化阈值；AC-04 缺测量协议 |
| 测试覆盖完整性 | 1/10 | 原始 §10 仅 3 个朴素场景，无 Debug/Release 分支、无动画生命周期、无对比度量化 |
| 格式/决议一致性 | 2/10 | PRD 写 vX.Y.Z 但 D-23 决议格式为 v1.0(1)/v1.0(1)debug；PRD 写 "半透明白" 但对比度致命 |
| 边界/异常覆盖 | 2/10 | 缺：超长版本号截断、动画冲突、Release 行为、无障碍朗读 |
| 测试数据可追溯性 | 4/10 | BuildConfig 提供稳定数据源，但格式映射关系在决议->代码->PRD 三处不一致 |

#### P0 清单 — 阻塞测试设计

| # | 问题 | 影响 | 决议引用 |
|---|------|------|----------|
| **P0-1** | **AC-01 格式定义与代码矛盾** — PRD 写 "vX.Y.Z"（三段语义化），代码 formatVersionTag() 产出 "v1.0(1)debug"（含 versionCode+buildType），D-23 决议格式为 "v1.0(1)/v1.0(1)debug"。测试无法对同一场景断言三种不同格式 | 所有验收测试的 Then 子句无法确定预期值 | D-23, §4 |
| **P0-2** | **AC-03 无可量化阈值** — "深色/浅色背景均清晰可读" 为定性描述，无对比度数值、无测量方法、无判定标准。PRD 自身 TC-02 写了 ≥4.5:1 但 §4 未体现 | TC-04 无法编写精确断言 | UX P0-1 |
| **P0-3** | **AC-04 缺测量协议** — "不增加启动页显示延迟" 无测量起点/终点定义（系统 splash 消失？Compose splash 首次渲染？logo 动画完成？），无测量工具指定，无样本量要求 | TC-06 无法执行 | §5 |
| **P0-4** | **Debug/Release 行为未在 AC 中体现** — AC-01~AC-04 均未区分构建类型，但代码库 Release 完全不渲染 VersionTag。测试需两套预期但 AC 只定义了一套 | 无法判断 Release 构建是否通过验收 | 产品 P0-1, 技术 P0-1 |

#### P1 清单 — 应修复，不阻塞测试设计

| # | 问题 | 影响 | 建议 |
|---|------|------|------|
| **P1-1** | **现有 VersionTagTest.kt 断言与 PRD 目标冲突** — 测试 T2 断言 "Release 不渲染"，但 PRD 要求 Release 也显示。迁移后 7 个测试中有 4 个需修改 | 测试代码需同步重构 | 先冻结 PRD 格式决议，再批量更新测试 |
| **P1-2** | **§9.3 交互规格标注 "无交互" 但缺 TalkBack 焦点行为验证** — 无交互 ≠ 无障碍不触及。静态 Text 仍会被 TalkBack 扫描 | TC-05 覆盖了此缺口 | — |
| **P1-3** | **版本号颜色方案未定导致 TC-04 无法固化** — §9.2 标注 "[待决策] 方案A/B/C"，测试预期颜色值不确定 | 需方案选型后方可编写对比度计算的具体预期 | 联动 UX P0-1 决策 |
| **P1-4** | **AC-02 "与 versionName 一致" 覆盖不充分** — 仅验证字符串相等，未覆盖 versionName 变更后重新构建的端到端流程 | TC-01/TC-02 已覆盖 | — |

#### 亮点

1. **BuildConfig 确定性数据源** — 版本号来自编译期常量，无异步/无网络依赖，测试数据可控性强
2. **formatVersionTag/formatVersionDescription 纯函数可单元测试** — 已有 VersionTagTest.kt 覆盖格式化逻辑，TC-01/TC-02 可复用
3. **§9.6 布局层级图明确** — Box/AnimatedVisibility/Text 三层结构清晰，TC-03 的 Given 子句可直接引用
4. **D-21/D-22/D-23 决议提供了可追溯的格式预期** — 测试预期值可从决议而非猜测中推导

#### §10 测试用例质量自评

| 维度 | 评分 | 说明 |
|------|:----:|------|
| AC 覆盖 | 8/10 | 4 个 AC 全覆盖，AC-03 深色模式场景待未来补充 |
| 边界覆盖 | 7/10 | 动画生命周期、无障碍、性能；超长字符串/RTL/IME 延后 |
| 可执行性 | 7/10 | TC-04 依赖颜色方案决策，TC-06 依赖 profiler 工具链 |
| 格式规范 | 10/10 | Given/When/Then 完整，优先级/AC 标注齐备 |

> **结论:** 3/10 分，**PRD 不可进入测试设计**。需先解决 4 项 P0（格式定义、对比度阈值、性能协议、Debug/Release 策略）后验收标准才具备可测试性。当前已产出 §10 的 6 个 Gherkin 测试用例（TC-01~TC-06）作为目标态测试设计，待 PRD 修订后可直接用于测试实现。

### 12.6 讨论决议

| 决议编号 | 决议内容 | 来源 | 状态 |
|----------|----------|------|------|
| R-01 | **Debug/Release 双版本格式：** Debug 显示 `v{name}({code}){buildType}`（如 `v1.0(1)debug`），Release 显示 `v{name}`（如 `v1.0`），对齐 D-21/D-23 | 产品/技术/UX/QA | ✅ 已修订 |
| R-02 | **对比度方案选型A：** 文字色 `rgba(0,0,0,0.55)`，在 #1A73E8 背景上对比度 ~4.85:1，达到 WCAG AA 4.5:1 | UX/QA | ✅ 已修订 |
| R-03 | **版本号独立于 AnimatedVisibility：** 放在 Box 根容器，不参与 splash fadeOut 动画，确保始终可见 | 技术/UX | ✅ 已修订（§9.6 布局层级图） |
| R-04 | **复用 formatVersionTag() 而非新建组件：** splash 场景为专用 Composable 调用现有格式化函数，新增 textColor 参数用于 splash 专用颜色 | 产品/技术 | ✅ 已修订 |
| R-05 | **contentDescription 同步：** 使用 `formatVersionDescription()` 输出 "应用版本号 v1.0"，对齐 D-22 | UX/QA | ✅ 已修订（§9.3） |
| R-06 | **无 ViewModel：** 版本号是 BuildConfig 编译期常量，纯展示无状态变化，符合 YAGNI | 技术 | ✅ 已记录 |

---

> **状态:** 多视角评审已完成，N 项决议待确认。确认后冻结版本号。
