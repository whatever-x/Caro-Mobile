package com.whatever.caro.core.designsystem

private val importParseErrorDialogCopy: Map<String, String> =
    mapOf(
        "import_dialog_file_too_large_title" to "파일이 너무 커요",
        "import_dialog_file_too_large_body" to "100MB까지 가져올 수 있어요.",
        "import_dialog_file_too_large_apkg" to "Anki에서 미디어를 빼거나 하위 덱별로 나눠 내보내 주세요.",
        "import_dialog_file_too_large_xlsx" to "시트나 행을 나눠 여러 파일로 저장해 주세요.",
        "import_dialog_file_too_large_text" to "파일을 나눠 다시 골라 주세요.",
        "import_dialog_unsupported_title" to "가져올 수 없는 파일이에요",
        "import_dialog_unsupported_xls" to "Excel에서 .xlsx로 저장해 주세요.",
        "import_dialog_unsupported_xlsx" to "암호가 걸려 있다면 풀고, Excel에서 .xlsx로 다시 저장해 주세요.",
        "import_dialog_unsupported_apkg" to "Anki 내보내기에서 이전 버전 호환 옵션을 켜고 다시 내보내 주세요.",
        "import_dialog_unsupported_generic" to "지원하는 파일(.apkg, .txt, .csv, .tsv, .xlsx)을 골라 주세요.",
        "import_dialog_corrupt_title" to "파일을 열 수 없어요",
        "import_dialog_corrupt_body" to "파일이 손상되었을 수 있어요.",
        "import_dialog_corrupt_apkg" to "Anki에서 다시 내보내 주세요.",
        "import_dialog_corrupt_xlsx" to "Excel에서 다시 저장해 주세요.",
        "import_dialog_encoding_title" to "글자를 읽을 수 없어요",
        "import_dialog_encoding_body" to "파일을 UTF-8로 저장한 뒤 다시 골라 주세요.",
        "import_dialog_read_failed_title" to "파일을 읽지 못했어요",
        "import_dialog_read_failed_body" to "파일을 나눠 다시 골라 주세요.",
        "import_dialog_too_many_columns_body" to "열이 너무 많아요. 필요한 열만 남겨 다시 저장해 주세요.",
        "import_dialog_paste_failed_title" to "내용을 읽지 못했어요",
        "import_dialog_paste_failed_body" to "내용을 나눠 다시 붙여 넣어 주세요.",
        "import_dialog_empty_result_title" to "가져올 카드가 없어요",
        "import_dialog_empty_result_body" to "카드로 만들 내용이 있는지 확인해 주세요.",
    )

private val importFlowDialogCopy: Map<String, String> =
    mapOf(
        "import_dialog_discard_title" to "가져온 내용을 버릴까요?",
        "import_dialog_discard_body" to "자료 가져오기로 돌아가면 읽은 내용이 사라져요.",
        "import_dialog_deck_create_failed_title" to "덱을 만들지 못했어요",
        "import_dialog_deck_create_failed_body" to "네트워크를 확인하고 다시 시도해 주세요.",
        "import_dialog_stop_title" to "가져오기를 멈출까요?",
        "import_dialog_stop_body" to "저장 중인 카드까지만 덱에 남고, 나머지는 이어서 가져올 수 없어요.",
        "import_dialog_paused_title" to "저장이 잠시 멈췄어요",
        "import_dialog_partial_failure_title" to "일부 카드를 저장하지 못했어요",
        "import_dialog_key_conflict_title" to "카드를 더 저장할 수 없어요",
        "import_dialog_deck_unavailable_title" to "덱을 찾을 수 없어요",
        "import_dialog_deck_unavailable_body" to "다른 기기에서 덱이 삭제됐을 수 있어요.",
        "import_dialog_saved_cards" to "%1\$s장까지 저장했어요.",
        "import_dialog_saved_none" to "아직 저장한 카드가 없어요.",
        "import_dialog_cause_check_network" to "네트워크를 확인하고 다시 시도해 주세요.",
        "import_dialog_cause_try_later" to "잠시 뒤 다시 시도해 주세요.",
        "import_dialog_cause_retry_or_view_deck" to "다시 시도하거나 저장한 카드로 덱을 볼 수 있어요.",
        "import_dialog_cause_retry_or_view_empty_deck" to "다시 시도하거나 빈 덱을 볼 수 있어요.",
        "import_dialog_cause_view_deck" to "저장한 카드로 덱을 볼 수 있어요.",
        "import_dialog_cause_empty_deck_deletable" to "빈 덱은 덱 상세에서 지울 수 있어요.",
    )

private val importDialogButtonCopy: Map<String, String> =
    mapOf(
        "import_dialog_button_confirm" to "확인",
        "import_dialog_button_discard" to "버리기",
        "import_dialog_button_cancel" to "취소",
        "import_dialog_button_stop" to "멈추기",
        "import_dialog_button_continue" to "계속 가져오기",
        "import_dialog_button_retry" to "다시 시도",
        "import_dialog_button_stop_and_view_deck" to "멈추고 덱 보기",
        "import_dialog_button_view_deck" to "덱 보기",
        "import_dialog_button_go_home" to "홈으로",
    )

private val importMessageCopy: Map<String, String> =
    mapOf(
        "import_snackbar_completed" to "카드 %1\$s장을 가져왔어요",
        "import_snackbar_partial" to "카드 %1\$s장 중 %2\$s장을 가져왔어요",
        "import_snackbar_empty_deck" to "카드를 가져오지 못해 빈 덱이 남았어요",
        "import_snackbar_deck_invalid_input" to "덱을 만들지 못했어요. 이름과 설명을 확인해 주세요.",
        "import_snackbar_deck_create_failed" to "덱을 만들지 못했어요. 잠시 뒤 다시 시도해 주세요.",
        "import_sheet_manual_title" to "직접 만들기",
        "import_sheet_manual_body" to "카드를 한 장씩 입력해 덱을 만들어요",
        "import_sheet_import_title" to "자료 가져오기",
        "import_sheet_import_body" to "Anki, Quizlet, 엑셀 자료로 덱을 만들어요",
    )

private val importAccessibilityCopy: Map<String, String> =
    mapOf(
        "import_a11y_field_selected" to "%1\$s, 선택됨, %2\$s %3\$s번째",
        "import_a11y_field_unavailable" to "사용할 수 없음",
        "import_a11y_pick_fields" to "%1\$s에 넣을 항목 고르기, %2\$s개 중 %3\$s개 선택됨",
        "import_a11y_field_moved" to "%1\$s에서 빼고 %2\$s %3\$s번째로 넣었어요",
        "import_a11y_disabled_with_reason" to "%1\$s, 사용할 수 없음, %2\$s",
        "import_a11y_filter_result" to "%1\$s %2\$s장",
        "import_a11y_saved_progress" to "%1\$s장 중 %2\$s장 저장됨",
        "import_a11y_table_row" to "%1\$s행, %2\$s",
        "import_a11y_image_occlusion" to "%1\$s, 노트 유형 %2\$s장, 이미지 가리기 카드는 가져올 수 없어요.",
    )

// 위쪽 표들을 모으는 값이라 맨 아래에 둔다. 앞에 두면 초기화 전 null을 읽는다.
internal val importDialogCopy: Map<String, String> =
    importParseErrorDialogCopy +
        importFlowDialogCopy +
        importDialogButtonCopy +
        importMessageCopy +
        importAccessibilityCopy

internal val importKoCopy: Map<String, String> = importScreenCopy + importDialogCopy
