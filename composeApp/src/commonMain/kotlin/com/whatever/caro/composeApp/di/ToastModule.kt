package com.whatever.caro.composeApp.di

import com.whatever.caro.composeApp.snackbar.SnackbarControllerImpl
import com.whatever.caro.core.ui.snackbar.SnackbarController
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val snackbar =
    module {
        single<SnackbarControllerImpl>() bind SnackbarController::class
    }
