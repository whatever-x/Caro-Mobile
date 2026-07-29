package com.whatever.caro.feature.deck.detail

import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.model.deck.DeckState
import com.whatever.caro.feature.deck.detail.mvi.DeckDetailState
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue

class DeckDetailStateTest : FunSpec() {
    init {
        val deck =
            Deck(
                id = 1L,
                title = "영어 단어",
                description = "매일 공부할 영어 단어 덱",
                cardTotalCount = 24,
                todayLearningCount = 10,
                todayCompleteCount = 4,
                state = DeckState.LEARNING,
            )

        test("카드 목록 초기 로딩 중에는 덱 상세 본문을 노출하지 않는다") {
            DeckDetailState(deck = deck, isCardListLoading = true).isLoadedContentVisible.shouldBeFalse()
        }

        test("카드 목록 초기 로딩이 끝나면 덱 상세 본문을 노출한다") {
            DeckDetailState(deck = deck, isCardListLoading = false).isLoadedContentVisible.shouldBeTrue()
        }
    }
}
