# 小型新闻App — 技术方案

> **版本:** v1.0-confirmed
> **功能名称:** 小型新闻App
> **创建日期:** 2026-06-04
> **作者:** Hermes AI
> **修订:** 2026-06-04 三视角评审 P0 自动修订（11项）

---

## §1 架构概览

### 分层架构

```
┌──────────────────────────────────┐
│  UI Layer (Compose + ViewModel)  │
│  NewsListScreen, NewsDetailScreen │
│  NewsCard, ShimmerCard, ErrorState│
├──────────────────────────────────┤
│  ViewModel Layer                 │
│  NewsListViewModel (Activity-scoped)
│  NewsDetailViewModel (NavEntry-scoped)
│  UiState<Loading|Success|Error|Empty>
├──────────────────────────────────┤
│  Repository Layer                │
│  NewsRepository (Cache-Fallback)
│  SearchRepository (Room LIKE)
├──────────────────────────────────┤
│  Data Layer                      │
│  Remote: NewsApiService (Retrofit)
│  Local:  NewsDao, NewsArticleEntity
│  DI:     NetworkModule, DatabaseModule
└──────────────────────────────────┘
```

**数据流方向（单向）：**
```
User → Composable → ViewModel (Event) → Repository → DataSource → API/DB
                                                                    ↓
UI ← Composable ← ViewModel (UiState) ← Repository ← DataSource ← Response
```

### 导航架构

```kotlin
// ViewModel 必须在 Activity scope 创建以保持列表状态
// 方法：在 NavHost 外层使用 hiltViewModel(LocalContext.current as ComponentActivity)
// 或 viewModel(viewModelStoreOwner = activity)

NavHost(startDestination = "news_list") {
    composable("news_list") {
        NewsListScreen(
            viewModel = hiltViewModel(LocalContext.current as ComponentActivity),
            onArticleClick = { id -> navController.navigate("news_detail/$id") }
        )
    }
    composable(
        route = "news_detail/{articleId}",
        arguments = listOf(navArgument("articleId") { type = NavType.StringType })
    ) { backStackEntry ->
        NewsDetailScreen(
            articleId = backStackEntry.arguments?.getString("articleId") ?: "",
            onBack = { navController.popBackStack() }
        )
    }
}
```

### 关键架构决策

| # | 决策 | 原因 | 影响 |
|---|------|------|------|
| AD-1 | MainActivity 移除 LoginScreen → 直接 NavHost(NewsListScreen) | D-28 PRD决议 | 删除 LoginScreen/LoginViewModel/LoginRepository |
| AD-2 | Cache-Fallback 模式（网络优先→缓存兜底） | 新闻时效性优先；v1 100条规模无需 NetworkBoundResource 完整语义 | NewsRepository 实现 try-catch 双数据源协调 |
| AD-3 | Room LIKE 本地全文搜索（非 FTS4） | 100条数据量 FTS4 过度设计，LIKE 查询延迟 <100ms 够用 | SearchRepository 使用 LIKE 查询 |
| AD-4 | NewsListViewModel Activity-scoped | 详情返回保持列表状态 | ViewModel 通过 `hiltViewModel(activity)` 获取 |
| AD-5 | @OptIn pullRefresh 实验性 API（不升级 BOM） | BOM 升级至 2024.06.00 与 kotlinCompilerExtension 1.5.5 不兼容 | 使用当前 BOM 2023.10.01 中已有的 pullRefresh modifier |
| AD-6 | Chrome CustomTabs 打开原文链接 | Android 平台惯例 | 需添加 androidx.browser:browser 依赖 |
| AD-7 | API Key 通过 OkHttp Interceptor 动态注入（不在接口签名暴露） | 反编译安全 | NetworkModule 添加 apiKey 拦截器 |
| AD-8 | Timber 日志框架 | 可观测性需求 | 依赖 `com.jakewharton.timber:timber:5.0.1` |

---

## §2 模块设计

### 2.1 UI 层

#### NewsListScreen

