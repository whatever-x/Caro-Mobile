package com.whatever.caro.core.navigator.entries

import androidx.navigation3.runtime.NavKey
import com.whatever.caro.core.model.deck.Deck
import kotlinx.serialization.Serializable

@Serializable
data class DeckDetailEntry(
    val deck: Deck,
) : NavKey
