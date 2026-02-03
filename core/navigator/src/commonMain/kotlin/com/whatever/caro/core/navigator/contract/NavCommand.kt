package com.whatever.caro.core.navigator.contract

import androidx.navigation3.runtime.NavKey

sealed interface NavCommand {

    data object Back : NavCommand

    data class To(
        val key: NavKey
    ) : NavCommand

    data class Replace(
        val key: NavKey,
    ) : NavCommand

    data class ResetTo(
        val key: NavKey
    ) : NavCommand

}