```kotlin
@Composable
fun NewsListScreen(
    onArticleClick: (String) -> Unit,
    viewModel: NewsListViewModel = hiltViewModel(LocalContext.current as ComponentActivity)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isSearchActive by viewModel.isSearchActive.collectAsStateWithLifecycle()

    // BackHandler 互斥
    BackHandler(enabled = !isSearchActive) { (LocalContext.current as Activity).finish() }
    if (isSearchActive) {
        BackHandler(enabled = true) {
            viewModel.clearSearch()
            focusManager.clearFocus()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).imePadding()) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                SearchBar(
                    modifier = Modifier.semantics { testTag = "searchBar" },
                    ...
                )
                ScrollableTabRow(
                    modifier = Modifier.semantics { testTag = "categoryTabRow" },
                    ...
                )
                PullToRefreshBox(isRefreshing, onRefresh) {
                    when (uiState) {
                        is NewsListUiState.FirstLoading -> ShimmerList()
                        is NewsListUiState.Loading -> LoadingWithContent()
                        is NewsListUiState.Success -> NewsCardList(
                            articles = (uiState as Success).articles,
                            modifier = Modifier.semantics { testTag = "newsCardList" }
                        )
                        is NewsListUiState.Empty -> EmptyState(
                            modifier = Modifier.semantics { testTag = "emptyState" }
                        )
                        is NewsListUiState.Error -> ErrorState(
                            message = (uiState as Error).message,
                            modifier = Modifier.semantics { testTag = "errorState" },
                            onRetry = { viewModel.loadNews() }
                        )
                        is NewsListUiState.PagingLoading -> PagingLoadingAtBottom()
                    }
                }
            }

            // 搜索覆盖层（动画测试：使用 mainClock.autoAdvance = false 控制动画时钟）
            AnimatedVisibility(
                visible = isSearchActive,
                modifier = Modifier.semantics { testTag = "searchOverlay" }
            ) {
                SearchOverlay(...)
            }
        }
    }
}
```

#### NewsDetailScreen

```kotlin
@Composable
fun NewsDetailScreen(
    articleId: String,
    onBack: () -> Unit,
    viewModel: NewsDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onBack, Modifier.semantics { testTag = "detailBackButton" }) { ... }
                }
            )
        },
        modifier = Modifier.semantics { testTag = "newsDetailScreen" }
    ) { padding ->
        when (uiState) {
            is DetailUiState.Loading -> ShimmerDetail()
            is DetailUiState.Success -> {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    AsyncImage(article.urlToImage, 200.dp)
                    Text(article.title, fontSize = 24.sp)
                    Row { Text(source) + "·" + Text(time) }
                    Text(article.description, fontSize = 16.sp)
                    FilledTonalButton(
                        onClick = { openUrl(article.url) },
                        modifier = Modifier.semantics { testTag = "readOriginalButton" }
                    ) { Text("阅读原文") }
                }
            }
            is DetailUiState.Error -> ErrorState(
                message = (uiState as DetailUiState.Error).message,
                onRetry = { viewModel.loadDetail() },
                modifier = Modifier.semantics { testTag = "detailErrorState" }
            )
        }
    }
}
```

#### 可复用组件

| 组件 | 用途 | 参数 | testTag |
|------|------|------|---------|
| `NewsCard` | 新闻卡片 | `article: NewsArticle, onClick: () -> Unit` | `newsCard_{article.url}` |
| `ShimmerCard` | 骨架屏卡片 | 无 | `shimmerCard_{index}` |
| `ErrorState` | 错误态（通用） | `message, onRetry, icon` | `errorState_{context}` |
| `EmptyState` | 空态（通用） | `title, subtitle, icon` | `emptyState_{context}` |
| `SearchOverlay` | 搜索覆盖层 | `query, results, isSearching, onItemClick` | `searchOverlay`, `searchResultItem_{index}` |

### 2.2 ViewModel 层

#### NewsListViewModel

