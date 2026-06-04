# 小型新闻App — UI 设计方案

> **版本:** v0.2-review
> **功能名称:** 小型新闻App
> **设计日期:** 2026-06-04
> **作者:** Hermes AI Design

---

## §1 页面清单

| 编号 | 页面 | 路由 | 说明 |
|------|------|------|------|
| P1 | 新闻列表页 | `news_list` | 默认首页：SearchBar + 5 Tab (推荐/科技/财经/体育/娱乐) + 卡片 LazyColumn + PullToRefresh + 无限滚动 |
| P2 | 新闻详情页 | `news_detail/{articleId}` | 沉浸式阅读：TopAppBar(返回) + 固定头图(200dp) + 标题/来源/时间 + description 正文 + "阅读原文"按钮 |
| P3 | 搜索内嵌态 | `news_list?search=active` | 列表页搜索栏聚焦 ≥3字符输入→ Room 本地搜索结果替换列表内容 |

---

## §2 线框图（ASCII Art）

### P1 新闻列表页（默认态）

```
┌──────────────────────────────────────┐
│  🔍 搜索新闻                         │ ← OutlinedTextField 48dp
├──────────────────────────────────────┤
│ [推荐] [科技] [财经] [体育] [娱乐]     │ ← TabRow 44dp + SecondaryIndicator
├──────────────────────────────────────┤
│ ┌──────────────────────────────────┐ │ ← Card 12dp圆角 elevation=1dp
│ │ ┌────────┐ 标题标题标题标题...   │ │   Row: 缩略图80×60dp 8dp圆角
│ │ │ 缩略图  │ 来源 · 2小时前       │ │   | Column(标题 16sp 2行
│ │ │ 80×60  │                      │ │   |         来源 12sp
│ │ └────────┘                      │ │   |         时间 12sp)
│ └──────────────────────────────────┘ │   卡片间距 12dp
│                                      │
│ ┌──────────────────────────────────┐ │
│ │ ┌────────┐ 标题标题标题标题...   │ │
│ │ │ 缩略图  │ 来源 · 5小时前       │ │
│ │ └────────┘                      │ │
│ └──────────────────────────────────┘ │
│                                      │
│  ─── 加载更多 ───                     │ ← 分页加载指示器
└──────────────────────────────────────┘
```

### P1 首次加载态（骨架屏）

```
┌──────────────────────────────────────┐
│  🔍 搜索新闻 (disabled alpha=0.38)   │
├──────────────────────────────────────┤
│ [推荐] [科技] [财经] [体育] [娱乐]     │ ← disabled alpha=0.38
├──────────────────────────────────────┤
│ ┌──────────────────────────────────┐ │
│ │ ░░░░░░░░░░  ░░░░░░░░░░░░░░░░░░  │ │ ← Shimmer 动画
│ │ ░░░░░░░░░░  ░░░░░░░░░░░░░░░░░░  │ │   3 张骨架屏卡片
│ │              ░░░░░░░░░░         │ │
│ └──────────────────────────────────┘ │
│ ┌──────────────────────────────────┐ │
│ │ ░░░░░░░░░░  ░░░░░░░░░░░░░░░░░░  │ │
│ │ ░░░░░░░░░░  ░░░░░░░░░░░░░░░░░░  │ │
│ └──────────────────────────────────┘ │
└──────────────────────────────────────┘
```

### P1 错误态

```
┌──────────────────────────────────────┐
│  🔍 搜索新闻                         │
├──────────────────────────────────────┤
│ [推荐] [科技] [财经] [体育] [娱乐]     │
├──────────────────────────────────────┤
│                                      │
│              ☁️                      │ ← CloudOff 图标 64dp
│          加载失败                     │ ← headlineSmall
│      请检查网络连接后重试              │ ← bodyMedium (onSurfaceVariant)
│                                      │
│         [ 重试加载 ]                  │ ← FilledTonalButton
│                                      │
└──────────────────────────────────────┘
```

### P1 空分类态

