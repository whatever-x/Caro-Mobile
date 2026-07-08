package com.whatever.caro.core.model.card

import androidx.compose.runtime.Immutable

@Immutable
data class Card(
    val id: Long,
    val content: CardContent,
)
