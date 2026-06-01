# 技术方案 — 全局搜索功能

> 版本：v1.0-confirmed | 状态：已冻结 | 基于 [PRD v1.0-confirmed](../docs/PRD.md) + [UI_DESIGN v1.0-confirmed](../docs/UI_DESIGN.md)

---

## 1. 架构设计

### 1.1 整体分层

```
┌─────────────────────────────────────────────────────────┐
│  UI Layer (Compose)                                     │
│  CalculatorScreen ← AnimatedVisibility ← SearchScreen   │
│       │                        │                        │
│       ▼                        ▼                        │
│  CalculatorViewModel    SearchViewModel                 │
│  + ExpandHistoryAndScroll  + SearchUiState (8态)        │
│  + SharedFlow<CalculatorEvent>                          │
│       │                        │                        │
│       ▼                        ▼                        │
│  CalculatorHistoryStore  SearchRepository               │
│       │                  │                              │
│       └──────────────────┤                              │
│                          ▼                              │
│  Domain Layer                                           │
│  ┌─────────────────────────────────────────┐            │
│  │ SearchSource (interface)                │            │
│  │   └── CalculatorHistorySearchSource     │            │
│  │ SearchHistoryStore (interface)          │            │
│  │   ├── EncryptedSearchHistoryStore (prod)│            │
│  │   └── InMemorySearchHistoryStore (test) │            │
│  │ SearchResultItem (data class)           │            │
│  │ SearchQuery (data class)                │            │
│  └─────────────────────────────────────────┘            │
│                          │                              │
│                          ▼                              │
│  Data Layer                                             │
│  ┌─────────────────────┐  ┌──────────────────────────┐  │
│  │ CalculatorHistory-  │  │ EncryptedSharedPrefs     │  │
│  │ Store (DataStore)   │  │ "search_history_store"   │  │
│  └─────────────────────┘  └──────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

### 1.2 模块划分

| 层 | 模块 | 职责 |
|----|------|------|
| UI | `ui/search/` | SearchScreen, SearchBar, HighlightedText, 各状态 Composable |
| UI | `ui/search/SearchViewModel.kt` | 搜索状态机、debounce、竞态控制、navigation 事件 |
| UI | `ui/calculator/CalculatorScreen.kt` | 嵌入 DockedSearchBar + isSearchExpanded 状态 + BackHandler |
| UI | `ui/calculator/CalculatorViewModel.kt` | 新增 `ExpandHistoryAndScroll` 事件 + `SharedFlow<CalculatorEvent>` |
| Domain | `domain/search/SearchSource.kt` | 搜索数据源接口 |
| Domain | `domain/search/CalculatorHistorySearchSource.kt` | 计算历史双字段 contains 搜索 |
| Domain | `domain/search/SearchHistoryStore.kt` | 搜索历史持久化接口 |
| Domain | `domain/search/SearchResultItem.kt` | 搜索结果 data class |
| Domain | `domain/search/SearchRepository.kt` | 搜索数据层接口（提升自 data/） |
| Data | `data/search/SearchRepositoryImpl.kt` | 实现：Hilt @IntoSet 聚合 SearchSource |
| Data | `data/search/EncryptedSearchHistoryStore.kt` | AES 加密实现 |
| Data | `data/search/InMemorySearchHistoryStore.kt` | 测试用内存实现 |
| DI | `di/SearchModule.kt` | Hilt 绑定 |

### 1.3 关键架构决策

| # | 决策 | 依据 |
|---|------|------|
| AD1 | SearchScreen 作为 AnimatedVisibility overlay，非独立路由 | PRD §6 S11：嵌入 CalculatorScreen，非独立导航目标 |
| AD2 | `SearchSource` 接口 + `CalculatorHistorySearchSource` 实现 | PRD S1/S8：v1.0 计算历史，v1.1 扩展多数据源 |
| AD3 | `CalculatorOutputEvent` 独立 sealed interface + `SharedFlow` 通道 | P0-D1,D2 修订：`ExpandHistoryAndScroll` 不从属 `CalculatorEvent` |
| AD4 | `SearchRepositoryImpl` 使用 Hilt `@IntoSet` multibinding | P0-D3 修订：替代 `Provider` + `by lazy`，可纯 JVM 测试 |
| AD5 | 搜索历史 AES 加密 + 退出登录清除 | PRD S2：EncryptedSharedPreferences；LoginViewModel.onLogout() trigger |
| AD6 | `historyListState` 在 CalculatorScreen 层创建并传入 Sheet | P0-D4 修订：外部 `animateScrollToItem` 需要 hoist state |
| AD7 | SearchViewModel 使用 `SavedStateHandle` | PRD S17：配置变更保留搜索状态 |
| AD8 | Snackbar 错误队列：Error > 复制 > 删除 | UI R3 UR3-10 |
| AD9 | `withTimeout` 仅捕获 `TimeoutCancellationException`，不捕获 `CancellationException` | P0-D5,D6 修订：`searchJob?.cancel()` 由协程框架传播 |

---

## 2. 数据流

### 2.1 搜索完整流程

```
User Input (onValueChange)
  → SearchViewModel.onQueryChange(query)
    ├─ isBlank() 检查（UR3-8）→ return
    ├─ maxLength=100 检查 → Toast
    ├─ searchJob?.cancel() 竞态控制（PRD S4）
    ├─ searchJob = viewModelScope.launch(Dispatchers.IO) {
    │     delay(300)  // debounce
    │     _uiState.value = Loading(query)
    │     val startMs = System.currentTimeMillis()
    │     val results = try {
    │       withTimeout(5_000) { searchRepository.search(query) }
    │     } catch (e: TimeoutCancellationException) {
    │       Timber.e(e, "Search timeout: query=%s", query)
    │       _uiState.value = Error(query, "搜索超时，请重试")
    │       return@launch
    │     }  // CancellationException 不捕获，由协程框架传播
    │     val latencyMs = System.currentTimeMillis() - startMs
    │     Timber.i("Search completed: query=%s, count=%d, latency=%dms", query, results.size, latencyMs)
    │     _uiState.value = if (results.isEmpty()) Empty(query) else Results(query, results, results.size)
    │     LaunchedEffect: announceForAccessibility(...)
    │   }
    │   // 注意：不捕获 CancellationException — searchJob?.cancel() 触发时框架自动取消，
    │   // StateFlow 仍为上一次值（Loading），无需切换到 Error
    └─ IME Search（跳过 debounce，立即触发）

