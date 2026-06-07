# C2 视觉审美评审报告 — 登录页 UI 方案

> **评审日期:** 2026-06-07 | **方法:** HTML源码审查 + 像素级图像分析(10张PNG) | **模型:** deepseek-v4-pro
> **设计基准:** 375dp @2x (750×1624) | M3 Token体系 | #1A73E8品牌色

---

## 一、10维度评分总览表

| # | 维度 | 均分 | 关键问题 |
|---|------|:---:|------|
| 1 | 格式塔感知 | 4/5 | 五层分组清晰(图标→标题→副标题→输入框→按钮)，8dp网格一致；错误态时按钮/错误Card分组间距略大 |
| 2 | 视觉层级 | 3/5 | ⚠️ 标题24sp/副标题14sp/按钮16sp/提示11sp四层梯度分明，但标题在部分状态(text抗锯齿)视觉存在感偏弱 |
| 3 | 色彩系统 | 2/5 | 🔴 **暗色模式完全失效**，login_dark.png与亮色模式无差异；副标题对比度4.4:1低于WCAG AA 4.5:1；loading态副标题对比度仅1.9:1 |
| 4 | 字体排版 | 4/5 | 全部映射M3 Typography Token，层级数量合理(4级)；中英文混排一致；副标题行高20sp/字号14sp=1.43略紧 |
| 5 | 空间与网格 | 5/5 | 8dp基准网格严格对齐，水平padding 24dp，弹性spacer实现垂直居中，组件间距16dp/24dp/32dp分层清晰 |
| 6 | 布局与比例 | 4/5 | 375dp基准视口平衡，按钮fillMaxWidth+48dp触控；icon 64dp/标题24sp比例协调；横屏滚动已处理 |
| 7 | 可感知可操作 | 3/5 | 🔴 **按钮文字疑似未渲染**(像素扫描全程未捕获#FFFFFF文字)；disabled态0.38透明与enabled态区分明显；输入框focus-within焦点环正确 |
| 8 | 一致性 | 4/5 | 品牌色#1A73E8统一，M3 Token全量覆盖；但login.html暗色模式仅用inline style覆写phoneFrame，与@media方案不一致 |
| 9 | 情感与品牌 | 3/5 | 功能型页面，无品牌差异化元素；64dp图标可作为Logo占位但当前仅为通用email SVG；底部测试提示破坏正式感 |
| 10 | 平台与适配 | 2/5 | 🔴 暗色模式完全失效(见§三)；login_full_page.png尺寸1600×1800非标准；平板大屏未验证 |

**综合评分: 34/50** — ⚠️ 评分偏低(≥30免重新设计线刚过)，色彩系统和平台适配维度需紧急修复。

---

## 二、逐截图评审简表

| 截图 | 格式塔 | 视觉层级 | 色彩 | 字体 | 空间 | 布局 | 可操作性 | 一致性 | 情感品牌 | 平台适配 | 小计 |
|------|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| login_idle | 5 | 3 | 2 | 4 | 5 | 5 | 3 | 5 | 3 | 2 | 37 |
| login_editing | 5 | 4 | 2 | 4 | 5 | 4 | 3 | 5 | 3 | 2 | 37 |
| login_email_error | 4 | 3 | 2 | 4 | 4 | 4 | 3 | 4 | 3 | 2 | 33 |
| login_loading | 4 | 3 | 2 | 3 | 5 | 4 | 4 | 5 | 3 | 2 | 35 |
| login_error_401 | 4 | 4 | 2 | 4 | 4 | 4 | 4 | 5 | 3 | 2 | 36 |
| login_error_403 | 4 | 4 | 2 | 4 | 4 | 4 | 4 | 5 | 3 | 2 | 36 |
| login_error_429 | 4 | 4 | 2 | 4 | 4 | 4 | 4 | 5 | 3 | 2 | 36 |
| login_timeout | 4 | 4 | 2 | 4 | 4 | 4 | 4 | 5 | 3 | 2 | 36 |
| login_dark | 2 | 2 | 1 | 2 | 5 | 5 | 2 | 1 | 2 | 1 | 23 🔴 |
| login_full_page | 4 | 3 | 3 | 4 | 4 | 3 | 3 | 4 | 3 | 3 | 34 |

---

## 三、详细问题清单

### 🔴 P0 严重 (阻断级)

| # | 严重度 | 维度 | 问题描述 | 截图位置 | 改进建议 | 参考案例 |
|---|:---:|------|----------|----------|----------|----------|
| P0-1 | 🔴 | 色彩/平台 | **暗色模式完全失效**。`login_dark.png`背景色`#FEFBFF`与亮色模式无差异，所有暗色Token(`#1C1B1F` surface, `#8AB4F8` primary, `#E6E1E5` onSurface)均未生效 | login_dark.png 全屏 | HTML中`.dark` class无对应CSS规则；`toggleDarkMode()`仅覆写phoneFrame inline style但Playwright截图未触发。修复：添加`html.dark { ... }`规则或使用`prefers-color-scheme`媒体查询；重拍暗色截图 | M3 Dark Theme: surface=#1C1B1F, primary=#8AB4F8, onPrimary=#003A75 |
| P0-2 | 🔴 | 可感知性 | **按钮文字可能未渲染**。像素扫描横跨按钮200-548px全程仅捕获`#1A73E8`(primary)，未检测到`#FFFFFF`(onPrimary)文字像素。按钮呈现为纯色矩形无文字 | editing/error系列 按钮区域 | 排查CSS `color: var(--on-primary)`是否正确应用；确认字体加载(`Roboto, Noto Sans SC`)；Playwright截图前等待字体ready | M3 Button: filled样式=primary背景+onPrimary文字，对比度≥4.5:1 |

### 🟠 P1 高优先级

| # | 严重度 | 维度 | 问题描述 | 截图位置 | 改进建议 | 参考案例 |
|---|:---:|------|----------|----------|----------|----------|
| P1-1 | 🟠 | 色彩 | **副标题对比度4.4:1低于WCAG AA 4.5:1**。`#79747E`(onSurfaceVariant) on `#FEFBFF`(surface)，14sp body文字需≥4.5:1 | 全部亮色截图 副标题位置 | 将onSurfaceVariant从`#49454F`调整为更深的`#44414A`(~5.2:1)或增大副标题至18sp(大文字≥3:1) | WCAG 1.4.3: body text≥4.5:1; Google Material onSurfaceVariant=#49454F CR=6.1:1 |
| P1-2 | 🟠 | 色彩 | **Loading态副标题对比度仅1.9:1**。loading时输入框应用`.disabled { opacity: 0.5 }`，但副标题`<div>`不在disabled范围内却被整体opacity影响 | login_loading.png 副标题 | 将disabled opacity从0.5降至仅作用于输入框(用`.input-field.disabled input`)，不影响副标题；(或)loading态标题/副标题保持100% opacity | M3 loading: 仅按钮转圈+disabled，其他文字不变 |
| P1-3 | 🟠 | 可感知性 | **错误Card未通过像素检测确认渲染**。401/403/429/Timeout截图在预期error card区域(y~1450-1550)未检测到`#F9DEDC` errorContainer颜色 | error系列 错误Card区域 | 检查Playwright截图是否在`errorCard.classList.add('visible')` AND `updateButtonState()`之后截取；增加500ms等待动画完成 | AnimatedVisibility确保截图在动画完成后执行 |
| P1-4 | 🟠 | 视觉层级 | **Idle/Loading/email_error态标题对比度下降**。像素扫描显示标题区域darkest像素：idle=`#918D95`(CR 3.17)，loading=`#8C8A8E`(CR 3.33)，email_error=`#79747E`(CR 4.44)，均低于正常`#1C1B1F`(CR 16.69) | idle/loading/email_error 标题区 | 检查是否有全局opacity/disabled状态影响标题；标题div不应受input disabled影响；确认font-weight:700生效 | M3 headlineMedium: 24sp Bold(#1C1B1F), CR 16.7:1始终 |

### 🟡 P2 中优先级

| # | 严重度 | 维度 | 问题描述 | 截图位置 | 改进建议 | 参考案例 |
|---|:---:|------|----------|----------|----------|----------|
| P2-1 | 🟡 | 字体 | **副标题行高20sp/字号14sp=1.43**低于推荐的1.5-1.6倍，中文字符纵向偏紧 | 全部截图 副标题 | 调整bodyMedium line-height从20sp→22sp(1.57倍) | M3 bodyMedium: lineHeight=20sp(1.43x)为英文优化；中文建议22sp |
| P2-2 | 🟡 | 情感品牌 | **底部"测试账号: admin / 123456"提示破坏正式感**，且admin/123456为旧的username方案而非PRD的email方案 | 全部截图 底部提示区 | 生产构建移除测试提示；调试构建改为`labelSmall+"仅调试可见"+alpha 0.38` | 测试文案应在debug badge内或完全移除 |
| P2-3 | 🟡 | 一致性 | **暗色模式实现方式不一致**。login.html用JS inline style覆写phoneFrame；index.html用`@media(prefers-color-scheme)`。两套逻辑并存 | login.html L607-636 | 统一使用`@media(prefers-color-scheme: dark)`，删除JS手动覆写逻辑 | M3: prefers-color-scheme + M3 darkColorScheme() |
| P2-4 | 🟡 | 一致性 | **输入框圆角4dp(线框图=4dp)→线上常见M3为4dp**✅，但错误Card圆角12dp与按钮pill 24dp跨度大，圆形-圆角混合风格 | 全局 形状系统 | 考虑错误Card圆角统一为8dp或16dp，形成等比数列(4/8/16/24) | M3 shape: extraSmall=4, small=8, medium=12, large=16, extraLarge=28 |

### 🔵 P3 低优先级/建议

| # | 严重度 | 维度 | 问题描述 | 截图位置 | 改进建议 | 参考案例 |
|---|:---:|------|----------|----------|----------|----------|
| P3-1 | 🔵 | 平台适配 | **login_full_page.png尺寸1600×1800非标准**，无法对应实际设备 | login_full_page.png | 全页截图指定标准viewport 1280×800或1920×1080 | Playwright viewport: { width: 1280, height: 800 } |
| P3-2 | 🔵 | 视觉层级 | **VersionTag底部居中字号12sp**但无分割线与内容区分，在白色背景上可能被忽略(这其实是优点) | 全部截图 底部 | 当前设计合理——版本号应低调；可考虑在内容与版本号间加`<hr>`或padding | M3: tertiary信息用outline色+小字号 |
| P3-3 | 🔵 | 可感知性 | **输入框placeholder透明度0.6**(`opacity: 0.6`+`onSurfaceVariant`)，实际对比度约2.6:1，虽placeholders豁免WCAG但建议≥3:1 | 全部截图 输入框placeholder | placeholder opacity从0.6→0.7或使用outline色替代onSurfaceVariant | M3 placeholder: onSurfaceVariant at 0.6-0.7 opacity |

---

## 四、审美亮点

1. **M3 Token体系完整度极高**：颜色13项+字体8项+间距6项+形状8项全量定义，Light/Dark双模覆盖，HTML原型100% CSS变量驱动，维护成本极低
2. **8dp网格严格对齐**：spacing0/8/16/24/32五级间距形成清晰视觉节奏，弹性Spacer(weight=1f)实现垂直居中自适应
3. **视觉层级四层梯度分明**：24sp Bold标题→14sp副标题→16sp按钮→11sp提示，字号/字重/颜色三通道同时编码信息层级
4. **交互状态机完整**：6状态(Idle/Editing/Loading/Success/Error/Timeout)+9状态覆盖表，状态间转换路径清晰
5. **无障碍设计意识强**：17元素contentDescription/testTag/LiveRegion标注，密码可见性切换≥48dp触控目标
6. **错误态设计得体**：内联error Card+fadeIn动画+errorContainer背景色，不阻断操作流
7. **Phone frame渲染精致**：36dp圆角+双层边框+阴影模拟真实设备，截图展示效果好

---

## 五、综合评定

| 项目 | 值 |
|------|-----|
| **综合评分** | **34/50** |
| **评级** | ⚠️ 良好（≥30免重新设计，但色彩/平台适配需紧急修复） |
| **P0问题** | 2项（暗色模式失效、按钮文字未渲染） |
| **P1问题** | 4项（副标题对比度、loading对比度、错误Card验证、标题对比度波动） |
| **P2问题** | 4项（行高、测试文案、暗色实现、圆角一致性） |
| **P3建议** | 3项 |
| **登需重新设计** | ❌ 否（34≥30），但 **login_dark.png 单项23分需重拍** |

### 修复优先级路径
```
P0-1 暗色模式 → P0-2 按钮文字 → P1-1/P1-2 对比度 → P1-3 错误Card验证 → 重拍全部截图验证
```

---

## 六、附录：像素分析方法

- **工具**: Python 3.11 + Pillow 12.2
- **方法**: (1) 区域极值分析(亮/暗像素对比) → 验证文本渲染 (2) 垂直列扫描@4px间隔 → 检测文字笔画 (3) 色块识别 → 定位组件位置 (4) WCAG相对亮度+对比度计算
- **局限**: 750×1624 @2x截图存在子像素抗锯齿，文字色取样为混合值(非纯text color)；未使用视觉模型直接理解图像语义
- **完整分析数据**: [precise_analysis.json](./precise_analysis.json) | [analysis_results.json](./analysis_results.json)
