package com.whatever.caro.feature.importer.logic

import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.mvi.PauseReason
import com.whatever.caro.feature.importer.save.ChunkFailure
import com.whatever.caro.feature.importer.save.ImportSaveOutcome

internal sealed interface SaveOutcomeAction {
    data class ShowDialog(
        val dialog: ImportDialog,
    ) : SaveOutcomeAction

    data class NavigateToDeckDetail(
        val saved: Int,
        val completed: Boolean,
    ) : SaveOutcomeAction
}

internal enum class SavedCountLine {
    SavedCards,
    NoCardsSaved,
}

internal enum class CauseLine {
    CheckNetwork,
    TryLater,
    RetryOrViewDeck,
    RetryOrViewEmptyDeck,
    ViewDeck,
    EmptyDeckDeletable,
}

internal data class SaveDialogBody(
    val saved: Int,
    val savedLine: SavedCountLine,
    val causeLine: CauseLine,
)

/** 멈추는 중에는 멈춤을 이미 확인했으므로 진행 중 묶음이 실패해도 대화상자 없이 부분 저장 덱으로 간다. 덱을 찾을 수 없는 경우만 예외다. */
internal fun resolveSaveOutcome(
    outcome: ImportSaveOutcome,
    alreadySaved: Int,
    isStopping: Boolean,
): SaveOutcomeAction =
    when (outcome) {
        is ImportSaveOutcome.Completed -> {
            SaveOutcomeAction.NavigateToDeckDetail(saved = alreadySaved + outcome.savedCards, completed = true)
        }

        is ImportSaveOutcome.Stopped -> {
            val saved = alreadySaved + outcome.savedCards
            val dialog = outcome.failure.toDialog(saved)
            if (dialog == null || (isStopping && dialog != ImportDialog.DeckUnavailable)) {
                SaveOutcomeAction.NavigateToDeckDetail(saved = saved, completed = false)
            } else {
                SaveOutcomeAction.ShowDialog(dialog)
            }
        }
    }

/** 저장 장수를 담지 않는 대화상자는 본문 규칙이 없어 null이다. */
internal fun saveDialogBody(dialog: ImportDialog): SaveDialogBody? =
    when (dialog) {
        is ImportDialog.Paused -> {
            val cause = if (dialog.reason == PauseReason.NETWORK) CauseLine.CheckNetwork else CauseLine.TryLater
            SaveDialogBody(dialog.saved, savedCountLine(dialog.saved), cause)
        }

        is ImportDialog.PartialFailure -> {
            val cause = if (dialog.saved == 0) CauseLine.RetryOrViewEmptyDeck else CauseLine.RetryOrViewDeck
            SaveDialogBody(dialog.saved, savedCountLine(dialog.saved), cause)
        }

        is ImportDialog.KeyConflict -> {
            val cause = if (dialog.saved == 0) CauseLine.EmptyDeckDeletable else CauseLine.ViewDeck
            SaveDialogBody(dialog.saved, savedCountLine(dialog.saved), cause)
        }

        else -> {
            null
        }
    }

private fun savedCountLine(saved: Int): SavedCountLine = if (saved == 0) SavedCountLine.NoCardsSaved else SavedCountLine.SavedCards

/** 사용자가 멈춘 결과는 대화상자가 없어 null이다. */
private fun ChunkFailure.toDialog(saved: Int): ImportDialog? =
    when (this) {
        ChunkFailure.RetryExhausted -> ImportDialog.Paused(saved, PauseReason.NETWORK)

        ChunkFailure.ProcessingTimeout,
        ChunkFailure.InvalidResponse,
        ChunkFailure.Unknown,
        -> ImportDialog.Paused(saved, PauseReason.LATER)

        is ChunkFailure.Rejected -> ImportDialog.PartialFailure(saved)

        ChunkFailure.KeyConflict,
        is ChunkFailure.ResponseCountMismatch,
        -> ImportDialog.KeyConflict(saved)

        ChunkFailure.DeckUnavailable -> ImportDialog.DeckUnavailable

        ChunkFailure.StoppedByUser -> null
    }
