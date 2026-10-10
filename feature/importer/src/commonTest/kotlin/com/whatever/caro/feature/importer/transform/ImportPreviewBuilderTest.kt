package com.whatever.caro.feature.importer.transform

import com.whatever.caro.core.importparser.model.HtmlMode
import com.whatever.caro.core.importparser.model.ImportFormat
import com.whatever.caro.core.importparser.model.ParseWarning
import com.whatever.caro.core.importparser.model.ParsedNoteType
import com.whatever.caro.core.importparser.model.ParsedRow
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.model.importing.FieldMapping
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ImportPreviewBuilderTest : FunSpec() {
    private val builder = ImportPreviewBuilder()

    init {
        test("기본 연결은 첫 항목을 앞면, 나머지를 고른 순서대로 줄바꿈으로 합쳐 뒷면에 넣고 빈 항목은 건너뛴다") {
            val table = textTable(listOf("a", "b", "c"), listOf("apple", "", "사과"))

            val preview = builder.build(table, defaultFieldMappings(table), EMPTY)

            preview.cards.single().front shouldBe "apple"
            preview.cards.single().back shouldBe "사과"
        }

        test("사용자가 고른 순서대로 합치고 연결하지 않은 항목은 넣지 않는다") {
            val table = textTable(listOf("a", "b", "c"), listOf("apple", "fruit", "사과"))
            val mapping = FieldMapping("text", mapOf(FieldMapping.FRONT to listOf("c", "a"), FieldMapping.BACK to emptyList()))

            val card = builder.build(table, mapOf("text" to mapping), EMPTY).cards.single()

            card.front shouldBe "사과\napple"
            card.back shouldBe EMPTY
            card.backReplaced shouldBe true
        }

        test("양쪽이 모두 비면 제외하고 그 카드에서 지운 미디어는 세지 않는다") {
            val table = textTable(listOf("a", "b"), listOf("[sound:x.mp3]", " "), listOf("앞", "[sound:y.mp3]뒤"))

            val preview = builder.build(table, defaultFieldMappings(table), EMPTY)

            preview.stats shouldBe
                ImportPreviewStats(cardCount = 1, excludedCount = 1, emptyReplacedCount = 0, truncatedCount = 0, mediaRemovedCount = 1)
            preview.cards.single().sourceIndex shouldBe 1
        }

        test("한 면만 비면 빈 면 문구로 채우고 대체 수를 센다") {
            val table = textTable(listOf("a", "b"), listOf("", "뒤"))

            val card = builder.build(table, defaultFieldMappings(table), EMPTY).cards.single()

            card.front shouldBe EMPTY
            card.frontReplaced shouldBe true
        }

        test("500자를 넘는 면은 500에서 자르고 서로게이트 쌍 가운데면 한 칸 앞에서 자른다") {
            val pair = "😀"
            val long = "a".repeat(499) + pair + "b"
            val table = textTable(listOf("a", "b"), listOf(long, "x".repeat(501)))

            val card = builder.build(table, defaultFieldMappings(table), EMPTY).cards.single()

            card.front shouldBe "a".repeat(499)
            card.frontTruncated shouldBe true
            card.back.length shouldBe 500
            card.backTruncated shouldBe true
        }

        test("제외한 노트 유형의 행은 카드도 제외 수도 만들지 않는다") {
            val table = textTable(listOf("a", "b"), listOf("앞", "뒤"))
            val mapping = defaultFieldMappings(table).getValue("text").copy(excluded = true)

            val preview = builder.build(table, mapOf("text" to mapping), EMPTY)

            preview.cards shouldBe emptyList()
            preview.stats.excludedCount shouldBe 0
        }

        test("텍스트는 앞면 값에 빈칸 표기가 있는 행만 변환하고 뒷면에만 있는 표기는 그대로 둔다") {
            val table = textTable(listOf("a", "b"), listOf("{{c1::서울}}은 수도", "추가"), listOf("일반", "{{c1::뒷면}}"))

            val cards = builder.build(table, defaultFieldMappings(table), EMPTY).cards

            cards[0].front shouldBe "[...]은 수도"
            cards[0].back shouldBe "서울은 수도\n추가"
            cards[1].back shouldBe "{{c1::뒷면}}"
        }

        test("빈칸 문장 안의 미디어는 노트당 한 번만 센다") {
            val table = textTable(listOf("a", "b"), listOf("{{c1::물}}<img src=\"a.png\">", "설명"), htmlMode = HtmlMode.HTML)

            builder
                .build(table, defaultFieldMappings(table), EMPTY)
                .cards
                .single()
                .mediaRemoved shouldBe 1
        }

        test("가져올 카드 수가 상한을 넘어도 카드와 통계를 만들고 상한 초과만 표시한다") {
            val rows = List(4) { index -> listOf("앞$index", "뒤$index") }
            val table = textTable(listOf("a", "b"), *rows.toTypedArray())

            val preview = builder.build(table, defaultFieldMappings(table), EMPTY, maxCards = 3)

            preview.stats.cardCount shouldBe 4
            preview.limitExceeded shouldBe true
            builder.build(table, defaultFieldMappings(table), EMPTY, maxCards = 4).limitExceeded shouldBe false
        }

        test("파싱 경고를 미리보기 경고로 그대로 넘긴다") {
            val warning = ParseWarning("XLSX_NUMBER_FORMAT_RAW", count = 2)
            val table = textTable(listOf("a", "b"), listOf("앞", "뒤")).copy(warnings = listOf(warning))

            builder.build(table, defaultFieldMappings(table), EMPTY).warnings shouldBe listOf(warning)
        }
    }

    private fun textTable(
        fieldNames: List<String>,
        vararg rows: List<String>,
        htmlMode: HtmlMode = HtmlMode.PLAIN,
    ) = ParsedTable(
        format = ImportFormat.TEXT,
        sourceName = "words.txt",
        htmlMode = htmlMode,
        noteTypes = listOf(ParsedNoteType("text", "words.txt", fieldNames, isCloze = false, noteCount = rows.size)),
        rows = rows.map { values -> ParsedRow("text", fields = fieldNames.zip(values).toMap()) },
    )

    private companion object {
        const val EMPTY = "빈 항목입니다"
    }
}
