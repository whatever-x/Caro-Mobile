package com.whatever.caro.feature.card

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.card_action_edit
import caromobile.core.designsystem.generated.resources.card_button_add
import caromobile.core.designsystem.generated.resources.card_content_description_back
import caromobile.core.designsystem.generated.resources.card_field_label_back
import caromobile.core.designsystem.generated.resources.card_field_label_front
import caromobile.core.designsystem.generated.resources.card_list_empty
import caromobile.core.designsystem.generated.resources.card_list_error
import caromobile.core.designsystem.generated.resources.card_list_retry
import caromobile.core.designsystem.generated.resources.card_list_title
import caromobile.core.designsystem.generated.resources.ic_add_24
import caromobile.core.designsystem.generated.resources.ic_chevron_left_24
import com.whatever.caro.core.designsystem.components.CaroTopBar
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.card.Card
import com.whatever.caro.core.model.card.CardContent
import com.whatever.caro.feature.card.mvi.DeckCardsIntent
import com.whatever.caro.feature.card.mvi.DeckCardsState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

private val PageHorizontalPadding = 28.dp
private val TopBarIconSize = 24.dp
private val CardMinHeight = 120.dp
private val SwipeActionWidth = 132.dp
private val FloatingButtonBottomPadding = 24.dp
private val FloatingButtonMinHeight = 52.dp
private val DividerHeight = 1.dp
private const val SWIPE_SETTLE_THRESHOLD = 0.45f
private val SwipeActionBackground = Color(0xFF6DFD87)
private val SWIPE_OFFSET_ANIMATION_SPEC =
    tween<Float>(
        durationMillis = 160,
        easing = FastOutSlowInEasing,
    )

@Composable
internal fun DeckCardsScreen(
    state: DeckCardsState,
    onIntent: (DeckCardsIntent) -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(CaroTheme.color.background.primary),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            CaroTopBar(
                modifier = Modifier.padding(horizontal = CaroTheme.spacing.xl),
                leadingContent = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            modifier =
                                Modifier
                                    .size(TopBarIconSize)
                                    .clickable { onIntent(DeckCardsIntent.ClickBack) },
                            painter = painterResource(Res.drawable.ic_chevron_left_24),
                            contentDescription = stringResource(Res.string.card_content_description_back),
                            tint = CaroTheme.color.icon.brand,
                        )
                        Text(
                            text = state.deckTitle.ifBlank { stringResource(Res.string.card_list_title) },
                            style = CaroTheme.typography.heading2,
                            color = CaroTheme.color.text.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
            )

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
            ) {
                when {
                    state.isLoading && state.cards.isEmpty() -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = CaroTheme.color.surface.brand,
                        )
                    }

                    state.hasLoadFailed && state.cards.isEmpty() -> {
                        LoadFailedMessage(
                            modifier = Modifier.align(Alignment.Center),
                            onRetryClick = { onIntent(DeckCardsIntent.ClickRetry) },
                        )
                    }

                    state.isEmpty -> {
                        Text(
                            modifier =
                                Modifier
                                    .align(Alignment.Center)
                                    .padding(horizontal = CaroTheme.spacing.xl2),
                            text = stringResource(Res.string.card_list_empty),
                            style = CaroTheme.typography.heading3,
                            color = CaroTheme.color.text.primary,
                            textAlign = TextAlign.Center,
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding =
                                PaddingValues(
                                    start = PageHorizontalPadding,
                                    top = CaroTheme.spacing.m,
                                    end = PageHorizontalPadding,
                                    bottom = 96.dp,
                                ),
                            verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m),
                        ) {
                            items(
                                items = state.cards,
                                key = { card -> card.id },
                            ) { card ->
                                SwipeToEditCardItem(
                                    card = card,
                                    onEditClick = {
                                        onIntent(DeckCardsIntent.ClickEditCard(card.id))
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        AddCardFloatingButton(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = FloatingButtonBottomPadding),
            onClick = { onIntent(DeckCardsIntent.ClickAddCard) },
        )
    }
}

@Composable
private fun SwipeToEditCardItem(
    card: Card,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val latestOnEditClick by rememberUpdatedState(onEditClick)
    val maxOffsetPx = with(density) { SwipeActionWidth.toPx() }
    val offsetAnimation = remember(card.id) { Animatable(0f) }
    var offsetX by remember(card.id) { mutableFloatStateOf(0f) }

    fun animateOffsetTo(
        target: Float,
        onFinished: (() -> Unit)? = null,
    ) {
        coroutineScope.launch {
            offsetAnimation.stop()
            offsetAnimation.snapTo(offsetX)
            offsetAnimation.animateTo(
                targetValue = target,
                animationSpec = SWIPE_OFFSET_ANIMATION_SPEC,
            ) {
                offsetX = value
            }
            onFinished?.invoke()
        }
    }

    val draggableState =
        rememberDraggableState { delta ->
            offsetX = (offsetX + delta).coerceIn(-maxOffsetPx, 0f)
        }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(CaroTheme.shape.xl),
    ) {
        EditActionBackground(
            modifier = Modifier.fillMaxSize(),
            onClick = {
                animateOffsetTo(target = 0f) {
                    latestOnEditClick()
                }
            },
        )
        CardListItem(
            modifier =
                Modifier
                    .offset { IntOffset(x = offsetX.roundToInt(), y = 0) }
                    .draggable(
                        state = draggableState,
                        orientation = Orientation.Horizontal,
                        startDragImmediately = offsetX != 0f,
                        onDragStarted = {
                            offsetAnimation.stop()
                        },
                        onDragStopped = {
                            val target =
                                if (offsetX <= -maxOffsetPx * SWIPE_SETTLE_THRESHOLD) {
                                    -maxOffsetPx
                                } else {
                                    0f
                                }
                            animateOffsetTo(target = target)
                        },
                    ),
            card = card,
        )
    }
}

