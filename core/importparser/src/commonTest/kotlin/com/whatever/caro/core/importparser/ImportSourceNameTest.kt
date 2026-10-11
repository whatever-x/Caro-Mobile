package com.whatever.caro.core.importparser

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ImportSourceNameTest : FunSpec() {
    init {
        listOf("apkg", "txt", "csv", "tsv", "xlsx").forEach { extension ->
            test("확장자 ${extension}는 지원하는 확장자로 돌려준다") {
                ImportSourceName.supportedExtension("deck.$extension") shouldBe extension
            }
        }

        test("대문자 확장자는 소문자로 바꿔 돌려준다") {
            ImportSourceName.supportedExtension("A.APKG") shouldBe "apkg"
        }

        test("점이 여러 개면 마지막 점 뒤를 확장자로 본다") {
            ImportSourceName.supportedExtension("x.tar.csv") shouldBe "csv"
        }

        listOf("a", "a.", "a.xls", "deck.colpkg").forEach { name ->
            test("지원하지 않는 이름 ${name}은 null이다") {
                ImportSourceName.supportedExtension(name) shouldBe null
            }
        }
    }
}
