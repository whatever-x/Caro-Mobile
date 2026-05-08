package com.whatever.caro.core.remote.di

import com.whatever.caro.core.remote.device.AndroidDeviceIdProvider
import com.whatever.caro.core.remote.device.DeviceIdProvider
import org.koin.core.module.Module
import org.koin.dsl.module

actual val deviceModule: Module =
    module {
        single<DeviceIdProvider> { AndroidDeviceIdProvider(context = get()) }
    }
