# 项目决策记录
> 本文档记录架构选型、技术决策及变更历史。

## 变更历史

| 日期 | 变更内容 |
|------|----------|
| 2026-06-08 | 启动页版本号显示 PRD 评审（第2轮），决议 R2-01~R2-08 |
| 2026-06-08 | 启动页版本号显示 PRD 评审（第1轮），决议 R-01~R-06 |
| 2026-06-03 | 初始 PRD 评审，决议 D-21~D-25 |

## 决策列表

| 编号 | 日期 | 决策内容 | 状态 |
|------|------|----------|------|
| R2-01 | 2026-06-08 | formatVersionTag() 签名变更保持向后兼容，新增参数设默认值 | ✅ 已采纳 |
| R2-02 | 2026-06-08 | 版本号生命周期：启动即显示，splash 结束保留不消失 | ✅ 已采纳 |
| R2-03 | 2026-06-08 | contentDescription 区分 Debug/Release 构建类型 | ✅ 已采纳 |
| R2-04 | 2026-06-08 | Box 布局：根容器 Alignment.Center + 版本号 BottomCenter，不被 AnimatedVisibility 包裹 | ✅ 已采纳 |
| R2-05 | 2026-06-08 | 导航栏安全区：叠加 WindowInsets.navigationBars 到 bottom padding (32dp + navBarsInset) | ✅ 已采纳 |
| R2-06 | 2026-06-08 | 长版本号溢出：maxLines=1 + TextOverflow.Ellipsis | ✅ 已采纳 |
| R2-07 | 2026-06-08 | BuildConfig 异常：静默降级显示空字符串不崩溃 | ✅ 已采纳 |
| R2-08 | 2026-06-08 | RTL 适配：TextDirection.Content 继承系统 locale | ✅ 已采纳 |
| R2-01 | 2026-06-08 | 嵌套Box布局 + 独立AnimatedVisibility(fadeIn 300ms) | ✅ 已采纳 |
| R2-02 | 2026-06-08 | Release 显示 `v{name}({code})` 不含 buildType | ✅ 已采纳 |
| R2-03 | 2026-06-08 | 版本号颜色 `Color.White.copy(alpha=0.7f)`，对比度6.5:1≥AAA | ✅ 已采纳 |
| R2-04 | 2026-06-08 | 无障碍: Modifier.semantics显式绑定 + contentDescription始终完整 | ✅ 已采纳 |
| R2-05 | 2026-06-08 | 字体缩放上限1.5x | ✅ 已采纳 |
| R2-06 | 2026-06-08 | Debug下替换VersionTag避免新旧重复 | ✅ 已采纳 |
| R2-07 | 2026-06-08 | splash ~1.5s短暂展示，非永久主屏 | ✅ 已采纳 |
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
