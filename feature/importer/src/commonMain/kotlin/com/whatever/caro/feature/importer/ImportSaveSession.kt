package com.whatever.caro.feature.importer

import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.feature.importer.logic.SaveOutcomeAction
import com.whatever.caro.feature.importer.logic.resolveSaveOutcome
import com.whatever.caro.feature.importer.mvi.ImportDeckSideEffect
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.mvi.SavingStatus
import com.whatever.caro.feature.importer.save.ChunkFailure
import com.whatever.caro.feature.importer.save.ImportChunk
import com.whatever.caro.feature.importer.save.ImportSaveCoordinator
import com.whatever.caro.feature.importer.save.ImportSaveOutcome
import com.whatever.caro.feature.importer.save.ImportSaveProgress
import com.whatever.caro.feature.importer.save.UserRetry

/** 저장 흐름이 ViewModel의 필드를 직접 만지지 않고 화면 상태와 effect를 바꾸는 통로. */
internal interface ImportSaveHost {
    val state: ImportDeckState

    fun reduce(transform: ImportDeckState.() -> ImportDeckState)

    fun postSideEffect(effect: ImportDeckSideEffect)

    fun launch(block: suspend () -> Unit)
}

/** [deck]을 만든 뒤 카드 묶음을 저장하는 흐름 하나. 묶음 목록과 키는 화면에 보이지 않으므로 상태에 넣지 않고 여기에 둔다. */
internal class ImportSaveSession(
    private val coordinator: ImportSaveCoordinator,
    private val deck: Deck,
    private var chunks: List<ImportChunk>,
    private val host: ImportSaveHost,
) {
    // 이전 저장 호출들이 덱에 저장한 장수다. 다시 시도한 호출의 결과에는 이 값을 더해 쓴다.
    private var savedCards = 0
    private var stopRequested = false

    // 실패 대화상자를 띄운 채 사용자 선택을 기다리는 중인 저장. 이동하거나 다시 시도하면 비워 같은 선택이 두 번 처리되지 않게 한다.
    private var stalledSave: StalledSave? = null

    fun start() {
        run(fromIndex = 0)
    }

    /** 멈추는 중이거나 다른 대화상자가 떠 있으면 확인을 다시 열지 않는다. */
    fun requestStop() {
        val state = host.state
        val progress = state.saving ?: return
        if (progress.status == SavingStatus.Stopping || state.dialog != null) return
        host.reduce { copy(dialog = ImportDialog.StopConfirm) }
    }

    fun confirmStop() {
        if (host.state.dialog != ImportDialog.StopConfirm) return
        stopRequested = true
        host.reduce { copy(dialog = null).withSavingStatus(SavingStatus.Stopping) }
    }

    /** 새 키가 필요한 실패는 멈춘 묶음만 키를 바꾸고, 같은 키로 보낼 실패는 묶음을 그대로 다시 보낸다. */
    fun retry() {
        val stalled = stalledSave ?: return
        val index = stalled.chunkIndex
        val userRetry = stalled.failure.userRetry
        if (userRetry == UserRetry.NONE) return
        if (userRetry == UserRetry.NEW_KEY) chunks = chunks.take(index) + coordinator.renewKey(chunks[index]) + chunks.drop(index + 1)
        stalledSave = null
        host.reduce { copy(dialog = null).withSavingStatus(SavingStatus.Saving) }
        run(fromIndex = index)
    }

    fun viewDeck() {
        val stalled = stalledSave ?: return
        if (stalled.failure is ChunkFailure.DeckUnavailable) return
        navigateToDeckDetail(savedCards, completed = false)
    }

    fun goHome() {
        if (stalledSave?.failure !is ChunkFailure.DeckUnavailable) return
        stalledSave = null
        host.postSideEffect(ImportDeckSideEffect.NavigateHome)
    }

    private fun run(fromIndex: Int) {
        val alreadySaved = savedCards
        var savedInRun = 0
        host.launch {
            val outcome =
                coordinator.saveChunks(
                    deckId = deck.id,
                    chunks = chunks,
                    fromIndex = fromIndex,
                    stopRequested = { stopRequested },
                    onProgress = { progress ->
                        when (progress) {
                            is ImportSaveProgress.ChunkSaved -> {
                                savedInRun += progress.cardCount
                                host.reduce { withSavingStatus(SavingStatus.Saving, alreadySaved + savedInRun) }
                            }

                            is ImportSaveProgress.Retrying -> {
                                host.reduce { withSavingStatus(SavingStatus.Retrying) }
                            }

                            is ImportSaveProgress.WaitingForServer -> {
                                host.reduce { withSavingStatus(SavingStatus.WaitingForServer) }
                            }
                        }
                    },
                )
            onOutcome(outcome, alreadySaved)
        }
    }

    private fun onOutcome(
        outcome: ImportSaveOutcome,
        alreadySaved: Int,
    ) {
        savedCards = alreadySaved + outcome.savedCards
        when (val action = resolveSaveOutcome(outcome, alreadySaved, isStopping = stopRequested)) {
            is SaveOutcomeAction.NavigateToDeckDetail -> {
                navigateToDeckDetail(action.saved, action.completed)
            }

            is SaveOutcomeAction.ShowDialog -> {
                stalledSave = (outcome as? ImportSaveOutcome.Stopped)?.let { stopped -> StalledSave(stopped.chunkIndex, stopped.failure) }
                host.reduce { copy(dialog = action.dialog).withSavingStatus(SavingStatus.Saving, savedCards) }
            }
        }
    }

    private fun navigateToDeckDetail(
        saved: Int,
        completed: Boolean,
    ) {
        stalledSave = null
        host.postSideEffect(
            ImportDeckSideEffect.NavigateToDeckDetail(
                deck = deck.copy(cardTotalCount = saved),
                saved = saved,
                total = chunks.sumOf { chunk -> chunk.cards.size },
                completed = completed,
            ),
        )
    }
}

/** [chunkIndex] 묶음부터 저장하지 못했다. */
private class StalledSave(
    val chunkIndex: Int,
    val failure: ChunkFailure,
)

/** 멈추는 중에는 진행 이벤트가 상태를 덮어쓰지 않는다. [savedCards]가 null이면 저장한 장수는 그대로 둔다. */
private fun ImportDeckState.withSavingStatus(
    status: SavingStatus,
    savedCards: Int? = null,
): ImportDeckState =
    copy(
        saving =
            saving?.let { progress ->
                progress.copy(
                    savedCards = savedCards ?: progress.savedCards,
                    status = if (progress.status == SavingStatus.Stopping) progress.status else status,
                )
            },
    )
