package com.whatever.caro.feature.deck.detail.mvi

import com.whatever.caro.core.viewmodel.contract.UiState
import com.whatever.caro.feature.deck.detail.model.DeckUiModel
import com.whatever.caro.feature.deck.detail.model.LearningUiModel

data class DeckDetailState(
    val deckUiModel: DeckUiModel = DeckUiModel.preview(),
    val learningUiModel: LearningUiModel = LearningUiModel.preview(),
    val isSortBottomSheetVisible: Boolean = false,
    val isDeckEditBottomSheetVisible: Boolean = false,
    val selectedSortOption: DeckDetailSortOption = DeckDetailSortOption.CREATED,
) : UiState {
    val isEmptyDeckCard: Boolean
        get() = deckUiModel.deckCardList.isEmpty()
}

enum class DeckDetailSortOption {
    CREATED,
    LAST_REVIEWED,
    FREQUENCY,
}
