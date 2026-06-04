package com.example.myandroidapp.ui.news

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.myandroidapp.ui.components.ErrorState
import com.example.myandroidapp.ui.components.NewsDimens
import com.example.myandroidapp.ui.components.ShimmerCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsDetailScreen(
    articleId: String,
    onBack: () -> Unit,
    viewModel: NewsDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(articleId) {
        viewModel.loadDetail(articleId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("新闻详情") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("detailBackButton")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        modifier = Modifier.testTag("newsDetailScreen")
    ) { padding ->
        when (uiState) {
            is DetailUiState.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(NewsDimens.CardPadding)
                ) {
                    ShimmerCard(testTag = "shimmerDetail")
                }
            }
            is DetailUiState.Success -> {
                val article = (uiState as DetailUiState.Success).article
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (!article.urlToImage.isNullOrBlank()) {
                        AsyncImage(
                            model = article.urlToImage,
                            contentDescription = article.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(NewsDimens.DetailImageHeight),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Column(modifier = Modifier.padding(NewsDimens.CardPadding)) {
                        Text(
                            text = article.title,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.height(NewsDimens.CardGap))
                        Row {
                            Text(
                                text = article.sourceName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(NewsDimens.CardGap))
                            Text(text = "·", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(NewsDimens.CardGap))
                            Text(
                                text = article.publishedAt,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(NewsDimens.CardPadding))
                        if (!article.description.isNullOrBlank()) {
                            Text(
                                text = article.description,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Spacer(modifier = Modifier.height(NewsDimens.DetailBottomSpacing))
                        FilledTonalButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.url))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.testTag("readOriginalButton")
                        ) {
                            Text("阅读原文")
                        }
                    }
                }
            }
            is DetailUiState.Error -> {
                ErrorState(
                    message = (uiState as DetailUiState.Error).message,
                    onRetry = { viewModel.loadDetail(articleId) },
                    testTag = "detailErrorState",
                    modifier = Modifier.fillMaxSize().padding(padding)
                )
            }
        }
    }
}
