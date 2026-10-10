package com.whatever.caro.core.importparser.model

/** Backend 설계의 공통 경고 코드 표가 정본이다. 새 코드는 그 표에 먼저 추가한다. */
object ParseWarningCode {
    const val ENCODING_FALLBACK_CP949 = "ENCODING_FALLBACK_CP949"
    const val TEXT_INCONSISTENT_COLUMNS = "TEXT_INCONSISTENT_COLUMNS"
    const val XLSX_FORMULA_WITHOUT_VALUE = "XLSX_FORMULA_WITHOUT_VALUE"
    const val XLSX_NUMBER_FORMAT_RAW = "XLSX_NUMBER_FORMAT_RAW"
    const val APKG_FIELD_COUNT_MISMATCH = "APKG_FIELD_COUNT_MISMATCH"
    const val APKG_IMAGE_OCCLUSION_EXCLUDED = "APKG_IMAGE_OCCLUSION_EXCLUDED"
}

internal fun countWarning(
    code: String,
    count: Int,
): ParseWarning? = if (count > 0) ParseWarning(code = code, count = count) else null
