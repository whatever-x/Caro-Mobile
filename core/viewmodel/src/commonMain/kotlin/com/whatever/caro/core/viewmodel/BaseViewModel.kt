package com.whatever.caro.core.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatever.caro.core.viewmodel.contract.UiIntent
import com.whatever.caro.core.viewmodel.contract.UiSideEffect
import com.whatever.caro.core.viewmodel.contract.UiState
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

abstract class BaseViewModel<S : UiState, I : UiIntent, SE : UiSideEffect>(
    initialState: S,
) : ViewModel() {
    protected abstract suspend fun handleIntent(intent: I)

    private val _state = MutableStateFlow(initialState)
    val state = _state.asStateFlow()

    private val _sideEffect =
        Channel<SE>(
            capacity = Channel.BUFFERED,
            onBufferOverflow = BufferOverflow.SUSPEND,
        )
    val sideEffect = _sideEffect.receiveAsFlow()

    protected val currentState: S
        get() = _state.value

    protected val coroutineExceptionHandler =
        CoroutineExceptionHandler { _, throwable ->
            handleClientException(throwable)
        }

    fun intent(intent: I) {
        launch {
            handleIntent(intent)
        }
    }

    protected fun reduce(reduce: S.() -> S) {
        val state = currentState.reduce()
        _state.value = state
    }

    protected fun postSideEffect(sideEffect: SE) {
        viewModelScope.launch { _sideEffect.send(sideEffect) }
    }

    protected inline fun launch(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        crossinline action: suspend CoroutineScope.() -> Unit,
    ): Job =
        viewModelScope.launch(context + coroutineExceptionHandler, start = start) {
            action()
        }

    open fun handleClientException(throwable: Throwable) {
        Napier.e { "handleClientException = ${throwable.message}" }
    }
}
