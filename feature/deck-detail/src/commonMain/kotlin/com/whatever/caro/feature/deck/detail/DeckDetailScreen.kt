package com.whatever.caro.feature.deck.detail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.deck_delete_dialog_button_cancel
import caromobile.core.designsystem.generated.resources.deck_delete_dialog_button_delete
import caromobile.core.designsystem.generated.resources.deck_delete_dialog_content
import caromobile.core.designsystem.generated.resources.deck_delete_dialog_title
import com.whatever.caro.core.designsystem.components.CaroDialog
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.model.deck.DeckState
import com.whatever.caro.core.ui.loading.CaroLoadingOverlayBox
import com.whatever.caro.core.ui.modifier.noRippleClickable
import com.whatever.caro.feature.deck.detail.components.DeckDetailGuid
import com.whatever.caro.feature.deck.detail.components.DeckDetailTopBar
import com.whatever.caro.feature.deck.detail.components.DeckEditBottomSheet
import com.whatever.caro.feature.deck.detail.components.SortBottomSheet
import com.whatever.caro.feature.deck.detail.components.lazycolumn.AddCardButtonItem
import com.whatever.caro.feature.deck.detail.components.lazycolumn.DeckDetailHeader
import com.whatever.caro.feature.deck.detail.components.lazycolumn.FilterAndSortSection
import com.whatever.caro.feature.deck.detail.components.lazycolumn.SwipeToRevealCardItem
import com.whatever.caro.feature.deck.detail.mvi.DeckDetailIntent
import com.whatever.caro.feature.deck.detail.mvi.DeckDetailState
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DeckDetailScreen(
    state: DeckDetailState,
    onIntent: (DeckDetailIntent) -> Unit,
) {
    CaroLoadingOverlayBox(isLoading = state.isCardListLoading) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(color = CaroTheme.color.background.primary),
        ) {
            DeckDetailTopBar(
                title = state.deck.title,
                isLoading = state.isCardListLoading,
                onBack = { onIntent(DeckDetailIntent.ClickBack) },
                onEditDeck = { onIntent(DeckDetailIntent.ClickEditDeck) },
            )

            if (state.isLoadedContentVisible) {
                if (state.isEmptyDeckCard) {
                    DeckDetailGuid(
                        onAddFirstCard = { onIntent(DeckDetailIntent.ClickAddCard) },
                    )
                } else {
                    LazyColumn(
                        modifier =
                            Modifier
                                .fillMaxSize(),
                        overscrollEffect = null,
                    ) {
                        item {
                            DeckDetailHeader(
                                description = state.deck.description,
                                learningCardTotal = state.deck.todayLearningCount,
                                learningProgress = state.deck.todayProgress,
                                currentLearningStatus = state.deck.state,
                                onAllStudy = { onIntent(DeckDetailIntent.ClickAllStudy) },
                                onDailyStudy = { onIntent(DeckDetailIntent.ClickDailyStudy) },
                            )
                        }

                        stickyHeader {
                            FilterAndSortSection(
                                deckCardTotal = state.deck.cardTotalCount,
                                selectedSortOption = state.selectedSortOption,
                                onSortCardList = { onIntent(DeckDetailIntent.ClickSortCardList) },
                                onEditCardList = { onIntent(DeckDetailIntent.ClickEditCardList) },
                            )
                        }

                        item {
                            AddCardButtonItem(
                                onAddCard = { onIntent(DeckDetailIntent.ClickAddCard) },
                            )
                        }

                        itemsIndexed(
                            items = state.deckCardList,
                            key = { _, card -> card.id },
                        ) { index, card ->
                            SwipeToRevealCardItem(
                                card = card,
                                onClick = { onIntent(DeckDetailIntent.ClickCard(card.id)) },
                                onEdit = { onIntent(DeckDetailIntent.ClickEditCard(card.id)) },
                                modifier =
                                    Modifier
                                        .padding(
                                            start = CaroTheme.spacing.xl,
                                            top = CaroTheme.spacing.m,
                                            end = CaroTheme.spacing.xl,
                                            bottom =
                                                if (index == state.deckCardList.lastIndex) {
                                                    CaroTheme.spacing.m
                                                } else {
                                                    0.dp
                                                },
                                        ),
                            )
                        }
                    }
                }
            }
        }

        if (state.isSortBottomSheetVisible) {
            SortBottomSheet(
                selectedSortOption = state.selectedSortOption,
                onSortOptionClick = { sortOption ->
                    onIntent(DeckDetailIntent.ClickSortOption(sortOption))
                },
                onDismissRequest = { onIntent(DeckDetailIntent.DismissSortBottomSheet) },
            )
        }

        if (state.isDeckEditBottomSheetVisible) {
            DeckEditBottomSheet(
                onEditDeck = { onIntent(DeckDetailIntent.ClickDeckEditBottomSheetEdit) },
                onDeleteDeck = { onIntent(DeckDetailIntent.ClickDeckEditBottomSheetDelete) },
                onDismissRequest = { onIntent(DeckDetailIntent.DismissDeckEditBottomSheet) },
            )
        }

        if (state.isDeckDeleteDialogVisible) {
            DeckDeleteDialog(
                isDeleting = state.isDeckDeleting,
                onDelete = { onIntent(DeckDetailIntent.ClickDeckDeleteDialogConfirm) },
                onCancel = { onIntent(DeckDetailIntent.ClickDeckDeleteDialogCancel) },
            )
        }
    }
}

