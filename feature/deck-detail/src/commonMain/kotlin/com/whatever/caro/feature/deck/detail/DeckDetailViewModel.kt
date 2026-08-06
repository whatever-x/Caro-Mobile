package com.whatever.caro.feature.deck.detail

import com.whatever.caro.core.data.repository.deck.DeckRepository
import com.whatever.caro.core.data.util.suspendRunCatching
import com.whatever.caro.core.model.card.CardBadge
import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.model.deck.DeckCardSortType
import com.whatever.caro.core.model.learning.LearningMode
import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.deck.detail.model.CardItem
import com.whatever.caro.feature.deck.detail.model.CardReviewState
import com.whatever.caro.feature.deck.detail.mvi.DeckDetailIntent
import com.whatever.caro.feature.deck.detail.mvi.DeckDetailSideEffect
import com.whatever.caro.feature.deck.detail.mvi.DeckDetailSortOption
import com.whatever.caro.feature.deck.detail.mvi.DeckDetailState
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException

class DeckDetailViewModel(
    private val deckRepository: DeckRepository,
    deck: Deck,
    exceptionFilter: ExceptionFilter,
) : BaseViewModel<DeckDetailState, DeckDetailIntent, DeckDetailSideEffect>(
        initialState =
            DeckDetailState(
                deck = deck,
            ),
        exceptionFilter = exceptionFilter,
    ) {
    private var cardLoadGeneration = 0L

    init {
        loadCards()
    }

    override fun handleClientException(throwable: Throwable) {
        super.handleClientException(throwable)
        reduce { copy(isCardListLoading = false) }
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
                postSideEffect(
                    DeckDetailSideEffect.NavigateToLearning(
                        deckId = currentState.deck.id,
                        mode = LearningMode.ALL,
                    ),
                )
            }

            DeckDetailIntent.ClickDailyStudy -> {
                postSideEffect(
                    DeckDetailSideEffect.NavigateToLearning(
                        deckId = currentState.deck.id,
                        mode = LearningMode.DAILY,
                    ),
                )
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
                loadCards()
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
                    copy(
                        isDeckEditBottomSheetVisible = false,
                        isDeleteDeckDialogVisible = true,
                    )
                }
            }

            DeckDetailIntent.ClickDeleteDeckCancel -> {
                reduce {
                    copy(isDeleteDeckDialogVisible = false)
                }
            }

            DeckDetailIntent.ClickDeleteDeckConfirm -> {
                deleteDeck()
            }

            is DeckDetailIntent.ClickCard -> {
                if (currentState.deckCardList.any { it.id == intent.cardId }) {
                    postSideEffect(
                        DeckDetailSideEffect.NavigateToCardDetail(
                            deckId = currentState.deck.id,
                            cardId = intent.cardId,
                        ),
                    )
                }
            }

            is DeckDetailIntent.ClickEditCard -> {
                handleClickEditCard(cardId = intent.cardId)
            }

            DeckDetailIntent.RefreshCards -> {
                loadCards()
            }
        }
    }

    private fun handleClickEditCard(cardId: Long) {
        val card = currentState.deckCardList.firstOrNull { it.id == cardId } ?: return
        postSideEffect(
            DeckDetailSideEffect.NavigateToEditCard(
                cardId = card.id,
                front = card.front,
                back = card.back,
            ),
        )
    }

    private fun deleteDeck() {
        if (currentState.isDeckDeleting) return
        reduce {
            copy(
                isDeleteDeckDialogVisible = false,
                isDeckDeleting = true,
            )
        }

        launch {
            suspendRunCatching {
                deckRepository.deleteDeck(deckId = currentState.deck.id)
            }.onSuccess {
                postSideEffect(DeckDetailSideEffect.NavigateToHome)
            }.onFailure {
                reduce {
                    copy(isDeckDeleting = false)
                }
                postSideEffect(DeckDetailSideEffect.ShowDeckDeleteError)
            }
        }
    }

    private fun loadCards() {
        val generation = ++cardLoadGeneration
        val deckId = currentState.deck.id
        val sortOption = currentState.selectedSortOption
        reduce {
            copy(isCardListLoading = true)
        }

        launch {
            runCatching {
                when (sortOption) {
                    DeckDetailSortOption.CREATED -> {
                        deckRepository.getDeckCards(deckId = deckId)
                    }

                    DeckDetailSortOption.LAST_REVIEWED,
                    DeckDetailSortOption.FREQUENCY,
                    -> {
                        deckRepository.getDeckCards(
                            deckId = deckId,
                            sortType = sortOption.toDeckCardSortType(),
                        )
                    }
                }
            }.onSuccess { cards ->
                if (generation != cardLoadGeneration) return@onSuccess
                reduce {
                    copy(
                        deck = deck.copy(cardTotalCount = cards.size),
                        deckCardList =
                            cards
                                .map { card ->
                                    CardItem(
                                        id = card.id,
                                        front = card.content.front,
                                        back = card.content.back,
                                        reviewCount = card.reviewCount,
                                        reviewState = card.badge.toCardReviewState(),
                                    )
                                }.toPersistentList(),
                        isCardListLoading = false,
                    )
                }
            }.onFailure { throwable ->
                if (throwable is CancellationException) throw throwable
                if (generation != cardLoadGeneration) return@onFailure
                reduce {
                    copy(isCardListLoading = false)
                }
                postSideEffect(DeckDetailSideEffect.ShowCardLoadError)
            }
        }
    }

    private fun DeckDetailSortOption.toDeckCardSortType(): DeckCardSortType =
        when (this) {
            DeckDetailSortOption.CREATED -> DeckCardSortType.CREATED
            DeckDetailSortOption.LAST_REVIEWED -> DeckCardSortType.LAST_REVIEWED
            DeckDetailSortOption.FREQUENCY -> DeckCardSortType.FREQUENCY
        }

    private fun CardBadge.toCardReviewState(): CardReviewState =
        when (this) {
            CardBadge.NEW -> CardReviewState.NEW
            CardBadge.REVIEW -> CardReviewState.REVIEW
            CardBadge.HARD -> CardReviewState.HARD
        }
}
