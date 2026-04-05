package com.whatever.caro.core.data.di

import com.whatever.caro.core.data.repository.demo.DemoRepository
import com.whatever.caro.core.data.repository.demo.DemoRepositoryImpl
import org.koin.dsl.module

val dataModule =
    module {
        single<DemoRepository> { DemoRepositoryImpl(get()) }
    }
