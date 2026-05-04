package com.whatever.caro.core.messaging

internal object IosMessagingBridgeHolder {
    @Volatile
    var bridge: IosMessagingBridge? = null
        private set

    fun set(bridge: IosMessagingBridge) {
        this.bridge = bridge
    }
}

fun registerIosMessagingBridge(bridge: IosMessagingBridge) {
    IosMessagingBridgeHolder.set(bridge)
}