@Composable
private fun DeckDeleteDialog(
    isDeleting: Boolean,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
) {
    CaroDialog(
        onDismissRequest = { if (isDeleting.not()) onCancel() },
        title = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(Res.string.deck_delete_dialog_title),
                color = CaroTheme.color.text.primary,
                style = CaroTheme.typography.heading2,
            )
        },
        content = {
            Spacer(modifier = Modifier.size(CaroTheme.spacing.s))
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(Res.string.deck_delete_dialog_content),
                color = CaroTheme.color.text.secondary,
                style = CaroTheme.typography.body2.medium,
            )
            Spacer(modifier = Modifier.size(CaroTheme.spacing.m))
        },
        buttons = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
            ) {
                Text(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                color = CaroTheme.color.surface.error,
                                shape = CaroTheme.shape.xxl,
                            ).padding(
                                horizontal = CaroTheme.spacing.l,
                                vertical = CaroTheme.spacing.m,
                            ).then(
                                if (isDeleting) Modifier else Modifier.noRippleClickable(onDelete),
                            ),
                    text = stringResource(Res.string.deck_delete_dialog_button_delete),
                    color = CaroTheme.color.text.error,
                    style = CaroTheme.typography.caption1.regular,
                    textAlign = TextAlign.Center,
                )
                Text(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                color = CaroTheme.color.surface.tertiary,
                                shape = CaroTheme.shape.xxl,
                            ).padding(
                                horizontal = CaroTheme.spacing.l,
                                vertical = CaroTheme.spacing.m,
                            ).then(
                                if (isDeleting) Modifier else Modifier.noRippleClickable(onCancel),
                            ),
                    text = stringResource(Res.string.deck_delete_dialog_button_cancel),
                    color = CaroTheme.color.text.brand,
                    style = CaroTheme.typography.caption1.regular,
                    textAlign = TextAlign.Center,
                )
            }
        },
    )
}

@Preview(locale = "en")
@Preview(locale = "ko")
@Composable
private fun DeckDetailScreenPreview() {
    CaroTheme {
        DeckDetailScreen(
            state =
                DeckDetailState(
                    Deck(
                        id = 1,
                        title = "Android",
                        description = "기초 학습",
                        cardTotalCount = 100,
                        todayLearningCount = 10,
                        todayCompleteCount = 0,
                        state = DeckState.NOT_STARTED,
                    ),
                ),
            onIntent = { },
        )
    }
}
