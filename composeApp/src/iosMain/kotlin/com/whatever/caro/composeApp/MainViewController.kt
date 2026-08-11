package com.whatever.caro.composeApp

import androidx.compose.ui.window.ComposeUIViewController
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
fun mainViewController() =
    ComposeUIViewController {
        CaroApp()
    }
