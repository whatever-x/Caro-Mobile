package com.whatever.caro.core.model.card

import androidx.compose.runtime.Immutable

@Immutable
data class DeckCard(
    val id: Long,
    val content: CardContent,
    val badge: CardBadge,
    val reviewCount: Int,
)