```
┌──────────────────────────────────────┐
│  🔍 搜索新闻                         │
├──────────────────────────────────────┤
│ [推荐] [科技] [财经] [体育] [娱乐]     │
├──────────────────────────────────────┤
│                                      │
│           📰?                        │ ← ArticleOff 图标 64dp
│       该分类暂无新闻                   │ ← headlineSmall
│       下拉刷新试试                     │ ← bodyMedium
│                                      │
└──────────────────────────────────────┘
```

### P2 新闻详情页

```
┌──────────────────────────────────────┐
│ ← 返回                               │ ← TopAppBar + NavigationIcon
├──────────────────────────────────────┤
│ ┌──────────────────────────────────┐ │
│ │                                  │ │ ← AsyncImage 200dp 高
│ │          新闻头图                 │ │   三态 fallback
│ │                                  │ │
│ └──────────────────────────────────┘ │
│                                      │
│  标题标题标题标题标题标题标题标题标题    │ ← headlineSmall 24sp
│  标题标题标题标题标题标题               │
│                                      │
│  来源名称  ·  2026-06-04 10:30       │ ← bodyMedium 14sp
│                                      │
│  正文正文正文正文正文正文正文正文正文    │ ← bodyLarge 16sp
│  正文正文正文正文正文正文正文正文正文    │
│  正文正文正文正文正文正文正文...        │
│                                      │
│  ┌────────────────────────────────┐  │
│  │       阅读原文 (NewsAPI)        │  │ ← FilledTonalButton
│  └────────────────────────────────┘  │
│                                      │
│  ─── 来源: NewsAPI ───               │ ← labelSmall footer
└──────────────────────────────────────┘
```

### P3 搜索内嵌态（结果）

```
┌──────────────────────────────────────┐
│  🔍 AI科技 (X)                       │ ← OutlinedTextField 有内容
├──────────────────────────────────────┤
│ [推荐] [科技] [财经] [体育] [娱乐]     │
├──────────────────────────────────────┤
│  找到 3 条结果                        │ ← labelMedium 12sp outline色
│                                      │
│ ┌──────────────────────────────────┐ │
│ │ ┌────────┐ AI科技新突破...       │ │ ← 关键字高亮(primary色)
│ │ │ 缩略图  │ 来源 · 2小时前       │ │
│ │ └────────┘                      │ │
│ └──────────────────────────────────┘ │
│ ┌──────────────────────────────────┐ │
│ │ ┌────────┐ AI助力科技发展...     │ │
│ │ │ 缩略图  │ 来源 · 1天前         │ │
│ │ └────────┘                      │ │
│ └──────────────────────────────────┘ │
└──────────────────────────────────────┘
```

### P3 搜索无结果态

```
┌──────────────────────────────────────┐
│  🔍 xyz不存在的词 (X)                │
├──────────────────────────────────────┤
│ [推荐] [科技] [财经] [体育] [娱乐]     │
├──────────────────────────────────────┤
│                                      │
│              🔍?                     │ ← SearchOff 图标 64dp
│      未找到"xyz不存在的词"            │ ← headlineSmall
│       相关新闻                        │
│      换个关键词试试                    │ ← bodyMedium
│                                      │
└──────────────────────────────────────┘
```

---

## §3 组件层级树

### P1 新闻列表页

