package com.whatever.caro.core.remote.network.device

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings

internal class AndroidDeviceIdProvider(
    private val context: Context,
) : DeviceIdProvider {
    @SuppressLint("HardwareIds")
    override fun get(): String =
        Settings.Secure
            .getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            .orEmpty()
}