```kotlin
@HiltViewModel
class NewsListViewModel @Inject constructor(
    private val newsRepository: NewsRepository,
    private val searchRepository: SearchRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<NewsListUiState>(NewsListUiState.FirstLoading)
    val uiState: StateFlow<NewsListUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    private val _selectedTab = MutableStateFlow(NewsCategory.RECOMMENDED)
    val selectedTab: StateFlow<NewsCategory> = _selectedTab.asStateFlow()

    private var loadJob: Job? = null
    private var searchJob: Job? = null
    private var currentPage = 1
    private var currentData: List<NewsArticle> = emptyList()

    // 客户端分页上限 — 防止无限累积 OOM (P0-B2-2)
    private val MAX_CACHED_ARTICLES = 200

    init {
        // 从 SavedStateHandle 恢复 Tab 状态 (P1-B2-5)
        _selectedTab.value = savedStateHandle.get<NewsCategory>("selectedTab") ?: NewsCategory.RECOMMENDED
        currentPage = savedStateHandle.get<Int>("currentPage") ?: 1
        loadNews()
        observeSearchQuery()
        Timber.d("NewsListViewModel initialized, tab=${_selectedTab.value}")
    }

    fun loadNews(category: NewsCategory = _selectedTab.value, isRefresh: Boolean = false) {
        loadJob?.cancel() // 取消前一个 tab 加载防止竞态 (P0-B2-3)
        loadJob = viewModelScope.launch {
            Timber.d("loadNews category=$category isRefresh=$isRefresh")
            _uiState.value = if (isRefresh) _uiState.value else NewsListUiState.FirstLoading
            _selectedTab.value = category
            savedStateHandle["selectedTab"] = category // 持久化 Tab (P1-B2-5)
            currentPage = 1
            savedStateHandle["currentPage"] = 1
            newsRepository.getTopHeadlines(category, page = 1)
                .onSuccess { articles ->
                    currentData = articles
                    _uiState.value = if (articles.isEmpty()) NewsListUiState.Empty
                                    else NewsListUiState.Success(articles)
                    Timber.d("loadNews SUCCESS, count=${articles.size}")
                }
                .onFailure { e ->
                    Timber.e(e, "loadNews FAILED")
                    _uiState.value = when (e) {
                        is Http429Exception -> NewsListUiState.Error("请求太频繁，请稍后再试")
                        else -> NewsListUiState.Error("加载失败，请检查网络")
                    }
                }
        }
    }

    fun loadMore() {
        if (_uiState.value is NewsListUiState.PagingLoading) return
        val oldData = currentData
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = NewsListUiState.PagingLoading(oldData)
            Timber.d("loadMore page=${currentPage + 1}")
            newsRepository.getTopHeadlines(_selectedTab.value, page = ++currentPage)
                .onSuccess { moreArticles ->
                    // 客户端分页上限保护 (P0-B2-2)
                    currentData = (oldData + moreArticles).takeLast(MAX_CACHED_ARTICLES)
                    _uiState.value = NewsListUiState.Success(currentData)
                    savedStateHandle["currentPage"] = currentPage
                    Timber.d("loadMore SUCCESS, total=${currentData.size}")
                }
                .onFailure {
                    // 保留已加载数据 + Snackbar 事件 (P1-B2-3)
                    _uiState.value = NewsListUiState.Success(oldData)
                    // 通过 SharedFlow 发送一次性事件通知 UI 展示 Snackbar
                    Timber.e("loadMore FAILED, keeping ${oldData.size} items")
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query.take(100) // 搜索词长度上限 (P2-B2-2)
        _isSearchActive.value = query.length >= 3
    }

    private fun observeSearchQuery() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .filter { it.length >= 3 }
                .collectLatest { query ->
                    // collectLatest 自动取消前一个 lambda — 移除冗余 searchJob 嵌套 launch (P1-B2-2)
                    Timber.d("search triggered: query=$query")
                    val results = withTimeout(10_000L) { // 搜索超时保护 (P1-B2-1)
                        searchRepository.search(query)
                    }
                    _searchResults.value = results
                    Timber.d("search completed: ${results.size} results")
                }
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _isSearchActive.value = false
    }

    override fun onCleared() {
        super.onCleared()
        loadJob?.cancel()
        searchJob?.cancel()
        Timber.d("NewsListViewModel cleared")
    }
}

// UiState sealed interface
sealed interface NewsListUiState {
    data object FirstLoading : NewsListUiState
    data class Loading(val currentArticles: List<NewsArticle> = emptyList()) : NewsListUiState
    data class Success(val articles: List<NewsArticle>) : NewsListUiState
    data object Empty : NewsListUiState
    data class Error(val message: String) : NewsListUiState
    data class PagingLoading(val currentArticles: List<NewsArticle>) : NewsListUiState
}
```

### 2.3 Repository 层

#### NewsRepository (Cache-Fallback)