```
NewsListScreen
├── Scaffold
│   ├── topBar: SearchBar (OutlinedTextField)
│   │   ├── leadingIcon: Search icon (24dp)
│   │   ├── placeholder: "搜索新闻"
│   │   │   └── trailingIcon: Clear (X, 48×48dp, visible when text non-empty)
│   └── content:
│       └── Column(Modifier.padding(horizontal=16.dp))
│           ├── TabRow
│           │   ├── Tab("推荐")
│           │   ├── Tab("科技")
│           │   ├── Tab("财经")
│           │   ├── Tab("体育")
│           │   └── Tab("娱乐")
│           │
│           └── PullToRefreshBox(isRefreshing, onRefresh)
│               └── when {
│                   isFirstLoading → ShimmerList (3 skeleton cards)
│                   isError → ErrorState (CloudOff icon + "重试" button)
│                   isEmpty → EmptyState (ArticleOff icon + "暂无新闻")
│                   isSearchActive && searchQuery.length >= 3 →
│                       SearchResultsList (highlighted items)
│                   else → NewsCardList
│               }
│
│   └── snackbarHost: SnackbarHostState (离线提示)
│
│ NewsCard (per item, key=article.id)
│ ├── Card(shape=12.dp, elevation=1.dp)
│ │   └── Row(padding=16.dp)
│ │       ├── AsyncImage(80×60dp, shape=8.dp, 3-state fallback)
│ │       └── Column(weight=1f, spacing=4.dp)
│ │           ├── Text(title, maxLines=2, ellipsis, 16sp)
│ │           ├── Text(source, maxLines=1, ellipsis, 12sp, onSurfaceVariant)
│ │           └── Text(time, 12sp, onSurfaceVariant)
│ └── clickable → NavController.navigate("news_detail/{id}")
│
│ ShimmerCard (per skeleton item)
│ └── Card(shape=12.dp, elevation=1.dp)
│     └── Row(padding=16.dp)
│         ├── Box(80×60dp, shimmer)  // 缩略图占位
│         └── Column(spacing=8.dp)
│             ├── Box(fillMaxWidth×0.7, 16.dp, shimmer)  // 标题占位
│             ├── Box(fillMaxWidth×0.4, 12.dp, shimmer)  // 来源占位
│             └── Box(fillMaxWidth×0.25, 12.dp, shimmer) // 时间占位
```

### P2 新闻详情页

```
NewsDetailScreen
├── Scaffold
│   ├── topBar: TopAppBar
│   │   ├── navigationIcon: IconButton(←, contentDescription="返回新闻列表")
│   │   └── title: "" (empty)
│   └── content:
│       └── Column(Modifier.verticalScroll)
│           ├── when {
│               isImageLoading → Shimmer(200dp)     // 头图加载中
│               isImageError → BrokenImagePlaceholder(200dp)  // 头图失败
│               hasImageUrl → AsyncImage(200dp)      // 头图成功
│           }
│           ├── Text(title, 24sp, headlineSmall, padding=16dp)
│           ├── Row(padding=horizontal 16dp)
│           │   ├── Text(source, 14sp, bodyMedium)
│           │   └── Text("  ·  ", 14sp)
│           │   └── Text(publishedAt, 14sp, bodyMedium)
│           ├── Text(description, 16sp, bodyLarge, padding=16dp)
│           └── FilledTonalButton("阅读原文")
│               └── onClick → openUrl(article.url)
│   └── snackbarHost (离线提示)
│
│ ErrorState (详情页加载失败)
└── Column(center, fillMaxSize)
    ├── Icon(ArticleOff, 64dp, outline)
    ├── Text("文章已被删除或不存在", headlineSmall)
    └── Row
        ├── FilledTonalButton("重试")
        └── TextButton("返回列表")
```

### P3 搜索内嵌态

```
SearchOverlay (内嵌于 NewsListScreen)
├── AnimatedVisibility(visible = isSearchActive)
│   └── Box(fillMaxSize)
│       ├── when {
│           isSearching → Box(scrim + CircularProgressIndicator)
│           searchResults.isEmpty() → EmptySearchState
│           else → LazyColumn(searchResults, key=article.id)
│       }
│
│ SearchResultItem
│ ├── Card → same as NewsCard
│ └── 标题中 searchQuery 匹配部分:
│     └── SpanStyle(color=primary, fontWeight=Bold, background=primary.copy(alpha=0.12))
```

---

## §4 交互状态机

### 列表页状态机

