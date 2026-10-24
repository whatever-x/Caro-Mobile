package com.whatever.caro.feature.importer

import com.whatever.caro.core.model.exception.CaroAuthException
import com.whatever.caro.core.model.exception.CaroServerException
import com.whatever.caro.core.model.exception.NetworkException
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckSideEffect
import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.mvi.PauseReason
import com.whatever.caro.feature.importer.mvi.SavingStatus
import com.whatever.caro.feature.importer.save.ImportSaveCoordinator
import com.whatever.caro.feature.importer.save.ImportSaveCoordinatorFactory
import com.whatever.caro.feature.importer.support.ImportViewModelRig
import com.whatever.caro.feature.importer.support.SAVE_SCENARIO_DECK
import com.whatever.caro.feature.importer.support.SaveScenario
import com.whatever.caro.feature.importer.support.ScriptedCardRepository
import com.whatever.caro.feature.importer.support.runSaveToEnd
import com.whatever.caro.feature.importer.support.saveScenario
import com.whatever.caro.feature.importer.support.savedDeckRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldBeUnique
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/** 덱 생성 뒤 저장 흐름: 묶음 순서와 키, 자동 재시도와 서버 대기, 일시 정지와 다시 시도, 확정 실패의 이동, 닫을 수 없는 대화상자를 확인한다. */
class ImportDeckViewModelSaveTest : FunSpec() {
    private val rig = ImportViewModelRig()

