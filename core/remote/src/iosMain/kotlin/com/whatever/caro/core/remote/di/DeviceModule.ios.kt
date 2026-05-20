package com.whatever.caro.core.remote.di

import com.whatever.caro.core.remote.network.device.DeviceIdProvider
import com.whatever.caro.core.remote.network.device.IosDeviceIdProvider
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

actual val deviceModule: Module =
    module {
        single<IosDeviceIdProvider>() bind DeviceIdProvider::class
    }
