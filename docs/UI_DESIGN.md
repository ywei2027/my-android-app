# UI 设计方案 — 记事本功能

> 版本: v0.1-draft | 基于 PRD v1.0-confirmed | 创建: 2026-06-02
> 状态: 设计初稿（待评审） | 未冻结

---

## 设计依据

- **PRD 第 9 节**：页面清单（P1/P2）、布局规格、交互规格、组件选型、设计约束
- **CLAUDE.md**：MVVM + Compose M3 + Hilt + Room，架构分层约束
- **DECISIONS.md**：记事本模块全部技术决策（Room 存储/草稿三层防护/软删除/Markdown 渲染库/预览页架构）
- **项目 Theme**：无独立 Theme 文件（Color.kt/Type.kt/Theme.kt 均不存在）— 基于 PRD §9.5 设计约束自建 M3 Token 体系
- **已有组件**：MainActivity / MyApplication / LoginViewModel / LoginRepository（4 文件，均为登录模块）
- **Figma 参考**：无
- **Compose BOM**：2023.10.01

---

## 页面设计

### P1: 笔记列表页 (`/notes`)

#### 线框图

```
┌──────────────────────────────────────┐
│  Status Bar                     9:41 │
├──────────────────────────────────────┤
│ ┌──────────────────────────────────┐ │
│ │ 🔍 搜索笔记                       │ │  ← SearchBar (M3, EnterAlways)
│ └──────────────────────────────────┘ │     docked: surfaceVariant 容器
│                                       │     focused: surface + primary(2dp)
│                                       │     radius=28dp, padding=12dp_top+16dp_h
│                                       │     leadingIcon=Search(24dp)
├──────────────────────────────────────┤
│ ┌──────────────────────────────────┐ │
│ │ 会议纪要 — 2026-06-01            │ │  ← ElevatedCard (12dp)
│ │ 讨论了Q2产品路线图，确定了…       │ │     titleMedium + bodyMedium 摘要
│ │          今天 14:35              │ │     labelSmall + 相对时间
│ └──────────────────────────────────┘ │     left swipe → EndToStart 删除
│ ┌──────────────────────────────────┐ │
│ │ 学习笔记：Jetpack Compose        │ │
│ │ StateFlow vs LiveData 的对比…    │ │
│ │          昨天 09:12              │ │
│ └──────────────────────────────────┘ │
│ ┌──────────────────────────────────┐ │
│ │ 待办事项                         │ │
│ │ 1. Room 数据库调研 2. M3 主题…   │ │
│ │          周二 16:00              │ │
│ └──────────────────────────────────┘ │
│                                       │
│                                  ┌──┐ │
│                                  │+ │ │  ← FAB (primaryContainer, 16dp)
│                                  └──┘ │     contentDescription="创建新笔记"
└──────────────────────────────────────┘
```

#### 组件层级树

