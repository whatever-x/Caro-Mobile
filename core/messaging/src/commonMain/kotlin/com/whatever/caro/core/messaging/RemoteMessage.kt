package com.whatever.caro.core.messaging

data class RemoteMessage(
    val messageId: String?,
    val title: String?,
    val body: String?,
    val data: Map<String, String>,
    val sentTime: Long,
)
