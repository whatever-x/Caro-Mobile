package com.whatever.caro.core.importparser

import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import okio.Path

/**
 * 파싱 한 번의 설정. 앱 정책 값(장수, 파일 크기)은 호출자가 넣어 이 모듈이 앱 모델에 의존하지 않게 한다.
 *
 * @property maxRows 보관할 데이터 행 수. 상한을 넘었는지 알 수 있도록 앱 상한보다 1 크게 넣는다.
 * @property tempDirectory 압축을 푼 Anki 컬렉션을 둘 디렉터리. SQLite가 경로로 열어야 하므로 실제 파일 시스템이어야 한다.
 * @property subdeckIds 읽을 Anki 하위 덱 id. null이면 카드가 있는 하위 덱 전체이고 카드가 없는 노트는 빠진다.
 * @property sqliteDriver 기기와 서버가 같은 SQLite 버전으로 WAL, 콜레이션 처리를 같게 하려고 기본값을 번들 드라이버로 둔다.
 */
data class ParseOptions(
    val maxRows: Int,
    val maxFileBytes: Long,
    val tempDirectory: Path,
    val archiveLimits: ArchiveLimits = ArchiveLimits(),
    val verifyIntegrity: Boolean = false,
    val subdeckIds: Set<String>? = null,
    val sheetIndex: Int = 0,
    val delimiter: Char? = null,
    val hasHeaderRow: Boolean? = null,
    val legacyKoreanDecoder: LegacyKoreanDecoder? = null,
    val sqliteDriver: SQLiteDriver = BundledSQLiteDriver(),
    val dateStyle: SpreadsheetDateStyle = SpreadsheetDateStyle.YEAR_MONTH_DAY,
    val maxColumns: Int = DEFAULT_MAX_COLUMNS,
    val maxFieldChars: Int = DEFAULT_MAX_FIELD_CHARS,
    val maxCells: Int = DEFAULT_MAX_CELLS,
    val maxTotalChars: Long = DEFAULT_MAX_TOTAL_CHARS,
) {
    init {
        require(maxRows > 0) { "maxRows must be positive: $maxRows" }
        require(maxFileBytes > 0) { "maxFileBytes must be positive: $maxFileBytes" }
        require(sheetIndex >= 0) { "sheetIndex must not be negative: $sheetIndex" }
        require(maxColumns > 0) { "maxColumns must be positive: $maxColumns" }
        require(maxFieldChars > 0) { "maxFieldChars must be positive: $maxFieldChars" }
        require(maxCells > 0) { "maxCells must be positive: $maxCells" }
        require(maxTotalChars > 0) { "maxTotalChars must be positive: $maxTotalChars" }
    }

    companion object {
        const val DEFAULT_MAX_COLUMNS = 1_000
        const val DEFAULT_MAX_FIELD_CHARS = 1_000_000

        /** 저사양 기기 메모리를 지키려고 표 하나가 만들 행 값 칸 수를 제한한다. 10,001행이면 행마다 약 25칸까지다. */
        const val DEFAULT_MAX_CELLS = 250_000

        /** 값 하나는 [DEFAULT_MAX_FIELD_CHARS]로 막혀도 값 수만큼 곱해지므로, 표 하나가 보관할 글자 수의 합계를 따로 제한한다. */
        const val DEFAULT_MAX_TOTAL_CHARS = 10_000_000L
    }
}

/** 압축 파일 안전 상한. 헤더의 크기 값은 믿지 않고 실제로 풀어낸 바이트로 판정한다. */
data class ArchiveLimits(
    val maxEntries: Int = DEFAULT_MAX_ENTRIES,
    val maxEntryBytes: Long = DEFAULT_MAX_ENTRY_BYTES,
    val maxCompressionRatio: Long = DEFAULT_MAX_COMPRESSION_RATIO,
    val maxTotalBytes: Long = DEFAULT_MAX_TOTAL_BYTES,
    val maxCentralDirectoryBytes: Long = DEFAULT_MAX_CENTRAL_DIRECTORY_BYTES,
) {
    init {
        require(maxEntries > 0) { "maxEntries must be positive: $maxEntries" }
        require(maxEntryBytes > 0) { "maxEntryBytes must be positive: $maxEntryBytes" }
        require(maxCompressionRatio > 0) { "maxCompressionRatio must be positive: $maxCompressionRatio" }
        require(maxCentralDirectoryBytes > 0) { "maxCentralDirectoryBytes must be positive: $maxCentralDirectoryBytes" }
    }

    companion object {
        const val DEFAULT_MAX_ENTRIES = 100_000
        const val DEFAULT_MAX_ENTRY_BYTES = 512L * 1024 * 1024
        const val DEFAULT_MAX_COMPRESSION_RATIO = 100L
        const val DEFAULT_MAX_TOTAL_BYTES = 1024L * 1024 * 1024

        /** Okio가 중앙 디렉터리를 통째로 메모리에 올리므로 항목 수와 별개로 바이트 크기도 막는다. 미디어가 많은 Anki 패키지도 이 안에 든다. */
        const val DEFAULT_MAX_CENTRAL_DIRECTORY_BYTES = 8L * 1024 * 1024
    }
}

/** 엑셀 기본 날짜 서식은 기기 지역 설정을 따르므로 앱 언어로 고정한 표기를 고른다. */
enum class SpreadsheetDateStyle {
    /** yyyy-MM-dd */
    YEAR_MONTH_DAY,

    /** M/d/yyyy */
    MONTH_DAY_YEAR,
}
