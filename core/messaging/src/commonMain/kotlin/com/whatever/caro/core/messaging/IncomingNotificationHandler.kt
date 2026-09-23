package com.whatever.caro.core.messaging

class IncomingNotificationHandler(
    private val presenter: NotificationPresenter,
) {
    fun onReceived(
        title: String?,
        body: String?,
    ) {
        if (title.isNullOrBlank() && body.isNullOrBlank()) return
        presenter.show(title.orEmpty(), body.orEmpty())
    }
}