```
NoteListScreen
├── Scaffold
│   ├── topBar: TopAppBar (enterAlwaysCollapsed)
│   │   ├── title: Text("记事本")
│   │   └── actions: IconButton(Search) → scrollBehavior collapse
│   │
│   ├── content: Column
│   │   ├── SearchBar (docked, enterAlwaysCollapsed)
│   │   │   ├── leadingIcon: Icon(Search, 24dp, onSurfaceVariant)
│   │   │   ├── placeholder: Text("搜索笔记", onSurfaceVariant)
│   │   │   ├── trailingIcon: if(query.notEmpty) IconButton(Close, 48dp)
│   │   │   │   └── contentDescription = "清除搜索内容"
│   │   │   ├── modifier.imePadding()
│   │   │   └── onFocusChange → if(gained) expand search
│   │   │
│   │   └── when(uiState)
│   │       ├── Loading →
│   │       │   Box(Modifier.fillMaxSize(), contentAlignment=Center)
│   │       │   ├── CircularProgressIndicator(48dp, primary, strokeWidth=4dp)
│   │       │   └── Text("加载笔记中...", bodyMedium, onSurfaceVariant, top=24dp)
│   │       │   └── 超时 3s → Error + Snackbar
│   │       │
│   │       ├── Empty →
│   │       │   EmptyNoteState
│   │       │   ├── Column(horizontalAlignment=CenterHorizontally, fillMaxSize)
│   │       │   ├── Spacer(weight=1)
│   │       │   ├── Box(120dp, primaryContainer, CircleShape)
│   │       │   │   └── Icon(NoteAdd, 64dp, onPrimaryContainer)
│   │       │   ├── Spacer(24dp)
│   │       │   ├── Text("还没有笔记", headlineSmall, onSurface)
│   │       │   ├── Spacer(8dp)
│   │       │   ├── Text("点击右下角按钮创建第一篇笔记", bodyMedium, onSurfaceVariant)
│   │       │   ├── Spacer(weight=1)
│   │       │   └── FAB 引导箭头 (Canvas, primary, arrow+curve)
│   │       │
│   │       ├── List →
│   │       │   LazyColumn
│   │       │   └── items(notes, key={it.id}, animateItemPlacement)
│   │       │       └── SwipeToDismissBox(state, EndToStart,
│   │       │               enableDismissFromStartToEnd=false,
│   │       │               gestureStartEnd=24.dp)
│   │       │           ├── background: Box(constraintModifier=fillMaxSize)
│   │       │           │   └── Row(horizontalArrangement=End)
│   │       │           │       └── Icon(Delete, 24dp, contentColor=onError)
│   │       │           │           .background(error, roundedCorner(8dp))
│   │       │           └── ElevatedCard(12dp, clickable)
│   │       │               ├── Column(padding=16dp)
│   │       │               │   ├── Text(title, titleMedium, maxLines=1, overflow=Ellipsis)
│   │       │               │   ├── Spacer(4dp)
│   │       │               │   ├── Text(summary, bodyMedium, onSurfaceVariant,
│   │       │               │   │       maxLines=2, overflow=Ellipsis)
│   │       │               │   ├── Spacer(8dp)
│   │       │               │   └── Text(relativeTime, labelSmall, onSurfaceVariant)
│   │       │               └── modifier.combinedClickable:
│   │       │                   ├── onClick → navToEditor(noteId)
│   │       │                   └── onLongClick → haptic + enterMultiSelect(noteId)
│   │       │
│   │       ├── SearchResults →
│   │       │   Column
│   │       │   ├── Text("找到 {count} 条结果", labelMedium, onSurfaceVariant, 12dp_top+16dp_h)
│   │       │   └── LazyColumn
│   │       │       └── items(filteredNotes, key={it.id})
│   │       │           └── NoteCard (同 List 卡片，含 HighlightedText 高亮)
│   │       │
│   │       ├── SearchEmpty →
│   │       │   SearchEmptyState
│   │       │   ├── Column(horizontalAlignment=Center, fillMaxSize)
│   │       │   ├── Spacer(weight=1)
│   │       │   ├── Icon(SearchOff, 64dp, outline)
│   │       │   │   └── contentDescription = "未找到搜索结果"
│   │       │   ├── Spacer(16dp)
│   │       │   ├── Text("没有找到包含"{query}"的笔记", headlineSmall, textAlign=Center)
│   │       │   ├── Spacer(8dp)
│   │       │   ├── Text("换个关键词试试", bodyMedium, onSurfaceVariant)
│   │       │   ├── Spacer(24dp)
│   │       │   ├── OutlinedButton("清除搜索")
│   │       │   └── Spacer(weight=1)
│   │       │
│   │       ├── MultiSelect →
│   │       │   Column
│   │       │   ├── MultiSelectTopBar (替换原有 TopAppBar)
│   │       │   │   ├── IconButton(Close, contentDesc="退出多选模式")
│   │       │   │   ├── Text("已选 {count} 项", titleLarge)
│   │       │   │   └── TextButton("全选")
│   │       │   ├── LazyColumn
│   │       │   │   └── items(notes, key={it.id})
│   │       │   │       └── Row(verticalAlignment=CenterVertically)
│   │       │   │           ├── Checkbox(checked, onCheckedChange)
│   │       │   │           └── NoteCard (简化版，无 swipe)
│   │       │   └── BottomAppBar
│   │       │       └── Button("删除选中({count})", error)
│   │       │           └── onClick → AlertDialog 确认
│   │       │
│   │       └── Error →
│   │           Column(horizontalAlignment=Center, fillMaxSize, padding=16dp)
│   │           ├── Spacer(weight=1)
│   │           ├── Box(64dp, errorContainer, CircleShape)
│   │           │   └── Icon(ErrorOutline, 32dp, error)
│   │           ├── Spacer(24dp)
│   │           ├── Text("加载失败", headlineSmall)
│   │           ├── Spacer(8dp)
│   │           ├── Text(errorMessage, bodyMedium, onSurfaceVariant, textAlign=Center)
│   │           ├── Spacer(24dp)
│   │           ├── Button("重试")
│   │           └── Spacer(weight=1)
│   │
│   ├── floatingActionButton: FAB
│   │   ├── onClick → navToEditor(null)  // 新建
│   │   ├── containerColor = primaryContainer
│   │   ├── contentColor = onPrimaryContainer
│   │   ├── Icon(Add, 24dp)
│   │   ├── contentDescription = "创建新笔记"
│   │   ├── enabled = noteCount < 500
│   │   └── if disabled: alpha 0.38 + longClick Toast"已达上限"
│   │
│   ├── snackbarHost: SnackbarHost
│   │   └── Snackbar(action="撤销", duration=SnackbarDuration.Long+2s)
│   │
│   └── AlertDialog (if showDeleteDialog):
│       AlertDialog(
│           role = Role.AlertDialog,
│           title = "确定删除{N}条笔记？",
│           text = if N==1 "此操作可以撤销" else "可在8秒内撤销",
│           confirmButton = TextButton("删除", error),
│           dismissButton = TextButton("取消"),
│       )
```

