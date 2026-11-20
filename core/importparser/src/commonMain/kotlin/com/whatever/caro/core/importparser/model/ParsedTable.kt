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

/**
 * 노트가 1개 이상 속한 Anki 덱.
 *
 * @property noteCount 노트 유형을 아는 노트만 세므로 유형별 [ParsedNoteType.noteCount]의 합과 같다.
 * @property imageOcclusionNoteCount [noteCount] 가운데 Image Occlusion 노트 수. 행으로 옮기지 않으므로 가져올 노트 수를 셀 때 [noteCount]에서 뺀다.
 */
@Serializable
data class ParsedSubdeck(
    val id: String,
    val path: String,
    val noteCount: Int,
    val imageOcclusionNoteCount: Int = 0,
)

/** @property rowCount 값 요소가 있는 행 수다. 시트를 읽지 않고 세므로 빈 문자열 값은 가려내지 못해, 읽었을 때의 행 수보다 클 수 있다. */
@Serializable
data class ParsedSheet(
    val index: Int,
    val name: String,
    val rowCount: Int,
)

/**
 * @property noteCount APKG는 덱에 속한 노트 수이고 선택 범위로 좁히지 않는다. 이 값이 0인 유형은 목록에 넣지 않는다.
 * @property selectedCardCount 선택 범위 안 노트의 Anki 카드 수. 한 노트의 카드가 여러 덱에 있어도 모두 노트가 속한 덱 쪽으로 센다.
 */
@Serializable
data class ParsedNoteType(
    val id: String,
    val name: String,
    val fieldNames: List<String>,
    val isCloze: Boolean,
    val noteCount: Int,
    val isImageOcclusion: Boolean = false,
    val selectedNoteCount: Int? = null,
    val selectedCardCount: Int? = null,
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
 * @property singleFieldRowCount 보관한 데이터 행 가운데 Anki 텍스트 머리말의 메타 열을 빼면 칸이 하나뿐이고 그 칸에 글자가 있는 행 수. 구분자로 나누지 않는 XLSX는 0이다.
 */
@Serializable
data class ParsedLayout(
    val delimiter: String?,
    val hasHeaderRow: Boolean,
    val columnsFromHeader: Boolean = false,
    val singleFieldRowCount: Int = 0,
)
