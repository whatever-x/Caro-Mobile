package com.whatever.caro.feature.importer.transform

import com.whatever.caro.core.importparser.SpreadsheetDateStyle
import com.whatever.caro.core.importparser.model.ParsedSubdeck
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.feature.importer.ImportFixtures
import com.whatever.caro.feature.importer.boolean
import com.whatever.caro.feature.importer.int
import com.whatever.caro.feature.importer.nativeArchiveLibrariesLoadable
import com.whatever.caro.feature.importer.obj
import com.whatever.caro.feature.importer.objects
import com.whatever.caro.feature.importer.string
import com.whatever.caro.feature.importer.strings
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonObject

/** fixture 기대값의 카드 예시와 미리보기 통계를 앱 언어 ko 기본 연결로 다시 계산해 비교한다. */
class ImportPreviewFixtureTest : FunSpec() {
    private val builder = ImportPreviewBuilder()

    init {
        listOf("text", "xlsx", "apkg").forEach { directory ->
            ImportFixtures.expectedFiles(directory).forEach { path ->
                val expected = ImportFixtures.readExpected(path)
                val fixture = expected.string("fixture") ?: error("fixture missing in $path")
                if (expected.obj("error") != null || !ImportFixtures.exists(fixture)) return@forEach
                if (!hasPreviewExpectation(expected)) return@forEach

                test("$fixture 카드 예시와 미리보기 통계가 기대값과 같다").config(enabled = directory != "apkg" || nativeArchiveLibrariesLoadable) {
                    when (directory) {
                        "text" -> verifyTable(ImportFixtures.parse(fixture), expected)
                        "xlsx" -> verifyWorkbook(fixture, expected)
                        else -> verifyPackage(fixture, expected)
                    }
                }
            }
        }
    }

    private fun hasPreviewExpectation(expected: JsonObject): Boolean =
        "cards" in expected || "previewStats" in expected || expected.objects("perSheet").any { "cards" in it }

    private suspend fun verifyTable(
        table: ParsedTable,
        expected: JsonObject,
    ) {
        val preview = builder.build(table, defaultFieldMappings(table), ImportFixtures.EMPTY_SIDE_KO)
        expected.obj("cards")?.objects("items")?.let { items -> preview.shouldMatchItems(items) }
        expected.obj("previewStats")?.let { stats -> preview.stats.shouldMatch(stats) }
        expected.obj("limit")?.boolean("exceeded")?.let { exceeded -> preview.limitExceeded shouldBe exceeded }
    }

    private suspend fun verifyWorkbook(
        fixture: String,
        expected: JsonObject,
    ) {
        if ("previewStats" in expected) verifyTable(ImportFixtures.parse(fixture), expected)
        expected.objects("perSheet").filter { "cards" in it }.forEach { sheet ->
            val index = sheet.int("sheetIndex") ?: error("sheetIndex missing")
            withClue("sheet $index") { verifyTable(ImportFixtures.parse(fixture, sheetIndex = index), sheet) }
            val englishItems =
                sheet
                    .obj("cards")
                    ?.objects("items")
                    .orEmpty()
                    .filter { "backEn" in it }
            if (englishItems.isNotEmpty()) {
                val english = ImportFixtures.parse(fixture, sheetIndex = index, dateStyle = SpreadsheetDateStyle.MONTH_DAY_YEAR)
                val cards = builder.build(english, defaultFieldMappings(english), ImportFixtures.EMPTY_SIDE_KO).cards
                englishItems.forEach { item ->
                    withClue("en row ${item.int("rowIndex")}") {
                        cards.first { it.sourceIndex == item.int("rowIndex") }.back shouldBe item.string("backEn")
                    }
                }
            }
        }
    }

    private suspend fun verifyPackage(
        fixture: String,
        expected: JsonObject,
    ) {
        val defaultTable = ImportFixtures.parse(fixture)
        val allNotes = ImportFixtures.parse(fixture, subdeckIds = defaultTable.subdecks.map { it.id }.toSet() + ParsedSubdeck.NO_DECK_ID)
        expected.obj("cards")?.objects("items")?.let { items ->
            builder.build(allNotes, defaultFieldMappings(allNotes), ImportFixtures.EMPTY_SIDE_KO).shouldMatchItems(items)
        }
        val defaultPreview = builder.build(defaultTable, defaultFieldMappings(defaultTable), ImportFixtures.EMPTY_SIDE_KO)
        expected.obj("previewStats")?.let { stats -> defaultPreview.stats.shouldMatch(stats) }
        expected.obj("limit")?.boolean("exceededWithDefaultSelection")?.let { exceeded -> defaultPreview.limitExceeded shouldBe exceeded }
        expected.objects("selectionExamples").filter { "cardCount" in it }.forEach { example ->
            val ids =
                example
                    .strings("selectedPaths")
                    .map { selected ->
                        selected?.let { defaultTable.subdecks.first { it.path == selected }.id } ?: ParsedSubdeck.NO_DECK_ID
                    }.toSet()
            val table = ImportFixtures.parse(fixture, subdeckIds = ids)
            val preview = builder.build(table, defaultFieldMappings(table), ImportFixtures.EMPTY_SIDE_KO)
            withClue("selection ${example.strings("selectedPaths")}") {
                preview.stats.cardCount shouldBe example.int("cardCount")
                example.boolean("exceeded")?.let { exceeded -> preview.limitExceeded shouldBe exceeded }
            }
        }
    }

    private fun ImportPreview.shouldMatchItems(items: List<JsonObject>) {
        items.forEach { item ->
            val index = item.int("rowIndex") ?: error("rowIndex missing")
            val card = cards.firstOrNull { it.sourceIndex == index }
            withClue("row $index") {
                if (item.boolean("excluded") == true) {
                    card.shouldBeNull()
                } else {
                    val actual = card ?: error("card $index missing")
                    listOf(actual.front, actual.back, actual.frontReplaced, actual.backReplaced) shouldBe
                        listOf(item.string("front"), item.string("back"), item.boolean("frontReplaced"), item.boolean("backReplaced"))
                    listOf(actual.frontTruncated, actual.backTruncated, actual.mediaRemoved) shouldBe
                        listOf(item.boolean("frontTruncated"), item.boolean("backTruncated"), item.int("mediaRemoved"))
                }
            }
        }
    }

    private fun ImportPreviewStats.shouldMatch(expected: JsonObject) {
        expected.int("cardCount")?.let { cardCount shouldBe it }
        expected.int("excludedCount")?.let { excludedCount shouldBe it }
        expected.int("emptyReplacedCount")?.let { emptyReplacedCount shouldBe it }
        expected.int("truncatedCount")?.let { truncatedCount shouldBe it }
        expected.int("mediaRemovedCount")?.let { mediaRemovedCount shouldBe it }
    }
}
