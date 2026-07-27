package com.whatever.caro.core.navigator.entries

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class CardDetailEntry(
    val deckId: Long,
    val cardId: Long,
) : NavKey
