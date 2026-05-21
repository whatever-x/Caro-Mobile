package com.whatever.caro.core.data.di

import com.whatever.caro.core.data.repository.demo.DemoRepository
import com.whatever.caro.core.data.repository.demo.DemoRepositoryImpl
import com.whatever.caro.core.data.repository.fcm.FcmTokenRepository
import com.whatever.caro.core.data.repository.fcm.FcmTokenRepositoryImpl
import com.whatever.caro.core.data.repository.profile.ProfileRepository
import com.whatever.caro.core.data.repository.profile.ProfileRepositoryImpl
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val dataModule =
    module {
        single<DemoRepositoryImpl>() bind DemoRepository::class
        single<FcmTokenRepositoryImpl>() bind FcmTokenRepository::class
        single<ProfileRepository> { ProfileRepositoryImpl(profileDataSource = get()) }
    }
