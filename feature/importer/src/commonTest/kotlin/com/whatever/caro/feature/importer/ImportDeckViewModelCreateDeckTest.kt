package com.whatever.caro.feature.importer

import app.cash.turbine.test
import com.whatever.caro.core.data.repository.deck.DeckRepository
import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.model.deck.DeckState
import com.whatever.caro.core.model.exception.CaroServerException
import com.whatever.caro.core.model.exception.NetworkException
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckSideEffect
import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.mvi.SavingProgress
import com.whatever.caro.feature.importer.support.FakeImportSourceReader
import com.whatever.caro.feature.importer.support.ImportTestTables
import com.whatever.caro.feature.importer.support.ImportViewModelHarness
import com.whatever.caro.feature.importer.support.ImportViewModelRig
import com.whatever.caro.feature.importer.support.openDeckInfo
import com.whatever.caro.feature.importer.support.openMappingFromFile
import com.whatever.caro.feature.importer.support.openPreview
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode.Companion.exactly
import dev.mokkery.verifySuspend
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

/** 덱 생성: 한 번만 부르기, 진행 중 입력 차단, 실패와 거절 처리, 자동 재시도 없음을 확인한다. */
class ImportDeckViewModelCreateDeckTest : FunSpec() {
    private val rig = ImportViewModelRig()

