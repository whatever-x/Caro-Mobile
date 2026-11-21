package com.whatever.caro.feature.importer.logic

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PasteRulesTest : FunSpec() {
    init {
        context("넘친 붙여넣기 유지 판정") {
            val overflowed = "0123456789A"

            test("상한을 넘는 입력은 넘친 것이다") {
                nextPasteOverflowed(previous = "", wasOverflowed = false, edited = overflowed, maxPasteChars = 10) shouldBe true
            }

            test("넘친 적 없고 상한 이하면 넘치지 않은 것이다") {
                nextPasteOverflowed(previous = "abc", wasOverflowed = false, edited = "abcd", maxPasteChars = 10) shouldBe false
            }

            test("넘친 뒤 비우면 풀린다") {
                nextPasteOverflowed(overflowed, wasOverflowed = true, edited = "", maxPasteChars = 10) shouldBe false
            }

            test("넘친 뒤 통째로 다른 글로 바꾸면 풀린다") {
                nextPasteOverflowed(overflowed, wasOverflowed = true, edited = "새 글", maxPasteChars = 10) shouldBe false
            }

            test("넘친 뒤 다시 상한을 넘기면 계속 넘친 것이다") {
                nextPasteOverflowed(overflowed, wasOverflowed = true, edited = "새 글을 아주 길게 붙여 넣은 입력", maxPasteChars = 10) shouldBe true
            }
        }

        context("넘친 뒤 일부를 바꾼 편집") {
            val stored = buildString { repeat(101) { index -> append('a' + index % 26) } }

            test("가운데 5자를 한 글자로 바꿔 상한 이하로 돌아와도 계속 넘친 것이다") {
                val edited = stored.replaceRange(48, 53, "X")

                edited.length shouldBe 97
                nextPasteOverflowed(stored, wasOverflowed = true, edited = edited, maxPasteChars = 100) shouldBe true
            }

            test("지워서 줄인 글 끝에 덧붙여도 계속 넘친 것이다") {
                val shortened = stored.take(60)

                nextPasteOverflowed(shortened, wasOverflowed = true, edited = shortened + "XYZ", maxPasteChars = 100) shouldBe true
            }
        }

        context("공통 부분 경계") {
            val overflowed = "0123456789A"

            test("앞이나 뒤의 공통 부분이 새 글의 정확히 절반이면 계속 넘친 것이다") {
                nextPasteOverflowed(overflowed, wasOverflowed = true, edited = "01ZZ", maxPasteChars = 10) shouldBe true
                nextPasteOverflowed(overflowed, wasOverflowed = true, edited = "ZZ9A", maxPasteChars = 10) shouldBe true
            }

            test("공통 부분이 새 글의 절반보다 적으면 풀린다") {
                nextPasteOverflowed(overflowed, wasOverflowed = true, edited = "01ZZZ", maxPasteChars = 10) shouldBe false
            }

            test("보관된 글이 앞과 뒤에 함께 걸쳐도 같은 글자를 두 번 세지 않는다") {
                nextPasteOverflowed("aa", wasOverflowed = true, edited = "aaZZZZaa", maxPasteChars = 10) shouldBe false
            }

            test("앞쪽 공통 부분이 서로게이트 쌍 가운데서 끊기면 같은 high surrogate는 세지 않는다") {
                val stored = "01😀3456789"

                nextPasteOverflowed(stored, wasOverflowed = true, edited = "01😁ZZ", maxPasteChars = 10) shouldBe false
                nextPasteOverflowed(stored, wasOverflowed = true, edited = "01😀ZZ", maxPasteChars = 10) shouldBe true
            }

            test("뒤쪽 공통 부분이 서로게이트 쌍 가운데서 끊기면 같은 low surrogate는 세지 않는다") {
                val stored = "012345678🐀"

                nextPasteOverflowed(stored, wasOverflowed = true, edited = "🀀", maxPasteChars = 10) shouldBe false
                nextPasteOverflowed(stored, wasOverflowed = true, edited = "🐀", maxPasteChars = 10) shouldBe true
            }
        }
    }
}
