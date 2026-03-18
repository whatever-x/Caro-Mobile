package com.whatever.caro.feature.login.provider

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import com.google.firebase.auth.FirebaseAuth
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

            val provider =
                OAuthProvider.newBuilder("apple.com").apply {
                    scopes = listOf("email", "name")
                    addCustomParameter("locale", "ko_KR")
                }

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
                    }.addOnFailureListener { _ ->
                        coroutine.resume(SocialLoginResult.Failed)
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
                    }.addOnFailureListener {
                        coroutine.resume(SocialLoginResult.Failed)
                    }
            }
        }
    }
}