SearchRepository.search(query)
  → sources.forEach { source → source.search(query) }  // @IntoSet，并行 awaitAll 预留
  → CalculatorHistorySearchSource.search(query):
      calcHistoryStore.first()
        .map { SearchResultItem(it, highlightRanges(query, it.expression + it.result)) }
        .filter { it.highlightRanges.isNotEmpty() }
        .take(200)

错误日志
  → Timber.d(TAG, "Search started: query=%s", query)    [onQueryChange]
  → Timber.i(TAG, "Search done: %d results, %dms", n, ms) [结果返回]
  → Timber.e(e, "Search failed: query=%s", query)         [catch 非取消异常]
  → Timber.d(TAG, "Search cancelled: newQuery=%s", query)  [searchJob?.cancel()]
```

### 2.2 搜索结果点击 → 计算器定位

```
User 点击结果项 (itemId)
  → SearchViewModel 发送: _events.emit(SearchEvent.NavigateToCalculator(itemId))
  → SearchScreen LaunchedEffect 收集:
      dismiss overlay (250ms collapse)
      calculatorViewModel.onOutputEvent(ExpandHistoryAndScroll(itemId))
  → CalculatorViewModel:
      outputEventChannel.emit(ExpandHistoryAndScroll(itemId))
        // outputEventChannel: MutableSharedFlow<CalculatorOutputEvent>(
        //   replay=0, extraBufferCapacity=1, onBufferOverflow=DROP_OLDEST)
  → CalculatorScreen LaunchedEffect(outputEvents):
      when (event) {
        is ExpandHistoryAndScroll -> {
          historySheetState.expand()
          historyListState.animateScrollToItem(event.itemId)  // LazyListState 由 Screen 层创建
          highlightAnimation(event.itemId)  // primaryContainer → transparent 2s fade
        }
      }
