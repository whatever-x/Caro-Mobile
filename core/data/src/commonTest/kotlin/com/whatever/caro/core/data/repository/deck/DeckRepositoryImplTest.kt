package com.whatever.caro.core.data.repository.deck

import com.whatever.caro.core.remote.datasource.deck.DeckDataSource
import com.whatever.caro.core.remote.dto.deck.request.CreateDeckRequest
import com.whatever.caro.core.remote.dto.deck.response.CreateDeckResponse
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import io.kotest.core.spec.style.FunSpec
import kotlinx.coroutines.test.runTest

class DeckRepositoryImplTest : FunSpec() {
    init {
        test("createDeck은 이름과 설명으로 덱 생성 요청을 전달한다") {
            runTest {
                val deckDataSource =
                    mock<DeckDataSource> {
                        everySuspend { createDeck(any()) } returns
                            CreateDeckResponse(id = 1L, deckName = "영어 단어", deckDescription = "일상 단어")
                    }
                val repository = DeckRepositoryImpl(deckDataSource)

                repository.createDeck(name = "영어 단어", description = "일상 단어")

                verifySuspend {
                    deckDataSource.createDeck(
                        CreateDeckRequest(name = "영어 단어", description = "일상 단어"),
                    )
                }
            }
        }
    }
}
