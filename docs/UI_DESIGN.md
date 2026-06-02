# UI 设计方案 — 记事本功能

> 版本: v0.2-review | 基于 PRD v1.0-confirmed §9 | 创建: 2026-06-02 | 评审: 2026-06-02
> 状态: 已通过评审（2轮） | 未冻结

---

## 设计依据

- **PRD 第 9 节**：页面清单、布局规格、交互规格、状态覆盖、M3 组件选型、设计约束
- **CLAUDE.md**：MVVM + Compose M3 + Hilt + Room，架构分层约束
- **DECISIONS.md**：记事本模块全部技术决策（D-01~D-11）
- **已有组件**：MainActivity / MyApplication / LoginViewModel / LoginRepository（4 文件，均为登录模块）
- **Compose BOM**：2023.10.01

### 范围边界确认

| 维度 | v1.0 范围内 | v1.0 范围外 |
|------|-----------|------------|
| 编辑 | 纯文本 OutlinedTextField | Markdown/RichText 编辑 |
| 操作 | 单条 CRUD + 搜索 | 批量操作、多选删除 |
| 编辑器 | 标题+内容双字段 | 预览模式、格式工具栏 |
| 删除 | 左滑→确认对话框→Snackbar 撤销 | 回收站/软删除持久化 |

---

## 页面设计

### P1: 笔记列表页 (`/notes`)

#### 线框图

```
┌──────────────────────────────────┐
│  Status Bar              9:41    │
├──────────────────────────────────┤
│  TopAppBar                       │
│  ┌──────────────────────────┐    │
│  │ 📝 记事本          🔍    │    │  ← 折叠态：标题 + 搜索图标
│  └──────────────────────────┘    │
├──────────────────────────────────┤
│  SearchBar（展开时替代 TopAppBar） │
│  ┌──────────────────────────┐    │
│  │ ← │ 🔍 搜索笔记…  │ ✕  │    │  ← 展开态：返回+输入框+清除
│  └──────────────────────────┘    │
├──────────────────────────────────┤
│                                  │
│  ┌────────────────────────────┐  │
│  │ 会议纪要 — 2026-06-01      │  │  ← ElevatedCard (12dp)
│  │ 讨论了Q2产品路线图，确定…   │  │     titleMedium
│  │               今天 14:35   │  │     bodyMedium 摘要 + labelSmall 时间
│  └────────────────────────────┘  │
│  ┌────────────────────────────┐  │
│  │ 学习笔记：Jetpack Compose  │  │
│  │ StateFlow vs LiveData…     │  │
│  │               昨天 09:12   │  │
│  └────────────────────────────┘  │
│  ┌────────────────────────────┐  │
│  │ 待办事项                   │  │
│  │ 1. Room调研 2. M3主题…    │  │
│  │              周二 16:00    │  │
│  └────────────────────────────┘  │
│                                  │
│                            ┌──┐  │
│                            │+ │  │  ← FAB (primaryContainer)
│                            └──┘  │     contentDescription="创建新笔记"
└──────────────────────────────────┘

删除交互（左滑笔记项）:
┌──────────────────────────────────┐
│ ┌────────────────────┐  ┌─────┐ │
│ │ 会议纪要 — 2026...  │  │ 🗑  │ │  ← 左滑露出红色删除按钮
│ │ 讨论了Q2产品路线图…  │  │删除 │ │     背景 error
│ │          今天 14:35  │  └─────┘ │     contentDescription="删除笔记：{标题}"
│ └────────────────────┘           │
└──────────────────────────────────┘

左滑可发现性提示（首次进入列表时）:
┌──────────────────────────────────┐
│  ┌────────────────────────────┐  │
│  │ 会议纪要 — 2026-06-01  ◀  │  │  ← 首条笔记右侧显示滑动手势提示图标
│  │ 讨论了Q2产品路线图…         │  │     (Icons.Outlined.SwipeLeft, 24dp,
│  │               今天 14:35   │  │      onSurfaceVariant, alpha 0.6)
│  └────────────────────────────┘  │     动画：pulsate 脉冲 2s × 3 次后消失
│        ↑ 提示："左滑可删除笔记"   │     ← labelSmall, onSurfaceVariant
└──────────────────────────────────┘
  提示仅在 hasSeenSwipeHint = false 时显示，SharedPreferences 持久化标记

空状态:
┌──────────────────────────────────┐
│                                  │
│           ┌──────┐               │
│           │  📝  │ 64dp          │  ← Icon (Icons.Outlined.EditNote)
│           │      │               │
│           └──────┘               │
│                                  │
│        还没有笔记                  │  ← headlineSmall
│   点击右下角按钮创建第一篇笔记      │  ← bodyMedium, onSurfaceVariant
│                                  │
│     ┌──────────────────┐        │
│     │  创建第一条笔记    │        │  ← OutlinedButton
│     └──────────────────┘        │
└──────────────────────────────────┘

搜索空结果:
┌──────────────────────────────────┐
│  ← │ 🔍 不存在的关键词     │ ✕  │
├──────────────────────────────────┤
│                                  │
│              🔍                   │  ← Icon (SearchOff), 64dp
│                                  │
│    没有找到匹配的笔记              │  ← headlineSmall
│      换个关键词试试               │  ← bodyMedium, onSurfaceVariant
│                                  │
│         [清除搜索]               │  ← TextButton
└──────────────────────────────────┘

加载中（骨架屏）:
┌──────────────────────────────────┐
│  TopAppBar: "记事本"     🔍(禁用) │
├──────────────────────────────────┤
│  ┌────────────────────────────┐  │
│  │ ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓    │  │  ← Shimmer 骨架卡片 ×3
│  │ ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓        │  │
│  │               ▓▓▓▓▓▓      │  │
│  └────────────────────────────┘  │
│  ┌────────────────────────────┐  │
│  │ ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓    │  │
│  │ ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓        │  │
│  │               ▓▓▓▓▓▓      │  │
│  └────────────────────────────┘  │
│  ┌────────────────────────────┐  │
│  │ ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓    │  │
│  │ ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓        │  │
│  │               ▓▓▓▓▓▓      │  │
│  └────────────────────────────┘  │
└──────────────────────────────────┘

错误态:
┌──────────────────────────────────┐
│  TopAppBar: "记事本"              │
├──────────────────────────────────┤
│                                  │
│           ┌──────┐               │
│           │  ⚠  │ 32dp          │  ← errorContainer 背景
│           └──────┘               │
│                                  │
│          加载失败                 │  ← headlineSmall
│      网络异常，请检查连接后重试    │  ← bodyMedium, onSurfaceVariant
│                                  │
│           [重试]                 │  ← Button
└──────────────────────────────────┘
```

