package com.whatever.caro.feature.deck.detail

import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.deck.detail.mvi.DeckDetailIntent
import com.whatever.caro.feature.deck.detail.mvi.DeckDetailSideEffect
import com.whatever.caro.feature.deck.detail.mvi.DeckDetailState

class DeckDetailViewModel(
    deck: Deck,
    exceptionFilter: ExceptionFilter,
) : BaseViewModel<DeckDetailState, DeckDetailIntent, DeckDetailSideEffect>(
        initialState =
            DeckDetailState(
                deck = deck,
            ),
        exceptionFilter = exceptionFilter,
    ) {
    override suspend fun handleIntent(intent: DeckDetailIntent) {
        when (intent) {
            DeckDetailIntent.ClickBack -> {
                postSideEffect(DeckDetailSideEffect.NavigateBack)
            }

            DeckDetailIntent.ClickAddCard -> {
                TODO()
            }

            DeckDetailIntent.ClickAllStudy -> {
                TODO()
            }

            DeckDetailIntent.ClickDailyStudy -> {
                TODO()
            }

            DeckDetailIntent.ClickSortCardList -> {
                reduce {
                    copy(isSortBottomSheetVisible = true)
                }
            }

            DeckDetailIntent.DismissSortBottomSheet -> {
                reduce {
                    copy(isSortBottomSheetVisible = false)
                }
            }

            is DeckDetailIntent.ClickSortOption -> {
                reduce {
                    copy(
                        selectedSortOption = intent.sortOption,
                        isSortBottomSheetVisible = false,
                    )
                }
            }

            DeckDetailIntent.ClickEditCardList -> {
                TODO()
            }

            DeckDetailIntent.ClickEditDeck -> {
                reduce {
                    copy(isDeckEditBottomSheetVisible = true)
                }
            }

            DeckDetailIntent.DismissDeckEditBottomSheet -> {
                reduce {
                    copy(isDeckEditBottomSheetVisible = false)
                }
            }

            DeckDetailIntent.ClickDeckEditBottomSheetEdit -> {
                reduce {
                    copy(isDeckEditBottomSheetVisible = false)
                }
            }

            DeckDetailIntent.ClickDeckEditBottomSheetDelete -> {
                reduce {
                    copy(isDeckEditBottomSheetVisible = false)
                }
            }

            is DeckDetailIntent.ClickCard -> {
                TODO()
            }
        }
    }
}
