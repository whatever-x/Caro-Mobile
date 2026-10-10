package com.whatever.caro.core.importparser

import com.whatever.caro.core.importparser.model.ParsedSubdeck
import com.whatever.caro.core.importparser.model.ParsedTable
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonObject

class ApkgFixtureTest : FunSpec() {
    init {
        Fixtures.expectedFiles("apkg").forEach { expectedPath ->
            val expected = Fixtures.readExpected(expectedPath)
            val fixture = expected.string("fixture") ?: error("fixture missing in $expectedPath")

            test("$fixture 파싱 결과가 기대값과 같다").config(enabled = nativeArchiveLibrariesLoadable) {
                val errorKind = expected.obj("error")?.string("kind")
                if (errorKind != null) {
                    val error = shouldThrow<ImportParseException> { Fixtures.parse(fixture) }
                    error.kind.name shouldBe errorKind
                } else {
                    verifyPackage(fixture, expected)
                }
            }
        }
    }

    private suspend fun verifyPackage(
        fixture: String,
        expected: JsonObject,
    ) {
        val table = Fixtures.parse(fixture)
        table.shouldMatchHeader(expected)
        table.shouldMatchNoteTypes(expected.objects("noteTypes"))
        table.shouldMatchSubdecks(expected.objects("subdecks"))
        table.shouldMatchWarnings(expected.objects("warnings"))
        withClue("notes without deck") {
            table.noteTypes.sumOf { it.noteCount } - table.subdecks.sumOf { it.noteCount } shouldBe expected.int("notesWithoutDeck")
        }

        val allNotes = Fixtures.parse(fixture, Fixtures.options(subdeckIds = table.allSelectionIds()))
        allNotes.shouldMatchListedRows(expected.objects("rows"), indexKey = "rowIndex")
        verifyRowSubdecks(allNotes, expected.objects("rows"))
        expected.objects("selectionExamples").forEach { example -> verifySelection(fixture, table, example) }
    }

    private fun verifyRowSubdecks(
        table: ParsedTable,
        expectedRows: List<JsonObject>,
    ) {
        expectedRows.filter { row -> "subdeckPath" in row }.forEach { row ->
            val index = row.int("rowIndex") ?: error("rowIndex missing")
            val subdeckId = table.rows[index].subdeckId
            val path = table.subdecks.firstOrNull { it.id == subdeckId }?.path
            withClue("row $index subdeck") { path shouldBe row.string("subdeckPath") }
        }
    }

    private suspend fun verifySelection(
        fixture: String,
        table: ParsedTable,
        example: JsonObject,
    ) {
        val noteCount = example.int("noteCount") ?: return
        val ids =
            example
                .strings("selectedPaths")
                .map { path -> path?.let { table.subdecks.first { it.path == path }.id } ?: ParsedSubdeck.NO_DECK_ID }
                .toSet()

        val selected = Fixtures.parse(fixture, Fixtures.options(subdeckIds = ids))

        withClue("selection ${example.strings("selectedPaths")}") { selected.rows.size shouldBe noteCount }
    }

    private fun ParsedTable.allSelectionIds(): Set<String> = subdecks.map { it.id }.toSet() + ParsedSubdeck.NO_DECK_ID
}
