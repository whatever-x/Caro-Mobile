package com.whatever.caro.feature.importer.logic

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ImportDisplayTextTest : FunSpec() {
    init {
        context("파일 이름 자르기") {
            test("120자 이하는 그대로 둔다") {
                val name = "a".repeat(115) + ".apkg"

                truncateFileName(name) shouldBe name
            }

            test("121자부터는 전체 길이가 120자가 되도록 이름 부분을 ...으로 끝낸다") {
                val name = "a".repeat(116) + ".apkg"

                truncateFileName(name) shouldBe "a".repeat(114) + "…" + ".apkg"
            }

            test("아주 긴 이름도 확장자는 그대로 남긴다") {
                truncateFileName("a".repeat(500) + ".xlsx").endsWith("….xlsx") shouldBe true
            }

            test("긴 한글 이름도 전체 길이가 120자다") {
                truncateFileName("한".repeat(150) + ".txt").length shouldBe 120
            }

            test("확장자가 없으면 이름 끝을 ...으로 끝낸다") {
                truncateFileName("b".repeat(200)) shouldBe "b".repeat(119) + "…"
            }

            test("확장자만으로 한도를 채우는 이름은 전체를 120자로 자른다") {
                truncateFileName("a." + "x".repeat(200)) shouldBe "a." + "x".repeat(117) + "…"
            }

            test("점이 여러 개면 마지막 점 뒤만 확장자로 남긴다") {
                truncateFileName("v1.2." + "c".repeat(200) + ".tsv") shouldBe "v1.2." + "c".repeat(110) + "…" + ".tsv"
            }
        }

        context("선택기가 준 파일 이름 정리") {
            test("방향 제어 문자를 지워 확장자를 다르게 보이게 하지 못한다") {
                sanitizeDisplayName("deck\u202Egpj.apkg") shouldBe "deckgpj.apkg"
                sanitizeDisplayName("\u200Fdeck\u200E.apkg") shouldBe "deck.apkg"
                sanitizeDisplayName("a\u202Ab\u202Cc\u2066d\u2069.txt") shouldBe "abcd.txt"
            }

            test("줄바꿈과 널 같은 제어 문자를 지운다") {
                sanitizeDisplayName("de\nck\u0000.apkg") shouldBe "deck.apkg"
                sanitizeDisplayName("de\u007Fck.apkg") shouldBe "deck.apkg"
            }

            test("정상 이름은 한글과 공백을 포함해 그대로 둔다") {
                sanitizeDisplayName("영어 단어 (1).apkg") shouldBe "영어 단어 (1).apkg"
            }

            test("아주 긴 이름은 확장자를 남기고 120자로 줄인다") {
                val name = sanitizeDisplayName("a".repeat(5_000) + ".apkg")

                name.length shouldBe 120
                name.endsWith("….apkg") shouldBe true
            }
        }

        context("항목 미리 자르기") {
            test("셀은 200자까지 그대로 둔다") {
                truncateCell("c".repeat(200)) shouldBe "c".repeat(200)
            }

            test("셀이 201자면 199자 뒤에 ...을 붙여 200자로 만든다") {
                truncateCell("c".repeat(201)) shouldBe "c".repeat(199) + "…"
            }

            test("칩은 80자까지 그대로 둔다") {
                truncateChip("h".repeat(80)) shouldBe "h".repeat(80)
            }

            test("칩이 81자면 79자 뒤에 ...을 붙여 80자로 만든다") {
                truncateChip("h".repeat(81)) shouldBe "h".repeat(79) + "…"
            }

            test("경로는 200자까지 그대로 둔다") {
                truncatePath("p".repeat(200)) shouldBe "p".repeat(200)
            }

            test("경로가 201자면 199자 뒤에 ...을 붙여 200자로 만든다") {
                truncatePath("p".repeat(201)) shouldBe "p".repeat(199) + "…"
            }

            test("시트 이름은 100자까지 그대로 둔다") {
                truncateSheetName("s".repeat(100)) shouldBe "s".repeat(100)
            }

            test("시트 이름이 101자면 99자 뒤에 ...을 붙여 100자로 만든다") {
                truncateSheetName("s".repeat(101)) shouldBe "s".repeat(99) + "…"
            }

            test("빈 문자열은 그대로 둔다") {
                truncateCell("") shouldBe ""
            }

            test("자르는 위치가 서로게이트 쌍 가운데면 깨진 글자를 남기지 않는다") {
                // 이모지는 UTF-16 두 칸이라 199번째 칸에서 자르면 앞 절반만 남는다.
                truncateCell("a".repeat(198) + "😀" + "b") shouldBe "a".repeat(198) + "…"
            }
        }
    }
}
