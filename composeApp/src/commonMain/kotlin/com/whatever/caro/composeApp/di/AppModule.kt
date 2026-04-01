package com.whatever.caro.composeApp.di

import com.whatever.caro.core.data.di.RepositoryModule
import com.whatever.caro.core.navigator.di.NavigatorModule
import com.whatever.caro.core.remote.di.NetworkModule
import com.whatever.caro.core.remote.di.RemoteModule
import com.whatever.caro.feature.home.di.HomeModule
import com.whatever.caro.feature.login.di.LoginModule
import com.whatever.caro.feature.profile.di.ProfileModule
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module

@Configuration
@Module(
    includes = [
        // :core
        NavigatorModule::class,
        RepositoryModule::class,
        RemoteModule::class,
        NetworkModule::class,

        // :feature
        HomeModule::class,
        LoginModule::class,
        ProfileModule::class,
    ],
)
class AppModule
