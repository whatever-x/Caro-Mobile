package com.whatever.caro.core.remote.di

import com.whatever.caro.core.remote.device.DeviceIdProvider
import com.whatever.caro.core.remote.device.IosDeviceIdProvider
import org.koin.core.module.Module
import org.koin.dsl.module

actual val deviceModule: Module =
    module {
        single<DeviceIdProvider> { IosDeviceIdProvider() }
    }
