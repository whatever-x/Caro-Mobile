package com.whatever.caro.feature.importer.support

import com.whatever.caro.core.data.repository.deck.DeckRepository
import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.model.deck.DeckState
import com.whatever.caro.core.model.exception.CaroServerException
import com.whatever.caro.core.model.exception.NetworkException
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckSideEffect
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.save.ImportSaveCoordinator
import com.whatever.caro.feature.importer.save.ImportSaveCoordinatorFactory
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle

internal val SAVE_SCENARIO_DECK = Deck(7L, "words", "words.txt에서 가져왔어요", 0, 0, 0, DeckState.entries.first())

internal fun savedDeckRepository(): DeckRepository =
    mock<DeckRepository> {
        everySuspend { createDeck(any(), any()) } returns SAVE_SCENARIO_DECK
    }

internal fun networkTimeout(): NetworkException = NetworkException.Timeout(debugMessage = "timeout")

internal fun serverError(code: String): CaroServerException = CaroServerException(code = code, message = code, debugMessage = code)

/** 미리보기까지 열어 둔 가져오기 화면이다. [ImportDeckIntent.ClickImport]를 보내면 덱을 만들고 저장을 시작한다. [total]은 이동 effect의 전체 장수다. */
internal class SaveScenario(
    val harness: ImportViewModelHarness,
    val cards: ScriptedCardRepository,
    val effects: List<ImportDeckSideEffect>,
    val total: Int,
) {
    val state: ImportDeckState get() = harness.state

    fun send(intent: ImportDeckIntent) = harness.send(intent)

    fun navigation(
        saved: Int,
        completed: Boolean,
        isSaveUnconfirmed: Boolean = false,
    ) = ImportDeckSideEffect.NavigateToDeckDetail(SAVE_SCENARIO_DECK, saved, total, completed, isSaveUnconfirmed)
}

/** 조정 객체의 시간원을 가상 시간에 맞춘다. [coordinatorFactory]를 넘기면 저장 조정 객체를 만드는 방식을 테스트가 정한다. */
internal fun TestScope.saveScenario(
    rig: ImportViewModelRig,
    cards: ScriptedCardRepository,
    total: Int = 3,
    coordinatorFactory: ImportSaveCoordinatorFactory? = null,
    exceptionFilter: ExceptionFilter = ExceptionFilter.None,
    defaultDispatcher: CoroutineDispatcher = rig.dispatcher,
): SaveScenario {
    val deckRepository = savedDeckRepository()
    val harness =
        rig.create(
            reader = FakeImportSourceReader().returns(ImportTestTables.text(rows = total)),
            default = defaultDispatcher,
            saveCoordinatorFactory =
                coordinatorFactory
                    ?: ImportSaveCoordinatorFactory { ImportSaveCoordinator(deckRepository, cards, timeSource = testScheduler.timeSource) },
            exceptionFilter = exceptionFilter,
        )
    harness.pickNamed("words.txt")
    advanceUntilIdle()
    harness.state.step shouldBe ImportStep.Preview

    val effects = mutableListOf<ImportDeckSideEffect>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { harness.viewModel.sideEffect.collect { effects += it } }
    return SaveScenario(harness, cards, effects, total)
}

/** [가져오기]를 눌러 저장을 끝까지(가상 시간) 돌린다. */
internal fun TestScope.runSaveToEnd(scenario: SaveScenario) {
    scenario.send(ImportDeckIntent.ClickImport)
    advanceUntilIdle()
}
