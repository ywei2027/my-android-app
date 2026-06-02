# 主界面底部版本号显示 — UI 设计方案

> **版本:** v0.2-review
> **功能名称:** 主界面底部版本号显示
> **创建日期:** 2026-06-02
> **基于:** PRD v1.0-confirmed §9

---

## 页面清单

| 页面 | 标识 | 类型 | 说明 |
|------|------|------|------|
| 主界面 | page_main | 已有页面（修改） | 在底部追加版本号元素 |

---

## P1 — 主界面（含版本号）

### 线框图

```
┌──────────────────────────────┐
│  TopAppBar                   │
│  "我的应用"                   │
├──────────────────────────────┤
│                              │
│      主界面内容区域            │
│                              │
│                              │
│                              │
├──────────────────────────────┤
│  BottomNavigationBar         │
│  [首页] [发现] [我的]         │
│  ─────────────────────────  │
│          v1.0.0              │  ← 新增
└──────────────────────────────┘
```

### 组件层级树

```
MainScreen (Scaffold)
├── TopAppBar(title = "我的应用")
├── Content: 主界面内容区域
└── BottomBar (Column)
    ├── NavigationBar
    │   ├── NavigationBarItem("首页", Icons.Home)
    │   ├── NavigationBarItem("发现", Icons.Explore)
    │   └── NavigationBarItem("我的", Icons.Person)
    └── VersionTag(versionName)
        └── Text(
              text = "v${BuildConfig.VERSION_NAME}",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(bottom = 8.dp)
                   .semantics { contentDescription = "应用版本号" }
            )
```

### 交互状态机

```
         ┌──────────┐
         │  Idle    │ ← 唯一状态
         └──────────┘
        静态文本，无交互
```

### 状态覆盖表

| 状态 | UI 表现 | 说明 |
|------|---------|------|
| 默认 | 底部显示 `v1.0.0`，onSurfaceVariant 色，12sp | — |
| 暗色模式 | 同上，颜色自动切换为 `onSurfaceVariant` 暗色值 | 无需额外代码 |
| 横屏 | 底部显示，宽度自适应 | padding 不变 |
| 字体缩放 | 随系统字体缩放（12sp） | sp 单位自动适配 |

### Token 映射表

| 设计属性 | Compose Token | 浅色值 | 深色值 | 来源 |
|----------|---------------|--------|--------|------|
| 版本号文字色 | `onSurfaceVariant` | `#49454F` | `#CAC4D0` | Material3 默认 |
| 字体大小 | — | 12sp | 12sp | PRD §9 |
| 底部外边距 | — | 8dp | 8dp | PRD §9 |
| 水平对齐 | `Alignment.CenterHorizontally` | — | — | PRD §9 |

### 组件复用分析

| 组件 | 复用/新增 | 来源 | 修改 |
|------|-----------|------|------|
| Scaffold | 复用 | 现有 MainScreen | 无 |
| TopAppBar | 复用 | 现有 MainScreen | 无 |
| NavigationBar | 复用 | 现有 BottomBar | 无 |
| VersionTag | 新增 | — | 新建 `Text` composable |

### 新增组件清单

| 组件 | 复杂度 | 预估工时 | 说明 |
|------|--------|----------|------|
| VersionTag | 低 | 0.5h | 一个 `Text` + `padding` + `semantics` |

### 架构协调设计

- **无跨 Screen 通信**：版本号标签为静态元素，不涉及事件通道
- **无 BackHandler 冲突**：版本号标签无交互，不消费返回事件
- **WindowInsets 适配**：使用 `Scaffold` 自动处理 `imePadding` 和 `navigationBarsPadding`

---

## 多视角评审记录

> **评审日期:** 2026-06-02
> **评审方式:** delegate_task 三视角并行 (C1 UX交互 / C2 视觉审美 / C3 前端实现)

### 评审总览

| 视角 | 评审模型 | P0 | P1 | P2 | 综合评分 |
|------|----------|----|----|-----|---------|
| C1 UX 交互 | deepseek-v4-flash | 2 | 5 | 3 | — |
| C2 视觉审美 | deepseek-v4-flash | 0 | 3 | 5 | **37/50** |
| C3 前端实现 | deepseek-v4-flash | 4 | 3 | 0 | — |

### P0 问题清单（必须修复）

| # | 来源 | 问题 | 决议 |
|---|------|------|------|
| P0-01 | C1/C3 | 版本信息格式与 D-15 不一致：仅 `v1.0.0`，缺失 VERSION_CODE + buildType | 待确认：扩展为 `v{name}({code}){buildType}` |
| P0-02 | C1/C3 | 未实现 D-12 Debug/Release 条件：Release 构建不应在底部显示版本号 | 待确认：添加 `if (BuildConfig.DEBUG)` 条件 |
| P0-03 | C3 | VersionTag 无挂载点：当前项目无 MainScreen/Scaffold/导航栏架构 | 待确认：需先建设 MainScreen 架构 |
| P0-04 | C3 | darkColorScheme 未配置：深色模式下 Token 无有效值 | 待确认：新建 Theme.kt with darkColorScheme |

### P1 问题清单（建议修复）

| # | 来源 | 问题 |
|---|------|------|
| P1-01 | C2 | 版本号下边距 12px 偏离 8dp 网格体系 |
| P1-02 | C2 | 版本号颜色与导航文字同级（同 Token），视觉层级未降级 |
| P1-03 | C2 | 品牌情感表达薄弱 |
| P1-04 | C1 | WindowInsets 适配描述过于笼统 |
| P1-05 | C1/C3 | 横屏模式空间占用风险（BottomBar Column 占 30% 高度） |
| P1-06 | C1 | 字体缩放极端值未覆盖 |
| P1-07 | C1 | D-17 对比度未在局部背景验证 |
| P1-08 | C3 | 工时估算 0.5h → 实际需 6-8h（前置 MainScreen 架构未计） |

### C2 视觉审美 10 维度详情

| # | 维度 | 评分 | 关键意见 |
|---|------|------|----------|
| 一 | 格式塔感知 | 4/5 | 底部闭合区域清晰，导航图标缺语义 |
| 二 | 视觉层级 | 3/5 | 版本号与导航同色，层级未降级 |
| 三 | 色彩系统 | 4/5 | onSurfaceVariant 对比度 7.3:1(浅)/10.1:1(暗) |
| 四 | 字体排版 | 4/5 | 12px 合理，缺 line-height 显式设置 |
| 五 | 空间与网格 | 3/5 | 下边距 12px 偏离 8dp 网格 |
| 六 | 布局与比例 | 4/5 | 居中正确，比例合理 |
| 七 | 可感知可操作 | 4/5 | aria-label 正确，静态展示符合 PRD |
| 八 | 一致性 | 4/5 | CSS 变量体系规范 |
| 九 | 情感与品牌 | 3/5 | 纯文本，缺乏品牌个性 |
| 十 | 平台与适配 | 4/5 | 暗色模式 @media 完整 |
| **综合** | | **37/50** | 良好，≥30 通过 |

---

## 变更记录

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| v0.1-draft | 2026-06-02 | 初始生成，基于 PRD v1.0-confirmed §9 |
| v0.2-review | 2026-06-02 | 三视角评审完成：4 P0 / 8 P1 / 8 P2 / C2 综合 37/50 |

---

> **状态:** 待确认 (v0.2-review) — 请审阅后回复「确认」冻结进入技术方案。
