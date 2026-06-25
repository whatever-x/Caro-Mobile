package com.whatever.caro.composeApp.toast

import com.whatever.caro.core.ui.toast.ToastController
import com.whatever.caro.core.ui.toast.ToastMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

internal class ToastControllerImpl : ToastController {
    private val channel =
        Channel<ToastMessage>(
            capacity = Channel.BUFFERED,
        )

    override val messages = channel.receiveAsFlow()

    override suspend fun show(message: ToastMessage) {
        channel.send(message)
    }
}