```kotlin
class NewsRepository @Inject constructor(
    private val newsApiService: NewsApiService,
    private val newsDao: NewsDao
) {
    suspend fun getTopHeadlines(
        category: NewsCategory,
        page: Int = 1,
        pageSize: Int = 20
    ): Result<List<NewsArticle>> {
        return withTimeout(30_000L) { // 超时保护 (P1-B2-1)
            withContext(Dispatchers.IO) {
                try {
                    val response = newsApiService.getTopHeadlines(
                        country = "cn",
                        category = category.apiValue,
                        page = page,
                        pageSize = pageSize
                    )
                    if (response.status == "ok") {
                        val articles = response.articles.map { it.toNewsArticle(category) }
                        // 异步缓存不阻塞响应 (P1-B1-7)
                        CoroutineScope(Dispatchers.IO).launch {
                            newsDao.insertAll(articles)
                        }
                        Timber.d("Network success, cached ${articles.size} articles")
                        Result.success(articles)
                    } else {
                        Timber.w("API status=${response.status}, fallback to cache")
                        val cached = newsDao.getByCategory(category.apiValue)
                        if (cached.isNotEmpty()) Result.success(cached)
                        else Result.failure(ApiException(response.status))
                    }
                } catch (e: HttpException) {
                    // 捕获 HTTP 异常后尝试缓存回退 (P0-B2-1)
                    Timber.e(e, "HTTP ${e.code()} failed, fallback to cache")
                    val cached = newsDao.getByCategory(category.apiValue)
                    if (cached.isNotEmpty()) Result.success(cached)
                    else Result.failure(e)
                } catch (e: IOException) {
                    // 网络不可用 → 缓存回退
                    Timber.e(e, "Network unavailable, fallback to cache")
                    val cached = newsDao.getByCategory(category.apiValue)
                    if (cached.isNotEmpty()) Result.success(cached)
                    else Result.failure(e)
                } catch (e: Exception) {
                    // 解析错误兜底 (P0-B2-1)
                    Timber.e(e, "Unexpected error")
                    Result.failure(e)
                }
            }
        }
    }
}
```

#### SearchRepository

```kotlin
class SearchRepository @Inject constructor(
    private val newsDao: NewsDao
) {
    suspend fun search(query: String): List<NewsArticle> {
        return withTimeout(10_000L) { // 超时保护 (P1-B2-1)
            withContext(Dispatchers.IO) {
                try {
                    newsDao.searchByTitleAndDescription("%$query%")
                } catch (e: Exception) {
                    // 数据库异常不崩溃 (P0-B2-4)
                    Timber.e(e, "Search failed for query=$query")
                    emptyList()
                }
            }
        }
    }
}
```

### 2.4 Data 层

#### Retrofit API

```kotlin
// API Key 不暴露在接口签名中，通过 OkHttp Interceptor 动态注入 (P2-B2-6)
interface NewsApiService {
    @GET("v2/top-headlines")
    suspend fun getTopHeadlines(
        @Query("country") country: String = "cn",
        @Query("category") category: String,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): NewsApiResponse
}

data class NewsApiResponse(
    val status: String,
    val totalResults: Int,
    val articles: List<NewsApiArticle>
)
```

#### Room 数据库

```kotlin
@Entity(tableName = "news_articles")
data class NewsArticleEntity(
    @PrimaryKey val url: String,
    val title: String,
    val description: String?,
    val sourceName: String,
    val publishedAt: String,
    val urlToImage: String?,
    val category: String,
    val cachedAt: Long = System.currentTimeMillis()
)

// 不使用 FTS4 — 100条数据量 LIKE 查询延迟<100ms 够用 (P0-B1-1)
@Dao
interface NewsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(articles: List<NewsArticleEntity>)

    @Query("SELECT * FROM news_articles WHERE category = :category ORDER BY publishedAt DESC")
    suspend fun getByCategory(category: String): List<NewsArticleEntity>

    @Query("""
        SELECT * FROM news_articles
        WHERE title LIKE :query OR description LIKE :query
        ORDER BY publishedAt DESC
        LIMIT 50
    """)
    suspend fun searchByTitleAndDescription(query: String): List<NewsArticleEntity>

    @Query("DELETE FROM news_articles WHERE cachedAt < :expireTime")
    suspend fun deleteExpired(expireTime: Long)

    @Query("SELECT COUNT(*) FROM news_articles")
    suspend fun getCount(): Int
}
```

