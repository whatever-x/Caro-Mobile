package com.whatever.caro.feature.importer.mvi

/** 화면이 단계별 상태 보존 키로 `step::class.simpleName`을 쓰므로 enum이 아니라 객체로 둔다. */
sealed interface ImportStep {
    data object Source : ImportStep

    data object Paste : ImportStep

    data object SubdeckSelect : ImportStep

    data object SheetSelect : ImportStep

    data object TextCheck : ImportStep

    data object Mapping : ImportStep

    data object Preview : ImportStep

    data object DeckInfo : ImportStep

    data object CreatingDeck : ImportStep

    data object Saving : ImportStep
}
