package com.whatever.caro.feature.card.detail

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.card_detail_back
import caromobile.core.designsystem.generated.resources.card_detail_delete
import caromobile.core.designsystem.generated.resources.card_detail_delete_body
import caromobile.core.designsystem.generated.resources.card_detail_delete_cancel
import caromobile.core.designsystem.generated.resources.card_detail_delete_confirm
import caromobile.core.designsystem.generated.resources.card_detail_delete_title
import caromobile.core.designsystem.generated.resources.card_detail_edit
import caromobile.core.designsystem.generated.resources.card_detail_flip_hint
import caromobile.core.designsystem.generated.resources.card_detail_more
import caromobile.core.designsystem.generated.resources.card_detail_next
import caromobile.core.designsystem.generated.resources.card_detail_previous
import caromobile.core.designsystem.generated.resources.ic_arrow_left_16
import caromobile.core.designsystem.generated.resources.ic_arrow_left_24
import caromobile.core.designsystem.generated.resources.ic_arrow_right_16
import caromobile.core.designsystem.generated.resources.ic_edit_24
import caromobile.core.designsystem.generated.resources.ic_trash_24
import com.whatever.caro.core.designsystem.components.CaroDialog
import com.whatever.caro.core.designsystem.components.CaroTopBar
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.card.CardBadge
import com.whatever.caro.core.model.card.CardContent
import com.whatever.caro.core.model.card.DeckCard
import com.whatever.caro.core.ui.card.CaroFlashCard
import com.whatever.caro.feature.card.detail.mvi.CardDetailIntent
import com.whatever.caro.feature.card.detail.mvi.CardDetailState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CardDetailScreen(
    state: CardDetailState,
    onIntent: (CardDetailIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(CaroTheme.color.background.primary),
    ) {
        when {
            state.cards.isEmpty() -> {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = CaroTheme.color.icon.primary,
                    )
                }
            }

            else -> {
                CardDetailContent(
                    cards = state.cards,
                    currentIndex = state.currentIndex,
                    isFlipped = state.isFlipped,
                    onIntent = onIntent,
                )
            }
        }
    }

    if (state.isDeleteDialogVisible) {
        CardDetailDeleteDialog(
            isDeleting = state.isDeleting,
            onDelete = { onIntent(CardDetailIntent.ConfirmDelete) },
            onCancel = { onIntent(CardDetailIntent.DismissDeleteDialog) },
        )
    }
}

@Composable
private fun CardDetailContent(
    cards: ImmutableList<DeckCard>,
    currentIndex: Int,
    isFlipped: Boolean,
    onIntent: (CardDetailIntent) -> Unit,
) {
    val cardCount = cards.size
    val cardIds = remember(cards) { cards.map(DeckCard::id) }
    val currentOnIntent by rememberUpdatedState(onIntent)
    val flipHint = stringResource(Res.string.card_detail_flip_hint)
    val moreLabel = stringResource(Res.string.card_detail_more)
    val pagerState =
        key(cardIds) {
            rememberPagerState(
                initialPage = cardDetailInitialPage(currentIndex, cardCount),
                pageCount = { if (cardCount > 1) Int.MAX_VALUE else 1 },
            )
        }
    val coroutineScope = rememberCoroutineScope()
    val canNavigate = cardCount > 1 && !pagerState.isScrollInProgress
    val flippedCardId = cards.getOrNull(currentIndex)?.id?.takeIf { isFlipped }

    LaunchedEffect(pagerState, cardCount) {
        snapshotFlow { pagerState.settledPage }
            .map { page -> cardDetailIndexForPage(page, cardCount) }
            .distinctUntilChanged()
            .collect { index -> currentOnIntent(CardDetailIntent.ChangeCard(index)) }
    }

    Column(Modifier.fillMaxSize()) {
        CardDetailTopBar(
            current = currentIndex + 1,
            total = cardCount,
            onBack = { currentOnIntent(CardDetailIntent.ClickBack) },
            onEdit = { currentOnIntent(CardDetailIntent.ClickEdit) },
            onDelete = { currentOnIntent(CardDetailIntent.ClickDelete) },
        )
        CardDetailPager(
            cards = cards,
            state = pagerState,
            flippedCardId = flippedCardId,
            flipHint = flipHint,
            moreLabel = moreLabel,
            onFlip = { currentOnIntent(CardDetailIntent.FlipCard) },
            modifier = Modifier.weight(1f),
        )
        CardDetailBottomBar(
            enabled = canNavigate,
            onPrevious = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(
                        page = pagerState.currentPage - 1,
                        animationSpec = CardDetailPageAnimationSpec,
                    )
                }
            },
            onNext = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(
                        page = pagerState.currentPage + 1,
                        animationSpec = CardDetailPageAnimationSpec,
                    )
                }
            },
        )
    }
}