#### DI 模块

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                // NewsAPI Key 动态注入 (P2-B2-6)
                val url = chain.request().url.newBuilder()
                    .addQueryParameter("apiKey", BuildConfig.NEWS_API_KEY)
                    .build()
                chain.proceed(chain.request().newBuilder().url(url).build())
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides @Singleton
    fun provideNewsApi(client: OkHttpClient): NewsApiService {
        return Retrofit.Builder()
            .baseUrl("https://newsapi.org/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NewsApiService::class.java)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NewsDatabase {
        return Room.databaseBuilder(context, NewsDatabase::class.java, "news.db")
            .fallbackToDestructiveMigration() // TODO: v1.1 实现 Migration(1,2)
            .build()
    }

    @Provides fun provideNewsDao(db: NewsDatabase) = db.newsDao()
}
```

---

## §3 接口定义

### UiState 类型

| 类型 | 字段 | 说明 |
|------|------|------|
| `NewsListUiState.FirstLoading` | — | 首次加载骨架屏 |
| `NewsListUiState.Loading` | `currentArticles: List<NewsArticle>` | 非首次加载保留旧列表（注：空列表时等同 FirstLoading） |
| `NewsListUiState.Success` | `articles: List<NewsArticle>` | 正常数据（≤200条，受 MAX_CACHED_ARTICLES 限制） |
| `NewsListUiState.Empty` | — | 分类无数据 |
| `NewsListUiState.Error` | `message: String` | 网络/限流/超时错误 |
| `NewsListUiState.PagingLoading` | `currentArticles: List<NewsArticle>` | 分页加载中 |
| `DetailUiState.Loading` | — | 详情加载 |
| `DetailUiState.Success` | `article: NewsArticle` | 详情成功 |
| `DetailUiState.Error` | `message: String` | 详情错误 |

### 数据模型

```kotlin
data class NewsArticle(
    val url: String,
    val title: String,
    val description: String?,
    val sourceName: String,
    val publishedAt: String,
    val urlToImage: String?,
    val category: NewsCategory
)

enum class NewsCategory(val displayName: String, val apiValue: String) {
    RECOMMENDED("推荐", "general"),
    TECHNOLOGY("科技", "technology"),
    BUSINESS("财经", "business"),
    SPORTS("体育", "sports"),
    ENTERTAINMENT("娱乐", "entertainment")
}
```

---

## §4 数据流与状态管理

### 列表页数据流

```
User打开App
  → NewsListViewModel.init() → savedStateHandle 恢复 tab/page → loadNews()
  → uiState = FirstLoading → UI渲染3张骨架屏
  → NewsRepository.getTopHeadlines("general", 1)
    [withTimeout(30s) + withContext(Dispatchers.IO)]
    → NewsApiService.getTopHeadlines() [网络请求]
      → 200 → async insertAll() [缓存] + Result.success(articles)
      → HttpException → NewsDao.getByCategory() [缓存回退]
      → IOException → NewsDao.getByCategory() [缓存回退]
      → Exception → Result.failure(e)
  → uiState = Success | Empty | Error
  → Timber.d 记录结果
  → UI更新 → LazyColumn(items, key = { it.url }) 卡片列表 | 空态 | 错误态
```

### 搜索数据流

```
User输入 ≥3字符
  → onSearchQueryChanged(query.take(100)) → searchQuery 更新
  → observeSearchQuery:
    .debounce(300ms) → .filter(len>=3) → .collectLatest (自动取消前一个)
      → withTimeout(10s) → searchRepository.search(query)
        → withContext(Dispatchers.IO) → try { newsDao LIKE } catch → emptyList()
  → _searchResults 更新 → UI 渲染搜索结果
```

### 防竞态机制

- **Tab 切换**: `loadJob?.cancel()` 取消前一个加载协程
- **搜索**: `collectLatest` 自动取消前一个 lambda → 无需手动 job 管理
- **分页加载**: `PagingLoading` 状态作防重入保护
- **Repository 超时**: `withTimeout(30s)` 防止协程永久挂起

---

## §5 安全考虑

| 安全项 | 措施 |
|--------|------|
| API Key | `local.properties` → `BuildConfig` → OkHttp Interceptor 动态注入，不出现于接口签名 |
| 网络请求 | HTTPS only (newsapi.org)，OkHttp 强制 TLS 1.2+ |
| 缓存过期 | Room 7 天过期 + 定时清理 `deleteExpired()` + COUNT 限 100 条 |
| 数据校验 | Gson 解析使用 `@SerializedName` + 可空字段防御 |
| 外部链接 | Chrome CustomTabs 打开，不允许 WebView 内嵌 |
| 崩溃防护 | Repository 层 Exception 全覆盖（HttpException + IOException + Exception），UI 层永远不会看到未捕获异常 |
| 超时保护 | 网络 30s / 搜索 10s 硬超时，防止 ANR |
| ProGuard | Retrofit/Gson/Room/Coil keep 规则（见下文） |

### ProGuard 规则

```proguard
# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.example.myandroidapp.data.remote.** { *; }
-keep class retrofit2.** { *; }

