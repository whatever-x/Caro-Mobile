package com.whatever.caro.feature.importer.logic

import com.whatever.caro.core.importparser.SpreadsheetDateStyle
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ImportDateStyleTest : FunSpec() {
    init {
        test("앱 언어가 ko이면 연월일 순서로 쓴다") {
            spreadsheetDateStyleFor("ko") shouldBe SpreadsheetDateStyle.YEAR_MONTH_DAY
        }

        test("앱 언어가 en이면 월일연 순서로 쓴다") {
            spreadsheetDateStyleFor("en") shouldBe SpreadsheetDateStyle.MONTH_DAY_YEAR
        }

        test("앱 문구가 영어로 대체되는 언어도 월일연 순서로 쓴다") {
            spreadsheetDateStyleFor("ja") shouldBe SpreadsheetDateStyle.MONTH_DAY_YEAR
        }
    }
}