```
                    ┌─────────────────────┐
                    │    Idle（初始态）     │
                    │  无缓存无网络 → 请求   │
                    └────────┬────────────┘
                             │ start data fetch
                             ▼
              ┌──────────────────────────────┐
              │       FirstLoading            │
              │  3张骨架屏 Shimmer            │
              │  Tab/Search disabled(0.38)    │──── timeout 5s ──→ Error
              └──────────┬───────────────────┘
                         │ success            │ fail
                         ▼                    ▼
              ┌──────────────────┐  ┌──────────────────────────┐
              │   Success        │  │       Error              │
              │  LazyColumn 卡片  │  │ CloudOff icon + 错误文本  │
              │  全交互可用        │  │ + "重试"按钮              │
              └──┬────┬────┬─────┘  └──────────┬───────────────┘
                 │    │    │                    │ 点击重试 → FirstLoading
     ┌───────────┘    │    └──────────┐         │
     ▼                ▼               ▼         │
┌─────────┐  ┌──────────────┐  ┌────────────┐  │
│ 下拉刷新 │  │ Tab 切换      │  │ 搜索 ≥3字符 │  │
│ PullRef │  │ Loading(非首次)│  │ → Search   │  │
│ -resh   │  │ 保留旧列表     │  │ Overlay    │  │
└────┬────┘  └──────┬───────┘  └──────┬─────┘  │
     │              │                 │         │
     │  成功/失败    │   成功/失败      │         │
     ▼              ▼                 ▼         │
  成功→Success  成功→Success      结果→Res- ────┘
  失败→Snackbar 空→Empty           ultState
  保留旧列表    失败→Error         无→EmptyS
                                    earchState
```

### 搜索态状态机

```
   Idle（搜索栏失焦）
     │ 点击搜索栏/输入
     ▼
   Typing（输入中，<3字符）
     │ 不触发搜索，保持上一个≥3字符的搜索结果（冻结态）
     │ 搜索栏下方显示提示文字 「请输入至少3个字符」
     │ 输入 ≥3字符 + 300ms防抖
     ▼
   Searching（搜索中）
     │ scrim + CircularProgressIndicator
     │ 覆盖在原列表上
     ├── timeout 5s → Error
     ├── 返回结果 >0 → Results
     └── 返回结果 =0 → NoResults
     │
   Typing → 新输入 → 取消前一个 Job → Searching
   │
   IME Search Action (键盘搜索键) → 立即触发搜索（不等防抖）
   │
   用户手势下滑收起键盘 → 保持搜索结果 + 隐藏 scrim，搜索栏保留文本+X按钮
```

### 详情页状态机

```
   Idle（从列表导航进入，携带 articleId）
     │ 开始加载
     ▼
   Loading
     │ 标题+正文 Shimmer 段落 ×5
     │ 返回按钮可用
     ├── success → Success
     ├── error (网络) → Error (重试+返回)
     └── error (文章不存在) → NotFound (返回)
     │
   Success
     │ 头图+标题+来源+时间+description+"阅读原文"
     │ 离线时 + Snackbar "📡 当前离线"
     │
   Error → "重试" → Loading
   NotFound → "返回列表" → news_list(保持状态)
```

---

## §5 状态覆盖表

