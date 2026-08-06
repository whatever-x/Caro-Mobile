package com.whatever.caro.feature.card

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
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
import caromobile.core.designsystem.generated.resources.card_discard_dialog_body
import caromobile.core.designsystem.generated.resources.card_discard_dialog_button_leave
import caromobile.core.designsystem.generated.resources.card_discard_dialog_button_stay
import caromobile.core.designsystem.generated.resources.card_discard_dialog_title
import caromobile.core.designsystem.generated.resources.card_field_label_back
import caromobile.core.designsystem.generated.resources.card_field_label_front
import caromobile.core.designsystem.generated.resources.card_field_max_reached
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
import com.whatever.caro.core.designsystem.components.CaroDialog
import com.whatever.caro.core.designsystem.components.CaroDialogButton
import com.whatever.caro.core.designsystem.components.CaroTextArea
import com.whatever.caro.core.designsystem.components.CaroTopBar
import com.whatever.caro.core.designsystem.modifier.noRippleClickable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.card.CardContent
import com.whatever.caro.core.model.card.CardInputLimits
import com.whatever.caro.core.ui.loading.CaroLoadingOverlayBox
import com.whatever.caro.feature.card.mvi.CreateCardIntent
import com.whatever.caro.feature.card.mvi.CreateCardState
import com.whatever.caro.feature.card.mvi.StagedCard
import kotlinx.collections.immutable.ImmutableList
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
private const val PREVIEW_TEXT_MAX_LINES = 3

@Composable
internal fun CreateCardScreen(
    state: CreateCardState,
    onIntent: (CreateCardIntent) -> Unit,
) {
    val frontFocusRequester = remember { FocusRequester() }
    val contentScrollState = rememberScrollState()
    // nextCardId 는 카드 추가 시에만 증가하므로, 추가 직후에만 앞면으로 포커스를 되돌린다.
    // 포커스만 옮기면 화면은 뒷면에 머물러 커서가 보이지 않으므로 최상단으로 함께 스크롤한다.
    LaunchedEffect(state.nextCardId) {
        if (state.nextCardId > 0L) {
            contentScrollState.animateScrollTo(0)
            frontFocusRequester.requestFocus()
        }
    }

    CaroLoadingOverlayBox(isLoading = state.isSaving) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(CaroTheme.color.background.primary),
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
                                    .noRippleClickable { onIntent(CreateCardIntent.ClickBack) },
                            painter = painterResource(Res.drawable.ic_chevron_left_24),
                            contentDescription = stringResource(Res.string.card_content_description_back),
                            tint = CaroTheme.color.icon.brand,
                        )
                        Text(
                            text = stringResource(Res.string.card_title_create),
                            style = CaroTheme.typography.heading2,
                            color = CaroTheme.color.text.primary,
                        )
                    }
                },
            )

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(contentScrollState)
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
                    footer = {
                        FieldCounter(
                            count = state.frontCount,
                            isMaxReached = state.isFrontMaxReached,
                        )
                    },
                    focusRequester = frontFocusRequester,
                )

                SwapButton(onClick = { onIntent(CreateCardIntent.ClickSwap) })

                CaroTextArea(
                    value = state.back,
                    onValueChange = { onIntent(CreateCardIntent.UpdateBack(it)) },
                    placeholder = stringResource(Res.string.card_field_placeholder_back),
                    header = { RequiredFieldHeader(label = stringResource(Res.string.card_field_label_back)) },
                    footer = {
                        FieldCounter(
                            count = state.backCount,
                            isMaxReached = state.isBackMaxReached,
                        )
                    },
                )

                TipSection()

                AddedCardsSection(
                    addedCount = state.addedCount,
                    addedCards = state.addedCards,
                    scrollToEndKey = state.nextCardId,
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

        if (state.isDiscardDialogVisible) {
            DiscardConfirmDialog(
                onLeave = { onIntent(CreateCardIntent.ConfirmDiscard) },
                onStay = { onIntent(CreateCardIntent.DismissDiscardDialog) },
            )
        }
    }
}

@Composable
private fun DiscardConfirmDialog(
    onLeave: () -> Unit,
    onStay: () -> Unit,
) {
    CaroDialog(
        onDismissRequest = onStay,
        title = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(Res.string.card_discard_dialog_title),
                style = CaroTheme.typography.heading2,
                color = CaroTheme.color.text.primary,
            )
        },
        content = {
            Spacer(modifier = Modifier.size(CaroTheme.spacing.s))
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(Res.string.card_discard_dialog_body),
                style = CaroTheme.typography.body2.medium,
                color = CaroTheme.color.text.secondary,
            )
            Spacer(modifier = Modifier.size(CaroTheme.spacing.l))
        },
        buttons = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
            ) {
                CaroDialogButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(Res.string.card_discard_dialog_button_leave),
                    backgroundColor = CaroTheme.color.surface.dangerous,
                    textColor = CaroTheme.color.text.dangerous,
                    onClick = onLeave,
                )
                CaroDialogButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(Res.string.card_discard_dialog_button_stay),
                    backgroundColor = CaroTheme.color.surface.tertiary,
                    textColor = CaroTheme.color.text.brand,
                    onClick = onStay,
                )
            }
        },
    )
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
            style = CaroTheme.typography.body2.semiBold,
            color = CaroTheme.color.text.dangerous,
        )
    }
}

@Composable
private fun FieldCounter(
    count: String,
    isMaxReached: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isMaxReached) {
            Text(
                text = stringResource(Res.string.card_field_max_reached, CardInputLimits.FIELD_MAX),
                style = CaroTheme.typography.caption1.regular,
                color = CaroTheme.color.text.dangerous,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = count,
            style = CaroTheme.typography.caption1.regular,
            color = if (isMaxReached) CaroTheme.color.text.dangerous else CaroTheme.color.text.tertiary,
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
                    ).noRippleClickable(onClick = onClick),
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
            style = CaroTheme.typography.body2.semiBold,
            color = CaroTheme.color.text.primary,
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
            style = CaroTheme.typography.caption1.regular,
            color = CaroTheme.color.text.tertiary,
        )
    }
}

@Composable
private fun AddedCardsSection(
    addedCount: Int,
    addedCards: ImmutableList<StagedCard>,
    scrollToEndKey: Long,
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
            // 삭제로는 스크롤이 튀지 않도록, 추가 시에만 증가하는 키를 사용한다.
            LaunchedEffect(scrollToEndKey) {
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
            style = CaroTheme.typography.caption1.regular,
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
                style = CaroTheme.typography.caption1.regular,
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
                        .background(CaroTheme.color.divider.secondary),
            )
            Text(
                modifier = Modifier.fillMaxWidth().weight(1f),
                text = card.back,
                style = CaroTheme.typography.caption1.regular,
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
                    .noRippleClickable(onClick = onRemove),
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
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
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
            CaroTheme.color.surface.disabled
        }
    val textColor =
        if (enabled) {
            CaroTheme.color.text.inverse
        } else {
            CaroTheme.color.text.disabled
        }
    val iconColor =
        if (enabled) {
            CaroTheme.color.icon.inverse
        } else {
            CaroTheme.color.icon.disabled
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
                .noRippleClickable(enabled = enabled, onClick = onClick)
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
            tint = iconColor,
        )
        Text(
            text = label,
            style = CaroTheme.typography.caption1.regular,
            color = textColor,
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
            CaroTheme.color.text.disabled
        }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .noRippleClickable(enabled = enabled, onClick = onClick)
                .padding(vertical = CaroTheme.spacing.m),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(Res.string.card_button_save),
            style = CaroTheme.typography.caption1.regular,
            color = textColor,
        )
    }
}
