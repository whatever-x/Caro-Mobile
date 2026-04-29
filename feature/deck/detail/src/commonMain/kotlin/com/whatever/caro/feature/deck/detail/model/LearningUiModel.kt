package com.whatever.caro.feature.deck.detail.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class LearningUiModel(
    val learningCardList: ImmutableList<CardItem> = persistentListOf(),
    val reviewedCardCount: Int = 0,
) {
    val learningCardTotal: Int
        get() = learningCardList.size

    val learningProgress: Int
        get() {
            val result = (reviewedCardCount.toFloat() / learningCardTotal.toFloat()) * 100f

            return result.toInt()
        }

    val currentLearningStatus: LearningStatus
        get() =
            when {
                reviewedCardCount == 0 && learningCardTotal > 0 -> LearningStatus.READY
                reviewedCardCount == 0 && learningCardTotal == 0 -> LearningStatus.UNAVAILABLE
                reviewedCardCount in 1..<learningCardTotal -> LearningStatus.IN_PROGRESS
                reviewedCardCount == learningCardTotal -> LearningStatus.COMPLETED
                else -> error("현재 학습 상태가 될 수 없습니다.")
            }

    companion object {
        fun preview(): LearningUiModel =
            LearningUiModel(
                learningCardList = CardItem.fakeList(),
                reviewedCardCount = 0,
            )
    }
}

enum class LearningStatus {
    READY,
    IN_PROGRESS,
    COMPLETED,
    UNAVAILABLE,
}
