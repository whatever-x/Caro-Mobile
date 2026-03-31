package com.whatever.caro.core.remote.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module

@Module(includes = [NetworkModule::class])
@ComponentScan("com.whatever.caro.core.remote")
@Configuration
class RemoteModule
