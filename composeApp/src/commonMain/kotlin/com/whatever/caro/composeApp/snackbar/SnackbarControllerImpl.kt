package com.whatever.caro.composeApp.snackbar

import com.whatever.caro.core.ui.snackbar.SnackBarMessage
import com.whatever.caro.core.ui.snackbar.SnackbarController
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

internal class SnackbarControllerImpl : SnackbarController {
    private val channel =
        Channel<SnackBarMessage>(
            capacity = Channel.BUFFERED,
        )

    override val messages = channel.receiveAsFlow()

    override suspend fun show(message: SnackBarMessage) {
        channel.send(message)
    }
}
