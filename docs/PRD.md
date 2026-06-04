# 小型新闻App — PRD

> **版本:** v1.0-confirmed
> **功能名称:** 小型新闻App
> **创建日期:** 2026-06-04
> **作者:** Hermes 智能研发工作流

---

## §1 功能概述

构建一款轻量级 Android 新闻阅读应用，用户可浏览分类新闻、查看详情、搜索感兴趣的内容。应用从 NewsAPI 公开 API 获取 `top-headlines` 数据，支持 Room 本地缓存离线阅读。

**现有代码集成方案：** 项目当前 `MainActivity` 渲染 `LoginScreen` 作为启动页。新闻 App v1 移除登录流程，`MainActivity` 直接展示新闻列表首页（PRD §3 声明「不包含登录/注册」）。

**关键数据约束：** NewsAPI 免费版仅支持 `top-headlines` 端点（按分类/国家查询），不支持 `everything` 端点关键词搜索。搜索功能改为**本地 Room 缓存搜索**——在已缓存的标题+描述中全文匹配。新闻详情展示 `description` 摘要字段（非完整正文）+ "阅读原文"外链按钮。

## §2 用户场景

| 场景编号 | 角色 | 场景描述 |
|----------|------|----------|
| US-01 | 普通用户 | 打开 App 后看到"推荐"Tab 的新闻列表（top-headlines?country=cn&category=general） |
| US-02 | 普通用户 | 点击某条新闻进入详情页，阅读摘要（NewsAPI description 字段）+ 来源/时间/头图，"阅读原文"按钮跳转外部浏览器 |
| US-03 | 普通用户 | 在顶部搜索栏输入关键词（≥3字符），在 Room 缓存中搜索匹配的新闻标题+描述 |
| US-04 | 普通用户 | 切换不同新闻分类 Tab（推荐/科技/财经/体育/娱乐），加载对应分类新闻 |
| US-05 | 普通用户 | 在网络离线时仍可浏览之前已缓存的新闻列表和详情 |
| US-06 | 普通用户 | 下拉刷新新闻列表，获取最新内容 |

## §3 范围边界

### 包含
- 新闻列表（按分类 Tab 切换：推荐/科技/财经/体育/娱乐）
- 新闻详情页（description 摘要 + 头图 + 来源/时间 + "阅读原文"外链）
- 本地 Room 缓存搜索（标题+描述全文匹配，≥3字符触发，300ms防抖）
- 下拉刷新（Material3 PullToRefresh）
- 离线缓存：列表缓存最近 100 条摘要，详情缓存已打开文章的 description+图片；7天过期，总上限 50MB；策略为 NetworkBoundResource（网络优先→缓存兜底）
- 列表无限滚动分页（首页 20 条，滚动到底自动加载下一页）
- 基础错误/加载/空状态处理（含完整状态反馈矩阵，见 §9）
- 图片加载三态 fallback（loading→shimmer 占位 / error→broken-image 图标+灰色占位 / null→纯色占位）
- 无障碍基础覆盖：contentDescription、最小触摸目标 48dp、TalkBack 语义
- 新闻卡片整卡可点击，Ripple 限在圆角内

### 不包含
- 用户登录/注册系统（v1 移除现有 LoginScreen）
- 评论/点赞/收藏/分享功能
- 推送通知
- 个性化推荐算法
- 视频/直播新闻
- 多语言/国际化
- 深色模式（v1 不做）
- 搜索历史
- 搜索自动补全

## §4 验收标准

