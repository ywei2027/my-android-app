# ADR-2026-001: Release 构建 versionTag 格式

- **编号:** D-21 / ADR-2026-001
- **日期:** 2026-06-03
- **状态:** 已采纳
- **关联:** PRD §4 AC-01, DESIGN.md §3.2

## 背景

`formatVersionTag()` 在 Release 构建下返回格式 `v1.0.0(42)release`，其中 `release` 后缀对终端用户无意义。版本号显示功能需要在 Release 构建中仅展示 `v1.0.0(42)`。

## 选项

1. 修改 `formatVersionTag()` 函数签名，新增参数控制是否显示 buildType
2. 在调用侧判断构建类型，传空字符串给 buildType 参数
3. 新增独立函数 `formatReleaseVersionTag()` 仅用于 Release

## 决定

采用选项 2：调用 `formatVersionTag(buildType="")` 去掉后缀，函数签名不变。最小改动且不影响现有调用方。

## 否决方案

- 选项 1：改动函数签名需要同步修改所有现有调用点，影响面大
- 选项 3：增加函数数量，维护负担
