package com.whatever.caro.feature.importer.mvi

import com.whatever.caro.core.viewmodel.contract.UiSideEffect

sealed interface ImportDeckSideEffect : UiSideEffect {
    data object OpenFilePicker : ImportDeckSideEffect

    data object NavigateBack : ImportDeckSideEffect
}
