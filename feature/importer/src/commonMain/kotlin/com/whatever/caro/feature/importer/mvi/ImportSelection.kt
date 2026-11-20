package com.whatever.caro.feature.importer.mvi

import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

/** 미리보기를 만든 표의 읽기 범위다. [subdeckIds]는 Anki에서만, [sheetIndex]는 엑셀에서만 뜻이 있다. */
data class ImportSelection(
    val subdeckIds: ImmutableSet<String> = persistentSetOf(),
    val sheetIndex: Int = 0,
)
