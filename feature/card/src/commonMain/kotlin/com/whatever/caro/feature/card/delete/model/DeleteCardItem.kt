package com.whatever.caro.feature.card.delete.model

import com.whatever.caro.core.model.card.Card

data class DeleteCardItem(
    val card: Card,
    val isSelected: Boolean = false,
)