    init {
        rig.installOn(this)

        context("묶음 저장") {
            test("미리보기 카드를 순서 그대로 1,000장씩 묶어 하나씩 보낸다") {
                runTest(rig.dispatcher) {
                    val scenario = saveScenario(rig, ScriptedCardRepository(), total = 2_500)
                    val previewCards = scenario.state.preview!!.cards

                    runSaveToEnd(scenario)

                    scenario.cards.calls.map { it.cards.size } shouldBe listOf(1_000, 1_000, 500)
                    scenario.cards.calls.flatMap { call -> call.cards.map { it.front } } shouldBe previewCards.map { it.front }
                    scenario.cards.calls.flatMap { call -> call.cards.map { it.back } } shouldBe previewCards.map { it.back }
                    scenario.cards.calls
                        .map { it.deckId }
                        .toSet() shouldBe setOf(SAVE_SCENARIO_DECK.id)
                }
            }

            test("묶음마다 서로 다른 키를 쓴다") {
                runTest(rig.dispatcher) {
                    val scenario = saveScenario(rig, ScriptedCardRepository(), total = 2_500)

                    runSaveToEnd(scenario)

                    scenario.cards.calls.size shouldBe 3
                    scenario.cards.calls
                        .map { it.key }
                        .shouldBeUnique()
                }
            }

            test("모든 묶음이 성공하면 전체 장수를 담아 완료로 덱 상세에 이동한다") {
                runTest(rig.dispatcher) {
                    val scenario = saveScenario(rig, ScriptedCardRepository(), total = 2_500)

                    runSaveToEnd(scenario)

                    scenario.effects shouldBe listOf(scenario.navigation(saved = 2_500, completed = true))
                    scenario.state.dialog shouldBe null
                }
            }

            test("첫 응답 전에는 저장한 장수가 0이라 진행률 없는 저장 중이다") {
                runTest(rig.dispatcher) {
                    val cards = ScriptedCardRepository().apply { callDelayMillis = 100 }
                    val scenario = saveScenario(rig, cards, total = 2_500)

                    scenario.send(ImportDeckIntent.ClickCreateDeck)
                    runCurrent()

                    scenario.cards.calls.size shouldBe 1
                    scenario.state.saving!!.isIndeterminate shouldBe true
                    scenario.state.saving!!.status shouldBe SavingStatus.Saving
                }
            }

            test("묶음이 성공할 때마다 저장한 장수가 쌓인다") {
                runTest(rig.dispatcher) {
                    val cards = ScriptedCardRepository().apply { callDelayMillis = 100 }
                    val scenario = saveScenario(rig, cards, total = 2_500)
                    scenario.send(ImportDeckIntent.ClickCreateDeck)

                    advanceTimeBy(150)
                    val afterFirst = scenario.state.saving!!.savedCards
                    advanceTimeBy(100)
                    val afterSecond = scenario.state.saving!!.savedCards

                    afterFirst shouldBe 1_000
                    afterSecond shouldBe 2_000
                }
            }
        }

        context("자동 재시도와 서버 대기") {
            test("연결 오류 뒤 간격을 기다리는 동안 다시 보내는 중으로 보인다") {
                runTest(rig.dispatcher) {
                    val scenario = saveScenario(rig, ScriptedCardRepository(networkTimeout(), SUCCESS))

                    scenario.send(ImportDeckIntent.ClickCreateDeck)
                    runCurrent()

                    scenario.state.saving!!.status shouldBe SavingStatus.Retrying
                }
            }

            test("자동 재시도는 같은 키와 같은 카드 목록을 다시 쓴다") {
                runTest(rig.dispatcher) {
                    val scenario = saveScenario(rig, ScriptedCardRepository(networkTimeout(), SUCCESS))

                    runSaveToEnd(scenario)

                    scenario.cards.calls.size shouldBe 2
                    scenario.cards.calls[1].key shouldBe scenario.cards.calls[0].key
                    scenario.cards.calls[1].cards shouldBeSameInstanceAs scenario.cards.calls[0].cards
                    scenario.effects shouldBe listOf(scenario.navigation(saved = 3, completed = true))
                }
            }

            test("C010을 받으면 서버 처리를 기다리는 중으로 보인다") {
                runTest(rig.dispatcher) {
                    val scenario = saveScenario(rig, ScriptedCardRepository(*Array(30) { processing() }))

                    scenario.send(ImportDeckIntent.ClickCreateDeck)
                    runCurrent()

                    scenario.state.saving!!.status shouldBe SavingStatus.WaitingForServer
                }
            }

            test("C010은 첫 전송부터 가상 시간 25초까지 기다린 뒤 잠시 뒤 다시 시도하라는 일시 정지로 멈춘다") {
                runTest(rig.dispatcher) {
                    val scenario = saveScenario(rig, ScriptedCardRepository(*Array(30) { processing() }))
                    val start = currentTime

                    runSaveToEnd(scenario)

                    scenario.state.dialog shouldBe ImportDialog.Paused(saved = 0, reason = PauseReason.LATER)
                    (currentTime - start) shouldBe 25_000L
                    scenario.effects.shouldBeEmpty()
                }
            }
        }

        context("일시 정지와 다시 시도") {
            test("자동 재시도를 다 쓰면 저장한 장수와 함께 네트워크 원인의 일시 정지로 멈춘다") {
                runTest(rig.dispatcher) {
                    val scenario = pausedByNetwork()

                    scenario.state.dialog shouldBe ImportDialog.Paused(saved = 1_000, reason = PauseReason.NETWORK)
                    scenario.cards.calls.size shouldBe 5
                    scenario.effects.shouldBeEmpty()
                }
            }

            test("[다시 시도]는 멈춘 묶음만 같은 키와 같은 카드 목록으로 다시 보낸다") {
                runTest(rig.dispatcher) {
                    val scenario = pausedByNetwork()

                    scenario.send(ImportDeckIntent.RetrySave)
                    advanceUntilIdle()

                    scenario.cards.calls.size shouldBe 6
                    scenario.cards.calls[5].key shouldBe scenario.cards.calls[1].key
                    scenario.cards.calls[5].cards shouldBeSameInstanceAs scenario.cards.calls[1].cards
                }
            }

            test("[다시 시도]는 대화상자를 닫고 저장한 장수를 유지한 채 저장 중으로 돌아간다") {
                runTest(rig.dispatcher) {
                    val scenario = pausedByNetwork()
                    scenario.cards.callDelayMillis = 100

                    scenario.send(ImportDeckIntent.RetrySave)
                    runCurrent()

                    scenario.state.dialog shouldBe null
                    scenario.state.saving!!.savedCards shouldBe 1_000
                    scenario.state.saving!!.status shouldBe SavingStatus.Saving
                }
            }

            test("[다시 시도] 뒤 완료하면 이전에 저장한 장수와 이번 장수를 더해 이동한다") {
                runTest(rig.dispatcher) {
                    val scenario = pausedByNetwork()

                    scenario.send(ImportDeckIntent.RetrySave)
                    advanceUntilIdle()

                    scenario.effects shouldBe listOf(scenario.navigation(saved = 1_500, completed = true))
                }
            }

            test("[다시 시도]는 첫 전송 시각을 기억하는 같은 조정 객체로 보낸다") {
                runTest(rig.dispatcher) {
                    val coordinators = mutableListOf<ImportSaveCoordinator>()
                    val scenario = pausedByNetwork(coordinators = coordinators)
                    coordinators.sumOf { it.trackedKeyCount() } shouldBe 1
                    val createdBeforeRetry = coordinators.size

                    scenario.send(ImportDeckIntent.RetrySave)
                    advanceUntilIdle()

                    coordinators.size shouldBe createdBeforeRetry
                    coordinators.sumOf { it.trackedKeyCount() } shouldBe 0
                }
            }

            test("저장 흐름마다 팩토리에서 다른 조정 객체를 받는다") {
                runTest(rig.dispatcher) {
                    val repositories = mutableListOf<ScriptedCardRepository>()
                    val factory =
                        ImportSaveCoordinatorFactory {
                            val repository = ScriptedCardRepository().also(repositories::add)
                            ImportSaveCoordinator(savedDeckRepository(), repository)
                        }
                    val first = saveScenario(rig, ScriptedCardRepository(), coordinatorFactory = factory)
                    val second = saveScenario(rig, ScriptedCardRepository(), coordinatorFactory = factory)

                    runSaveToEnd(first)
                    runSaveToEnd(second)

                    repositories.filter { it.calls.isNotEmpty() }.map { it.calls.size } shouldBe listOf(1, 1)
                }
            }
        }

        context("확정 실패") {
            test("서버가 거절하면 자동 재시도 없이 저장한 장수와 함께 일부 저장 실패로 멈춘다") {
                runTest(rig.dispatcher) {
                    val scenario = saveScenario(rig, ScriptedCardRepository(SUCCESS, serverError("C001")), total = 1_500)

                    runSaveToEnd(scenario)

                    scenario.state.dialog shouldBe ImportDialog.PartialFailure(saved = 1_000)
                    scenario.cards.calls.size shouldBe 2
                    scenario.effects.shouldBeEmpty()
                }
            }

            test("거절 뒤 [다시 시도]는 같은 카드 목록을 새 키로 보낸다") {
                runTest(rig.dispatcher) {
                    val scenario = saveScenario(rig, ScriptedCardRepository(SUCCESS, serverError("C001")), total = 1_500)
                    runSaveToEnd(scenario)

                    scenario.send(ImportDeckIntent.RetrySave)
                    advanceUntilIdle()

                    scenario.cards.calls.size shouldBe 3
                    (scenario.cards.calls[2].key != scenario.cards.calls[1].key) shouldBe true
                    scenario.cards.calls[2].cards shouldBeSameInstanceAs scenario.cards.calls[1].cards
                    scenario.effects shouldBe listOf(scenario.navigation(saved = 1_500, completed = true))
                }
            }

            test("일시 정지의 [멈추고 덱 보기]는 저장한 장수까지만 남은 덱으로 이동한다") {
                runTest(rig.dispatcher) {
                    val scenario = pausedByNetwork()

                    scenario.send(ImportDeckIntent.ClickViewDeck)
                    advanceUntilIdle()

                    scenario.effects shouldBe listOf(scenario.navigation(saved = 1_000, completed = false))
                }
            }

            test("저장한 카드가 없을 때 일부 저장 실패의 [멈추고 덱 보기]는 0장으로 이동한다") {
                runTest(rig.dispatcher) {
                    val scenario = saveScenario(rig, ScriptedCardRepository(serverError("C001")))
                    runSaveToEnd(scenario)
                    scenario.state.dialog shouldBe ImportDialog.PartialFailure(saved = 0)

                    scenario.send(ImportDeckIntent.ClickViewDeck)
                    advanceUntilIdle()

                    scenario.effects shouldBe listOf(scenario.navigation(saved = 0, completed = false))
                }
            }

            test("덱을 찾을 수 없으면 덱을 찾을 수 없음 대화상자를 띄우고 [홈으로]는 홈으로 이동한다") {
                runTest(rig.dispatcher) {
                    val scenario = saveScenario(rig, ScriptedCardRepository(serverError("D001")))
                    runSaveToEnd(scenario)
                    scenario.state.dialog shouldBe ImportDialog.DeckUnavailable

                    scenario.send(ImportDeckIntent.ClickGoHome)
                    advanceUntilIdle()

                    scenario.effects shouldBe listOf(ImportDeckSideEffect.NavigateHome)
                }
            }
        }

        context("장수 불일치") {
            test("둘째 묶음이 2장만 만들면 첫 묶음과 더한 1,002장으로 키 충돌 대화상자를 띄운다") {
                runTest(rig.dispatcher) {
                    val scenario = mismatchedSecondChunk()

                    scenario.state.dialog shouldBe ImportDialog.KeyConflict(saved = 1_002)
                    scenario.effects.shouldBeEmpty()
                }
            }

            test("[덱 보기]는 만들어진 장수를 두 번 더하지 않은 1,002장으로 이동한다") {
                runTest(rig.dispatcher) {
                    val scenario = mismatchedSecondChunk()

                    scenario.send(ImportDeckIntent.ClickViewDeck)
                    advanceUntilIdle()

                    scenario.effects shouldBe listOf(scenario.navigation(saved = 1_002, completed = false))
                }
            }

            test("다시 시도할 동작이 없어 RetrySave를 보내도 묶음을 다시 보내지 않는다") {
                runTest(rig.dispatcher) {
                    val scenario = mismatchedSecondChunk()
                    val callsBefore = scenario.cards.calls.size

                    scenario.send(ImportDeckIntent.RetrySave)
                    advanceUntilIdle()

                    scenario.cards.calls.size shouldBe callsBefore
                    scenario.state.dialog shouldBe ImportDialog.KeyConflict(saved = 1_002)
                    scenario.effects.shouldBeEmpty()
                }
            }
        }

        context("닫을 수 없는 대화상자") {
            listOf(
                Triple(
                    "일시 정지",
                    { ScriptedCardRepository(networkTimeout(), networkTimeout(), networkTimeout(), networkTimeout()) },
                    ImportDialog.Paused(0, PauseReason.NETWORK),
                ),
                Triple("일부 저장 실패", { ScriptedCardRepository(serverError("C001")) }, ImportDialog.PartialFailure(0)),
                Triple("키 충돌", { ScriptedCardRepository(serverError("C009")) }, ImportDialog.KeyConflict(0)),
                Triple("덱을 찾을 수 없음", { ScriptedCardRepository(serverError("D001")) }, ImportDialog.DeckUnavailable),
            ).forEach { (name, script, dialog) ->
                test("$name 대화상자는 닫기를 보내도 그대로 남는다") {
                    runTest(rig.dispatcher) {
                        val scenario = saveScenario(rig, script())
                        runSaveToEnd(scenario)
                        scenario.state.dialog shouldBe dialog

                        scenario.send(ImportDeckIntent.DismissDialog)
                        advanceUntilIdle()

                        scenario.state.dialog shouldBe dialog
                        scenario.effects.shouldBeEmpty()
                    }
                }
            }
        }

        context("인증 만료") {
            test("CaroAuthException은 대화상자나 오류 side effect로 바꾸지 않고 예외 처리 경로로 넘긴다") {
                runTest(rig.dispatcher) {
                    val unhandled = mutableListOf<Throwable>()
                    val filter = ExceptionFilter { throwable -> unhandled.add(throwable) }
                    val cards = ScriptedCardRepository(CaroAuthException.TokenExpired(debugMessage = "expired"))
                    val scenario = saveScenario(rig, cards, exceptionFilter = filter)

                    runSaveToEnd(scenario)

                    scenario.cards.calls.size shouldBe 1
                    unhandled.single().shouldBeInstanceOf<CaroAuthException.TokenExpired>()
                    scenario.state.dialog shouldBe null
                    scenario.effects.shouldBeEmpty()
                }
            }
        }
    }

