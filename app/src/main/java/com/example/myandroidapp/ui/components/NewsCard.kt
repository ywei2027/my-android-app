package com.example.myandroidapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.myandroidapp.domain.model.NewsArticle

/**
 * 新闻卡片 — PRD §9 规格：
 *   Row[缩略图 80×60dp 圆角 8dp | Column[标题 16sp maxLines=2 | 来源 12sp | 描述 14sp]]
 *   Card 圆角 12dp, elevation=1dp
 *   图片三态 fallback: loading(shimmer) / error(broken-icon) / null(纯色)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsCard(
    article: NewsArticle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("newsCard")
            .semantics {
                contentDescription = "《${article.title}》，${article.sourceName}"
            },
        shape = NewsDimens.CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = NewsDimens.CardElevation)
    ) {
        Row(
            modifier = Modifier.padding(NewsDimens.CardPadding),
            verticalAlignment = Alignment.Top
        ) {
            // 左侧缩略图 80×60dp — PRD §9 三态 fallback
            ThumbnailImage(
                url = article.urlToImage,
                contentDescription = article.title
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 右侧文字区
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(NewsDimens.CardSpacing))
                Text(
                    text = "${article.sourceName} · ${article.publishedAt}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!article.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(NewsDimens.CardSpacing))
                    Text(
                        text = article.description,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 缩略图组件 — 三态 fallback：
 * - 有 URL：Coil SubcomposeAsyncImage (loading→shimmer / error→broken-icon 灰色占位)
 * - 无 URL：纯色占位
 */
@Composable
private fun ThumbnailImage(url: String?, contentDescription: String?) {
    val context = LocalContext.current
    val size = Modifier
        .width(NewsDimens.CardThumbnailWidth)
        .height(NewsDimens.CardThumbnailHeight)
        .clip(NewsDimens.ThumbnailShape)

    if (!url.isNullOrBlank()) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(url)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            modifier = size,
            contentScale = ContentScale.Crop,
            loading = {
                Box(
                    Modifier
                        .matchParentSize()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            NewsDimens.ThumbnailShape
                        )
                )
            },
            error = {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            NewsDimens.ThumbnailShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.BrokenImage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.width(24.dp).height(24.dp)
                    )
                }
            }
        )
    } else {
        Box(
            modifier = size
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    NewsDimens.ThumbnailShape
                )
        )
    }
}
