package com.whatever.caro.feature.deck.detail.mvi

import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.viewmodel.contract.UiState
import com.whatever.caro.feature.deck.detail.model.CardItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class DeckDetailState(
    val deck: Deck,
    val deckCardList: ImmutableList<CardItem> = persistentListOf(),
    val isCardListLoading: Boolean = false,
    val isSortBottomSheetVisible: Boolean = false,
    val isDeckEditBottomSheetVisible: Boolean = false,
    val isDeleteDeckDialogVisible: Boolean = false,
    val isDeckDeleting: Boolean = false,
    val isCardLoadError: Boolean = false,
    val selectedSortOption: DeckDetailSortOption = DeckDetailSortOption.CREATED,
) : UiState {
    val isLoading: Boolean
        get() = isCardListLoading || isDeckDeleting

    val isLoadedContentVisible: Boolean
        get() = isCardListLoading.not()

    // 보여줄 카드가 없을 때만 전체 오류 화면을 띄운다. 이미 목록이 있는데
    // 새로고침이 실패한 경우에는 목록을 유지하고 스낵바로만 알린다.
    val isCardLoadErrorVisible: Boolean
        get() = isCardLoadError && deckCardList.isEmpty() && isCardListLoading.not()

    val isEmptyDeckCard: Boolean
        get() = deckCardList.isEmpty() && isCardListLoading.not() && isCardLoadError.not()
}

enum class DeckDetailSortOption {
    CREATED,
    LAST_REVIEWED,
    FREQUENCY,
}
