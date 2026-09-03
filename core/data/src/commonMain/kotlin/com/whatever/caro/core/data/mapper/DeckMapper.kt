package com.whatever.caro.core.data.mapper

import com.whatever.caro.core.model.card.CardBadge
import com.whatever.caro.core.model.card.CardContent
import com.whatever.caro.core.model.card.DeckCard
import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.model.deck.DeckCardSortType
import com.whatever.caro.core.model.deck.DeckState
import com.whatever.caro.core.model.exception.CaroInvalidResponseException
import com.whatever.caro.core.remote.dto.deck.response.CreateDeckResponse
import com.whatever.caro.core.remote.dto.deck.response.DeckCardResponse
import com.whatever.caro.core.remote.dto.deck.response.DeckListResponse
import com.whatever.caro.core.remote.dto.studySession.response.StudySessionProgressResponseDto

private const val FIELD_FRONT = "front"
private const val FIELD_BACK = "back"

// 서버 정렬 파라미터 값. 복습 횟수 정렬만 도메인 enum 이름(FREQUENCY)과 달라 REVIEW_FREQUENCY 로 보낸다.
private const val SORT_TYPE_CREATED = "CREATED"
private const val SORT_TYPE_LAST_REVIEWED = "LAST_REVIEWED"
private const val SORT_TYPE_REVIEW_FREQUENCY = "REVIEW_FREQUENCY"

internal fun DeckCardSortType.toSortTypeQuery(): String =
    when (this) {
        DeckCardSortType.CREATED -> SORT_TYPE_CREATED
        DeckCardSortType.LAST_REVIEWED -> SORT_TYPE_LAST_REVIEWED
        DeckCardSortType.FREQUENCY -> SORT_TYPE_REVIEW_FREQUENCY
    }

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

internal fun CreateDeckResponse.toDeckModel(
    fallbackName: String,
    fallbackDescription: String,
): Deck =
    Deck(
        id = id ?: throw CaroInvalidResponseException(debugMessage = "CreateDeckResponse.id is null"),
        title = deckName ?: fallbackName,
        description = deckDescription ?: fallbackDescription,
        cardTotalCount = 0,
        todayLearningCount = 0,
        todayCompleteCount = 0,
        state = DeckState.NOT_STARTED,
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
