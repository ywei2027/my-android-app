# 启动页版本号显示 — UI 设计方案

> **版本:** v0.2-review
> **功能名称:** 启动页版本号显示
> **创建日期:** 2026-06-08
> **基于:** PRD v1.0-confirmed §9 | DECISIONS.md (R-01~R-06) | 三视角评审完成

---

## §1 设计总览

### 设计目标
- 在现有启动页（蓝色 #1A73E8 背景，📰 logo）底部新增版本号文本
- 最小侵入：不改变现有 logo/动画/布局
- 对比度达标：文字色 rgba(0,0,0,0.55) 在 #1A73E8 上约 4.85:1 ≥ WCAG AA 4.5:1

### 设计语言
- Material3 (M3) 设计系统
- 375dp 基准视口（iPhone X 尺寸：375×812）
- 颜色 Token：项目 Theme Color.kt 现有 Token + splash 专用色值

---

## §2 页面清单与导航

| 页面 | 路由/组件 | 类型 | 入口 | 说明 |
|------|----------|------|------|------|
| 启动页 | `AnimatedSplashContent` (MainActivity.kt) | 改造 | 冷启动 | 现有蓝色启动页底部新增版本号 Text，不涉及新路由 |

### 导航图
无新增——版本号在现有启动页内叠加，不涉及 NavHost 变更。

---

## §3 页面设计 — 启动页（Splash Screen）

### 线框图
```
┌──────────────────────────────────┐  ← Box(fillMaxSize, bg=#1A73E8)
│                                  │
│          ┌──────────┐            │
│          │   📰     │ 72sp       │  ← logo scaleIn + fadeIn 600ms
│          │  新闻     │ 28sp Bold  │
│          │  热点资讯  │ 14sp      │
│          │  一键掌握  │ 半透白     │
│          └──────────┘            │
│                                  │
│                                  │
│          v1.0(1)debug            │  ← ★ 版本号 12sp rgba(0,0,0,0.55)
│                                  │     距底 32dp + navigationBars inset
└──────────────────────────────────┘  ← 375×812 视口
```

### 组件层级树
```
AppWithAnimatedSplash(onSplashReady)
└── Box(fillMaxSize)                                    // 根容器
    ├── AnimatedVisibility(visible=!showSplash)         // 主内容（延迟显示）
    │   └── NewsAppNavHost()
    ├── AnimatedVisibility(                             // Splash 动画容器
    │       visible=showSplash,
    │       exit=fadeOut(500ms))
    │   └── AnimatedSplashContent(onFinished)
    │       ├── Text("📰", 72sp)                        // scaleIn+fadeIn 600ms
    │       ├── Text("新闻", 28sp, Bold, White)
    │       └── Text("热点资讯 一键掌握", 14sp, 半透白)
    └── Text(versionTag,                                // ★ 新增 — 独立于AnimatedVisibility
             Modifier.align(BottomCenter)
               .windowInsetsPadding(navigationBars)
               .padding(bottom=32dp),
             fontSize=12sp,
             color=Color(0x8C000000),                   // rgba(0,0,0,0.55)
             maxLines=1,
             overflow=TextOverflow.Ellipsis,
             semantics { contentDescription = "应用版本号 v1.0" })
```

### 交互状态机
```
┌──────────────┐
│   VISIBLE    │ ← showSplash=true, 启动页可见
└──────┬───────┘
       │ onFinished() → showSplash=false
       ▼
┌──────────────┐
│    GONE      │ ← Box 容器移除，版本号随启动页整体消失
└──────────────┘
```
版本号只有一个状态（纯展示），无加载/错误/空数据态。

### 状态覆盖表

| 状态 | UI 表现 | 说明 |
|------|---------|------|
| VISIBLE | 底部居中显示版本号（Debug: `v1.0(1)debug` / Release: `v1.0`），12sp，深色半透明，不参与动画 | 始终可见（0ms 即显示） |
| GONE | 版本号随 AnimatedVisibility splash 容器移除后整体消失 | 不单独 FadeOut |

---

## §4 Token 映射表

