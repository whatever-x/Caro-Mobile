package com.whatever.caro.core.data.repository.card

import com.whatever.caro.core.model.card.Card
import com.whatever.caro.core.model.card.CardContent
import com.whatever.caro.core.remote.datasource.card.CardDataSource
import com.whatever.caro.core.remote.dto.cardController.response.CardResponse
import com.whatever.caro.core.remote.dto.cardController.response.DeleteCardResponse
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode.Companion.exactly
import dev.mokkery.verifySuspend
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CardRepositoryImplDeleteTest : FunSpec() {
    init {
        test("getCards 는 카드 응답을 Card 도메인 모델로 매핑한다") {
            val cardDataSource =
                mock<CardDataSource> {
                    everySuspend { getCards(any()) } returns
                        listOf(
                            CardResponse(
                                cardId = 1L,
                                fields = mapOf("front" to "apple", "back" to "사과"),
                            ),
                            CardResponse(
                                cardId = 2L,
                                fields = mapOf("front" to "run", "back" to "달리다"),
                            ),
                        )
                }
            val repository = CardRepositoryImpl(cardDataSource = cardDataSource)

            val result = repository.getCards(deckId = 42L)

            result shouldBe
                listOf(
                    Card(id = 1L, content = CardContent(front = "apple", back = "사과")),
                    Card(id = 2L, content = CardContent(front = "run", back = "달리다")),
                )
        }

        test("deleteCards 는 선택된 카드 id 를 각각 datasource 에 위임한다") {
            val cardDataSource =
                mock<CardDataSource> {
                    everySuspend { deleteCard(any()) } returns DeleteCardResponse(cardId = null)
                }
            val repository = CardRepositoryImpl(cardDataSource = cardDataSource)

            repository.deleteCards(cardIds = listOf(1L, 2L))

            verifySuspend(exactly(1)) {
                cardDataSource.deleteCard(cardId = 1L)
            }
            verifySuspend(exactly(1)) {
                cardDataSource.deleteCard(cardId = 2L)
            }
        }
    }
}