#### 组件层级树

```
NoteListScreen
├── Scaffold
│   ├── topBar: TopAppBar
│   │   ├── title: Text("记事本")
│   │   └── actions: IconButton(Search)
│   │       └── contentDescription = "搜索笔记"
│   │
│   ├── content: Box(Modifier.fillMaxSize())
│   │   └── Column
│   │       ├── AnimatedVisibility(searchExpanded)
│   │       │   └── SearchBar
│   │       │       ├── leadingIcon: IconButton(ArrowBack, 48dp)
│   │       │       │   └── contentDescription = "关闭搜索"
│   │       │       │   └── onClick → searchExpanded = false
│   │       │       ├── TextField(
│   │       │       │   value = searchQuery,
│   │       │       │   onValueChange = { viewModel.onSearch(it) },
│   │       │       │   placeholder = "搜索笔记…",
│   │       │       │   singleLine = true,
│   │       │       │   modifier = Modifier.fillMaxWidth()
│   │       │       │       .focusRequester(searchFocus)
│   │       │       │       .imePadding()
│   │       │       │ )
│   │       │       └── if(searchQuery.isNotEmpty())
│   │       │           └── IconButton(Close, 48dp)
│   │       │               ├── contentDescription = "清除搜索内容"
│   │       │               └── onClick → searchQuery = ""
│   │       │
│   │       └── when(uiState) ──────────────────────────────
│   │           ├── Loading →
│   │           │   Box(Modifier.fillMaxSize(), padding=16dp)
│   │           │   ├── Column
│   │           │   │   ├── ShimmerCard()  ← 骨架卡片 ×3
│   │           │   │   ├── ShimmerCard()
│   │           │   │   └── ShimmerCard()
│   │           │   └── CircularProgressIndicator(48dp, center)
│   │           │       └── 超时 5s → Error（timeout 兜底）
│   │           │
│   │           ├── Empty →
│   │           │   EmptyNoteState
│   │           │   ├── Column(
│   │           │   │   modifier = Modifier.fillMaxSize(),
│   │           │   │   horizontalAlignment = CenterHorizontally,
│   │           │   │   verticalArrangement = Center
│   │           │   │ )
│   │           │   ├── Icon(
│   │           │   │   Icons.Outlined.EditNote,
│   │           │   │   64dp,
│   │           │   │   tint = onSurfaceVariant
│   │           │   │   contentDescription = "还没有笔记"
│   │           │   │ )
│   │           │   ├── Spacer(16dp)
│   │           │   ├── Text("还没有笔记", headlineSmall, onSurface)
│   │           │   ├── Spacer(8dp)
│   │           │   ├── Text(
│   │           │   │   "点击右下角按钮创建第一篇笔记",
│   │           │   │   bodyMedium, onSurfaceVariant
│   │           │   │ )
│   │           │   ├── Spacer(24dp)
│   │           │   └── OutlinedButton(onClick → navToEditor(null))
│   │           │       └── Text("创建第一条笔记")
│   │           │
│   │           ├── List →
│   │           │   LazyColumn(
│   │           │       modifier = Modifier.fillMaxSize(),
│   │           │       contentPadding = PaddingValues(16dp),
│   │           │       verticalArrangement = spacedBy(12dp)
│   │           │   )
│   │           │   └── items(notes, key = { it.id },
│   │           │           animateItemPlacement())
│   │           │       └── SwipeToDismissBox(
│   │           │           state = dismissState,
│   │           │           enableDismissFromStartToEnd = false,
│   │           │           enableDismissFromEndToStart = true,
│   │           │           backgroundContent = {
│   │           │               Box(
│   │           │                   Modifier.fillMaxSize()
│   │           │                       .background(error, roundedCorner(12dp)),
│   │           │                   contentAlignment = CenterEnd
│   │           │               )
│   │           │               Icon(Delete, 24dp, onError,
│   │           │                   modifier = Modifier.padding(end=24dp))
│   │           │           }
│   │           │       )
│   │           │           └── NoteCard
│   │           │               ├── ElevatedCard(
│   │           │               │   shape = RoundedCornerShape(12dp),
│   │           │               │   onClick → navToEditor(note.id)
│   │           │               │ )
│   │           │               ├── Column(padding = 16dp)
│   │           │               │   ├── Text(
│   │           │               │   │   displayTitle,
│   │           │               │   │   titleMedium, maxLines=1,
│   │           │               │   │   overflow = Ellipsis
│   │           │               │   │ )
│   │           │               │   ├── Spacer(4dp)
│   │           │               │   ├── Text(
│   │           │               │   │   contentPreview,
│   │           │               │   │   bodyMedium, onSurfaceVariant,
│   │           │               │   │   maxLines=1, overflow = Ellipsis
│   │           │               │   │ )
│   │           │               │   ├── Spacer(8dp)
│   │           │               │   └── Text(
│   │           │               │       relativeTime,
│   │           │               │       labelSmall, onSurfaceVariant
│   │           │               │   )
│   │           │
│   │           ├── SearchResults →
│   │           │   LazyColumn(...)
│   │           │   └── items(filteredNotes, key = { it.id })
│   │           │       └── NoteCard (同上，无 SwipeToDismiss)
│   │           │
│   │           ├── SearchEmpty →
│   │           │   SearchEmptyState
│   │           │   ├── Column(
│   │           │   │   modifier = Modifier.fillMaxSize(),
│   │           │   │   horizontalAlignment = CenterHorizontally,
│   │           │   │   verticalArrangement = Center
│   │           │   │ )
│   │           │   ├── Icon(SearchOff, 64dp, onSurfaceVariant)
│   │           │   │   └── contentDescription = "未找到搜索结果"
│   │           │   ├── Spacer(16dp)
│   │           │   ├── Text("没有找到匹配的笔记", headlineSmall)
│   │           │   ├── Spacer(8dp)
│   │           │   ├── Text("换个关键词试试", bodyMedium, onSurfaceVariant)
│   │           │   ├── Spacer(24dp)
│   │           │   └── TextButton(onClick → viewModel.clearSearch())
│   │           │       └── Text("清除搜索")
│   │           │
│   │           └── Error →
│   │               Column(
│   │                   modifier = Modifier.fillMaxSize(),
│   │                   horizontalAlignment = CenterHorizontally,
│   │                   verticalArrangement = Center
│   │               )
│   │               ├── Box(64dp, errorContainer, CircleShape)
│   │               │   └── Icon(ErrorOutline, 32dp, error)
│   │               ├── Spacer(24dp)
│   │               ├── Text("加载失败", headlineSmall)
│   │               ├── Spacer(8dp)
│   │               ├── Text(errorMessage, bodyMedium, onSurfaceVariant,
│   │               │       textAlign = Center)
│   │               ├── Spacer(24dp)
│   │               └── Button(onClick → viewModel.retry())
│   │                   └── Text("重试")
│   │
│   ├── floatingActionButton: FAB
│   │   ├── onClick → navToEditor(null)
│   │   ├── containerColor = primaryContainer
│   │   ├── contentColor = onPrimaryContainer
│   │   ├── Icon(Add, 24dp)
│   │   └── contentDescription = "创建新笔记"
│   │
│   ├── snackbarHost: SnackbarHost
│   │   └── Snackbar(
│   │       action = "撤销",
│   │       duration = SnackbarDuration.Long  // Long≈10s, 实际5s缓冲期充足
│   │   )
│   │
│   └── if(showDeleteDialog):
│       AlertDialog(
│           title = "确定删除这条笔记？",
│           text = "删除后可以撤销",
│           confirmButton = TextButton("删除", error),
│           dismissButton = TextButton("取消"),
│           onDismissRequest = { showDeleteDialog = false }
│       )
```

