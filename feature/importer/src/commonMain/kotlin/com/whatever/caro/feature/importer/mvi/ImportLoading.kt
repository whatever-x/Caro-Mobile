package com.whatever.caro.feature.importer.mvi

/** 단계 위를 덮고 [CreatingDeck] 말고는 뒤로 가기만 받는 처리 중 표시다. 200ms 안에 끝나면 보이지 않게 하는 지연은 화면이 맡는다. */
sealed interface ImportLoading {
    /** provider에서 파일 이름을 받기 전에는 [fileName]이 null이다. */
    data class ReadingFile(
        val fileName: String?,
    ) : ImportLoading

    data object ReadingPaste : ImportLoading

    /** 설정 화면의 [적용]이나 [미리보기] 뒤 미리보기를 만드는 중이다. 붙여넣기는 파일 이름이 없어 [fileName]이 null이다. */
    data class BuildingPreview(
        val fileName: String?,
    ) : ImportLoading

    /**
     * 미리보기 위에서 덱을 만드는 중이다. 단계를 바꾸지 않아야 실패했을 때 보던 미리보기 자리로 돌아온다.
     * 만들어졌는지 모르는 채 멈추면 사용자가 모르는 빈 덱이 남을 수 있어 뒤로 가기도 받지 않는다.
     */
    data object CreatingDeck : ImportLoading
}
