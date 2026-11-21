package com.whatever.caro.feature.importer.logic

import com.whatever.caro.core.importparser.ImportParser
import com.whatever.caro.core.importparser.SpreadsheetDateStyle
import com.whatever.caro.core.importparser.model.ImportFormat
import com.whatever.caro.core.importparser.model.ParsedLayout
import com.whatever.caro.core.importparser.model.ParsedRow
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.model.importing.FieldMapping
import com.whatever.caro.feature.importer.source.ImportLimitsConfig
import com.whatever.caro.feature.importer.source.buildParseOptions
import com.whatever.caro.feature.importer.support.ImportTestTables
import com.whatever.caro.feature.importer.transform.ImportPreview
import com.whatever.caro.feature.importer.transform.ImportPreviewBuilder
import com.whatever.caro.feature.importer.transform.defaultFieldMappings
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import okio.FileSystem

class PreviewRulesTest : FunSpec() {
    init {
        context("카드 0장의 이유") {
            test("Image Occlusion만 있는 표는 ImageOcclusionOnly다") {
                val table = imageOcclusionOnly()

                noCardsKind(table, preview(table)) shouldBe NoCardsKind.ImageOcclusionOnly
            }

            test("Image Occlusion 말고도 노트가 있는데 카드가 없으면 NoContent다") {
                val anki = ImportTestTables.apkg("modern_image_occlusion.apkg", "default")
                val table = anki.copy(rows = anki.rows.map { row -> row.copy(fields = row.fields.mapValues { "" }) })

                noCardsKind(table, preview(table)) shouldBe NoCardsKind.NoContent
            }

            test("고른 범위에 노트가 없는 일반 유형은 세지 않아 범위 안 유형이 Image Occlusion뿐이면 ImageOcclusionOnly다") {
                val anki = ImportTestTables.apkg("modern_image_occlusion.apkg", "default")
                val table =
                    anki.copy(
                        noteTypes = anki.noteTypes.map { type -> type.copy(selectedNoteCount = if (type.isImageOcclusion) 2 else 0) },
                        rows = emptyList(),
                    )

                noCardsKind(table, preview(table)) shouldBe NoCardsKind.ImageOcclusionOnly
            }

            test("카드가 있으면 null이다") {
                val table = ImportTestTables.text(rows = 1)

                noCardsKind(table, preview(table)) shouldBe null
            }
        }

        context("구분 기준 확인") {
            test("두 칸 2줄과 한 칸 1줄은 거짓이다") {
                needsDelimiterCheck(parseText("사과\tapple\n바나나\tbanana\n포도")) shouldBe false
            }

            test("한 칸 행이 데이터 행의 절반이면 참이다") {
                needsDelimiterCheck(textWithSingleFieldRows(rows = 2, singleFieldRows = 1)) shouldBe true
            }

            test("텍스트가 아닌 형식은 거짓이다") {
                val table = textWithSingleFieldRows(rows = 3, singleFieldRows = 3).copy(format = ImportFormat.XLSX)

                needsDelimiterCheck(table) shouldBe false
            }
        }

        context("첫 카드 미리보기") {
            test("앞뒤가 모두 빈 행은 건너뛰고 내용이 있는 첫 행으로 만든다") {
                val table = text(listOf("", " "), listOf("앞", "뒤"))

                val card = firstPreviewCard(table, defaultFieldMappings(table).getValue(TEXT_ID), EMPTY_SIDE).shouldNotBeNull()

                card.sourceIndex shouldBe 1
                card.front shouldBe "앞"
                card.back shouldBe "뒤"
            }

            test("한 면만 빈 행은 빈 면 문구로 채워 고른다") {
                val table = text(listOf("", "뒤"), listOf("앞", "뒤"))

                val card = firstPreviewCard(table, defaultFieldMappings(table).getValue(TEXT_ID), EMPTY_SIDE).shouldNotBeNull()

                card.sourceIndex shouldBe 0
                card.front shouldBe EMPTY_SIDE
                card.frontReplaced shouldBe true
            }

            test("항목 이름이 같은 다른 유형의 행은 건너뛰고 넘겨받은 연결의 유형 행으로 만든다") {
                val base = ImportTestTables.text(rows = 0)
                val first = base.noteTypes.single()
                val second = first.copy(id = "second", name = "둘째 유형")
                val fields = first.fieldNames
                val rows =
                    listOf(
                        ParsedRow(noteTypeId = first.id, fields = mapOf(fields[0] to "첫째 앞", fields[1] to "첫째 뒤")),
                        ParsedRow(noteTypeId = second.id, fields = mapOf(fields[0] to "둘째 앞", fields[1] to "둘째 뒤")),
                    )
                val table = base.copy(noteTypes = listOf(first, second), rows = rows)
                val mapping =
                    FieldMapping(second.id, mapOf(FieldMapping.FRONT to listOf(fields[0]), FieldMapping.BACK to listOf(fields[1])))

                val card = firstPreviewCard(table, mapping, EMPTY_SIDE).shouldNotBeNull()

                card.sourceIndex shouldBe 1
                card.front shouldBe "둘째 앞"
            }
        }
    }

    private suspend fun preview(table: ParsedTable): ImportPreview =
        ImportPreviewBuilder().build(table, defaultFieldMappings(table), EMPTY_SIDE)

    private fun imageOcclusionOnly(): ParsedTable {
        val anki = ImportTestTables.apkg("modern_image_occlusion.apkg", "default")
        return anki.copy(noteTypes = anki.noteTypes.filter { type -> type.isImageOcclusion }, rows = emptyList())
    }

    private suspend fun parseText(text: String): ParsedTable =
        ImportParser(FileSystem.SYSTEM).parseText(
            text = text,
            sourceName = "words.txt",
            options = buildParseOptions(ImportLimitsConfig(), SpreadsheetDateStyle.YEAR_MONTH_DAY),
        )

    private fun textWithSingleFieldRows(
        rows: Int,
        singleFieldRows: Int,
    ): ParsedTable =
        ImportTestTables
            .text(rows = rows)
            .copy(layout = ParsedLayout(delimiter = "\t", hasHeaderRow = false, singleFieldRowCount = singleFieldRows))

    private fun text(vararg rows: List<String>): ParsedTable {
        val base = ImportTestTables.text(rows = rows.size)
        val names = base.noteTypes.single().fieldNames
        return base.copy(rows = base.rows.zip(rows) { row, values -> row.copy(fields = names.zip(values).toMap()) })
    }

    private companion object {
        const val TEXT_ID = "text"
        const val EMPTY_SIDE = "(비어 있음)"
    }
}
