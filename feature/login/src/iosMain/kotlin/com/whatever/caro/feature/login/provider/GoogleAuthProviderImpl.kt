@file:OptIn(ExperimentalForeignApi::class)

package com.whatever.caro.feature.login.provider

import GoogleLoginBridge.GoogleLoginBridge
import androidx.compose.runtime.Composable
import com.whatever.caro.feature.login.model.GoogleUser
import com.whatever.caro.feature.login.model.SocialAuthenticator
import com.whatever.caro.feature.login.model.SocialLoginResult
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class GoogleAuthProviderImpl(
    private val bridge: GoogleLoginBridge,
) : GoogleAuthProvider {
    @Composable
    override fun get(): SocialAuthenticator<GoogleUser> = GoogleAuthenticator(bridge = bridge)
}

private class GoogleAuthenticator(
    private val bridge: GoogleLoginBridge,
) : SocialAuthenticator<GoogleUser> {
    override suspend fun authenticate(): SocialLoginResult<GoogleUser> =
        suspendCancellableCoroutine { coroutine ->
            bridge.requestWithSuccess(
                success = { idToken ->
                    if (idToken == null) {
                        coroutine.resume(SocialLoginResult.Failed)
                    } else {
                        coroutine.resume(SocialLoginResult.Success(authResult = GoogleUser(idToken = idToken)))
                    }
                },
                failure = {
                    coroutine.resume(SocialLoginResult.Failed)
                },
                cancelled = {
                    coroutine.resume(SocialLoginResult.UserCancelled)
                },
            )
        }
}
