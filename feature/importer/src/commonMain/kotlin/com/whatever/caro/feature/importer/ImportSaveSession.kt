package com.whatever.caro.feature.importer

import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.model.exception.CaroAuthException
import com.whatever.caro.feature.importer.logic.SaveOutcomeAction
import com.whatever.caro.feature.importer.logic.resolveSaveOutcome
import com.whatever.caro.feature.importer.mvi.ImportDeckSideEffect
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.mvi.ImportLoading
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.mvi.SavingProgress
import com.whatever.caro.feature.importer.mvi.SavingStatus
import com.whatever.caro.feature.importer.save.ChunkFailure
import com.whatever.caro.feature.importer.save.DeckCreationResult
import com.whatever.caro.feature.importer.save.DeckRejection
import com.whatever.caro.feature.importer.save.ImportChunk
import com.whatever.caro.feature.importer.save.ImportSaveCoordinator
import com.whatever.caro.feature.importer.save.ImportSaveCoordinatorFactory
import com.whatever.caro.feature.importer.save.ImportSaveOutcome
import com.whatever.caro.feature.importer.save.ImportSaveProgress
import com.whatever.caro.feature.importer.save.UserRetry
import com.whatever.caro.feature.importer.save.isSaveUnconfirmed
import com.whatever.caro.feature.importer.source.ImportDispatchers
import com.whatever.caro.feature.importer.transform.ImportCard
import com.whatever.caro.feature.importer.transform.toCardContent
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext

/** BaseViewModel의 reduce, postSideEffect, launch는 protected라, 저장 흐름은 ViewModel이 구현한 이 interface로 상태와 effect를 바꾼다. */
internal interface ImportSaveHost {
    val state: ImportDeckState

    fun reduce(transform: ImportDeckState.() -> ImportDeckState)

    fun postSideEffect(effect: ImportDeckSideEffect)

    fun launch(block: suspend () -> Unit): Job

    /** 저장을 시작하면 앞 단계로 돌아갈 수 없으므로, 네트워크를 쓰는 동안 원본 사본을 들고 있지 않는다. */
    fun releaseSource()
}

/**
 * 미리보기의 [가져오기]부터 덱 만들기와 카드 묶음 저장까지 맡는다.
 * 묶음 목록과 키는 화면에 보이지 않으므로 상태에 넣지 않고 여기에 둔다.
 * 저장 중 인증 만료는 앱 전역 처리가 로그인 화면으로 바꾸며 이 ViewModel을 지운다는 전제로 받지 않는다.
 */
