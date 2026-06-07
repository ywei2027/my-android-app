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

---

## 登录模块 — PRD 评审阶段（2026-06-07）

### 评审概况
| 轮次 | 日期 | 方式 | 评审结论 |
|------|------|------|----------|
| R1 | 2026-06-07 | 4-Agent 并行 (产品/技术/UX/QA) | 22 P0 已识别，14 P1 编码阶段消化，待人工确认冻结 |

### 核心决策：邮箱+密码替代手机号+验证码
| 属性 | 原方案（2026-05-31） | 新方案（2026-06-07） |
|------|---------------------|---------------------|
| 认证方式 | 手机号+验证码 | 邮箱+密码 |
| 理由 | 试点阶段无必要复杂密码体系 | 用户需求明确要求邮箱+密码；邮箱作为通用身份标识更符合账号体系长期规划 |
| 否决原因 | 邮箱+密码增加开发成本 | — |

**决策（v2，覆盖旧版）：** 采用邮箱+密码登录，覆盖 2026-05-31 决策。现有 LoginViewModel/LoginScreen/AuthModels 等组件需从 username→email 重构。关联 PRD.md R-01。

### 22 项 P0 致命决议（已识别，待修订）
| # | 决议 | 来源 | 关联 PRD |
|----|------|------|----------|
| D-65 | 覆盖 DECISIONS.md 原决策，重新评估邮箱+密码为主要登录方式 | 产品+技术 | R-01 |
| D-66 | 补充注册入口或明确账号来源（管理员预分配/最简注册页） | 产品 | R-02 |
| D-67 | Token 持久化纳入首版（DataStore 加密存储，启动自动校验） | 产品 | R-03 |
| D-68 | 补充网络异常场景（超时→错误提示→重试） | 产品+QA | R-04 |
| D-69 | 补充 403/429 错误码交互和测试覆盖 | 产品+QA | R-05 |
| D-70 | 新建 LoginApi (Retrofit) 接口，替换 AuthRepository 硬编码 mock | 技术 | R-06 |
| D-71 | LoginResponse 数据模型重构为 `token+user{id,email,displayName}` | 技术 | R-07 |
| D-72 | 实现邮箱格式客户端校验，LoginScreen 字段 username→email | 技术 | R-08 |
| D-73 | 修复登录按钮启用条件 | 技术+UX | R-09 |
| D-74 | 实现返回键退出应用（BackHandler） | 技术 | R-10 |
| D-75 | AuthRepository 添加 HttpException 捕获 + withTimeout | 技术 | R-11 |
| D-76 | 定义品牌色 Token primary=#1A73E8 + M3 ColorScheme | UX | R-12 |
| D-77 | 字体层级映射 M3 Token | UX | R-13 |
| D-78 | 间距基于 8dp 网格系统 | UX | R-14 |
| D-79 | 补齐无障碍（contentDescription/semantics） | UX | R-15 |
| D-80 | 明确键盘类型和焦点链 | UX | R-16 |
| D-81 | 补全 Gherkin 测试用例（超时/403/429/状态枚举） | QA | R-17 |
| D-82 | 修复 Loading 中间态测试断言 | QA | R-18 |
| D-83 | 补充按钮联动正向验证测试 | QA | R-19 |
| D-84 | 字段重构同步更新全部测试 | QA | R-20 |
| D-85 | 统一错误展示为内联 Text+AnimatedVisibility | UX | R-21 |
| D-86 | 添加登录成功过渡态（✓ 300ms→导航） | UX | R-22 |

### 14 项 P1 重要决议（编码阶段消化）
| # | 决议 | 来源 | 关联 PRD |
|----|------|------|----------|
| D-87 | LoginStateManager key username→user 对象 | 技术 | R-25 |
| D-88 | LoginViewModel 使用 collectLatest（D-55） | 技术 | R-26 |
| D-89 | LoginViewModel 添加 onCleared()（D-58） | 技术 | R-27 |
| D-90 | 新增 LoginScreen preview test（D-61） | 技术 | R-28 |
| D-91 | 密码显隐图标 testTag（D-46） | 技术 | R-29 |
| D-92 | AuthModule 改为 Hilt+Retrofit 注入 | 技术 | R-30 |
| D-93 | LoginUiState 添加显式 status 枚举 | 技术 | R-31 |
| D-94 | Token refresh 标注 v1.1 | 技术 | R-32 |
| D-95 | 网络超时 UI（>10s→提示+恢复） | UX | R-33 |
| D-96 | 429 限流文案+禁用 N 秒 | UX | R-34 |
| D-97 | 输入框 maxLength 约束反馈 | UX | R-35 |
| D-98 | Loading 态输入框 disabled UI 验证 | QA | R-36 |
| D-99 | 决策冲突补充论证 | 产品 | R-23 |
| D-100 | UI 预留忘记密码占位 | 产品 | R-24 |
