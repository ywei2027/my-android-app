# DECISIONS.md

> 启动页面增加版本号显示功能 — 架构决策记录
> 创建日期: 2026-06-03

## 变更历史

| 日期 | 变更 |
|------|------|
| 2026-06-03 | Stage 3 UI 设计 — 三视角评审完成，P0 自动修订（imePadding + 设计-代码一致性） |

## 决策列表

| 编号 | 日期 | 决策内容 | 状态 |
|------|------|----------|------|
| R-01 | 2026-06-03 | formatVersionTag() 增加 Debug/Release 分支：`val suffix = if (BuildConfig.DEBUG) buildType else ""`，输出 `"v$name($code)$suffix"` | 已确认 |
| R-02 | 2026-06-03 | VersionTag 底部 48dp 安全区约束，避免与交互控件（FAB/按钮/表单域）z-order 碰撞 | 已确认 |
| R-03 | 2026-06-03 | Release 不可逆变更：接受发版回滚成本，上线前通过 Release APK 截图验证 | 已确认 |
| D-12 | 2026-06-03 | Release 构建显示版本号，省略 buildType 后缀（覆盖旧决策 "Release 不显示"） | 已确认 |
| D-13 | — | 版本号颜色使用 `MaterialTheme.colorScheme.onSurfaceVariant` | 保持 |
| D-15 | — | 格式 `v{name}({code}){buildType}`，Release 省略 buildType | 更新 |
| D-16 | — | `windowInsetsPadding(WindowInsets.navigationBars)` 适配手势导航 | 保持 |
| D-17 | — | TalkBack `contentDescription = "应用版本号 v{name}"` | 保持（R-07 建议追加 versionCode，待确认） |
| D-20 | — | 底部内边距 8dp | 保持 |
| UR-01 | 2026-06-03 | VersionTag 追加 `Modifier.imePadding()` 防止键盘弹出时文本重叠 | 已确认 |
| UR-02 | 2026-06-03 | §9 增加设计-代码一致性声明：Release 不渲染 → 移除守卫 + 内部分支 | 已确认 |
| UR-03 | 2026-06-03 | HTML 预览补充字重/行高/间距声明（下一迭代） | 待确认 |
| UR-04 | 2026-06-03 | 字体缩放防御方案（下一迭代评估） | 待确认 |
