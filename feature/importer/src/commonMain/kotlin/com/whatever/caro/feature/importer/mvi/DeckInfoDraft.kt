package com.whatever.caro.feature.importer.mvi

/** 덱 정보 시트의 입력이다. [적용]해야 덱 이름과 설명으로 옮겨지고 닫으면 버린다. */
data class DeckInfoDraft(
    val name: String,
    val description: String,
) {
    /** 시트 안에는 사유 줄이 없어 버튼만 끈다. */
    val isApplyEnabled: Boolean
        get() = name.isNotBlank() && description.isNotBlank()
}
