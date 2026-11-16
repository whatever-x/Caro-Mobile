package com.whatever.caro.feature.setting.platform

import androidx.compose.runtime.Composable

@Composable
internal expect fun InquiryLifecycleEffect(
    onBackground: () -> Unit,
    onForeground: () -> Unit,
)

@Composable
internal expect fun rememberOpenInquiry(): suspend (String) -> Boolean

/** Keeps accepted inquiry state when the current UI host is being recreated. */
@Composable
internal expect fun rememberShouldPreserveInquiryOnDispose(): () -> Boolean
