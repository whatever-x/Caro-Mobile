package com.whatever.caro.feature.card

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.card_button_add
import caromobile.core.designsystem.generated.resources.card_button_add_more
import caromobile.core.designsystem.generated.resources.card_button_save
import caromobile.core.designsystem.generated.resources.card_content_description_back
import caromobile.core.designsystem.generated.resources.card_content_description_remove
import caromobile.core.designsystem.generated.resources.card_content_description_swap
import caromobile.core.designsystem.generated.resources.card_counter_added
import caromobile.core.designsystem.generated.resources.card_field_label_back
import caromobile.core.designsystem.generated.resources.card_field_label_front
import caromobile.core.designsystem.generated.resources.card_field_placeholder_back
import caromobile.core.designsystem.generated.resources.card_field_placeholder_front
import caromobile.core.designsystem.generated.resources.card_field_required
import caromobile.core.designsystem.generated.resources.card_tip_label
import caromobile.core.designsystem.generated.resources.card_tip_max_cards
import caromobile.core.designsystem.generated.resources.card_tip_split_by_topic
import caromobile.core.designsystem.generated.resources.card_title_create
import caromobile.core.designsystem.generated.resources.ic_add_16
import caromobile.core.designsystem.generated.resources.ic_chevron_left_24
import caromobile.core.designsystem.generated.resources.ic_switch_16
import caromobile.core.designsystem.generated.resources.ic_x_circle_16
import com.whatever.caro.core.designsystem.components.CaroTextArea
import com.whatever.caro.core.designsystem.components.CaroTopBar
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.card.CardContent
import com.whatever.caro.feature.card.mvi.CreateCardIntent
import com.whatever.caro.feature.card.mvi.CreateCardState
import com.whatever.caro.feature.card.mvi.StagedCard
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val PageHorizontalPadding = 28.dp
private val CtaButtonHeight = 56.dp
private val TopBarIconSize = 24.dp
private val SwapButtonSize = 32.dp
private val SmallIconSize = 16.dp
private val RemoveIconSize = 20.dp
private val RemoveHitTargetSize = 40.dp
private val PreviewCardWidth = 110.dp
private val PreviewCardHeight = 133.dp
private val TipDotSize = 4.dp
private val HairlineThickness = 1.dp
private const val DISABLED_ALPHA = 0.4f
private const val PREVIEW_TEXT_MAX_LINES = 3

@Composable
internal fun CreateCardScreen(
    state: CreateCardState,
    onIntent: (CreateCardIntent) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(CaroTheme.color.background.primary),
    ) {
        CaroTopBar(
            leadingContent = {
                Icon(
                    modifier =
                        Modifier
                            .size(TopBarIconSize)
                            .clickable { onIntent(CreateCardIntent.ClickBack) },
                    painter = painterResource(Res.drawable.ic_chevron_left_24),
                    contentDescription = stringResource(Res.string.card_content_description_back),
                    tint = CaroTheme.color.icon.brand,
                )
            },
            centerContent = {
                Text(
                    text = stringResource(Res.string.card_title_create),
                    style = CaroTheme.typography.heading2,
                    color = CaroTheme.color.text.primary,
                )
            },
        )

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = CaroTheme.spacing.xl,
                        vertical = CaroTheme.spacing.m,
                    ),
            verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.l),
        ) {
            CaroTextArea(
                value = state.front,
                onValueChange = { onIntent(CreateCardIntent.UpdateFront(it)) },
                placeholder = stringResource(Res.string.card_field_placeholder_front),
                header = { RequiredFieldHeader(label = stringResource(Res.string.card_field_label_front)) },
                footer = { FieldCounter(count = state.frontCount) },
            )

            SwapButton(onClick = { onIntent(CreateCardIntent.ClickSwap) })

            CaroTextArea(
                value = state.back,
                onValueChange = { onIntent(CreateCardIntent.UpdateBack(it)) },
                placeholder = stringResource(Res.string.card_field_placeholder_back),
                header = { RequiredFieldHeader(label = stringResource(Res.string.card_field_label_back)) },
                footer = { FieldCounter(count = state.backCount) },
            )

            TipSection()

            AddedCardsSection(
                addedCount = state.addedCount,
                addedCards = state.addedCards,
                onRemove = { id -> onIntent(CreateCardIntent.ClickRemoveCard(id)) },
            )
        }

        BottomBar(
            isAddEnabled = state.isAddEnabled,
            isSaveEnabled = state.isSaveEnabled,
            hasAddedCards = state.addedCards.isNotEmpty(),
            onAdd = { onIntent(CreateCardIntent.ClickAddCard) },
            onSave = { onIntent(CreateCardIntent.ClickSave) },
        )
    }
}

@Composable
private fun RequiredFieldHeader(label: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = CaroTheme.typography.heading3,
            color = CaroTheme.color.text.primary,
        )
        Text(
            text = stringResource(Res.string.card_field_required),
            style = CaroTheme.typography.label1,
            color = CaroTheme.color.text.accent,
        )
    }
}

@Composable
private fun FieldCounter(count: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = count,
            style = CaroTheme.typography.caption1,
            color = CaroTheme.color.text.tertiary,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun SwapButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(SwapButtonSize)
                    .clip(CircleShape)
                    .background(CaroTheme.color.surface.primary)
                    .border(
                        width = HairlineThickness,
                        color = CaroTheme.color.border.secondary,
                        shape = CircleShape,
                    ).clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(SmallIconSize),
                painter = painterResource(Res.drawable.ic_switch_16),
                contentDescription = stringResource(Res.string.card_content_description_swap),
                tint = CaroTheme.color.icon.brand,
            )
        }
    }
}

