package com.whatever.caro.composeApp.initializer

import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import kotlin.experimental.ExperimentalNativeApi

@ExperimentalNativeApi
fun initNapier() {
    if (Platform.isDebugBinary) {
        Napier.base(DebugAntilog())
    }
}