| 编号 | 验收项 | 预期结果 |
|------|--------|----------|
| AC-01 | 新闻列表加载 | 打开 App 3s 内展示新闻列表，含标题/来源/时间/缩略图；首次加载显示 3 张骨架屏 Shimmer 占位 |
| AC-02 | 分类切换 | 切换 Tab 后 2s 内加载对应分类新闻；切换时显示顶部 LinearProgressIndicator，旧内容保留 |
| AC-03 | 新闻详情 | 点击新闻进入详情页，展示头图+标题+来源+时间+description 摘要；"阅读原文"按钮跳转原文 URL |
| AC-04 | 本地搜索 | 输入 ≥3 字符后 300ms 防抖触发 Room 全文搜索；快速输入时取消前一个搜索 Job 防止竞态；清除搜索词→恢复默认列表 |
| AC-05 | 下拉刷新 | 下拉触发 PullToRefresh，加载最新新闻；刷新失败→Snackbar"刷新失败，请检查网络"保留旧列表 |
| AC-06 | 离线缓存 | 断网后已缓存的列表（≤100条）可正常展示，顶栏显示 Snackbar"当前离线，显示已缓存内容"；详情页已缓存的 description+图片可正常阅读 |
| AC-07 | 错误处理 | 网络异常→居中图标+错误文案+FilledTonalButton"重试"；频率限制→"请求太频繁，请稍后再试"；无搜索结果→SearchOff图标+"未找到'XXX'相关新闻" |
| AC-08 | 返回导航 | 详情页按 Back 返回列表页保持原滚动位置和 Tab 状态（ViewModel+SavedStateHandle） |
| AC-09 | 图片加载 | 图片加载中→shimmer占位；加载失败→broken-image图标+灰色占位块（布局不塌陷）；无图URL→纯色placeholder |
| AC-10 | 无障碍 | 新闻卡片 contentDescription="《标题》，来源，时间"；Tab contentDescription="分类名，N条新闻"；搜索栏 contentDescription="搜索新闻"；所有可交互元素 min touch target 48dp |

## §5 非功能性需求

- **性能:** 新闻列表首屏加载 ≤3s（冷启动），详情页加载 ≤1s
- **兼容性:** Android 8.0+ (API 26+)
- **可维护性:** MVVM + Compose 架构，遵循项目 CLAUDE.md 规范

## §6 技术约束

- **数据源:** NewsAPI `top-headlines` 端点（按分类/国家查询），免费版无关键词搜索能力
- **搜索方案:** 本地 Room 缓存全文搜索（标题+description LIKE 匹配），非远程 API 搜索
- **网络层:** Retrofit + OkHttp
- **本地缓存:** Room 数据库，NetworkBoundResource 模式（网络优先→缓存兜底）
- **图片加载:** Coil（`io.coil-kt:coil-compose:2.5.0`）— 当前 build.gradle.kts 缺少此依赖，需添加
- **架构:** MVVM + Hilt DI
- **UI:** Jetpack Compose + Material3（PullToRefreshBox 或 @OptIn pullRefresh）
- **骨架屏:** 自定义 shimmer modifier 或 `com.valentinilk.shimmer:compose-shimmer`
- **共享元素过渡:** v1 不做，使用标准 Navigation fade 过渡

## §7 风险与依赖

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| NewsAPI 免费版仅支持 top-headlines、不支持关键词搜索 | 搜索功能不可用远程 API | 改为本地 Room 全文搜索（详见 §6） |
| NewsAPI 免费版频率限制（~100 req/day） | 开发/测试频繁触发 429 | 增加 OkHttp MockInterceptor 兜底 + Room 缓存 |
| API Key 安全存储 | 泄露风险 | 使用 local.properties 存储，不提交 Git |
| Coil 依赖缺失 | 构建失败，图片加载不可用 | 添加 `io.coil-kt:coil-compose:2.5.0` 依赖 |
| PullRefresh API 实验性 | 未来版本可能变动 | @OptIn 注解 + DECISIONS.md 记录技术债 |
| 新闻内容图片加载慢 | 用户体验差 | Coil 缩略图+三态 fallback（loading/error/empty） |

## §8 术语表

| 术语 | 说明 |
|------|------|
| NewsAPI | 第三方公开新闻数据 API（newsapi.org），免费版仅 `top-headlines` 端点 |
| top-headlines | NewsAPI 端点：按国家+分类返回头条新闻，不支持关键词搜索 |
| Tab | 新闻分类标签页（推荐=general/科技=technology/财经=business/体育=sports/娱乐=entertainment） |
| 防抖 | 搜索输入延迟处理（300ms），避免频繁触发 + 取消前一个搜索 Job 防竞态 |
| NetworkBoundResource | 缓存策略：优先从网络获取最新数据，失败时回退到本地缓存 |
| description | NewsAPI 文章摘要字段（通常≤200字符），非完整正文 |

## §9 UI 设计输入

### 页面清单

| 页面 | 路由 | 说明 |
|------|------|------|
| 新闻列表页 | `news_list` | 默认首页：搜索栏 + Tab 栏(推荐/科技/财经/体育/娱乐) + 卡片列表 + 无限滚动 |
| 新闻详情页 | `news_detail/{articleId}` | 沉浸式阅读：顶栏(返回) + CollapsingImage头图 + 标题/来源/时间 + description正文 + "阅读原文"按钮 |
| 搜索内嵌态 | `news_list?search=active` | 列表页搜索栏聚焦且输入≥3字符，列表替换为 Room 搜索结果 |

