package com.whatever.caro.core.data.repository.card

import com.whatever.caro.core.model.card.CardContent
import com.whatever.caro.core.model.exception.CaroInvalidResponseException
import com.whatever.caro.core.remote.datasource.card.CardDataSource
import com.whatever.caro.core.remote.dto.card.request.CreateCardItemDto
import com.whatever.caro.core.remote.dto.card.request.CreateCardItemDto.CardTypeDto
import com.whatever.caro.core.remote.dto.card.request.CreateCardsRequest
import com.whatever.caro.core.remote.dto.card.response.CardResponse
import com.whatever.caro.core.remote.dto.card.response.CreateCardsResponse
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode.Companion.exactly
import dev.mokkery.verifySuspend
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class CardRepositoryImplChunkTest : FunSpec() {
    init {
        test("createCardsChunk 는 멱등 키와 매핑한 요청을 datasource 에 넘기고 생성 장수를 돌려준다") {
            val cardDataSource =
                mock<CardDataSource> {
                    everySuspend { createCardChunk(any(), any(), any()) } returns created(2)
                }
            val repository = CardRepositoryImpl(cardDataSource = cardDataSource)

            val count = repository.createCardsChunk(deckId = 42L, cards = cards, idempotencyKey = KEY)

            count shouldBe 2
            verifySuspend(exactly(1)) {
                cardDataSource.createCardChunk(
                    deckId = 42L,
                    idempotencyKey = KEY,
                    request =
                        CreateCardsRequest(
                            items =
                                listOf(
                                    CreateCardItemDto(fields = mapOf("front" to "Run", "back" to "달리다"), cardType = CardTypeDto.BASIC),
                                    CreateCardItemDto(fields = mapOf("front" to "Walk", "back" to "걷다"), cardType = CardTypeDto.BASIC),
                                ),
                        ),
                )
            }
        }

        test("createCardsChunk 는 응답 항목 수가 요청 장수와 달라도 서버가 만든 장수를 그대로 돌려준다") {
            val cardDataSource =
                mock<CardDataSource> {
                    everySuspend { createCardChunk(any(), any(), any()) } returns created(1)
                }
            val repository = CardRepositoryImpl(cardDataSource = cardDataSource)

            repository.createCardsChunk(deckId = 42L, cards = cards, idempotencyKey = KEY) shouldBe 1
        }

        test("같은 카드 목록을 두 번 저장하면 저장소 매핑을 거친 요청 본문 바이트가 같다") {
            val requests = mutableListOf<CreateCardsRequest>()
            val repository = CardRepositoryImpl(cardDataSource = CapturingCardDataSource(requests))
            val sameList = listOf(CardContent(front = "사과\n\"따옴표\"", back = "apple"), CardContent(front = "가", back = ""))

            repeat(2) { repository.createCardsChunk(deckId = 42L, cards = sameList, idempotencyKey = KEY) }

            val bodies = requests.map { request -> Json.encodeToString(CreateCardsRequest.serializer(), request).encodeToByteArray() }
            bodies[0].contentEquals(bodies[1]) shouldBe true
        }

        test("createCardsChunk 는 응답 항목이 없으면 유효하지 않은 응답으로 처리한다") {
            val cardDataSource =
                mock<CardDataSource> {
                    everySuspend { createCardChunk(any(), any(), any()) } returns CreateCardsResponse(items = null)
                }
            val repository = CardRepositoryImpl(cardDataSource = cardDataSource)

            shouldThrow<CaroInvalidResponseException> {
                repository.createCardsChunk(deckId = 42L, cards = cards, idempotencyKey = KEY)
            }
        }
    }

    private class CapturingCardDataSource(
        private val requests: MutableList<CreateCardsRequest>,
    ) : CardDataSource by mock() {
        override suspend fun createCardChunk(
            deckId: Long,
            idempotencyKey: String,
            request: CreateCardsRequest,
        ): CreateCardsResponse {
            requests += request
            return CreateCardsResponse(
                items =
                    request.items.mapIndexed {
                        index,
                        _,
                        ->
                        CardResponse(cardId = index.toLong(), fields = null)
                    },
            )
        }
    }

    private fun created(count: Int) =
        CreateCardsResponse(items = List(count) { index -> CardResponse(cardId = index.toLong(), fields = null) })

    private companion object {
        const val KEY = "4f8f2c64-5f0e-4b55-9d2a-0a8c3e9b7a11"
        val cards = listOf(CardContent(front = "Run", back = "달리다"), CardContent(front = "Walk", back = "걷다"))
    }
}
