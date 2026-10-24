package com.whatever.caro.feature.importer.mvi

/** 단계 위에 덮는 읽기 표시. 읽기가 단계 전환마다 생기므로 단계가 아니라 별도 상태로 둔다. */
sealed interface ImportLoading {
    /** [fileName]이 null이면 붙여 넣은 내용을 읽는 중이다. */
    data class Parsing(
        val fileName: String?,
    ) : ImportLoading

    /** 하위 덱, 시트, 제목 행 선택 뒤 다시 읽는 중이다. 원본은 늘 파일이다. */
    data class Reparsing(
        val fileName: String,
    ) : ImportLoading

    data object BuildingPreview : ImportLoading

    /** 텍스트 확인의 구분자나 제목 행을 바꿔 다시 나누는 중이다. 오버레이 대신 결과 표 자리에만 그린다. */
    data object TextReparsing : ImportLoading
}
