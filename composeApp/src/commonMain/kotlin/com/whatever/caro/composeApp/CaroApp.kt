package com.whatever.caro.composeApp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration
import com.whatever.caro.core.designsystem.components.CaroSnackBarHost
import com.whatever.caro.core.designsystem.components.CaroSnackbar
import com.whatever.caro.core.designsystem.components.showSnackbarMessage
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.auth.AuthSessionEvent
import com.whatever.caro.core.model.auth.AuthSessionEventBus
import com.whatever.caro.core.navigator.contract.NavCommand
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.core.navigator.entries.CreateCardEntry
import com.whatever.caro.core.navigator.entries.CreateDeckEntry
import com.whatever.caro.core.navigator.entries.CreateProfileEntry
import com.whatever.caro.core.navigator.entries.DeckDetailEntry
import com.whatever.caro.core.navigator.entries.EditDeckEntry
import com.whatever.caro.core.navigator.entries.EditProfileEntry
import com.whatever.caro.core.navigator.entries.HomeEntry
import com.whatever.caro.core.navigator.entries.LoginEntry
import com.whatever.caro.core.navigator.entries.SettingEntry
import com.whatever.caro.core.navigator.entries.SplashEntry
import com.whatever.caro.core.ui.image.ConfigureCaroImageLoader
import com.whatever.caro.core.ui.snackbar.SnackbarController
import io.github.aakira.napier.Napier
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.compose.koinInject

@Composable
fun CaroApp(
    navDispatcher: NavigationDispatcher = koinInject(),
    authSessionEventBus: AuthSessionEventBus = koinInject(),
    snackbarController: SnackbarController = koinInject(),
) {
    ConfigureCaroImageLoader()

    val savedStateConfiguration =
        remember {
            SavedStateConfiguration {
                serializersModule =
                    SerializersModule {
                        polymorphic(NavKey::class) {
                            subclass(SplashEntry::class, SplashEntry.serializer())
                            subclass(LoginEntry::class, LoginEntry.serializer())
                            subclass(CreateProfileEntry::class, CreateProfileEntry.serializer())
                            subclass(EditProfileEntry::class, EditProfileEntry.serializer())
                            subclass(CreateDeckEntry::class, CreateDeckEntry.serializer())
                            subclass(DeckDetailEntry::class, DeckDetailEntry.serializer())
                            subclass(CreateCardEntry::class, CreateCardEntry.serializer())
                            subclass(HomeEntry::class, HomeEntry.serializer())
                            subclass(SettingEntry::class, SettingEntry.serializer())
                            subclass(EditDeckEntry::class, EditDeckEntry.serializer())
                        }
                    }
            }
        }

    val backStack = rememberNavBackStack(savedStateConfiguration, SplashEntry)

    LaunchedEffect(navDispatcher) {
        navDispatcher.commands.collect { command ->
            when (command) {
                is NavCommand.Back -> {
                    if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                }

                is NavCommand.To -> {
                    Napier.d { "To : ${command.key}" }
                    backStack.add(command.key)
                }

                is NavCommand.Replace -> {
                    if (backStack.isNotEmpty()) backStack.removeAt(backStack.lastIndex)
                    backStack.add(command.key)
                }

                is NavCommand.ResetTo -> {
                    backStack.clear()
                    backStack.add(command.key)
                }
            }
        }
    }

    LaunchedEffect(authSessionEventBus, navDispatcher) {
        authSessionEventBus.events.collect { event ->
            when (event) {
                AuthSessionEvent.Expired -> {
                    Napier.w { "Auth session expired → navigate to LoginEntry" }
                    if (backStack.first() == SplashEntry) {
                        navDispatcher.emit(NavCommand.ResetTo(LoginEntry))
                    } else {
                        // TODO: 팝업 처리 이후 emit
                    }
                }
            }
        }
    }
    CaroTheme {
        val snackBarHostState = remember { SnackbarHostState() }

        LaunchedEffect(snackbarController, snackBarHostState) {
            snackbarController.messages.collect { snackbar ->
                showSnackbarMessage(
                    coroutineScope = this,
                    snackbarHostState = snackBarHostState,
                    message = snackbar.message,
                    style = snackbar.style,
                    duration = snackbar.duration,
                )
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = {
                CaroSnackBarHost(
                    modifier = Modifier,
                    hostState = snackBarHostState,
                    snackbar = { snackbarData ->
                        CaroSnackbar(
                            snackbarData = snackbarData,
                        )
                    },
                )
            },
        ) { innerPadding ->
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues = innerPadding),
            ) {
                CaroNavHost(
                    backStack = backStack,
                )
            }
        }
    }
}