```

> **P0-D4 修订**：`historyListState = rememberLazyListState()` 在 CalculatorScreen 层创建，通过参数传入 `CalculatorHistorySheet`。Sheet 内 `LazyColumn(state = historyListState, ...)` 使用外部传入的 state，确保 `animateScrollToItem` 可被外部调用。

### 2.3 搜索历史 FIFO 淘汰流程

```
addQuery(" 123×2.34 ")  // 带空格
  → trim() → "123×2.34"
  → lowercase() → "123×2.34"
  → 去重检查: history.some { existing.trim().lowercase() == trimmed }
      true  → 更新时间戳，排序 → 移至顶部
      false → prepend + .take(5) → FIFO 淘汰最旧
  → EncryptedSearchHistoryStore.save(history)
```

### 2.4 错误降级路径

```
EncryptedSearchHistoryStore.load()
  → success → history
  → KeystoreException → Log.w + _errors.emit + return emptyList()
  → IOException → Log.e + _errors.emit + return emptyList()
  → 任意异常 → 空列表降级

CalculatorHistorySearchSource.search()
  → DataStore read 成功 → 结果
  → DataStore read 失败 → _errors.emit + throw → ViewModel 捕获 → Error Snackbar
```

---

## 3. 接口定义

### 3.0 CalculatorOutputEvent（UI→UI 通信通道）

```kotlin
// ui/calculator/CalculatorOutputEvent.kt
/** CalculatorViewModel → CalculatorScreen 单向 UI 事件（独立于用户输入事件） */
sealed interface CalculatorOutputEvent {
    data class ExpandHistoryAndScroll(val itemId: String) : CalculatorOutputEvent
}
```

> **设计决策（P0-D1, P0-D2 修订）**：`ExpandHistoryAndScroll` 不从属于 `CalculatorEvent` sealed interface。`CalculatorEvent` 承载用户输入（Digit, Operator, Equals...），`CalculatorOutputEvent` 承载 VM→UI 的导航/动画指令。`CalculatorViewModel` 暴露出 `val outputEvents: SharedFlow<CalculatorOutputEvent>`（`replay=0, extraBufferCapacity=1, onBufferOverflow=DROP_OLDEST`），CalculatorScreen 通过 `LaunchedEffect` 收集。

### 3.1 SearchSource

```kotlin
// domain/search/SearchSource.kt
interface SearchSource {
    /** 搜索名称，用于多源聚合时的来源标识 */
    val name: String

    /** 按关键词搜索，返回带高亮范围的结果列表 */
    suspend fun search(query: String): List<SearchResultItem>
}
```

### 3.2 SearchResultItem

```kotlin
// domain/search/SearchResultItem.kt
data class SearchResultItem(
    /** 来源数据源 */
    val source: String,
    /** 原始历史记录 */
    val history: CalcHistory,
    /** 需要高亮的字符范围（0-indexed，已 coerceIn） */
    val highlightRanges: List<IntRange>,
)
```

### 3.3 SearchHistoryStore

```kotlin
// domain/search/SearchHistoryStore.kt
data class SearchQuery(
    val query: String,
    val timestamp: Long,
)

interface SearchHistoryStore {
    val history: Flow<List<SearchQuery>>

    suspend fun addQuery(query: String)
    suspend fun removeQuery(query: String)
    suspend fun clearAll()
}
```

### 3.4 SearchRepository

```kotlin
// domain/search/SearchRepository.kt  ← 接口提升到 Domain 层
interface SearchRepository {
    suspend fun search(query: String): List<SearchResultItem>
}

// data/search/SearchRepositoryImpl.kt
class SearchRepositoryImpl @Inject constructor(
    private val sources: Set<@JvmSuppressWildcards SearchSource>,  // Hilt @IntoSet multibinding
) : SearchRepository {
    override suspend fun search(query: String): List<SearchResultItem> =
        withContext(Dispatchers.IO) {
            sources.flatMap { it.search(query) }.take(200)
        }
}

// di/SearchModule.kt 中绑定
@Binds @IntoSet
fun bindCalculatorHistorySearchSource(impl: CalculatorHistorySearchSource): SearchSource
```

> **设计决策（P0-D3 修订）**：改用 Hilt `@IntoSet` multibinding 替代 `Provider` + `by lazy`。移除 `@ApplicationContext Context` 依赖，`SearchRepositoryImpl` 现在可纯 JVM 单元测试。新增 SearchSource 只需添加 `@Binds @IntoSet` 绑定，无需修改 Impl 代码。`SearchRepository` 接口提升到 `domain/search/` 符合依赖倒置。

### 3.5 SearchUiState（8 态状态机）

```kotlin
// ui/search/SearchUiState.kt
sealed interface SearchUiState {
    /** Docked：折叠在 CalculatorScreen 顶部 */
    data object Docked : SearchUiState