@Composable
private fun EditActionBackground(
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            modifier
                .background(SwipeActionBackground)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Box(
            modifier =
                Modifier
                    .width(SwipeActionWidth)
                    .fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(Res.string.card_action_edit),
                style = CaroTheme.typography.display,
                color = CaroTheme.color.text.primary,
            )
        }
    }
}

@Composable
private fun CardListItem(
    modifier: Modifier,
    card: Card,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = CardMinHeight)
                .clip(CaroTheme.shape.xl)
                .background(CaroTheme.color.surface.primary)
                .border(
                    width = DividerHeight,
                    color = CaroTheme.color.border.secondary,
                    shape = CaroTheme.shape.xl,
                ).padding(CaroTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
    ) {
        CardFieldRow(
            label = stringResource(Res.string.card_field_label_front),
            text = card.content.front,
        )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(DividerHeight)
                    .background(CaroTheme.color.divider.secondary),
        )
        CardFieldRow(
            label = stringResource(Res.string.card_field_label_back),
            text = card.content.back,
        )
    }
}

@Composable
private fun CardFieldRow(
    label: String,
    text: String,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xs),
    ) {
        Text(
            text = label,
            style = CaroTheme.typography.caption1.regular,
            color = CaroTheme.color.text.tertiary,
        )
        Text(
            text = text,
            style = CaroTheme.typography.body1,
            color = CaroTheme.color.text.primary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LoadFailedMessage(
    modifier: Modifier,
    onRetryClick: () -> Unit,
) {
    Column(
        modifier = modifier.padding(horizontal = CaroTheme.spacing.xl2),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m),
    ) {
        Text(
            text = stringResource(Res.string.card_list_error),
            style = CaroTheme.typography.heading3,
            color = CaroTheme.color.text.primary,
            textAlign = TextAlign.Center,
        )
        Text(
            modifier = Modifier.clickable(onClick = onRetryClick),
            text = stringResource(Res.string.card_list_retry),
            style = CaroTheme.typography.label1,
            color = CaroTheme.color.text.brand,
        )
    }
}

@Composable
private fun AddCardFloatingButton(
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            modifier
                .heightIn(min = FloatingButtonMinHeight)
                .clip(CaroTheme.shape.xxl)
                .background(CaroTheme.color.button.surface.floating)
                .clickable(onClick = onClick)
                .padding(horizontal = CaroTheme.spacing.l, vertical = CaroTheme.spacing.m),
        horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(TopBarIconSize),
            painter = painterResource(Res.drawable.ic_add_24),
            contentDescription = null,
            tint = CaroTheme.color.icon.inverse,
        )
        Text(
            text = stringResource(Res.string.card_button_add),
            style = CaroTheme.typography.body2.semiBold,
            color = CaroTheme.color.text.inverse,
        )
    }
}

@Preview
@Composable
private fun DeckCardsScreenPreview() {
    CaroTheme {
        DeckCardsScreen(
            state =
                DeckCardsState(
                    deckId = 1L,
                    deckTitle = "English",
                    cards =
                        persistentListOf(
                            Card(id = 1L, content = CardContent(front = "Run", back = "달리다")),
                            Card(id = 2L, content = CardContent(front = "Walk", back = "걷다")),
                        ),
                ),
            onIntent = {},
        )
    }
}

@Preview
@Composable
private fun DeckCardsScreenEmptyPreview() {
    CaroTheme {
        DeckCardsScreen(
            state = DeckCardsState(deckId = 1L, deckTitle = "English"),
            onIntent = {},
        )
    }
}
