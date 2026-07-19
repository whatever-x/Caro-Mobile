package com.whatever.caro.core.navigator.entries

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class EditDeckEntry(
    val deckName: String,
    val deckDescription: String,
    val deckId: Long,
) : NavKey
