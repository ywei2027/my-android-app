# ADR-2026-006: HTML aria-label 含 versionCode

- **编号:** D-26 / ADR-2026-006
- **日期:** 2026-06-03
- **状态:** 已修订 (P0-1)
- **关联:** PRD §4 AC-01

## 背景

PRD 评审 P0-1 项：HTML UI 预览中版本号 Composable 的 `aria-label`（对应 Android contentDescription）需要包含 versionCode 以提供完整语义。

## 选项

1. aria-label 仅显示版本名（如 `应用版本号 v1.0(1)`）
2. aria-label 包含 versionCode（如 `应用版本号 v1.0(1) debug 构建`）

## 决定

采用选项 2：HTML aria-label 包含 versionCode 和构建类型信息。格式为 `应用版本号 v{versionName}({versionCode}) {buildType} 构建`。

## 否决方案

- 选项 1：缺少 versionCode 信息，无障碍场景下用户无法获取完整版本标识
