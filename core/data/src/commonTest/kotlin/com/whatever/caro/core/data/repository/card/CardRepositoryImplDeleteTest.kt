package com.whatever.caro.core.data.repository.card

import com.whatever.caro.core.model.exception.CaroInvalidResponseException
import com.whatever.caro.core.remote.datasource.card.CardDataSource
import com.whatever.caro.core.remote.dto.card.request.DeleteCardsRequest
import com.whatever.caro.core.remote.dto.card.response.DeleteCardResponse
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode.Companion.exactly
import dev.mokkery.verifySuspend
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec

class CardRepositoryImplDeleteTest : FunSpec() {
    init {
        test("deleteCards 는 선택된 카드 id 를 request body 로 datasource 에 위임한다") {
            val cardDataSource =
                mock<CardDataSource> {
                    everySuspend { deleteCards(any()) } returns DeleteCardResponse(deletedCardsCount = 2)
                }
            val repository = CardRepositoryImpl(cardDataSource = cardDataSource)

            repository.deleteCards(cardIds = listOf(1L, 2L))

            verifySuspend(exactly(1)) {
                cardDataSource.deleteCards(request = DeleteCardsRequest(cardIds = setOf(1L, 2L)))
            }
        }

        test("deleteCards 는 요청한 카드 수와 삭제된 카드 수가 다르면 유효하지 않은 응답으로 처리한다") {
            val cardDataSource =
                mock<CardDataSource> {
                    everySuspend { deleteCards(any()) } returns DeleteCardResponse(deletedCardsCount = 1)
                }
            val repository = CardRepositoryImpl(cardDataSource = cardDataSource)

            shouldThrow<CaroInvalidResponseException> {
                repository.deleteCards(cardIds = listOf(1L, 2L))
            }
        }

        test("deleteCards 는 삭제된 카드 수가 없으면 유효하지 않은 응답으로 처리한다") {
            val cardDataSource =
                mock<CardDataSource> {
                    everySuspend { deleteCards(any()) } returns DeleteCardResponse(deletedCardsCount = null)
                }
            val repository = CardRepositoryImpl(cardDataSource = cardDataSource)

            shouldThrow<CaroInvalidResponseException> {
                repository.deleteCards(cardIds = listOf(1L))
            }
        }
    }
}
