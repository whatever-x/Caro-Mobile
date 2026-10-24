package com.whatever.caro.feature.importer

import com.whatever.caro.core.model.exception.CaroInvalidResponseException
import com.whatever.caro.core.model.exception.CaroServerException
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckSideEffect
import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.mvi.SavingStatus
import com.whatever.caro.feature.importer.support.ImportViewModelRig
import com.whatever.caro.feature.importer.support.SaveScenario
import com.whatever.caro.feature.importer.support.ScriptedCardRepository
import com.whatever.caro.feature.importer.support.saveScenario
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/** 저장 중 멈춤: 멈춤 확인, 멈추는 중 입력 무시, 진행 중 묶음의 결과에 따른 이동, 멈추는 중 실패 처리를 확인한다. */
class ImportDeckViewModelStopTest : FunSpec() {
    private val rig = ImportViewModelRig()

    private class StopFailureCase(
        val label: String,
        val savedByFailedChunk: Int,
        val newCards: () -> ScriptedCardRepository,
    )

    init {
        rig.installOn(this)

        context("멈춤 확인") {
            listOf("[가져오기 멈추기]" to ImportDeckIntent.ClickStop, "뒤로 가기" to ImportDeckIntent.ClickBack).forEach { (label, intent) ->
                test("저장 중 $label 입력은 멈춤 확인 대화상자를 띄운다") {
                    runTest(rig.dispatcher) {
                        val scenario = savingInFlight()

                        scenario.send(intent)
                        runCurrent()

                        scenario.state.dialog shouldBe ImportDialog.StopConfirm
                    }
                }
            }

            test("[계속 가져오기]는 멈춤 확인을 닫고 저장을 이어서 완료한다") {
                runTest(rig.dispatcher) {
                    val scenario = savingInFlight()
                    scenario.send(ImportDeckIntent.ClickStop)
                    runCurrent()

                    scenario.send(ImportDeckIntent.DismissDialog)
                    advanceUntilIdle()

                    scenario.state.dialog shouldBe null
                    scenario.cards.calls.size shouldBe 3
                    scenario.effects shouldBe listOf(scenario.navigation(saved = 2_500, completed = true))
                }
            }

            test("멈추기를 확인하면 대화상자를 닫고 멈추는 중으로 바뀐다") {
                runTest(rig.dispatcher) {
                    val scenario = savingInFlight()

                    stop(scenario)

                    scenario.state.dialog shouldBe null
                    scenario.state.saving!!.status shouldBe SavingStatus.Stopping
                }
            }

            test("[계속 가져오기]로 닫은 뒤 늦게 온 ConfirmStop은 무시한다") {
                runTest(rig.dispatcher) {
                    val scenario = savingInFlight()
                    scenario.send(ImportDeckIntent.ClickStop)
                    runCurrent()
                    scenario.state.dialog shouldBe ImportDialog.StopConfirm
                    scenario.send(ImportDeckIntent.DismissDialog)
                    runCurrent()

                    scenario.send(ImportDeckIntent.ConfirmStop)
                    runCurrent()

                    scenario.state.saving!!.status shouldBe SavingStatus.Saving
                }
            }
        }

        context("멈추는 중") {
            test("뒤로 가기와 멈추기를 다시 눌러도 무시한다") {
                runTest(rig.dispatcher) {
                    val scenario = savingInFlight()
                    stop(scenario)
                    val before = scenario.state
                    before.saving!!.status shouldBe SavingStatus.Stopping

                    scenario.send(ImportDeckIntent.ClickBack)
                    scenario.send(ImportDeckIntent.ClickStop)
                    runCurrent()

                    scenario.state shouldBe before
                }
            }

            test("서버 처리를 기다리는 진행이 와도 멈추는 중으로 보인다") {
                runTest(rig.dispatcher) {
                    val scenario = saveScenario(rig, ScriptedCardRepository(*Array(30) { processing() }))
                    scenario.send(ImportDeckIntent.ClickCreateDeck)
                    runCurrent()
                    stop(scenario)

                    advanceTimeBy(3_000)

                    scenario.state.saving!!.status shouldBe SavingStatus.Stopping
                }
            }

            test("진행 중이던 묶음이 끝나면 다음 묶음을 보내지 않고 저장한 장수까지만 남은 덱으로 이동한다") {
                runTest(rig.dispatcher) {
                    val scenario = savingInFlight()
                    stop(scenario)

                    advanceUntilIdle()

                    scenario.cards.calls.size shouldBe 1
                    scenario.effects shouldBe listOf(scenario.navigation(saved = 1_000, completed = false))
                    scenario.state.dialog shouldBe null
                }
            }

            test("진행 중이던 묶음이 마지막 묶음이고 성공하면 완료로 처리한다") {
                runTest(rig.dispatcher) {
                    val scenario = savingInFlight(total = 3)
                    stop(scenario)

                    advanceUntilIdle()

                    scenario.effects shouldBe listOf(scenario.navigation(saved = 3, completed = true))
                }
            }
        }

        context("멈추는 중 실패") {
            listOf(
                StopFailureCase("거절", savedByFailedChunk = 0) { delayedFailure(serverError("C001")) },
                StopFailureCase("응답 해석 실패", savedByFailedChunk = 0) { delayedFailure(CaroInvalidResponseException(debugMessage = "html")) },
                StopFailureCase("장수 불일치", savedByFailedChunk = 999) { delayedShortResponse() },
            ).forEach { case ->
                test("멈추는 중 ${case.label}이면 대화상자 없이 저장한 장수까지만 남은 덱으로 이동한다") {
                    runTest(rig.dispatcher) {
                        val scenario = saveScenario(rig, case.newCards(), total = 1_500)
                        scenario.send(ImportDeckIntent.ClickCreateDeck)
                        runCurrent()
                        stop(scenario)

                        advanceUntilIdle()

                        scenario.effects shouldBe listOf(scenario.navigation(saved = case.savedByFailedChunk, completed = false))
                        scenario.state.dialog shouldBe null
                    }
                }
            }

            test("서버 처리 대기가 첫 전송부터 가상 시간 25초를 넘기면 대화상자 없이 저장한 장수까지만 남은 덱으로 이동한다") {
                runTest(rig.dispatcher) {
                    val scenario = saveScenario(rig, ScriptedCardRepository(*Array(30) { processing() }), total = 1_500)
                    val start = currentTime
                    scenario.send(ImportDeckIntent.ClickCreateDeck)
                    runCurrent()
                    stop(scenario)

                    advanceUntilIdle()

                    (currentTime - start) shouldBe 25_000L
                    scenario.effects shouldBe listOf(scenario.navigation(saved = 0, completed = false))
                    scenario.state.dialog shouldBe null
                }
            }

            test("덱을 찾을 수 없으면 이동하지 않고 덱을 찾을 수 없음 대화상자를 띄운다") {
                runTest(rig.dispatcher) {
                    val scenario = saveScenario(rig, delayedFailure(serverError("D001")), total = 1_500)
                    scenario.send(ImportDeckIntent.ClickCreateDeck)
                    runCurrent()
                    stop(scenario)

                    advanceUntilIdle()

                    scenario.state.dialog shouldBe ImportDialog.DeckUnavailable
                    scenario.effects.shouldBeEmpty()
                }
            }

            test("멈추는 중 받은 덱을 찾을 수 없음 대화상자의 [홈으로]는 홈으로 이동한다") {
                runTest(rig.dispatcher) {
                    val scenario = saveScenario(rig, delayedFailure(serverError("D001")), total = 1_500)
                    scenario.send(ImportDeckIntent.ClickCreateDeck)
                    runCurrent()
                    stop(scenario)
                    advanceUntilIdle()

                    scenario.send(ImportDeckIntent.ClickGoHome)
                    advanceUntilIdle()

                    scenario.effects shouldBe listOf(ImportDeckSideEffect.NavigateHome)
                }
            }
        }
    }

