package com.whatever.caro.feature.card.mvi

import com.whatever.caro.core.model.card.CardInputLimits
import com.whatever.caro.core.viewmodel.contract.UiState

data class EditCardState(
    val front: String,
    val back: String,
    val isSaving: Boolean = false,
) : UiState {
    val frontCount: String
        get() = "${front.length}/${CardInputLimits.FIELD_MAX}"

    val backCount: String
        get() = "${back.length}/${CardInputLimits.FIELD_MAX}"

    val isSaveEnabled: Boolean
        get() = front.isNotBlank() && back.isNotBlank() && isSaving.not()
}