#### 交互状态机

```
Idle(进入页面) → [Room Flow 首帧未到] → Loading
Loading → [3s 内收到 Flow] → if notes.isEmpty() → Empty
                              → else            → List
Loading → [3s 超时] → Error

Empty → [点击 FAB] → navToEditor(null)

List → [点击 FAB] → navToEditor(null)
List → [点击卡片] → navToEditor(noteId)
List → [点击搜索框] → SearchMode(保持列表)
List → [长按卡片] → MultiSelect(初始 0 项选中)

SearchMode → [输入关键词] → debounce 300ms → SearchResults / SearchEmpty
SearchMode → [清空搜索框] → List
SearchMode → [按返回键] → List（失去焦点）

SearchResults → [点击卡片] → navToEditor(noteId)
SearchResults → [点击 ✕] → List（清空+失去焦点）

SearchEmpty → [点击清除搜索] → List
SearchEmpty → [修改关键词] → debounce → SearchResults / SearchEmpty

MultiSelect → [勾选卡片] → selectedCount++ / --
MultiSelect → [点击全选] → selectAll
MultiSelect → [点击 ✕] → List（退出多选）
MultiSelect → [按返回键] → List（退出多选）
MultiSelect → [最后一项取消勾选] → List（自动退出）
MultiSelect → [点击删除选中] → AlertDialog → 确认 → 批量软删除
         → Snackbar("已删除 N 条" + "撤销", 8s)
         → 撤销期间点击撤销 → 恢复所有 + List
         → 超时 → 后台物理清理

Error → [点击重试] → Loading
Error → [点击 FAB] → navToEditor(null)（允许新建）

List → [左滑卡片] → SwipeToDismissBox 动画
      → if 滑过阈值 → AlertDialog 确认
      → 确认 → 软删除 + Snackbar("已删除"+"撤销", 8s)
      → 撤销 → 恢复卡片到原位
      → 超时 → 后台物理清理
```

#### 状态覆盖（P1 完整态）

| 状态 | SearchBar | FAB | 内容区 | TopAppBar | 左滑删除 |
|------|-----------|-----|--------|-----------|:--:|
| Loading | 折叠, disabled(alpha 0.5) | 可见(可点击) | CircularProgressIndicator + 骨架卡片 | "记事本" | 否 |
| Empty | 折叠, enabled | **可见(引导箭头)** | 插画+文案+引导 | "记事本" | 否 |
| List | 折叠, enabled | 可见 | 笔记卡片 LazyColumn | "记事本" | **是** |
| SearchMode | 展开, focused, border=primary(2dp) | 可见 | 搜索过滤列表 | "记事本" | 否 |
| SearchEmpty | 展开, focused, border=primary(2dp) | 可见 | SearchOff(64dp)+文案+清除按钮 | "记事本" | 否 |
| MultiSelect | 隐藏 | 隐藏 | Checkbox+卡片+BottomAppBar | **替换为** "已选 N 项 \| 全选 \| ✕" | 否 |
| Error | 折叠, disabled(0.5) | 可见(可点击) | error 图标+文案+重试按钮 | "记事本" | 否 |

---

### P2: 笔记编辑器 (`/notes/edit?noteId={noteId}`)

#### 线框图

```
┌──────────────────────────────────────┐
│  Status Bar                     9:41 │
├──────────────────────────────────────┤
│  ← │ 笔记标题_______________ │ 👁    │  ← TopAppBar
│    │         12/100           │      │     ← 计数(≥90字符出现)
│    │                          │      │     ← 预览切换按钮
│    │                          │      │
├──────────────────────────────────────┤
│                                      │
│                                      │
│   BasicTextField                     │  ← 编辑区（占满剩余空间）
│   placeholder:                        │     imePadding() 避让键盘
│   "开始输入笔记内容..."               │
│                                      │
│   ## 标题                            │
│   **粗体** *斜体*                     │
│   - 列表项                           │
│   [链接](url)                        │
│   ``` 代码块 ```                      │
│                                      │
│                                      │
├──────────────────────────────────────┤
│  # │ B │ I │ │ ≡ │ 🔗 │ </> │      │  ← Markdown 快捷工具栏
├──────────────────────────────────────┤   (键盘弹起时收缩为单行内联)
│  已自动保存 14:32                    │  ← 轻量保存提示（非 Snackbar）
└──────────────────────────────────────┘
```

#### 组件层级树

