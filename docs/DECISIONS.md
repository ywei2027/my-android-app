# 项目决策记录
> 本文档记录架构选型、技术决策及变更历史。

## 变更历史

| 日期 | 变更内容 |
|------|----------|
| 2026-06-03 | 初始 PRD 评审，决议 D-21~D-25 |

## 决策列表

| 编号 | 日期 | 决策内容 | 状态 |
|------|------|----------|------|
| D-21 | 2026-06-03 | Release 构建调用 `formatVersionTag(buildType="")` 去掉后缀，函数签名不变 | ✅ 已采纳 |
| D-22 | 2026-06-03 | contentDescription 在 Debug/Release 下与可见文本保持一致 | ✅ 已采纳 |
| D-23 | 2026-06-03 | 示例版本号统一为 `v1.0(1)` / `v1.0(1)debug`，与代码库一致 | ✅ 已采纳 |
| D-24 | 2026-06-03 | US-03 移除（开发者确认不属于用户场景） | ✅ 已移除 |
| D-25 | 2026-06-03 | P1 项（IME适配/labelSmall Token/语义角色/RTL）标记为后续迭代 | 延后处理 |
| D-26 | 2026-06-03 | HTML aria-label 含 versionCode: `应用版本号 v1.0(1) debug 构建` (P0-1 修订) | ✅ 已修订 |
| D-27 | 2026-06-03 | MainActivity 需 `@OptIn(ExperimentalComposeUiApi::class)` 支持 `WindowInsets.isImeVisible` | ✅ 已记录 |
