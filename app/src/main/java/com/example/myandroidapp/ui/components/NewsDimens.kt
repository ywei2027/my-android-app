package com.example.myandroidapp.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * News App 专用设计 Token — 对齐 PRD §9 设计约束。
 * 所有尺寸、圆角、阴影值集中管理，确保 UI 组件间尺寸同步。
 */
object NewsDimens {
    // ===== 卡片 =====
    /** 卡片阴影高度 (PRD §9: 1dp) */
    val CardElevation: Dp = 1.dp
    /** 卡片圆角 (PRD §9: 12dp) */
    val CardCorner: Dp = 12.dp
    /** 卡片内边距 (原型: 12dp) */
    val CardInnerPadding: Dp = 12.dp
    /** 容器/列表水平内边距 */
    val CardPadding: Dp = 16.dp
    /** 卡片文字元素间距 */
    val CardSpacing: Dp = 4.dp
    /** 卡片间垂直间距 */
    val CardGap: Dp = 8.dp

    // ===== 缩略图 =====
    /** 卡片缩略图宽度 (PRD §9: 80dp) */
    val CardThumbnailWidth: Dp = 80.dp
    /** 卡片缩略图高度 (PRD §9: 60dp) */
    val CardThumbnailHeight: Dp = 60.dp
    /** 缩略图圆角 (PRD §9: 8dp) */
    val CardThumbnailCorner: Dp = 8.dp

    // ===== 骨架屏 =====
    /** 骨架屏缩略图宽度 (与真实卡片一致) */
    val ShimmerThumbnailWidth: Dp = 80.dp
    /** 骨架屏缩略图高度 (与真实卡片一致) */
    val ShimmerThumbnailHeight: Dp = 60.dp
    /** 骨架屏内容行间距 (原型: 8dp) */
    val ShimmerLineGap: Dp = 8.dp
    /** 骨架屏标题行1高度 (原型: 16dp, 85%) */
    val ShimmerTitle1Height: Dp = 16.dp
    /** 骨架屏标题行2高度 (原型: 16dp, 60%) */
    val ShimmerTitle2Height: Dp = 16.dp
    /** 骨架屏元信息行高度 (原型: 12dp, 40%) */
    val ShimmerMetaHeight: Dp = 12.dp

    // ===== 详情页 =====
    /** 详情页头图高度 */
    val DetailImageHeight: Dp = 200.dp
    /** 详情页底部留白（阅读原文按钮上方） */
    val DetailBottomSpacing: Dp = 24.dp

    // ===== 状态页 =====
    /** 图标尺寸（Empty/Error 状态） */
    val StateIconSize: Dp = 64.dp
    /** 状态页内边距 */
    val StatePadding: Dp = 32.dp

    // ===== 形状预计算 =====
    /** 卡片圆角 Shape */
    val CardShape: Shape = RoundedCornerShape(CardCorner)
    /** 缩略图圆角 Shape */
    val ThumbnailShape: Shape = RoundedCornerShape(CardThumbnailCorner)
}
