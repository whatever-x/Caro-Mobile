@file:Suppress("ktlint:standard:filename")

package com.whatever.caro.core.remote.di.qualifier

sealed interface NetworkClient {
    enum class Caro {
        AUTH,
        NON_AUTH,
    }
}
