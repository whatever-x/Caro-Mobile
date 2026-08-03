package com.whatever.caro.composeApp

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal actual fun rememberAppExit(): () -> Unit {
    val activity = LocalActivity.current
    return remember(activity) { { activity?.finish() } }
}
