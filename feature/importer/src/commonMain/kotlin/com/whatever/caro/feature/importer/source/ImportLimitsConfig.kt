package com.whatever.caro.feature.importer.source

import com.whatever.caro.core.model.importing.ImportLimits

/** 기본값은 정책 상수이고, 성능 측정 결과에 따라 낮춘 상한(5,000장)을 테스트가 주입할 수 있게 인자로 받는다. */
internal data class ImportLimitsConfig(
    val maxCards: Int = ImportLimits.MAX_CARDS,
    val maxFileBytes: Long = ImportLimits.MAX_FILE_BYTES,
    val maxPasteChars: Int = ImportLimits.MAX_PASTE_CHARS,
)
