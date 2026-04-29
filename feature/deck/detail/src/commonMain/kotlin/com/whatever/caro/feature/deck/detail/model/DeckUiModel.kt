package com.whatever.caro.feature.deck.detail.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class DeckUiModel(
    val id: Long = 0L,
    val title: String = "",
    val description: String = "",
    val deckCardList: ImmutableList<CardItem> = persistentListOf(),
) {
    val deckCardTotal: Int
        get() = deckCardList.size

    companion object {
        fun preview(): DeckUiModel =
            DeckUiModel(
                id = 1L,
                title = "Preview Title",
                description = "Preview Description for Deck Ui Model",
                deckCardList = CardItem.fakeList(),
            )
    }
}
