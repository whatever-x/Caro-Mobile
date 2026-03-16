package com.whatever.caro.feature.login.di

import com.whatever.caro.feature.login.provider.GoogleAuthProvider
import com.whatever.caro.feature.login.provider.GoogleAuthProviderImpl
import org.koin.core.module.Module
import org.koin.dsl.module

actual val loginModule: Module = module {
    factory<GoogleAuthProvider> { GoogleAuthProviderImpl() }
}