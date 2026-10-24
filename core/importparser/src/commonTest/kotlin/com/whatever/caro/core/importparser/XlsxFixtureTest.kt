package com.whatever.caro.core.importparser

import com.whatever.caro.core.importparser.model.ParsedLayout
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonObject

class XlsxFixtureTest : FunSpec() {
    init {
        Fixtures.expectedFiles("xlsx").forEach { expectedPath ->
            val expected = Fixtures.readExpected(expectedPath)
            val fixture = expected.string("fixture") ?: error("fixture missing in $expectedPath")

            test("$fixture 파싱 결과가 기대값과 같다") {
                val errorKind = expected.obj("error")?.string("kind")
                if (errorKind != null) {
                    val error = shouldThrow<ImportParseException> { Fixtures.parse(fixture) }
                    error.kind.name shouldBe errorKind
                } else {
                    verifyWorkbook(fixture, expected)
                }
            }
        }

        test("앱 언어가 영어면 기본 날짜 서식을 M/d/yyyy로 쓴다") {
            val options = Fixtures.options(sheetIndex = 1).copy(dateStyle = SpreadsheetDateStyle.MONTH_DAY_YEAR)

            val table = Fixtures.parse("xlsx/wordlist_multi_sheet.xlsx", options)

            table.valuesAt(9) shouldBe listOf("date 14", "3/15/2023")
            table.valuesAt(10) shouldBe listOf("datetime 22", "3/15/2023 18:00")
        }

        test("엑셀 layout은 구분자 없이 제목 행 추정값만 담는다") {
            val table = Fixtures.parse("xlsx/date1904.xlsx")

            table.layout shouldBe ParsedLayout(delimiter = null, hasHeaderRow = true)
        }

        test("제목 행 설정을 끄면 엑셀 layout의 제목 행은 false다") {
            val table = Fixtures.parse("xlsx/date1904.xlsx", Fixtures.options(hasHeaderRow = false))

            table.layout shouldBe ParsedLayout(delimiter = null, hasHeaderRow = false)
        }

        test("엑셀 노트 유형은 선택 범위 노트 수를 채우지 않는다") {
            val table = Fixtures.parse("xlsx/date1904.xlsx")

            table.noteTypes.single().selectedNoteCount shouldBe null
        }

        test("없는 시트 번호를 고르면 지원하지 않는 파일로 멈춘다") {
            val error =
                shouldThrow<ImportParseException> {
                    Fixtures.parse("xlsx/date1904.xlsx", Fixtures.options(sheetIndex = 5))
                }

            error.kind shouldBe ImportParseErrorKind.UNSUPPORTED_FILE
        }
    }

    private suspend fun verifyWorkbook(
        fixture: String,
        expected: JsonObject,
    ) {
        val defaultTable = Fixtures.parse(fixture)
        defaultTable.shouldMatchHeader(expected)
        val sheets = defaultTable.sheets.map { sheet -> Triple(sheet.index, sheet.name, sheet.rowCount) }
        sheets shouldBe expected.objects("sheets").map { Triple(it.int("index"), it.string("name"), it.int("rowCount")) }
        defaultTable.shouldMatchWarnings(expected.objects("warnings"))

        if ("noteTypes" in expected) defaultTable.shouldMatchNoteTypes(expected.objects("noteTypes"))
        defaultTable.shouldMatchListedRows(expected.objects("rows"), indexKey = "index")
        expected.int("dataRowCount")?.let { count -> defaultTable.rows.size shouldBe count }
        expected.obj("limit")?.int("rowsKept")?.let { kept -> defaultTable.rows.size shouldBe kept }
        expected.obj("limit")?.boolean("exceeded")?.let { exceeded -> defaultTable.rowLimitReached shouldBe exceeded }
        expected.boolean("hasHeaderRow")?.let { header -> defaultTable.layout?.hasHeaderRow shouldBe header }

        expected.objects("perSheet").forEach { sheet -> verifySheet(fixture, sheet) }
    }

    private suspend fun verifySheet(
        fixture: String,
        expected: JsonObject,
    ) {
        val index = expected.int("sheetIndex") ?: error("sheetIndex missing")
        val table = Fixtures.parse(fixture, Fixtures.options(sheetIndex = index))

        withClue("sheet $index") {
            table.shouldMatchWarnings(expected.objects("warnings"))
            if (expected.boolean("emptyResult") == true) {
                table.rows.shouldBeEmpty()
            } else {
                table.shouldMatchNoteTypes(expected.objects("noteTypes"))
                table.shouldMatchListedRows(expected.objects("rows"), indexKey = "index")
                table.rows.size shouldBe expected.int("rowCount")
            }
        }
    }
}