@Composable
private fun TipSection() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
    ) {
        Text(
            text = stringResource(Res.string.card_tip_label),
            style = CaroTheme.typography.caption1,
            color = CaroTheme.color.text.secondary,
        )
        TipRow(text = stringResource(Res.string.card_tip_max_cards))
        TipRow(text = stringResource(Res.string.card_tip_split_by_topic))
    }
}

@Composable
private fun TipRow(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(TipDotSize)
                    .clip(CircleShape)
                    .background(CaroTheme.color.text.tertiary),
        )
        Text(
            text = text,
            style = CaroTheme.typography.caption1,
            color = CaroTheme.color.text.tertiary,
        )
    }
}

@Composable
private fun AddedCardsSection(
    addedCount: Int,
    addedCards: List<StagedCard>,
    onRemove: (Long) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m),
    ) {
        AddedCounterChip(count = addedCount)

        if (addedCards.isNotEmpty()) {
            val listState = rememberLazyListState()
            // 카드를 추가하면 맨 끝에 붙으므로, 방금 추가된 카드를 보이도록 끝으로 스크롤한다.
            LaunchedEffect(addedCards.size) {
                listState.animateScrollToItem(addedCards.lastIndex)
            }
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m),
            ) {
                items(items = addedCards, key = { it.id }) { staged ->
                    CardPreview(
                        card = staged.content,
                        onRemove = { onRemove(staged.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AddedCounterChip(count: Int) {
    Box(
        modifier =
            Modifier
                .clip(CaroTheme.shape.xxl)
                .background(CaroTheme.color.surface.brand)
                .padding(
                    horizontal = CaroTheme.spacing.s,
                    vertical = CaroTheme.spacing.xs,
                ),
    ) {
        Text(
            text = stringResource(Res.string.card_counter_added, count),
            style = CaroTheme.typography.caption1,
            color = CaroTheme.color.text.inverse,
        )
    }
}

@Composable
private fun CardPreview(
    card: CardContent,
    onRemove: () -> Unit,
) {
    Box {
        Column(
            modifier =
                Modifier
                    .width(PreviewCardWidth)
                    .height(PreviewCardHeight)
                    .clip(CaroTheme.shape.m)
                    .background(CaroTheme.color.surface.info)
                    .padding(CaroTheme.spacing.m),
            verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xs),
        ) {
            Text(
                modifier = Modifier.fillMaxWidth().weight(1f),
                text = card.front,
                style = CaroTheme.typography.caption1,
                color = CaroTheme.color.text.primary,
                textAlign = TextAlign.Center,
                maxLines = PREVIEW_TEXT_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(HairlineThickness)
                        .background(CaroTheme.color.border.info),
            )
            Text(
                modifier = Modifier.fillMaxWidth().weight(1f),
                text = card.back,
                style = CaroTheme.typography.caption1,
                color = CaroTheme.color.text.secondary,
                textAlign = TextAlign.Center,
                maxLines = PREVIEW_TEXT_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // ✕ 아이콘의 시각 크기/위치는 그대로 두고, 터치 히트 영역만 넓힌다(접근성).
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .size(RemoveHitTargetSize)
                    .clickable(onClick = onRemove),
            contentAlignment = Alignment.TopEnd,
        ) {
            Icon(
                modifier = Modifier.size(RemoveIconSize),
                painter = painterResource(Res.drawable.ic_x_circle_16),
                contentDescription = stringResource(Res.string.card_content_description_remove),
                tint = CaroTheme.color.icon.brand,
            )
        }
    }
}

@Composable
private fun BottomBar(
    isAddEnabled: Boolean,
    isSaveEnabled: Boolean,
    hasAddedCards: Boolean,
    onAdd: () -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.ime.exclude(WindowInsets.navigationBars))
                .padding(
                    horizontal = PageHorizontalPadding,
                    vertical = CaroTheme.spacing.l,
                ),
        verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
    ) {
        AddCardButton(
            enabled = isAddEnabled,
            hasAddedCards = hasAddedCards,
            onClick = onAdd,
        )
        SaveButton(
            enabled = isSaveEnabled,
            onClick = onSave,
        )
    }
}

@Composable
private fun AddCardButton(
    enabled: Boolean,
    hasAddedCards: Boolean,
    onClick: () -> Unit,
) {
    val backgroundColor =
        if (enabled) {
            CaroTheme.color.surface.brand
        } else {
            CaroTheme.color.surface.brand
                .copy(alpha = DISABLED_ALPHA)
        }
    val label =
        if (hasAddedCards) {
            stringResource(Res.string.card_button_add_more)
        } else {
            stringResource(Res.string.card_button_add)
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = CtaButtonHeight)
                .clip(CaroTheme.shape.xxl)
                .background(backgroundColor)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(
                    horizontal = CaroTheme.spacing.xl,
                    vertical = CaroTheme.spacing.l,
                ),
        horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xs, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(SmallIconSize),
            painter = painterResource(Res.drawable.ic_add_16),
            contentDescription = null,
            tint = CaroTheme.color.icon.inverse,
        )
        Text(
            text = label,
            style = CaroTheme.typography.label1,
            color = CaroTheme.color.text.inverse,
        )
    }
}

@Composable
private fun SaveButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val textColor =
        if (enabled) {
            CaroTheme.color.text.primary
        } else {
            CaroTheme.color.text.tertiary
        }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick)
                .padding(vertical = CaroTheme.spacing.m),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(Res.string.card_button_save),
            style = CaroTheme.typography.label1,
            color = textColor,
        )
    }
}
