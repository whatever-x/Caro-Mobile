package com.whatever.caro.core.importparser

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

        test("1904 기준 통합 문서는 일련번호 0이 1904-01-01이다") {
            val format = XlsxNumberFormat(date1904 = true, dateStyle = SpreadsheetDateStyle.YEAR_MONTH_DAY)

            format.format("0", 14).text shouldBe "1904-01-01"
        }

        test("XML 리더는 미리 정의된 엔티티와 숫자 참조만 풀고 모르는 엔티티는 글자로 둔다") {
            val reader = XmlPullReader(Buffer().writeUtf8("<t a=\"&quot;x&quot;\">&lt;b&gt; &#54620;&#xAE00; &foo;</t>"))

            val start = reader.next() as XmlEvent.Start
            val text = reader.next() as XmlEvent.Text

            start.attributes["a"] shouldBe "\"x\""
            text.text shouldBe "<b> 한글 &foo;"
        }

        test("XML 리더는 DOCTYPE 선언을 만나면 지원하지 않는 파일로 멈춘다") {
            val reader = XmlPullReader(Buffer().writeUtf8("<?xml version=\"1.0\"?><!DOCTYPE x [<!ENTITY a \"aaaa\">]><x>&a;</x>"))

            val error = shouldThrow<ImportParseException> { generateSequence { reader.next() }.toList() }

            error.kind shouldBe ImportParseErrorKind.UNSUPPORTED_FILE
        }
    }
}
