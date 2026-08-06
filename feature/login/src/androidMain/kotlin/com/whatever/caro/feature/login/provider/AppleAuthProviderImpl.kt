package com.whatever.caro.feature.login.provider

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthWebException
import com.google.firebase.auth.OAuthCredential
import com.google.firebase.auth.OAuthProvider
import com.whatever.caro.feature.login.model.AppleUser
import com.whatever.caro.feature.login.model.SocialAuthenticator
import com.whatever.caro.feature.login.model.SocialLoginResult
import io.github.aakira.napier.Napier
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AppleAuthProviderImpl : AppleAuthProvider {
    @Composable
    override fun get(): SocialAuthenticator<AppleUser> {
        val activity = LocalActivity.current
        return remember(activity) {
            AppleAuthenticator(activity = activity, auth = FirebaseAuth.getInstance())
        }
    }
}

enum class FirebaseAuthWebExceptionCode {
    ERROR_WEB_CONTEXT_CANCELED,
}

private class AppleAuthenticator(
    private val activity: Activity?,
    private val auth: FirebaseAuth,
) : SocialAuthenticator<AppleUser> {
    override suspend fun authenticate(): SocialLoginResult<AppleUser> {
        return suspendCancellableCoroutine { coroutine ->
            if (activity == null) {
                coroutine.resume(SocialLoginResult.Failed)
                return@suspendCancellableCoroutine
            }

            val provider = OAuthProvider.newBuilder("apple.com")

            val signInTask =
                auth.pendingAuthResult
                    ?: auth.startActivityForSignInWithProvider(activity, provider.build())

            signInTask
                .addOnSuccessListener { authResult ->
                    val credential = authResult.credential as? OAuthCredential
                    val idToken = credential?.idToken ?: ""
                    if (idToken.isEmpty()) {
                        Napier.e { "apple login: idToken is empty (credential=${authResult.credential})" }
                        coroutine.resume(SocialLoginResult.Failed)
                    } else {
                        coroutine.resume(SocialLoginResult.Success(AppleUser(idToken = idToken)))
                    }
                }.addOnFailureListener { e ->
                    val isCancelled =
                        e is FirebaseAuthWebException &&
                            e.errorCode == FirebaseAuthWebExceptionCode.ERROR_WEB_CONTEXT_CANCELED.name
                    if (isCancelled) {
                        coroutine.resume(SocialLoginResult.UserCancelled)
                    } else {
                        Napier.e(throwable = e) {
                            "apple login failed: ${(e as? FirebaseAuthWebException)?.errorCode ?: e::class.simpleName}"
                        }
                        coroutine.resume(SocialLoginResult.Failed)
                    }
                }
        }
    }
}
