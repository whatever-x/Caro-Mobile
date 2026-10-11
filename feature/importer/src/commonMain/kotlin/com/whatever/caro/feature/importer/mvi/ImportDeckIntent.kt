package com.whatever.caro.feature.importer.mvi

import com.whatever.caro.core.importparser.SpreadsheetDateStyle
import com.whatever.caro.core.viewmodel.contract.UiIntent
import com.whatever.caro.feature.importer.platform.PickResult
import com.whatever.caro.feature.importer.transform.ImportCard

/**
 * 문구와 앱 언어는 Route가 리소스에서 읽어 넘기고 ViewModel은 평문만 받는다.
 * [SpreadsheetDateStyle]은 첫 읽기에서만 받고, 이후 다시 읽기는 같은 값을 쓴다.
 */
sealed interface ImportDeckIntent : UiIntent {
    data object ClickBack : ImportDeckIntent

    /** 닫기 규칙은 대화상자의 dismissPolicy를 따른다. 닫을 수 없는 대화상자에는 무시한다. */
    data object DismissDialog : ImportDeckIntent

    data object ClickPickFile : ImportDeckIntent

    data object ClickPaste : ImportDeckIntent

    data class FilePicked(
        val result: PickResult,
        val dateStyle: SpreadsheetDateStyle,
    ) : ImportDeckIntent

    data class UpdatePasteText(
        val text: String,
    ) : ImportDeckIntent

    data class ClickPasteNext(
        val sourceName: String,
        val dateStyle: SpreadsheetDateStyle,
    ) : ImportDeckIntent

    data class ToggleSubdeck(
        val id: String,
    ) : ImportDeckIntent

    data object ToggleAllSubdecks : ImportDeckIntent

    data class SelectSheet(
        val index: Int,
    ) : ImportDeckIntent

    data class SelectDelimiter(
        val delimiter: Char,
    ) : ImportDeckIntent

    /** 시트 선택과 텍스트 확인이 함께 쓴다. */
    data class ChangeHeaderRow(
        val hasHeaderRow: Boolean,
    ) : ImportDeckIntent

    /** 하위 덱 선택, 시트 선택, 텍스트 확인의 다음 버튼. */
    data object ClickSelectionNext : ImportDeckIntent

    data class ToggleNoteType(
        val noteTypeId: String,
    ) : ImportDeckIntent

    /** [target]은 `FieldMapping.FRONT` 또는 `FieldMapping.BACK`이다. 이미 고른 항목이면 선택을 푼다. */
    data class ToggleField(
        val noteTypeId: String,
        val target: String,
        val fieldName: String,
    ) : ImportDeckIntent

    data class ClickMappingNext(
        val emptySideText: String,
    ) : ImportDeckIntent

    data class SelectPreviewFilter(
        val filter: ImportPreviewFilter,
    ) : ImportDeckIntent

    data class ClickCard(
        val card: ImportCard,
    ) : ImportDeckIntent

    /** 덱 정보의 이름과 설명 미리 채움에 쓰는 문구다. 파일 설명 템플릿의 `{file}` 자리에 파일 이름이 들어간다. */
    data class ClickPreviewNext(
        val fileDescriptionTemplate: String,
        val pasteDescription: String,
    ) : ImportDeckIntent

    data class UpdateDeckName(
        val name: String,
    ) : ImportDeckIntent

    data class UpdateDeckDescription(
        val description: String,
    ) : ImportDeckIntent

    data object ClickCreateDeck : ImportDeckIntent

    data object ConfirmDiscard : ImportDeckIntent

    data object RetryCreateDeck : ImportDeckIntent

    /** 저장 중 화면의 멈추기 버튼. 뒤로 가기와 같은 멈춤 확인을 연다. */
    data object ClickStop : ImportDeckIntent

    data object ConfirmStop : ImportDeckIntent

    data object RetrySave : ImportDeckIntent

    /** 멈추고 덱 보기, 덱 보기. 저장한 카드까지만 남은 덱 상세로 간다. */
    data object ClickViewDeck : ImportDeckIntent

    data object ClickGoHome : ImportDeckIntent
}
