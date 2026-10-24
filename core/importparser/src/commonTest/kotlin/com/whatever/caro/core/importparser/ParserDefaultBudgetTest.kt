package com.whatever.caro.core.importparser

import com.whatever.caro.core.importparser.model.ParsedTable
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import okio.Path

class ParserDefaultBudgetTest : FunSpec() {
    init {
        test("표 예산 기본값은 값 있는 칸 25만 개와 전체 글자 1천만 자다") {
            assertSoftly {
                ParseOptions.DEFAULT_MAX_CELLS shouldBe 250_000
                ParseOptions.DEFAULT_MAX_TOTAL_CHARS shouldBe 10_000_000L
            }
        }

        context("텍스트의 기본 예산 경계") {
            test("값 있는 칸이 정확히 25만 개면 읽는다") {
                val table = parseText(tsvGrid(rows = 10_000, columns = 25), Fixtures.options(hasHeaderRow = false))

                table.rows.size shouldBe 10_000
            }

            test("값 있는 칸이 25만 1개면 내용이 너무 김으로 멈춘다") {
                val text = tsvGrid(rows = 10_000, columns = 25) + "\nb"

                shouldThrow<ImportParseException> { parseText(text, Fixtures.options(hasHeaderRow = false)) }.kind shouldBe
                    ImportParseErrorKind.CONTENT_TOO_LONG
            }

            test("글자 수 합계가 정확히 1천만 자면 읽는다") {
                val table = parseText(longLines(count = 20), Fixtures.options(hasHeaderRow = false))

                table.rows.size shouldBe 20
            }

            test("글자 수 합계가 1천만 1자면 내용이 너무 김으로 멈춘다") {
                val text = longLines(count = 20) + "\nb"

                shouldThrow<ImportParseException> { parseText(text, Fixtures.options(hasHeaderRow = false)) }.kind shouldBe
                    ImportParseErrorKind.CONTENT_TOO_LONG
            }
        }

        context("엑셀의 기본 예산 경계") {
            test("값 있는 칸이 정확히 25만 개면 읽는다") {
                val table = parseXlsx(gridSheet(rows = 10_000, columns = 25), Fixtures.options(hasHeaderRow = false))

                table.rows.size shouldBe 10_000
            }

            test("값 있는 칸이 25만 1개면 내용이 너무 김으로 멈춘다") {
                val sheet = gridSheet(rows = 10_000, columns = 25, extraCells = 1)

                shouldThrow<ImportParseException> { parseXlsx(sheet, Fixtures.options(hasHeaderRow = false)) }.kind shouldBe
                    ImportParseErrorKind.CONTENT_TOO_LONG
            }
        }

        context("Anki의 기본 예산 경계") {
            // 앱 상한(10,000장)보다 1 큰 maxRows까지 읽으므로 노트 10,001개째를 읽는 순간 칸 수가 예산을 넘는다.
            test("노트 1만 개 x 항목 25개로 칸이 정확히 25만 개면 읽는다").config(enabled = nativeArchiveLibrariesLoadable) {
                val table = parseBulkApkg("cells-at-budget.apkg", noteCount = 10_000, fieldCount = 25)

                table.rows.size shouldBe 10_000
            }

            test("노트 10,001개 x 항목 25개로 칸이 25만 개를 넘으면 내용이 너무 김으로 멈춘다").config(enabled = nativeArchiveLibrariesLoadable) {
                shouldThrow<ImportParseException> {
                    parseBulkApkg("cells-over-budget.apkg", noteCount = 10_001, fieldCount = 25)
                }.kind shouldBe ImportParseErrorKind.CONTENT_TOO_LONG
            }
        }

        context("실제 규모의 큰 덱") {
            test("항목 20개 x 노트 1만 개로 칸이 20만 개인 Anki 덱은 읽는다").config(enabled = nativeArchiveLibrariesLoadable) {
                val table = parseBulkApkg("big-deck.apkg", noteCount = 10_000, fieldCount = 20, value = "abcdefghijklmnopqrst")

                table.rows.size shouldBe 10_000
            }
        }
    }

    private fun tsvGrid(
        rows: Int,
        columns: Int,
    ): String = List(rows) { List(columns) { "a" }.joinToString("\t") }.joinToString("\n")

    // 값 하나는 필드 길이 상한(100만 자) 안에 두고, 줄 수로 전체 글자 수를 맞춘다.
    private fun longLines(count: Int): String = List(count) { "a".repeat(LONG_LINE_CHARS) }.joinToString("\n")

    // 셀 주소 없이 순서대로 열이 정해지는 공유 문자열 셀이라 25만 칸도 5MB 안팎의 시트로 만든다.
    private fun gridSheet(
        rows: Int,
        columns: Int,
        extraCells: Int = 0,
    ): String {
        val cell = "<c t=\"s\"><v>0</v></c>"
        val full = "<row>${cell.repeat(columns)}</row>".repeat(rows)
        val extra = if (extraCells > 0) "<row>${cell.repeat(extraCells)}</row>" else ""
        return TestZip.sheet(full + extra)
    }

    // notes와 cards를 한 문장으로 채워 노트 1만 개를 건별 insert 없이 만든다.
    private fun bulkNotesSql(
        noteCount: Int,
        fieldCount: Int,
        value: String,
    ): List<String> {
        val fields = "replace('${List(fieldCount) { value }.joinToString(",")}', ',', char(31))"
        val sequence = "WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < $noteCount)"
        return listOf(
            "CREATE TABLE notes (id INTEGER, mid INTEGER, flds TEXT)",
            "CREATE TABLE cards (nid INTEGER, did INTEGER, odid INTEGER, ord INTEGER)",
            "$sequence INSERT INTO notes SELECT n, $NOTE_TYPE_ID, $fields FROM seq",
            "$sequence INSERT INTO cards SELECT n, $DECK_ID, 0, 0 FROM seq",
        )
    }

    private suspend fun parseBulkApkg(
        name: String,
        noteCount: Int,
        fieldCount: Int,
        value: String = "a",
    ): ParsedTable =
        parseGenerated(name, Fixtures.options()) { path ->
            TestAnki.legacy(
                path,
                fieldCount = fieldCount,
                notes = emptyList(),
                notesAndCardsSql = bulkNotesSql(noteCount, fieldCount, value),
            )
        }

    private suspend fun parseXlsx(
        sheetXml: String,
        options: ParseOptions,
    ): ParsedTable =
        parseGenerated("default-budget.xlsx", options) { path ->
            TestZip.xlsx(path, sheetXml, sharedStrings = "<si><t>a</t></si>")
        }

    // 안전성 테스트가 임시 디렉터리 점검에서 input- 접두사 파일만 제외하므로 이 접두사를 지키고, 큰 파일이라 읽은 뒤 바로 지운다.
    private suspend fun parseGenerated(
        name: String,
        options: ParseOptions,
        write: (Path) -> Unit,
    ): ParsedTable {
        Fixtures.fileSystem.createDirectories(Fixtures.tempRoot)
        val path = Fixtures.tempRoot / "input-$name"
        write(path)
        try {
            return ImportParser(Fixtures.fileSystem).parseFile(path, name, options)
        } finally {
            Fixtures.fileSystem.delete(path, mustExist = false)
        }
    }

    private suspend fun parseText(
        text: String,
        options: ParseOptions,
    ) = ImportParser(Fixtures.fileSystem).parseText(text, "p", options)

    private companion object {
        const val LONG_LINE_CHARS = 500_000
        const val NOTE_TYPE_ID = 1
        const val DECK_ID = 1
    }
}
