package com.whatever.caro.feature.importer.mvi

import com.whatever.caro.core.importparser.ImportParseErrorKind
import com.whatever.caro.feature.importer.logic.NoCardsKind

/**
 * 모두 바깥 누름과 뒤로 가기로 닫힌다. 버튼이 하나인 대화상자는 닫기가 [확인]과 같고, [DiscardConfirm]은 [취소]와 같다.
 * [FileTooLarge]와 [ParseError]의 extension은 소문자이고 확장자가 없으면 빈 문자열이다.
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
}
