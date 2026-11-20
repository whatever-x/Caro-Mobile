package com.whatever.caro.feature.importer

import com.whatever.caro.core.model.exception.CaroInvalidResponseException
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.mvi.PauseReason
import com.whatever.caro.feature.importer.mvi.SavingStatus
import com.whatever.caro.feature.importer.save.ImportSaveLimits
import com.whatever.caro.feature.importer.support.ImportViewModelRig
import com.whatever.caro.feature.importer.support.SaveScenario
import com.whatever.caro.feature.importer.support.ScriptedCardRepository
import com.whatever.caro.feature.importer.support.networkTimeout
import com.whatever.caro.feature.importer.support.runSaveToEnd
import com.whatever.caro.feature.importer.support.saveScenario
import com.whatever.caro.feature.importer.support.serverError
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/** 저장 결과를 확인하지 못한 멈춤의 이동. ScriptedCardRepository는 호출을 기록한 뒤 던지므로 서버는 받았지만 응답이 유실된 상황과 같다. */
class ImportDeckViewModelUnconfirmedSaveTest : FunSpec() {
    private val rig = ImportViewModelRig()

    init {
        rig.installOn(this)

        context("저장 결과를 확인하지 못한 멈춤") {
            test("첫 묶음의 응답을 해석하지 못하면 [멈추고 덱 보기]는 0장과 결과 미확인을 담아 이동한다") {
                runTest(rig.dispatcher) {
                    val scenario = saveScenario(rig, ScriptedCardRepository(CaroInvalidResponseException(debugMessage = "html")))
                    runSaveToEnd(scenario)
                    scenario.state.dialog shouldBe ImportDialog.Paused(saved = 0, reason = PauseReason.Later)

                    scenario.send(ImportDeckIntent.ClickViewDeck)
                    advanceUntilIdle()

                    scenario.effects shouldBe listOf(scenario.navigation(saved = 0, completed = false, isSaveUnconfirmed = true))
                }
            }

            test("재시도 간격을 기다리는 중에 멈추기를 확인하면 대화상자 없이 결과 미확인으로 이동한다") {
                runTest(rig.dispatcher) {
                    val scenario = saveScenario(rig, ScriptedCardRepository(SUCCESS, networkTimeout()), total = 1_500)
                    scenario.send(ImportDeckIntent.ClickImport)
                    runCurrent()
                    scenario.state.saving!!.status shouldBe SavingStatus.Retrying

                    stop(scenario)
                    advanceUntilIdle()

                    scenario.cards.calls.size shouldBe 2
                    scenario.state.dialog shouldBe null
                    scenario.effects shouldBe listOf(scenario.navigation(saved = 1_000, completed = false, isSaveUnconfirmed = true))
                }
            }

            test("첫 묶음이 키 충돌이면 결과 미확인 키 충돌 대화상자를 띄우고 [덱 보기]도 결과 미확인으로 이동한다") {
                runTest(rig.dispatcher) {
                    val scenario = saveScenario(rig, ScriptedCardRepository(serverError("C009")))
                    runSaveToEnd(scenario)
                    scenario.state.dialog shouldBe ImportDialog.KeyConflict(saved = 0, isSaveUnconfirmed = true)

                    scenario.send(ImportDeckIntent.ClickViewDeck)
                    advanceUntilIdle()

                    scenario.effects shouldBe listOf(scenario.navigation(saved = 0, completed = false, isSaveUnconfirmed = true))
                }
            }
        }

        context("일시 정지 뒤 다시 시도") {
            test("같은 키로 다시 보내도 응답을 받지 못하면 [멈추고 덱 보기]는 결과 미확인으로 이동한다") {
                runTest(rig.dispatcher) {
                    val scenario =
                        saveScenario(rig, ScriptedCardRepository(SUCCESS, *exhaustedNetwork(), *exhaustedNetwork()), total = 1_500)
                    runSaveToEnd(scenario)
                    scenario.send(ImportDeckIntent.RetrySave)
                    advanceUntilIdle()
                    scenario.state.dialog shouldBe ImportDialog.Paused(saved = 1_000, reason = PauseReason.Network)

                    scenario.send(ImportDeckIntent.ClickViewDeck)
                    advanceUntilIdle()

                    scenario.effects shouldBe listOf(scenario.navigation(saved = 1_000, completed = false, isSaveUnconfirmed = true))
                }
            }

            test("거절 뒤 새 키로 다시 보낸 묶음이 응답을 받지 못하면 [멈추고 덱 보기]는 결과 미확인으로 이동한다") {
                runTest(rig.dispatcher) {
                    val scenario = saveScenario(rig, ScriptedCardRepository(serverError("C001"), *exhaustedNetwork()))
                    runSaveToEnd(scenario)
                    scenario.send(ImportDeckIntent.RetrySave)
                    advanceUntilIdle()
                    scenario.state.dialog shouldBe ImportDialog.Paused(saved = 0, reason = PauseReason.Network)

                    scenario.send(ImportDeckIntent.ClickViewDeck)
                    advanceUntilIdle()

                    scenario.effects shouldBe listOf(scenario.navigation(saved = 0, completed = false, isSaveUnconfirmed = true))
                }
            }
        }
    }

    private fun TestScope.stop(scenario: SaveScenario) {
        scenario.send(ImportDeckIntent.ClickStop)
        runCurrent()
        scenario.state.dialog shouldBe ImportDialog.StopConfirm
        scenario.send(ImportDeckIntent.ConfirmStop)
        runCurrent()
    }

    private companion object {
        val SUCCESS: Throwable? = null

        fun exhaustedNetwork() = Array(ImportSaveLimits.AUTO_RETRY_COUNT + 1) { networkTimeout() }
    }
}
