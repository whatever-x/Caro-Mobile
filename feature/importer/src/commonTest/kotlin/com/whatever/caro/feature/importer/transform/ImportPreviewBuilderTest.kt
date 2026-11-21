package com.whatever.caro.feature.importer.transform

import com.whatever.caro.core.importparser.model.HtmlMode
import com.whatever.caro.core.importparser.model.ImportFormat
import com.whatever.caro.core.importparser.model.ParseWarning
import com.whatever.caro.core.importparser.model.ParsedNoteType
import com.whatever.caro.core.importparser.model.ParsedRow
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.model.card.CardContent
import com.whatever.caro.core.model.importing.FieldMapping
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ImportPreviewBuilderTest : FunSpec() {
    private val builder = ImportPreviewBuilder()

    init {
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

        test("500자를 넘는 면은 500에서 자르고 서로게이트 쌍 가운데면 한 칸 앞에서 자른다") {
            val pair = "\uD83D\uDE00"
            val long = "a".repeat(499) + pair + "b"
            val table = textTable(listOf("a", "b"), listOf(long, "x".repeat(501)))

            val card = builder.build(table, defaultFieldMappings(table), EMPTY).cards.single()

            card.front shouldBe "a".repeat(499)
            card.frontTruncated shouldBe true
            card.back.length shouldBe 500
            card.backTruncated shouldBe true
        }

        test("잘림 수는 잘린 면이 하나라도 있는 카드의 수다") {
            val table =
                textTable(
                    listOf("a", "b"),
                    listOf("a".repeat(501), "b".repeat(501)),
                    listOf("c".repeat(501), "뒤"),
                    listOf("앞", "뒤"),
                )

            builder.build(table, defaultFieldMappings(table), EMPTY).stats.truncatedCount shouldBe 2
        }

        test("빈 면 문구와 잘린 값도 그대로 CardContent로 옮긴다") {
            val table = textTable(listOf("a", "b"), listOf("", "x".repeat(501)))

            val card = builder.build(table, defaultFieldMappings(table), EMPTY).cards.single()

            card.toCardContent() shouldBe CardContent(front = EMPTY, back = "x".repeat(500))
        }

        test("제외한 노트 유형의 행은 카드도 제외 수도 만들지 않는다") {
            val table = textTable(listOf("a", "b"), listOf("앞", "뒤"))
            val mapping = defaultFieldMappings(table).getValue("text").copy(excluded = true)

            val preview = builder.build(table, mapOf("text" to mapping), EMPTY)

            preview.cards shouldBe emptyList()
            preview.stats.excludedCount shouldBe 0
        }

        test("가져올 카드 수가 상한을 넘어도 카드와 통계를 만들고 상한 초과만 표시한다") {
            val rows = List(4) { index -> listOf("앞$index", "뒤$index") }
            val table = textTable(listOf("a", "b"), *rows.toTypedArray())

            val preview = builder.build(table, defaultFieldMappings(table), EMPTY, maxCards = 3)

            preview.stats.cardCount shouldBe 4
            preview.limitExceeded shouldBe true
            builder.build(table, defaultFieldMappings(table), EMPTY, maxCards = 4).limitExceeded shouldBe false
        }

        test("파서가 행 상한에 닿았으면 제외 카드로 장수가 상한 아래여도 상한 초과다") {
            val rows = List(4) { index -> listOf("앞$index", "뒤$index") } + listOf(listOf("", ""))
            val table = textTable(listOf("a", "b"), *rows.toTypedArray()).copy(rowLimitReached = true)

            val preview = builder.build(table, defaultFieldMappings(table), EMPTY, maxCards = 4)

            preview.stats.cardCount shouldBe 4
            preview.limitExceeded shouldBe true
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
