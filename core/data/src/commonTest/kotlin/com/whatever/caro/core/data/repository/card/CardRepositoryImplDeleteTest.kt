package com.whatever.caro.core.data.repository.card

import com.whatever.caro.core.remote.datasource.card.CardDataSource
import com.whatever.caro.core.remote.dto.cardController.request.DeleteCardsRequest
import com.whatever.caro.core.remote.dto.cardController.response.DeleteCardResponse
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode.Companion.exactly
import dev.mokkery.verifySuspend
import io.kotest.core.spec.style.FunSpec

class CardRepositoryImplDeleteTest : FunSpec() {
    init {
        test("deleteCards 는 선택된 카드 id 를 request body 로 datasource 에 위임한다") {
            val cardDataSource =
                mock<CardDataSource> {
                    everySuspend { deleteCards(any()) } returns DeleteCardResponse(deletedCardsCount = null)
                }
            val repository = CardRepositoryImpl(cardDataSource = cardDataSource)

            repository.deleteCards(cardIds = listOf(1L, 2L))

            verifySuspend(exactly(1)) {
                cardDataSource.deleteCards(request = DeleteCardsRequest(cardIds = setOf(1L, 2L)))
            }
        }
    }
}
