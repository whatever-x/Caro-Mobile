package com.whatever.caro.feature.importer.logic

import com.whatever.caro.feature.importer.mvi.ImportPreviewFilter
import com.whatever.caro.feature.importer.support.PreviewSamples
import com.whatever.caro.feature.importer.transform.defaultFieldMappings
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PreviewTransitionsTest : FunSpec() {
    init {
        test("설정을 적용해 미리보기에 다시 들어가면 필터는 전체로 돌리고 카드 전체 보기는 닫는다") {
            val table = PreviewSamples.ankiTable
            val preview = PreviewSamples.ankiPreview
            val before =
                PreviewSamples.state(
                    table,
                    preview,
                    filter = ImportPreviewFilter.Truncated,
                    detailCard = PreviewSamples.ankiCards.first(),
                )

            val shown = before.showingPreview(table, before.selection, defaultFieldMappings(table), preview, before.deckName)

            shown.previewFilter shouldBe ImportPreviewFilter.All
            shown.filteredCards shouldBe preview.cards
            shown.detailCard shouldBe null
        }
    }
}
