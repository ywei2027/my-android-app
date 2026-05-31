# CLAUDE.md — my-android-app

## 项目概述
Android 应用项目，遵循 MVVM 架构，使用 Jetpack Compose 构建 UI。

## 构建命令
```bash
./gradlew assembleDebug      # Debug APK
./gradlew test                # 运行所有单元测试
./gradlew lint                # Lint 检查
./gradlew connectedAndroidTest # 仪表化测试（需设备）
```

## 技术栈
- UI: Jetpack Compose
- 架构: MVVM (ViewModel + StateFlow)
- 网络: Retrofit + OkHttp
- 本地存储: Room
- DI: Hilt
- 协程: Kotlin Coroutines
- 图片: Coil
- 测试: JUnit 4 + MockK

## 架构分层
```
UI (Composable) → ViewModel (UiState/Event) → Repository → DataSource (Room/Retrofit)
```

规则：
- ViewModel 只暴露 StateFlow<UiState>，不暴露 LiveData
- View 层不直接访问 Repository
- Repository 是唯一的数据调度层
- 所有协程绑定 viewModelScope 或 lifecycleScope，禁止 GlobalScope
- 网络请求和数据库操作必须运行在 Dispatchers.IO

## 命名规范
- Activity: [Feature]Activity (如 LoginActivity)
- ViewModel: [Feature]ViewModel (如 LoginViewModel)
- Repository: [Feature]Repository (如 NoteRepository)
- Composable: PascalCase，以名词开头 (如 NoteListScreen)
- 资源 ID: snake_case (如 ic_add_note)
- 颜色/主题: 使用 Material3 规范

## 代码审查 P0 检查（必须修复）
- 内存泄漏：Activity/Fragment 引用在 ViewModel 中持有
- ANR 风险：主线程 IO 操作
- 必然崩溃：未处理的 NPE、空安全违规
- 协程泄漏：未绑定 lifecycleScope 或 viewModelScope

## Git 规范
- 分支: feature/[功能名], fix/[问题描述]
- 提交信息: 中文或英文，简洁描述变更内容
