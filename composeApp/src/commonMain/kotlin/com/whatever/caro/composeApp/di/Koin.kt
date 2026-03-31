package com.whatever.caro.composeApp.di

import org.koin.core.annotation.KoinApplication
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.includes
import org.koin.plugin.module.dsl.startKoin

@KoinApplication
object KoinApp

fun initKoin(configuration: KoinAppDeclaration? = null) {
    startKoin<KoinApp> {
        includes(configuration)
        modules(
            navigationModule,
        )
    }
}