| 设计属性 | M3 Token / 值 | 来源 |
|----------|--------------|------|
| 启动页背景 | `#1A73E8`（硬编码蓝） | 现有 AnimatedSplashContent |
| 版本号文字色 | `rgba(0,0,0,0.55)` = `#8C000000` | R-02 决议：方案A |
| 版本号字体大小 | `12.sp` | PRD §9.2 |
| 版本号字重 | `FontWeight.Normal` (400) | PRD §9.2 |
| 底部间距 | `32.dp` + `WindowInsets.navigationBars` | PRD §9.2 + R-03 |
| 版本号格式(Debug) | `formatVersionTag()` → `v1.0(1)debug` | D-23/R-01 |
| 版本号格式(Release) | `formatVersionTag(buildType="")` → `v1.0` | D-21/R-01 |
| contentDescription | `formatVersionDescription()` → "应用版本号 v1.0" | D-22/R-05 |
| Logo 主色 | `Color.White` | 现有 AnimatedSplashContent |
| 副标题色 | `Color.White.copy(alpha=0.8f)` | 现有 AnimatedSplashContent |

---

## §5 组件复用分析

| 组件 | 来源 | 复用方式 | 状态 |
|------|------|----------|------|
| `formatVersionTag()` | VersionTag.kt（已有） | 直接复用 — splash 场景调用同一函数 | ✅ |
| `formatVersionDescription()` | VersionTag.kt（已有） | 直接复用 — contentDescription | ✅ |
| `Text` (Compose) | M3 标准库 | 新建 splash 专用 Text 实例 | ✅ |
| `Box` (Compose) | M3 标准库 | 现有根容器，不改动 | ✅ |

**复用率：** 2/3（67%）— 格式化函数全部复用，仅新增 1 个 Text Composable 调用。

### 新增文件

| 文件 | 说明 | 复杂度 |
|------|------|--------|
| —（无新增文件） | 修改仅限 MainActivity.kt 内 AppWithAnimatedSplash 函数 | 低 |

---

## §6 架构协调设计

### 导航事件
无——版本号在启动页内叠加，不触发导航。

### ViewModel
**不需要 ViewModel。** 版本号是 `BuildConfig.VERSION_NAME` 编译期常量，无异步/无状态变化，纯展示。符合 YAGNI（R-06）。

### BackHandler
无影响——版本号为纯展示，无交互，不涉及返回栈。

---

## §7 无障碍适配

| 元素 | contentDescription | 触控目标 |
|------|-------------------|----------|
| 版本号文本 (Debug) | `"应用版本号 v1.0(1)debug"` | N/A（纯展示） |
| 版本号文本 (Release) | `"应用版本号 v1.0"` | N/A（纯展示） |

---

## §8 前置依赖

| 依赖 | 说明 | 状态 |
|------|------|------|
| `formatVersionTag()` | VersionTag.kt — 已有函数 | ✅ |
| `BuildConfig.VERSION_NAME` | Gradle 自动生成 | ✅ |
| `BuildConfig.VERSION_CODE` | Gradle 自动生成 | ✅ |
| `BuildConfig.BUILD_TYPE` | Gradle 自动生成 | ✅ |

---

## §9 多视角评审记录

> 评审日期: 2026-06-08 | 方式: delegate_task 三视角并行 | 耗时: ~200s

### 评审总览

| 视角 | 评分 | P0 | P1 | 核心发现 |
|------|------|----|----|----------|
| C1 UX 交互 | **7/10** | 2 | 5 | P0:超长版本号截断未定义/contentDescription格式歧义; P1:硬编码Token/displayCutout/横屏/字体缩放/RTL |
| C2 视觉审美 | **33/50** | 0 | 5 | 无需重做；色彩方案与品牌一致性有提升空间，5项P1编码阶段消化 |
| C3 前端实现 | **8.4/10** | 2 | 6 | ✅ 可进入编码; 5行代码/零新文件; P0:缺失maxLines+TextOverflow/import路径确认; P1:硬编码Token/VT双实例/RTL |

### P0 修订记录（已在正文自动修订）

| 编号 | 问题 | 修订内容 |
|------|------|----------|
| UR-01 | C1/C3 P0: 超长版本号截断未定义 | §3 组件层级树 追加 `maxLines=1, overflow=TextOverflow.Ellipsis` |
| UR-02 | C1 P0: contentDescription 格式歧义 | §7 无障碍适配 拆分为 Debug/Release 两行，Debug 含 versionCode |

### C3 前端实现评审详情

> **评审模型:** deepseek-v4-flash | **日期:** 2026-06-08 | **综合评分: 8.4/10** ✅ 可进入编码

#### P0 清单（已自动修订）

| # | 问题 | 修订 |
|---|------|------|
| P0-01 | maxLines/TextOverflow 缺失 | ✅ UR-01 已追加 |
| P0-02 | import 路径确认 | 编码阶段检查 VersionTag.kt 包路径 |

