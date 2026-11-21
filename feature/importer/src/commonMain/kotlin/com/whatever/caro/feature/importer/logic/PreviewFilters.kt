package com.whatever.caro.feature.importer.logic

import com.whatever.caro.feature.importer.mvi.ImportPreviewFilter
import com.whatever.caro.feature.importer.transform.ImportCard
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap

/** 미디어 누락 칩도 카드 수다. 지운 참조 수(`ImportPreviewStats.mediaRemovedCount`)는 한 카드를 여러 번 센다. */
internal fun previewFilterCounts(cards: List<ImportCard>): ImmutableMap<ImportPreviewFilter, Int> =
    ImportPreviewFilter.entries.associateWith { filter -> cards.count { card -> card.matches(filter) } }.toImmutableMap()

internal fun filterCards(
    cards: ImmutableList<ImportCard>,
    filter: ImportPreviewFilter,
): ImmutableList<ImportCard> {
    if (filter == ImportPreviewFilter.All) return cards
    return cards.filter { card -> card.matches(filter) }.toImmutableList()
}

private fun ImportCard.matches(filter: ImportPreviewFilter): Boolean =
    when (filter) {
        ImportPreviewFilter.All -> true
        ImportPreviewFilter.EmptyReplaced -> frontReplaced || backReplaced
        ImportPreviewFilter.Truncated -> frontTruncated || backTruncated
        ImportPreviewFilter.MediaRemoved -> mediaRemoved > 0
    }
