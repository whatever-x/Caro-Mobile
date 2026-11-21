package com.whatever.caro.feature.importer.transform

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ClozeTransformerTest : FunSpec() {
    init {
        test("중첩이 아주 깊어도 멈추지 않고 깊이 상한 밖의 여는 표기는 글자로 둔다") {
            val depth = 100_000
            val literal = depth - MAX_DEPTH
            val text = "{{c1::".repeat(depth) + "x" + "}}".repeat(depth)

            ClozeTransformer.front(text) shouldBe "[...]" + "}}".repeat(literal)
            ClozeTransformer.back(text) shouldBe "{{c1::".repeat(literal) + "x" + "}}".repeat(literal)
        }

        test("힌트가 시작된 뒤의 글자와 중첩 빈칸은 순서대로 모두 힌트에 들어간다") {
            ClozeTransformer.front("{{c1::a::hint {{c2::x}} tail}}") shouldBe "[hint {{c2::x}} tail]"
            ClozeTransformer.back("{{c1::a::hint {{c2::x}} tail}}") shouldBe "a"
            ClozeTransformer.back("{{c1::a::h {{c2::x}}") shouldBe "{{c1::a::h {{c2::x}}"
        }

        test("빈칸 표기 판정은 {{c숫자:: 모양만 본다") {
            ClozeTransformer.hasClozeMarkup("{{c1::x}}") shouldBe true
            ClozeTransformer.hasClozeMarkup("{{c1,2::x}}") shouldBe true
            ClozeTransformer.hasClozeMarkup("{{c::x}}") shouldBe false
            ClozeTransformer.hasClozeMarkup("{{Front}}") shouldBe false
        }
    }

    private companion object {
        const val MAX_DEPTH = 32
    }
}
