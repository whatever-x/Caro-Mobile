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
            title = state.deck.title,
            onBack = { onIntent(DeckDetailIntent.ClickBack) },
            onEditDeck = { onIntent(DeckDetailIntent.ClickEditDeck) },
        )

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
                        onEdit = { onIntent(DeckDetailIntent.ClickCard(card.id)) },
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
            state = DeckDetailState(),
            onIntent = { },
        )
    }
}
