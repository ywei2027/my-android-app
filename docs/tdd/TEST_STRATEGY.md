# 启动页版本号显示 — 测试策略

> **基于:** DESIGN.md v1.0-confirmed §5 | PRD §10 Gherkin | PRD §11 JSON Schema

---

## 测试用例矩阵

| # | 类型 | 用例 | 输入 | 期望 | 对应 Gherkin |
|---|------|------|------|------|-------------|
| T1 | Unit | Debug formatVersionTag | `buildType="debug"` | `"v1.0.0(1)debug"` | TC-01 |
| T2 | Unit | Release formatVersionTag | `buildType=""` | `"v1.0.0(1)"` | TC-02 |
| T3 | Unit | null VERSION_NAME 降级 | mock null | `"v?.?(1)"` | TC-11 |
| T4 | Unit | formatVersionDescription Debug | — | 含 `"调试版本"` | TC-12 |
| T5 | Unit | formatVersionDescription Release | — | 不含 `"调试版本"` | TC-12 |
| T6 | Unit | 空 buildType | `buildType=""` | 结尾无多余字符串 | TC-02 |
| T7 | Compose | versionTag Text 可找到 | AnimatedSplashContent | `onNodeWithText` 匹配 versionTag | TC-01 |
| T8 | Compose | contentDescription 存在 | AnimatedSplashContent | `onNode(hasContentDescription())` | TC-12 |

## CI 策略

```bash
./gradlew testDebugUnitTest testReleaseUnitTest
```

## 覆盖率目标

| 层 | 目标 |
|----|------|
| `formatVersionTag()` | 100% (纯函数) |
| `formatVersionDescription()` | 100% (纯函数) |
| `AnimatedSplashContent` | 80% (Compose UI) |