```
NoteEditScreen(noteId: String?)
├── Scaffold
│   ├── topBar: TopAppBar
│   │   ├── navigationIcon: IconButton(ArrowBack, 48dp)
│   │   │   ├── contentDescription = "返回"
│   │   │   └── onClick → { saveDraft; navController.popBackStack() }
│   │   │
│   │   ├── title: Row
│   │   │   ├── BasicTextField(
│   │   │   │   value = title,
│   │   │   │   onValueChange = { if(it.length<=100) update; else block },
│   │   │   │   placeholder = "笔记标题",
│   │   │   │   textStyle = titleMedium,
│   │   │   │   singleLine = true,
│   │   │   │   modifier.weight(1f),
│   │   │   │ )
│   │   │   └── if(title.length >= 90)
│   │   │       └── Text("{title.length}/100",
│   │   │             labelSmall,
│   │   │             color = if(title.length>=100) error else onSurfaceVariant)
│   │   │
│   │   └── actions: Row
│   │       └── IconToggleButton(
│   │           checked = isPreviewMode,
│   │           onCheckedChange = { togglePreview() },
│   │           contentDescription = if(isPreview) "编辑" else "预览",
│   │           )
│   │           ├── if(edit): Icon(Preview, 24dp)
│   │           └── if(preview): Icon(Edit, 24dp)
│   │
│   ├── content: Box(Modifier.fillMaxSize().imePadding())
│   │   └── if(isPreviewMode)
│   │       └── PreviewContent
│   │           ├── Column(Modifier.verticalScroll(rememberScrollState()))
│   │           ├── Text(title, headlineSmall, onSurface)  ← 只读标题
│   │           └── NoteMarkdownRenderer(content, modifier)  ← Markdown 渲染
│   │               ├── 标题: h1/h2/h3 via SpanStyle
│   │               ├── 粗体: Bold
│   │               ├── 斜体: Italic
│   │               ├── 列表: bullet + indent
│   │               ├── 链接: primary + UrlRole + underline
│   │               ├── 代码块: surfaceVariant bg + mono font
│   │               └── 行内代码: surfaceVariant bg + mono font + padding
│   │   └── else
│   │       └── EditContent
│   │           ├── BasicTextField(
│   │           │   value = content,
│   │           │   onValueChange = { if(it.length<=50000) update; else block },
│   │           │   placeholder = "开始输入笔记内容...",
│   │           │   textStyle = bodyLarge(lineHeight=24sp),
│   │           │   modifier = Modifier.fillMaxSize().padding(16.dp),
│   │           │ )
│   │           └── LaunchedEffect(content) {
│   │               debounce(500ms) → saveDraftToDataStore()
│   │           }
│   │
│   ├── bottomBar: if(!isPreviewMode)  ← 预览态隐藏
│   │   └── MarkdownToolbar (keyboardAware: 键盘弹起时收缩)
│   │       ├── Row(horizontalArrangement=SpaceEvenly, padding=8dp_h)
│   │       │   ├── IconButton("#", title="标题")    ← TooltipBox(500ms)
│   │       │   ├── IconButton("B", title="粗体")
│   │       │   ├── IconButton("I", title="斜体")
│   │       │   ├── VerticalDivider(24dp)
│   │       │   ├── IconButton("≡", title="无序列表")
│   │       │   ├── IconButton("🔗", title="链接")
│   │       │   └── IconButton("</>", title="代码块")
│   │       └── if 键盘弹起: collapse to InlineToolbar
│   │           └── Row(padding=4dp_h)
│   │               ├── IconButton("#")
│   │               ├── IconButton("B")
│   │               ├── IconButton("-")
│   │               └── IconButton("...", onClick → expand full toolbar)
│   │
│   └── snackbarHost: SnackbarHost
│       ├── 首次进入(新建): Snackbar("使用工具栏快速插入格式", duration=3s)
│       └── 保存失败: Snackbar("保存失败，请重试", action="重试")
│
│   LaunchedEffect(Unit):
│   ├── if noteId != null → loadNoteFromRoom(noteId)
│   └── if noteId == null → loadDraftFromDataStore()  ← 草稿恢复
│
│   DisposableEffect:
│   └── onDispose → saveDraftToDataStore()  ← L3 最后写入
│
│   SavedStateHandle:  ← L1 旋转/配置变更
│   ├── save("title", title)
│   ├── save("content", content)
│   └── restore → setState
```

#### 交互状态机

