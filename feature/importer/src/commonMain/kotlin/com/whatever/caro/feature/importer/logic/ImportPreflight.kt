package com.whatever.caro.feature.importer.logic

import com.whatever.caro.core.importparser.model.ImportFormat
import com.whatever.caro.core.importparser.model.ParsedSheet
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.model.importing.ImportLimits

/** 첫 읽기 뒤 미리보기 전에 먼저 처리할 일이다. [Ok]면 카드를 만들어 [noCardsKind]로 이어 판정한다. */
internal sealed interface ImportPreflight {
    data object Ok : ImportPreflight

    /** 읽은 시트에 데이터 행이 없다. 호출한 쪽이 [candidates]를 차례로 다시 읽어 데이터 행이 있는 첫 시트를 고른다. */
    data class PickSheet(
        val candidates: List<ParsedSheet>,
    ) : ImportPreflight

    data object SubdeckFirst : ImportPreflight

    data object SheetFirst : ImportPreflight

    data object TooMany : ImportPreflight
}

/**
 * [sheetIndex]는 [table]로 읽은 시트다. 후보 시트를 다시 읽은 결과도 같은 규칙으로 판정하도록 받는다.
 * 모든 시트가 비면 [ImportPreflight.Ok]다. 앱 안에서 고칠 곳이 없어 카드 0장 판정으로 넘긴다.
 */
internal fun preflight(
    table: ParsedTable,
    maxCards: Int = ImportLimits.MAX_CARDS,
    sheetIndex: Int = 0,
): ImportPreflight =
    when (table.format) {
        ImportFormat.APKG -> subdeckPreflight(table, maxCards)
        ImportFormat.TEXT -> if (isOverLimit(table, maxCards)) ImportPreflight.TooMany else ImportPreflight.Ok
        ImportFormat.XLSX -> sheetPreflight(table, maxCards, sheetIndex)
    }

/**
 * 시트 목록의 상한 초과 표시도 같은 기준을 쓴다. [ParsedSheet.rowCount]는 제목 행을 포함한 값 있는 행 수이므로 상한에 1을 더한 값까지 후보다.
 * 이 행 수는 시트를 읽지 않고 센 값이라 후보를 읽으면 0행일 수 있다. 그때 다음 후보로 넘어가는 것은 호출한 쪽이 한다.
 */
internal fun isCandidateSheet(
    sheet: ParsedSheet,
    maxCards: Int = ImportLimits.MAX_CARDS,
): Boolean = sheet.rowCount in 1..maxCards + 1

private fun sheetPreflight(
    table: ParsedTable,
    maxCards: Int,
    sheetIndex: Int,
): ImportPreflight {
    val others = table.sheets.filter { sheet -> sheet.index != sheetIndex }
    val candidates = others.filter { sheet -> isCandidateSheet(sheet, maxCards) }
    return when {
        isOverLimit(table, maxCards) -> if (candidates.isEmpty()) ImportPreflight.TooMany else ImportPreflight.SheetFirst
        table.rows.isNotEmpty() -> ImportPreflight.Ok
        candidates.isNotEmpty() -> ImportPreflight.PickSheet(candidates)
        others.any { sheet -> sheet.rowCount > 0 } -> ImportPreflight.TooMany
        else -> ImportPreflight.Ok
    }
}

/** 혼자서 상한 안에 드는 하위 덱이 없으면 하위 덱을 골라도 상한을 맞출 수 없으므로 [ImportPreflight.TooMany]다. */
private fun subdeckPreflight(
    table: ParsedTable,
    maxCards: Int,
): ImportPreflight {
    val counts = table.subdecks.map(::subdeckCardCount)
    return when {
        counts.sum() <= maxCards -> ImportPreflight.Ok
        counts.any { count -> count in 1..maxCards } -> ImportPreflight.SubdeckFirst
        else -> ImportPreflight.TooMany
    }
}

/** 파서가 행 상한에서 읽기를 멈췄으면 읽지 않은 행이 남아 있으므로 보관한 행 수와 상관없이 넘은 것으로 본다. */
private fun isOverLimit(
    table: ParsedTable,
    maxCards: Int,
): Boolean = table.rowLimitReached || table.rows.size > maxCards
