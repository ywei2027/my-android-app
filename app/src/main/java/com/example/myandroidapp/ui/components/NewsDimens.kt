package com.example.myandroidapp.ui.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * News App 专用设计 Token — B2-P0 修复：消除硬编码尺寸。
 * 共享常量确保 NewsCard ↔ ShimmerCard ↔ NewsDetailScreen 尺寸同步。
 */
object NewsDimens {
    /** 卡片缩略图高度（NewsCard + ShimmerCard 共享） */
    val CardImageHeight: Dp = 180.dp

    /** 详情页头图高度 */
    val DetailImageHeight: Dp = 200.dp

    /** 骨架屏标题占位高度 */
    val ShimmerTitleHeight: Dp = 20.dp

    /** 骨架屏副标题占位高度 */
    val ShimmerSubtitleHeight: Dp = 14.dp

    /** 骨架屏描述占位高度 */
    val ShimmerBodyHeight: Dp = 40.dp

    /** 标准卡片内边距 */
    val CardPadding: Dp = 16.dp

    /** 卡片元素间距 */
    val CardSpacing: Dp = 4.dp

    /** 卡片间垂直间距 */
    val CardGap: Dp = 8.dp

    /** 图标尺寸（Empty/Error 状态） */
    val StateIconSize: Dp = 64.dp

    /** 状态页内边距 */
    val StatePadding: Dp = 32.dp
}
