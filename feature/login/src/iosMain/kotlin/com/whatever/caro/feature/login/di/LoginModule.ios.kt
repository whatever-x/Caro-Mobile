package com.whatever.caro.feature.login.di

import com.whatever.caro.feature.login.provider.AppleAuthProvider
import com.whatever.caro.feature.login.provider.AppleAuthProviderImpl
import com.whatever.caro.feature.login.provider.GoogleAuthProvider
import com.whatever.caro.feature.login.provider.GoogleAuthProviderImpl
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

@OptIn(ExperimentalForeignApi::class)
actual val socialModule: Module =
    module {
        single<GoogleAuthProviderImpl>() bind GoogleAuthProvider::class
        single<AppleAuthProviderImpl>() bind AppleAuthProvider::class
    }