@Composable
private fun CardDetailPager(
    cards: ImmutableList<DeckCard>,
    state: PagerState,
    flippedCardId: Long?,
    flipHint: String,
    moreLabel: String,
    onFlip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardCount = cards.size

    HorizontalPager(
        state = state,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = CardDetailCardVerticalPadding),
        contentPadding = PaddingValues(horizontal = CaroTheme.spacing.xl),
        pageSpacing = CaroTheme.spacing.xl,
    ) { page ->
        val card = cards[cardDetailIndexForPage(page, cardCount)]
        CaroFlashCard(
            frontText = card.content.front,
            backText = card.content.back,
            isFlipped = card.id == flippedCardId,
            flipHint = flipHint,
            moreLabel = moreLabel,
            onFlip = {
                if (page == state.settledPage) {
                    onFlip()
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun CardDetailTopBar(
    current: Int,
    total: Int,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    CaroTopBar(
        modifier = Modifier.padding(horizontal = CaroTheme.spacing.xl2),
        leadingContent = {
            CardDetailIcon(
                drawable = Res.drawable.ic_arrow_left_24,
                contentDescription = stringResource(Res.string.card_detail_back),
                tint = CaroTheme.color.icon.primary,
                onClick = onBack,
            )
        },
        centerContent = {
            Box(
                modifier =
                    Modifier
                        .background(CaroTheme.color.surface.tertiary, CaroTheme.shape.xxl)
                        .padding(
                            horizontal = CaroTheme.spacing.l,
                            vertical = CardDetailProgressVerticalPadding,
                        ),
            ) {
                Crossfade(
                    targetState = current,
                    animationSpec =
                        tween(
                            durationMillis = CARD_DETAIL_PROGRESS_FADE_DURATION_MILLIS,
                            easing = FastOutSlowInEasing,
                        ),
                    label = "card_detail_progress",
                ) { animatedCurrent ->
                    Text(
                        text = "$animatedCurrent / $total",
                        style = CaroTheme.typography.caption1.regular,
                        color = CaroTheme.color.text.secondary,
                    )
                }
            }
        },
        trailingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s)) {
                CardDetailIcon(
                    drawable = Res.drawable.ic_edit_24,
                    contentDescription = stringResource(Res.string.card_detail_edit),
                    tint = CaroTheme.color.icon.brand,
                    onClick = onEdit,
                )
                CardDetailIcon(
                    drawable = Res.drawable.ic_trash_24,
                    contentDescription = stringResource(Res.string.card_detail_delete),
                    tint = CaroTheme.color.icon.brand,
                    onClick = onDelete,
                )
            }
        },
    )
}

@Composable
private fun CardDetailIcon(
    drawable: DrawableResource,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Icon(
        painter = painterResource(drawable),
        contentDescription = contentDescription,
        modifier =
            Modifier
                .size(CardDetailIconSize)
                .clickable(
                    role = Role.Button,
                    onClick = onClick,
                ),
        tint = tint,
    )
}

@Composable
private fun CardDetailBottomBar(
    enabled: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(CardDetailBottomBarHeight)
                .padding(horizontal = CaroTheme.spacing.xl, vertical = CaroTheme.spacing.m),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CardDetailPageButton(
            text = stringResource(Res.string.card_detail_previous),
            icon = Res.drawable.ic_arrow_left_16,
            iconAtStart = true,
            enabled = enabled,
            onClick = onPrevious,
        )
        CardDetailPageButton(
            text = stringResource(Res.string.card_detail_next),
            icon = Res.drawable.ic_arrow_right_16,
            iconAtStart = false,
            enabled = enabled,
            onClick = onNext,
        )
    }
}

