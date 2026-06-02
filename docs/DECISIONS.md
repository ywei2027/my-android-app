# 项目决策记录

> 本文档记录架构选型、技术决策及变更历史。

---

## 决策列表

| 编号 | 日期 | 决策内容 | 状态 |
|------|------|----------|------|
| D-01 | 2026-06-02 | v1.0 采用纯文本编辑，不含 Markdown（对齐"快速记录"定位，降低复杂度） | 已确认 |
| D-02 | 2026-06-02 | 删除采用"左滑→确认对话框→Snackbar 撤销(5秒)"模式 | 已确认 |
| D-03 | 2026-06-02 | 列表全量加载驱动 LazyColumn（非 Paging 3），1000 条内无压力，v1.1 预留扩展 | 已确认 |
| D-04 | 2026-06-02 | 标题为空时自动截取内容前 20 字作为显示标题 | 已确认 |
| D-05 | 2026-06-02 | 编辑页返回确认：有修改弹窗拦截，无修改直接返回 | 已确认 |
| D-06 | 2026-06-02 | v1.0 不做数据导出，标记为 v1.1 高优先级 | 已确认 |
| D-07 | 2026-06-02 | 搜索匹配标题+内容双字段 OR，不区分大小写 contains，300ms debounce | 已确认 |
| D-08 | 2026-06-02 | 导航入口：NavigationBar 底部 Tab，路由 /notes 和 /notes/edit?noteId= | 已确认 |
| D-09 | 2026-06-02 | Room v1.0 使用 fallbackToDestructiveMigration()，exportSchema=true，v1.1 开始手动 Migration | 已确认 |
| D-10 | 2026-06-02 | Repository 层返回 Result<T>，ViewModel 映射到 UiState.Error（统一错误处理契约） | 已确认 |
| D-11 | 2026-06-02 | 编辑页草稿通过 SavedStateHandle 保护（进程死亡恢复），最大丢失 ≤ 500ms 内容 | 已确认 |
| D-12 | 2026-06-02 | 版本号展示策略: Debug 构建在主界面底部显示（含 VERSION_NAME + VERSION_CODE + 构建类型），Release 构建仅在「设置→关于」显示 | 待确认 |
| D-13 | 2026-06-02 | 版本号颜色使用 Material3 `onSurfaceVariant` Token，废弃 PRD 硬编码 `#9E9E9E`/`#BDBDBD`（不满足 WCAG AA 对比度） | 待确认 |
| D-14 | 2026-06-02 | minSdk 统一修正为 26（PRD 原声明 API 24 与实际 build.gradle 配置 26 不一致） | 待确认 |
| D-15 | 2026-06-02 | 版本号信息格式: `v{name} ({code}) {buildType}` + 可选 Git SHA 短码，替代原 `v1.0.0` 单一字段 | 待确认 |
| D-16 | 2026-06-02 | 新增 windowInsets 适配全面屏手势导航栏 + 横屏/字体缩放/分屏场景覆盖 | 待确认 |
| D-17 | 2026-06-02 | 版本号 TalkBack contentDescription 设为「应用版本号」，对比度目标 ≥ 3:1（WCAG AA 大文本标准） | 待确认 |
| D-18 | 2026-06-02 | UI 设计决议: 版本号格式扩展为 `v{name}({code}){buildType}` (D-15)，Debug/Release 条件编译 (D-12)，darkColorScheme 补建 | 已确认 |
| D-19 | 2026-06-02 | UI 设计决议: VersionTag 视觉层级降级——使用较低对比度或 opacity 0.6 与导航文字区分 | 已确认 |
| D-20 | 2026-06-02 | UI 设计决议: 下边距修正为 8px(8dp 网格对齐)，横屏/分屏 <480dp 隐藏版本号 | 已确认 |
| D-21 | 2026-06-02 | 技术方案决议: VersionTag 与 LoginScreen 解耦（Box bottom-align），`v{name}({code}){buildType}` 格式，WindowInsets.navigationBars，maxLines+Ellipsis | 待确认 |
| R2-D-22 | 2026-06-02 | 关于页面入口方案: 登录页底部「关于」TextButton → 路由 /about | 待确认 |
| R2-D-23 | 2026-06-02 | AboutScreen 始终显示版本号，不受 BuildConfig.DEBUG 控制（VersionTag 仅 Debug 的规则不适用） | 待确认 |
| R2-D-24 | 2026-06-02 | 关于页面版本号为静态展示，不添加点击复制等交互 | 待确认 |
| R2-D-25 | 2026-06-02 | 导航初版双路由 Login + About，NavHost 接线在本功能中一并实现 | 待确认 |
| R2-D-26 | 2026-06-02 | TalkBack contentDescription 包含完整版本信息（name+code+buildType） | 待确认 |
| R3-D-27 | 2026-06-02 | PRD 轻量 3-Agent 评审: AC-01 格式对齐 D-15，交互对齐 D-24，minSdk 修正为 API 26，新增 AC-04 无障碍验收 — 7 P0 已自动修订 | 待确认 |
| R3-D-28 | 2026-06-02 | UI 设计 3-Agent 轻量评审: C2 视觉 37/50 条件通过，版本号方案统一为 BuildConfig(对齐 VersionTag.kt)，textIsSelectable 放行(原生选中≠自定义交互)，降级态增加视觉差异化 | 待确认 |

---

## 变更历史

| 日期 | 变更 | 触发事件 |
|------|------|----------|
| 2026-06-02 | 关于页面 UI 设计三视角评审 (C1/C2/C3): 6 P0 → 自动修订，C2 视觉 41/50 通过 | delegate_task 并行评审 |
| 2026-06-02 | 技术方案三视角评审 (B1/B2/B3): 7 P0 → 5 项自动修订，新增 D-21 | delegate_task 并行评审 |
| 2026-06-02 | UI 设计三视角评审 (C1/C2/C3): 4 P0 / 8 P1 / C2 综合 37/50，新增 3 项决议 (D-18~D-20) | delegate_task 并行评审 |
| 2026-06-02 | PRD 轻量 3-Agent 评审: 5 项共识 P0 自动修订，新增 R3-D-27 | delegate_task 轻量并行评审 |
