package com.whatever.caro.core.designsystem

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ImportCopyTest :
    FunSpec({
        importKoCopy.forEach { (key, expected) ->
            test("ko $key 문구는 설계 문구와 같다") {
                val actual = ImportStringsXml.valueOf(StringsLocale.Ko, key)

                ImportStringsXml.normalizeArgTypes(actual) shouldBe ImportStringsXml.normalizeArgTypes(expected)
            }
        }

        test("en 빈 면 채움 문구는 Empty다") {
            ImportStringsXml.valueOf(StringsLocale.En, "import_value_empty_field") shouldBe "Empty"
        }

        test("en 붙여넣기 덱 이름 문구는 Pasted cards다") {
            ImportStringsXml.valueOf(StringsLocale.En, "import_value_paste_deck_name") shouldBe "Pasted cards"
        }

        test("ko 팁은 상한 인자를 5,000으로 넘기면 5,000장으로 바뀐다") {
            val tip = ImportStringsXml.valueOf(StringsLocale.Ko, "import_tip_max_cards")

            ImportStringsXml.render(tip, "5,000") shouldBe "한 번에 최대 5,000장까지 가져올 수 있어요."
        }

        test("ko 하위 덱 상한 초과 요약은 상한 인자를 5,000으로 넘기면 최대 5,000장으로 바뀐다") {
            val summary = ImportStringsXml.valueOf(StringsLocale.Ko, "import_summary_over_limit")

            ImportStringsXml.render(summary, "12,400", "5,000") shouldBe "12,400장 (초과) / 최대 5,000장"
        }

        test("ko 텍스트 파일 상한 초과 사유는 상한 인자를 5,000으로 넘기면 장수와 줄 수가 함께 바뀐다") {
            val reason = ImportStringsXml.valueOf(StringsLocale.Ko, "import_reason_preview_over_limit_text")

            ImportStringsXml.render(reason, "5,000") shouldBe
                "한 번에 5,000장까지 가져올 수 있어요. 파일을 5,000줄 이하로 나눠 다시 골라 주세요."
        }

        test("ko 항목 고르기 읽기 문구에 설계 예시 인자를 넣으면 설계 예시 문장이 된다") {
            val picked = ImportStringsXml.valueOf(StringsLocale.Ko, "import_a11y_pick_fields")

            ImportStringsXml.render(picked, "뒷면", "20", "4") shouldBe "뒷면에 넣을 항목 고르기, 20개 중 4개 선택됨"
        }

        test("ko 순서 있는 선택 읽기 문구에 설계 예시 인자를 넣으면 설계 예시 문장이 된다") {
            val selected = ImportStringsXml.valueOf(StringsLocale.Ko, "import_a11y_field_selected")

            ImportStringsXml.render(selected, "뜻", "뒷면", "1") shouldBe "뜻, 선택됨, 뒷면 1번째"
        }

        test("ko 다른 면 항목 이동 알림에 설계 예시 인자를 넣으면 설계 예시 문장이 된다") {
            val moved = ImportStringsXml.valueOf(StringsLocale.Ko, "import_a11y_field_moved")

            ImportStringsXml.render(moved, "앞면", "뒷면", "5") shouldBe "앞면에서 빼고 뒷면 5번째로 넣었어요"
        }
    })