    /** History：搜索框为空 + 有历史记录 */
    data class History(
        val query: String,
        val history: List<SearchQuery>,
    ) : SearchUiState

    /** Typing：正在输入（保持上次历史/结果的 snapshot） */
    data class Typing(
        val query: String,
        val previousState: SearchUiState,
    ) : SearchUiState

    /** Loading：骨架屏 */
    data class Loading(
        val query: String,
    ) : SearchUiState

    /** Results：搜索完成有结果 */
    data class Results(
        val query: String,
        val items: List<SearchResultItem>,
        val totalCount: Int,
    ) : SearchUiState

    /** Empty：搜索完成无结果 */
    data class Empty(
        val query: String,
    ) : SearchUiState

    /** Error：搜索异常 */
    data class Error(
        val query: String,
        val message: String,
        val previousItems: List<SearchResultItem> = emptyList(), // 保留上次结果
    ) : SearchUiState
}
```

### 3.6 SearchEvent

```kotlin
// ui/search/SearchViewModel.kt
sealed interface SearchEvent {
    data class OnQueryChange(val query: String) : SearchEvent
    data object OnImeSearch : SearchEvent
    data class OnHistoryClick(val query: SearchQuery) : SearchEvent
    data class OnHistoryDelete(val query: String) : SearchEvent
    data object OnClearAllHistory : SearchEvent
    data class OnResultClick(val itemId: String) : SearchEvent
    data class OnResultLongPress(val item: SearchResultItem) : SearchEvent
    data object OnRetry : SearchEvent
    data object OnDismiss : SearchEvent
    data object OnClearInput : SearchEvent
    data class OnExpand(val isExpanded: Boolean) : SearchEvent
    data class CopyToClipboard(val text: String, val label: String) : SearchEvent
}
```

---

## 4. 安全考虑

### 4.1 搜索历史加密

| 策略 | 实现 |
|------|------|
| 存储引擎 | `EncryptedSharedPreferences`（AES-256-GCM） |
| Master Key | `MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)` |
| 文件路径 | `"search_history_store"` → `"search_queries"` key |
| 序列化 | JSON（Kotlinx Serialization） |
| 退出登录清除 | `EncryptedSharedPreferences.edit().clear().apply()` 在 `LoginViewModel.onLogout()` 中触发 |
| 降级策略 | `catch KeyStoreException → 空列表降级 + Timber.w` |

### 4.2 输入安全

- **SQL 注入防护**：纯本地 SearchSource 使用 `String.contains()`，不接触 SQL。无注入风险。
- **XSS 防护**：UI 层使用 Compose `AnnotatedString`，不渲染 HTML。
- **特殊字符**：正则 `.contains()` 天然转义无特殊语义字符（`%`, `_` 非 SQL）。
- **关键词上限**：100 字符硬限制，防止超长字符串内存占用。

### 4.3 权限

- **无新增权限**：纯本地搜索，不需要 INTERNET / READ_EXTERNAL_STORAGE 等权限。
- **Keystore 权限**：Android Keystore 系统级权限，应用无需额外声明。

---

## 5. 测试策略

> **基础设施**：ViewModel 单测使用 `kotlinx-coroutines-test` 的 `runTest` + `StandardTestDispatcher`（控制虚拟时间）。Compose UI 测试使用 `createComposeRule()`。

### 5.1 单元测试 — ViewModel（16 case，含回归路径）

| 状态转移 | case | 验证点 |
|---------|:--:|------|
| Docked → History | 1 | 点击 SearchBar → `_uiState == History(...)` |
| History → Typing | 2 | 输入字符 → `_uiState is Typing`；纯空格 `"   "` onQueryChange → 不触发搜索 |
| Typing → Loading | 3 | debounce 300ms 后 → `_uiState is Loading`；超长输入 101 字符 → Toast + 不触发 |
| Loading → Results | 4 | `searchRepository.search()` 返回非空 → `_uiState is Results` |
| Loading → Empty | 5 | `searchRepository.search()` 返回空列表 → `_uiState is Empty` |
| **Loading → Typing（回归）** | 6 | Loading 中 onQueryChange → `searchJob?.cancel()` → `_uiState is Typing` |
| Loading → Error | 7 | withTimeout(5s) → `_uiState is Error("搜索超时，请重试")` |
| **Error → Typing（回归）** | 8 | Error 态 onQueryChange → `_uiState is Typing` + searchJob cancel |
| Error → Loading（重试） | 9 | OnRetry → `_uiState is Loading` |
| Results → Docked | 10 | 返回键 → `_uiState is Docked` |
| Empty → Typing | 11 | Empty 态修改输入 → `_uiState is Typing` |
| History → 历史项点击 → Loading | 12 | OnHistoryClick → 填入 query → 立即搜索 |
| **SavedStateHandle 恢复后重搜** | 13 | SavedStateHandle("query"="123") + prev Loading → 自动 retry |
| 竞态取消 | 14 | 连续 3 次 onQueryChange → 仅最后一次 searchJob 存活 |
| trim+lowercase 去重 | 15 | "ABC" → "  abc  " → 不产生重复 |
| FIFO 淘汰 | 16 | 6 条历史 → 最旧一条被移除 |

### 5.2 单元测试 — Domain/Data

| 被测对象 | 最少 case | 示例 |
|---------|:--:|------|
| CalculatorHistorySearchSource | 5 | 双字段匹配 / expression 忽略空格 / 大小写不敏感 / 空关键词 / highlightRanges coerceIn 边界 |
| SearchRepositoryImpl | 2 | FakeSearchSource 单源搜索 / 双源聚合（注入 Set<SearchSource>） |
| EncryptedSearchHistoryStore | 4 | 写入→读取往返 / 去重 / FIFO 5→6 淘汰 / KeystoreException 降级（**需 Instrumentation**） |
| InMemorySearchHistoryStore | 4 | 同上 4 case（纯 JVM，用于 ViewModel 测试的 test double） |

### 5.3 Compose UI 测试（22 case）

| 测试 | 最少 case | 说明 |
|------|:--:|------|
| SearchScreen 状态展示 | 5 | History / History(空) / Results / Empty / Error |
| SearchBar 交互 | 5 | 点击展开 / 输入 / 清除 / IME Search / **Loading 态禁用交互** |
| BackHandler 互斥 | 3 | SearchScreen 展开时优先 / 收起后 CalculatorScreen 恢复 / **快速连按返回键竞态** |
| contentDescription 验证 | 4 | Docked / 结果项 / 空状态图标 / **DropdownMenu 无障碍** |
| Accessibility 焦点顺序 | 1 | TalkBack 遍历 Tab 1→5 |
| **Shimmer 200ms 延迟** | 1 | `mainClock.advanceTimeBy(150)` → 不展示 Shimmer；`advanceTimeBy(250)` → 必须展示 |
| **Snackbar 排队策略** | 2 | Error + 复制 → Error 先展示；删除 + 复制 → 复制后排队 |
| **isBlank 守卫** | 1 | `"   "` 输入 → 不触发搜索 |

### 5.4 UI 验收检查项（不阻塞 CI）

| 检查项 | 方式 |
|--------|------|
| WCAG 对比度 | APCA 或 Contrast Checker |
| 触控 48dp | Layout Inspector |
| `.disabled` 类生效 | `assertIsNotEnabled()` |
| AnimatedVisibility 250ms | `composeTestRule.waitForIdle()` |

### 5.5 代码覆盖率阈值

| 层 | 行覆盖率 | 说明 |
|------|:--:|------|
| Domain | ≥90% | SearchSource/SearchHistoryStore 路径少，极易覆盖 |
| ViewModel | ≥85% | 16 个状态转移 case 覆盖主路径 |
| UI (Compose) | ≥70% | 22 个 UI case 覆盖主要状态展示 |
| Data | ≥80% | Repository 纯委托，EncryptedSearchHistoryStore 在 Instrumentation 侧 |

### 5.6 CI Pipeline（阻塞）

```
每个 PR / push 触发:
  ┌──────────────────────────────────────────────────────┐
  │ 1. ./gradlew lintDebug              ← 阻塞（abortOnError）│
  │ 2. ./gradlew testDebugUnitTest      ← 阻塞（覆盖率≥80%）│
  │ 3. ./gradlew connectedCheck         ← 阻塞（UI + 无障碍）│
  │ 4. lint 无障碍规则                    ← 阻塞（contentDescription 缺失拦截）│
  │ 5. dependencyUpdates                ← 警告（BOM 版本过期提示）│
  └──────────────────────────────────────────────────────┘
