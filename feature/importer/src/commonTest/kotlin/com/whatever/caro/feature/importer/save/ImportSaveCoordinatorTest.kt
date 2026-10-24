package com.whatever.caro.feature.importer.save

import com.whatever.caro.core.data.repository.card.CardRepository
import com.whatever.caro.core.data.repository.deck.DeckRepository
import com.whatever.caro.core.model.card.CardContent
import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.model.deck.DeckState
import com.whatever.caro.core.model.exception.CaroAuthException
import com.whatever.caro.core.model.exception.CaroInvalidResponseException
import com.whatever.caro.core.model.exception.CaroServerException
import com.whatever.caro.core.model.exception.NetworkException
import com.whatever.caro.feature.importer.support.ScriptedCardRepository
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode.Companion.exactly
import dev.mokkery.verifySuspend
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeUnique
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest

class ImportSaveCoordinatorTest : FunSpec() {
    init {
        test("묶음을 원본 순서대로 하나씩 보내고 묶음마다 다른 키를 쓴다") {
            runTest {
                val cards = ScriptedCardRepository()
                val coordinator = coordinator(cards)
                val chunks = coordinator.planChunks(List(2_500) { CardContent("앞$it", "뒤$it") })

                val outcome = coordinator.saveChunks(DECK_ID, chunks)

                outcome shouldBe ImportSaveOutcome.Completed(savedCards = 2_500)
                cards.calls.map { it.cards.first().front } shouldBe listOf("앞0", "앞1000", "앞2000")
                cards.calls.map { it.key }.shouldBeUnique()
            }
        }

        test("네트워크 오류와 C999는 같은 키·같은 카드 목록으로 3번까지 자동 재시도한다") {
            runTest {
                val cards = ScriptedCardRepository(networkTimeout(), serverError(C999), networkTimeout())
                val coordinator = coordinator(cards)
                val chunk = coordinator.planChunks(listOf(CardContent("a", "b"))).single()

                coordinator.saveChunks(DECK_ID, listOf(chunk)) shouldBe ImportSaveOutcome.Completed(savedCards = 1)

                cards.calls.size shouldBe 4
                cards.calls.map { it.key }.toSet() shouldBe setOf(chunk.idempotencyKey)
                cards.calls.forEach { call -> call.cards shouldBeSameInstanceAs chunk.cards }
            }
        }

        test("자동 재시도를 다 쓰면 같은 키로 다시 시도할 수 있는 일시 정지로 멈춘다") {
            runTest {
                val cards = ScriptedCardRepository(networkTimeout(), networkTimeout(), networkTimeout(), networkTimeout())
                val coordinator = coordinator(cards)
                val chunks = coordinator.planChunks(listOf(CardContent("a", "b")))

                val outcome = coordinator.saveChunks(DECK_ID, chunks)

                outcome shouldBe ImportSaveOutcome.Stopped(chunkIndex = 0, savedCards = 0, failure = ChunkFailure.RetryExhausted)
                outcome.failure.userRetry shouldBe UserRetry.SAME_KEY
                cards.calls.size shouldBe 4
            }
        }

        test("C010은 실패로 세지 않고 기다렸다가 같은 키로 다시 보낸다") {
            runTest {
                val cards = ScriptedCardRepository(processing(), processing(), processing(), processing(), networkTimeout())
                val coordinator = coordinator(cards, this)

                val outcome = coordinator.saveChunks(DECK_ID, coordinator.planChunks(listOf(CardContent("a", "b"))))

                outcome shouldBe ImportSaveOutcome.Completed(savedCards = 1)
                cards.calls.size shouldBe 6
            }
        }

        test("C010 대기는 그 묶음의 첫 전송부터 25초를 넘기지 않고 일시 정지로 멈춘다") {
            runTest {
                val cards = ScriptedCardRepository(*Array(30) { processing() })
                val coordinator = coordinator(cards, this)

                val outcome = coordinator.saveChunks(DECK_ID, coordinator.planChunks(listOf(CardContent("a", "b"))))

                outcome.failure shouldBe ChunkFailure.ProcessingTimeout
                (currentTime <= 25_000L) shouldBe true
            }
        }

        test("확정 4xx는 자동 재시도 없이 멈추고 사용자 재시도에는 새 키를 쓴다") {
            runTest {
                val cards = ScriptedCardRepository(serverError("C001"))
                val coordinator = coordinator(cards)
                val chunk = coordinator.planChunks(listOf(CardContent("a", "b"))).single()

                val outcome = coordinator.saveChunks(DECK_ID, listOf(chunk))

                outcome.failure shouldBe ChunkFailure.Rejected("C001")
                outcome.failure.userRetry shouldBe UserRetry.NEW_KEY
                cards.calls.size shouldBe 1
                val renewed = coordinator.renewKey(chunk)
                (renewed.idempotencyKey != chunk.idempotencyKey) shouldBe true
                renewed.cards shouldBeSameInstanceAs chunk.cards
            }
        }

        test("C009는 다시 시도를 제공하지 않고 멈춘다") {
            runTest {
                val cards = ScriptedCardRepository(serverError("C009"))
                val coordinator = coordinator(cards)

                val outcome = coordinator.saveChunks(DECK_ID, coordinator.planChunks(listOf(CardContent("a", "b"))))

                outcome.failure shouldBe ChunkFailure.KeyConflict
                outcome.failure.userRetry shouldBe UserRetry.NONE
                cards.calls.size shouldBe 1
            }
        }

        test("JSON이 아닌 응답은 서버 저장 여부를 모르므로 자동 재시도 없이 같은 키 재시도로 멈춘다") {
            runTest {
                val cards = ScriptedCardRepository(CaroInvalidResponseException(debugMessage = "html"))
                val coordinator = coordinator(cards)

                val outcome = coordinator.saveChunks(DECK_ID, coordinator.planChunks(listOf(CardContent("a", "b"))))

                outcome.failure shouldBe ChunkFailure.InvalidResponse
                outcome.failure.userRetry shouldBe UserRetry.SAME_KEY
                cards.calls.size shouldBe 1
            }
        }

        test("덱이 없거나 권한이 없으면 덱을 찾을 수 없음으로 멈춘다") {
            runTest {
                val coordinator = coordinator(ScriptedCardRepository(serverError("D001")))

                coordinator.saveChunks(DECK_ID, coordinator.planChunks(listOf(CardContent("a", "b")))).failure shouldBe
                    ChunkFailure.DeckUnavailable
            }
        }

        test("멈춘 묶음부터 다시 시작하면 성공한 묶음은 다시 보내지 않는다") {
            runTest {
                val cards = ScriptedCardRepository(SUCCESS, serverError("C001"))
                val coordinator = coordinator(cards)
                val chunks = coordinator.planChunks(List(1_500) { CardContent("앞$it", "뒤$it") })

                val stopped = coordinator.saveChunks(DECK_ID, chunks)
                val resumed =
                    coordinator.saveChunks(
                        DECK_ID,
                        chunks.toMutableList().also { it[1] = coordinator.renewKey(it[1]) },
                        fromIndex = 1,
                    )

                stopped shouldBe ImportSaveOutcome.Stopped(chunkIndex = 1, savedCards = 1_000, failure = ChunkFailure.Rejected("C001"))
                resumed shouldBe ImportSaveOutcome.Completed(savedCards = 500)
                cards.calls.map { it.cards.first().front } shouldBe listOf("앞0", "앞1000", "앞1000")
            }
        }

        test("멈추기를 요청하면 진행 중 묶음은 재시도하지 않고 다음 묶음을 시작하지 않는다") {
            runTest {
                var stopRequested = false
                val cards = ScriptedCardRepository(SUCCESS, networkTimeout())
                cards.onCall = { if (cards.calls.size == 2) stopRequested = true }
                val coordinator = coordinator(cards)
                val chunks = coordinator.planChunks(List(2_500) { CardContent("앞$it", "뒤$it") })

                val outcome = coordinator.saveChunks(DECK_ID, chunks, stopRequested = { stopRequested })

                outcome shouldBe ImportSaveOutcome.Stopped(chunkIndex = 1, savedCards = 1_000, failure = ChunkFailure.StoppedByUser)
                cards.calls.size shouldBe 2
            }
        }

        test("자동 재시도 횟수는 재시도 간격 목록 길이에서 나온다") {
            ImportSaveLimits.AUTO_RETRY_COUNT shouldBe ImportSaveLimits.RETRY_BACKOFF.size
        }

        test("재시도 간격을 기다리는 중에 멈추기를 요청하면 다시 보내지 않는다") {
            runTest {
                var stopRequested = false
                val cards = ScriptedCardRepository(networkTimeout())
                val coordinator = coordinator(cards)
                launch {
                    delay(1_000)
                    stopRequested = true
                }

                val outcome =
                    coordinator.saveChunks(
                        DECK_ID,
                        coordinator.planChunks(listOf(CardContent("a", "b"))),
                        stopRequested = { stopRequested },
                    )

                outcome.failure shouldBe ChunkFailure.StoppedByUser
                cards.calls.size shouldBe 1
            }
        }

        test("같은 키로 다시 시도해도 C010 대기 상한은 그 키의 첫 전송부터 센다") {
            runTest {
                val cards =
                    ScriptedCardRepository(processing(), CaroInvalidResponseException(debugMessage = "html"), *Array(30) { processing() })
                val coordinator = coordinator(cards)
                val chunks = coordinator.planChunks(listOf(CardContent("a", "b")))

                coordinator.saveChunks(DECK_ID, chunks).failure shouldBe ChunkFailure.InvalidResponse
                delay(10_000)
                val retried = coordinator.saveChunks(DECK_ID, chunks)

                retried.failure shouldBe ChunkFailure.ProcessingTimeout
                (currentTime <= 25_000L) shouldBe true
            }
        }

        test("서버가 만든 장수가 보낸 장수와 다르면 다시 시도 없이 멈춘다") {
            runTest {
                val cards = ScriptedCardRepository().apply { createdCount = { sent -> sent - 1 } }
                val coordinator = coordinator(cards)

                val outcome = coordinator.saveChunks(DECK_ID, coordinator.planChunks(List(3) { CardContent("a$it", "b") }))

                outcome.failure shouldBe ChunkFailure.ResponseCountMismatch(created = 2)
                outcome.failure.userRetry shouldBe UserRetry.NONE
                cards.calls.size shouldBe 1
            }
        }

        test("장수가 다르게 저장된 묶음도 서버가 만든 장수를 저장 장수에 더한다") {
            runTest {
                val cards = ScriptedCardRepository().apply { createdCount = { sent -> if (sent == 3) 2 else sent } }
                val coordinator = coordinator(cards)

                val outcome = coordinator.saveChunks(DECK_ID, coordinator.planChunks(List(1_003) { CardContent("a$it", "b") }))

                outcome shouldBe
                    ImportSaveOutcome.Stopped(chunkIndex = 1, savedCards = 1_002, failure = ChunkFailure.ResponseCountMismatch(created = 2))
            }
        }

        test("키별 첫 전송 시각은 성공하거나 다시 보낼 수 없을 때 버리고 같은 키 재시도를 기다릴 때만 남긴다") {
            runTest {
                val cards = ScriptedCardRepository(CaroInvalidResponseException(debugMessage = "html"))
                val coordinator = coordinator(cards)
                val chunks = coordinator.planChunks(listOf(CardContent("a", "b")))

                coordinator.saveChunks(DECK_ID, chunks)
                coordinator.trackedKeyCount() shouldBe 1
                coordinator.saveChunks(DECK_ID, chunks)
                coordinator.trackedKeyCount() shouldBe 0
            }
        }

        test("한 조정 객체의 저장 호출이 겹쳐도 묶음을 섞지 않고 차례로 보낸다") {
            runTest {
                val cards = ScriptedCardRepository().apply { callDelayMillis = 100 }
                val coordinator = coordinator(cards)
                val first = coordinator.planChunks(List(2_000) { CardContent("A$it", "b") })
                val second = coordinator.planChunks(List(2_000) { CardContent("B$it", "b") })

                val jobs = listOf(first, second).map { chunks -> launch { coordinator.saveChunks(DECK_ID, chunks) } }
                jobs.forEach { it.join() }

                cards.calls.map { it.cards.first().front } shouldBe listOf("A0", "A1000", "B0", "B1000")
            }
        }

        test("인증 만료는 화면 오류로 바꾸지 않고 그대로 전파한다") {
            runTest {
                val coordinator = coordinator(ScriptedCardRepository(CaroAuthException.TokenExpired(debugMessage = "expired")))

                shouldThrow<CaroAuthException.TokenExpired> {
                    coordinator.saveChunks(DECK_ID, coordinator.planChunks(listOf(CardContent("a", "b"))))
                }
            }
        }

        test("덱 생성은 1.0 요청을 한 번만 보내고 실패해도 자동 재시도하지 않는다") {
            runTest {
                val deckRepository = mock<DeckRepository> { everySuspend { createDeck(any(), any()) } throws networkTimeout() }
                val coordinator = ImportSaveCoordinator(deckRepository, ScriptedCardRepository())

                val result = coordinator.createDeck("TOEIC", "설명")

                result shouldBe DeckCreationResult.Failed(networkTimeout())
                verifySuspend(exactly(1)) { deckRepository.createDeck("TOEIC", "설명") }
            }
        }

        test("덱 생성의 서버 4xx 오류는 덱 정보에 머무는 거절로 구분한다") {
            runTest {
                val deckRepository = mock<DeckRepository> { everySuspend { createDeck(any(), any()) } throws serverError("C001") }
                val coordinator = ImportSaveCoordinator(deckRepository, ScriptedCardRepository())

                coordinator.createDeck("TOEIC", "설명") shouldBe DeckCreationResult.Rejected("C001")
            }
        }

        test("덱 생성이 성공하면 만든 덱을 돌려준다") {
            runTest {
                val deck = Deck(DECK_ID, "TOEIC", "설명", 0, 0, 0, DeckState.entries.first())
                val deckRepository = mock<DeckRepository> { everySuspend { createDeck(any(), any()) } returns deck }
                val coordinator = ImportSaveCoordinator(deckRepository, ScriptedCardRepository())

                coordinator.createDeck("TOEIC", "설명") shouldBe DeckCreationResult.Created(deck)
            }
        }
    }

    private val ImportSaveOutcome.failure: ChunkFailure get() = (this as ImportSaveOutcome.Stopped).failure

    private fun TestScope.coordinator(
        cards: CardRepository,
        scope: TestScope = this,
    ) = ImportSaveCoordinator(
        deckRepository = mock(),
        cardRepository = cards,
        timeSource = scope.testScheduler.timeSource,
    )

    private companion object {
        const val DECK_ID = 7L
        const val C999 = "C999"
        val SUCCESS: Throwable? = null

        fun networkTimeout() = NetworkException.Timeout(debugMessage = "timeout")

        fun processing() = serverError("C010")

        fun serverError(code: String) = CaroServerException(code = code, message = code, debugMessage = code)
    }
}
