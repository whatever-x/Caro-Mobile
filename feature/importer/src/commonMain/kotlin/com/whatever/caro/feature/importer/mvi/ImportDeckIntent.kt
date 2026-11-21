package com.whatever.caro.feature.importer.mvi

import com.whatever.caro.core.importparser.SpreadsheetDateStyle
import com.whatever.caro.core.viewmodel.contract.UiIntent
import com.whatever.caro.feature.importer.platform.PickResult
import com.whatever.caro.feature.importer.transform.ImportCard

/**
 * 문구와 앱 언어는 Route가 리소스에서 읽어 넘기고 ViewModel은 평문만 받는다.
 * 처리 중이거나 대화상자, 카드 전체 보기가 떠 있으면 [isAcceptedWhileBusy]인 입력만 받고 나머지는 버린다.
 */
sealed interface ImportDeckIntent : UiIntent {
    /**
     * 화면은 상태를 따지지 않고 뒤로 가기를 이 입력으로 보낸다. 대화상자, 카드 전체 보기, 덱 정보 시트 순으로 맨 위의 것 하나만 닫고, 닫을 수 없는 대화상자는 닫지 않는다.
     * 읽는 중이면 읽기를 멈춘다. 덱을 만드는 중이면 무시하고, 저장 중에는 바로 멈추지 않고 멈춤 확인을 연다.
     */
    data object ClickBack : ImportDeckIntent

    /**
     * 버튼이 하나인 대화상자의 [확인], 처음으로 돌아갈지 묻는 대화상자와 덱 만들기 실패의 [취소], 멈춤 확인의 [계속 가져오기], 바깥 누름이 모두 이 입력이다.
     * 닫을 수 없는 대화상자에서는 무시하므로 그 대화상자의 버튼에 이 입력을 쓰면 눌러도 아무 일도 일어나지 않는다.
     */
    data object DismissDialog : ImportDeckIntent

    /** 처음으로 돌아갈지 묻는 대화상자의 [돌아가기]. */
    data object ConfirmDiscard : ImportDeckIntent

    data object ClickPickFile : ImportDeckIntent

    data object ClickPaste : ImportDeckIntent

    /**
     * [emptySideText]는 빈 면에 넣을 앱 언어 문구다. 읽은 뒤 바로 미리보기를 만들므로 읽기를 시작하는 입력이 함께 넘긴다.
     * [descriptionTemplate]은 덱 설명 기본값이고 파일 이름 자리에 `FILE_NAME_PLACEHOLDER`를 둔다.
     */
    data class FilePicked(
        val result: PickResult,
        val dateStyle: SpreadsheetDateStyle,
        val emptySideText: String,
        val descriptionTemplate: String,
    ) : ImportDeckIntent

    data class UpdatePasteText(
        val text: String,
    ) : ImportDeckIntent

    /** [sourceName]은 덱 이름 기본값, [description]은 덱 설명 기본값으로도 쓴다. */
    data class ClickPasteNext(
        val sourceName: String,
        val dateStyle: SpreadsheetDateStyle,
        val emptySideText: String,
        val description: String,
    ) : ImportDeckIntent

    data class SelectPreviewFilter(
        val filter: ImportPreviewFilter,
    ) : ImportDeckIntent

    data class ClickCard(
        val card: ImportCard,
    ) : ImportDeckIntent

    data object CloseCardDetail : ImportDeckIntent

    data object DismissDelimiterBanner : ImportDeckIntent

    /** 설정 박스 줄의 [변경]. 설정 확인 띠의 [구분 기준 변경]은 [SettingsKind.Delimiter]로 보낸다. */
    data class ClickChangeSetting(
        val kind: SettingsKind,
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

    data class ChangeHeaderRow(
        val hasHeaderRow: Boolean,
    ) : ImportDeckIntent

    data class ToggleNoteType(
        val noteTypeId: String,
    ) : ImportDeckIntent

    /** [target]은 `FieldMapping.FRONT` 또는 `FieldMapping.BACK`이다. 항목 고르기 시트에서 고른 항목도 이 입력이다. */
    data class ToggleField(
        val noteTypeId: String,
        val target: String,
        val fieldName: String,
    ) : ImportDeckIntent

    /** 설정 화면의 [적용]과 먼저 고칠 것 화면의 [미리보기]. */
    data object ClickApplySettings : ImportDeckIntent

    /** 미리보기 설정 박스의 덱 이름 줄 [변경]. 덱 정보 시트를 연다. */
    data object ClickChangeDeckInfo : ImportDeckIntent

    data class UpdateDeckInfoName(
        val name: String,
    ) : ImportDeckIntent

    data class UpdateDeckInfoDescription(
        val description: String,
    ) : ImportDeckIntent

    /** 덱 정보 시트의 [적용]. */
    data object ApplyDeckInfo : ImportDeckIntent

    /** 덱 정보 시트의 바깥 누름, 끌어내림, 시트 창의 뒤로 가기. 바꾼 내용 없이 닫는다. */
    data object DismissDeckInfo : ImportDeckIntent

    /** 미리보기 하단의 [가져오기]. 덱을 만들고 저장을 시작한다. */
    data object ClickImport : ImportDeckIntent

    /** 덱 만들기 실패 대화상자의 [다시 시도]. */
    data object RetryCreateDeck : ImportDeckIntent

    /** 저장 중 화면의 [가져오기 멈추기]. 뒤로 가기와 같은 멈춤 확인을 연다. */
    data object ClickStop : ImportDeckIntent

    /** 멈춤 확인 대화상자의 [멈추기]. [계속 가져오기]는 [DismissDialog]다. */
    data object ConfirmStop : ImportDeckIntent

    /** 저장이 멈춘 대화상자의 [다시 시도]. */
    data object RetrySave : ImportDeckIntent

    /** [멈추고 덱 보기]와 [덱 보기]. 저장한 카드까지만 남은 덱으로 간다. */
    data object ClickViewDeck : ImportDeckIntent

    /** 덱을 찾을 수 없음 대화상자의 [홈으로]. */
    data object ClickGoHome : ImportDeckIntent
}

/** 뒤로 가기, 닫기, 선택기 결과와 대화상자의 버튼이다. 버튼 입력은 처리하는 쪽이 그 대화상자가 떠 있는지 다시 확인해야 연달아 누른 입력이 두 번 처리되지 않는다. */
internal val ImportDeckIntent.isAcceptedWhileBusy: Boolean
    get() =
        when (this) {
            ImportDeckIntent.ClickBack,
            ImportDeckIntent.DismissDialog,
            ImportDeckIntent.ConfirmDiscard,
            ImportDeckIntent.CloseCardDetail,
            is ImportDeckIntent.FilePicked,
            ImportDeckIntent.RetryCreateDeck,
            ImportDeckIntent.ConfirmStop,
            ImportDeckIntent.RetrySave,
            ImportDeckIntent.ClickViewDeck,
            ImportDeckIntent.ClickGoHome,
            -> true

            else -> false
        }