```

> **P0-D7, P0-D8 修订**：补充了 ViewModel 回归路径测试（Loading→Typing, Error→Typing）、CI Pipeline 定义（test + connectedCheck）、代码覆盖率阈值。

### 5.7 可观测性设计

| 位置 | 级别 | 内容 |
|------|:--:|------|
| `SearchViewModel.onQueryChange()` | `Timber.d` | `"Search started: query=%s"` |
| `SearchViewModel` 结果返回 | `Timber.i` | `"Search done: %d results, %dms"` |
| `SearchViewModel` catch 非取消异常 | `Timber.e` | `"Search failed: query=%s"` + stacktrace |
| `SearchViewModel` searchJob?.cancel() | `Timber.d` | `"Search cancelled: newQuery=%s"` |
| `EncryptedSearchHistoryStore` KeystoreException | `Timber.w` | `"Keystore unavailable, history degraded"` |
| `EncryptedSearchHistoryStore` IOException | `Timber.e` | `"History read/write failed"` |
| Debug build | `Timber.d` | `"UiState: %s → %s"` 状态机转移追踪 |
| 性能埋点 | `measureTimeMillis` | 每次搜索记录耗时（PRD §5 <100ms 基线） |

> **预留扩展**：定义 `interface SearchAnalytics { fun onSearch(query, resultCount, latencyMs); fun onHistoryDelete() }`，当前注入空实现 `NoOpSearchAnalytics`，后续替换 Firebase/Analytics。

---

## 6. 文件变更清单

### 6.1 新增文件（19 个）

```
app/src/main/java/com/example/myandroidapp/
├── ui/search/
│   ├── SearchScreen.kt                    # SearchScreen 主 Composable（8态状态机）
│   ├── SearchViewModel.kt                 # ViewModel（debounce + 竞态 + SharedFlow）
│   ├── SearchUiState.kt                   # sealed interface（8态）
│   ├── HighlightedText.kt                 # AnnotatedString 关键词高亮组件
│   ├── SearchHistorySection.kt           # 搜索历史区域 Composable
│   ├── SearchResultCard.kt               # 搜索结果项 Composable（原 SearchResultItem 改名）
│   ├── EmptySearchView.kt                # 空结果 Composable
│   ├── EmptyHistoryGuide.kt              # 空历史引导 Composable
│   └── ShimmerSearchSkeleton.kt          # 加载骨架 Composable
├── ui/calculator/
│   └── CalculatorOutputEvent.kt           # CalculatorVM→UI 单向导航/动画事件
├── domain/search/
│   ├── SearchSource.kt                   # 搜索源接口
│   ├── CalculatorHistorySearchSource.kt  # 计算历史搜索实现
│   ├── SearchResultItem.kt               # data class（domain）
│   ├── SearchHistoryStore.kt             # 历史持久化接口 + SearchQuery
│   └── SearchRepository.kt               # 搜索数据层接口（提升自 data/）
├── data/search/
│   ├── SearchRepositoryImpl.kt           # 实现（Hilt @IntoSet 聚合）
│   ├── EncryptedSearchHistoryStore.kt    # AES 加密持有
│   └── InMemorySearchHistoryStore.kt     # 测试实现
└── di/
    └── SearchModule.kt                   # Hilt @Module（@Binds @IntoSet 绑定）
