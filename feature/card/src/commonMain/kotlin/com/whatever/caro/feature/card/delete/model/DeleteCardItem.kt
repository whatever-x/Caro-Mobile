package com.whatever.caro.feature.card.delete.model

import androidx.compose.runtime.Immutable
import com.whatever.caro.core.model.card.Card

@Immutable
data class DeleteCardItem(
    val card: Card,
    val isSelected: Boolean = false,
)