| 状态 | 触发条件 | 视觉表现 | 可用操作 |
|------|----------|----------|----------|
| **首次加载** | 冷启动，无缓存，首次 fetch | 3 张骨架屏 Shimmer 卡片；SearchBar/TabBar disabled(alpha=0.38) | 无（等待完成） |
| **加载中（非首次）** | Tab 切换、搜索触发 | 顶部 LinearProgressIndicator；旧列表保留不闪烁 | Tab 不可切换 |
| **成功（在线）** | API 返回 articles 非空 | LazyColumn 卡片列表，全交互可用 | 全部：下拉刷新、点击卡片、Tab、搜索 |
| **成功（离线）** | 无网络 + Room 有缓存 | 列表正常交互 + Snackbar "📡 当前离线，显示已缓存内容" | 下拉刷新→"当前离线，无法刷新" |
| **空*分类** | API 某分类返回空数组 | 居中 ArticleOff 图标(64dp, outline) + "该分类暂无新闻" + "下拉刷新试试" | 下拉刷新、切换 Tab |
| **空*搜索** | Room LIKE 查询匹配 0 条 | 居中 SearchOff 图标(64dp) + "未找到'{query}'相关新闻" + "换个关键词试试" | 修改搜索词、清除搜索 |
| **错误*网络** | HTTP 非 2xx / IOException | 居中 CloudOff 图标 + "加载失败" + 错误原因 + FilledTonalButton "重试" | 重试、切换 Tab |
| **错误*限流** | HTTP 429 | 同上 + "请求太频繁，请稍后再试" | 等待重试 |
| **错误*超时** | 请求 >5s 无响应 | 同上 + "请求超时，请重试" | 重试 |
| **分页*加载中** | 滚动到底触发分页加载 | 底部 CircularProgressIndicator + 保留已加载列表 | 正常交互（已加载卡片可点击） |
| **分页*失败** | 分页请求失败 | Snackbar "加载更多失败，请重试" + 底部 "加载更多" 重试按钮 | 点击重试、正常交互 |
| **离线*无缓存** | 离线且该分类无 Room 缓存 | 居中 CloudOff 图标 + "当前离线，该分类无缓存数据" + "联网后下拉刷新" | 切换 Tab |
| **详情*加载中** | 导航到详情页，fetch article | 顶栏 + 标题/正文 Shimmer 段落 ×5；返回按钮可用 | 返回 |
| **详情*成功** | API 返回 article | 头图 200dp + 标题 24sp + 来源·时间 14sp + description 16sp + "阅读原文"按钮 | 返回、滚动、原文链接 |
| **详情*离线** | 无网络 + Room 有缓存 | 同成功 + Snackbar "📡 当前离线" | 返回 |
| **详情*错误** | 网络异常 | 居中 ArticleOff 图标 + "加载失败" + "重试" + "返回列表" | 重试、返回 |
| **详情*不存在** | API 404 / article deleted | 居中 ArticleOff 图标 + "文章已被删除" + "返回列表" | 返回 |
| **图片*加载中** | AsyncImage 请求中 | Shimmer 占位 80×60/200dp | — |
| **图片*失败** | 图片 URL 加载失败 | broken-image 图标 + 灰色占位背景（布局不塌陷） | — |
| **图片*null** | article.urlToImage = null | 纯色 placeholder（无图标） | — |

---

## §6 Token 映射表

### 颜色 Token

| 语义 Token | 用途 | 日间值 | 暗色值 (预置) |
|------------|------|--------|---------------|
| `primary` | Tab 选中指示器、按钮、链接、搜索高亮 | `#1A4FBF` | `#B0C6FF` |
| `onPrimary` | 主色上的文字 | `#FFFFFF` | `#001D36` |
| `primaryContainer` | 淡底色容器 | `#D8E2FF` | `#00315F` |
| `surface` | 页面背景、卡片背景 | `#FEFBFF` | `#1C1B1F` |
| `surfaceVariant` | 搜索栏背景、骨架屏色 | `#E7E0EC` | `#49454F` |
| `onSurface` | 标题、正文文字 | `#1C1B1E` | `#E6E1E5` |
| `onSurfaceVariant` | 来源、时间辅助文字 | `#49454F` | `#CAC4D0` |
| `error` | 错误图标、错误文字 | `#B81C1C` | `#FFB4AB` |
| `onError` | 错误色上的文字 | `#FFFFFF` | `#690005` |
| `outline` | 搜索栏边框、空状态图标 | `#79747E` | `#938F99` |
| `outlineVariant` | 卡片分割线 | `#CAC4D0` | `#49454F` |

### 字体 Token

| 语义 Token | 用途 | 大小/行高 | Compose API |
|------------|------|-----------|-------------|
| `headlineSmall` | 详情页标题 | 24sp / 32sp | `MaterialTheme.typography.headlineSmall` |
| `titleMedium` | 新闻卡片标题 | 16sp / 24sp | `MaterialTheme.typography.titleMedium` |
| `bodyLarge` | 详情正文 | 16sp / 24sp | `MaterialTheme.typography.bodyLarge` |
| `bodyMedium` | 来源、时间、辅助文本 | 14sp / 20sp | `MaterialTheme.typography.bodyMedium` |
| `labelMedium` | 搜索结果计数 | 12sp / 16sp | `MaterialTheme.typography.labelMedium` |
| `labelSmall` | Tab 文字 | 12sp / 16sp | `MaterialTheme.typography.labelSmall` |

