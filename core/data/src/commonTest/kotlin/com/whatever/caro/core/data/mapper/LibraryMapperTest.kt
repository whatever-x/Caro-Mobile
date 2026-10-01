package com.whatever.caro.core.data.mapper

import com.whatever.caro.core.model.deck.DeckState
import com.whatever.caro.core.remote.dto.library.LibraryCardResponse
import com.whatever.caro.core.remote.dto.library.LibraryCopyResponse
import com.whatever.caro.core.remote.dto.library.LibraryDetailResponse
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class LibraryMapperTest :
    FunSpec({
        test("복사 응답은 새 개인 덱의 이름과 카드 수를 사용하며 학습을 시작하지 않는다") {
            val deck = LibraryCopyResponse(42, "단어 (3)", "뜻", 200).toPersonalDeck()
            deck.id shouldBe 42
            deck.title shouldBe "단어 (3)"
            deck.cardTotalCount shouldBe 200
            deck.state shouldBe DeckState.NOT_STARTED
            deck.todayLearningCount shouldBe 0
        }
        test("미리보기는 제공 덱 ID와 순서를 보존한다") {
            val model =
                LibraryDetailResponse(
                    2,
                    "단어",
                    "",
                    2,
                    listOf(
                        LibraryCardResponse(9, "b", "비", 1),
                        LibraryCardResponse(8, "a", "에이", 0),
                    ),
                ).toModel()
            model.deck.id shouldBe 2
            model.cards.map { it.id } shouldBe listOf(8, 9)
        }
    })
