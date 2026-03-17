package com.whatever.caro.feature.login.di

import GoogleLoginBridge.GoogleLoginBridge
import com.whatever.caro.feature.login.provider.GoogleAuthProvider
import com.whatever.caro.feature.login.provider.GoogleAuthProviderImpl
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.module.Module
import org.koin.dsl.module

@OptIn(ExperimentalForeignApi::class)
actual val loginModule: Module = module {
    factory<GoogleAuthProvider> { GoogleAuthProviderImpl(bridge = GoogleLoginBridge()) }
}