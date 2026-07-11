package com.whatever.caro.core.model.deck

import kotlinx.serialization.Serializable

@Serializable
enum class DeckState {
    NOT_STARTED,
    LEARNING,
    COMPLETE,
}