```

### 6.2 修改文件（7 个）

```
app/src/main/java/com/example/myandroidapp/
├── ui/calculator/
│   ├── CalculatorScreen.kt               # +DockedSearchBar + isSearchExpanded + BackHandler
│   │                                       + Box 包裹（AnimatedVisibility overlay 层级）
│   │                                       + historyListState hoist + LaunchedEffect(outputEvents)
│   ├── CalculatorHistorySheet.kt         # +historyListState 参数 + 高亮动画
│   ├── CalculatorViewModel.kt            # +outputEventChannel: MutableSharedFlow<CalculatorOutputEvent>
│   │                                       + onOutputEvent() 方法
│   └── CalculatorUiState.kt              # 无变更（ExpandHistoryAndScroll 已独立）
├── ui/login/
│   └── LoginViewModel.kt                 # +onLogout() → SearchHistoryStore.clearAll()
├── navigation/NavGraph.kt                # 无路由变更（SearchScreen 非独立路由）
└── di/CalculatorModule.kt                # 引用 SearchModule

---

## 7. 构建配置

### 7.1 依赖（build.gradle.kts）

```kotlin
// 已存在 — 无需新增依赖
// compose-bom:2023.10.01 → 包含 M3 SearchBar、placeholder modifier
// hilt-android、room-ktx、kotlinx-coroutines — 已有

// 需确认版本（CR check）
// M3 BOM ≥ 2023.01.00（SearchBar + placeholder 支持）
// EncryptedSharedPreferences → androidx.security:security-crypto:1.1.0-alpha06+
```

