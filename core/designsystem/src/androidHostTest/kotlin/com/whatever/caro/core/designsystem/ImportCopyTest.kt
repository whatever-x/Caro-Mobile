package com.whatever.caro.core.designsystem

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ImportCopyTest : FunSpec() {
    init {
        // 인자 번호를 맞바꿔도 형식 인자 집합 검사는 통과하므로, 인자가 둘 이상인 import_* 키는 모두 이 표에서 서로 다른 값으로 그린 문장을 본다.
        listOf(
            ArgOrderCase(
                "import_content_description_field_moved",
                listOf("Front", "Back", "2"),
                "Front에서 빼고 Back 2번째로 넣었어요",
                "Moved from Front to Back, position 2",
            ),
            ArgOrderCase(
                "import_content_description_field_order_state",
                listOf("Back", "3"),
                "선택됨, Back 3번째",
                "Selected, Back, position 3",
            ),
            ArgOrderCase("import_content_description_filter_result", listOf("Edited", "12"), "Edited 12장", "Edited, 12 cards"),
            ArgOrderCase(
                "import_content_description_image_occlusion",
                listOf("Occlusion", "7"),
                "Occlusion, 7장, 가져올 수 없어요",
                "Occlusion, 7 cards, can’t be imported",
            ),
            ArgOrderCase(
                "import_content_description_pick_fields",
                listOf("Back", "20", "4"),
                "Back에 넣을 항목 고르기, 20개 중 4개 선택됨",
                "Choose items for Back, 4 of 20 selected",
            ),
            ArgOrderCase("import_content_description_saved_progress", listOf("120", "30"), "120장 중 30장 저장됨", "30 of 120 cards saved"),
            ArgOrderCase("import_content_description_table_row", listOf("2", "apple"), "2번째 줄, apple", "Row 2, apple"),
            ArgOrderCase("import_content_description_title_with_notice", listOf("Title", "Notice"), "Title, Notice", "Title, Notice"),
            ArgOrderCase("import_button_pick_fields", listOf("20", "4"), "항목 고르기 (20개 중 4개)", "Choose items (4 of 20)"),
            ArgOrderCase("import_label_fields_more", listOf("Meaning", "2"), "Meaning 외 2개", "Meaning and 2 more"),
            ArgOrderCase("import_label_saving_progress", listOf("30", "120"), "30 / 120장", "30 / 120 cards"),
            ArgOrderCase("import_label_table_cell", listOf("Front", "apple"), "Front: apple", "Front: apple"),
            ArgOrderCase("import_label_table_info", listOf("3", "120", "4"), "열 3개, 120줄 중 처음 4줄", "3 columns, first 4 of 120 rows"),
            ArgOrderCase(
                "import_label_table_info_truncated",
                listOf("25", "20", "120", "4"),
                "열 25개 중 20개, 120줄 중 처음 4줄",
                "20 of 25 columns, first 4 of 120 rows",
            ),
            ArgOrderCase("import_snackbar_partial", listOf("120", "30"), "카드 120장 중 30장을 가져왔어요", "30 of 120 cards imported"),
            ArgOrderCase("import_tag_subdeck_some", listOf("3", "1"), "3개 중 1개", "1 of 3"),
        ).forEach { case ->
            listOf(StringsLocale.Ko to case.ko, StringsLocale.En to case.en).forEach { (locale, expected) ->
                test("${locale.name.lowercase()} ${case.key} 문구는 서로 다른 인자를 번호에 맞는 자리에 넣는다") {
                    ImportStringsXml.render(ImportStringsXml.valueOf(locale, case.key), *case.args.toTypedArray()) shouldBe expected
                }
            }
        }
    }
}

private data class ArgOrderCase(
    val key: String,
    val args: List<String>,
    val ko: String,
    val en: String,
)
