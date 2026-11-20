package com.whatever.caro.feature.importer.mvi

import com.whatever.caro.core.importparser.SpreadsheetDateStyle
import com.whatever.caro.core.viewmodel.contract.UiIntent
import com.whatever.caro.feature.importer.platform.PickResult
import com.whatever.caro.feature.importer.transform.ImportCard

/**
 * 문구와 앱 언어는 Route가 리소스에서 읽어 넘기고 ViewModel은 평문만 받는다.
 * 처리 중이거나 대화상자, 카드 전체 보기가 떠 있으면 뒤로 가기, 닫기, 선택기 결과만 받고 나머지는 버린다.
 */
sealed interface ImportDeckIntent : UiIntent {
    /** 대화상자와 카드 전체 보기가 떠 있으면 그것만 닫고, 처리 중이면 처리를 멈춘다. */
    data object ClickBack : ImportDeckIntent

    /** 버튼이 하나인 대화상자의 [확인], 처음으로 돌아갈지 묻는 대화상자의 [취소], 바깥 누름이 모두 이 입력이다. */
    data object DismissDialog : ImportDeckIntent

    /** 처음으로 돌아갈지 묻는 대화상자의 [돌아가기]. */
    data object ConfirmDiscard : ImportDeckIntent

    data object ClickPickFile : ImportDeckIntent

    data object ClickPaste : ImportDeckIntent

    /** [emptySideText]는 빈 면에 넣을 앱 언어 문구다. 읽은 뒤 바로 미리보기를 만들므로 읽기를 시작하는 입력이 함께 넘긴다. */
    data class FilePicked(
        val result: PickResult,
        val dateStyle: SpreadsheetDateStyle,
        val emptySideText: String,
    ) : ImportDeckIntent

    data class UpdatePasteText(
        val text: String,
    ) : ImportDeckIntent

    /** [sourceName]은 붙여넣기의 덱 이름 기본값으로도 쓴다. */
    data class ClickPasteNext(
        val sourceName: String,
        val dateStyle: SpreadsheetDateStyle,
        val emptySideText: String,
    ) : ImportDeckIntent

    data class SelectPreviewFilter(
        val filter: ImportPreviewFilter,
    ) : ImportDeckIntent

    data class ClickCard(
        val card: ImportCard,
    ) : ImportDeckIntent

    data object CloseCardDetail : ImportDeckIntent

    data object DismissDelimiterBanner : ImportDeckIntent
}
