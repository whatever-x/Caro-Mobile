package com.whatever.caro.core.remote.dto.library

import kotlinx.serialization.Serializable

@Serializable
data class LibraryDeckResponse(
    val libraryDeckId: Long,
    val name: String,
    val description: String,
    val cardCount: Int,
)

@Serializable
data class LibraryCardResponse(
    val libraryCardId: Long,
    val front: String,
    val back: String,
    val position: Int,
)

@Serializable
data class LibraryDetailResponse(
    val libraryDeckId: Long,
    val name: String,
    val description: String,
    val cardCount: Int,
    val cards: List<LibraryCardResponse>,
)

@Serializable
data class LibraryCopyResponse(
    val deckId: Long,
    val name: String,
    val description: String,
    val cardCount: Int,
)
