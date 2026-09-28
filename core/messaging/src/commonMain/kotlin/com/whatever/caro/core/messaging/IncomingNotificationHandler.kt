package com.whatever.caro.core.messaging

class IncomingNotificationHandler(
    private val presenter: NotificationPresenter,
) {
    fun onReceived(
        title: String?,
        body: String?,
    ) {
        if (!shouldShowNotification(title, body)) return
        presenter.show(title.orEmpty(), body.orEmpty())
    }
}

internal fun shouldShowNotification(
    title: String?,
    body: String?,
): Boolean = !title.isNullOrBlank() || !body.isNullOrBlank()
