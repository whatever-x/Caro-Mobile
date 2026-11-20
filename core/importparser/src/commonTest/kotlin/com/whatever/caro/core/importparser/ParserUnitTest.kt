package com.whatever.caro.core.importparser

import com.whatever.caro.core.importparser.model.ParsedLayout
import com.whatever.caro.core.importparser.text.CharCursor
import com.whatever.caro.core.importparser.text.DelimitedRecordReader
import com.whatever.caro.core.importparser.xlsx.XlsxNumberFormat
import com.whatever.caro.core.importparser.xlsx.XmlEvent
import com.whatever.caro.core.importparser.xlsx.XmlPullReader
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import okio.Buffer

class ParserUnitTest : FunSpec() {
    init {
        test("CR만 쓴 줄 끝, 닫는 구분자, 닫히지 않은 따옴표를 Python csv 기본 동작처럼 읽는다") {
            val reader = DelimitedRecordReader(CharCursor(listOf("a,b\rc,\r\"open").iterator()), ',')

            generateSequence { reader.nextRecord() }.toList() shouldBe listOf(listOf("a", "b"), listOf("c", ""), listOf("open"))
        }

        test("조각 경계에 걸친 따옴표 안 줄바꿈과 CRLF를 한 레코드로 읽는다") {
            val reader = DelimitedRecordReader(CharCursor(listOf("x\t\"줄1\r", "\n줄2\"\r", "\ny\tz").iterator()), '\t')

            generateSequence { reader.nextRecord() }.toList() shouldBe listOf(listOf("x", "줄1\r\n줄2"), listOf("y", "z"))
        }

        test("숫자 서식은 반올림 올림수와 음수, 지수 표기를 Excel 표시처럼 만든다") {
            val format = XlsxNumberFormat(date1904 = false, dateStyle = SpreadsheetDateStyle.YEAR_MONTH_DAY)

            format.format("0.99995", 10).text shouldBe "100.00%"
            format.format("-1234.5", 3).text shouldBe "-1,235"
            format.format("1E-7", 0).text shouldBe "0.0000001"
            format.format("-0.0000000000000001", 0).text shouldBe "-0.0000000000000001"
            format.format("45000.75", 18).text shouldBe "6:00 PM"
            format.format("45000", 15).text shouldBe "15-Mar-23"
            format.format("45000", 17).text shouldBe "Mar-23"
            format.format("1234.5", 2).raw shouldBe true
        }

        test("XML 리더는 미리 정의된 엔티티와 숫자 참조만 풀고 모르는 엔티티는 글자로 둔다") {
            val reader = XmlPullReader(Buffer().writeUtf8("<t a=\"&quot;x&quot;\">&lt;b&gt; &#54620;&#xAE00; &foo;</t>"))

            val start = reader.next() as XmlEvent.Start
            val text = reader.next() as XmlEvent.Text

            start.attributes["a"] shouldBe "\"x\""
            text.text shouldBe "<b> 한글 &foo;"
        }

        test("Anki 텍스트 머리말에 #columns가 있으면 layout이 머리말 항목 이름 사용을 알리고 제목 행은 없다고 한다") {
            val text = "#separator:tab\n#columns:Front\tBack\na\tb"

            val table = ImportParser(Fixtures.fileSystem).parseText(text, "붙여넣기", Fixtures.options())

            table.layout shouldBe ParsedLayout(delimiter = "\t", hasHeaderRow = false, columnsFromHeader = true)
        }

        test("#columns가 없는 텍스트는 layout이 머리말 항목 이름 사용을 알리지 않는다") {
            val table = ImportParser(Fixtures.fileSystem).parseText("a\tb\nc\td", "붙여넣기", Fixtures.options())

            table.layout shouldBe ParsedLayout(delimiter = "\t", hasHeaderRow = false, columnsFromHeader = false)
        }

        listOf(
            Triple("구분자가 없는 텍스트 3줄은 한 칸 행이 3개다", "apple\nbanana\ncherry", 3),
            Triple("두 칸 2줄과 한 칸 1줄이면 한 칸 행은 1개다", "apple\t사과\nbanana\t바나나\ncherry", 1),
            Triple("Anki 텍스트 머리말의 메타 열을 뺀 뒤 남은 칸으로 한 칸 행을 센다", "#separator:tab\n#tags column:2\napple\ttag1\nbanana\ttag2", 2),
            Triple("빈 줄은 행이 아니므로 한 칸 행으로 세지 않는다", "apple\t사과\n\n\r\nbanana\t바나나\n", 0),
            Triple("공백만 있는 줄은 한 칸 행으로 세지 않는다", "apple\t사과\n \nbanana\t바나나\n \n", 0),
            Triple("메타 열만 있는 행은 한 칸 행으로 세지 않는다", "#separator:tab\n#tags column:1\ntag1\ntag2\tapple", 1),
        ).forEach { (name, text, expected) ->
            test(name) {
                singleFieldRows(text, hasHeaderRow = false) shouldBe expected
            }
        }

        test("한 칸뿐인 제목 행은 한 칸 행으로 세지 않는다") {
            singleFieldRows("단어\napple\t사과\nbanana\t바나나", hasHeaderRow = true) shouldBe 0
        }

        test("maxRows에서 읽기를 멈추면 보관한 행 안에서만 한 칸 행을 센다") {
            singleFieldRows("apple\nbanana\ncherry", hasHeaderRow = false, maxRows = 2) shouldBe 2
        }

        test("XML 리더는 DOCTYPE 선언을 만나면 지원하지 않는 파일로 멈춘다") {
            val reader = XmlPullReader(Buffer().writeUtf8("<?xml version=\"1.0\"?><!DOCTYPE x [<!ENTITY a \"aaaa\">]><x>&a;</x>"))

            val error = shouldThrow<ImportParseException> { generateSequence { reader.next() }.toList() }

            error.kind shouldBe ImportParseErrorKind.UNSUPPORTED_FILE
        }
    }

    private suspend fun singleFieldRows(
        text: String,
        hasHeaderRow: Boolean,
        maxRows: Int = Fixtures.MAX_ROWS,
    ): Int? =
        ImportParser(Fixtures.fileSystem)
            .parseText(text, "붙여넣기", Fixtures.options(hasHeaderRow = hasHeaderRow, maxRows = maxRows))
            .layout
            ?.singleFieldRowCount
}
