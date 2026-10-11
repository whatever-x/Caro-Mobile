package com.whatever.caro.feature.importer.support

import com.whatever.caro.core.data.repository.card.CardRepository
import com.whatever.caro.core.model.card.CardContent
import kotlinx.coroutines.delay

internal class ChunkCall(
    val deckId: Long,
    val cards: List<CardContent>,
    val key: String,
)

/** 호출마다 미리 정한 결과를 차례로 돌려준다. 결과가 떨어지면 성공으로 본다. */
internal class ScriptedCardRepository(
    vararg outcomes: Throwable?,
) : CardRepository {
    val calls = mutableListOf<ChunkCall>()
    var onCall: () -> Unit = {}
    var createdCount: (Int) -> Int = { sent -> sent }

    // 호출을 기록한 뒤 이 시간만큼 기다려 응답을 늦춘다. 묶음이 전송 중인 순간을 만들 때 쓴다.
    var callDelayMillis = 0L
    private val script = ArrayDeque(outcomes.toList())

    override suspend fun createCardsChunk(
        deckId: Long,
        cards: List<CardContent>,
        idempotencyKey: String,
    ): Int {
        calls += ChunkCall(deckId, cards, idempotencyKey)
        onCall()
        if (callDelayMillis > 0) delay(callDelayMillis)
        script.removeFirstOrNull()?.let { failure -> throw failure }
        return createdCount(cards.size)
    }

    override suspend fun createCards(
        deckId: Long,
        cards: List<CardContent>,
    ) = error("not used")

    override suspend fun updateCard(
        cardId: Long,
        content: CardContent,
    ) = error("not used")

    override suspend fun deleteCards(cardIds: List<Long>) = error("not used")
}
