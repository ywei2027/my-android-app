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
| D-28 | 2026-06-04 | R-01: v1 移除 LoginScreen，MainActivity 直接展示新闻列表首页 | ✅ 已采纳 |
| D-29 | 2026-06-04 | R-02: "推荐"Tab = NewsAPI top-headlines?country=cn&category=general | ✅ 已采纳 |
| D-30 | 2026-06-04 | R-03: 新闻详情展示 description 摘要 + "阅读原文"外链按钮 | ✅ 已采纳 |
| D-31 | 2026-06-04 | R-04: 列表分页→首页 20 条 + 无限滚动自动加载 | ✅ 已采纳 |
| D-32 | 2026-06-04 | R-05: 离线缓存→Room 100条摘要，7天过期，50MB上限，NetworkBoundResource | ✅ 已采纳 |
| D-33 | 2026-06-04 | R-06: 搜索改为本地 Room 全文搜索（标题+description LIKE），≥3字符，300ms防抖+防竞态 | ✅ 已采纳 |
| D-34 | 2026-06-04 | R-07: 添加 Coil 依赖 io.coil-kt:coil-compose:2.5.0 | ✅ 记录，编码阶段执行 |
| D-35 | 2026-06-04 | R-08: 所有页面含完整状态反馈矩阵（8态列表页+5态详情页+3态图片） | ✅ 已采纳 |
| D-36 | 2026-06-04 | R-09: 所有交互元素 min touch target 48dp + contentDescription | ✅ 已采纳 |
| D-37 | 2026-06-04 | R-10: 搜索中切换 Tab→清空搜索词恢复默认列表 | ✅ 已采纳 |
| D-38 | 2026-06-04 | R-11: 图片加载三态 fallback（loading/error/null） | ✅ 已采纳 |
| D-39 | 2026-06-04 | R-12: 搜索防竞态→ViewModel 持 Job，新搜索 cancel 前一个 | ✅ 已采纳 |
| UR-01 | 2026-06-04 | UI设计: 新增「分页加载中」「分页失败」「离线无缓存」3个状态 | ✅ 已修订 |
| UR-02 | 2026-06-04 | UI设计: IME Search Action→立即搜索（不等防抖），删至1-2字符保持冻结结果 | ✅ 已修订 |
| UR-03 | 2026-06-04 | UI设计: 清除按钮 48×48dp, 卡片 padding 16dp, headlineSmall 24sp | ✅ 已修订 |
| UR-04 | 2026-06-04 | UI设计: CollapsingImage→固定头图(200dp)，v1降级决策 | ✅ 已修订 |
| UR-05 | 2026-06-04 | UI设计: Coil 2.5.0依赖 + composeBom升级 + navArgument DSL | ✅ 编码阶段执行 |