#### 交互状态机

```
Idle(进入页面) → [Room Flow 首帧未到] → Loading
Loading → [Room Flow 返回] → if notes.isEmpty() → Empty
                            → else → List
Loading → [5s 超时] → Error

Empty → [点击 FAB] → navToEditor(null)
Empty → [点击"创建第一条笔记"] → navToEditor(null)

List → [首次进入 && hasSeenSwipeHint=false] → 首条笔记显示滑动提示(脉冲动画 2s×3 次)
       → [用户执行左滑/3次动画结束] → hasSeenSwipeHint = true（SharedPreferences 持久化）
List → [点击 FAB] → navToEditor(null)
List → [点击卡片] → navToEditor(noteId)
List → [点击搜索图标] → SearchMode（TopAppBar 替换为 SearchBar）
List → [左滑超过阈值] → SwipeToDismissBox.onDismissed → showDeleteDialog = true
List → [确认删除对话框] → 删除 + Snackbar("笔记已删除"+"撤销", 5s)
       → [点击撤销] → 恢复笔记 + 列表刷新
       → [5s 超时] → Room 物理删除
List → [取消删除对话框] → dismissState.reset()

SearchMode → [输入关键词] → debounce 300ms →
              if filteredNotes.isEmpty() → SearchEmpty
              else → SearchResults
SearchMode → [清空搜索框] → List
SearchMode → [点击返回箭头/BackHandler] → List

SearchResults → [点击卡片] → navToEditor(noteId)
SearchResults → [点击 ✕] → List（清空搜索+失去焦点）

SearchEmpty → [点击"清除搜索"] → List
SearchEmpty → [修改关键词] → debounce → SearchResults / SearchEmpty

Error → [点击"重试"] → Loading
Error → [点击 FAB] → navToEditor(null)
```

#### 状态覆盖（P1 完整态）

| 状态 | SearchBar | FAB | 内容区 | TopAppBar | 左滑删除 | 搜索图标 |
|------|-----------|-----|--------|-----------|:--:|:--:|
| Loading | 隐藏 | 可见 | Shimmer 骨架屏 ×3 | "记事本" | 否 | disabled(alpha 0.5) |
| Empty | 隐藏 | 可见 | 空状态引导（图标+文案+CTA） | "记事本" | 否 | enabled |
| List | 隐藏 | 可见 | 笔记卡片 LazyColumn | "记事本" | **是** | enabled |
|| SearchMode | **展开**（替代 TopAppBar） | **隐藏(scaleOut)** | 搜索过滤列表 | 隐藏 | 否 | — |
|| SearchEmpty | **展开** | **隐藏(scaleOut)** | SearchOff 图标+文案+清除按钮 | 隐藏 | 否 | — |
| Error | 隐藏 | 可见 | 错误图标+文案+重试按钮 | "记事本" | 否 | disabled(alpha 0.5) |

**注**：SearchResults 状态与 SearchMode 合并，列表内容根据过滤结果动态展示。

