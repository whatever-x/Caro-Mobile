package com.whatever.caro.core.data.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module
@ComponentScan("com.whatever.caro.core.data.repository")
class RepositoryModule