### 间距 Token（8dp 网格）

| Token | 值 | 用途 |
|-------|-----|------|
| `spacing-xs` | 4dp | 卡片内部小间距 |
| `spacing-sm` | 8dp | Tab 与列表间距 |
| `spacing-md` | 12dp | 卡片间距 |
| `spacing-lg` | 16dp | 页面水平 padding、卡片内边距 |
| `spacing-xl` | 24dp | 区块间距 |
| `card-elevation` | 1dp | 新闻卡片阴影 |
| `card-shape` | 12dp | 卡片圆角 |
| `thumbnail-shape` | 8dp | 缩略图圆角 |

---

## §7 组件复用分析

| 现有组件 | 位置 | 复用？ | 变更 |
|----------|------|--------|------|
| `VersionTag` | `ui/components/VersionTag.kt` | ✅ 直接复用 | 保留在 MainActivity BottomCenter |
| `ImeUtil.isKeyboardVisible()` | `ui/util/ImeUtil.kt` | ✅ 直接复用 | 列表页搜索键盘适配 |
| `MainActivity` | `MainActivity.kt` | ⚠️ 需改造 | 移除 LoginScreen → 替换为 NewsListScreen |
| `LoginScreen` | `MainActivity.kt` | ❌ 删除 | D-28: v1 移除登录 |
| `LoginViewModel` | `ui/login/LoginViewModel.kt` | ❌ 删除 | 不在 scope 内 |
| `LoginRepository` | `data/LoginRepository.kt` | ❌ 可删 | 不在 scope 内 |

### 新增组件清单

| 组件 | 层级 | 复杂度 | 预估行数 |
|------|------|--------|----------|
| `NewsListScreen` + `NewsListViewModel` | UI + VM | 🔴 高 | ~250 |
| `NewsDetailScreen` + `NewsDetailViewModel` | UI + VM | 🟡 中 | ~180 |
| `NewsCard` | UI Component | 🟢 低 | ~60 |
| `ShimmerCard` | UI Component | 🟢 低 | ~50 |
| `NewsApiService` (Retrofit) | Data/Remote | 🟢 低 | ~30 |
| `NewsDao` + `NewsArticleEntity` | Data/Local | 🟢 低 | ~50 |
| `NewsRepository` | Data | 🟡 中 | ~100 |
| `SearchRepository` | Data | 🟡 中 | ~60 |
| `NetworkModule` / `DatabaseModule` | DI | 🟢 低 | ~40 |
| `ErrorState`, `EmptyState` | UI Component | 🟢 低 | ~40 |

---

## §8 架构协调设计

### 导航架构

```kotlin
NavHost(startDestination = "news_list") {
    composable("news_list") {
        NewsListScreen(
            onArticleClick = { id -> navController.navigate("news_detail/$id") }
        )
    }
    composable("news_detail/{articleId}") { backStackEntry ->
        NewsDetailScreen(
            articleId = backStackEntry.arguments?.getString("articleId") ?: "",
            onBack = { navController.popBackStack() }
        )
    }
}
```

> **D-28 已确认**: MainActivity 直接渲染 NewsListScreen，移除 LoginScreen 和登录流程。

### BackHandler 互斥策略

搜索激活时，列表页 BackHandler 被禁用，搜索层 BackHandler 优先：

```kotlin
// 宿主 NewsListScreen
BackHandler(enabled = !isSearchActive) {
    if (navController.currentBackStackEntry?.destination?.route == "news_list") {
        activity.finish()  // 退出 App
    }
}

// 搜索覆盖层
BackHandler(enabled = isSearchActive) {
    searchQuery = ""
    isSearchActive = false
    focusManager.clearFocus()
}
```

### 跨 Screen 事件通道

详情页返回 → 列表保持状态通过 ViewModel scope 实现。NewsListViewModel 作用域为 Activity（而非 composable），确保导航返回时 StateFlow 数据不丢失：

```kotlin
val viewModel: NewsListViewModel = hiltViewModel()  // Activity-scoped 自动
```