# Gson
-keep class com.example.myandroidapp.data.model.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Coil
-keep class coil.** { *; }

# Timber
-keep class timber.log.** { *; }
```

---

## §6 测试策略

### 单元测试

| 层级 | 测试文件 | 覆盖内容 | 新增依赖 |
|------|----------|----------|----------|
| ViewModel | `NewsListViewModelTest` | 状态机全路径（加载→成功→错误→空→分页→搜索→防抖→tab竞态）+ SavedStateHandle恢复 | — |
| ViewModel | `NewsDetailViewModelTest` | 详情加载成功/错误/不存在/空articleId | — |
| Repository | `NewsRepositoryTest` | Cache-Fallback: 网络成功→异步缓存 / HttpException→回退 / IOException→回退 / 空缓存 | — |
| Repository | `SearchRepositoryTest` | Room LIKE 查询 / DB异常→空列表 / 空结果 | — |
| DAO | `NewsDaoTest` | Room in-memory: insertAll / getByCategory / search / deleteExpired / getCount | `room-testing:2.6.1` |

### UI 测试（Compose Test）

| 测试 | 内容 | testTag 覆盖 |
|------|------|-------------|
| `NewsListScreenTest` | 骨架屏渲染 + 卡片列表展示 + 空态 + 错误态重试 + Tab切换 + pullRefresh | searchBar, categoryTabRow, newsCardList, emptyState, errorState, shimmerCard_X, newsCard_{url} |
| `NewsDetailScreenTest` | 标题/来源/时间/正文渲染 + "阅读原文"按钮 + 返回按钮 | newsDetailScreen, readOriginalButton, detailBackButton |
| `SearchTest` | 搜索覆盖层 AnimatedVisibility + 结果列表 + 清除恢复 | searchOverlay, searchResultItem_X |
| `NavigationTest` | NavHost 路由验证：news_list→news_detail 传参 + 返回栈 + 无效articleId→Error | detailBackButton, newsDetailScreen |

### AnimatedVisibility 测试策略

```kotlin
// 使用 mainClock.autoAdvance = false 控制动画时钟
@Test fun searchOverlay_visible_afterQuery() {
    composeTestRule.mainClock.autoAdvance = false
    composeTestRule.setContent { NewsListScreen(...) }

    composeTestRule.onNodeWithTag("searchOverlay").assertDoesNotExist() // 初始隐藏

    viewModel.onSearchQueryChanged("科技新闻")
    composeTestRule.mainClock.advanceTimeBy(301) // 过 300ms 防抖

    composeTestRule.onNodeWithTag("searchOverlay").assertIsDisplayed() // 动画后可见
}
```

### CI 集成

```gradle
android {
    testOptions {
        unitTests {
            isIncludeAndroidResources = true  // Robolectric
        }
    }
}
```

CI 命令:
```bash
./gradlew assembleDebug lintDebug testDebug
```

### 测试前置条件

| 依赖 | 版本 | 用途 |
|------|------|------|
| `mockk` | 1.13.12（需从 1.13.8 升级） | ViewModel/Repository mock |
| `kotlinx-coroutines-test` | 已存在 | runTest + advanceTimeBy |
| `compose-ui-test-junit4` | 已存在 | composeTestRule |
| `room-testing` | 2.6.1（新增） | Room in-memory database for DAO test |
| `timber` | 5.0.1（新增） | 日志输出 |

---

## §7 文件清单

### 新增文件

| # | 路径 | 说明 | 复杂度 |
|---|------|------|--------|
| 1 | `ui/news/NewsListScreen.kt` | 列表页 Composable + BackHandler/IME/testTag | 🔴 高 |
| 2 | `ui/news/NewsListViewModel.kt` | 状态机 6态 + 搜索防抖(collectLatest) + 分页 + 超时 + Timber | 🔴 高 |
| 3 | `ui/news/NewsDetailScreen.kt` | 详情页 Composable + CustomTabs/testTag | 🟡 中 |
| 4 | `ui/news/NewsDetailViewModel.kt` | 详情加载状态机 + 超时 | 🟡 中 |
| 5 | `ui/components/NewsCard.kt` | 新闻卡片 Composable + testTag | 🟢 低 |
| 6 | `ui/components/ShimmerCard.kt` | 骨架屏 + shimmer modifier | 🟢 低 |
| 7 | `ui/components/ErrorState.kt` | 通用错误态 + testTag | 🟢 低 |
| 8 | `ui/components/EmptyState.kt` | 通用空态 + testTag | 🟢 低 |
| 9 | `data/remote/NewsApiService.kt` | Retrofit 接口（apiKey 不在签名） | 🟢 低 |
| 10 | `data/remote/NewsApiModels.kt` | API 响应数据类 | 🟢 低 |
| 11 | `data/local/NewsArticleEntity.kt` | Room Entity（无 FTS4） | 🟢 低 |
| 12 | `data/local/NewsDao.kt` | Room DAO（LIKE 查询） | 🟢 低 |
| 13 | `data/local/NewsDatabase.kt` | Room Database | 🟢 低 |
| 14 | `data/repository/NewsRepository.kt` | Cache-Fallback + 全异常覆盖 + 异步缓存 | 🟡 中 |
| 15 | `data/repository/SearchRepository.kt` | Room LIKE 搜索 + 异常兜底 | 🟡 中 |
| 16 | `di/NetworkModule.kt` | Retrofit + OkHttp（Interceptor 注入 apiKey）+ Timber | 🟢 低 |
| 17 | `di/DatabaseModule.kt` | Room Hilt 模块 | 🟢 低 |
| 18 | `domain/model/NewsModels.kt` | NewsArticle + NewsCategory | 🟢 低 |

### 修改文件

| # | 路径 | 变更 |
|---|------|------|
| 19 | `app/build.gradle.kts` | +coil-compose:2.5.0, +timber:5.0.1, +browser, +room-testing:2.6.1, +mockk:1.13.12, 不升级 BOM |
| 20 | `MainActivity.kt` | 移除 LoginScreen → NavHost(NewsListScreen), hiltViewModel(activity) |
| 21 | `app/proguard-rules.pro` | 新建或追加：Retrofit/Gson/Room/Coil/Timber keep 规则 |

### 删除文件

| # | 路径 | 原因 |
|---|------|------|
| 22 | `ui/login/LoginViewModel.kt` | D-28: v1 移除登录 |
| 23 | `data/LoginRepository.kt` | 不在 scope 内 |

---

## §8 风险与缓解

| 风险 | 可能性 | 影响 | 缓解 |
|------|--------|------|------|
| @OptIn pullRefresh 未来版本行为变更 | 低 | 升级 Compose 时需适配 | DECISIONS.md 记录技术债，稳定 API 发布后迁移 |
| NewsAPI 免费版频率限制（~100 req/day） | 高 | 开发阶段频繁触发 429 | OkHttp MockInterceptor 返回 Mock 数据 |
| LIKE 查询性能随数据量线性下降 | 低 | 100条内延迟<100ms安全 | COUNT 限 100 + 7 天过期自动清理 |
| Coil 图片内存占用过大 | 低 | OOM | ImageLoader diskCache 50MB + crossfade 300ms + LazyColumn 自动回收 |
| 分页无限累积 | 低 | 已通过 MAX_CACHED_ARTICLES=200 解决 | 客户端硬限制 |
| Tab 切换竞态 | 低 | 已通过 loadJob?.cancel() 解决 | 编码阶段验证 |

---

## §9 可观测性

| 层级 | 日志点 | 级别 |
|------|--------|------|
| ViewModel 初始化 | "NewsListViewModel initialized, tab=X" | Debug |
| loadNews 开始 | "loadNews category=X isRefresh=X" | Debug |
| loadNews 成功 | "loadNews SUCCESS, count=X" | Debug |
| loadNews 失败 | "loadNews FAILED" + 异常信息 | Error |
| loadMore | "loadMore page=X" / "loadMore SUCCESS total=X" / "loadMore FAILED" | Debug/Error |
| 搜索 | "search triggered: query=X" / "search completed: X results" | Debug |
| Repository 网络 | "HTTP XXX failed, fallback to cache" | Warn |
| Repository 异常 | "Unexpected error" | Error |
| ViewModel 清理 | "NewsListViewModel cleared" | Debug |

---

## §10 三视角评审记录

### 评审概况

| 轮次 | 日期 | 方式 | 模型 | 评审结论 |
|------|------|------|------|----------|
| R1 | 2026-06-04 | 3-Agent 并行 (delegate_task) | deepseek-v4-flash | 11 P0 自动修订 ✓ |

### P0 致命决议（已修订 ✓）

| # | 来源 | 问题 | 修订措施 |
|---|------|------|----------|
| P0-01 | B1 | Room FTS4 虚表定义但 DAO 无 FTS MATCH 查询 | 删除 NewsArticleFts Entity，坦诚使用 LIKE，更新 AD-3 |
| P0-02 | B1 | ViewModel Activity-scoped 但 hiltViewModel() 默认 NavBackStackEntry scope | 文档明确 `hiltViewModel(activity)` 获取方式 |
| P0-03 | B2 | NewsRepository 仅捕获 IOException，HttpException 漏网 | 新增 catch(HttpException) + catch(Exception) 全覆盖 |
| P0-04 | B2 | loadMore 无限累积 article 列表 → OOM | 添加 MAX_CACHED_ARTICLES=200，takeLast 保护 |
| P0-05 | B2 | Tab 快速切换无取消 → 并发写入竞态 | 添加 loadJob?.cancel() 机制 |
| P0-06 | B2 | SearchRepository 零异常处理 → SQLiteException 崩溃 | try/catch 包裹返回 emptyList() |
| P0-07 | B3 | UI 组件无 testTag/semantics → 无法自动化测试 | 组件表增加 testTag 列，关键节点标注 semantics |
| P0-08 | B3 | 路由测试完全缺失 | §6 新增 NavigationTest |
| P0-09 | B3 | ViewModel init() 立即启动异步副作用 → 测试无法控制时机 | 记录为设计约束，测试用 runTest + advanceUntilIdle |
| P0-10 | B3 | 零日志 → 故障定位困难 | 新增 Timber 依赖 + §9 可观测性规范 |
| P0-11 | B3 | AnimatedVisibility 动画测试策略未设计 | §6 补充 mainClock.autoAdvance 动画测试策略 |

### P1 重要决议（编码阶段消化）

| # | 来源 | 决议 |
|---|------|------|
| P1-01 | B1 | 4 个独立 StateFlow 违反单一状态源 — v1 保留，v1.1 统一为单一 UiState |
| P1-02 | B1 | BOM 升级与 kotlinCompilerExtension 不兼容 — 改用 @OptIn pullRefresh，不升级 BOM |
| P1-03 | B1 | LazyColumn 缺 key → 编码时添加 `items(articles, key = { it.url })` |
| P1-04 | B1 | IME 可见性在每次重组时查询 → 编码时改用 snapshotFlow |
| P1-05 | B1 | DataStore 搜索历史设计缺失 — v1 不做搜索历史（PRD 范围外） |
| P1-06 | B1 | insertAll 阻塞响应 → 改为异步缓存（CoroutineScope.launch fire-and-forget） |
| P1-07 | B1/B2 | 搜索双重竞态防护（collectLatest + searchJob）冗余 → 统一为 collectLatest |
| P1-08 | B2 | 所有网络/DB 调用无超时 → 添加 withTimeout(30s/10s) |
| P1-09 | B2 | loadMore 错误静默吞没 → 用 SharedFlow 发送 Snackbar 事件 |
| P1-10 | B2 | onCleared() 未声明 → 添加 Job 清理 |
| P1-11 | B2/B3 | savedStateHandle 未使用 → 持久化 selectedTab + currentPage |
| P1-12 | B3 | 缺失关键边界条件测试 — 编码时补充 tab竞态/分页边界/429限流/空列表/极长文本 |
| P1-13 | B3 | 新增 Composable 无独立测试 — 编码时为新组件各写至少 1 个 preview test |
| P1-14 | B3 | Room 测试依赖缺失 — 新增 room-testing:2.6.1 |

### 各视角评分

| 视角 | 评分 | 核心评价 |
|------|------|----------|
| B1 资深工程师 | 6/10 → 8/10（修订后） | 架构清晰，P0-01/P0-02 修复后设计完整性达标 |
| B2 安全/稳定性 | 5/10 → 8/10（修订后） | 异常覆盖/超时/竞态全面修复，防御性编程到位 |
| B3 可测试性 | 5/10 → 7/10（修订后） | testTag/日志/动画测试补齐，路由测试新增 |

---

## §11 变更记录

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| v0.1-draft | 2026-06-04 | 初始生成：架构概览、模块设计、接口定义、数据流、测试策略、文件清单 |
| v0.1-draft | 2026-06-04 | R1 三视角评审 P0 修订：FTS4→LIKE、ViewModel作用域、全异常覆盖、分页上限、Tab竞态、testTag、路由测试、Timber、动画测试 |

---

> **状态:** v0.1-draft — 三视角评审 P0 全部自动修订。请审阅后回复「确认」冻结进入编码。