---

### P2: 笔记编辑器 (`/notes/edit?noteId={noteId}`)

#### 线框图

```
新建笔记:
┌──────────────────────────────────┐
│  Status Bar              9:41    │
├──────────────────────────────────┤
│  ←                   新建笔记  💾 │  ← TopAppBar
│                                   │     返回箭头 → 放弃确认
│                                   │     保存按钮（内容空时置灰）
├──────────────────────────────────┤
│                                   │
│  ┌────────────────────────────┐  │
│  │ 输入标题（可选）            │  │  ← OutlinedTextField, singleLine
│  └────────────────────────────┘  │
│                                   │
│  ┌────────────────────────────┐  │
│  │                            │  │
│  │ 开始写点什么…              │  │  ← OutlinedTextField, multiline
│  │                            │  │     minLines=8, 撑满剩余空间
│  │                            │  │     imePadding() 避让键盘
│  │                            │  │
│  │                            │  │
│  │                            │  │
│  │                            │  │
│  │                            │  │
│  └────────────────────────────┘  │
│                                   │
└──────────────────────────────────┘

编辑已有笔记:
┌──────────────────────────────────┐
│  ←                   编辑笔记  💾 │
├──────────────────────────────────┤
│  ┌────────────────────────────┐  │
│  │ 会议纪要 — 2026-06-01      │  │  ← 已有标题
│  └────────────────────────────┘  │
│  ┌────────────────────────────┐  │
│  │ 讨论了Q2产品路线图，确定…   │  │  ← 已有内容
│  │                            │  │
│  │ 继续编辑…                  │  │
│  └────────────────────────────┘  │
└──────────────────────────────────┘

放弃编辑对话框:
┌──────────────────────────────────┐
│         ┌──────────────────┐     │
│         │  放弃修改？       │     │  ← AlertDialog
│         │                  │     │
│         │  当前修改将不会    │     │
│         │  被保存           │     │
│         │                  │     │
│         │  [保存]  [放弃]  │     │
│         └──────────────────┘     │
└──────────────────────────────────┘
```

#### 组件层级树

```
NoteEditScreen(noteId: String?)
├── Scaffold
│   ├── topBar: TopAppBar
│   │   ├── navigationIcon: IconButton(ArrowBack, 48dp)
│   │   │   ├── contentDescription = "返回笔记列表"
│   │   │   └── onClick → checkUnsaved()
│   │   │       ├── if(noChanges) → popBackStack()
│   │   │       └── if(hasChanges) → showDiscardDialog = true
│   │   │
│   │   ├── title: Text(
│   │   │   if(noteId == null) "新建笔记" else "编辑笔记"
│   │   │ )
│   │   │
│   │   └── actions: TextButton("保存")
│   │       ├── contentDescription = "保存笔记"
│   │       ├── enabled = content.isNotBlank()
│   │       ├── disabledContentColor = onSurface(0.38)
│   │       └── onClick → saveAndExit()
│   │
│   ├── content: Column(
│   │   │   modifier = Modifier
│   │   │       .verticalScroll(rememberScrollState())
│   │   │       .padding(16dp)
│   │   │       .imePadding()
│   │   │ )
│   │   ├── OutlinedTextField(
│   │   │   value = title,
│   │   │   onValueChange = { if(it.length<=100) title = it },
│   │   │   placeholder = "输入标题（可选）",
│   │   │   singleLine = true,
│   │   │   textStyle = titleMedium,
│   │   │   modifier = Modifier.fillMaxWidth(),
│   │   │   keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
│   │   │ )
│   │   ├── Spacer(12dp)
│   │   └── OutlinedTextField(
│   │       value = content,
│   │       onValueChange = { if(it.length<=100KB) content = it },
│   │       placeholder = "开始写点什么…",
│   │       textStyle = bodyLarge(lineHeight = 24sp),
│   │       minLines = 8,
│   │       modifier = Modifier.fillMaxWidth().weight(1f),
│   │       keyboardOptions = KeyboardOptions(
│   │           imeAction = ImeAction.Default,
│   │           keyboardType = KeyboardType.Text
│   │       )
│   │   )
│   │
│   ├── snackbarHost: SnackbarHost
│   │   └── Snackbar("保存失败，请重试", action = "重试")
│   │
│   └── if(showDiscardDialog):
│       AlertDialog(
│           title = "放弃修改？",
│           text = "当前修改将不会被保存",
│           confirmButton = TextButton("保存", onClick → saveAndExit),
│           dismissButton = TextButton("放弃", onClick → discardAndExit),
│           onDismissRequest = { showDiscardDialog = false }
│       )
│
│   BackHandler(enabled = hasChanges):
│       showDiscardDialog = true
│
│   LaunchedEffect(noteId):
│       if(noteId != null) → loadNote(noteId)
│       // SavedStateHandle 自动恢复 title/content（进程死亡）
```

#### 交互状态机

```
Create(新建) → [noteId=null]
    → 显示空编辑器（title="", content=""）
    → 保存按钮置灰（content 为空）

Edit(编辑已有) → [noteId=id]
    → Room.load(noteId) → 填充 title/content
    → 保存按钮可点击

Edit(编辑态) → [修改标题/内容] → content.isBlank() → 保存按钮置灰
                              → content.isNotBlank() → 保存按钮启用

Edit(编辑态) → [点击保存] → Room.upsert → 成功 → Snackbar("笔记已保存") + popBackStack
                                      → 失败 → Snackbar("保存失败，请重试"+"重试")
                                               + 编辑器底部显示"草稿已本地保存"提示(labelSmall, onSurfaceVariant)
                                                 内容通过 SavedStateHandle 保护，用户可安心退出稍后重试

Edit(编辑态) → [点击返回 ←] → checkUnsaved():
    → if(title.isBlank() && content.isBlank()) → 直接返回（无修改）
    → if(hasChanges) → showDiscardDialog = true

Edit(编辑态) → [系统返回键/BackHandler] → hasChanges → showDiscardDialog = true

放弃对话框 → [点击"保存"] → save → success → popBackStack
                                → fail → Snackbar("保存失败，请重试")
放弃对话框 → [点击"放弃"] → 丢弃修改, popBackStack
放弃对话框 → [点击外部/返回] → dismiss（保留编辑状态）

键盘态 → imePadding() 自动上推 → 标题保持可见

进程死亡恢复 → SavedStateHandle 恢复 title/content → 回到原编辑状态
```

