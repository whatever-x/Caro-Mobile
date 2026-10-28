package com.whatever.caro.composeApp

import androidx.compose.ui.window.ComposeUIViewController
import com.whatever.caro.core.inappreview.ProvideIosInAppReviewRequester
import platform.UIKit.UIViewController
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
fun mainViewController(onRequestStoreReview: (UIViewController) -> Unit) =
    ComposeUIViewController {
        ProvideIosInAppReviewRequester(onRequestStoreReview) {
            CaroApp()
        }
    }