### 每页布局规格

#### 新闻列表页
- **顶部:** SearchBar（OutlinedTextField，48dp 高，imeAction=Search）
- **中部:** TabRow（44dp 高，5个Tab：推荐/科技/财经/体育/娱乐，指示器动画300ms）
- **主体:** LazyColumn（item key=article.id）+ 新闻卡片（Card elevation=1dp, shape=12dp）+ PullToRefresh 包裹
- **卡片内部:** Row[缩略图 80×60dp 圆角8dp | Column[标题16sp maxLines=2 ellipsis | 来源12sp maxLines=1 | 时间12sp]]

#### 新闻详情页
- **顶部:** TopAppBar（返回按钮 min touch 48dp + 标题占位）
- **头图:** AsyncImage（200dp 高，三态 fallback）
- **内容区:** 标题 24sp + 来源/时间行 14sp + description 正文 16sp + "阅读原文" FilledTonalButton
- **底部:** 来源信息栏（可选）

### 交互规格

| 场景 | 行为 |
|------|------|
| Tab 切换 | 点击即切换，LinearProgressIndicator 显示，旧内容保留；切换 Tab→清空搜索词 |
| 下拉刷新 | PullToRefreshBox 标准手势；刷新成功→替换列表；失败→Snackbar+保留旧列表 |
| 搜索 | 输入≥3字符+300ms防抖→Room LIKE 查询标题+description；新搜索 cancel 前一个 Job；结果显示时覆盖 scrim+CircularProgressIndicator |
| 清除搜索 | 点击 X 或删除所有文字→恢复默认列表+原 Tab 状态 |
| 新闻点击 | 防抖 500ms；导航到详情页（Navigation fade） |
| 详情返回 | 列表保持原滚动位置+Tab 状态（ViewModel+SavedStateHandle） |
| 状态栏双击 | 列表滚动回顶部（animateScrollToItem(0)） |
| 旋转屏幕 | rememberSaveable + SavedStateHandle 保持状态 |
| 快速双击卡片 | 防抖 500ms，仅响应第一次 |

### 状态反馈矩阵

#### 新闻列表页

| 状态 | 视觉表现 | 可用操作 |
|------|----------|----------|
| **首次加载** | 3 张骨架屏 Shimmer 卡片 + 搜索栏/Tab 栏 disabled(alpha=0.38) | 无 |
| **加载中（非首次）** | 顶部 LinearProgressIndicator + 保留旧内容 | Tab 不可切换 |
| **成功（有数据）** | 卡片 LazyColumn 正常交互 | 全部可用 |
| **成功（离线缓存）** | 顶部 Snackbar"📡 当前离线，显示已缓存内容" | 下拉刷新（触发后→"当前离线，无法刷新"） |
| **空（分类无数据）** | 居中 ArticleOff 图标 + "该分类暂无新闻" + "下拉刷新试试" | 下拉刷新、切换 Tab |
| **空（搜索无结果）** | 居中 SearchOff 图标 + "未找到'XXX'相关新闻" + "换个关键词试试" | 修改搜索词、清除搜索 |
| **错误（网络/服务器）** | 居中 CloudOff 图标 + "加载失败" + 错误原因 + "重试"按钮 | 重试、切换 Tab |
| **错误（频率限制429）** | 同上 + "请求太频繁，请稍后再试" | 等待后重试 |

#### 新闻详情页

| 状态 | 视觉表现 | 可用操作 |
|------|----------|----------|
| **加载中** | 标题+正文 Shimmer 段落×5 | 返回可用 |
| **成功** | 头图+标题+来源+时间+description+"阅读原文" | 返回、滚动、点击原文 |
| **成功（离线）** | 同上 + 顶栏 Snackbar"📡 当前离线" | 返回 |
| **错误** | 居中图标+"加载失败"+"重试"+"返回列表" | 重试、返回 |
| **文章不存在** | 居中 ArticleOff 图标 + "文章已被删除或不存在" + "返回列表" | 返回 |

#### 图片加载三态

