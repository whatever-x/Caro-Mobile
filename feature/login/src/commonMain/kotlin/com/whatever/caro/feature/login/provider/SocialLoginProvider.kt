package com.whatever.caro.feature.login.provider

import androidx.compose.runtime.Composable
import com.whatever.caro.feature.login.model.SocialAuthenticator

interface SocialLoginProvider<T> {
    @Composable
    fun get(): SocialAuthenticator<T>
}
