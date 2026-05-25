package com.whatever.caro.feature.login.provider

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthWebException
import com.google.firebase.auth.OAuthCredential
import com.google.firebase.auth.OAuthProvider
import com.whatever.caro.feature.login.model.AppleUser
import com.whatever.caro.feature.login.model.SocialAuthenticator
import com.whatever.caro.feature.login.model.SocialLoginResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AppleAuthProviderImpl : AppleAuthProvider {
    @Composable
    override fun get(): SocialAuthenticator<AppleUser> {
        val activity = LocalActivity.current
        return AppleAuthenticator(activity = activity, auth = FirebaseAuth.getInstance())
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

            val pendingResultTask = auth.pendingAuthResult
            if (pendingResultTask != null) {
                pendingResultTask
                    .addOnSuccessListener { authResult ->
                        val credential = authResult.credential as? OAuthCredential
                        val idToken = credential?.idToken ?: ""
                        if (idToken.isEmpty()) {
                            coroutine.resume(SocialLoginResult.Failed)
                        } else {
                            coroutine.resume(SocialLoginResult.Success(AppleUser(idToken = idToken)))
                        }
                    }.addOnFailureListener { e ->
                        val result =
                            when {
                                e is FirebaseAuthWebException &&
                                    e.errorCode == FirebaseAuthWebExceptionCode.ERROR_WEB_CONTEXT_CANCELED.name
                                -> SocialLoginResult.UserCancelled

                                else -> SocialLoginResult.Failed
                            }
                        coroutine.resume(result)
                    }
            } else {
                auth
                    .startActivityForSignInWithProvider(activity, provider.build())
                    .addOnSuccessListener { authResult ->
                        val credential = authResult.credential as? OAuthCredential
                        val idToken = credential?.idToken ?: ""
                        if (idToken.isEmpty()) {
                            coroutine.resume(SocialLoginResult.Failed)
                        } else {
                            coroutine.resume(SocialLoginResult.Success(AppleUser(idToken = idToken)))
                        }
                    }.addOnFailureListener { e ->
                        val result =
                            when {
                                e is FirebaseAuthWebException &&
                                    e.errorCode == FirebaseAuthWebExceptionCode.ERROR_WEB_CONTEXT_CANCELED.name
                                -> SocialLoginResult.UserCancelled

                                else -> SocialLoginResult.Failed
                            }
                        coroutine.resume(result)
                    }
            }
        }
    }
}
