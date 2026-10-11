package com.whatever.caro.feature.importer.source

import com.whatever.caro.core.importparser.ArchiveLimits
import com.whatever.caro.core.importparser.ParseOptions
import com.whatever.caro.core.importparser.SpreadsheetDateStyle
import com.whatever.caro.feature.importer.platform.platformLegacyKoreanDecoder
import okio.Path

// 저사양 기기의 앱 힙이 192~256MB 안팎이라, 서버용 기본값(항목 512MiB, 합계 1GiB)보다 낮춰 압축 폭탄이 힙과 캐시를 채우지 못하게 한다.
private val MOBILE_ARCHIVE_LIMITS = ArchiveLimits(maxEntryBytes = 192L * 1024 * 1024, maxTotalBytes = 256L * 1024 * 1024)

/**
 * 상한을 넘었는지 알아볼 수 있도록 maxRows는 장수 상한보다 1 크게 넣는다.
 * CP949 디코더가 빠지면 UTF-8이 아닌 한국어 파일이 인코딩 오류로 멈추므로 항상 넣는다.
 */
internal fun buildParseOptions(
    limits: ImportLimitsConfig,
    dateStyle: SpreadsheetDateStyle,
    subdeckIds: Set<String>? = null,
    sheetIndex: Int = 0,
    delimiter: Char? = null,
    hasHeaderRow: Boolean? = null,
    tempDirectory: Path = ImportTempStorage.DEFAULT_ROOT,
): ParseOptions =
    ParseOptions(
        maxRows = limits.maxCards + 1,
        maxFileBytes = limits.maxFileBytes,
        tempDirectory = tempDirectory,
        subdeckIds = subdeckIds,
        sheetIndex = sheetIndex,
        delimiter = delimiter,
        hasHeaderRow = hasHeaderRow,
        legacyKoreanDecoder = platformLegacyKoreanDecoder(),
        dateStyle = dateStyle,
        archiveLimits = MOBILE_ARCHIVE_LIMITS,
    )