internal class ImportSaveSession(
    private val host: ImportSaveHost,
    private val coordinatorFactory: ImportSaveCoordinatorFactory,
    private val dispatchers: ImportDispatchers,
) {
    private class SaveTarget(
        val coordinator: ImportSaveCoordinator,
        val deck: Deck,
        val total: Int,
    )

    private var target: SaveTarget? = null
    private var chunks: List<ImportChunk> = emptyList()

    // 이전 저장 호출들이 덱에 저장한 장수다.
    // 장수 불일치로 만들어진 카드는 진행 이벤트로 오지 않으므로, 대화상자를 띄울 때 결과 장수를 상태에 써 둬야 다시 시도와 [덱 보기]가 이 값을 쓴다.
    private val savedCards: Int get() = host.state.saving?.savedCards ?: 0

    // 멈춤을 확인하면 Stopping이 되고 진행 이벤트가 이를 덮지 않으므로, 저장이 끝날 때까지 멈춤 요청 여부와 같다.
    private val isStopping: Boolean get() = host.state.saving?.status == SavingStatus.Stopping

    // 실패 대화상자를 띄운 채 사용자 선택을 기다리는 저장이다. 이동하거나 다시 시도하면 비워 같은 선택이 두 번 처리되지 않게 한다.
    private var stalledSave: StalledSave? = null

    /** 이름이나 설명이 비면 서버가 거절할 값이라 보내지 않고 거절과 같은 안내를 띄운다. */
    fun startImport() {
        val state = host.state
        if (state.step != ImportStep.Preview || state.deckInfoDraft != null) return
        if (state.deckName.isBlank() || state.deckDescription.isBlank()) {
            host.postSideEffect(ImportDeckSideEffect.ShowDeckInputError)
            return
        }
        createDeck()
    }

    fun retryCreateDeck() {
        if (host.state.dialog == ImportDialog.DeckCreateFailed) createDeck()
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
        host.reduce { copy(dialog = null).withSavingStatus(SavingStatus.Stopping) }
    }

    /** 새 키가 필요한 실패는 멈춘 묶음만 키를 바꾸고, 같은 키로 보낼 실패는 묶음을 그대로 다시 보낸다. */
    fun retry() {
        val stalled = stalledSave ?: return
        val active = target ?: return
        val index = stalled.chunkIndex
        when (stalled.failure.userRetry) {
            UserRetry.NONE -> return
            UserRetry.NEW_KEY -> chunks = chunks.take(index) + active.coordinator.renewKey(chunks[index]) + chunks.drop(index + 1)
            UserRetry.SAME_KEY -> Unit
        }
        stalledSave = null
        host.reduce { copy(dialog = null).withSavingStatus(SavingStatus.Saving) }
        run(fromIndex = index)
    }

    fun viewDeck() {
        val stalled = stalledSave ?: return
        if (stalled.failure is ChunkFailure.DeckUnavailable) return
        navigateToDeckDetail(savedCards, completed = false, isSaveUnconfirmed = stalled.failure.isSaveUnconfirmed)
    }

    fun goHome() {
        if (stalledSave?.failure !is ChunkFailure.DeckUnavailable) return
        stalledSave = null
        host.postSideEffect(ImportDeckSideEffect.NavigateHome)
    }

    /** 덮개를 먼저 띄워 연달아 누른 입력을 막는다. 덱이 만들어졌는지 모를 수 있어 실패해도 자동으로 다시 보내지 않는다. */
    private fun createDeck() {
        val state = host.state
        val cards = state.preview?.cards ?: return
        host.reduce { copy(dialog = null, loading = ImportLoading.CreatingDeck) }
        host.launch {
            val coordinator = coordinatorFactory.create()
            val result =
                try {
                    coordinator.createDeck(state.deckName, state.deckDescription)
                } catch (expired: CaroAuthException) {
                    // 세션 만료 이벤트 없이 이 예외만 오면 전역 처리가 화면을 바꾸지 않아, 덮개를 걷지 않으면 뒤로 가기도 막힌 채 갇힌다.
                    host.reduce { copy(loading = null) }
                    throw expired
                }
            when (result) {
                is DeckCreationResult.Created -> startSaving(coordinator, result.deck, cards)
                is DeckCreationResult.Failed -> failCreation(result.cause)
                is DeckCreationResult.Rejected -> rejectCreation(result.reason)
            }
        }
    }

    private fun failCreation(cause: Throwable?) {
        cause?.let { Napier.w(throwable = it) { "import deck creation failed" } }
        host.reduce { copy(loading = null, dialog = ImportDialog.DeckCreateFailed) }
    }

    private fun rejectCreation(reason: DeckRejection) {
        when (reason) {
            DeckRejection.INVALID_INPUT -> {
                host.reduce { copy(loading = null) }
                host.postSideEffect(ImportDeckSideEffect.ShowDeckInputError)
            }

            DeckRejection.OTHER -> {
                failCreation(cause = null)
            }
        }
    }

    /**
     * 변환과 묶음 나누기가 최대 1만 장의 모든 글자를 훑으므로 main에서 하지 않는다.
     * 묶음 계획이 실패해도 덱은 이미 만들어졌으므로, 다시 만들게 하지 않고 저장 0장으로 덱 화면에 보낸다.
     */
    private suspend fun startSaving(
        coordinator: ImportSaveCoordinator,
        deck: Deck,
        cards: List<ImportCard>,
    ) {
        target = SaveTarget(coordinator, deck, cards.size)
        chunks =
            try {
                withContext(dispatchers.default) { coordinator.planChunks(cards.map(ImportCard::toCardContent)) }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                Napier.e(throwable = error) { "import chunk planning failed" }
                navigateToDeckDetail(saved = 0, completed = false, isSaveUnconfirmed = false)
                return
            }
        host.releaseSource()
        host.reduce {
            ImportDeckState(
                step = ImportStep.Saving,
                deckName = deckName,
                deckDescription = deckDescription,
                saving = SavingProgress(deckName = deckName, totalCards = cards.size),
            )
        }
        run(fromIndex = 0)
    }

    private fun run(fromIndex: Int) {
        val active = target ?: return
        val planned = chunks
        val alreadySaved = savedCards
        var savedInRun = 0
        host.launch {
            val outcome =
                active.coordinator.saveChunks(
                    deckId = active.deck.id,
                    chunks = planned,
                    fromIndex = fromIndex,
                    stopRequested = { isStopping },
                    onProgress = { progress ->
                        if (progress is ImportSaveProgress.ChunkSaved) savedInRun += progress.cardCount
                        host.reduce { withSavingStatus(progress.status(), alreadySaved + savedInRun) }
                    },
                )
            onOutcome(outcome, alreadySaved)
        }
    }

    private fun onOutcome(
        outcome: ImportSaveOutcome,
        alreadySaved: Int,
    ) {
        when (val action = resolveSaveOutcome(outcome, alreadySaved, isStopping)) {
            is SaveOutcomeAction.NavigateToDeckDetail -> {
                navigateToDeckDetail(action.saved, action.completed, action.isSaveUnconfirmed)
            }

            is SaveOutcomeAction.ShowDialog -> {
                stalledSave = StalledSave(action.chunkIndex, action.failure)
                host.reduce { copy(dialog = action.dialog).withSavingStatus(SavingStatus.Saving, alreadySaved + outcome.savedCards) }
            }
        }
    }

    private fun navigateToDeckDetail(
        saved: Int,
        completed: Boolean,
        isSaveUnconfirmed: Boolean,
    ) {
        val active = target ?: return
        stalledSave = null
        host.postSideEffect(ImportDeckSideEffect.NavigateToDeckDetail(active.deck, saved, active.total, completed, isSaveUnconfirmed))
    }
}

/** [chunkIndex] 묶음에서 멈췄다. 앞 묶음은 저장됐다. */
private class StalledSave(
    val chunkIndex: Int,
    val failure: ChunkFailure,
)

private fun ImportSaveProgress.status(): SavingStatus =
    when (this) {
        is ImportSaveProgress.ChunkSaved -> SavingStatus.Saving
        is ImportSaveProgress.Retrying -> SavingStatus.Retrying
        is ImportSaveProgress.WaitingForServer -> SavingStatus.WaitingForServer
    }

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