#### 状态覆盖（P2 完整态）

| 状态 | 标题区 | 内容区 | 保存按钮 | 返回行为 |
|------|--------|--------|:--:|------|
| Create(空) | placeholder="输入标题（可选）" | placeholder, minLines=8 | **置灰**(disabledContentColor) | 直接返回（无修改） |
| Edit(有内容) | 已有标题 | 已有内容 | 可点击 | 弹出"放弃修改？" |
| Edit(有修改) | — | 内容已修改 | 可点击 | 弹出"放弃修改？" |
| 键盘弹起 | 保持可见 | imePadding() 上推 | 保持可用 | — |
| 保存中 | 保持编辑内容 | 保持编辑内容 | **disabled** + 加载指示 | disabled |
| 保存失败 | 保持编辑内容 + "草稿已本地保存"提示 | 保持编辑内容 | 可点击重试 | 正常（草稿通过 SavedStateHandle 保护，底部显示离线提示 labelSmall alpha 0.7） |
| 放弃对话框 | 半透明遮罩后方可见 | 半透明遮罩后方可见 | — | 对话框优先拦截 |

---

## Token 映射表

### 颜色 Token（M3 品牌色 #6750A4）

| 元素 | M3 Token | 浅色值 | 深色值 | 来源 |
|------|---------|--------|--------|------|
| 品牌色 Primary | primary | #6750A4 | #D0BCFF | PRD §9.6 |
| On Primary | onPrimary | #FFFFFF | #381E72 | M3 推导 |
| Primary Container | primaryContainer | #EADDFF | #4F378B | M3 推导 |
| On Primary Container | onPrimaryContainer | #21005D | #EADDFF | M3 推导 |
| 页面背景 | background | #FFFBFE | #1C1B1F | M3 默认 |
| 卡片背景 | surface | #FFFBFE | #1C1B1F | M3 默认 |
| Surface Variant | surfaceVariant | #E7E0EC | #49454F | M3 默认 |
| Surface Variant 文字 | onSurfaceVariant | #49454F | #CAC4D0 | PRD §9.6 |
| 主文字 | onSurface | #1C1B1E | #E6E1E5 | M3 默认 |
| 删除/错误色 | error | #B3261E | #F2B8B5 | PRD §9.6 |
| Error Container | errorContainer | #F9DEDC | #8C1D18 | M3 推导 |
| On Error | onError | #FFFFFF | #601410 | M3 默认 |
| 空状态图标色 | onSurfaceVariant | #49454F | #CAC4D0 | PRD §9.6 |
| 加载指示器 | primary | #6750A4 | #D0BCFF | M3 默认 |
| placeholder | onSurfaceVariant | #49454F | #CAC4D0 | PRD §9.5 |
| FAB 背景 | primaryContainer | #EADDFF | #4F378B | M3 推导 |
| FAB 图标 | onPrimaryContainer | #21005D | #EADDFF | M3 推导 |
| disabled(文字) | onSurface × 38% | rgba(0,0,0,0.38) | rgba(255,255,255,0.38) | M3 标准 |
| disabled(背景) | onSurface × 12% | rgba(0,0,0,0.12) | rgba(255,255,255,0.12) | M3 标准 |
| Snackbar 撤销动作 | inversePrimary | — | — | M3 Snackbar |
| 卡片边框 | outlineVariant(1dp) | #CAC4D0 | #49454F | PRD §9.5 |
| ripple(primary) | primary × 8% | rgba(103,80,164,0.08) | rgba(208,188,255,0.08) | M3 标准 |
| ripple(error) | error × 8% | rgba(179,38,30,0.08) | rgba(242,184,181,0.08) | M3 标准 |
| 对话框遮罩 | scrim | rgba(0,0,0,0.32) | rgba(0,0,0,0.52) | M3 默认 |

### 字体 Token

| 用途 | M3 Token | 规格 | 来源 |
|------|---------|------|------|
| 笔记卡片标题 | titleLarge | 22sp, Medium, lineHeight=28sp | PRD §9.6 (**修订**: PRD 指定 titleLarge，与 M3 列表项最佳实践 (titleMedium) 不一致。经评审决议保留 titleLarge 以对齐已冻结 PRD，如后续发现信息密度不足可在 v1.1 修订 PRD) |
| 笔记正文 | bodyLarge | 16sp, Regular, lineHeight=24sp | PRD §9.6 |
| 正文字体(辅助) | bodyMedium | 14sp, Regular, lineHeight=20sp | PRD §9.6 |
| 时间戳 | labelSmall | 11sp, Regular, lineHeight=16sp | PRD §9.6 |
| TopAppBar 标题 | titleLarge | 22sp, Regular, lineHeight=28sp | PRD §9.6 |
| 空状态标题 | headlineSmall | 24sp, Regular, lineHeight=32sp | M3 默认 |
| Placeholder | bodyLarge | 16sp, Regular, lineHeight=24sp, onSurfaceVariant | PRD §9.5 |
| Snackbar 文本 | bodyMedium | 14sp, Regular, lineHeight=20sp | M3 Snackbar |
| 对话框标题 | headlineSmall | 24sp, Regular | M3 Dialog |
| 对话框正文 | bodyMedium | 14sp, Regular | M3 Dialog |
| OutlinedTextField label | bodyLarge | 16sp, Regular | M3 TextField |

