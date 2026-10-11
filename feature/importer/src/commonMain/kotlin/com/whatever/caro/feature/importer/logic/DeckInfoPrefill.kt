package com.whatever.caro.feature.importer.logic

import com.whatever.caro.core.importparser.model.ImportFormat
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.model.deck.DeckInputLimits
import com.whatever.caro.feature.importer.mvi.ImportSelection

private const val FILE_PLACEHOLDER = "{file}"
private const val SHEET_SEPARATOR = " - "

internal data class DeckInfoPrefill(
    val name: String,
    val description: String,
)

/**
 * 붙여넣기는 이름과 설명 어디에도 붙여 넣은 내용을 쓰지 않는다. sourceName은 앱 언어 문구로 정해 넘어온 값이다.
 * [fileDescriptionTemplate]의 `{file}` 자리에는 확장자를 포함한 파일 이름이 들어간다.
 */
internal fun deckInfoPrefill(
    table: ParsedTable,
    isPaste: Boolean,
    selection: ImportSelection,
    fileDescriptionTemplate: String,
    pasteDescription: String,
): DeckInfoPrefill {
    val name = if (isPaste) table.sourceName else fileDeckName(table, selection)
    val description = if (isPaste) pasteDescription else fileDescriptionTemplate.replace(FILE_PLACEHOLDER, table.sourceName)
    return DeckInfoPrefill(
        name = name.takeWithoutSplittingSurrogate(DeckInputLimits.NAME_MAX),
        description = description.takeWithoutSplittingSurrogate(DeckInputLimits.DESC_MAX),
    )
}

private fun fileDeckName(
    table: ParsedTable,
    selection: ImportSelection,
): String {
    val baseName = fileNameWithoutExtension(table.sourceName)
    return when (table.format) {
        ImportFormat.APKG -> ankiDeckName(table, selection) ?: baseName
        ImportFormat.XLSX -> sheetDeckName(table, selection, baseName)
        ImportFormat.TEXT -> baseName
    }
}

/**
 * 공통 상위 덱의 마지막 이름, 없으면 고른 덱의 최상위 덱이 하나일 때 그 이름이다.
 * 전체 경로를 쓰면 덱 이름에 Anki의 경로 구분자가 들어가므로 마지막 이름만 쓴다.
 * 덱 없음 묶음은 덱 경로가 없어 고른 덱에 들어가지 않는다.
 */
private fun ankiDeckName(
    table: ParsedTable,
    selection: ImportSelection,
): String? {
    val paths = table.subdecks.filter { subdeck -> subdeck.id in selection.subdeckIds }.map { subdeck -> subdeck.path }
    val commonParent = commonParentPath(paths)
    if (commonParent.isNotEmpty()) return commonParent.substringAfterLast(DECK_PATH_SEPARATOR)
    return paths.map { path -> path.substringBefore(DECK_PATH_SEPARATOR) }.distinct().singleOrNull()
}

private fun sheetDeckName(
    table: ParsedTable,
    selection: ImportSelection,
    baseName: String,
): String {
    if (table.sheets.size <= 1) return baseName
    val sheet = table.sheets.firstOrNull { candidate -> candidate.index == selection.sheetIndex } ?: return baseName
    return baseName + SHEET_SEPARATOR + sheet.name
}
