package com.whatever.caro.core.remote.di.qualifier

import org.koin.core.annotation.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class NetworkClient(
    val type: CaroClient,
)

enum class CaroClient {
    Auth,
    Default,
}
