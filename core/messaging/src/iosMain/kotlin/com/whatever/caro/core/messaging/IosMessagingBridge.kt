package com.whatever.caro.core.messaging

interface IosMessagingBridge {
    fun fetchToken(callback: (token: String?, error: String?) -> Unit)

    fun deleteToken(callback: (error: String?) -> Unit)

    fun subscribe(
        topic: String,
        callback: (error: String?) -> Unit,
    )

    fun unsubscribe(
        topic: String,
        callback: (error: String?) -> Unit,
    )
}