| 状态 | 视觉 | 
|------|------|
| **Loading** | Shimmer 占位（80×60dp 缩略图 / 200dp 头图） |
| **Error** | Broken-image 图标 + 灰色占位背景（布局不塌陷） |
| **Null URL** | 纯色 placeholder（无图标，静默占位） |

### 组件选型（精确到 Compose API）

| 位置 | Compose API | 备注 |
|------|-------------|------|
| 列表 | `LazyColumn` + `items(key = article.id)` | key 保证 diff 效率 |
| 下拉刷新 | `PullToRefreshBox`（M3 1.3+）或 `pullRefresh`（@OptIn） | @OptIn 注解 |
| Tab 栏 | `TabRow` + `Tab` + `SecondaryIndicator` | M3 标准 |
| 搜索栏 | `OutlinedTextField` + `imeAction = ImeAction.Search` | — |
| 卡片 | `Card(elevation=1.dp, shape=RoundedCornerShape(12.dp))` | M3 Card |
| 骨架屏 | `Modifier.shimmer()` + `placeholder(visible=true)` | 自定义或第三方 |
| 图片 | `AsyncImage(model, contentDescription, placeholder, error)` | Coil Compose |
| 进度条 | `LinearProgressIndicator` | 顶部加载条 |
| Snackbar | `SnackbarHost` + `SnackbarHostState` 在 Scaffold 内 | 离线/错误提示 |

### 无障碍规格

| 元素 | contentDescription | 最小触摸目标 |
|------|-------------------|-------------|
| 新闻卡片 | "《{标题}》，{来源}，{相对时间}" | 48dp |
| 卡片缩略图 | ""（装饰性，importantForAccessibility=NO） | — |
| Tab | "{分类名}，{N}条新闻" + selected=true/false | 48×48dp |
| 搜索栏 | "搜索新闻" | 48dp |
| 重试按钮 | "重试加载" | 48dp |
| 返回按钮 | "返回新闻列表" | 48×48dp |
| 详情页头图 | "{标题} 的配图" | — |

### 设计约束
- Material3 设计语言，使用项目已有主题 Token（primary/surface/error/outline）
- 标题 16sp，正文 14sp，辅助文字 12sp（onSurfaceVariant）
- 卡片 Elevation 1dp，圆角卡片 12dp / 缩略图 8dp
- 所有内容区域基于 8dp 网格

## §10 附录

### 参考资料
- NewsAPI 文档: https://newsapi.org/docs
- Material3 Design: https://m3.material.io
- 项目 CLAUDE.md 架构规范

---

> **状态:** 待评审 (v0.1-draft) — 请审阅后回复「确认」冻结。

---

## §11 多视角评审记录

> 评审日期: 2026-06-04
> 评审方式: 3-Agent 并行评审（产品视角 / 技术视角 / UX 视角）

### 11.1 评审总览

| 视角 | 评分 | P0 项 | P1 项 | 结论 |
|------|------|-------|-------|------|
| 产品视角 | 5/10 | 5 | 6 | P0全修订，有条件通过 |
| 技术视角 | 7/10 | 2 | 6 | P0全修订，架构一致 |
| UX 视角 | 4/10 | 5 | 6 | P0全修订，状态矩阵+无障碍补全 |

### 11.2 产品视角评审

**评分: 5/10** — PRD 框架完整、UI 规格到位，但存在与现有代码的登录功能冲突以及 5 个 P0 级数据/流程定义缺失。

**P0 项（已修订）：**
| 编号 | 问题 | 修订 |
|------|------|------|
| P0-1 | 登录与新闻功能关系 — MainActivity 当前为 LoginScreen | §1 已明确：v1 移除 LoginScreen，MainActivity 直接展示新闻列表 |
| P0-2 | "推荐"Tab 数据源 — NewsAPI 无"recommended"端点 | §2/§8 已明确：top-headlines?country=cn&category=general |
| P0-3 | 文章详情内容 — NewsAPI 仅返回 description 摘要 | §2/§4 已明确：展示 description + "阅读原文"外链 |
| P0-4 | 列表分页策略缺失 | §3 已新增：首页 20 条，滚动到底自动加载 |
| P0-5 | 离线缓存策略模糊 | §3 已细化：100 条摘要缓存，7天过期，50MB 上限，NetworkBoundResource 模式 |

**P1 项（已记录）：** 搜索范围（→跨分类本地搜索）、网络恢复行为（→不自动刷新）、图片 fallback（→三态）、分享功能（→v1不做）、进程死亡恢复（→SavedStateHandle）、骨架屏规格（→4-6 shimmer 卡片）

