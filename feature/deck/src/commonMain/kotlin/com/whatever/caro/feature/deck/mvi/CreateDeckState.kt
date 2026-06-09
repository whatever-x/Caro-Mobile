package com.whatever.caro.feature.deck.mvi

import com.whatever.caro.core.viewmodel.contract.UiState
import com.whatever.caro.feature.deck.DeckInputDefaults

data class CreateDeckState(
    val name: String = "",
    val description: String = "",
    val isLoading: Boolean = false,
) : UiState {
    val nameCount: String
        get() = "${name.length}/${DeckInputDefaults.NAME_MAX}"

    val descriptionCount: String
        get() = "${description.length}/${DeckInputDefaults.DESC_MAX}"

    val isConfirmEnabled: Boolean
        get() = name.isNotBlank() && description.isNotBlank() && isLoading.not()
}
