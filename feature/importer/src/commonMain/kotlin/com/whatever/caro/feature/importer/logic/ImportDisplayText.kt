package com.whatever.caro.feature.importer.logic

private const val ELLIPSIS = "…"
private const val FILE_NAME_MAX = 120
private const val CELL_MAX = 200
private const val CHIP_MAX = 80
private const val PATH_MAX = 200
private const val SHEET_NAME_MAX = 100

/** 이름 부분만 줄이고 확장자는 남긴다. 전체 길이가 한도를 넘지 않는다. */
internal fun truncateFileName(fileName: String): String {
    if (fileName.length <= FILE_NAME_MAX) return fileName
    val extensionStart = extensionStart(fileName)
    val extension = fileName.substring(extensionStart)
    // 확장자만으로 한도를 채우면 이름 부분이 들어갈 자리가 없다.
    if (extension.length >= FILE_NAME_MAX) return truncate(fileName, FILE_NAME_MAX)
    return truncate(fileName.substring(0, extensionStart), FILE_NAME_MAX - extension.length) + extension
}

internal fun truncateCell(text: String): String = truncate(text, CELL_MAX)

internal fun truncateChip(text: String): String = truncate(text, CHIP_MAX)

internal fun truncatePath(text: String): String = truncate(text, PATH_MAX)

internal fun truncateSheetName(text: String): String = truncate(text, SHEET_NAME_MAX)

internal fun fileNameWithoutExtension(fileName: String): String = fileName.substring(0, extensionStart(fileName))

/** UTF-16 코드 단위 기준이다. 서로게이트 쌍의 앞 절반만 남으면 글자가 깨지므로 한 칸 앞에서 자른다. */
internal fun String.takeWithoutSplittingSurrogate(limit: Int): String {
    if (length <= limit) return this
    val end = if (limit > 0 && this[limit - 1].isHighSurrogate()) limit - 1 else limit
    return substring(0, end)
}

private fun truncate(
    text: String,
    limit: Int,
): String = if (text.length <= limit) text else text.takeWithoutSplittingSurrogate(limit - 1) + ELLIPSIS

/** 맨 앞 점은 확장자 구분이 아니라 숨김 파일 표기라 이름으로 본다. */
private fun extensionStart(fileName: String): Int = fileName.lastIndexOf('.').takeIf { index -> index > 0 } ?: fileName.length
