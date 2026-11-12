package com.whatever.caro.feature.home.banner

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.home_banner_page
import caromobile.core.designsystem.generated.resources.home_banner_position
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import coil3.compose.asPainter
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.whatever.caro.core.data.util.suspendRunCatching
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.banner.HomeBanner
import com.whatever.caro.feature.home.library.HomeDeckHeader
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

private const val AUTO_ADVANCE_INTERVAL_MILLIS = 5_000L
private const val IMAGE_PREPARATION_TIMEOUT_MILLIS = 8_000L

@Immutable
private data class LoadedBanner(
    val banner: HomeBanner,
    val painter: Painter,
)

@Composable
internal fun HomeBannerSection(
    banners: ImmutableList<HomeBanner>,
    isVisibleAndIdle: Boolean,
    autoAdvanceStopped: Boolean,
    selectedBannerKey: String?,
    onStopAutoAdvance: () -> Unit,
    onSelectedBanner: (String) -> Unit,
    onBannerClick: (HomeBanner) -> Unit,
    onLibraryClick: () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().padding(horizontal = CaroTheme.spacing.xl2).padding(top = CaroTheme.spacing.xl2),
    ) {
        val context = LocalPlatformContext.current
        val imageLoader = remember(context) { SingletonImageLoader.get(context) }
        val imageWidth = with(LocalDensity.current) { maxWidth.roundToPx() }.coerceAtLeast(1)
        val imageHeight = (imageWidth / 3f).toInt().coerceAtLeast(1)
        val loaded by produceState(emptyList<LoadedBanner>(), banners, context, imageWidth) {
            value = emptyList()
            coroutineScope {
                val loadedByIndex = arrayOfNulls<LoadedBanner>(banners.size)
                banners.forEachIndexed { index, banner ->
                    launch {
                        val cacheKey = "${banner.key}:${banner.imageUrl}"
                        val request =
                            ImageRequest
                                .Builder(context)
                                .data(banner.imageUrl)
                                .memoryCacheKey("$cacheKey:$imageWidth:$imageHeight")
                                .diskCacheKey(cacheKey)
                                .size(imageWidth, imageHeight)
                                .build()
                        val result =
                            withTimeoutOrNull(IMAGE_PREPARATION_TIMEOUT_MILLIS) {
                                suspendRunCatching { imageLoader.execute(request) }.getOrNull()
                            }
                        if (result is SuccessResult) {
                            loadedByIndex[index] = LoadedBanner(banner, result.image.asPainter(context))
                            value = loadedByIndex.filterNotNull()
                        }
                    }
                }
            }
        }
        val now by produceState(Clock.System.now(), banners) {
            while (true) {
                value = Clock.System.now()
                val nextExpiry = banners.map { it.expiresAt }.filter { it > value }.minOrNull() ?: break
                delay((nextExpiry - value).inWholeMilliseconds.coerceAtLeast(1))
            }
        }
        val visible = loaded.filter { it.banner.expiresAt > now }.toImmutableList()
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m),
        ) {
            if (visible.isNotEmpty()) {
                BannerPager(
                    items = visible,
                    isVisibleAndIdle = isVisibleAndIdle,
                    autoAdvanceStopped = autoAdvanceStopped,
                    selectedBannerKey = selectedBannerKey,
                    onStopAutoAdvance = onStopAutoAdvance,
                    onSelectedBanner = onSelectedBanner,
                    onBannerClick = onBannerClick,
                )
            }
            HomeDeckHeader(onLibraryClick)
        }
    }
}

@Composable
private fun BannerPager(
    items: ImmutableList<LoadedBanner>,
    isVisibleAndIdle: Boolean,
    autoAdvanceStopped: Boolean,
    selectedBannerKey: String?,
    onStopAutoAdvance: () -> Unit,
    onSelectedBanner: (String) -> Unit,
    onBannerClick: (HomeBanner) -> Unit,
) {
    val keys = items.map { it.banner.key }
    val pager = rememberPagerState(initialPage = keys.indexOf(selectedBannerKey).coerceAtLeast(0)) { items.size }
    val dragged by pager.interactionSource.collectIsDraggedAsState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val accessibilityAllowsAutoAdvance = rememberBannerAutoAdvanceAllowed()
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    LaunchedEffect(dragged, accessibilityAllowsAutoAdvance) {
        if (dragged || !accessibilityAllowsAutoAdvance) onStopAutoAdvance()
    }
    LaunchedEffect(pager, keys) {
        snapshotFlow { pager.settledPage }.collect { page -> keys.getOrNull(page)?.let(onSelectedBanner) }
    }
    val canAdvance =
        items.size > 1 && !autoAdvanceStopped && !dragged && !pressed &&
            accessibilityAllowsAutoAdvance && isVisibleAndIdle && lifecycle.isAtLeast(Lifecycle.State.RESUMED)
    LaunchedEffect(canAdvance, keys) {
        if (canAdvance) {
            while (true) {
                delay(AUTO_ADVANCE_INTERVAL_MILLIS)
                pager.animateScrollToPage((pager.settledPage + 1) % items.size)
            }
        }
    }
    Box(Modifier.fillMaxWidth().aspectRatio(3f).clip(CaroTheme.shape.l)) {
        HorizontalPager(state = pager, key = { keys[it] }, modifier = Modifier.fillMaxSize(), userScrollEnabled = items.size > 1) { page ->
            val item = items[page]
            val position = stringResource(Res.string.home_banner_page, page + 1, items.size)
            Image(
                painter = item.painter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .semantics {
                            contentDescription = item.banner.accessibleLabel
                            stateDescription = position
                        }.onFocusChanged { if (it.isFocused) onStopAutoAdvance() }
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            enabled = !pager.isScrollInProgress,
                            role = Role.Button,
                        ) { if (item.banner.expiresAt > Clock.System.now()) onBannerClick(item.banner) },
            )
        }
        if (items.size > 1) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(44.dp, 24.dp)
                        .clip(CaroTheme.shape.l)
                        .background(Color.Black.copy(alpha = 0.52f))
                        .clearAndSetSemantics {},
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.home_banner_position, pager.settledPage.coerceAtMost(items.lastIndex) + 1, items.size),
                    style = CaroTheme.typography.caption1.regular,
                    color = Color.White,
                )
            }
        }
    }
}
