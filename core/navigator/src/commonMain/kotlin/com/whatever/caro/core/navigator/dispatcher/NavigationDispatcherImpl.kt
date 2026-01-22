package com.whatever.caro.core.navigator.dispatcher

import com.whatever.caro.core.navigator.contract.NavCommand
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import org.koin.core.annotation.Single

/**
 * internal 접근 제한자로 외부 모듈에서 NavigationDispatcherImpl에 대한 접근을 막는다
 */
@Single(binds = [NavigationDispatcher::class])
internal class NavigationDispatcherImpl : NavigationDispatcher {
    private val channel = Channel<NavCommand>(capacity = Channel.BUFFERED)

    override val commands = channel.receiveAsFlow()

    override suspend fun emit(command: NavCommand) {
        channel.send(command)
    }
}