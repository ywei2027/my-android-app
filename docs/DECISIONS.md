# 项目决策记录
> 本文档记录架构选型、技术决策及变更历史。

## 变更历史

| 日期 | 变更内容 |
|------|----------|
| 2026-06-08 | 启动页版本号显示 PRD 评审，决议 R-01~R-06 |
| 2026-06-03 | 初始 PRD 评审，决议 D-21~D-25 |

## 决策列表

| 编号 | 日期 | 决策内容 | 状态 |
|------|------|----------|------|
| R-01 | 2026-06-08 | Debug/Release 双版本格式：Debug `v{name}({code}){buildType}` / Release `v{name}`，对齐 D-21/D-23 | ✅ 已修订 |
| R-02 | 2026-06-08 | 对比度方案A：文字色 `rgba(0,0,0,0.55)`，在 #1A73E8 背景上 ~4.85:1 ≥ WCAG AA 4.5:1 | ✅ 已修订 |
| R-03 | 2026-06-08 | 版本号独立于 AnimatedVisibility，放在 Box 根容器不参与 fadeOut | ✅ 已修订 |
| R-04 | 2026-06-08 | 复用 formatVersionTag()，splash 专用 Composable 调用，新增 textColor 参数 | ✅ 已记录 |
| R-05 | 2026-06-08 | contentDescription 使用 formatVersionDescription()，格式 "应用版本号 v1.0" | ✅ 已修订 |
| R-06 | 2026-06-08 | 无 ViewModel — BuildConfig 编译期常量，纯展示无状态变化，符合 YAGNI | ✅ 已记录 |
| D-21 | 2026-06-03 | Release 构建调用 `formatVersionTag(buildType="")` 去掉后缀，函数签名不变 | ✅ 已采纳 |
| D-22 | 2026-06-03 | contentDescription 在 Debug/Release 下与可见文本保持一致 | ✅ 已采纳 |
| D-23 | 2026-06-03 | 示例版本号统一为 `v1.0(1)` / `v1.0(1)debug`，与代码库一致 | ✅ 已采纳 |
| D-24 | 2026-06-03 | US-03 移除（开发者确认不属于用户场景） | ✅ 已移除 |
| D-25 | 2026-06-03 | P1 项（IME适配/labelSmall Token/语义角色/RTL）标记为后续迭代 | 延后处理 |
| D-26 | 2026-06-03 | HTML aria-label 含 versionCode: `应用版本号 v1.0(1) debug 构建` (P0-1 修订) | ✅ 已修订 |
| D-27 | 2026-06-03 | MainActivity 需 `@OptIn(ExperimentalComposeUiApi::class)` 支持 `WindowInsets.isImeVisible` | ✅ 已记录 |
