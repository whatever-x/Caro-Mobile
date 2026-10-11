package com.whatever.caro.feature.importer

import com.whatever.caro.core.model.exception.CaroServerException
import com.whatever.caro.core.model.exception.NetworkException
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckSideEffect
import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.mvi.PauseReason
import com.whatever.caro.feature.importer.mvi.SavingStatus
import com.whatever.caro.feature.importer.support.ImportViewModelRig
import com.whatever.caro.feature.importer.support.ScriptedCardRepository
import com.whatever.caro.feature.importer.support.runSaveToEnd
import com.whatever.caro.feature.importer.support.saveScenario
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/** 저장 선택 입력의 가드: 저장 중이 아닐 때, 같은 선택의 중복 입력, 멈춤 확인과 실패 대화상자가 겹칠 때를 확인한다. */
class ImportDeckViewModelSaveGuardTest : FunSpec() {
    private val rig = ImportViewModelRig()

    init {
        rig.installOn(this)

        context("저장 중이 아닐 때") {
            test("덱 정보 단계에서는 멈춤과 저장 선택 입력을 모두 무시한다") {
                runTest(rig.dispatcher) {
                    val scenario = saveScenario(rig, ScriptedCardRepository())
                    val before = scenario.state

                    listOf(
                        ImportDeckIntent.ClickStop,
                        ImportDeckIntent.ConfirmStop,
                        ImportDeckIntent.RetrySave,
                        ImportDeckIntent.ClickViewDeck,
                        ImportDeckIntent.ClickGoHome,
                    ).forEach(scenario::send)
                    advanceUntilIdle()

                    scenario.state shouldBe before
                    scenario.effects.shouldBeEmpty()
                    scenario.cards.calls.shouldBeEmpty()
                }
            }

            test("저장이 대화상자 없이 진행 중이면 [다시 시도], [덱 보기], [홈으로]를 무시한다") {
                runTest(rig.dispatcher) {
                    val scenario = saveScenario(rig, ScriptedCardRepository().apply { callDelayMillis = 100 }, total = 2_500)
                    scenario.send(ImportDeckIntent.ClickCreateDeck)
                    runCurrent()
                    val before = scenario.state

                    listOf(ImportDeckIntent.RetrySave, ImportDeckIntent.ClickViewDeck, ImportDeckIntent.ClickGoHome).forEach(scenario::send)
                    runCurrent()

                    scenario.state shouldBe before
                    scenario.effects.shouldBeEmpty()
                    scenario.cards.calls.size shouldBe 1
                }
            }
        }

        context("같은 선택의 중복 입력") {
            test("[멈추고 덱 보기]를 연달아 눌러도 한 번만 이동한다") {
                runTest(rig.dispatcher) {
                    val scenario = saveScenario(rig, ScriptedCardRepository(serverError("C001")))
                    runSaveToEnd(scenario)

                    scenario.send(ImportDeckIntent.ClickViewDeck)
                    scenario.send(ImportDeckIntent.ClickViewDeck)
                    advanceUntilIdle()

                    scenario.effects shouldBe listOf(scenario.navigation(saved = 0, completed = false))
                }
            }

            test("[홈으로]를 연달아 눌러도 한 번만 이동한다") {
                runTest(rig.dispatcher) {
                    val scenario = saveScenario(rig, ScriptedCardRepository(serverError("D001")))
                    runSaveToEnd(scenario)

                    scenario.send(ImportDeckIntent.ClickGoHome)
                    scenario.send(ImportDeckIntent.ClickGoHome)
                    advanceUntilIdle()

                    scenario.effects shouldBe listOf(ImportDeckSideEffect.NavigateHome)
                }
            }

            test("[다시 시도]를 연달아 눌러도 멈춘 묶음을 한 번만 다시 보낸다") {
                runTest(rig.dispatcher) {
                    val scenario = saveScenario(rig, ScriptedCardRepository(serverError("C001")))
                    runSaveToEnd(scenario)

                    scenario.send(ImportDeckIntent.RetrySave)
                    scenario.send(ImportDeckIntent.RetrySave)
                    advanceUntilIdle()

                    scenario.cards.calls.size shouldBe 2
                    scenario.effects shouldBe listOf(scenario.navigation(saved = 3, completed = true))
                }
            }
        }

        context("대화상자별 가능한 선택") {
            test("덱을 찾을 수 없음 대화상자에서는 저장된 덱이 없으므로 [덱 보기] 입력을 무시한다") {
                runTest(rig.dispatcher) {
                    val scenario = saveScenario(rig, ScriptedCardRepository(serverError("D001")))
                    runSaveToEnd(scenario)

                    scenario.send(ImportDeckIntent.ClickViewDeck)
                    scenario.send(ImportDeckIntent.RetrySave)
                    advanceUntilIdle()

                    scenario.state.dialog shouldBe ImportDialog.DeckUnavailable
                    scenario.effects.shouldBeEmpty()
                    scenario.cards.calls.size shouldBe 1
                }
            }

            test("키 충돌 대화상자에서는 [홈으로] 입력을 무시한다") {
                runTest(rig.dispatcher) {
                    val scenario = saveScenario(rig, ScriptedCardRepository(serverError("C009")))
                    runSaveToEnd(scenario)

                    scenario.send(ImportDeckIntent.ClickGoHome)
                    advanceUntilIdle()

                    scenario.state.dialog shouldBe ImportDialog.KeyConflict(saved = 0)
                    scenario.effects.shouldBeEmpty()
                }
            }

            test("실패 대화상자가 떠 있는 동안 뒤로 가기와 [가져오기 멈추기]는 멈춤 확인을 열지 않는다") {
                runTest(rig.dispatcher) {
                    val scenario =
                        saveScenario(rig, ScriptedCardRepository(networkTimeout(), networkTimeout(), networkTimeout(), networkTimeout()))
                    runSaveToEnd(scenario)
                    scenario.state.dialog shouldBe ImportDialog.Paused(saved = 0, reason = PauseReason.NETWORK)

                    scenario.send(ImportDeckIntent.ClickBack)
                    scenario.send(ImportDeckIntent.ClickStop)
                    advanceUntilIdle()

                    scenario.state.dialog shouldBe ImportDialog.Paused(saved = 0, reason = PauseReason.NETWORK)
                    scenario.state.saving!!.status shouldBe SavingStatus.Saving
                }
            }
        }

        context("멈춤 확인이 떠 있는 동안") {
            test("묶음이 실패하면 실패 대화상자가 멈춤 확인을 대신하고 늦은 ConfirmStop은 무시한다") {
                runTest(rig.dispatcher) {
                    val cards = ScriptedCardRepository(serverError("C001")).apply { callDelayMillis = 100 }
                    val scenario = saveScenario(rig, cards, total = 1_500)
                    scenario.send(ImportDeckIntent.ClickCreateDeck)
                    runCurrent()
                    scenario.send(ImportDeckIntent.ClickStop)
                    runCurrent()
                    scenario.state.dialog shouldBe ImportDialog.StopConfirm

                    advanceUntilIdle()
                    scenario.send(ImportDeckIntent.ConfirmStop)
                    runCurrent()

                    scenario.state.dialog shouldBe ImportDialog.PartialFailure(saved = 0)
                    scenario.state.saving!!.status shouldBe SavingStatus.Saving
                    scenario.effects.shouldBeEmpty()
                }
            }
        }
    }

    private companion object {
        fun networkTimeout() = NetworkException.Timeout(debugMessage = "timeout")

        fun serverError(code: String) = CaroServerException(code = code, message = code, debugMessage = code)
    }
}
