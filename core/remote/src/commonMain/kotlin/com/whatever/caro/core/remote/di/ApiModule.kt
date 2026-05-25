package com.whatever.caro.core.remote.di

import com.whatever.caro.core.remote.api.AuthApi
import com.whatever.caro.core.remote.api.ProfileApi
import com.whatever.caro.core.remote.api.createAuthApi
import com.whatever.caro.core.remote.api.createProfileApi
import com.whatever.caro.core.remote.di.qualifier.NetworkClient
import de.jensklingenberg.ktorfit.Ktorfit
import org.koin.core.qualifier.named
import org.koin.dsl.module

val apiModule =
    module {
        single<AuthApi>(named(NetworkClient.Caro.NON_AUTH)) {
            get<Ktorfit>(named(NetworkClient.Caro.NON_AUTH)).createAuthApi()
        }

        single<AuthApi>(named(NetworkClient.Caro.AUTH)) {
            get<Ktorfit>(named(NetworkClient.Caro.AUTH)).createAuthApi()
        }

        single<ProfileApi> {
            get<Ktorfit>(named(NetworkClient.Caro.AUTH)).createProfileApi()
        }
    }