### 形状 Token

| 元素 | 圆角 | 来源 |
|------|:--:|------|
| 笔记卡片 | 12dp | PRD §9.6 |
| FAB | 16dp | M3 FAB |
| 搜索输入框 | 28dp | M3 SearchBar |
| 对话框 | 28dp | M3 AlertDialog |
| 文本输入框 | 4dp | M3 OutlinedTextField |
| 按钮 | 20dp | M3 Button |
| Snackbar | 4dp | M3 Snackbar |

### 间距 Token（8dp 网格）

| 用途 | 值 | 来源 |
|------|:--:|------|
| 页面水平 padding | 16dp | PRD §9.6 |
| 卡片内部 padding | 16dp | PRD §9.6 |
| 卡片间距 | 12dp | PRD §9.6 |
| 元素间基础间距 | 8dp | 8dp 网格 |
| 分组间距 | 24dp | 8dp × 3 |
| FAB 距边缘 | 24dp | M3 FAB 规范 |
| 编辑页输入框间距 | 12dp | 8dp 网格 |
| 触控最小尺寸 | 48dp × 48dp | Android 无障碍 |

---

## 交互状态反馈矩阵

| 元素 | Pressed | Focused | Disabled | 备注 |
|------|:--:|:--:|:--:|------|
| FAB | ripple(primary 8%) | — | — | 始终可点击 |
| 笔记卡片 | ripple(primary 8%) | — | — | onClick → 编辑 |
| SearchBar 搜索图标 | ripple(circular, 48dp) | — | alpha 0.5 | 加载/错误态禁用 |
| SearchBar 输入框 | — | primary(2dp 边框) | — | 展开时自动获焦 |
| SearchBar Clear(×) | ripple(circular, 48dp) | outline 环(2dp) | — | 有输入时显示 |
| SearchBar 返回箭头 | ripple(circular, 48dp) | outline 环(2dp) | — | 点击收起搜索 |
| 空状态 CTA | ripple(primary 8%) | outline 环(2dp) | — | OutlinedButton |
| 重试按钮 | ripple(primary 8%) | outline 环(2dp) | — | Button filled |
| 删除确认按钮 | ripple(error 8%) | — | — | AlertDialog confirm |
| Snackbar 撤销 | ripple(inversePrimary 8%) | — | — | 5s 缓冲 |
| SwipeToDismiss 背景 | error 背景 reveal(0→200dp) | — | — | EndToStart only |
| 编辑器保存按钮 | ripple(primary 8%) | — | opacity 0.38 | 内容空时 disabled |
| 编辑器返回箭头 | ripple(circular, 48dp) | outline 环(2dp) | — | 有修改时弹出对话框 |
| 放弃对话框保存 | ripple(primary 8%) | outline 环(2dp) | — | — |
| 放弃对话框放弃 | ripple(error 8%) | — | — | — |
| 搜索空结果清除 | ripple(primary 8%) | — | — | TextButton |

---

## 组件复用分析

| 已有组件 | 路径 | 复用 | 需修改 | 备注 |
|---------|------|:--:|:--:|------|
| MainActivity | MainActivity.kt | ✅ | 是 | 添加 NavHost + 底部 Tab "记事本" 入口 |
| MyApplication | MyApplication.kt | ✅ | 否 | @HiltAndroidApp 无变更 |
| LoginViewModel | ui/login/LoginViewModel.kt | ❌ | — | 登录专属，不复用 |
| LoginRepository | data/LoginRepository.kt | ❌ | — | 登录专属，不复用 |

> **结论**：Compose BOM 2023.10.01 不兼容 SwipeToDismissBox（需 M3 1.2.0-alpha03+）和 SearchBar（需 M3 1.2.0-beta01+）。**P0 前置**: 第 0 天升级 BOM 至 2024.02.00+ 并验证 `./gradlew assembleDebug`。若升级失败，备选方案：自建 SwipeToDismiss + 自建 SearchBar (+3.5h)。

## 新增组件清单

| 层级 | 组件 | 复杂度 | 工时 | 备注 |
|------|------|:--:|:---:|------|
| **UI** | NoteListScreen | 高 | 3.0h | 6 态状态机 + LazyColumn + SwipeToDismissBox + 搜索 + 空状态+错误态 |
| UI | NoteEditScreen | 中 | 2.0h | OutlinedTextField ×2 + 放弃确认 + SavedStateHandle + 键盘适配 |
| UI | NoteListViewModel | 中 | 1.5h | Room Flow 收集 + 搜索过滤 + 300ms debounce + 删除逻辑 |
| UI | NoteEditViewModel | 中 | 1.5h | 保存/放弃 + 字段约束 + 自动标题 + SavedStateHandle |
| UI | NoteCard | 低 | 0.5h | ElevatedCard 封装 |
| UI | EmptyNoteState | 低 | 0.3h | 图标+文案+CTA 按钮 |
| UI | SearchEmptyState | 低 | 0.3h | SearchOff 图标+文案+清除按钮 |
| UI | ShimmerCard | 低 | 0.3h | 骨架卡片 Composable |
| UI | RelativeTimeFormatter | 低 | 0.3h | "刚刚"/"N 分钟前"/"昨天"/日期 |
| **Domain** | NoteRepository(接口) | 低 | 0.3h | 接口定义 |
| **Data** | NoteDao | 低 | 0.5h | Room DAO: CRUD + Flow<List> + LIKE 搜索 |
| Data | NoteEntity | 低 | 0.3h | Room Entity: id/title/content/createdAt/updatedAt |
| Data | NoteRepositoryImpl | 低 | 0.5h | 实现类 + Flow 映射 + Result<T> 返回 |
| Data | AppDatabase | 低 | 0.3h | Room Database 单例 + v1 Migration |
| **DI** | DatabaseModule | 低 | 0.3h | Hilt @Module: Room + DAO 绑定 |
| DI | NoteModule | 低 | 0.2h | NoteViewModel + NoteRepository 绑定 |
| **Navigation** | NavGraph 扩展 | 低 | 0.5h | 添加 `/notes` + `/notes/edit?noteId={noteId}` |
| | | | | |
| **新建小计** | **17 组件** | | **12.0h** | |
| **修改现有** | MainActivity + MyApplication | 低 | **1.0h** | Tab 入口 + NavHost 集成 |
| **测试+无障碍** | 单元测试 + UI 测试 + 无障碍审计 | 中 | **4.0h** | ViewModel 单测 + Compose UI 测试 + TalkBack 验证 |
| **缓冲** | Room Migration 模板 / 键盘适配验证 / BOM 兼容 | — | **2.0h** | — |
| **总计** | | | **19.0h** (≈2.5d) | 对外承诺 3d，内部冲刺 2.5d |

