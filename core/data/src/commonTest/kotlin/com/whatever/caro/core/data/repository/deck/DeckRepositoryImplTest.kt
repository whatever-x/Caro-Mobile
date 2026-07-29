package com.whatever.caro.core.data.repository.deck

import com.whatever.caro.core.model.card.CardBadge
import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.model.deck.DeckState
import com.whatever.caro.core.model.exception.CaroInvalidResponseException
import com.whatever.caro.core.remote.datasource.deck.DeckDataSource
import com.whatever.caro.core.remote.dto.deck.request.CreateDeckRequest
import com.whatever.caro.core.remote.dto.deck.request.UpdateDeckRequest
import com.whatever.caro.core.remote.dto.deck.response.CreateDeckResponse
import com.whatever.caro.core.remote.dto.deck.response.DeckCardResponse
import com.whatever.caro.core.remote.dto.deck.response.DeckListResponse
import com.whatever.caro.core.remote.dto.deck.response.DeleteDeckResponse
import com.whatever.caro.core.remote.dto.deck.response.UpdateDeckResponse
import com.whatever.caro.core.remote.dto.studySession.response.StudySessionProgressResponseDto
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
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

                val result = repository.createDeck(name = "영어 단어", description = "일상 단어")

                result shouldBe
                    Deck(
                        id = 1L,
                        title = "영어 단어",
                        description = "일상 단어",
                        cardTotalCount = 0,
                        todayLearningCount = 0,
                        todayCompleteCount = 0,
                        state = DeckState.NOT_STARTED,
                    )

                verifySuspend {
                    deckDataSource.createDeck(
                        CreateDeckRequest(name = "영어 단어", description = "일상 단어"),
                    )
                }
            }
        }

        test("createDeck은 응답의 이름과 설명이 없으면 요청 값을 사용한다") {
            runTest {
                val deckDataSource =
                    mock<DeckDataSource> {
                        everySuspend { createDeck(any()) } returns
                            CreateDeckResponse(id = 2L, deckName = null, deckDescription = null)
                    }
                val repository = DeckRepositoryImpl(deckDataSource)

                val result = repository.createDeck(name = "요청 이름", description = "요청 설명")

                result shouldBe
                    Deck(
                        id = 2L,
                        title = "요청 이름",
                        description = "요청 설명",
                        cardTotalCount = 0,
                        todayLearningCount = 0,
                        todayCompleteCount = 0,
                        state = DeckState.NOT_STARTED,
                    )
            }
        }

        test("createDeck은 응답 id가 없으면 유효하지 않은 응답 예외를 던진다") {
            runTest {
                val deckDataSource =
                    mock<DeckDataSource> {
                        everySuspend { createDeck(any()) } returns
                            CreateDeckResponse(id = null, deckName = "이름", deckDescription = "설명")
                    }
                val repository = DeckRepositoryImpl(deckDataSource)

                shouldThrow<CaroInvalidResponseException> {
                    repository.createDeck(name = "이름", description = "설명")
                }
            }
        }

        test("updateDeck은 덱 id와 함께 이름/설명 수정 요청을 전달한다") {
            runTest {
                val deckDataSource =
                    mock<DeckDataSource> {
                        everySuspend { updateDeck(any(), any()) } returns
                            UpdateDeckResponse(id = 7L, deckName = "새 이름", deckDescription = "새 설명")
                    }
                val repository = DeckRepositoryImpl(deckDataSource)

                repository.updateDeck(deckId = 7L, name = "새 이름", description = "새 설명")

                verifySuspend {
                    deckDataSource.updateDeck(
                        deckId = 7L,
                        request = UpdateDeckRequest(name = "새 이름", description = "새 설명"),
                    )
                }
            }
        }

        test("deleteDeck은 덱 id로 삭제 요청을 전달한다") {
            runTest {
                val deckDataSource =
                    mock<DeckDataSource> {
                        everySuspend { deleteDeck(any()) } returns DeleteDeckResponse(id = 3L)
                    }
                val repository = DeckRepositoryImpl(deckDataSource)

                repository.deleteDeck(deckId = 3L)

                verifySuspend {
                    deckDataSource.deleteDeck(deckId = 3L)
                }
            }
        }

        test("getDecks는 데이터소스 응답을 Deck 모델로 매핑한다") {
            runTest {
                val deckDataSource =
                    mock<DeckDataSource> {
                        everySuspend { getDecks() } returns
                            listOf(
                                DeckListResponse(
                                    deckId = 5L,
                                    name = "영어 단어",
                                    description = "일상 단어",
                                    cardCount = 30,
                                    progress =
                                        StudySessionProgressResponseDto(
                                            state = StudySessionProgressResponseDto.StateDto.IN_PROGRESS,
                                            sessionId = 1L,
                                            studiedCardCount = 12,
                                            totalCardCount = 24,
                                        ),
                                ),
                            )
                    }
                val repository = DeckRepositoryImpl(deckDataSource)

                val decks = repository.getDecks()

                decks.size shouldBe 1
                val deck = decks.first()
                deck.id shouldBe 5L
                deck.title shouldBe "영어 단어"
                deck.description shouldBe "일상 단어"
                deck.cardTotalCount shouldBe 30
                deck.todayLearningCount shouldBe 24
                deck.todayCompleteCount shouldBe 12
                deck.state shouldBe DeckState.LEARNING
            }
        }

        test("getDeckCards는 badge/복습 수를 포함해 DeckCard 모델로 매핑하고 cardId 가 없는 응답은 제외한다") {
            runTest {
                val deckDataSource =
                    mock<DeckDataSource> {
                        everySuspend { getDeckCards(any()) } returns
                            listOf(
                                DeckCardResponse(
                                    cardId = 1L,
                                    fields = mapOf("front" to "Run", "back" to "달리다"),
                                    badge = DeckCardResponse.BadgeDto.HARD,
                                    reviewCount = 5,
                                ),
                                DeckCardResponse(
                                    cardId = null,
                                    fields = mapOf("front" to "Skip", "back" to "제외"),
                                    badge = DeckCardResponse.BadgeDto.NEW,
                                    reviewCount = 0,
                                ),
                            )
                    }
                val repository = DeckRepositoryImpl(deckDataSource)

                val cards = repository.getDeckCards(deckId = 42L)

                cards.size shouldBe 1
                val card = cards.first()
                card.id shouldBe 1L
                card.content.front shouldBe "Run"
                card.content.back shouldBe "달리다"
                card.badge shouldBe CardBadge.HARD
                card.reviewCount shouldBe 5
            }
        }
    }
}
