package com.whatever.caro.core.data.repository.card

import com.whatever.caro.core.model.card.CardContent

interface CardRepository {
    suspend fun createCards(
        deckId: Long,
        cards: List<CardContent>,
    )

    /**
     * 가져오기 묶음을 멱등 키와 함께 저장하고 서버가 만든 장수를 돌려준다. 재시도에는 같은 [cards] 목록과 같은 키를 넘겨야 서버가 첫 응답을 재생한다.
     * 만든 장수가 보낸 장수와 다르면 서버는 이미 저장했으므로 같은 키 재시도로 고칠 수 없다. 그 판단은 호출자가 한다.
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
