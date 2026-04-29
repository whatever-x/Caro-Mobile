package com.whatever.caro.feature.deck.detail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.deck.detail.components.AddCardButtonItem
import com.whatever.caro.feature.deck.detail.components.DeckCardItem
import com.whatever.caro.feature.deck.detail.components.DeckDetailGuid
import com.whatever.caro.feature.deck.detail.components.DeckDetailHeader
import com.whatever.caro.feature.deck.detail.components.DeckDetailTopBar
import com.whatever.caro.feature.deck.detail.components.DeckEditBottomSheet
import com.whatever.caro.feature.deck.detail.components.SortBottomSheet
import com.whatever.caro.feature.deck.detail.components.dailyLearningCard
import com.whatever.caro.feature.deck.detail.components.filterAndSortStickyHeader
import com.whatever.caro.feature.deck.detail.model.DeckUiModel
import com.whatever.caro.feature.deck.detail.model.LearningUiModel
import com.whatever.caro.feature.deck.detail.mvi.DeckDetailIntent
import com.whatever.caro.feature.deck.detail.mvi.DeckDetailState

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DeckDetailScreen(
    state: DeckDetailState,
    onIntent: (DeckDetailIntent) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(color = CaroTheme.color.background.primary),
    ) {
        DeckDetailTopBar(
            onBack = { onIntent(DeckDetailIntent.ClickBack) },
            onEditDeck = { onIntent(DeckDetailIntent.ClickEditDeck) },
        )

        if (state.isEmptyDeckCard) {
            DeckDetailHeader(
                title = state.deckUiModel.title,
                description = state.deckUiModel.description,
            )

            DeckDetailGuid(
                onAddFirstCard = { onIntent(DeckDetailIntent.ClickAddCard) },
            )
        } else {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize(),
            ) {
                item {
                    DeckDetailHeader(
                        title = state.deckUiModel.title,
                        description = state.deckUiModel.description,
                    )
                }

                dailyLearningCard(
                    learningCardCount = state.learningUiModel.reviewedCardCount,
                    learningCardTotal = state.learningUiModel.learningCardTotal,
                    learningProgress = state.learningUiModel.learningProgress,
                    currentLearningStatus = state.learningUiModel.currentLearningStatus,
                    onAllStudy = { onIntent(DeckDetailIntent.ClickAllStudy) },
                    onDailyStudy = { onIntent(DeckDetailIntent.ClickDailyStudy) },
                )

                filterAndSortStickyHeader(
                    deckCardTotal = state.deckUiModel.deckCardTotal,
                    selectedSortOption = state.selectedSortOption,
                    onSortCardList = { onIntent(DeckDetailIntent.ClickSortCardList) },
                    onEditCardList = { onIntent(DeckDetailIntent.ClickEditCardList) },
                )

                item {
                    AddCardButtonItem(
                        onAddCard = { onIntent(DeckDetailIntent.ClickAddCard) },
                    )
                }

                itemsIndexed(
                    items = state.deckUiModel.deckCardList,
                    key = { index, card -> "${card.id}-$index" },
                ) { index, card ->
                    DeckCardItem(
                        card = card,
                        onClick = { onIntent(DeckDetailIntent.ClickCard(card.id)) },
                        modifier =
                            Modifier
                                .padding(
                                    start = CaroTheme.spacing.l,
                                    top = CaroTheme.spacing.s,
                                    end = CaroTheme.spacing.l,
                                    bottom =
                                        if (index == state.deckUiModel.deckCardList.lastIndex) {
                                            CaroTheme.spacing.s
                                        } else {
                                            0.dp
                                        },
                                ),
                    )
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
}

@Preview(locale = "en")
@Preview(locale = "ko")
@Composable
private fun DeckDetailScreenPreview() {
    CaroTheme {
        DeckDetailScreen(
            state =
                DeckDetailState(
                    deckUiModel = DeckUiModel.preview(),
                    learningUiModel = LearningUiModel.preview(),
                ),
            onIntent = { },
        )
    }
}
