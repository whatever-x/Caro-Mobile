package com.whatever.caro.core.model.library

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class LibraryDeck(
    val id: Long,
    val name: String,
    val description: String,
    val cardCount: Int,
)

@Immutable
@Serializable
data class LibraryCard(
    val id: Long,
    val front: String,
    val back: String,
    val position: Int,
)

@Serializable
data class LibraryDetail(
    val deck: LibraryDeck,
    val cards: List<LibraryCard>,
)
