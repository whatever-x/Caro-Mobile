package com.whatever.caro.core.data.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module

@Module
@ComponentScan("com.whatever.caro.core.data.repository")
@Configuration
class RepositoryModule
