package com.whatever.caro.core.remote.di

import com.whatever.caro.core.remote.api.AuthApi
import com.whatever.caro.core.remote.api.CardControllerApi
import com.whatever.caro.core.remote.api.DeckApi
import com.whatever.caro.core.remote.api.NicknameApi
import com.whatever.caro.core.remote.api.UserApi
import com.whatever.caro.core.remote.api.createAuthApi
import com.whatever.caro.core.remote.api.createCardControllerApi
import com.whatever.caro.core.remote.api.createDeckApi
import com.whatever.caro.core.remote.api.createNicknameApi
import com.whatever.caro.core.remote.api.createUserApi
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

        single<DeckApi> {
            get<Ktorfit>(named(NetworkClient.Caro.AUTH)).createDeckApi()
        }

        single<CardControllerApi> {
            get<Ktorfit>(named(NetworkClient.Caro.AUTH)).createCardControllerApi()
        }

        single<NicknameApi> {
            get<Ktorfit>(named(NetworkClient.Caro.AUTH)).createNicknameApi()
        }

        single<UserApi> {
            get<Ktorfit>(named(NetworkClient.Caro.AUTH)).createUserApi()
        }
    }
