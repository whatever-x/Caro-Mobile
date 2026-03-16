package com.whatever.caro.composeApp.di

import com.whatever.caro.feature.login.di.loginModule
import org.koin.core.annotation.KoinApplication
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.includes
import org.koin.ksp.generated.startKoin

@KoinApplication(modules = [AppModule::class])
object KoinApp

fun initKoin(configuration: KoinAppDeclaration? = null) {
    KoinApp.startKoin {
        includes(configuration)
        modules(
            navigationModule,
            loginModule,
        )
    }
}
