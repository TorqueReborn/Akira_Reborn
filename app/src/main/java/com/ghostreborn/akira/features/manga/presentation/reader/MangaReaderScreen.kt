package com.ghostreborn.akira.features.manga.presentation.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ghostreborn.akira.features.manga.models.MangaPage
import com.ghostreborn.akira.features.manga.models.ReadingMode

@Composable
fun MangaReaderScreen(
    mangaId: String,
    chapterString: String,
    translationType: String = "sub",
    onNavigateBack: () -> Unit,
    viewModel: MangaReaderViewModel = viewModel(factory = MangaReaderViewModel.provideFactory())
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(mangaId, chapterString) {
        viewModel.loadChapter(context, mangaId, chapterString, translationType)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        when (val state = uiState) {
            is MangaReaderUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Loading Chapter $chapterString...",
                            color = Color.White,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
            }
            is MangaReaderUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Button(
                            onClick = { viewModel.loadChapter(context, mangaId, chapterString, translationType) },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Text("Retry", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
            is MangaReaderUiState.Success -> {
                ReaderContent(
                    state = state,
                    onToggleControls = { viewModel.toggleControls() },
                    onNavigateBack = onNavigateBack,
                    onPageChanged = { viewModel.onPageChanged(context, it) },
                    onModeChanged = { viewModel.setReadingMode(it) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderContent(
    state: MangaReaderUiState.Success,
    onToggleControls: () -> Unit,
    onNavigateBack: () -> Unit,
    onPageChanged: (Int) -> Unit,
    onModeChanged: (ReadingMode) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onToggleControls() }
    ) {
        when (state.readingMode) {
            ReadingMode.WEBTOON -> {
                WebtoonView(
                    pages = state.pages,
                    onPageVisible = onPageChanged
                )
            }
            ReadingMode.PAGED_LTR, ReadingMode.PAGED_RTL -> {
                PagedView(
                    pages = state.pages,
                    currentPage = state.currentPage,
                    isRtl = state.readingMode == ReadingMode.PAGED_RTL,
                    onPageChanged = onPageChanged
                )
            }
        }

        // Top bar overlay
        AnimatedVisibility(
            visible = state.showControls,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            TopAppBar(
                title = { Text("Chapter ${state.chapterString}", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val nextMode = when (state.readingMode) {
                            ReadingMode.WEBTOON -> ReadingMode.PAGED_LTR
                            ReadingMode.PAGED_LTR -> ReadingMode.PAGED_RTL
                            ReadingMode.PAGED_RTL -> ReadingMode.WEBTOON
                        }
                        onModeChanged(nextMode)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Change Mode",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.8f)
                ),
                modifier = Modifier.statusBarsPadding()
            )
        }

        // Bottom page indicator
        AnimatedVisibility(
            visible = state.showControls,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${state.currentPage} / ${state.pages.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun WebtoonView(
    pages: List<MangaPage>,
    onPageVisible: (Int) -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index -> onPageVisible(index + 1) }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(pages) { _, page ->
            MangaPageItem(page = page)
        }
    }
}

@Composable
private fun PagedView(
    pages: List<MangaPage>,
    currentPage: Int,
    isRtl: Boolean,
    onPageChanged: (Int) -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = (currentPage - 1).coerceIn(0, pages.size - 1),
        pageCount = { pages.size }
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .collect { index -> onPageChanged(index + 1) }
    }

    HorizontalPager(
        state = pagerState,
        reverseLayout = isRtl,
        modifier = Modifier.fillMaxSize()
    ) { pageIndex ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            MangaPageItem(page = pages[pageIndex])
        }
    }
}

@Composable
private fun MangaPageItem(page: MangaPage) {
    val context = LocalContext.current
    val imageRequest = remember(page.imageUrl) {
        ImageRequest.Builder(context)
            .data(page.imageUrl)
            .crossfade(true)
            .apply {
                page.headers.forEach { (k, v) -> addHeader(k, v) }
            }
            .build()
    }

    AsyncImage(
        model = imageRequest,
        contentDescription = "Page ${page.pageNumber}",
        modifier = Modifier.fillMaxWidth(),
        contentScale = ContentScale.FillWidth
    )
}