```
Create(新建) → [noteId=null]
    → 加载 DataStore 草稿 → if 有草稿 → DraftRecovery
    → 显示空编辑器 + 首次引导 Snackbar(3s)

Edit(编辑已有) → [noteId=id]
    → Room.load(noteId) → 显示已有内容
    → 底部轻量文字 "已自动保存 HH:mm"

Edit(编辑态) → [输入标题/内容] → debounce 500ms → DataStore.writeDraft
Edit(编辑态) → [点击工具栏按钮] → 光标位置插入语法 + 更新 content
Edit(编辑态) → [点击 ← / BackHandler] → check:
    → if title.isBlank() && content.isBlank() → 不创建笔记, popBackStack
    → if title.isBlank() && content.isNotBlank() → autoTitle="无标题笔记(HH:mm)", Room.upsert, popBackStack
    → else → Room.upsert, popBackStack

Edit(编辑态) → [切后台/锁屏] → onDispose → DataStore.writeDraft
Edit(编辑态) → [杀进程] → L2 已 debounce(500ms) 写入, 丢失 ≤500ms

Edit(编辑态) → [点击预览按钮] → Preview
Preview → [点击编辑按钮 / BackHandler] → Edit（保持滚动位置）

DraftRecovery → [显示 Snackbar "恢复未保存的草稿？" + "恢复"/"放弃"]
    → 恢复: fill editor + 草稿清除
    → 放弃: 清除草稿 + 空编辑器

SaveFailed → [Room 写入异常 / 内容超限]
    → Snackbar(action="重试")
    → 重试 → Room.upsert → 成功 → popBackStack / 保持编辑
    → 返回 → 不保存, popBackStack（草稿已存 DataStore）
```

#### 状态覆盖（P2 完整态）

| 状态 | 标题区 | 编辑/预览 | 工具栏 | 底部提示 |
|------|--------|:--:|:--:|------|
| Create(空) | placeholder="笔记标题" | 编辑, 空, placeholder | 显示, 全部按钮 | 首次引导 Snackbar(3s 淡出) |
| Edit(有内容) | 标题内容 + 计数(≥90字符) | 编辑, 已填充 | 显示 | "已自动保存 HH:mm" |
| Preview | 只读标题 | 渲染 Markdown, 可滚动 | **隐藏** | "已自动保存 HH:mm" |
| DraftRecovery | 草稿标题 | 草稿内容 | 显示 | Snackbar("恢复未保存的草稿？") |
| SaveFailed | 保留编辑内容 | 保留编辑内容 | disabled(opacity 0.38) | Snackbar("保存失败，请重试"+"重试") |
| 键盘弹起 | 保留 | 编辑, imePadding() | **收缩为单行**: # / B / - / ... | — |

---

## Token 映射表

### 颜色 Token（M3 默认色板 — 基于 PRD §9.5 + 自建体系）

| 元素 | M3 Token | 浅色值 | 深色值 | 来源 |
|------|---------|--------|--------|------|
| 页面背景 | background | #FEFBFF | #1C1B1F | M3 默认 |
| 卡片背景 | surface | #FEFBFF | #1C1B1F | M3 默认 |
| 卡片边框 | outlineVariant(1dp) | #CAC4D0 | #49454F | PRD §9.5: 12dp 圆角 |
| SearchBar 容器(折叠) | surfaceVariant | #E7E0EC | #49454F | M3 SearchBar 默认 |
| SearchBar 容器(展开) | surface | #FEFBFF | #1C1B1F | M3 SearchBar focused |
| SearchBar 焦点边框 | primary(2dp) | #1A4FBF | #B0C6FF | PRD §9.5 |
| placeholder 文字 | onSurfaceVariant | #49454F | #CAC4D0 | PRD §9.5 |
| 卡片标题 | onSurface | #1C1B1E | #E6E1E5 | M3 默认 |
| 卡片摘要/时间 | onSurfaceVariant | #49454F | #CAC4D0 | PRD §9.5 |
| 关键词高亮 | primary Bold + bg | #1A4FBF on rgba(26,79,191,0.18) | #B0C6FF on rgba(176,198,255,0.18) | PRD §9.5 |
| FAB 背景 | primaryContainer | #DAE2FF | #0040A5 | M3 FAB |
| FAB 图标色 | onPrimaryContainer | #001849 | #DAE2FF | M3 FAB |
| FAB disabled | onSurface(38%) | opacity 0.38 | opacity 0.38 | PRD §9.5: 上限禁用 |
| 删除红 | error | #B81C1C | #FFB4AB | PRD §9.5 |
| Snackbar 撤销动作 | primary | #1A4FBF | #B0C6FF | PRD §9.5 |
| 空状态图标 | outline | #79747E | #938F99 | WCAG AA ≥4.5:1 |
| 加载指示器 | primary | #1A4FBF | #B0C6FF | M3 默认 |
| 错误图标背景 | errorContainer | #FFDAD6 | #93000A | M3 默认 |
| 错误图标色 | error | #B81C1C | #FFB4AB | M3 默认 |
| 代码块背景(浅) | surfaceVariant | #E7E0EC | — | PRD §9.5 |
| 代码块背景(深) | 加深 surface | — | #2D2D30 | PRD §9.5: 深色用更深 surface |
| 链接色 | primary | #1A4FBF | #B0C6FF | PRD §9.5 |
| ripple(pressed) | primary(8%) | rgba(26,79,191,0.08) | rgba(176,198,255,0.08) | M3 标准 |
| ripple(error) | error(8%) | rgba(184,28,28,0.08) | rgba(255,180,171,0.08) | M3 标准 |
| disabled(opacity) | onSurface(38%) | opacity 0.38 | opacity 0.38 | M3 标准 |
| 多选选中背景 | primaryContainer | #DAE2FF | #0040A5 | M3 默认 |
| 多选 Checkbox(选中) | primary | #1A4FBF | #B0C6FF | M3 Checkbox |

