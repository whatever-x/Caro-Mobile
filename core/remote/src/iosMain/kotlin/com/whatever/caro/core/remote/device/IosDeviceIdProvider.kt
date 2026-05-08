package com.whatever.caro.core.remote.device

import platform.UIKit.UIDevice

internal class IosDeviceIdProvider : DeviceIdProvider {
    override fun get(): String =
        UIDevice.currentDevice.identifierForVendor
            ?.UUIDString
            .orEmpty()
}
