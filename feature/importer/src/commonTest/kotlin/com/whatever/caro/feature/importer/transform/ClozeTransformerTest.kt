package com.whatever.caro.feature.importer.transform

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ClozeTransformerTest : FunSpec() {
    init {
        test("빈칸마다 앞면은 힌트 또는 [...]로 가리고 뒷면은 본문으로 채운다") {
            val text = "{{c1::Seoul}} is the capital of {{c2::Korea::country}}."

            ClozeTransformer.front(text) shouldBe "[...] is the capital of [country]."
            ClozeTransformer.back(text) shouldBe "Seoul is the capital of Korea."
        }

        test("중첩 빈칸은 앞면에서 가장 바깥 빈칸만 가리고 뒷면은 모두 채운다") {
            val text = "{{c1::outer {{c2::inner}} text}} end"

            ClozeTransformer.front(text) shouldBe "[...] end"
            ClozeTransformer.back(text) shouldBe "outer inner text end"
        }

        test("힌트는 본문 뒤 첫 :: 이후 전체다") {
            ClozeTransformer.front("{{c1::a::b::c}}") shouldBe "[b::c]"
            ClozeTransformer.back("{{c1::a::b::c}}") shouldBe "a"
        }

        test("번호 목록 c1,2 도 한 카드에서 가린다") {
            ClozeTransformer.front("{{c1,2::서울}}은 수도") shouldBe "[...]은 수도"
            ClozeTransformer.back("{{c1,2::서울}}은 수도") shouldBe "서울은 수도"
        }

        test("닫히지 않은 표기와 짝 없는 }} 는 글자 그대로 둔다") {
            ClozeTransformer.front("{{c1::open") shouldBe "{{c1::open"
            ClozeTransformer.back("{{c1::open::hint") shouldBe "{{c1::open::hint"
            ClozeTransformer.front("a}}b") shouldBe "a}}b"
        }

        test("빈칸 표기 판정은 {{c숫자:: 모양만 본다") {
            ClozeTransformer.hasClozeMarkup("{{c1::x}}") shouldBe true
            ClozeTransformer.hasClozeMarkup("{{c1,2::x}}") shouldBe true
            ClozeTransformer.hasClozeMarkup("{{c::x}}") shouldBe false
            ClozeTransformer.hasClozeMarkup("{{Front}}") shouldBe false
        }
    }
}
