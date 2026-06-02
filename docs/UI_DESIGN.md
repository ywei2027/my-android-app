# 主界面底部版本号显示 — UI 设计方案

> **版本:** v0.2-review
> **功能名称:** 主界面底部版本号显示
> **创建日期:** 2026-06-02

---

## §1 页面层级树

```
MainScreen (Scaffold)
├── TopAppBar: 标题「我的应用」
├── Content: 主界面内容区
└── BottomBar
    ├── BottomNavigation: 首页 / 发现 / 我的
    └── VersionText: v1.0.0 (12sp, onSurfaceVariant)
```

## §2 交互状态机

```
[应用启动] → [主界面渲染] → [底部版本号静态展示]
```
无交互状态切换，纯静态展示。

## §3 Token 映射表

| 设计属性 | Token | 浅色值 | 深色值 |
|----------|-------|--------|--------|
| 版本号文字色 | `onSurfaceVariant` | `#49454F` | `#CAC4D0` |
| 字体大小 | — | 12sp | 12sp |
| 底部内边距 | — | 8dp | 8dp |

## §4 组件复用分析

- **BottomNavigation**: 复用现有底部导航组件
- **VersionText**: 新建 `Text` composable，位于 BottomBar 下方 8dp
- 颜色直接引用 `MaterialTheme.colorScheme.onSurfaceVariant`，自动适配深浅色

## §5 三视角评审摘要

| 视角 | 评分 | 关键意见 |
|------|------|----------|
| UIC 交互 | 4/5 | 纯静态无交互风险，底部分隔清晰 |
| 视觉审美 | 3.2/5 | 颜色 Token 合规，但过于朴素；建议 10sp 更低调 |
| 前端实现 | 4/5 | `onSurfaceVariant` + `BuildConfig.VERSION_NAME` 无风险 |

### P0 问题
- 无

### P1 问题
- P1-01: 版本号字体建议从 12sp → 10sp，更低调

### P2 问题
- P2-01: 可考虑 debug 版本额外显示 build number

## §6 交互原型

- HTML 预览: `docs/ui-preview/index.html`（375px 视口，Material3 风格）
- 截图: 简化模式未生成 Playwright 截图

---

> **状态:** 待确认 (v0.2-review) — 请审阅后回复「确认」冻结进入技术方案。
