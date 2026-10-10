package com.whatever.caro.core.data.repository.card

import com.whatever.caro.core.model.card.CardContent

interface CardRepository {
    suspend fun createCards(
        deckId: Long,
        cards: List<CardContent>,
    )

    /**
     * 가져오기 묶음을 멱등 키와 함께 저장하고 만든 장수를 돌려준다. 재시도에는 같은 [cards] 목록과 같은 키를 넘겨야 서버가 첫 응답을 재생한다.
     */
    suspend fun createCardsChunk(
        deckId: Long,
        cards: List<CardContent>,
        idempotencyKey: String,
    ): Int

    suspend fun updateCard(
        cardId: Long,
        content: CardContent,
    )

    suspend fun deleteCards(cardIds: List<Long>)
}
