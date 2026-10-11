package com.whatever.caro.core.designsystem

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ImportCopyTest :
    FunSpec({
        importKoCopy.forEach { (key, expected) ->
            test("ko $key 문구는 Figma 문구와 같다") {
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

        test("ko 선택 요약의 상한은 상한 인자를 5,000으로 넘기면 최대 5,000장으로 바뀐다") {
            val limit = ImportStringsXml.valueOf(StringsLocale.Ko, "import_summary_limit")

            ImportStringsXml.render(limit, "5,000") shouldBe "/ 최대 5,000장"
        }

        test("ko 선택 요약의 초과 수치는 장수 뒤에 (초과)가 붙는다") {
            val over = ImportStringsXml.valueOf(StringsLocale.Ko, "import_summary_over")

            ImportStringsXml.render(over, "12,400") shouldBe "12,400장 (초과)"
        }

        test("ko 미리보기 상한 초과 수치는 상한 인자를 5,000으로 넘기면 5,000장 넘음이 된다") {
            val over = ImportStringsXml.valueOf(StringsLocale.Ko, "import_summary_importable_over_limit")

            ImportStringsXml.render(over, "5,000") shouldBe "5,000장 넘음"
        }

        test("ko 미리보기 요약의 가져올 카드 라벨과 장수 값은 따로 쓴다") {
            ImportStringsXml.valueOf(StringsLocale.Ko, "import_summary_importable_label") shouldBe "가져올 카드"
            ImportStringsXml.render(ImportStringsXml.valueOf(StringsLocale.Ko, "import_label_card_count"), "0") shouldBe "0장"
        }

        test("ko 지원하는 자료는 이름과 확장자 배지를 따로 두고 붙여넣기 배지만 한글이 들어간다") {
            ImportStringsXml.valueOf(StringsLocale.Ko, "import_label_format_quizlet") shouldBe "Quizlet 내보내기 텍스트"
            ImportStringsXml.valueOf(StringsLocale.Ko, "import_chip_format_quizlet") shouldBe ".txt · 붙여넣기"
            ImportStringsXml.valueOf(StringsLocale.Ko, "import_chip_format_apkg") shouldBe ".apkg"
        }

        test("ko 붙여넣기 열 안내는 두 문장을 한 문단으로 둔다") {
            ImportStringsXml.valueOf(StringsLocale.Ko, "import_hint_paste_columns") shouldBe
                "칸은 탭, 쉼표, 세미콜론으로 나눠 주세요. 어느 칸을 앞뒷면에 넣을지는 뒤에서 골라요."
        }

        test("ko 하위 덱 선택과 시트 선택 부제는 Figma 문구다") {
            ImportStringsXml.valueOf(StringsLocale.Ko, "import_subtitle_subdeck") shouldBe "가져올 하위 덱을 골라 주세요"
            ImportStringsXml.valueOf(StringsLocale.Ko, "import_hint_subdeck_merge") shouldBe "고른 하위 덱은 한 덱으로 합쳐요."
            ImportStringsXml.valueOf(StringsLocale.Ko, "import_hint_subdeck_no_deck_notes") shouldBe "카드가 없는 노트예요"
            ImportStringsXml.valueOf(StringsLocale.Ko, "import_subtitle_sheet") shouldBe "가져올 시트를 골라 주세요"
        }

        test("ko 항목이 없는 노트 유형 부제에는 항목 수가 없다") {
            val subtitle = ImportStringsXml.valueOf(StringsLocale.Ko, "import_label_note_type_count")

            ImportStringsXml.render(subtitle, "40") shouldBe "노트 유형 · 40장"
        }

        test("ko 고른 항목 요약은 이름 목록 뒤에 외 N개를 붙인다") {
            val summary = ImportStringsXml.valueOf(StringsLocale.Ko, "import_label_fields_more")

            ImportStringsXml.render(summary, "뜻, 예문, 읽기", "1") shouldBe "뜻, 예문, 읽기 외 1개"
        }

        test("ko 항목 고르기 시트의 순서 라벨은 N번째다") {
            val order = ImportStringsXml.valueOf(StringsLocale.Ko, "import_label_order")

            ImportStringsXml.render(order, "1") shouldBe "1번째"
            ImportStringsXml.valueOf(StringsLocale.Ko, "import_hint_pick_order") shouldBe "고른 순서대로 줄을 바꿔 합쳐요."
        }

        test("ko 다른 면에 있는 항목 안내는 면 이름을 넣으면 앞면에 있어요가 된다") {
            val front = ImportStringsXml.valueOf(StringsLocale.Ko, "import_label_side_front")
            val inSide = ImportStringsXml.valueOf(StringsLocale.Ko, "import_label_in_side")

            ImportStringsXml.render(inSide, front) shouldBe "앞면에 있어요"
        }

        test("ko 카드 전체 보기의 구역 이름과 닫기 버튼은 Figma 문구다") {
            ImportStringsXml.valueOf(StringsLocale.Ko, "import_label_side_front") shouldBe "앞면"
            ImportStringsXml.valueOf(StringsLocale.Ko, "import_label_side_back") shouldBe "뒷면"
            ImportStringsXml.valueOf(StringsLocale.Ko, "import_button_close") shouldBe "닫기"
        }

        test("ko 미리보기 계산 실패 대화상자는 제목과 본문 뒤에 확인 버튼을 쓴다") {
            ImportStringsXml.valueOf(StringsLocale.Ko, "import_dialog_preview_failed_title") shouldBe "미리보기를 만들지 못했어요"
            ImportStringsXml.valueOf(StringsLocale.Ko, "import_dialog_preview_failed_body") shouldBe "잠시 뒤 다시 시도해 주세요."
            ImportStringsXml.valueOf(StringsLocale.Ko, "import_dialog_button_confirm") shouldBe "확인"
        }

        test("en 미리보기 계산 실패 대화상자 문구는 한글 없이 같은 의미의 초안이다") {
            ImportStringsXml.valueOf(StringsLocale.En, "import_dialog_preview_failed_title") shouldBe "The preview couldn’t be created"
            ImportStringsXml.valueOf(StringsLocale.En, "import_dialog_preview_failed_body") shouldBe "Please try again in a moment."
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

        test("ko 순서 있는 선택 상태 문구에 설계 예시 인자를 넣으면 설계 예시 문장이 된다") {
            val selected = ImportStringsXml.valueOf(StringsLocale.Ko, "import_a11y_field_order_state")

            ImportStringsXml.render(selected, "뒷면", "1") shouldBe "선택됨, 뒷면 1번째"
        }

        test("ko 다른 면 항목 이동 알림에 설계 예시 인자를 넣으면 설계 예시 문장이 된다") {
            val moved = ImportStringsXml.valueOf(StringsLocale.Ko, "import_a11y_field_moved")

            ImportStringsXml.render(moved, "앞면", "뒷면", "5") shouldBe "앞면에서 빼고 뒷면 5번째로 넣었어요"
        }
    })