### 字体 Token

| 用途 | M3 Token | 规格 | 来源 |
|------|---------|------|------|
| 标题(编辑器只读) | headlineSmall | 24sp, Regular, lineHeight=32sp | PRD §9.5 |
| 笔记标题 | titleMedium | 16sp, Medium, lineHeight=24sp | PRD §9.5 |
| 笔记正文 | bodyLarge | 16sp, Regular, lineHeight=24sp | PRD §9.5 |
| 摘要/引导文案 | bodyMedium | 14sp, Regular, lineHeight=20sp | PRD §9.5 |
| 时间戳 | labelSmall | 11sp, Regular, lineHeight=16sp | PRD §9.5 |
| 计数/分类标签 | labelMedium | 12sp, Medium, lineHeight=16sp | PRD §9.5 |
| 多选标题 | titleLarge | 22sp, Regular, lineHeight=28sp | M3 默认 |
| 标题输入 | titleMedium | 16sp, Medium, lineHeight=24sp | PRD §9.5 |
| Placeholder | bodyLarge | 16sp, Regular, lineHeight=24sp, onSurfaceVariant | PRD §9.5 |
| Snackbar 文本 | bodyMedium | 14sp, Regular, lineHeight=20sp | M3 Snackbar |
| 代码字体 | — | 14sp, JetBrains Mono, Regular | PRD §9.5 |
| 自动保存提示 | labelSmall | 11sp, Regular, onSurfaceVariant | PRD §9.5 |

### 形状 Token

| 元素 | 圆角 | 来源 |
|------|:--:|------|
| 笔记卡片 | 12dp | PRD §9.5 |
| FAB | 16dp | PRD §9.5 |
| 对话框 | 28dp | PRD §9.5 |
| SearchBar | 28dp | M3 SearchBar |
| 按钮(Card 内) | 20dp | M3 Button |
| Checkbox | 4dp | M3 Checkbox |
| Snackbar | 4dp | M3 Snackbar |

### 间距 Token（8dp 网格）

| 用途 | 值 | 来源 |
|------|:--:|------|
| 页面水平 padding | 16dp | PRD §9.5 |
| SearchBar top margin | 12dp | 与既有 UI_DESIGN 一致的呼吸感 |
| 卡片内部 padding | 16dp | 8dp 网格 × 2 |
| 卡片间距 | 12dp | PRD §9.5 |
| 元素间基础间距 | 8dp | 8dp 网格 |
| 分组间距 | 24dp | 8dp 网格 × 3 |
| FAB 距边缘 | 24dp | M3 FAB 规范 |
| 触控最小尺寸 | 48dp × 48dp | Android 无障碍 |

---

## 交互状态反馈矩阵

| 元素 | Pressed | Focused | Disabled | LongPress |
|------|:--:|:--:|:--:|:--:|
| FAB | ripple(primaryContainer 暗化) | — | opacity 0.38 | Toast"已达上限"(disabled) |
| 笔记卡片 | ripple(primary 8%) | — | — | haptic + 微缩放(0.95→1.0×100ms) |
| SearchBar 折叠 | ripple(primary 8%) | — | opacity 0.5 | — |
| SearchBar 展开 | — | primary(2dp 下划线) | — | — |
| SearchBar Clear(×) | ripple(circular, 48dp) | outline 环(2dp) | — | — |
| 返回箭头 | ripple(circular, 48dp) | outline 环(2dp) | — | — |
| 预览切换按钮 | ripple(circular, 48dp) | outline 环(2dp) | opacity 0.38 | — |
| 工具栏按钮 | ripple(primary 8%, 40dp) | outline 环(2dp) | opacity 0.38 | TooltipBox(500ms, 显示语义) |
| 删除确认按钮 | ripple(error 8%) | outline 环(2dp) | — | — |
| Snackbar 撤销 | ripple(primary 8%) | — | — | — |
| 重试按钮 | ripple(primary 8%) | outline 环(2dp) | — | — |
| Checkbox | ripple(primary 8%) | outline 环(2dp) | — | — |
| 全选/✕关闭 | ripple(primary 8%) | outline 环(2dp) | — | — |
| SwipeToDismissBox | 红色背景 reveal(0→200dp) | — | — | — |

---

## 组件复用分析

