package com.whatever.caro.composeApp.di

import com.whatever.caro.composeApp.toast.ToastControllerImpl
import com.whatever.caro.core.ui.toast.ToastController
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val toastModule =
    module {
        single<ToastControllerImpl>() bind ToastController::class
    }