### IME 适配

- 列表页搜索栏聚焦 → `imePadding()` 处理键盘上推
- 搜索覆盖层激活 → `AnimatedVisibility(visible = !isImeVisible)` 隐藏底部 VersionTag
- 详情页无 TextField → 无 IME 适配需求

---

## §9 变更记录

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| v0.1-draft | 2026-06-04 | 初始生成：页面清单、线框图、组件层级树、状态机、Token 映射、复用分析 |
| v0.2-review | 2026-06-04 | 三视角评审+质量门禁：新增分页/离线无缓存状态，修复IME Action/CollapsingImage术语/清除按钮48dp/卡片padding 16dp/Android视口标注，10项P0全部修订 |

---

## §10 三视角评审记录

> 评审日期: 2026-06-04 | 评审方式: delegate_task 并行 C1(UIC交互)/C2(视觉审美)/C3(前端实现)

### 10.1 评审总览

| 视角 | 评分 | P0 项 | P1 项 | P2 项 | 结论 |
|------|------|-------|-------|-------|------|
| C1 UIC交互 | 7/10 | 3 | 6 | 6 | P0全修订 |
| C2 视觉审美 | 35/50 | 4 | 8 | 6 | P0全修订 |
| C3 前端实现 | 7/10 | 3 | 8 | 10 | P0全修订 |

### 10.2 P0 决议

| 编号 | 来源 | 问题 | 修订 |
|------|------|------|------|
| UR-01 | C1 | 分页加载失败态缺失 | §5 新增「分页加载中」和「分页失败」状态 |
| UR-02 | C1 | IME Search Action 未定义 | §4 新增 IME Search Action→立即搜索（不等防抖）路径 |
| UR-03 | C1 | 搜索词删至1-2字符过渡行为 | §4 明确保持上一个≥3字符结果冻结+提示"请输入至少3个字符" |
| UR-04 | C2 | 搜索清除按钮36×36px < 48dp | 组件树更新为 48×48dp |
| UR-05 | C2 | 卡片 padding 12px → 16dp | 组件树 Column 添加 padding(horizontal=16.dp) |
| UR-06 | C2 | HTML 使用 iPhone 视口 | HTML 标注为示意原型，实际开发使用 Android 视口 |
| UR-07 | C2 | 详情标题 22sp → 24sp | Token 表确认 headlineSmall=24sp/32sp |
| UR-08 | C3 | Coil 依赖缺失 | 编码阶段添加 io.coil-kt:coil-compose:2.5.0 |
| UR-09 | C3 | PullToRefreshBox 与 BOM 不兼容 | 编码阶段升级 composeBom 至 2024.06.00 或使用 @OptIn pullRefresh |
| UR-10 | C3 | 导航参数 API 已废弃 | 使用 navArgument DSL 声明路由参数 |

### 10.3 C1 UIC交互评审摘要

**评分 7/10** — 状态矩阵/无障碍/搜索竞态已修订到位。P0: 分页失败态、IME闭环、删字过渡行为。P1: CollapsingImage术语(已改为固定头图)、加载中Tab disabled反馈、离线无缓存分类、搜索结果滚动位置、5Tab溢出(建议ScrollableTabRow)、阅读原文跳转(建议CustomTabs)。

### 10.4 C2 视觉审美评审摘要

**评分 35/50 (B级)** — M3 token体系架构规范、状态覆盖完整。亮点: shimmer动画设计精良、搜索无结果态关键字高亮细节、空状态文案友好。主要短板: 平台适配(iPhone视口)、可操作性(触控区域)、情感品牌(差异化)。

### 10.5 C3 前端实现评审摘要

**评分 7/10** — 状态机设计优秀。P0: Coil依赖/PullToRefreshBox BOM不兼容/导航API废弃。工时校准: 原估6.0d → 校准10.6d(+77%)，主要遗漏主题代码/测试/配置/无障碍/集成调试。

---

> **状态:** v0.2-review — 三视角评审完成，10项P0全部修订。请审阅后回复「确认」冻结进入技术方案。
