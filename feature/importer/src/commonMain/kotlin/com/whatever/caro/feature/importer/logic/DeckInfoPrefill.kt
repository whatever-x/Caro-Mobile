package com.whatever.caro.feature.importer.logic

import com.whatever.caro.core.importparser.model.ImportFormat
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.model.deck.DeckInputLimits
import com.whatever.caro.feature.importer.mvi.ImportSelection

internal const val DECK_PATH_SEPARATOR = "::"
private const val SHEET_SEPARATOR = " - "

/** 덱 설명 기본값 틀에서 파일 이름이 들어갈 자리다. 화면이 문구 리소스의 인자로 이 값을 넣어 틀을 만든다. */
internal const val FILE_NAME_PLACEHOLDER = "{file}"

/** 붙여넣기는 붙여 넣은 내용을 이름에 쓰지 않는다. 붙여넣기의 sourceName은 앱 언어 문구로 정해 넘어온 값이다. */
internal fun deckNamePrefill(
    table: ParsedTable,
    isPaste: Boolean,
    selection: ImportSelection,
): String {
    val name = if (isPaste) table.sourceName else fileDeckName(table, selection)
    return name.takeWithoutSplittingSurrogate(DeckInputLimits.NAME_MAX)
}

/**
 * 붙여넣기는 [text]를 그대로 쓰고, 파일은 [text]의 [FILE_NAME_PLACEHOLDER] 자리에 파일 이름을 넣는다.
 * 파일 이름을 줄이지 않으면 틀의 뒷부분이 설명 길이 제한에 잘린다.
 */
internal fun deckDescriptionPrefill(
    sourceName: String,
    isPaste: Boolean,
    text: String,
): String {
    val description = if (isPaste) text else text.replace(FILE_NAME_PLACEHOLDER, truncateFileName(sourceName))
    return description.takeWithoutSplittingSurrogate(DeckInputLimits.DESC_MAX)
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

/** 각 경로에서 마지막 이름을 뺀 부모 경로들의 공통 앞부분이다. 경로 전체의 공통 앞부분이면 단일 덱의 이름이 비어 버린다. */
internal fun commonParentPath(paths: List<String>): String {
    val parents = paths.map { path -> path.split(DECK_PATH_SEPARATOR).dropLast(1) }
    val first = parents.firstOrNull() ?: return ""
    val commonLength = first.indices.takeWhile { index -> parents.all { parent -> parent.getOrNull(index) == first[index] } }.size
    return first.take(commonLength).joinToString(DECK_PATH_SEPARATOR)
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
