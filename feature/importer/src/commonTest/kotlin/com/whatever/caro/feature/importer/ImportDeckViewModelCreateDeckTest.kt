package com.whatever.caro.feature.importer

import app.cash.turbine.test
import com.whatever.caro.core.data.repository.deck.DeckRepository
import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.model.exception.CaroAuthException
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckSideEffect
import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.mvi.ImportLoading
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.mvi.SavingProgress
import com.whatever.caro.feature.importer.save.ImportSaveCoordinator
import com.whatever.caro.feature.importer.save.ImportSaveCoordinatorFactory
import com.whatever.caro.feature.importer.support.FakeImportSourceReader
import com.whatever.caro.feature.importer.support.ImportTestTables
import com.whatever.caro.feature.importer.support.ImportViewModelHarness
import com.whatever.caro.feature.importer.support.ImportViewModelRig
import com.whatever.caro.feature.importer.support.SAVE_SCENARIO_DECK
import com.whatever.caro.feature.importer.support.ScriptedCardRepository
import com.whatever.caro.feature.importer.support.networkTimeout
import com.whatever.caro.feature.importer.support.serverError
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
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/** 덱 만들기: 미리보기 위 덮개, 한 번만 부르기, 그동안 뒤로 가기 막기, 실패 대화상자, 입력 거절 스낵바를 확인한다. */
class ImportDeckViewModelCreateDeckTest : FunSpec() {
    private val rig = ImportViewModelRig()

