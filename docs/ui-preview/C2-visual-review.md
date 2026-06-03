# C2 视觉审美轻量评审 — main_default.html

**评审对象**: `docs/ui-preview/main_default.html` (375×812, M3 CSS变量, 含 @media dark)  
**评审日期**: 2026-06-03  
**评审范围**: 10维度视觉审美 (1-5分制) + 详细问题 + 审美亮点 + 综合/50

---

## 评分总览表

| # | 维度 | 评分 | 关键词 |
|---|------|:---:|--------|
| 1 | 格式塔 (Gestalt) | 3 | 分组明确但 nav-bar 空洞破坏闭合 |
| 2 | 视觉层级 (Visual Hierarchy) | 4 | logo→欢迎语→表单→CTA 线性清晰 |
| 3 | 色彩 (Color) | 2 | M3 tokens 规范但 version-tag 对比度双双不达标 |
| 4 | 字体 (Typography) | 3 | Roboto 体系一致但 version-tag 突兀用 monospace |
| 5 | 空间 (Spacing) | 4 | 呼吸感良好，padding/margin 合理 |
| 6 | 布局 (Layout) | 4 | flex 居中+column 简洁有效 |
| 7 | 可感知性 (Perceivability) | 2 | version-tag 可读性不足，无 focus 态 |
| 8 | 一致性 (Consistency) | 4 | M3 token 全局引用，设计语言统一 |
| 9 | 情感/品牌 (Emotion & Brand) | 3 | 亲和但中性，缺品牌记忆点 |
| 10 | 平台适配 (Platform Adaptation) | 4 | dark mode 已覆盖，安全区/notch 未处理 |

| **综合** | **33/50** | **中偏下，version-tag 对比度是硬伤** |
|-----------|:---------:|--------------------------------------|

---

## 详细问题清单

### 🔴 P0 — 硬伤

| # | 问题 | 证据 | 影响维度 | PRD 依据 |
|---|------|------|:---:|------|
| P0-1 | **VersionTag 对比度不达标** | Light: `onSurfaceVariant #49454F @0.6 → 有效色 #918D95, CR=3.2:1` (需≥4.5:1)；Dark: `#CAC4D0 @0.6 → #848089, CR=4.4:1` (临界失败) | 色彩/可感知性 | PRD §9: 对比度≥4.5:1 |
| P0-2 | **Dark mode 背景即 #1C1B1F 非纯黑**，符合约束但版本号仍差 0.1 达标 | — | 色彩 | PRD §9: 若背景#000需提亮 (不适用) |

### 🟡 P1 — 重要

| # | 问题 | 证据 | 影响维度 |
|---|------|------|:---:|
| P1-1 | **nav-bar 34px 纯透明无内容**，形成视觉空洞 | `.nav-bar { height: 34px; background: transparent; }` 无任何子元素 | 格式塔/空间 |
| P1-2 | **VersionTag 字体为 monospace**，与全局 Roboto/Segoe UI 不统一 | `font-family: monospace` vs body 的 `'Roboto', 'Segoe UI', system-ui` | 字体/一致性 |
| P1-3 | **无 :focus 可见指示器** | 所有交互元素(input/button/link)缺少 `:focus-visible` 样式 | 可感知性 |
| P1-4 | **placeholder 文字未使用 opacity**，与 version-tag 的 `onSurfaceVariant` 用法不一致 | `.input-field::placeholder { color: var(--onSurfaceVariant); }` — 无透明度，而 version-tag 有 `opacity: 0.6` | 一致性 |

### 🔵 P2 — 建议

| # | 问题 | 证据 | 影响维度 |
|---|------|------|:---:|
| P2-1 | **Status bar 为硬编码 "9:41 📶 🔋"**，非动态系统渲染 | 静态文本，无实际状态栏语义 | 平台适配 |
| P2-2 | **Logo 仅为 📱 emoji**，无品牌定制图形 | `<span class="logo-icon">📱</span>` | 情感/品牌 |
| P2-3 | **Phone frame 圆角 44px 偏大**，与主流 Android 设备 (≈28-36px) 略有偏差 | `border-radius: 44px` | 平台适配 |
| P2-4 | **按钮无 hover 以外状态** (active/pressed 缺失) | 仅 `.login-btn:hover { opacity: 0.9; }` | 可感知性 |

---

## 审美亮点

- ✨ **M3 Design Token 体系完整**：14 个 CSS 变量覆盖 primary/secondary/tertiary/surface/error + dark mode 翻转，设计工程化程度高
- ✨ **视觉层级清晰**：logo→欢迎语→用户名→密码→CTA→关于 的 F-pattern 符合登录页预期
- ✨ **呼吸感控制得当**：`padding: 24px` + 各元素 `margin-bottom: 12-32px` 节奏稳定
- ✨ **Dark mode 自动适配**：`@media (prefers-color-scheme: dark)` 跟随系统，M3 token 对照合理
- ✨ **Phone-frame 预览形式**：box-shadow + border-radius 模拟真机，适合设计评审

---

## 修复建议速查

| 问题 | 修复方向 |
|------|----------|
| P0-1 对比度 | `opacity: 0.6` → `opacity: 0.75` (Light 可达 CR≈4.6, Dark≈5.6) 或改用 `color-mix(in srgb, var(--onSurfaceVariant) 75%, var(--background) 25%)` |
| P1-1 空洞 | 移除 `.nav-bar` 或改为 gesture indicator (5px 横条) |
| P1-2 字体 | `.version-tag` 改 `font-family: inherit` 保持一致性 |
| P1-3 focus | 添加 `.input-field:focus-visible { outline: 2px solid var(--primary); outline-offset: 2px; }` |
| P2-2 Logo | 考虑 SVG 或 Adaptive Icon 占位，而非 emoji |

---

*评审依据：W3C WCAG 2.1 contrast ratio 公式、Material Design 3 token 规范、PRD §9 设计约束*
