package com.whatever.caro.core.importparser.text

/**
 * Anki 텍스트 내보내기 파일 맨 앞의 `#key:value` 헤더.
 *
 * @property metaColumns 항목에서 뺄 메타데이터 열의 0부터 시작하는 번호.
 */
internal data class AnkiTextHeader(
    val separator: Char? = null,
    val html: Boolean? = null,
    val columns: String? = null,
    val metaColumns: Set<Int> = emptySet(),
) {
    companion object {
        private val META_COLUMN_KEYS = setOf("notetype column", "deck column", "tags column", "guid column")
        private val IGNORED_KEYS = setOf("tags", "notetype", "deck")
        private val SEPARATOR_NAMES =
            mapOf(
                "comma" to ',',
                "semicolon" to ';',
                "tab" to '\t',
                "space" to ' ',
                "pipe" to '|',
                "colon" to ':',
            )

        /** 헤더와 본문이 시작하는 위치를 돌려준다. 알려진 키가 아닌 줄을 만나면 그 줄부터 본문이다. */
        fun parse(text: String): Pair<AnkiTextHeader, Int> {
            var header = AnkiTextHeader()
            var offset = 0
            while (offset < text.length && text[offset] == '#') {
                val lineEnd = text.indexOf('\n', offset).let { index -> if (index < 0) text.length else index }
                header = header.withLine(text.substring(offset + 1, lineEnd).trimEnd('\r')) ?: break
                offset = minOf(text.length, lineEnd + 1)
            }
            return header to offset
        }

        private fun AnkiTextHeader.withLine(line: String): AnkiTextHeader? {
            val key = line.substringBefore(':', missingDelimiterValue = "").trim().lowercase()
            val value = line.substringAfter(':', missingDelimiterValue = "")
            return when (key) {
                "separator" -> copy(separator = separatorOf(value))
                "html" -> copy(html = value.trim().lowercase() == "true")
                "columns" -> copy(columns = value)
                in META_COLUMN_KEYS -> value.trim().toIntOrNull()?.let { column -> copy(metaColumns = metaColumns + (column - 1)) } ?: this
                in IGNORED_KEYS -> this
                else -> null
            }
        }

        private fun separatorOf(value: String): Char? = SEPARATOR_NAMES[value.trim().lowercase()] ?: value.singleOrNull()
    }
}
