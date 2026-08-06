package com.whatever.caro.core.messaging

import android.content.Intent

object IntentMessagingPublisher {
    /**
     * Android에서 앱 실행 시나 실행중 Notification을 클릭시 실행
     * MessagingEventBus.publishMessage()를 통해서 메세지를 collect하고 있는 flow에 보내기 가능
     * */
    fun publishFromIntent(intent: Intent?) {
        val extras = intent?.extras ?: return
        if (extras.containsKey(MESSAGE_VALUE_KEY).not()) return
        MessagingEventBus.publishMessage(
            CloudMessage(anyValue = extras.getString(MESSAGE_VALUE_KEY)),
        )
    }

    private const val MESSAGE_VALUE_KEY = "anyValue"
}