    /** 1,500장 중 첫 묶음은 저장되고, 둘째 묶음이 연결 오류를 4번 받아 자동 재시도를 다 쓴 상태다. */
    private fun TestScope.pausedByNetwork(coordinators: MutableList<ImportSaveCoordinator> = mutableListOf()): SaveScenario {
        val cards = ScriptedCardRepository(SUCCESS, networkTimeout(), networkTimeout(), networkTimeout(), networkTimeout())
        val deckRepository = savedDeckRepository()
        val factory =
            ImportSaveCoordinatorFactory {
                ImportSaveCoordinator(deckRepository, cards, timeSource = testScheduler.timeSource).also(coordinators::add)
            }
        val scenario = saveScenario(rig, cards, total = 1_500, coordinatorFactory = factory)
        runSaveToEnd(scenario)
        scenario.state.dialog shouldBe ImportDialog.Paused(saved = 1_000, reason = PauseReason.NETWORK)
        return scenario
    }

    /** 1,003장(1,000 + 3) 중 둘째 묶음이 3장 대신 2장만 만든 상태다. */
    private fun TestScope.mismatchedSecondChunk(): SaveScenario {
        val cards = ScriptedCardRepository().apply { createdCount = { sent -> if (sent == 3) 2 else sent } }
        val scenario = saveScenario(rig, cards, total = 1_003)
        runSaveToEnd(scenario)
        scenario.state.dialog shouldBe ImportDialog.KeyConflict(saved = 1_002)
        return scenario
    }

    private companion object {
        val SUCCESS: Throwable? = null

        fun networkTimeout() = NetworkException.Timeout(debugMessage = "timeout")

        fun processing() = serverError("C010")

        fun serverError(code: String) = CaroServerException(code = code, message = code, debugMessage = code)
    }
}
