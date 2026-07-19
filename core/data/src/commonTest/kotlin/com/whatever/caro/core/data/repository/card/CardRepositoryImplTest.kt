package com.whatever.caro.core.data.repository.card

import com.whatever.caro.core.model.card.Card
import com.whatever.caro.core.model.card.CardContent
import com.whatever.caro.core.remote.datasource.card.CardDataSource
import com.whatever.caro.core.remote.dto.cardController.request.CreateCardItemDto
import com.whatever.caro.core.remote.dto.cardController.request.CreateCardItemDto.CardTypeDto
import com.whatever.caro.core.remote.dto.cardController.request.CreateCardsRequest
import com.whatever.caro.core.remote.dto.cardController.request.UpdateCardRequest
import com.whatever.caro.core.remote.dto.cardController.response.CardResponse
import com.whatever.caro.core.remote.dto.cardController.response.CreateCardsResponse
import com.whatever.caro.core.remote.dto.cardController.response.UpdateCardResponse
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode.Companion.exactly
import dev.mokkery.verifySuspend
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CardRepositoryImplTest : FunSpec() {
    init {
        test("getCards 는 응답 DTO 를 Card 모델로 매핑한다") {
            val cardDataSource =
                mock<CardDataSource> {
                    everySuspend { getCards(any()) } returns
                        listOf(
                            CardResponse(
                                cardId = 1L,
                                fields = mapOf("front" to "Run", "back" to "달리다"),
                            ),
                            CardResponse(
                                cardId = 2L,
                                fields = mapOf("front" to "Walk", "back" to "걷다"),
                            ),
                            CardResponse(
                                cardId = null,
                                fields = mapOf("front" to "Skip", "back" to "제외"),
                            ),
                        )
                }
            val repository = CardRepositoryImpl(cardDataSource = cardDataSource)

            repository.getCards(deckId = 42L) shouldBe
                listOf(
                    Card(id = 1L, content = CardContent(front = "Run", back = "달리다")),
                    Card(id = 2L, content = CardContent(front = "Walk", back = "걷다")),
                )
        }

        test("createCards 는 CardContent 를 요청 DTO 로 매핑해 datasource 에 위임한다") {
            val cardDataSource =
                mock<CardDataSource> {
                    everySuspend { createCards(any(), any()) } returns CreateCardsResponse(items = null)
                }
            val repository = CardRepositoryImpl(cardDataSource = cardDataSource)

            repository.createCards(
                deckId = 42L,
                cards =
                    listOf(
                        CardContent(front = "Run", back = "달리다"),
                        CardContent(front = "Walk", back = "걷다"),
                    ),
            )

            verifySuspend(exactly(1)) {
                cardDataSource.createCards(
                    deckId = 42L,
                    request =
                        CreateCardsRequest(
                            items =
                                listOf(
                                    CreateCardItemDto(
                                        fields = mapOf("front" to "Run", "back" to "달리다"),
                                        cardType = CardTypeDto.BASIC,
                                    ),
                                    CreateCardItemDto(
                                        fields = mapOf("front" to "Walk", "back" to "걷다"),
                                        cardType = CardTypeDto.BASIC,
                                    ),
                                ),
                        ),
                )
            }
        }

        test("updateCard 는 CardContent 를 수정 요청 DTO 로 매핑해 datasource 에 위임한다") {
            val cardDataSource =
                mock<CardDataSource> {
                    everySuspend { updateCard(any(), any()) } returns
                        UpdateCardResponse(
                            cardId = 7L,
                            fields = null,
                        )
                }
            val repository = CardRepositoryImpl(cardDataSource = cardDataSource)

            repository.updateCard(
                cardId = 7L,
                content = CardContent(front = "Run", back = "달리다"),
            )

            verifySuspend(exactly(1)) {
                cardDataSource.updateCard(
                    cardId = 7L,
                    request =
                        UpdateCardRequest(
                            fields = mapOf("front" to "Run", "back" to "달리다"),
                        ),
                )
            }
        }
    }
}
