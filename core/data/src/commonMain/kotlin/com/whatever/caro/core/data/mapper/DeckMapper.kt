package com.whatever.caro.core.data.mapper

import com.whatever.caro.core.model.card.CardBadge
import com.whatever.caro.core.model.card.CardContent
import com.whatever.caro.core.model.card.DeckCard
import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.model.deck.DeckState
import com.whatever.caro.core.remote.dto.deckCardInformation.response.DeckCardResponse
import com.whatever.caro.core.remote.dto.deckCardInformation.response.DeckListResponse
import com.whatever.caro.core.remote.dto.studySession.response.StudySessionProgressResponseDto

private const val FIELD_FRONT = "front"
private const val FIELD_BACK = "back"

internal fun DeckCardResponse.toDeckCardModel(): DeckCard? {
    val id = cardId ?: return null
    return DeckCard(
        id = id,
        content =
            CardContent(
                front = fields?.get(FIELD_FRONT).orEmpty(),
                back = fields?.get(FIELD_BACK).orEmpty(),
            ),
        badge = badge.toCardBadge(),
        reviewCount = reviewCount ?: 0,
    )
}

private fun DeckCardResponse.BadgeDto?.toCardBadge(): CardBadge =
    when (this) {
        DeckCardResponse.BadgeDto.NEW -> CardBadge.NEW
        DeckCardResponse.BadgeDto.REVIEW -> CardBadge.REVIEW
        DeckCardResponse.BadgeDto.HARD -> CardBadge.HARD
        null -> CardBadge.NEW
    }

internal fun DeckListResponse.toDeckModel() =
    Deck(
        id = this.deckId ?: 0L,
        title = this.name ?: "",
        description = this.description ?: "",
        cardTotalCount = this.cardCount ?: 0,
        todayLearningCount = this.progress?.totalCardCount ?: 0,
        todayCompleteCount = this.progress?.studiedCardCount ?: 0,
        state = this.progress?.state.toDeckState(),
    )

internal fun StudySessionProgressResponseDto.StateDto?.toDeckState() =
    when (this) {
        StudySessionProgressResponseDto.StateDto.NOT_STARTED -> DeckState.NOT_STARTED

        StudySessionProgressResponseDto.StateDto.IN_PROGRESS -> DeckState.LEARNING

        StudySessionProgressResponseDto.StateDto.COMPLETED -> DeckState.COMPLETE

        StudySessionProgressResponseDto.StateDto.REST_DAY -> DeckState.REST_DAY

        // TODO: null 어떻게 가져갈지
        else -> DeckState.NOT_STARTED
    }
