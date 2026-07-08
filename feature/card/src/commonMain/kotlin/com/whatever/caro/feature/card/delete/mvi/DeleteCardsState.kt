package com.whatever.caro.feature.card.delete.mvi

import com.whatever.caro.core.viewmodel.contract.UiState
import com.whatever.caro.feature.card.delete.model.DeleteCardItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

data class DeleteCardsState(
    val cards: ImmutableList<DeleteCardItem> = persistentListOf(),
    val selectedCardIds: ImmutableSet<Long> = persistentSetOf(),
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val isDeleteConfirmDialogVisible: Boolean = false,
) : UiState {
    val selectedCount: Int
        get() = selectedCardIds.size

    val isDeleteEnabled: Boolean
        get() = selectedCardIds.isNotEmpty() && isDeleting.not()

    val isAllCardsSelected: Boolean
        get() = cards.isNotEmpty() && selectedCardIds.size == cards.size
}
