package com.whatever.caro.core.model.deck

import kotlinx.serialization.Serializable

@Serializable
data class Deck(
    val id: Long,
    val title: String,
    val description: String,
    val cardTotalCount: Int,
    val todayLearningCount: Int,
    val todayCompleteCount: Int,
    val state: DeckState,
) {
    val todayProgress: Int
        get() =
            if (todayLearningCount == 0) {
                0
            } else {
                ((todayCompleteCount * 100) / todayLearningCount).coerceIn(0, 100)
            }
}