    /** 2,500장의 첫 묶음이 응답을 기다리는 중이다. */
    private fun TestScope.savingInFlight(total: Int = 2_500): SaveScenario {
        val scenario = saveScenario(rig, ScriptedCardRepository().apply { callDelayMillis = 100 }, total = total)
        scenario.send(ImportDeckIntent.ClickCreateDeck)
        runCurrent()
        scenario.cards.calls.size shouldBe 1
        return scenario
    }

    private fun TestScope.stop(scenario: SaveScenario) {
        scenario.send(ImportDeckIntent.ClickStop)
        runCurrent()
        scenario.state.dialog shouldBe ImportDialog.StopConfirm
        scenario.send(ImportDeckIntent.ConfirmStop)
        runCurrent()
    }

    private companion object {
        fun processing() = serverError("C010")

        fun serverError(code: String) = CaroServerException(code = code, message = code, debugMessage = code)

        /** 응답이 늦게 와서 멈추기를 확인할 틈이 생기는 실패 하나. */
        fun delayedFailure(failure: Throwable): ScriptedCardRepository = ScriptedCardRepository(failure).apply { callDelayMillis = 100 }

        /** 응답이 늦게 오고 보낸 장수보다 한 장 적게 만든다. */
        fun delayedShortResponse(): ScriptedCardRepository =
            ScriptedCardRepository().apply {
                callDelayMillis = 100
                createdCount = { sent -> sent - 1 }
            }
    }
}