    init {
        rig.installOn(this)

        context("덱 만드는 중") {
            test("ClickCreateDeck은 CreatingDeck 단계로 가서 입력한 이름과 설명 그대로 createDeck을 한 번 부른다") {
                runTest(rig.dispatcher) {
                    val repository =
                        mock<DeckRepository> {
                            everySuspend { createDeck(any(), any()) } calls
                                { CompletableDeferred<Deck>().await() }
                        }
                    val harness = deckInfo(repository)
                    harness.send(ImportDeckIntent.UpdateDeckName("TOEIC 3000"))
                    harness.send(ImportDeckIntent.UpdateDeckDescription("영단어 모음"))

                    harness.send(ImportDeckIntent.ClickCreateDeck)
                    advanceUntilIdle()

                    harness.state.step shouldBe ImportStep.CreatingDeck
                    harness.state.isBusy shouldBe true
                    verifySuspend(exactly(1)) { repository.createDeck("TOEIC 3000", "영단어 모음") }
                }
            }

            test("ClickCreateDeck을 연달아 눌러도 createDeck은 한 번만 부른다") {
                runTest(rig.dispatcher) {
                    val repository =
                        mock<DeckRepository> {
                            everySuspend { createDeck(any(), any()) } calls
                                { CompletableDeferred<Deck>().await() }
                        }
                    val harness = deckInfo(repository)

                    harness.send(ImportDeckIntent.ClickCreateDeck)
                    harness.send(ImportDeckIntent.ClickCreateDeck)
                    advanceUntilIdle()

                    verifySuspend(exactly(1)) { repository.createDeck(any(), any()) }
                }
            }

            test("CreatingDeck 중 뒤로 가기와 다른 입력은 무시한다") {
                runTest(rig.dispatcher) {
                    val repository =
                        mock<DeckRepository> {
                            everySuspend { createDeck(any(), any()) } calls
                                { CompletableDeferred<Deck>().await() }
                        }
                    val harness = deckInfo(repository)
                    harness.send(ImportDeckIntent.ClickCreateDeck)
                    advanceUntilIdle()
                    val stateBefore = harness.state

                    harness.viewModel.sideEffect.test {
                        harness.send(ImportDeckIntent.ClickBack)
                        harness.send(ImportDeckIntent.UpdateDeckName("바꾼 이름"))
                        harness.send(ImportDeckIntent.RetryCreateDeck)
                        advanceUntilIdle()
                        expectNoEvents()
                    }

                    harness.state shouldBe stateBefore
                }
            }

            test("덱을 만들면 Saving 단계로 가서 저장할 총 장수를 보인다") {
                runTest(rig.dispatcher) {
                    val repository = mock<DeckRepository> { everySuspend { createDeck(any(), any()) } returns DECK }
                    val harness = deckInfo(repository)

                    harness.send(ImportDeckIntent.ClickCreateDeck)
                    advanceUntilIdle()

                    harness.state.step shouldBe ImportStep.Saving
                    harness.state.saving shouldBe SavingProgress(deckName = "words", totalCards = 3)
                }
            }
        }

        context("공백 입력") {
            test("이름이 공백이면 ClickCreateDeck을 무시한다") {
                runTest(rig.dispatcher) {
                    val repository = mock<DeckRepository>()
                    val harness = deckInfo(repository)
                    harness.send(ImportDeckIntent.UpdateDeckName("   "))

                    harness.send(ImportDeckIntent.ClickCreateDeck)
                    advanceUntilIdle()

                    harness.state.step shouldBe ImportStep.DeckInfo
                    verifySuspend(exactly(0)) { repository.createDeck(any(), any()) }
                }
            }
        }

        context("덱 생성 실패") {
            test("Failed는 DeckCreateFailed 대화상자를 띄우고 DeckInfo에서 입력을 유지한다") {
                runTest(rig.dispatcher) {
                    val repository = mock<DeckRepository> { everySuspend { createDeck(any(), any()) } throws networkTimeout() }
                    val harness = deckInfo(repository)

                    harness.send(ImportDeckIntent.ClickCreateDeck)
                    advanceUntilIdle()

                    harness.state.dialog shouldBe ImportDialog.DeckCreateFailed
                    harness.state.step shouldBe ImportStep.DeckInfo
                    harness.state.isBusy shouldBe false
                    harness.state.deckName shouldBe "words"
                }
            }

            test("Failed 뒤 시간이 지나도 createDeck을 다시 부르지 않는다") {
                runTest(rig.dispatcher) {
                    val repository = mock<DeckRepository> { everySuspend { createDeck(any(), any()) } throws networkTimeout() }
                    val harness = deckInfo(repository)

                    harness.send(ImportDeckIntent.ClickCreateDeck)
                    advanceUntilIdle()
                    advanceTimeBy(60_000)
                    advanceUntilIdle()

                    verifySuspend(exactly(1)) { repository.createDeck(any(), any()) }
                }
            }

            test("[다시 시도]는 createDeck을 정확히 한 번 더 부른다") {
                runTest(rig.dispatcher) {
                    val repository = mock<DeckRepository> { everySuspend { createDeck(any(), any()) } throws networkTimeout() }
                    val harness = deckInfo(repository)
                    harness.send(ImportDeckIntent.ClickCreateDeck)
                    advanceUntilIdle()

                    harness.send(ImportDeckIntent.RetryCreateDeck)
                    advanceUntilIdle()
                    advanceTimeBy(60_000)
                    advanceUntilIdle()

                    verifySuspend(exactly(2)) { repository.createDeck("words", "words.txt에서 가져왔어요") }
                }
            }

            test("[다시 시도]하는 동안 대화상자를 닫고 CreatingDeck으로 돌아간다") {
                runTest(rig.dispatcher) {
                    var attempts = 0
                    val repository =
                        mock<DeckRepository> {
                            everySuspend { createDeck(any(), any()) } calls {
                                attempts += 1
                                if (attempts == 1) throw networkTimeout() else CompletableDeferred<Deck>().await()
                            }
                        }
                    val harness = deckInfo(repository)
                    harness.send(ImportDeckIntent.ClickCreateDeck)
                    advanceUntilIdle()

                    harness.send(ImportDeckIntent.RetryCreateDeck)
                    advanceUntilIdle()

                    harness.state.dialog shouldBe null
                    harness.state.step shouldBe ImportStep.CreatingDeck
                }
            }

            test("[취소]는 대화상자를 닫고 입력을 유지한 채 DeckInfo에 머문다") {
                runTest(rig.dispatcher) {
                    val repository = mock<DeckRepository> { everySuspend { createDeck(any(), any()) } throws networkTimeout() }
                    val harness = deckInfo(repository)
                    harness.send(ImportDeckIntent.ClickCreateDeck)
                    advanceUntilIdle()

                    harness.send(ImportDeckIntent.DismissDialog)
                    advanceUntilIdle()

                    harness.state.dialog shouldBe null
                    harness.state.step shouldBe ImportStep.DeckInfo
                    harness.state.deckName shouldBe "words"
                    verifySuspend(exactly(1)) { repository.createDeck(any(), any()) }
                }
            }

            test("실패 대화상자가 없을 때 RetryCreateDeck은 무시한다") {
                runTest(rig.dispatcher) {
                    val repository = mock<DeckRepository>()
                    val harness = deckInfo(repository)

                    harness.send(ImportDeckIntent.RetryCreateDeck)
                    advanceUntilIdle()

                    harness.state.step shouldBe ImportStep.DeckInfo
                    verifySuspend(exactly(0)) { repository.createDeck(any(), any()) }
                }
            }
        }

        context("서버 거절") {
            listOf("C001", "C002").forEach { code ->
                test("$code 거절은 입력 오류 side effect를 보내고 DeckInfo에 머문다") {
                    runTest(rig.dispatcher) {
                        val repository = mock<DeckRepository> { everySuspend { createDeck(any(), any()) } throws rejected(code) }
                        val harness = deckInfo(repository)

                        harness.viewModel.sideEffect.test {
                            harness.send(ImportDeckIntent.ClickCreateDeck)
                            advanceUntilIdle()
                            awaitItem() shouldBe ImportDeckSideEffect.ShowDeckInputError
                        }

                        harness.state.step shouldBe ImportStep.DeckInfo
                        harness.state.dialog shouldBe null
                    }
                }
            }

            test("입력 오류가 아닌 거절은 일반 오류 side effect를 보내고 대화상자는 띄우지 않는다") {
                runTest(rig.dispatcher) {
                    val repository = mock<DeckRepository> { everySuspend { createDeck(any(), any()) } throws rejected("A002") }
                    val harness = deckInfo(repository)

                    harness.viewModel.sideEffect.test {
                        harness.send(ImportDeckIntent.ClickCreateDeck)
                        advanceUntilIdle()
                        awaitItem() shouldBe ImportDeckSideEffect.ShowDeckCreateError
                    }

                    harness.state.step shouldBe ImportStep.DeckInfo
                    harness.state.dialog shouldBe null
                }
            }

            test("거절 뒤에는 입력을 유지하고 고쳐서 다시 [가져오기]를 누를 수 있다") {
                runTest(rig.dispatcher) {
                    val repository = mock<DeckRepository> { everySuspend { createDeck(any(), any()) } throws rejected("C001") }
                    val harness = deckInfo(repository)
                    harness.send(ImportDeckIntent.ClickCreateDeck)
                    advanceUntilIdle()
                    harness.state.deckName shouldBe "words"
                    harness.state.isBusy shouldBe false
                    harness.send(ImportDeckIntent.UpdateDeckName("고친 이름"))

                    harness.send(ImportDeckIntent.ClickCreateDeck)
                    advanceUntilIdle()

                    verifySuspend(exactly(1)) { repository.createDeck("고친 이름", "words.txt에서 가져왔어요") }
                }
            }
        }
    }

    private fun TestScope.deckInfo(repository: DeckRepository): ImportViewModelHarness {
        val harness = rig.create(reader = FakeImportSourceReader().returns(ImportTestTables.text()), deckRepository = repository)
        openMappingFromFile(harness, "words.txt")
        openPreview(harness)
        openDeckInfo(harness)
        harness.state.step shouldBe ImportStep.DeckInfo
        return harness
    }

    private companion object {
        val DECK = Deck(7L, "words", "words.txt에서 가져왔어요", 0, 0, 0, DeckState.entries.first())

        fun networkTimeout() = NetworkException.Timeout(debugMessage = "timeout")

        fun rejected(code: String) = CaroServerException(code = code, message = code, debugMessage = code)
    }
}