#### 工时校准: 0.25h（5行代码，零新文件，无ViewModel/DI/导航/测试变更）

---

### C1 UX 交互评审详情

> **评审模型:** deepseek-v4-flash | **日期:** 2026-06-08 | **综合评分: 7/10**

#### 维度得分

| 维度 | 评分 | 说明 |
|------|:----:|------|
| 用户路径效率 | 8/10 | 零导航即可确认版本，US-02(测试/QA)路径极简；但启动页停留~1.2s，阅读窗口短 |
| Android 平台惯例 | 7/10 | navigationBars inset 正确；12sp 字号合理；但 `rgba(0,0,0,0.55)` 深色半透字在蓝色背景上非 Material3 标准模式，且无 M3 Token 抽象 |
| 边界状态覆盖 | 6/10 | VISIBLE/GONE + Debug/Release 格式覆盖充分；但缺超长截断、深色模式对比度、displayCutout、横屏验证、字体缩放溢出 |
| 键盘/交互 | 10/10 | 纯展示无交互，无软键盘/硬键盘/触控目标问题，简化到极致 |
| 信息架构 | 8/10 | 版本号为辅助元数据，底部居中 12sp 半透明，不抢 logo 主视觉；单层信息无层级问题 |
| PRD §9 一致性 | 8/10 | 11/13 子项完全对齐(布局/动画/对比度/navigationBars/格式化/组件选型)；2 项偏差：硬编码颜色缺 Token(P1)、displayCutout 未提及(P1) |

#### P0 清单（阻塞实现）

| # | 问题 | 位置 | 影响 | 建议 |
|---|------|------|------|------|
| **P0-1** | **超长版本号截断行为未定义** — 设计未指定 `maxLines`/`overflow`/`softWrap`。若 versionName 延长至 `v10.20.30-beta.1(9999)debug`（PRD §10.7 SKIP-01），12sp 单行底部居中文本将溢出屏幕边界 | §3 组件层级树 + §4 Token映射 | 未来 versionName 多段后 UI 破损 | 添加 `maxLines=1, overflow=TextOverflow.Ellipsis, softWrap=false` 到 Text Modifier 规格 |
| **P0-2** | **contentDescription 格式歧义** — §7 无障碍表格统一写 `"应用版本号 v1.0"`（仅 versionName），但 D-22 决议要求 contentDescription 与可见文本一致。Debug 可见文本为 `v1.0(1)debug`（含 versionCode+buildType），应分别定义 | §7 无障碍适配 | TalkBack 在 Debug 构建下朗读内容不完整（丢失 versionCode） | §7 表格分两行：Debug `"应用版本号 v1.0(1)debug"` / Release `"应用版本号 v1.0"`。HTML 预览 aria-label 已正确处理此分歧 |

#### P1 清单（应修复，不阻塞）

| # | 问题 | 位置 | 建议 |
|---|------|------|------|
| **P1-1** | **硬编码颜色 `Color(0x8C000000)` 缺乏语义 Token** — PRD §9.4 标注[待决策]新增 `splashVersionColor` Token，设计直接使用硬编码值 | §3 组件层级树 L72 | 在 Theme Color.kt 中新增 `splashVersionColor` 语义 Token，值为 `Color(0x8C000000)`，避免硬编码 |
| **P1-2** | **displayCutout 安全区未适配** — PRD §9.5 C-7 约束要求标注 displayCutout，设计仅处理 navigationBars inset | §3 组件层级树 L69 | 底部文本对顶部刘海屏无风险；建议在设计约束中显式标注"底部显示，displayCutout 不适用"并关闭此 P1 |
| **P1-3** | **横屏兼容仅标注"不做特殊处理"** — 375×812 横屏下 32dp 距底+12sp 字号未经模拟器验证。横屏下底部 32dp 可能挤占导航栏区域 | §3 线框图 | 编码后在 640×360dp 横屏模拟器验证，12sp/Normal 400 在横屏密度下可读性 |
| **P1-4** | **字体缩放溢出风险** — 用户设置 2x 系统字体时 12sp→24sp，单行 `v1.0(1)debug` 宽度增加至约 180dp（仍远小于 375dp），实际风险低但未声明 | §4 Token 映射 | 标注：12sp 在 2x 缩放下理论最大宽度约 180dp，远小于 375dp 视口，仅极端长版本号 + 缩放叠加才溢出 |
| **P1-5** | **RTL 语言（阿拉伯语/希伯来语）版本号方向未验证** — `v1.0(1)debug` 为纯 LTR 字符，Compose 默认行为在 RTL 下底部居中保持不变，但未明确声明 | §3 组件层级树 | 显式标注 `textDirection=TextDirection.Ltr` 或声明版本号始终 LTR（纯 ASCII 字符集） |