| 已有组件 | 路径 | 复用 | 需修改 | 备注 |
|---------|------|:--:|:--:|------|
| MainActivity | MainActivity.kt | ✅ | 是 | 添加底部 Tab"记事本"入口 + NavHost 路由 |
| MyApplication | MyApplication.kt | ✅ | 否 | @HiltAndroidApp 无变更 |
| LoginViewModel | ui/login/LoginViewModel.kt | ❌ | — | 登录模块，无复用价值 |
| LoginRepository | data/LoginRepository.kt | ❌ | — | 登录模块，无复用价值 |

> **结论**：现有 4 个组件中 2 个为登录模块专用（不复用），MainActivity 和 MyApplication 需小幅修改。记事本功能所有组件为**全新创建**。

## 新增组件清单

| 层级 | 组件 | 复杂度 | 工时 | 备注 |
|------|------|:--:|:---:|------|
| **UI** | NoteListScreen | 高 | 3.0h | 7 态状态机 + LazyColumn + SwipeToDismissBox + 搜索 + 多选 + 空状态 |
| UI | NoteEditScreen | 高 | 3.5h | 双态切换 + SavedStateHandle + 草稿恢复 + 键盘适配 + 工具栏 |
| UI | NoteListViewModel | 中 | 2.0h | Room Flow 收集 + 搜索过滤 + 防抖 300ms + 多选状态 + 删除逻辑 |
| UI | NoteEditViewModel | 高 | 2.5h | 草稿三层防护 + 自动保存 + 字段约束 + 自动标题 + Preview 切换 |
| UI | NoteCard | 低 | 0.5h | ElevatedCard + 标题/摘要/相对时间 + combinedClickable |
| UI | EmptyNoteState | 低 | 0.3h | 插画(Canvas)+文案+FAB 引导箭头 |
| UI | SearchEmptyState | 低 | 0.3h | SearchOff 图标+关键词回显+清除按钮 |
| UI | MultiSelectTopBar | 中 | 0.5h | "已选 N 项"+"全选"+"✕" |
| UI | NoteMarkdownRenderer | 中 | 1.5h | AnnotatedString 解析器 + M3 深浅双主题 |
| UI | MarkdownToolbar | 低 | 0.5h | 工具栏 + TooltipBox + 键盘感知收缩 |
| UI | HighlightedText | 低 | 0.5h | AnnotatedString + SpanStyle(primary Bold + bg) |
| UI | RelativeTimeFormatter | 低 | 0.3h | 今天/昨天/周X/MM-dd/yyyy-MM-dd |
| **Domain** | NoteRepository | 低 | 0.3h | 接口定义 |
| **Data** | NoteDao | 低 | 0.5h | Room DAO: CRUD + Flow 查询 + 搜索过滤 |
| Data | NoteEntity | 低 | 0.3h | Room Entity: id/title/content/createdAt/updatedAt/isDeleted |
| Data | NoteRepositoryImpl | 低 | 0.5h | 实现类 + Flow 映射 |
| Data | DraftRepository | 低 | 0.5h | DataStore 草稿读写 + L2 debounce 500ms |
| Data | AppDatabase | 低 | 0.3h | Room Database 单例 |
| **DI** | DatabaseModule | 低 | 0.3h | Hilt @Module: Room + DataStore 绑定 |
| DI | NoteModule | 低 | 0.2h | NoteViewModel + NoteRepository + DraftRepository |
| **Navigation** | NavGraph(扩展) | 低 | 0.5h | 添加 `/notes` + `/notes/edit?noteId={noteId}` |
| | | | | |
| **新建小计** | **21 组件** | | **18.8h** | |
| **修改现有** | MainActivity / MyApplication | 低 | **1.5h** | Tab 入口 + NavHost 集成 |
| **测试+无障碍** | 单元/UI/无障碍/TalkBack | 中 | **5.0h** | ViewModel 单测 + Compose UI 测试 + 无障碍审计 |
| **缓冲** | Room Migration 模板/Markdown 库验证/键盘适配测试 | — | **3.0h** | 确认 jeziellago/compose-markdown M3 兼容性 |
| **总计** | | | **28.3h** (≈3.5d) | 对外承诺 4d，内部冲刺 3.5d |

---

## 无障碍审计清单