### 7.2 ProGuard（无新增规则）

搜索功能无反射、无动态代理、无第三方 SDK。

---

## 8. 风险与缓解

| # | 风险 | 概率 | 影响 | 缓解 |
|---|------|:--:|:--:|------|
| R1 | BackHandler clearFocus + collapse 竞态 | 中 | 返回键双按 UI 错乱 | 100ms debounce + `isSearchExpanded` 状态锁（UR3-5） |
| R2 | EncryptedSharedPreferences 不兼容旧设备 | 低 | 搜索历史不可用 | KeystoreException 降级空列表（PRD S2） |
| R3 | DataStore 读取大历史全量扫描耗时 | 低 | 搜索延迟 >100ms | 当前上限 20 条，<100ms 无忧；后续 >200 条考虑索引 |
| R4 | Compose BOM 版本不足 M3 SearchBar | 低 | 编译失败 | CI `./gradlew lint` 前置检查；版本 ≥2023.01.00 |
| R5 | CalculatorScreen 加入 SearchBar 后重组范围扩大 | 中 | 键盘输入卡顿 | `remember(key)` 隔离搜索状态；`derivedStateOf` 降低重组 |

---

## 9. 组件工时估算

| 类别 | 组件 | 工时 |
|------|------|:---:|
| 新建 UI | SearchScreen + SearchViewModel + SearchUiState + 7 Composable | 8.5h |
| 新建 Domain | 5 文件（含 SearchRepository 接口提升） | 1.5h |
| 新建 Data | 3 文件 | 3.0h |
| 新建 DI | SearchModule（@Binds @IntoSet） | 0.5h |
| 修改 UI | CalculatorScreen/Sheet/ViewModel/LoginVM + CalculatorOutputEvent + NavGraph + DI | 5.0h |
| 测试 | 单元 + Compose UI + 无障碍 | 6.5h |
| 缓冲 | BOM 验证 / 集成联调 / CI | 3.0h |
| **总计** | | **28.0h (≈3.5d)** |

> 对外承诺 4d，内部冲刺 3.5d（对齐 UI_DESIGN R3 UR3-3）。

---

## 10. 多视角评审记录（第一轮 · 2026-06-01）

### 10.1 评审概况

| 轮次 | 日期 | 方式 | 评审结论 |
|------|------|------|----------|
| 第一轮 | 2026-06-01 | 3-Agent 并行（资深工程师 / 安全稳定性 / 可测试性） | 识别 8 P0 + 13 P1 + 13 P2 |

| 视角 | 结论 | 评分 |
|------|------|:--:|
| 资深工程师（C1） | 有条件通过 | 3.7/5 |
| 安全稳定性（C2） | 需大改 | ⚠️ |
| 可测试性（C3） | 需大改 | 5.3/10 |

### 10.2 P0 致命项（8 项，去重后，已修订 ✅）

