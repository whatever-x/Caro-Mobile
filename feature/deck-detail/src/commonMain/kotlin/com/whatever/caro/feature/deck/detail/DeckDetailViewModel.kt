package com.whatever.caro.feature.deck.detail

import com.whatever.caro.core.data.repository.card.CardRepository
import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.deck.detail.model.CardItem
import com.whatever.caro.feature.deck.detail.mvi.DeckDetailIntent
import com.whatever.caro.feature.deck.detail.mvi.DeckDetailSideEffect
import com.whatever.caro.feature.deck.detail.mvi.DeckDetailState
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException

class DeckDetailViewModel(
    deck: Deck,
    private val cardRepository: CardRepository,
    exceptionFilter: ExceptionFilter,
) : BaseViewModel<DeckDetailState, DeckDetailIntent, DeckDetailSideEffect>(
        initialState =
            DeckDetailState(
                deck = deck,
            ),
        exceptionFilter = exceptionFilter,
    ) {
    init {
        loadCards()
    }

    override suspend fun handleIntent(intent: DeckDetailIntent) {
        when (intent) {
            DeckDetailIntent.ClickBack -> {
                postSideEffect(DeckDetailSideEffect.NavigateBack)
            }

            DeckDetailIntent.ClickAddCard -> {
                postSideEffect(DeckDetailSideEffect.NavigateToCreateCard(deckId = currentState.deck.id))
            }

            DeckDetailIntent.ClickAllStudy -> {
                postSideEffect(DeckDetailSideEffect.NavigateToAllStudy(deckId = currentState.deck.id))
            }

            DeckDetailIntent.ClickDailyStudy -> {
                postSideEffect(DeckDetailSideEffect.NavigateToDailyStudy(deckId = currentState.deck.id))
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
                postSideEffect(DeckDetailSideEffect.NavigateToEditCardList(deckId = currentState.deck.id))
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
                postSideEffect(DeckDetailSideEffect.NavigateToEditDeck(deckId = currentState.deck.id))
            }

            DeckDetailIntent.ClickDeckEditBottomSheetDelete -> {
                reduce {
                    copy(isDeckEditBottomSheetVisible = false)
                }
            }

            is DeckDetailIntent.ClickCard -> {
                handleClickCard(cardId = intent.cardId)
            }

            DeckDetailIntent.RefreshCards -> {
                loadCards()
            }
        }
    }

    private fun handleClickCard(cardId: Long) {
        val card = currentState.deckCardList.firstOrNull { it.id == cardId } ?: return
        postSideEffect(
            DeckDetailSideEffect.NavigateToEditCard(
                cardId = card.id,
                front = card.front,
                back = card.back,
            ),
        )
    }

    private fun loadCards() {
        if (currentState.isCardListLoading) return
        reduce {
            copy(isCardListLoading = true)
        }

        launch {
            runCatching {
                cardRepository.getCards(deckId = currentState.deck.id)
            }.onSuccess { cards ->
                reduce {
                    copy(
                        deckCardList =
                            cards
                                .map { card ->
                                    CardItem(
                                        id = card.id,
                                        front = card.content.front,
                                        back = card.content.back,
                                    )
                                }.toPersistentList(),
                        isCardListLoading = false,
                    )
                }
            }.onFailure { throwable ->
                if (throwable is CancellationException) throw throwable
                reduce {
                    copy(isCardListLoading = false)
                }
                postSideEffect(DeckDetailSideEffect.ShowCardLoadError)
            }
        }
    }
}