| 元素 | contentDescription | TalkBack 焦点顺序 | 播报时机 |
|------|-------------------|:--:|------|
| FAB | "创建新笔记" | Tab 4(列表)/Tab 2(空状态) | 页面加载 |
| SearchBar 折叠 | "搜索笔记" | Tab 1 | 页面加载 |
| SearchBar Clear(×) | "清除搜索内容" | Tab 2 | 有输入时出现 |
| 笔记卡片 | {标题},{相对时间} | Tab 2+ | 列表可见时 |
| 左滑删除动作 | "删除操作"（自定义 TalkBack 动作） | 焦点在卡片时可用 | 卡片获焦时 |
| 多选 Checkbox | "{标题}，已选中/未选中" | Tab 2+ | 多选模式 |
| 多选 ✕ 关闭 | "退出多选模式" | Tab 1 | 多选模式激活 |
| 全选按钮 | "全选所有笔记" | Tab 1 右侧 | 多选模式激活 |
| 删除选中按钮 | "删除选中的{N}条笔记" | Tab 最后 | 有选中项时 |
| 空状态图标 | "还没有笔记" | Tab 1 | 空状态时 |
| 搜索无结果图标 | "未找到搜索结果" | Tab 1 | 搜索无结果时 |
| 编辑器返回 | "返回笔记列表" | Tab 1 | 编辑器 |
| 编辑器标题 | "笔记标题" | Tab 2 | 编辑器 |
| 编辑器内容 | "笔记内容" | Tab 3 | 编辑器 |
| 预览切换 | "切换预览"/"切换编辑" | Tab 2 右侧 | 编辑器 |
| 工具栏 # | "插入标题" | Tab 4 | 编辑态 |
| 工具栏 B | "插入粗体" | Tab 5 | 编辑态 |
| 工具栏 I | "插入斜体" | Tab 6 | 编辑态 |
| 工具栏 - | "插入列表" | Tab 7 | 编辑态 |
| 工具栏 🔗 | "插入链接" | Tab 8 | 编辑态 |
| 工具栏 </> | "插入代码块" | Tab 9 | 编辑态 |
| Snackbar | 自动播报 Snackbar 文本 | — | 出现时 |
| AlertDialog | "确定删除{N}条笔记？" | 获焦 | 弹出时 |
| 搜索结果数 | — | — | announceForAccessibility("找到{N}条匹配笔记") |
| 多选数量变化 | — | — | announceForAccessibility("已选中{N}条") |

## 动效规格

| 动效 | 时长 | 曲线 | 触发 |
|------|:--:|------|------|
| 笔记卡片入场 | 300ms, stagger 50ms | fadeIn + slideInVertically(20dp) | Loading → List |
| 卡片删除移除 | 200ms | shrinkVertically + fadeOut | 确认删除 |
| 搜索过滤刷新 | 自然跟随 Flow | — | searchQuery 变更 |
| 预览/编辑切换 | 200ms | AnimatedContent crossfade | Toggle 按钮 |
| Snackbar 出现 | 300ms | slideInVertically | 删除/错误/撤销 |
| Snackbar 消失 | 300ms | slideOutVertically | 超时/用户操作 |
| FAB 显隐 | 200ms | scaleIn / scaleOut | 多选模式 |
| 多选模式进入 | 100ms | scale(0.95→1.0) | 长按卡片 |
| 键盘弹起 | — | WindowInsets.ime | 焦点获取 |
| ripple | 150ms | circular | 点击/长按 |
| 首次引导淡出 | 3s → fadeOut(300ms) | easeOut | 进入编辑器 |

---

## 适配规格

| 宽度 | 布局 | 列表 | 编辑器 |
|------|------|------|------|
| < 600dp (compact) | 单列全屏 | 单列卡片 LazyColumn | 全屏编辑 |
| 600-840dp (medium) | 双列网格 | LazyVerticalGrid(2 columns) | 编辑器居中 maxWidth=720dp |
| ≥ 840dp (expanded) | 列表+编辑器并排 | LazyColumn 左侧 | 编辑器右侧常驻 |

**横屏**：
- 列表: LazyVerticalGrid(2 columns)
- 编辑器: contentMaxWidth=720dp + 居中
- 键盘弹起: MarkdownToolbar → TopAppBar 溢出菜单

---

## 架构协调设计

### 草稿保存三层防护

```
L1 — SavedStateHandle（旋转/配置变更恢复）
  → onSaveInstanceState { savedStateHandle["title"] = title; ... }
  → onCreate { title = savedStateHandle["title"] ?: "" }

L2 — DataStore debounce(500ms)（停止输入后自动写入）
  → LaunchedEffect(content) { delay(500); draftRepo.save(...) }

L3 — onDispose 最后写入（切后台/杀进程前）
  → DisposableEffect { onDispose { draftRepo.saveSync(...) } }
```

### 搜索防抖（300ms — 与 DECISIONS 统一）

```
NoteListViewModel:
  searchQuery.debounce(300).flatMapLatest { query ->
      if (query.isBlank()) noteRepo.getAllNotes()
      else noteRepo.searchNotes(query)
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
```

### 自动标题生成

```
fun autoTitle(): String {
    val now = java.time.LocalTime.now()
    val time = String.format("%02d:%02d", now.hour, now.minute)
    return "无标题笔记($time)"
}
```

---

## 变更记录

| 版本 | 日期 | 变更说明 |
|------|------|---------|
| v0.1-draft | 2026-06-02 | 初稿生成 — 基于 PRD v1.0-confirmed §9 + DECISIONS.md 记事本模块全部决策 |
