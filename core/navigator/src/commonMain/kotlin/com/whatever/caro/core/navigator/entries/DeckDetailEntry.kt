package com.whatever.caro.core.navigator.entries

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class DeckDetailEntry(
    val payload: DeckDetailPayload,
) : NavKey

@Serializable
data class DeckDetailPayload(
    val deckId: Long,
    val deckTitle: String,
    val deckDescription: String,
)
