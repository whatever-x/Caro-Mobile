package com.whatever.caro.feature.importer.transform

import com.whatever.caro.core.importparser.model.ParseWarning
import com.whatever.caro.core.model.card.CardContent
import kotlinx.collections.immutable.ImmutableList

/** 저장할 카드 한 장. [sourceIndex]는 `ParsedTable.rows` 안의 위치이고 원본 순서를 지키는 데 쓴다. */
data class ImportCard(
    val sourceIndex: Int,
    val front: String,
    val back: String,
    val frontReplaced: Boolean,
    val backReplaced: Boolean,
    val frontTruncated: Boolean,
    val backTruncated: Boolean,
    val mediaRemoved: Int,
)

internal fun ImportCard.toCardContent(): CardContent = CardContent(front = front, back = back)

data class ImportPreviewStats(
    val cardCount: Int,
    val excludedCount: Int,
    val emptyReplacedCount: Int,
    val truncatedCount: Int,
    val mediaRemovedCount: Int,
)

data class ImportPreview(
    val cards: ImmutableList<ImportCard>,
    val stats: ImportPreviewStats,
    val warnings: List<ParseWarning>,
    val limitExceeded: Boolean,
)
