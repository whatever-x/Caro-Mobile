package com.whatever.caro.feature.importer.mvi

import com.whatever.caro.core.importparser.ImportParseErrorKind
import com.whatever.caro.feature.importer.transform.ImportCard

enum class ImportDismissPolicy(
    val dismissOnBackPress: Boolean,
    val dismissOnClickOutside: Boolean,
) {
    /** 닫기가 확인 버튼과 같다. */
    Confirm(dismissOnBackPress = true, dismissOnClickOutside = true),

    /** 닫기가 취소 버튼과 같다. */
    Cancel(dismissOnBackPress = true, dismissOnClickOutside = true),

    /** 닫기가 계속 가져오기 버튼과 같다. */
    Continue(dismissOnBackPress = true, dismissOnClickOutside = true),

    /** 저장이 멈춘 뒤 대화상자만 닫히면 멈춘 저장 화면에 남아, 멈추기 버튼이 이미 멈춘 저장에 대해 다시 멈춤 확인을 묻게 된다. */
    NotDismissible(dismissOnBackPress = false, dismissOnClickOutside = false),
}

/** 저장이 멈춘 원인. 원인마다 안내 문장이 다르다. 응답 해석 실패에 네트워크 확인을 권하면 틀리므로 네트워크 소진만 따로 둔다. */
enum class PauseReason {
    NETWORK,
    LATER,
}

/**
 * [saved]는 지금까지 덱에 저장된 카드 수다.
 * [FileTooLarge], [ParseError]의 extension은 소문자이고 확장자가 없으면 빈 문자열이다.
 */
sealed class ImportDialog(
    val dismissPolicy: ImportDismissPolicy,
) {
    data class FileTooLarge(
        val extension: String,
    ) : ImportDialog(ImportDismissPolicy.Confirm)

    data class ParseError(
        val kind: ImportParseErrorKind,
        val extension: String,
        val isPaste: Boolean,
    ) : ImportDialog(ImportDismissPolicy.Confirm)

    data class EmptyResult(
        val isPaste: Boolean,
    ) : ImportDialog(ImportDismissPolicy.Confirm)

    /** 읽은 내용을 유지한 채 항목 연결에 머문다. 읽기 실패 대화상자와 달리 닫아도 처음 단계로 돌아가지 않는다. */
    data object PreviewFailed : ImportDialog(ImportDismissPolicy.Confirm)

    data object DiscardConfirm : ImportDialog(ImportDismissPolicy.Cancel)

    data object DeckCreateFailed : ImportDialog(ImportDismissPolicy.Cancel)

    data object StopConfirm : ImportDialog(ImportDismissPolicy.Continue)

    data class Paused(
        val saved: Int,
        val reason: PauseReason,
    ) : ImportDialog(ImportDismissPolicy.NotDismissible)

    data class PartialFailure(
        val saved: Int,
    ) : ImportDialog(ImportDismissPolicy.NotDismissible)

    data class KeyConflict(
        val saved: Int,
    ) : ImportDialog(ImportDismissPolicy.NotDismissible)

    data object DeckUnavailable : ImportDialog(ImportDismissPolicy.NotDismissible)

    data class CardDetail(
        val card: ImportCard,
    ) : ImportDialog(ImportDismissPolicy.Confirm)
}
