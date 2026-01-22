package com.whatever.caro.core.navigator.dispatcher

import com.whatever.caro.core.navigator.contract.NavCommand
import kotlinx.coroutines.flow.Flow

interface NavigationDispatcher {
    val commands: Flow<NavCommand>
    suspend fun emit(command: NavCommand)
}