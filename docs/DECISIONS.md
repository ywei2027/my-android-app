# DECISIONS.md — 架构决策记录

> 项目: my-android-app
> 最后更新: 2026-06-03

---

## 决策列表

| 编号 | 日期 | 决策内容 | 来源阶段 | 状态 |
|------|------|----------|----------|------|
| D-01 | 2026-06-03 | Release 构建需显示版本号，格式 `v{versionName}`（无 buildType 后缀）；Debug 保持完整格式 | STAGE_PRD | 待确认 |
| D-02 | 2026-06-03 | 不可直接复用 VersionTag（Debug-only 硬编码），需新建无条件组件或重构 | STAGE_PRD | 待实现 |
| D-03 | 2026-06-03 | 底部间距 8dp（对齐现有代码），非 24dp | STAGE_PRD | 已应用 |
| D-04 | 2026-06-03 | VersionTag 新增/修改：移除 `if (!DEBUG) return`，通过 formatVersionTag() 参数控制 Debug/Release 格式差异 | STAGE_PRD | 待确认 |

---

## 变更历史

| 日期 | 变更 |
|------|------|
| 2026-06-03 | 初始创建：启动页面版本号显示 PRD 评审产出 D-01~D-04 |
