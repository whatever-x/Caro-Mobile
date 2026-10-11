package com.whatever.caro.core.importparser.model

import kotlinx.serialization.Serializable

/**
 * 파일 하나를 읽은 결과. 기기 파싱과 후속 서버 파싱 응답이 같은 모양을 쓴다.
 *
 * 값은 HTML 정리와 빈칸 변환 전의 원문이다. 정리, 변환, 잘림, 빈 면 대체는 항목 연결 뒤 기기에서 한다.
 */
@Serializable
data class ParsedTable(
    val format: ImportFormat,
    val sourceName: String,
    val htmlMode: HtmlMode,
    val subdecks: List<ParsedSubdeck> = emptyList(),
    val sheets: List<ParsedSheet> = emptyList(),
    val noteTypes: List<ParsedNoteType>,
    val rows: List<ParsedRow>,
    val warnings: List<ParseWarning> = emptyList(),
    val layout: ParsedLayout? = null,
    val rowLimitReached: Boolean = false,
)

@Serializable
enum class ImportFormat { APKG, TEXT, XLSX }

@Serializable
enum class HtmlMode { HTML, PLAIN }

/** 카드가 있는 노트가 1개 이상인 Anki 덱. 카드가 없는 노트는 어느 덱에도 넣지 않는다. */
@Serializable
data class ParsedSubdeck(
    val id: String,
    val path: String,
    val noteCount: Int,
) {
    companion object {
        /** 카드가 없는 노트 묶음을 선택할 때 쓰는 예약 id. */
        const val NO_DECK_ID = "none"
    }
}

@Serializable
data class ParsedSheet(
    val index: Int,
    val name: String,
    val rowCount: Int,
)

@Serializable
data class ParsedNoteType(
    val id: String,
    val name: String,
    val fieldNames: List<String>,
    val isCloze: Boolean,
    val noteCount: Int,
    val isImageOcclusion: Boolean = false,
    val selectedNoteCount: Int? = null,
)

@Serializable
data class ParsedRow(
    val noteTypeId: String,
    val subdeckId: String? = null,
    val fields: Map<String, String>,
)

@Serializable
data class ParseWarning(
    val code: String,
    val count: Int? = null,
    val detail: String? = null,
)

/**
 * 원본에서 실제로 쓴 구분자와 제목 행 판정. 연결 화면에서 바꿔 다시 파싱할 때 시작값이 된다.
 *
 * @property delimiter 구분자가 없는 XLSX는 null이다.
 * @property columnsFromHeader 항목 이름을 Anki 텍스트 머리말의 `#columns`에서 가져왔다. 이때 첫 줄은 제목 행이 아니다.
 */
@Serializable
data class ParsedLayout(
    val delimiter: String?,
    val hasHeaderRow: Boolean,
    val columnsFromHeader: Boolean = false,
)
