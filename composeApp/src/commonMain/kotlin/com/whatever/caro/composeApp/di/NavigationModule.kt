package com.whatever.caro.composeApp.di

import com.whatever.caro.core.navigator.entries.CreateCardEntry
import com.whatever.caro.core.navigator.entries.CreateDeckEntry
import com.whatever.caro.core.navigator.entries.CreateProfileEntry
import com.whatever.caro.core.navigator.entries.DeckDetailEntry
import com.whatever.caro.core.navigator.entries.EditCardEntry
import com.whatever.caro.core.navigator.entries.EditProfileEntry
import com.whatever.caro.core.navigator.entries.HomeEntry
import com.whatever.caro.core.navigator.entries.LoginEntry
import com.whatever.caro.core.navigator.entries.SettingEntry
import com.whatever.caro.core.navigator.entries.SplashEntry
import com.whatever.caro.feature.card.CreateCardViewModel
import com.whatever.caro.feature.card.EditCardViewModel
import com.whatever.caro.feature.card.route.CreateCardRoute
import com.whatever.caro.feature.card.route.EditCardRoute
import com.whatever.caro.feature.deck.CreateDeckRoute
import com.whatever.caro.feature.deck.detail.DeckDetailViewModel
import com.whatever.caro.feature.deck.detail.route.DeckDetailRoute
import com.whatever.caro.feature.home.HomeViewModel
import com.whatever.caro.feature.home.route.HomeRoute
import com.whatever.caro.feature.login.LoginRoute
import com.whatever.caro.feature.profile.create.CreateProfileRoute
import com.whatever.caro.feature.profile.edit.EditProfileRoute
import com.whatever.caro.feature.profile.edit.EditProfileViewModel
import com.whatever.caro.feature.setting.route.SettingRoute
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
                snackbarController = get(),
            )
        }

        navigation<CreateProfileEntry> {
            CreateProfileRoute(
                viewModel = koinViewModel(),
                navDispatcher = get(),
            )
        }

        navigation<EditProfileEntry> { navKey ->
            EditProfileRoute(
                viewModel = koinViewModel<EditProfileViewModel> { parametersOf(navKey.nickname) },
                navDispatcher = get(),
            )
        }

        navigation<CreateDeckEntry> {
            CreateDeckRoute(
                viewModel = koinViewModel(),
                navDispatcher = get(),
                snackbarController = get(),
            )
        }

        navigation<DeckDetailEntry> { navKey ->
            DeckDetailRoute(
                viewModel =
                    koinViewModel<DeckDetailViewModel> {
                        parametersOf(
                            navKey.payload.deck,
                        )
                    },
                navDispatcher = get(),
                snackbarController = get(),
            )
        }

        navigation<HomeEntry> { navKey ->
            HomeRoute(
                viewModel = koinViewModel<HomeViewModel> { parametersOf(navKey) },
                navDispatcher = get(),
            )
        }

        navigation<CreateCardEntry> { navKey ->
            CreateCardRoute(
                viewModel = koinViewModel<CreateCardViewModel> { parametersOf(navKey.payload.deckId) },
                navDispatcher = get(),
                snackbarController = get(),
            )
        }

        navigation<EditCardEntry> { navKey ->
            EditCardRoute(
                viewModel =
                    koinViewModel<EditCardViewModel> {
                        parametersOf(
                            navKey.payload.cardId,
                            navKey.payload.front,
                            navKey.payload.back,
                        )
                    },
                navDispatcher = get(),
                snackbarController = get(),
            )
        }

        navigation<SettingEntry> {
            SettingRoute(
                viewModel = koinViewModel(),
                navDispatcher = get(),
                snackbarController = get(),
            )
        }
    }