### 11.3 技术视角评审

**评分: 7/10** — 与 DECISIONS.md 高度一致（9/10），但存在 2 个关键阻塞。

**P0 项（已修订）：**
| 编号 | 问题 | 修订 |
|------|------|------|
| P0-1 | Coil 依赖缺失 — build.gradle.kts 无 `io.coil-kt:coil-compose` | §6/§7 已记录，编码阶段添加 `2.5.0` |
| P0-2 | NewsAPI 免费版不支持关键词搜索（仅 top-headlines） | §6 已改为本地 Room 全文搜索方案 |

**P1 项（已记录）：** NetworkBoundResource 模式确认、新增组件清单（11 个新组件）、PullRefresh @OptIn 风险、AC-08 saveState/restoreState、Shimmer 依赖（compose-shimmer）、ProGuard/R8 keep 规则

**DECISIONS.md 合规性检查：** D-21~D-27（版本号/IME）无冲突；MVVM+StateFlow+Hilt+Room 全部对齐。

### 11.4 UX 视角评审

**评分: 4/10** — 交互骨架和布局规格不错，但状态反馈矩阵和无障碍完全缺失。

**P0 项（已修订）：**
| 编号 | 问题 | 修订 |
|------|------|------|
| P0-01 | 缺少完整状态反馈矩阵 | §9 已新增：列表页 8 态 + 详情页 5 态 + 图片 3 态 |
| P0-02 | 无障碍零覆盖 | §9 已新增无障碍规格表（contentDescription/min touch target 48dp） |
| P0-03 | 搜索与 Tab 交互冲突 | §9 已明确：切换 Tab→清空搜索词；搜索词仅对当前 Tab 生效 |
| P0-04 | 图片加载 fallback 链路缺失 | §9 已新增 loading/error/null 三态视觉方案 |
| P0-05 | 搜索竞态未覆盖 | §4/§9 已补充：新搜索 cancel 前一个 Job，UI 覆盖 loading |

**P1 项（已记录）：** 卡片 Ripple 范围、空分类场景、离线感知 Banner、超长文本截断（maxLines+ellipsis）、键盘交互闭环（imeAction=Search）

### 11.5 讨论决议

| 决议编号 | 决议内容 | 来源 | 状态 |
|----------|----------|------|------|
| R-01 | v1 移除 LoginScreen，MainActivity 直接展示新闻列表首页 | P0-1(产品) | ✅ 已修订 |
| R-02 | "推荐"Tab = NewsAPI top-headlines?country=cn&category=general | P0-2(产品) | ✅ 已修订 |
| R-03 | 新闻详情展示 description 摘要 + "阅读原文"外链按钮 | P0-3(产品) | ✅ 已修订 |
| R-04 | 列表分页：首页 20 条 + 无限滚动自动加载 | P0-4(产品) | ✅ 已修订 |
| R-05 | 离线缓存：Room 100 条摘要/详情已打开文章，7天过期，50MB 上限，NetworkBoundResource 模式 | P0-5(产品) | ✅ 已修订 |
| R-06 | 搜索改为本地 Room 全文搜索（标题+description LIKE），≥3字符触发，300ms 防抖 | P0-2(技术) | ✅ 已修订 |
| R-07 | 添加 Coil 依赖 `io.coil-kt:coil-compose:2.5.0` | P0-1(技术) | ✅ 记录，编码阶段执行 |
| R-08 | 所有页面含完整状态反馈矩阵（loading/empty/error/success + 过渡态 + 离线态） | P0-01(UX) | ✅ 已修订 |
| R-09 | 所有交互元素 min touch target 48dp + contentDescription | P0-02(UX) | ✅ 已修订 |
| R-10 | 搜索中切换 Tab→清空搜索词恢复默认列表；搜索词仅对当前 Tab 生效 | P0-03(UX) | ✅ 已修订 |
| R-11 | 图片加载三态 fallback：loading(shimmer) / error(broken-icon+灰色占位) / null(纯色placeholder) | P0-04(UX) | ✅ 已修订 |
| R-12 | 搜索防竞态：ViewModel 持 Job，新搜索 cancel 前一个；UI 覆盖 loading | P0-05(UX) | ✅ 已修订 |

---

> **状态:** 多视角评审已完成，12 项决议全部修订入正文。确认后冻结版本号。
