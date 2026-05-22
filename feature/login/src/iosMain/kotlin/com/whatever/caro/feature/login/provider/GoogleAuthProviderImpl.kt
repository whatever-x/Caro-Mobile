@file:OptIn(ExperimentalForeignApi::class)

package com.whatever.caro.feature.login.provider

import GoogleSignIn.GIDConfiguration
import GoogleSignIn.GIDSignIn
import GoogleSignIn.kGIDSignInErrorCodeCanceled
import androidx.compose.runtime.Composable
import com.whatever.caro.feature.login.config.GoogleAuthConfig
import com.whatever.caro.feature.login.model.GoogleUser
import com.whatever.caro.feature.login.model.SocialAuthenticator
import com.whatever.caro.feature.login.model.SocialLoginResult
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import kotlin.coroutines.resume

class GoogleAuthProviderImpl : GoogleAuthProvider {
    @Composable
    override fun get(): SocialAuthenticator<GoogleUser> = GoogleAuthenticator()
}

private class GoogleAuthenticator : SocialAuthenticator<GoogleUser> {
    override suspend fun authenticate(): SocialLoginResult<GoogleUser> {
        return suspendCancellableCoroutine { continuation ->
            val windowScene =
                UIApplication.sharedApplication.connectedScenes.firstOrNull() as? UIWindowScene
            if (windowScene == null) {
                continuation.resume(SocialLoginResult.Failed)
                return@suspendCancellableCoroutine
            }
            val keyWindow =
                UIApplication.sharedApplication.windows
                    .firstOrNull { (it as UIWindow).isKeyWindow() } as? UIWindow
            val rootViewController = keyWindow?.rootViewController
            if (rootViewController == null) {
                continuation.resume(SocialLoginResult.Failed)
                return@suspendCancellableCoroutine
            }
            val configuration =
                GIDConfiguration(
                    clientID = GoogleAuthConfig.GID_CLIENT_ID,
                    serverClientID = GoogleAuthConfig.GID_WEB_CLIENT_ID,
                )
            GIDSignIn.sharedInstance.configuration = configuration
            GIDSignIn.sharedInstance.signInWithPresentingViewController(rootViewController) { result, error ->
                if (error != null) {
                    if (error.code == kGIDSignInErrorCodeCanceled) {
                        continuation.resume(SocialLoginResult.UserCancelled)
                        return@signInWithPresentingViewController
                    }
                    continuation.resume(SocialLoginResult.Failed)
                    return@signInWithPresentingViewController
                }
                val token = result?.user?.idToken?.tokenString
                if (token.isNullOrEmpty()) {
                    continuation.resume(SocialLoginResult.Failed)
                    return@signInWithPresentingViewController
                }
                continuation.resume(SocialLoginResult.Success(GoogleUser(idToken = token)))
            }
        }
    }
}
