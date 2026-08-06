package com.whatever.caro.composeApp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.savedstate.serialization.SavedStateConfiguration
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.app_exit_dialog_button_cancel
import caromobile.core.designsystem.generated.resources.app_exit_dialog_button_exit
import caromobile.core.designsystem.generated.resources.app_exit_dialog_title
import com.whatever.caro.core.data.repository.fcm.FcmTokenRepository
import com.whatever.caro.core.designsystem.components.CaroDialog
import com.whatever.caro.core.designsystem.components.CaroDialogButton
import com.whatever.caro.core.designsystem.components.CaroSnackBarHost
import com.whatever.caro.core.designsystem.components.CaroSnackbar
import com.whatever.caro.core.designsystem.components.showSnackbarMessage
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.messaging.MessagingClient
import com.whatever.caro.core.model.auth.AuthSessionEvent
import com.whatever.caro.core.model.auth.AuthSessionEventBus
import com.whatever.caro.core.navigator.contract.NavCommand
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.core.navigator.entries.CardDetailEntry
import com.whatever.caro.core.navigator.entries.CreateCardEntry
import com.whatever.caro.core.navigator.entries.CreateDeckEntry
import com.whatever.caro.core.navigator.entries.CreateProfileEntry
import com.whatever.caro.core.navigator.entries.DeckDetailEntry
import com.whatever.caro.core.navigator.entries.DeleteCardsEntry
import com.whatever.caro.core.navigator.entries.EditCardEntry
import com.whatever.caro.core.navigator.entries.EditDeckEntry
import com.whatever.caro.core.navigator.entries.EditProfileEntry
import com.whatever.caro.core.navigator.entries.HomeEntry
import com.whatever.caro.core.navigator.entries.LearningEntry
import com.whatever.caro.core.navigator.entries.LoginEntry
import com.whatever.caro.core.navigator.entries.SettingEntry
import com.whatever.caro.core.navigator.entries.SplashEntry
import com.whatever.caro.core.ui.image.ConfigureCaroImageLoader
import com.whatever.caro.core.ui.snackbar.SnackbarController
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

private val HomeSnackbarBottomPadding = 88.dp

internal fun snackbarHostBottomPadding(currentDestination: NavKey?): Dp =
    if (currentDestination is HomeEntry) HomeSnackbarBottomPadding else 0.dp

@Composable
fun CaroApp(
    navDispatcher: NavigationDispatcher = koinInject(),
    authSessionEventBus: AuthSessionEventBus = koinInject(),
    snackbarController: SnackbarController = koinInject(),
    messagingClient: MessagingClient = koinInject(),
    fcmTokenRepository: FcmTokenRepository = koinInject(),
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
                            subclass(CardDetailEntry::class, CardDetailEntry.serializer())
                            subclass(EditCardEntry::class, EditCardEntry.serializer())
                            subclass(DeleteCardsEntry::class, DeleteCardsEntry.serializer())
                            subclass(HomeEntry::class, HomeEntry.serializer())
                            subclass(SettingEntry::class, SettingEntry.serializer())
                            subclass(EditDeckEntry::class, EditDeckEntry.serializer())
                            subclass(LearningEntry::class, LearningEntry.serializer())
                        }
                    }
            }
        }

    val backStack = rememberNavBackStack(savedStateConfiguration, SplashEntry)

    LaunchedEffect(messagingClient, fcmTokenRepository) {
        messagingClient.tokenFlow
            .filter(String::isNotBlank)
            .distinctUntilChanged()
            .collect(fcmTokenRepository::syncToken)
    }

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
                    navDispatcher.emit(NavCommand.ResetTo(LoginEntry))
                }
            }
        }
    }
    CaroTheme {
        val snackBarHostState = remember { SnackbarHostState() }
        val currentDestination = backStack.lastOrNull()
        val exitApp = rememberAppExit()
        var showExitDialog by remember { mutableStateOf(false) }
        val backState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)

        // 백스택 루트(로그인/홈 등)에서의 뒤로가기만 가로챈다. 하위 화면은 각 Route 의 핸들러가 먼저 처리한다.
        NavigationBackHandler(
            state = backState,
            isBackEnabled = backStack.size <= 1,
            onBackCompleted = { showExitDialog = true },
        )

        if (showExitDialog) {
            AppExitDialog(
                // iOS 의 exitApp 은 no-op 이라 먼저 닫지 않으면 다이얼로그가 남는다.
                onExit = {
                    showExitDialog = false
                    exitApp()
                },
                onCancel = { showExitDialog = false },
            )
        }
        val systemBarBackgroundRoles = systemBarBackgroundRoles(currentDestination)
        val snackbarBottomPadding = snackbarHostBottomPadding(currentDestination)

        LaunchedEffect(snackbarController, snackBarHostState) {
            snackbarController.messages.collect { snackbar ->
                showSnackbarMessage(
                    coroutineScope = this,
                    snackbarHostState = snackBarHostState,
                    message = snackbar.message,
                    style = snackbar.style,
                    duration = snackbar.duration,
                    actionLabel = snackbar.actionLabel,
                    onAction = snackbar.onAction,
                )
            }
        }

        CaroSystemBarBackground(roles = systemBarBackgroundRoles) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                snackbarHost = {
                    CaroSnackBarHost(
                        // Scaffold 는 systemBars 만 반영해 키보드가 올라오면 스낵바가 IME 뒤로 가려진다.
                        modifier =
                            Modifier
                                .imePadding()
                                .padding(bottom = snackbarBottomPadding),
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
}

@Composable
private fun AppExitDialog(
    onExit: () -> Unit,
    onCancel: () -> Unit,
) {
    CaroDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(Res.string.app_exit_dialog_title),
                color = CaroTheme.color.text.primary,
                style = CaroTheme.typography.heading2,
                textAlign = TextAlign.Center,
            )
        },
        content = {
            Spacer(modifier = Modifier.size(CaroTheme.spacing.l))
        },
        buttons = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
            ) {
                CaroDialogButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(Res.string.app_exit_dialog_button_exit),
                    backgroundColor = CaroTheme.color.surface.dangerous,
                    textColor = CaroTheme.color.text.dangerous,
                    onClick = onExit,
                )
                CaroDialogButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(Res.string.app_exit_dialog_button_cancel),
                    backgroundColor = CaroTheme.color.surface.tertiary,
                    textColor = CaroTheme.color.text.brand,
                    onClick = onCancel,
                )
            }
        },
    )
}
