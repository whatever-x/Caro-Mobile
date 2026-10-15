package com.whatever.caro.core.messaging

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal object MessagingEventBus {
    private val mutableTokenFlow = MutableStateFlow("")

    val tokenFlow: StateFlow<String> = mutableTokenFlow.asStateFlow()

    fun publishToken(token: String) {
        mutableTokenFlow.tryEmit(token)
    }
}
