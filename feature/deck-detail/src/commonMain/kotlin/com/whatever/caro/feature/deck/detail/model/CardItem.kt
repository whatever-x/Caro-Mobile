package com.whatever.caro.feature.deck.detail.model

import androidx.compose.runtime.Stable
import com.whatever.caro.core.model.card.Card
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlin.random.Random

@Stable
data class CardItem(
    val id: Long = 0L,
    val front: String = "",
    val back: String = "",
    val reviewCount: Int = 0,
    val reviewState: CardReviewState = CardReviewState.NEW,
) {
    companion object {
        fun toUiModel(card: Card): CardItem =
            CardItem(
                id = card.id,
                front = card.content.front,
                back = card.content.back,
            )

        fun fakeList(): ImmutableList<CardItem> =
            (0L..20L)
                .map { id ->
                    CardItem(
                        id = id,
                        front = id.toString(),
                        back = id.toString(),
                        reviewCount = Random.nextInt(from = 0, until = 10),
                        reviewState = CardReviewState.NEW,
                    )
                }.toImmutableList()
    }
}

enum class CardReviewState {
    NEW,
    REVIEW,
    HARD,
}
