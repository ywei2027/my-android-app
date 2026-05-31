# DECISIONS.md — my-android-app 技术决策日志

> 每轮技术讨论后追加新决策。Claude Code 每次生成方案前必须先读此文件。
> 否决的备选方案也记录原因，防止 AI 重复提出。

---

## 项目架构

### 2026-05-31
**决策**：采用 MVVM + Jetpack Compose 架构
**原因**：与参照项目保持一致，团队已熟悉 Compose 开发
**否决**：XML View（原因：新项目全面迁移 Compose，不再使用传统 View）

### 2026-05-31
**决策**：使用 StateFlow 管理 UiState，不用 LiveData
**原因**：项目全面使用 Kotlin Coroutines，StateFlow 生命周期管理更安全，支持单元测试更友好
**否决**：LiveData（原因：旧方案，新功能不引入）

### 2026-05-31
**决策**：DI 框架使用 Hilt
**原因**：Annotated DI 减少样板代码，与 ViewModel + Compose 集成成熟
**否决**：Koin（原因：Hilt 编译期检查更安全，参照项目已使用）

---

## 登录模块

### 2026-05-31
**决策**：登录方式采用手机号+验证码
**原因**：内部试点项目，不需要复杂密码体系
**否决**：邮箱+密码（原因：增加开发成本，试点阶段无必要）