@Composable
private fun CardDetailPageButton(
    text: String,
    icon: DrawableResource,
    iconAtStart: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .height(CardDetailPageButtonHeight)
                .clip(CaroTheme.shape.xxl)
                .background(CaroTheme.color.surface.primary)
                .border(CardDetailBorderWidth, CaroTheme.color.border.secondary, CaroTheme.shape.xxl)
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                ).padding(horizontal = CaroTheme.spacing.m),
        horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconAtStart) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(CardDetailArrowIconSize),
                tint = CaroTheme.color.icon.primary,
            )
        }
        Text(
            text = text,
            style = CaroTheme.typography.body2.semiBold,
            color = CaroTheme.color.text.primary,
        )
        if (!iconAtStart) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(CardDetailArrowIconSize),
                tint = CaroTheme.color.icon.primary,
            )
        }
    }
}

@Composable
private fun CardDetailDeleteDialog(
    isDeleting: Boolean,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
) {
    CaroDialog(
        onDismissRequest = { if (!isDeleting) onCancel() },
        title = {
            Text(
                text = stringResource(Res.string.card_detail_delete_title),
                modifier = Modifier.fillMaxWidth(),
                style = CaroTheme.typography.heading2,
                color = CaroTheme.color.text.primary,
            )
        },
        content = {
            Spacer(Modifier.height(CaroTheme.spacing.s))
            Text(
                text = stringResource(Res.string.card_detail_delete_body),
                modifier = Modifier.fillMaxWidth(),
                style = CaroTheme.typography.body2.medium,
                color = CaroTheme.color.text.secondary,
            )
            Spacer(Modifier.height(CaroTheme.spacing.l))
        },
        buttons = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
            ) {
                CardDetailDialogButton(
                    text = stringResource(Res.string.card_detail_delete_confirm),
                    backgroundColor = CaroTheme.color.surface.dangerous,
                    textColor = CaroTheme.color.text.dangerous,
                    enabled = !isDeleting,
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                )
                CardDetailDialogButton(
                    text = stringResource(Res.string.card_detail_delete_cancel),
                    backgroundColor = CaroTheme.color.surface.tertiary,
                    textColor = CaroTheme.color.text.brand,
                    enabled = !isDeleting,
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                )
            }
        },
    )
}

@Composable
private fun CardDetailDialogButton(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .height(CardDetailDialogButtonHeight)
                .clip(CaroTheme.shape.xxl)
                .background(backgroundColor)
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = CaroTheme.typography.caption1.regular,
            color = textColor,
            textAlign = TextAlign.Center,
        )
    }
}

internal fun cardDetailIndexForPage(
    page: Int,
    cardCount: Int,
): Int = if (cardCount > 0) page % cardCount else 0

internal fun cardDetailInitialPage(
    currentIndex: Int,
    cardCount: Int,
): Int {
    if (cardCount <= 1) return 0
    val anchor = Int.MAX_VALUE / 2
    return anchor - (anchor % cardCount) + currentIndex.coerceIn(0, cardCount - 1)
}

@Preview(locale = "ko", widthDp = 402, heightDp = 790)
@Composable
private fun CardDetailScreenPreview() {
    CaroTheme {
        CardDetailScreen(
            state =
                CardDetailState(
                    cards =
                        persistentListOf(
                            DeckCard(
                                id = 1,
                                content =
                                    CardContent(
                                        front = "What is Jetpack Compose?",
                                        back = "Android의 선언형 UI 툴킷",
                                    ),
                                badge = CardBadge.NEW,
                                reviewCount = 0,
                            ),
                            DeckCard(
                                id = 2,
                                content = CardContent(front = "Kotlin", back = "코틀린"),
                                badge = CardBadge.REVIEW,
                                reviewCount = 3,
                            ),
                        ),
                ),
            onIntent = {},
        )
    }
}

private val CardDetailCardVerticalPadding = 20.dp
private val CardDetailProgressVerticalPadding = 6.dp
private val CardDetailBottomBarHeight = 65.dp
private val CardDetailPageButtonHeight = 41.dp
private val CardDetailDialogButtonHeight = 38.dp
private val CardDetailIconSize = 24.dp
private val CardDetailArrowIconSize = 16.dp
private val CardDetailBorderWidth = 1.dp
private const val CARD_DETAIL_PAGE_ANIMATION_DURATION_MILLIS = 280
private const val CARD_DETAIL_PROGRESS_FADE_DURATION_MILLIS = 140
private val CardDetailPageAnimationSpec =
    tween<Float>(
        durationMillis = CARD_DETAIL_PAGE_ANIMATION_DURATION_MILLIS,
        easing = FastOutSlowInEasing,
    )
