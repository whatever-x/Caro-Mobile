package com.whatever.caro.feature.importer.logic

import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.mvi.PauseReason
import com.whatever.caro.feature.importer.save.ChunkFailure
import com.whatever.caro.feature.importer.save.ImportSaveOutcome
import com.whatever.caro.feature.importer.save.isSaveUnconfirmed

internal sealed interface SaveOutcomeAction {
    /** 대화상자의 [다시 시도]와 [덱 보기]는 [chunkIndex] 묶음과 [failure]로 처리하므로, 둘 없이는 대화상자를 띄울 수 없게 함께 담는다. */
    data class ShowDialog(
        val dialog: ImportDialog,
        val chunkIndex: Int,
        val failure: ChunkFailure,
    ) : SaveOutcomeAction

    data class NavigateToDeckDetail(
        val saved: Int,
        val completed: Boolean,
        val isSaveUnconfirmed: Boolean,
    ) : SaveOutcomeAction
}

internal enum class SavedCountLine {
    SavedCards,
    NoCardsSaved,
    SavedThenUnconfirmed,
    NoneUnconfirmed,
}

internal enum class CauseLine {
    CheckNetwork,
    TryLater,
    RetryOrViewDeck,
    RetryOrViewEmptyDeck,
    ViewDeck,
    CheckInDeck,
}

/** [causeLine]이 null이면 저장 줄만 보인다. */
internal data class SaveDialogBody(
    val saved: Int,
    val savedLine: SavedCountLine,
    val causeLine: CauseLine?,
)

/** 멈추는 중에는 멈춤을 이미 확인했으므로 진행 중 묶음이 실패해도 대화상자 없이 부분 저장 덱으로 간다. 덱을 찾을 수 없는 경우만 예외다. */
internal fun resolveSaveOutcome(
    outcome: ImportSaveOutcome,
    alreadySaved: Int,
    isStopping: Boolean,
): SaveOutcomeAction =
    when (outcome) {
        is ImportSaveOutcome.Completed -> {
            SaveOutcomeAction.NavigateToDeckDetail(saved = alreadySaved + outcome.savedCards, completed = true, isSaveUnconfirmed = false)
        }

        is ImportSaveOutcome.Stopped -> {
            val saved = alreadySaved + outcome.savedCards
            val dialog = outcome.failure.toDialog(saved)
            if (dialog == null || (isStopping && dialog != ImportDialog.DeckUnavailable)) {
                SaveOutcomeAction.NavigateToDeckDetail(
                    saved = saved,
                    completed = false,
                    isSaveUnconfirmed = outcome.failure.isSaveUnconfirmed,
                )
            } else {
                SaveOutcomeAction.ShowDialog(dialog, outcome.chunkIndex, outcome.failure)
            }
        }
    }

/**
 * 결과를 확인하지 못했으면 빈 덱이나 단정한 장수를 안내하지 않는다. 일시 정지는 늘 확인하지 못한 실패다.
 * 저장 장수를 담지 않는 대화상자는 본문 규칙이 없어 null이다.
 */
internal fun saveDialogBody(dialog: ImportDialog): SaveDialogBody? =
    when (dialog) {
        is ImportDialog.Paused -> {
            val cause = if (dialog.reason == PauseReason.Network) CauseLine.CheckNetwork else CauseLine.TryLater
            SaveDialogBody(dialog.saved, unconfirmedLine(dialog.saved), cause)
        }

        is ImportDialog.PartialFailure -> {
            val cause = if (dialog.saved == 0) CauseLine.RetryOrViewEmptyDeck else CauseLine.RetryOrViewDeck
            SaveDialogBody(dialog.saved, confirmedLine(dialog.saved), cause)
        }

        is ImportDialog.KeyConflict -> {
            keyConflictBody(dialog)
        }

        else -> {
            null
        }
    }

/** 장수가 확정된 키 충돌에서 저장한 카드가 없으면 볼 카드가 없으므로 원인 줄을 두지 않는다. */
private fun keyConflictBody(dialog: ImportDialog.KeyConflict): SaveDialogBody =
    when {
        dialog.isSaveUnconfirmed -> SaveDialogBody(dialog.saved, unconfirmedLine(dialog.saved), CauseLine.CheckInDeck)
        dialog.saved == 0 -> SaveDialogBody(dialog.saved, SavedCountLine.NoCardsSaved, causeLine = null)
        else -> SaveDialogBody(dialog.saved, SavedCountLine.SavedCards, CauseLine.ViewDeck)
    }

private fun confirmedLine(saved: Int): SavedCountLine = if (saved == 0) SavedCountLine.NoCardsSaved else SavedCountLine.SavedCards

private fun unconfirmedLine(saved: Int): SavedCountLine =
    if (saved == 0) SavedCountLine.NoneUnconfirmed else SavedCountLine.SavedThenUnconfirmed

/** 사용자가 멈춘 결과는 대화상자가 없어 null이다. */
private fun ChunkFailure.toDialog(saved: Int): ImportDialog? =
    when (this) {
        ChunkFailure.RetryExhausted -> {
            ImportDialog.Paused(saved, PauseReason.Network)
        }

        ChunkFailure.ProcessingTimeout,
        ChunkFailure.InvalidResponse,
        ChunkFailure.Unknown,
        -> {
            ImportDialog.Paused(saved, PauseReason.Later)
        }

        is ChunkFailure.Rejected -> {
            ImportDialog.PartialFailure(saved)
        }

        ChunkFailure.KeyConflict,
        is ChunkFailure.ResponseCountMismatch,
        -> {
            ImportDialog.KeyConflict(saved, isSaveUnconfirmed)
        }

        ChunkFailure.DeckUnavailable -> {
            ImportDialog.DeckUnavailable
        }

        ChunkFailure.StoppedByUser,
        ChunkFailure.StoppedAfterFailedSend,
        -> {
            null
        }
    }
