package com.whatever.caro.composeApp.di

import com.whatever.caro.core.navigator.entries.CreateDeckEntry
import com.whatever.caro.core.navigator.entries.CreateProfileEntry
import com.whatever.caro.core.navigator.entries.HomeEntry
import com.whatever.caro.core.navigator.entries.LoginEntry
import com.whatever.caro.core.navigator.entries.SplashEntry
import com.whatever.caro.feature.deck.CreateDeckRoute
import com.whatever.caro.feature.home.HomeViewModel
import com.whatever.caro.feature.home.route.HomeRoute
import com.whatever.caro.feature.login.LoginRoute
import com.whatever.caro.feature.profile.CreateProfileRoute
import com.whatever.caro.feature.splash.route.SplashRoute
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class)
val navEntryModule: Module =
    module {
        navigation<SplashEntry> {
            SplashRoute(
                viewModel = koinViewModel(),
                navDispatcher = get(),
            )
        }

        navigation<LoginEntry> {
            LoginRoute(
                viewModel = koinViewModel(),
                navDispatcher = get(),
            )
        }

        navigation<CreateProfileEntry> {
            CreateProfileRoute(
                viewModel = koinViewModel(),
                navDispatcher = get(),
            )
        }

        navigation<CreateDeckEntry> {
            CreateDeckRoute(
                viewModel = koinViewModel(),
                navDispatcher = get(),
            )
        }

        navigation<HomeEntry> { navKey ->
            HomeRoute(
                viewModel = koinViewModel<HomeViewModel> { parametersOf(navKey) },
                navDispatcher = get(),
            )
        }
    }