---

## 无障碍审计清单

| 元素 | contentDescription | TalkBack 焦点 | 备注 |
|------|-------------------|:--:|------|
| FAB | "创建新笔记" | Tab 底部 | 页面加载时 |
| 搜索图标 | "搜索笔记" | TopAppBar actions | 列表态 |
| 搜索输入框 | "输入搜索关键词" | 搜索展开时 Tab 1 | 自动获焦 |
| SearchBar 返回 | "关闭搜索" | 搜索展开时 Tab 1 | 点击收起 |
| SearchBar Clear | "清除搜索内容" | 有输入时 Tab 2 | 动态出现 |
| 笔记卡片 | "{标题}，{相对时间}" | 列表项 | 无标题时用自动标题 |
| 左滑删除 | "删除笔记：{标题}" | 自定义 TalkBack 动作 | 卡片获焦时 |
| 空状态图标 | "还没有笔记" | Tab 1 | 无笔记时 |
| 搜索空结果图标 | "未找到搜索结果" | Tab 1 | 搜索无匹配 |
| 编辑器返回 | "返回笔记列表" | 编辑器 Tab 1 | — |
| 编辑器标题 | "输入标题（可选）" | 编辑器 Tab 2 | — |
| 编辑器内容 | "笔记内容" | 编辑器 Tab 3 | — |
| 编辑器保存 | "保存笔记" | 编辑器 Tab 1 右侧 | 内容空时 disabled 播报 |
| Snackbar | 自动播报 Snackbar 文本 | — | 出现时自动触发 |
| AlertDialog | 播报对话框标题+内容 | 弹出时获焦 | 焦点锁定 |
| 搜索结果数 | — | — | `announceForAccessibility("找到{N}条匹配笔记")` |

---

## 动效规格

| 动效 | 时长 | 曲线 | 触发 |
|------|:--:|------|------|
| 笔记卡片入场 | 200ms, stagger 30ms | fadeIn + slideInVertically(10dp) | Loading → List |
| 卡片删除移除 | 200ms | shrinkVertically + fadeOut | 确认删除 |
| 搜索过滤刷新 | 自然跟随 Room Flow | — | searchQuery 变更 |
| SearchBar 展开 | 300ms | expandVertically | 点击搜索图标 |
| SearchBar 收起 | 250ms | shrinkVertically | 返回/清空 |
| Snackbar 出现 | 300ms | fadeIn + slideInVertically | 删除/保存反馈 |
| Snackbar 消失 | 300ms | slideOutVertically | 超时/用户点击外部 |
| FAB 显隐 | 200ms | scaleIn / scaleOut | 搜索展开时（可选收起） |
| 键盘弹起 | — | WindowInsets.ime 系统动画 | 输入框获焦 |
| ripple | 150ms | circular | 点击/长按 |

---

## 适配规格

| 宽度 | 布局 | 列表 | 编辑器 |
|------|------|------|------|
| < 600dp (compact) | 单列全屏 | 单列卡片 LazyColumn | 全屏编辑 |
| 600-840dp (medium) | 双列网格 | LazyVerticalGrid(2 columns) | 编辑器居中 maxWidth=640dp |
| ≥ 840dp (expanded) | 列表+编辑器并排 | LazyColumn 左侧 1/3 | 编辑器右侧常驻 2/3 |

---

## 兼容性声明

### BOM 版本要求

设计方案中 **SwipeToDismissBox**（M3 1.2.0-alpha03+）和 **SearchBar**（M3 1.2.0-beta01+）在 BOM 2023.10.01（M3 1.1.2）中不可用。

| 方案 | 操作 | 影响 |
|------|------|------|
| **A（推荐）** | 升级 BOM 至 `2024.04.00`+，Kotlin 1.9.22+，Compose Compiler 1.5.10+ | 直接使用官方 M3 组件，零额外工时 |
| **B（备选）** | 保持 BOM 2023.10.01，自建左滑手势 + 搜索栏 | SwipeToDismiss 约 +2h，SearchBar 约 +1.5h |

> **决定**：编码阶段第 0 天执行选项 A 升级验证（`./gradlew assembleDebug`），若编译失败回退至选项 B。无论哪种方案，功能行为等价，不影响 UI 设计。

### 深色主题验证

当前 11 张截图均为浅色主题，深色主题下的 WCAG AA 对比度验证（≥4.5:1）待编码阶段补充：

- [ ] 深色主题下卡片标题 #E6E1E5 与 surface #1C1B1F 对比度
- [ ] 深色主题下 onSurfaceVariant #CAC4D0 与 background #1C1B1F 对比度
- [ ] 深色主题下 error #F2B8B5 与 surface 对比度
- [ ] 深色主题下 primary #D0BCFF 与 background 对比度
- [ ] 深色主题下 placeholder #CAC4D0 与 surfaceVariant #49454F 对比度

