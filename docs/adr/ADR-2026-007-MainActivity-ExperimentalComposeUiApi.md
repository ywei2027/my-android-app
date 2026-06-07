# ADR-2026-007: MainActivity ExperimentalComposeUiApi

- **编号:** D-27 / ADR-2026-007
- **日期:** 2026-06-03
- **状态:** 已记录
- **关联:** DESIGN.md §4, MainActivity.kt

## 背景

DESIGN 方案中使用 `WindowInsets.isImeVisible` API 实现键盘可见性检测，该 API 需要 `@OptIn(ExperimentalComposeUiApi::class)` 注解。

## 选项

1. 使用 `@OptIn(ExperimentalComposeUiApi::class)` 注解 MainActivity
2. 使用替代 API 避免实验性注解（如 `WindowInsets.ime` 比较）

## 决定

采用选项 1：MainActivity 添加 `@OptIn(ExperimentalComposeUiApi::class)` 注解。该 API 在 Compose 1.5+ 中已稳定使用，风险可控。

## 否决方案

- 选项 2：替代方案需要手动处理 IME 状态变化，代码量增加且容易出错