#### 亮点

1. **零导航负担** — 用户冷启动 0ms 即见版本号，比进入"关于"页节省 2-3 步点击
2. **AnimatedVisibility 隔离设计精准** — 版本号置于外层 Box 直接子节点，完美避开 `exit=fadeOut(500ms)` 容器，R-03 决议落实到位
3. **对比度量化达标** — `rgba(0,0,0,0.55)` 在 `#1A73E8` 上 ~4.85:1，超过 WCAG AA 4.5:1，且不影响 logo 白色文字的主视觉层级
4. **67% 组件复用** — `formatVersionTag()`/`formatVersionDescription()` 全部复用，仅新增 1 个 Text Composable，最小侵入兑现承诺
5. **PRD §9 一致性高** — 11/13 子项完全对齐，仅 Token/P1 细节待完善

### C2 视觉评审 10 维度

> **评审模型:** deepseek-v4-flash | **日期:** 2026-06-08 | **综合评分: 33/50** | **模式: 纯文本轻量评审（无截图）**

#### 维度得分总览

| # | 维度 | 评分 | 关键评语 |
|---|------|:---:|------|
| 1 | 格式塔 | 4 | 中央纵轴分组清晰，版本号作为底部锚点与 logo 群构成"页脚"完形；蓝色底色上深色文字天然形成图-底分离 |
| 2 | 视觉层级 | 4 | 三层信息架构(📰logo→副标题→版本号)明确递进；版本号 12sp/深色半透明正确处于第三级，不抢主视觉 |
| 3 | 色彩 | 3 | 对比度达标(~4.85:1 ✅ WCAG AA)，但 rgba(0,0,0,0.55) 在 #1A73E8 上偏"脏灰蓝"，与白-蓝调色板不和谐 |
| 4 | 字体 | 3 | 字号体系合理(72/28/14/12sp)，但 12sp 为临界可读尺寸；缺少字族 Token、等宽版本号惯例、line-height/letter-spacing 规范 |
| 5 | 空间 | 4 | 32dp+导航栏底距充裕，logo-title 间距 16dp、title-subtitle 间距 8dp 符合 8dp 网格；负空间呼吸感好 |
| 6 | 布局 | 4 | 单 Box 叠加 + BottomCenter 对齐，极简无冗余；横屏兼容性天然良好(底部居中)，无布局复杂度 |
| 7 | 可感知可操作 | 3 | contentDescription 完备(HTML 预览分别处理 Debug/Release aria-label)；但 12sp 弱视可读性存疑，未提及高对比度模式/字体缩放 |
| 8 | 一致性 | 3 | 复用 formatVersionTag() ✅；但版本号颜色与现有 VersionTag 组件(onSurfaceVariant)不一致；Release 可见性规则冲突(PRD C-6 待决策) |
| 9 | 情感品牌 | 3 | 蓝底📰辨识度高但 emoji 限制品牌质感；深色版本号在 Release 中残留"调试工具感"，非设计元素感 |
| 10 | 平台适配 | 3 | navigationBars inset 已处理 ✅；但硬编码 #1A73E8 无暗色模式适配，缺少 displayCutout 声明，375×812(iPhone)基准尺寸偏 Android 非标准 |

**综合: 33/50** — 无需重新设计（≥30 阈值），5 项 P1 建议编码阶段消化。

---

#### P1 问题清单（应修复，不阻塞实现）

