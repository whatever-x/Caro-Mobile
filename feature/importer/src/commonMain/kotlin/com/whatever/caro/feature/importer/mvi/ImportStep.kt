package com.whatever.caro.feature.importer.mvi

sealed interface ImportStep {
    data object Source : ImportStep

    data object Paste : ImportStep

    data object Preview : ImportStep
}
