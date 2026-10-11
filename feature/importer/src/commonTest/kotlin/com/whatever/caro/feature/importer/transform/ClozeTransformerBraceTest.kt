package com.whatever.caro.feature.importer.transform

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

// 토큰 분리 정규식의 중괄호 이스케이프를 고쳐도 같은 결과가 나와야 하는 중괄호 경계 입력을 고정한다.
class ClozeTransformerBraceTest :
    FunSpec({
        test("닫는 중괄호가 세 개 이어지면 앞의 두 개로 닫고 남는 하나는 글자로 둔다") {
            ClozeTransformer.front("{{c1::a}}}") shouldBe "[...]}"
            ClozeTransformer.back("{{c1::a}}}") shouldBe "a}"
        }

        test("본문 안의 중괄호 한 개는 글자로 두고 빈칸을 닫지 않는다") {
            ClozeTransformer.front("{{c1::a}b}}") shouldBe "[...]"
            ClozeTransformer.back("{{c1::a}b}}") shouldBe "a}b"
            ClozeTransformer.back("{{c1::{x}}}") shouldBe "{x}"
        }

        test("힌트 안의 여는 중괄호도 글자로 두고 닫는 표기는 앞에서부터 짝짓는다") {
            ClozeTransformer.front("{{c1::a::{h}}}") shouldBe "[{h]}"
            ClozeTransformer.back("{{c1::a::{h}}}") shouldBe "a}"
        }

        test("짝 없는 닫는 표기 두 개는 글자 그대로 둔다") {
            ClozeTransformer.front("}}x}}") shouldBe "}}x}}"
            ClozeTransformer.back("}}x}}") shouldBe "}}x}}"
        }

        test("여는 중괄호가 한 개이거나 콜론이 하나뿐인 표기는 빈칸이 아니다") {
            ClozeTransformer.hasClozeMarkup("{c1::x}") shouldBe false
            ClozeTransformer.hasClozeMarkup("{{c1:x}}") shouldBe false
            ClozeTransformer.front("{c1::x} {{c1:x}}") shouldBe "{c1::x} {{c1:x}}"
        }

        test("붙어 있는 빈칸 두 개를 각각 가리고 각각 채운다") {
            ClozeTransformer.front("{{c1::a}}{{c2::b}}") shouldBe "[...][...]"
            ClozeTransformer.back("{{c1::a}}{{c2::b}}") shouldBe "ab"
        }

        test("본문이 빈 빈칸도 가리고 뒷면에는 빈 문자열로 채운다") {
            ClozeTransformer.front("x{{c1::}}y") shouldBe "x[...]y"
            ClozeTransformer.back("x{{c1::}}y") shouldBe "xy"
        }

        test("두 자리 번호 목록과 한글 본문도 한 빈칸으로 본다") {
            ClozeTransformer.front("{{c10,11::수도}}") shouldBe "[...]"
            ClozeTransformer.back("{{c10,11::수도}}") shouldBe "수도"
            ClozeTransformer.hasClozeMarkup("{{c10,11::수도}}") shouldBe true
        }

        test("중괄호가 없는 문장은 앞면과 뒷면 모두 그대로다") {
            ClozeTransformer.front("plain text") shouldBe "plain text"
            ClozeTransformer.back("plain text") shouldBe "plain text"
        }
    })
