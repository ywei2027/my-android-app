# ADR-2026-002: contentDescription 无障碍文本一致性

- **编号:** D-22 / ADR-2026-002
- **日期:** 2026-06-03
- **状态:** 已采纳
- **关联:** PRD §5 非功能性需求

## 背景

版本号显示 Composable 的 `contentDescription` 在 Debug 和 Release 构建下需要与可见文本保持一致，确保 TalkBack 等无障碍服务朗读正确的版本信息。

## 选项

1. Debug/Release 使用不同的 contentDescription 格式
2. Debug/Release 下 contentDescription 与可见文本保持完全一致

## 决定

采用选项 2：contentDescription 在 Debug/Release 下均与可见文本保持一致。例如 Debug 构建可见文本为 `v1.0(1)debug`，则 contentDescription 也为 `v1.0(1)debug`。

## 否决方案

- 选项 1：不一致会导致无障碍测试需要两套预期值，增加维护复杂度
