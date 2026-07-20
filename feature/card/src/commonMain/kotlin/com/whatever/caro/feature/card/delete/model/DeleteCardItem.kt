package com.whatever.caro.feature.card.delete.model

import androidx.compose.runtime.Immutable
import com.whatever.caro.core.model.card.Card
import com.whatever.caro.core.model.card.DeckCard

@Immutable
data class DeleteCardItem(
    val card: DeckCard,
    val isSelected: Boolean = false,
)
