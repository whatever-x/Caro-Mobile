package com.whatever.caro.feature.importer.logic

import com.whatever.caro.feature.importer.mvi.SavingProgress

internal enum class SavingProgressStyle {
    /** 첫 응답 전. 앞쪽이 흐르는 막대다. */
    Indeterminate,

    /** 첫 응답 전에 동작 줄이기가 켜진 경우. 움직이지 않는 막대다. */
    Static,

    Determinate,
}

internal fun savingProgressStyle(
    progress: SavingProgress,
    reduceMotion: Boolean,
): SavingProgressStyle =
    when {
        !progress.isIndeterminate -> SavingProgressStyle.Determinate
        reduceMotion -> SavingProgressStyle.Static
        else -> SavingProgressStyle.Indeterminate
    }
