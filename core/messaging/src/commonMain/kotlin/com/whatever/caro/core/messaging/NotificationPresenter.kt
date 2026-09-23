package com.whatever.caro.core.messaging

interface NotificationPresenter {
    fun show(
        title: String,
        body: String,
    )
}
