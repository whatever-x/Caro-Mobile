package com.whatever.caro.feature.importer.mvi

/** 단계 위를 덮고 뒤로 가기만 받는 처리 중 표시다. 200ms 안에 끝나면 보이지 않게 하는 지연은 화면이 맡는다. */
sealed interface ImportLoading {
    /** provider에서 파일 이름을 받기 전에는 [fileName]이 null이다. */
    data class ReadingFile(
        val fileName: String?,
    ) : ImportLoading

    data object ReadingPaste : ImportLoading
}
