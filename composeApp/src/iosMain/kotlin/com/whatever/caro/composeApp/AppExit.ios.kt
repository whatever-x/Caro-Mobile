package com.whatever.caro.composeApp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal actual fun rememberAppExit(): () -> Unit = remember { {} }