    init {
        rig.installOn(this)

        context("덱 만드는 중") {
            test("[가져오기]를 연달아 눌러도 덱은 한 번만 만든다") {
                runTest(rig.dispatcher) {
                    val repository = pendingRepository()
                    val harness = preview(repository)

                    harness.send(ImportDeckIntent.ClickImport)
                    harness.send(ImportDeckIntent.ClickImport)
                    advanceUntilIdle()

                    verifySuspend(exactly(1)) { repository.createDeck(any(), any()) }
                }
            }

            test("덱을 만드는 중에는 뒤로 가기와 다른 입력을 무시한다") {
                runTest(rig.dispatcher) {
                    val repository = pendingRepository()
                    val harness = preview(repository)
                    harness.send(ImportDeckIntent.ClickImport)
                    advanceUntilIdle()
                    val before = harness.state

                    harness.viewModel.sideEffect.test {
                        harness.send(ImportDeckIntent.ClickBack)
                        harness.send(ImportDeckIntent.ClickChangeDeckInfo)
                        harness.send(ImportDeckIntent.RetryCreateDeck)
                        advanceUntilIdle()
                        expectNoEvents()
                    }

                    harness.state shouldBe before
                    verifySuspend(exactly(1)) { repository.createDeck(any(), any()) }
                }
            }

            test("덱을 만들면 저장 단계로 가서 덱 이름과 저장할 총 장수를 보인다") {
                runTest(rig.dispatcher) {
                    val cards = ScriptedCardRepository().apply { callDelayMillis = 100 }
                    val repository = mock<DeckRepository> { everySuspend { createDeck(any(), any()) } returns SAVE_SCENARIO_DECK }
                    val harness = preview(repository, cards)

                    harness.send(ImportDeckIntent.ClickImport)
                    runCurrent()

                    harness.state.step shouldBe ImportStep.Saving
                    harness.state.saving shouldBe SavingProgress(deckName = "words", totalCards = 3)
                }
            }
        }

        context("덱 만들기 실패") {
            test("실패하면 덮개를 걷고 미리보기에 머물며 덱 만들기 실패 대화상자를 띄운다") {
                runTest(rig.dispatcher) {
                    val harness = preview(failingRepository())

                    harness.send(ImportDeckIntent.ClickImport)
                    advanceUntilIdle()

                    harness.state.dialog shouldBe ImportDialog.DeckCreateFailed
                    harness.state.step shouldBe ImportStep.Preview
                    harness.state.loading shouldBe null
                    harness.state.deckName shouldBe "words"
                }
            }

            test("[다시 시도]는 대화상자를 닫고 덮개를 띄운 뒤 같은 이름과 설명으로 덱을 한 번 더 만든다") {
                runTest(rig.dispatcher) {
                    var attempts = 0
                    val repository =
                        mock<DeckRepository> {
                            everySuspend { createDeck(any(), any()) } calls {
                                attempts += 1
                                if (attempts == 1) throw networkTimeout() else CompletableDeferred<Deck>().await()
                            }
                        }
                    val harness = preview(repository)
                    harness.send(ImportDeckIntent.ClickImport)
                    advanceUntilIdle()

                    harness.send(ImportDeckIntent.RetryCreateDeck)
                    advanceUntilIdle()

                    harness.state.dialog shouldBe null
                    harness.state.loading shouldBe ImportLoading.CreatingDeck
                    verifySuspend(exactly(2)) { repository.createDeck("words", "words.txt에서 가져왔어요") }
                }
            }

            listOf("[취소]" to ImportDeckIntent.DismissDialog, "뒤로 가기" to ImportDeckIntent.ClickBack).forEach { (label, intent) ->
                test("$label 입력은 대화상자를 닫고 미리보기에 머문다") {
                    runTest(rig.dispatcher) {
                        val repository = failingRepository()
                        val harness = preview(repository)
                        harness.send(ImportDeckIntent.ClickImport)
                        advanceUntilIdle()

                        harness.send(intent)
                        advanceUntilIdle()

                        harness.state.dialog shouldBe null
                        harness.state.step shouldBe ImportStep.Preview
                        harness.state.preview
                            ?.cards
                            ?.size shouldBe 3
                        verifySuspend(exactly(1)) { repository.createDeck(any(), any()) }
                    }
                }
            }

            test("실패 대화상자가 없을 때 [다시 시도]는 무시한다") {
                runTest(rig.dispatcher) {
                    val repository = mock<DeckRepository>()
                    val harness = preview(repository)

                    harness.send(ImportDeckIntent.RetryCreateDeck)
                    advanceUntilIdle()

                    harness.state.loading shouldBe null
                    verifySuspend(exactly(0)) { repository.createDeck(any(), any()) }
                }
            }

            test("입력 값이 아닌 이유로 거절되면 덱 만들기 실패 대화상자를 띄운다") {
                runTest(rig.dispatcher) {
                    val harness = preview(mock<DeckRepository> { everySuspend { createDeck(any(), any()) } throws serverError("A002") })

                    harness.send(ImportDeckIntent.ClickImport)
                    advanceUntilIdle()

                    harness.state.dialog shouldBe ImportDialog.DeckCreateFailed
                    harness.state.loading shouldBe null
                }
            }

            test("인증이 만료되면 덮개를 걷고 대화상자 없이 예외를 앱 전역 처리로 넘긴다") {
                runTest(rig.dispatcher) {
                    val unhandled = mutableListOf<Throwable>()
                    val filter = ExceptionFilter { throwable -> unhandled.add(throwable) }
                    val expired = CaroAuthException.TokenExpired(debugMessage = "expired")
                    val harness =
                        preview(mock<DeckRepository> { everySuspend { createDeck(any(), any()) } throws expired }, filter = filter)

                    harness.send(ImportDeckIntent.ClickImport)
                    advanceUntilIdle()

                    harness.state.loading shouldBe null
                    harness.state.dialog shouldBe null
                    harness.state.step shouldBe ImportStep.Preview
                    unhandled.single().shouldBeInstanceOf<CaroAuthException.TokenExpired>()
                }
            }
        }

        context("입력 거절") {
            listOf("C001", "C002").forEach { code ->
                test("$code 거절이면 미리보기에 머물고 입력 오류 스낵바를 띄우며 덱 정보 시트는 열지 않는다") {
                    runTest(rig.dispatcher) {
                        val harness = preview(mock<DeckRepository> { everySuspend { createDeck(any(), any()) } throws serverError(code) })

                        harness.viewModel.sideEffect.test {
                            harness.send(ImportDeckIntent.ClickImport)
                            advanceUntilIdle()
                            awaitItem() shouldBe ImportDeckSideEffect.ShowDeckInputError
                        }

                        harness.state.step shouldBe ImportStep.Preview
                        harness.state.loading shouldBe null
                        harness.state.dialog shouldBe null
                        harness.state.deckInfoDraft shouldBe null
                    }
                }
            }

            test("거절 뒤 덱 정보를 고쳐 다시 [가져오기]를 누르면 고친 이름으로 덱을 만든다") {
                runTest(rig.dispatcher) {
                    val repository = mock<DeckRepository> { everySuspend { createDeck(any(), any()) } throws serverError("C001") }
                    val harness = preview(repository)
                    harness.send(ImportDeckIntent.ClickImport)
                    advanceUntilIdle()

                    harness.send(ImportDeckIntent.ClickChangeDeckInfo)
                    harness.send(ImportDeckIntent.UpdateDeckInfoName("고친 이름"))
                    harness.send(ImportDeckIntent.ApplyDeckInfo)
                    harness.send(ImportDeckIntent.ClickImport)
                    advanceUntilIdle()

                    verifySuspend(exactly(1)) { repository.createDeck("고친 이름", "words.txt에서 가져왔어요") }
                }
            }

            test("미리 채운 이름이 공백뿐이면 덱을 만들지 않고 입력 오류 스낵바를 띄운다") {
                runTest(rig.dispatcher) {
                    val repository = mock<DeckRepository>()
                    val harness = preview(repository, fileName = "   .txt")
                    harness.state.deckName shouldBe "   "

                    harness.viewModel.sideEffect.test {
                        harness.send(ImportDeckIntent.ClickImport)
                        advanceUntilIdle()
                        awaitItem() shouldBe ImportDeckSideEffect.ShowDeckInputError
                    }

                    harness.state.loading shouldBe null
                    verifySuspend(exactly(0)) { repository.createDeck(any(), any()) }
                }
            }

            test("미리 채운 이름이 공백뿐이어도 덱 정보 시트에서 이름을 넣어 [적용]하면 [가져오기]가 그 이름으로 덱을 만든다") {
                runTest(rig.dispatcher) {
                    val repository = pendingRepository()
                    val harness = preview(repository, fileName = "   .txt")

                    harness.send(ImportDeckIntent.ClickChangeDeckInfo)
                    harness.send(ImportDeckIntent.UpdateDeckInfoName("고친 이름"))
                    harness.send(ImportDeckIntent.ApplyDeckInfo)
                    harness.send(ImportDeckIntent.ClickImport)
                    advanceUntilIdle()

                    harness.state.loading shouldBe ImportLoading.CreatingDeck
                    verifySuspend(exactly(1)) { repository.createDeck("고친 이름", any()) }
                }
            }

            test("덱 정보 시트가 열린 동안에는 [가져오기]를 무시한다") {
                runTest(rig.dispatcher) {
                    val repository = mock<DeckRepository>()
                    val harness = preview(repository)
                    harness.send(ImportDeckIntent.ClickChangeDeckInfo)

                    harness.send(ImportDeckIntent.ClickImport)
                    advanceUntilIdle()

                    harness.state.loading shouldBe null
                    verifySuspend(exactly(0)) { repository.createDeck(any(), any()) }
                }
            }
        }
    }

    private fun TestScope.preview(
        repository: DeckRepository,
        cards: ScriptedCardRepository = ScriptedCardRepository(),
        fileName: String = "words.txt",
        filter: ExceptionFilter = ExceptionFilter.None,
    ): ImportViewModelHarness {
        val harness =
            rig.create(
                reader = FakeImportSourceReader().returns(ImportTestTables.text(sourceName = fileName)),
                saveCoordinatorFactory = ImportSaveCoordinatorFactory { ImportSaveCoordinator(repository, cards) },
                exceptionFilter = filter,
            )
        harness.pickNamed(fileName)
        advanceUntilIdle()
        harness.state.step shouldBe ImportStep.Preview
        return harness
    }

    private fun pendingRepository(): DeckRepository =
        mock<DeckRepository> { everySuspend { createDeck(any(), any()) } calls { CompletableDeferred<Deck>().await() } }

    private fun failingRepository(): DeckRepository =
        mock<DeckRepository> {
            everySuspend { createDeck(any(), any()) } throws
                networkTimeout()
        }
}
