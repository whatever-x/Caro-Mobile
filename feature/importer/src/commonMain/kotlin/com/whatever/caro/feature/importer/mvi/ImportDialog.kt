package com.whatever.caro.feature.importer.mvi

import com.whatever.caro.core.importparser.ImportParseErrorKind
import com.whatever.caro.feature.importer.logic.NoCardsKind

/**
 * [isDismissible]인 대화상자는 바깥 누름과 뒤로 가기로 닫힌다. 버튼이 하나인 대화상자는 닫기가 [확인]과 같고,
 * [DiscardConfirm]과 [DeckCreateFailed]는 [취소], [StopConfirm]은 [계속 가져오기]와 같다.
 * [FileTooLarge]와 [ParseError]의 extension은 소문자이고 확장자가 없으면 빈 문자열이다.
 * 저장 결과 대화상자의 saved는 저장을 확인한 장수다.
 */
sealed interface ImportDialog {
    data class FileTooLarge(
        val extension: String,
    ) : ImportDialog

    /** 제목과 본문은 `parseErrorCopy`로 고른다. */
    data class ParseError(
        val kind: ImportParseErrorKind,
        val extension: String,
        val isPaste: Boolean,
    ) : ImportDialog

    data class NoCards(
        val kind: NoCardsKind,
    ) : ImportDialog

    data class TooManyCards(
        val isPaste: Boolean,
    ) : ImportDialog

    data object DiscardConfirm : ImportDialog

    /** 설정 화면의 초안 다시 읽기와 미리보기 만들기가 함께 쓰는 실패다. 원본을 남기므로 닫으면 그 화면에서 다시 시도한다. */
    data object PreviewFailed : ImportDialog

    /** 미리보기와 원본을 남기므로 [취소]하면 미리보기에 머문다. */
    data object DeckCreateFailed : ImportDialog

    data object StopConfirm : ImportDialog

    /** 본문 문장은 `saveDialogBody`로 고른다. 여기로 오는 실패는 모두 그 묶음의 저장 결과를 확인하지 못했다. */
    data class Paused(
        val saved: Int,
        val reason: PauseReason,
    ) : ImportDialog

    data class PartialFailure(
        val saved: Int,
    ) : ImportDialog

    /** [isSaveUnconfirmed]면 서버에 [saved]보다 더 저장됐을 수 있다. 다시 보낼 수 없어 [덱 보기]만 있다. */
    data class KeyConflict(
        val saved: Int,
        val isSaveUnconfirmed: Boolean,
    ) : ImportDialog

    data object DeckUnavailable : ImportDialog
}

/** 응답 해석 실패에 네트워크 확인을 권하면 틀리므로 자동 재시도를 다 쓴 연결 실패만 따로 둔다. */
enum class PauseReason {
    Network,
    Later,
}

/** 저장이 멈춘 뒤의 대화상자가 닫히면 멈춘 저장 화면에 남아, [가져오기 멈추기]가 이미 멈춘 저장을 다시 멈추려 한다. */
internal val ImportDialog.isDismissible: Boolean
    get() =
        when (this) {
            is ImportDialog.Paused,
            is ImportDialog.PartialFailure,
            is ImportDialog.KeyConflict,
            ImportDialog.DeckUnavailable,
            -> false

            is ImportDialog.FileTooLarge,
            is ImportDialog.ParseError,
            is ImportDialog.NoCards,
            is ImportDialog.TooManyCards,
            ImportDialog.DiscardConfirm,
            ImportDialog.PreviewFailed,
            ImportDialog.DeckCreateFailed,
            ImportDialog.StopConfirm,
            -> true
        }

internal fun ImportDeckState.dismissingDialog(): ImportDeckState = if (dialog?.isDismissible == true) copy(dialog = null) else this
