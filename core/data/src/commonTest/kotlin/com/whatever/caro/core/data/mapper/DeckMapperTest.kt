package com.whatever.caro.core.data.mapper

import com.whatever.caro.core.model.deck.DeckCardSortType
import com.whatever.caro.core.model.deck.DeckState
import com.whatever.caro.core.remote.dto.deck.response.DeckListResponse
import com.whatever.caro.core.remote.dto.studySession.response.StudySessionProgressResponseDto
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DeckMapperTest : FunSpec() {
    init {
        test("toDeckModel은 응답 필드를 Deck 모델로 매핑한다") {
            val response =
                DeckListResponse(
                    deckId = 9L,
                    name = "영어 단어",
                    description = "일상 단어",
                    cardCount = 30,
                    progress =
                        StudySessionProgressResponseDto(
                            state = StudySessionProgressResponseDto.StateDto.COMPLETED,
                            sessionId = 1L,
                            studiedCardCount = 20,
                            totalCardCount = 30,
                        ),
                )

            val deck = response.toDeckModel()

            deck.id shouldBe 9L
            deck.title shouldBe "영어 단어"
            deck.description shouldBe "일상 단어"
            deck.cardTotalCount shouldBe 30
            deck.todayLearningCount shouldBe 30
            deck.todayCompleteCount shouldBe 20
            deck.state shouldBe DeckState.COMPLETE
        }

        test("toDeckModel은 null 필드를 기본값으로 대체한다") {
            val response =
                DeckListResponse(
                    deckId = null,
                    name = null,
                    description = null,
                    cardCount = null,
                    progress = null,
                )

            val deck = response.toDeckModel()

            deck.id shouldBe 0L
            deck.title shouldBe ""
            deck.description shouldBe ""
            deck.cardTotalCount shouldBe 0
            deck.todayLearningCount shouldBe 0
            deck.todayCompleteCount shouldBe 0
            deck.state shouldBe DeckState.NOT_STARTED
        }

        test("toDeckState는 서버 상태를 DeckState로 매핑하며 null은 NOT_STARTED로 처리한다") {
            StudySessionProgressResponseDto.StateDto.NOT_STARTED.toDeckState() shouldBe DeckState.NOT_STARTED
            StudySessionProgressResponseDto.StateDto.IN_PROGRESS.toDeckState() shouldBe DeckState.LEARNING
            StudySessionProgressResponseDto.StateDto.COMPLETED.toDeckState() shouldBe DeckState.COMPLETE
            StudySessionProgressResponseDto.StateDto.REST_DAY.toDeckState() shouldBe DeckState.REST_DAY
            null.toDeckState() shouldBe DeckState.NOT_STARTED
        }

        test("toSortTypeQuery는 서버가 정의한 정렬 파라미터 값으로 매핑한다") {
            DeckCardSortType.CREATED.toSortTypeQuery() shouldBe "CREATED"
            DeckCardSortType.LAST_REVIEWED.toSortTypeQuery() shouldBe "LAST_REVIEWED"
            DeckCardSortType.FREQUENCY.toSortTypeQuery() shouldBe "REVIEW_FREQUENCY"
        }
    }
}