| # | 问题 | 来源 | 修订措施 |
|---|------|:--:|----------|
| P0-D1 | `ExpandHistoryAndScroll` 加入 `CalculatorEvent` → `else` 分支路由到状态机 | C1 | 独立为 `CalculatorOutputEvent` sealed interface（§3.0） |
| P0-D2 | `SharedFlow<CalculatorEvent>` 用户输入与 UI 跳转语义冲突 | C1 | `CalculatorOutputEvent` + `MutableSharedFlow`（replay=0, DROP_OLDEST）（§3.0） |
| P0-D3 | `SearchRepositoryImpl` 硬编码 `by lazy`+`Provider`，不可测+NPE | C1,C3 | 改为 Hilt `@IntoSet` multibinding（§3.4） |
| P0-D4 | `LazyListState` 未 hoist，外部无法调用 `animateScrollToItem` | C1 | `historyListState` 在 CalculatorScreen 层创建并传入 Sheet（§2.2） |
| P0-D5 | `Job.getOrElse` 伪代码 + `CancellationException` 误捕 | C2 | 仅捕获 `TimeoutCancellationException`，cancel 由协程框架传播（§2.1） |
| P0-D6 | `searchJob` 非原子赋值 + 超时/取消信号无法区分 | C2 | `viewModelScope.launch(IO)` + try-catch Timeout 固定模式（§2.1） |
| P0-D7 | 缺少 Loading→Typing / Error→Typing 回归路径测试 | C3 | 补充 16 个 ViewModel case（§5.1）+ 22 个 UI case（§5.3） |
| P0-D8 | CI 缺少 test 步骤 + 无 Timber 日志 | C3 | 补充 CI Pipeline（§5.6）+ 可观测性设计（§5.7） |

### 10.3 P1 重要项（13 项 — 编码阶段消化）

| # | 问题 | 来源 |
|---|------|:--:|
| P1-D1 | `SearchResultItem` 与 Domain data class 同名 → 改为 `SearchResultCard` | C1 |
| P1-D2 | `CalculatorHistoryStore` 位于 data/ 被 domain/ 引用 → 接口提升 | C1 |
| P1-D3 | `CalculatorHistorySearchSource` 无内存缓存 | C1 |
| P1-D4 | `SearchSource.search()` 错误处理契约未定义 | C1 |
| P1-D5 | CalculatorScreen Scaffold Column → Box 层级 | C1 |
| P1-D6 | `EncryptedSearchHistoryStore` 缺少 Mutex 并发保护 | C2 |
| P1-D7 | 退出登录 `clear()` 与进行中搜索 `addQuery()` 竞态 | C2 |
| P1-D8 | Android Auto Backup 包含加密文件跨设备不一致 | C2 |
| P1-D9 | 加密降级静默空列表与「首次使用」混淆 | C2 |
| P1-D10 | `SavedStateHandle` 恢复 CalcHistory 不可序列化 | C2 |
| P1-D11 | `EncryptedSearchHistoryStore` 未指定 `Dispatchers.IO` | C2 |
| P1-D12 | SharedFlow 无 `WhileSubscribed` → Screen 离开后协程泄漏 | C2 |
| P1-D13 | `kotlinx-coroutines-test` 基础设施未在 DESIGN 中说明 | C3 |

### 10.4 P2 优化项（13 项 — 不阻塞）

详见各 Agent 完整报告（已写入 C:\Users\PC\security-review-global-search.md，其余在 delegate_task 输出中）。

### 10.5 质量门禁执行摘要

- **P0 问题总数**：8 项（跨视角去重后）
- **修订轮次**：第 1 轮（本次）
- **修订范围**：§1.3(AD3-AD9), §2.1(搜索流), §2.2(定位流), §3.0(CalculatorOutputEvent), §3.4(SearchRepository), §5(测试策略), §5.6(CI Pipeline), §5.7(可观测性), §6(文件清单)
- **修订后 P0 状态**：8/8 已修订 ✅
- **P1 待执行项**：13 项转入编码阶段跟踪（DECISIONS.md）
- **AI 评审结论：有条件通过** — P0 已自动修订，P1 留编码阶段。架构就绪，可进入编码

---

## 变更记录

| 版本 | 日期 | 变更说明 |
|------|------|---------|
| v0.1-draft | 2026-06-01 | AI 初稿生成（基于 PRD v1.0-confirmed + UI_DESIGN v1.0-confirmed） |
| v0.1-draft | 2026-06-01 R1 | **第一轮修订**：三视角评审 8 P0 自动修订 — CalculatorOutputEvent 独立 / @IntoSet DI / LazyListState hoist / withTimeout 修复 / CI Pipeline / 测试回归路径 / Timber 日志 |
| v1.0-confirmed | 2026-06-01 | **人工批准冻结** — 三视角评审 8 P0 全部修订，13 P1 转入编码阶段 |