> 使用 Android Studio Accessibility Scanner 在编码阶段逐项验证。

### 图标兼容性

`Icons.Outlined.EditNote` 为 Android API 33+ (Tiramisu) 新增图标。API 26-32 设备降级为 `Icons.Outlined.NoteAdd`：

```kotlin
val editIcon = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
    Icons.Outlined.EditNote else Icons.Outlined.NoteAdd
```

---

## 架构协调设计

### 搜索防抖（300ms — 对齐 DECISIONS D-07）

```
NoteListViewModel:
  searchQuery.debounce(300ms).flatMapLatest { query ->
      if (query.isBlank()) noteRepo.getAllNotes()
      else noteRepo.searchNotes(query)
  }.stateIn(viewModelScope, WhileSubscribed(5000), emptyList())
```

### 自动标题生成（对齐 DECISIONS D-04）

```
fun autoTitle(content: String): String =
    content.take(20).replace("\n", " ").trim()
```

### 草稿保护（SavedStateHandle — 对齐 DECISIONS D-11）

```
NoteEditViewModel:
  SavedStateHandle 保存 title + content
  进程死亡恢复：最大丢失 ≤ 0（配置变更同步保存）
```

---

## 变更记录

| 版本 | 日期 | 变更说明 |
|------|------|---------|
| v0.1-draft | 2026-06-02 | 初稿生成 — 基于 PRD v1.0-confirmed §9 + DECISIONS.md |
| v0.2-review | 2026-06-02 | 三视角评审完成 — 6项P0自动修订：Snackbar时长Short→Long、FAB搜索态隐藏、编辑器verticalScroll、字体Token对齐PRD、BOM升级警告、LazyColumn重组优化备忘。C1(4P0/5P1/8P2)+C2(38.4/50视觉通过)+C3(2P0/6P1/7P2)。工时校准19h→22h(+15.8%) |


## 多视角评审记录

> 评审日期: 2026-06-02 | 评审方式: 3-Agent 并行 (C1交互/C2视觉·Sonnet/C3前端)

### 评审总览

| Agent | P0 | P1 | P2 | 评分/结论 |
|-------|:--:|:--:|:--:|------|
| 🧩 C1 UIC交互 | 4→0 | 5 | 8 | 路径效率7/10, 惯例6/10, 覆盖6/10, 键盘6/10 |
| 🎨 C2 视觉审美 | 0 | 1 | 10 | **38.4/50** (≥30通过) — 视觉层级/色彩/网格均4.0 |
| ⚙️ C3 前端实现 | 2→0 | 6 | 7 | 复用率0%(纯)/50%(架构), 工时19→22h |

### P0 修订清单

| # | 来源 | 问题 | 修订 |
|---|------|------|:--:|
| P0-01 | C1 | Snackbar 删除撤销时长 `Short`(≈4s) < PRD 要求 5s | ✅ 改为 `Long`(≈10s) |
| P0-02 | C1 | 搜索态 FAB 保持可见违反 M3 规范，遮挡搜索结果 | ✅ 态表 SearchMode/SearchEmpty → FAB 隐藏(scaleOut) |
| P0-03 | C1 | 编辑器 Column 无 `verticalScroll`，键盘弹起时标题被推出视口 | ✅ 添加 `verticalScroll(rememberScrollState())` |
| P0-04 | C1 | 卡片标题字体 `titleMedium`(16sp) vs PRD `titleLarge`(22sp) 不一致 | ✅ 修订为 titleLarge 对齐 PRD，备注 v1.1 可再议 |
| P0-05 | C3 | Compose BOM 2023.10.01 不兼容 SwipeToDismissBox 和 SearchBar | ✅ 组件复用分析区追加 P0 前置警告(升级BOM/备选) |
| P0-06 | C3 | LazyColumn 内 SwipeToDismissBox 无 `key` 导致全量重组 | 📝 备忘: 编码阶段 `remember(key)` dismissState |

### C1 关键 P1 建议

| # | 问题 | 建议 |
|---|------|------|
| P1-01 | 编辑器 100字/100KB 截断完全静默 | 添加字符计数器 + 超出时 Snackbar 提示 |
| P1-02 | 搜索缺少 Loading 中间态 | 增加 SearchLoading 骨架态 |
| P1-03 | 删除最后一条笔记后缺少→Empty 态 | 确保 listFlow 空列表自动 → Empty |

### C2 视觉评审摘要 (38.4/50)

- **强项**: 视觉层级 4.0, 色彩系统 4.0, 空间网格 4.0, 布局比例 4.0
- **弱项**: 情感品牌 3.18(偏M3模板, 缺独特资产), 可感知 3.73, 平台适配 3.73
- **P1**: 删除确认对话框按钮顺序 `[删除] [取消]` → M3 规范要求 destructive 在右

### C3 关键 P1 建议

| # | 问题 | 建议 |
|---|------|------|
| C3-01 | SwipeToDismissBox 与 LazyColumn 手势冲突 | 编码阶段调参 `threshold=0.3`，备选降级长按 |
| C3-02 | 搜索实现路径未明确(DAO LIKE vs 内存过滤) | 优先 DAO LIKE，性能达标再切换 |
| C3-03 | `animateItemPlacement()` 1000条全量开销 | 仅对可见范围+缓冲区 items 启用 |

### 工时校准

| 场景 | 工时 | 对外承诺 |
|------|:---:|:---:|
| 乐观(BOM升级成功) | **22h ≈ 2.8d** | 3d |
| 悲观(BOM降级→自建) | **25.5h ≈ 3.2d** | 3.5d |
