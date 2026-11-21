package com.whatever.caro.feature.importer.mvi

/** 설정 화면의 [적용]이나 [미리보기]를 끈 사유다. 문구는 화면이 고른다. */
enum class SettingsReason {
    SubdeckOverLimit,
    SheetEmpty,
    SheetOverLimit,
    NoTypeEnabled,
    NoCards,

    /** 구분 기준이나 카드 구성을 바꿔 만든 카드가 상한을 넘었다. */
    TooManyCards,
}
