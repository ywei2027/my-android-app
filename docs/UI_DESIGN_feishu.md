# 关于页面版本号显示 — UI 设计方案

> **版本:** v0.2-review | **C2 视觉:** 41/50 ✅

## 🔗 交互原型

👉 [在线预览原型](https://htmlpreview.github.io/?https://raw.githubusercontent.com/ywei2027/my-android-app/34579a5b6f2db7f83546024e5ea85e20dc84d065/docs/ui-preview/index.html)（375×812 手机模拟，支持浅色/深色切换）

## 📸 截图预览

| 页面 | 状态 | 截图 |
|------|------|------|
| 关于页面 | 默认态 | [查看](https://raw.githubusercontent.com/ywei2027/my-android-app/34579a5b6f2db7f83546024e5ea85e20dc84d065/docs/ui-preview/about_default.html) |

---
1|# 关于页面版本号显示 — UI 设计方案
2|
3|> **版本:** v0.2-review
4|> **功能名称:** 关于页面版本号显示
5|> **创建日期:** 2026-06-02
6|> **基于:** PRD v1.0-confirmed §9 | DECISIONS.md R2-D-22~R2-D-26 | C2 视觉 41/50
7|
8|---
9|
10|## §1 设计总览
11|
12|### 设计目标
13|- 登录页底部「关于」入口 → 导航至关于页面
14|- 关于页面展示应用图标、名称、版本号等基本信息
15|- 所有构建类型（Debug/Release）均可见
16|
17|### 设计语言
18|- Material3 (M3) 设计系统
19|- 375dp 基准视口
20|- 颜色 Token：`onSurfaceVariant` + alpha 0.6
21|
22|---
23|
24|## §2 页面清单与导航
25|
26|| 页面 | 路由 | 类型 | 入口 | 说明 |
27||------|------|------|------|------|
28|| 登录页（入口改造） | `/login` | 改造 | 启动默认 | 底部增加「关于」TextButton |
29|| 关于页面 | `/about` | 新建 | 登录页「关于」按钮 | 展示应用信息+版本号 |
30|
31|### 导航图
32|```
33|NavHost(startDestination = "login")
34|├── composable("login")  → LoginScreen（底部含「关于」按钮）
35|└── composable("about")  → AboutScreen（TopAppBar + 内容）
36|```
37|
38|---
39|
40|## §3 登录页入口改造
41|
42|### 线框图
43|```
44|┌──────────────────────────────────┐
45|│                                  │
46|│          📱 应用图标              │
47|│          应用名称                 │
48|│                                  │
49|│  ┌────────────────────────────┐  │
50|│  │  手机号                      │  │
51|│  └────────────────────────────┘  │
52|│  [        获取验证码        ]   │
53|│                                  │
54|│                                  │
55|│           关于                   │  ← TextButton, onSurfaceVariant
56|└──────────────────────────────────┘
57|```
58|
59|### 组件层级
60|```
61|LoginScreen (Column, fillMaxSize)
62|├── 应用图标 + 名称 (Column, center)
63|├── 手机号输入框 (TextField)
64|├── 获取验证码按钮 (Button)
65|└── 关于 (TextButton, Modifier.align(BottomCenter)
66|    .defaultMinSize(minHeight=48.dp, minWidth=48.dp))
67|    └── onClick → navController.navigate("about")
68|```
69|
70|---
71|
72|## §4 关于页面设计
73|
74|### 线框图
75|```
76|┌──────────────────────────────────┐
77|│  ←  关于                          │  ← CenterAlignedTopAppBar
78|├──────────────────────────────────┤
79|│                                  │
80|│           [应用图标]              │  ← 48×48dp, 圆角12dp
81|│          我的应用                 │  ← titleLarge 22sp
82|│          ─────────               │  ← HorizontalDivider
83|│          版本号                   │  ← label
84|│  v1.0.0(42)debug                 │  ← bodyMedium 14sp
85|│                                  │      onSurfaceVariant, alpha=0.6
86|│                                  │      maxWidth=240dp, maxLines=1
87|│ ← 底部 8dp + WindowInsets        │
88|└──────────────────────────────────┘
89|```
90|
91|### 组件层级树
92|```
93|AboutScreen
94|├── Scaffold
95|│   ├── topBar: CenterAlignedTopAppBar
96|│   │   ├── title: Text("关于")
97|│   │   └── navigationIcon: IconButton(ArrowBack)
98|│   │       └── onClick → navController.popBackStack()
99|│   └── content: Column (center, padding 16dp)
100|│       ├── Icon/Image  (48×48dp, rounded 12dp, contentDescription="应用图标")
101|│       ├── Spacer(16dp)
102|│       ├── Text(appName, titleLarge)
103|│       ├── Spacer(16dp)
104|│       ├── HorizontalDivider  (outlineVariant, importantForAccessibility=no)
105|│       ├── Spacer(16dp)
106|│       ├── Row("版本号" label)
107|│       └── Text(formatVersionTag(), bodyMedium,
108|│           color=onSurfaceVariant.copy(alpha=0.6f),
109|│           maxLines=1, overflow=Ellipsis,
110|│           modifier=Modifier.widthIn(max=240.dp)
111|│               .windowInsetsPadding(WindowInsets.navigationBars)
112|│               .padding(bottom=8.dp)
113|│               .semantics { contentDescription = formatVersionDescription() })
114|```
115|
116|---
117|
118|## §5 交互状态机
119|
120|```
121|┌─────────────┐
122|│    Idle     │ ← 进入页面
123|│  显示正常    │
124|└──────┬──────┘
125|       │ BuildConfig 异常
126|       ▼
127|┌─────────────┐
128|│  Fallback    │
129|│  版本未知    │ ← alpha=1.0, 不可点击,
130|│              │    contentDescription="版本信息暂时不可用"
131|└─────────────┘
132|```
133|
134|### 状态覆盖表
135|
136|| 状态 | 图标 | 名称 | 版本号 | 说明 |
137||------|------|------|--------|------|
138|| 默认 | ✅ 应用图标 | ✅ 应用名称 | ✅ v{name}({code}){buildType} | BuildConfig 正常 |
139|| 降级 | ✅ 应用图标 | ✅ 应用名称 | 版本未知 | BuildConfig 字段为空/null |
140|
141|---
142|
143|## §6 Token 映射表
144|
145|| 设计属性 | M3 Token | 值 |
146||----------|----------|-----|
147|| 页面背景 | `background` | light: #FFFBFE / dark: #1C1B1F |
148|| TopAppBar 标题色 | `onSurface` | light: #1C1B1F / dark: #E6E1E5 |
149|| TopAppBar 背景 | `surface` | light: #FFFBFE / dark: #1C1B1F |
150|| 标题文字色 | `onSurface` | light: #1C1B1F / dark: #E6E1E5 |
151|| 版本号文字色 | `onSurfaceVariant` × 0.6 | light: #49454F / dark: #CAC4D0 |
152|| 分隔线 | `outlineVariant` | light: #CAC4D0 / dark: #49454F |
153|| 返回箭头 | `onSurface` | light: #1C1B1F / dark: #E6E1E5 |
154|
155|---
156|
157|## §7 组件复用分析
158|
159|| 组件 | 来源 | 复用方式 | 状态 |
160||------|------|----------|------|
161|| `formatVersionTag()` | `ui/components/VersionTag.kt` | 直接调用 | ✅ 复用 |
162|| `formatVersionDescription()` | `ui/components/VersionTag.kt` | 直接调用 | ✅ 复用 |
163|| `VersionTag` Composable | `ui/components/VersionTag.kt` | ❌ 不复用 | Debug-only 限制与需求冲突 |
164|| `Scaffold` | material3 | 直接使用 | ✅ 标准组件 |
165|| `CenterAlignedTopAppBar` | material3 | 直接使用 | ✅ 标准组件 |
166|
167|### 新增文件
168|
169|| 文件 | 说明 | 复杂度 |
170||------|------|--------|
171|| `ui/about/AboutScreen.kt` | 关于页面 Composable | 低 |
172|| 导航修改 `MainActivity.kt` | 接入 NavHost | 中 |
173|
174|---
175|
176|## §8 架构协调设计
177|
178|### 导航事件
179|```
180|LoginScreen "关于" onClick → navController.navigate("about")
181|AboutScreen 返回箭头 → navController.popBackStack()
182|```
183|
184|### ViewModel（不需要）
185|关于页面为纯静态展示页面，无业务逻辑，不需要 ViewModel。
186|
187|### BackHandler
188|- 登录页：无特殊处理（默认行为）
189|- 关于页面：TopAppBar 返回箭头 + 系统返回手势 → popBackStack()
190|
191|---
192|
193|## §9 无障碍适配
194|
195|| 元素 | contentDescription | 触控目标 |
196||------|-------------------|----------|
197|| 返回按钮 | 「返回」 | ≥48dp |
198|| 应用图标 | 「应用图标」 | ≥48dp |
199|| 版本号 | 「应用版本号 v{name}，版本代码 {code}，构建类型 {buildType}」 | 静态文本 |
200|| 「关于」按钮 | 「关于此应用」 | ≥48dp |
201|
202|---
203|
204|## §10 前置依赖
205|
206|| 依赖 | 说明 | 状态 |
207||------|------|------|
208|| darkColorScheme | D-18 决议，需在 Theme.kt 中补建 | ⚠️ 编码阶段一并完成 |
209|| NavHost | navigation-compose 已引入，需在 MainActivity 接线 | ⚠️ 编码阶段一并完成 |
210|
211|---
212|
213|## §11 多视角评审记录
214|
215|> 评审日期: 2026-06-02 | 方式: delegate_task 三视角并行 | 耗时: ~120s
216|
217|### 评审总览
218|
219|| 视角 | 评分 | P0 | P1 | 核心发现 |
220||------|------|----|----|----------|
221|| C1 UX 交互 | 6/10 | 4 | 8 | 下边距/WindowInsets 缺失、maxWidth 未约束、Fallback 缺 contentDescription |
222|| C2 视觉审美 | 41/50 ✅ | 0 | 2 | 通过(≥40)，跨文件一致性 3/5 需关注 |
223|| C3 前端实现 | 4.2/5 | 2 | 4 | darkColorScheme+NavHost 前置依赖、工时 1.75-2h |
224|
225|### P0 修订记录（已在正文自动修订）
226|
227|| 编号 | 问题 | 修订内容 |
228||------|------|----------|
229|| P0-UX-01 | 下边距+WindowInsets 缺失 | §4 线框图+组件树增加 padding(bottom=8.dp)+windowInsetsPadding |
230|| P0-UX-02 | maxWidth 240dp 未约束 | §4 组件树增加 widthIn(max=240.dp) |
231|| P0-UX-03 | Fallback 缺 contentDescription | §5 状态机增加"版本信息暂时不可用" |
232|| P0-UX-04 | 按钮触摸目标未声明 | §3 组件树增加 defaultMinSize(48.dp) |
233|| P0-IMPL-01 | darkColorScheme 未建 | §10 前置依赖纳管，编码阶段补建 |
234|| P0-IMPL-02 | NavHost 未接线 | §10 前置依赖纳管，编码阶段一并实现 |
235|
236|### C2 视觉评审 10 维度
237|
238|| # | 维度 | 评分 | 关键评语 |
239||---|------|:---:|------|
240|| 1 | 格式塔 | 4 | 垂直流清晰，信息密度一致 |
241|| 2 | 视觉层级 | 4 | 四级递减合理(22sp→14sp→opacity 0.6) |
242|| 3 | 色彩 | 5 | M3 Token 完整，对比度 ≥3:1 ✅ |
243|| 4 | 字体 | 4 | M3 type scale + Noto Sans SC |
244|| 5 | 空间 | 4 | 48px padding-top, 8dp 底部网格对齐 |
245|| 6 | 布局 | 4 | 375×812 手机模拟，flex column 居中 |
246|| 7 | 可感知性 | 5 | aria-label 完整，≥48dp 触控 |
247|| 8 | 一致性 | 3 | 跨文件细节差异（字号/字重/Token 命名） |
248|| 9 | 情感品牌 | 4 | 紫色调专业温暖，品牌辨识度可提升 |
249|| 10 | 平台适配 | 4 | 深浅色+safe-area+窄屏 ✅ |
250|
251|**综合: 41/50** — 基于源码推断（快速通道无截图）
252|
253|---
254|
255|> **版本:** v0.2-review
256|> **状态:** 三视角评审完成，P0 已自动修订。请审阅后回复「确认」冻结进入技术方案。
257|