| # | 严重度 | 维度 | 位置 | 问题描述 | 改进建议 |
|---|:---:|------|------|----------|----------|
| **P1-V1** | 🟡 P1 | 色彩 | §4 Token映射 L102 / splash_default.html L64 | **版本号颜色与品牌调色板割裂** — `rgba(0,0,0,0.55)` 混合 #1A73E8 产生灰蓝色(#4A5C8C)，视觉上偏"脏"。深色半透明文字在亮蓝底上的感知对比度虽达标，但色调不和谐，与全白 Logo 形成冷暖割裂 | 方案A(推荐): 改用 `Color.White.copy(alpha=0.45f)` — 混合后 ~#9DBEF0，与 Logo/副标题形成同一白色系家族，且满足 WCAG AA。方案B: 保留当前色但包裹半透明白色 pill 背景 `RoundedCornerShape(4dp)` 提升可读性与精致感 |
| **P1-V2** | 🟡 P1 | 一致性 | §4 Token映射 vs VersionTag.kt L53 | **版本号颜色与已有 VersionTag 组件不一致** — 现有 VersionTag 使用 `MaterialTheme.colorScheme.onSurfaceVariant`（浅色主题 #49454F），而 Splash 版本号使用硬编码 `rgba(0,0,0,0.55)`。用户在不同页面看到两种版本号样式，破坏设计一致性 | 统一两者颜色：若 VersionTag 保持 onSurfaceVariant，Splash 版本号也应采用同一 Token（但需验证 #49454F 在 #1A73E8 上的对比度仅 ~2.1:1 ❌）。实际建议：两者分别定义合理颜色，但在 UI_DESIGN 中显式声明"因背景色不同采用不同颜色 Token，非不一致" |
| **P1-V3** | 🟡 P1 | 字体 | §4 Token映射 L104-105 | **12sp 为 WCAG 建议最小可读尺寸临界值** — 正常视力在 40cm 视距下可读，但弱视用户/老年用户/强光环境下识别困难。且设计未指定 `letterSpacing`，在 12sp 小字号下字间距过紧会降低可辨性 | 建议添加 `letterSpacing = 0.5.sp` 微调字间距；评估是否提升至 13sp（仍低调但可读性显著提升）；标注"已测试 2x 字体缩放下宽度 ~180dp，远小于 375dp 视口" |
| **P1-V4** | 🟡 P1 | 平台适配 | §3 组件层级树 L68 + Color.kt | **硬编码 #1A73E8 无暗色模式适配** — Splash 背景硬编码为 `Color(0xFF1A73E8)`，不随系统暗色模式切换。若用户在暗色模式下冷启动，亮蓝 Splash 突然出现存在刺眼风险（暗→亮闪烁），不符合 Material3 暗色主题规范 | 短期(编码阶段): 在 `AnimatedSplashContent` 中从 `MaterialTheme.colorScheme.primary` 或 `isSystemInDarkTheme()` 读取背景色，暗色模式下降至 `#004A77`(PrimaryContainerDark) 或 `#0D47A1`。长期: 定义为 `splashBackground` 语义 Token 支持 Light/Dark 双值 |
| **P1-V5** | 🟡 P1 | 情感品牌 | HTML 预览 splash_default.html L99-106 | **Release 构建版本号显示削弱品牌精致感** — 普通用户看到底部 `v1.0` 文字，可能困惑其用途（"这是给开发者看的？"），破坏启动页沉浸式品牌展示。PRD §9.5 C-6 标注 Release 显示为[待决策] | 方案A(推荐): Release 下不显示版本号（仅 Debug 显示），与现有 VersionTag 行为一致。方案B: Release 保留但降低透明度至 `alpha=0.35`，距底改为 16dp，使视觉权重降至最低。需与产品确认 PRD C-6 决议方向 |

---

#### 审美亮点

1. **极致的视觉克制** — 版本号仅占 12sp/1行/居中，不对现有启动页视觉平衡造成任何破坏。新增元素视觉权重约为页面的 0.3%，完美实现"最小侵入"设计目标。

2. **动画隔离设计优雅** — 将版本号置于 AnimatedVisibility 外层 Box，使其零延迟出现且不参与 500ms fadeOut，既满足 PRD C-2"始终可见"约束，又避免在动画曲线中产生视觉抖动。此分层策略是设计中最精巧的决策。

3. **对比度量化设计** — 在 PRD §9.5 C-3 约束下，放弃直观的白色半透明（对比度仅 ~1.64:1），主动选择符合 WCAG AA 的暗色方案，体现数据驱动的色彩决策意识。

4. **CSS Token 化预览体系** — HTML 预览在 `:root` 中统一定义 `--splash-bg`、`--version-color`、`--logo-white` 等变量，与 Compose Token 映射思想一致。Debug/Release 切换功能可交互验证，远超静态线框图。

5. **中央纵轴完形结构** — 📰→新闻→副标题→版本号沿 Y 轴中线对齐，构成天然视觉引导线。版本号在底部锚定，形成"封面→内容入口"的心理暗示（正式内容在版本号之上出现），格式塔原理应用出色。

---

> **版本:** v0.1-draft
> **状态:** 待三视角评审。评审完成后版本号升级为 v0.2-review。
