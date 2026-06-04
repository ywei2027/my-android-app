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

---

## 搜索模块

### 2026-05-31 — 搜索结果默认排序
**决策**：采用相关度优先排序（标题命中权重 > 正文命中权重）
**原因**：用户搜索的首要目标是快速定位相关文章，相关度排序比时间排序更贴合搜索意图
**否决**：纯时间倒序（原因：无法满足"找内容"而非"找最新"的核心场景）
**影响范围**：SearchViewModel UiState 排序逻辑、后端 API 排序参数
**关联**：PRD.md#讨论决议#1

### 2026-05-31 — 搜索历史本地存储方案
**决策**：使用 DataStore (Preferences) 存储搜索历史 JSON 列表（最多5条，去重）
**原因**：数据量极小(5条)，DataStore 比 Room 轻量，比 SharedPreferences 类型安全，支持 Flow 响应式读取
**否决**：Room（过度设计）；SharedPreferences（类型不安全）
**影响范围**：SearchRepository 本地数据层
**关联**：PRD.md#讨论决议#2

### 2026-05-31 — 搜索防抖参数
**决策**：300ms debounce，空关键词零延迟（立即回到 Idle 态）
**原因**：300ms 是移动端搜索交互的行业标准值，兼顾响应速度和请求数量
**否决**：实时搜索/无防抖（原因：每次击键触发请求浪费流量和算力）
**影响范围**：SearchViewModel 输入处理
**关联**：PRD.md#技术视角评审#3.2

### 2026-05-31 — 搜索自动补全延后
**决策**：v1.0 不做搜索建议/自动补全，v1.1 根据用户行为数据重新评估
**原因**：先验证核心搜索流程（关键词→结果）的使用率和转化率，避免在缺乏数据的情况下过早优化
**否决**：首版包含自动补全（原因：增加开发周期，且无数据支撑优先级判断）
**影响范围**：搜索功能范围边界
**关联**：PRD.md#讨论决议#3

---

## 小型新闻App — 技术方案评审阶段（2026-06-04）

### 评审概况
| 轮次 | 日期 | 方式 | 评审结论 |
|------|------|------|----------|
| R1 | 2026-06-04 | 3-Agent 并行 (B1工程/B2安全/B3测试) | 11 P0 已修订 ✅，14 P1 编码阶段消化 |

### 11 项 P0 致命决议（已修订 ✅）
| # | 决议 | 来源 | 修订状态 |
|----|------|------|----------|
| D-40 | Room 不使用 FTS4，坦诚使用 LIKE 查询（100条规模够用） | B1 | ✅ 已修订 |
| D-41 | NewsListViewModel 通过 `hiltViewModel(activity)` 获取 Activity scope | B1 | ✅ 已修订 |
| D-42 | NewsRepository 捕获 HttpException + Exception 全覆盖，不单靠 IOException | B2 | ✅ 已修订 |
| D-43 | loadMore 添加 MAX_CACHED_ARTICLES=200 客户端分页上限防 OOM | B2 | ✅ 已修订 |
| D-44 | Tab 切换前 loadJob?.cancel() 取消前一个加载协程 | B2 | ✅ 已修订 |
| D-45 | SearchRepository 添加 try/catch 兜底，DB异常返回空列表不崩溃 | B2 | ✅ 已修订 |
| D-46 | 所有 UI 组件添加 testTag/semantics 标记 | B3 | ✅ 已修订 |
| D-47 | 新增 NavigationTest 覆盖 NavHost 路由 | B3 | ✅ 已修订 |
| D-48 | 添加 Timber 日志框架 + §9 可观测性规范 | B3 | ✅ 已修订 |
| D-49 | AnimatedVisibility 动画测试用 mainClock.autoAdvance=false | B3 | ✅ 已修订 |
| D-50 | API Key 通过 OkHttp Interceptor 注入，不出现在接口签名 | B2 | ✅ 已修订 |

### 14 项 P1 重要决议（编码阶段消化）
| # | 决议 | 来源 |
|----|------|------|
| D-51 | 4 StateFlow v1 保留，v1.1 统一为单一 UiState | B1 |
| D-52 | 不升级 BOM，使用 @OptIn pullRefresh（kotlinCompilerExtension 兼容性） | B1 |
| D-53 | LazyColumn 编码时添加 `items(articles, key = { it.url })` | B1 |
| D-54 | insertAll 改为异步 fire-and-forget（CoroutineScope.launch） | B1 |
| D-55 | 搜索统一使用 collectLatest 取消前一个，移除冗余 searchJob | B1 |
| D-56 | 所有 Repository 方法添加 withTimeout 超时保护 | B2 |
| D-57 | loadMore 失败通过 SharedFlow 发送 Snackbar 事件 | B2 |
| D-58 | onCleared() 显式声明清理 loadJob/searchJob | B2 |
| D-59 | savedStateHandle 持久化 selectedTab + currentPage | B2 |
| D-60 | 编码时补充 6+ 边界条件测试 | B3 |
| D-61 | 新 Composable 各写独立 preview test | B3 |
| D-62 | 新增 room-testing:2.6.1 依赖 | B3 |
| D-63 | 搜索 query 限 100 字符上限 | B2 |
| D-64 | OkHttp 补全 writeTimeout + callTimeout | B2 |
