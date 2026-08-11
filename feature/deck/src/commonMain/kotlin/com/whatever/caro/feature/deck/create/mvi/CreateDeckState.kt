package com.whatever.caro.feature.deck.create.mvi

import com.whatever.caro.core.model.deck.DeckInputLimits
import com.whatever.caro.core.viewmodel.contract.UiState

data class CreateDeckState(
    val name: String = "",
    val description: String = "",
    val isLoading: Boolean = false,
) : UiState {
    val nameCount: String
        get() = "${name.length}/${DeckInputLimits.NAME_MAX}"

    val descriptionCount: String
        get() = "${description.length}/${DeckInputLimits.DESC_MAX}"

    val isConfirmEnabled: Boolean
        get() = name.isNotBlank() && description.isNotBlank() && isLoading.not()
}
