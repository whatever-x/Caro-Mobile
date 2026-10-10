package com.whatever.caro.core.importparser.table

/** 텍스트와 엑셀이 함께 쓰는 제목 행 판정과 항목 이름 규칙. */
internal object ColumnNames {
    /** 서버 파싱 응답과 같은 값이어야 하므로 화면 언어와 관계없이 고정한다. */
    private const val GENERATED_NAME_PREFIX = "열 "

    // 부분 문자열로 비교하면 word0001 같은 데이터 행이 제목 행으로 잘못 판정된다.
    private val TITLE_WORDS =
        setOf(
            "front",
            "back",
            "term",
            "definition",
            "question",
            "answer",
            "word",
            "meaning",
            "앞면",
            "뒷면",
            "단어",
            "뜻",
            "의미",
        )

    fun looksLikeTitleRow(values: List<String>): Boolean {
        val trimmed = values.map { value -> value.trim() }
        return trimmed.isNotEmpty() &&
            trimmed.all { value -> value.isNotEmpty() } &&
            values.toSet().size == values.size &&
            trimmed.any { value -> value.lowercase() in TITLE_WORDS }
    }

    /** 제목이 없거나, 비었거나, 겹치는 열은 `열 N`(1부터)으로 바꾼다. */
    fun fieldNames(
        header: List<String>?,
        width: Int,
    ): List<String> {
        val padded = List(maxOf(width, header?.size ?: 0)) { index -> header?.getOrNull(index).orEmpty() }
        val counts = padded.groupingBy { name -> name }.eachCount()
        return padded.mapIndexed { index, name ->
            if (name.isNotBlank() && counts[name] == 1) name else "$GENERATED_NAME_PREFIX${index + 1}"
        }
    }
}
