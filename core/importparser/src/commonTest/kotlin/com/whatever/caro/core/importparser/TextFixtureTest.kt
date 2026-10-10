package com.whatever.caro.core.importparser

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonObject

class TextFixtureTest : FunSpec() {
    init {
        Fixtures.expectedFiles("text").forEach { expectedPath ->
            val expected = Fixtures.readExpected(expectedPath)
            val fixture = expected.string("fixture") ?: error("fixture missing in $expectedPath")
            if (!Fixtures.exists(fixture)) return@forEach

            test("$fixture 파싱 결과가 기대값과 같다") {
                verify(fixture, expected)
            }
        }

        test("붙여넣은 텍스트는 같은 내용의 파일과 같은 행을 만들고 노트 유형 이름은 붙여넣기 문구다") {
            val fixture = "text/quizlet_default_tab_newline.txt"
            val text = Fixtures.fileSystem.read(Fixtures.path(fixture)) { readUtf8() }
            val fromFile = Fixtures.parse(fixture)

            val pasted = ImportParser(Fixtures.fileSystem).parseText(text, "붙여넣기", Fixtures.options())

            pasted.rows shouldBe fromFile.rows
            pasted.noteTypes.single().name shouldBe "붙여넣기"
            pasted.sourceName shouldBe "붙여넣기"
        }

        test("구분자를 쉼표로 바꿔 다시 파싱하면 쉼표 기준으로 나뉜다") {
            val table = Fixtures.parse("text/quizlet_custom_comma_semicolon.txt", Fixtures.options(delimiter = ','))

            table.valuesAt(0) shouldBe listOf("abandon", "버리다;benevolent", "자비로운;candid", "솔직한")
            table.textLayout?.delimiter shouldBe ","
        }

        test("제목 행 설정을 끄면 첫 행이 데이터 행이 되고 항목 이름은 열 N이다") {
            val table = Fixtures.parse("text/csv_header_bom_quoted.csv", Fixtures.options(hasHeaderRow = false))

            table.valuesAt(0) shouldBe listOf("term", "definition", "example")
            table.noteTypes.single().fieldNames shouldBe listOf("열 1", "열 2", "열 3")
            table.textLayout?.hasHeaderRow shouldBe false
        }
    }

    private suspend fun verify(
        fixture: String,
        expected: JsonObject,
    ) {
        val errorKind = expected.obj("error")?.string("kind")
        val needsDecoder = expected.string("encoding") == "CP949"
        when {
            errorKind != null -> {
                val error = shouldThrow<ImportParseException> { Fixtures.parse(fixture) }
                error.kind.name shouldBe errorKind
            }

            needsDecoder && testLegacyKoreanDecoder() == null -> {
                val error = shouldThrow<ImportParseException> { Fixtures.parse(fixture) }
                error.kind shouldBe ImportParseErrorKind.ENCODING_UNDETERMINED
            }

            else -> {
                verifyParsed(fixture, expected)
            }
        }
    }

    private suspend fun verifyParsed(
        fixture: String,
        expected: JsonObject,
    ) {
        val table = Fixtures.parse(fixture)

        table.shouldMatchHeader(expected)
        table.shouldMatchNoteTypes(expected.objects("noteTypes"))
        table.shouldMatchWarnings(expected.objects("warnings"))
        table.shouldMatchListedRows(expected.objects("rows"), indexKey = "index")
        expected.int("rowCount")?.let { count -> table.rows.size shouldBe count }
        expected.obj("limit")?.int("rowsKept")?.let { kept -> table.rows.size shouldBe kept }
        expected.string("separator")?.let { separator -> table.textLayout?.delimiter shouldBe separator }
        expected.boolean("hasHeaderRow")?.let { header -> table.textLayout?.hasHeaderRow shouldBe header }
        table.subdecks shouldBe emptyList()
        table.sheets shouldBe emptyList()
    }
}
