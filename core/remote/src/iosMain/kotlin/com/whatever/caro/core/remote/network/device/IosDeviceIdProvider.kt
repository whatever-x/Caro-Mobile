package com.whatever.caro.core.remote.network.device

import platform.UIKit.UIDevice

internal class IosDeviceIdProvider : DeviceIdProvider {
    override fun get(): String =
        UIDevice.Companion.currentDevice.identifierForVendor
            ?.UUIDString
            .orEmpty()
}
