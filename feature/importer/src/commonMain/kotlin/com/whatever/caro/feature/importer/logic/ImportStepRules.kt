package com.whatever.caro.feature.importer.logic

import androidx.compose.ui.state.ToggleableState
import com.whatever.caro.core.importparser.ImportParseErrorKind
import com.whatever.caro.core.importparser.ImportParseException
import com.whatever.caro.core.importparser.model.ImportFormat
import com.whatever.caro.core.importparser.model.ParsedSubdeck
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.mvi.ImportLoading
import kotlinx.collections.immutable.PersistentSet

/**
 * 읽기 중, 선택기가 열린 중, 덱 생성 중, 저장 중에는 뒤로 가기, 대화상자 닫기, 선택기 결과만 받는다.
 * 텍스트를 다시 나누는 중에는 구분자와 제목 행을 이어서 바꿀 수 있다. 앞선 읽기를 취소하고 마지막 선택만 남기기 위해서다.
 */
internal fun isIgnoredWhileBusy(
    state: ImportDeckState,
    intent: ImportDeckIntent,
): Boolean {
    if (!state.isBusy) return false
    return when (intent) {
        ImportDeckIntent.ClickBack, ImportDeckIntent.DismissDialog, is ImportDeckIntent.FilePicked -> false
        is ImportDeckIntent.SelectDelimiter, is ImportDeckIntent.ChangeHeaderRow -> state.loading != ImportLoading.TextReparsing
        else -> true
    }
}

/**
 * 텍스트는 노트 유형이 없을 때만 비었다고 본다. 노트 유형이 있으면 제목 행 때문에 행이 0이어도 텍스트 확인에서 고칠 수 있다.
 * Anki는 하위 덱도 덱 없음 노트도 없을 때, 엑셀은 모든 시트가 비었을 때다.
 */
internal fun isEmptyImport(table: ParsedTable): Boolean =
    when (table.format) {
        ImportFormat.APKG -> table.subdecks.isEmpty() && noDeckCount(table) == 0
        ImportFormat.XLSX -> table.sheets.all { sheet -> sheet.rowCount <= 0 }
        ImportFormat.TEXT -> table.noteTypes.isEmpty()
    }

/** 파서가 던진 오류만 종류를 가리고, 그 밖의 읽기 실패는 파일을 읽지 못했다는 문구가 나오는 종류로 보낸다. */
internal fun readFailureDialog(
    error: Exception,
    extension: String,
    isPaste: Boolean,
): ImportDialog =
    parseErrorDialog(
        kind = (error as? ImportParseException)?.kind ?: ImportParseErrorKind.ARCHIVE_LIMIT_EXCEEDED,
        extension = extension,
        isPaste = isPaste,
    )

/** 읽은 내용을 버린 채 처음 단계로 돌아가야 하는 대화상자. 다시 읽기에서 실패한 경우 표가 남아 있으므로 닫을 때 비운다. */
internal fun ImportDialog.isReadFailure(): Boolean =
    this is ImportDialog.FileTooLarge || this is ImportDialog.ParseError || this is ImportDialog.EmptyResult

/** 덱 없음 묶음은 카드 없는 노트가 있을 때만 고를 수 있다. */
internal fun isSelectableSubdeck(
    table: ParsedTable,
    id: String,
): Boolean = table.subdecks.any { subdeck -> subdeck.id == id } || (id == ParsedSubdeck.NO_DECK_ID && noDeckCount(table) > 0)

/** 전체가 선택된 상태에서는 하위 덱만 모두 풀고, 그 밖에는 모두 고른다. 덱 없음 선택은 바꾸지 않는다. */
internal fun toggleAllSubdecks(
    table: ParsedTable,
    selectedIds: PersistentSet<String>,
): PersistentSet<String> {
    val subdeckIds = table.subdecks.map { subdeck -> subdeck.id }
    return if (selectAllState(table, selectedIds) ==
        ToggleableState.On
    ) {
        selectedIds.removeAll(subdeckIds)
    } else {
        selectedIds.addAll(subdeckIds)
    }
}

/** 빈 시트는 고를 수는 있지만 [다음]으로 넘어갈 수 없다. */
internal fun isSheetEmpty(
    table: ParsedTable,
    sheetIndex: Int,
): Boolean = table.sheets.firstOrNull { sheet -> sheet.index == sheetIndex }?.let { sheet -> sheet.rowCount <= 0 } ?: false
