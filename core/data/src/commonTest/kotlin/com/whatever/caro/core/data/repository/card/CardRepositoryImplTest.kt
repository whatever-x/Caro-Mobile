package com.whatever.caro.core.data.repository.card

import com.whatever.caro.core.model.card.CardContent
import com.whatever.caro.core.remote.datasource.card.CardDataSource
import com.whatever.caro.core.remote.dto.cardController.request.CreateCardItemDto
import com.whatever.caro.core.remote.dto.cardController.request.CreateCardItemDto.CardTypeDto
import com.whatever.caro.core.remote.dto.cardController.request.CreateCardsRequest
import com.whatever.caro.core.remote.dto.cardController.response.CreateCardsResponse
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode.Companion.exactly
import dev.mokkery.verifySuspend
import io.kotest.core.spec.style.FunSpec

class CardRepositoryImplTest : FunSpec() {
    init {
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
    }
}
