package com.whatever.caro.feature.login.provider

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.whatever.caro.feature.login.generated.BuildKonfig
import com.whatever.caro.feature.login.model.GoogleUser
import com.whatever.caro.feature.login.model.SocialAuthenticator
import com.whatever.caro.feature.login.model.SocialLoginResult
import java.security.SecureRandom
import kotlin.io.encoding.Base64

class GoogleAuthProviderImpl : GoogleAuthProvider {
    @Composable
    override fun get(): SocialAuthenticator<GoogleUser> {
        val context = LocalContext.current
        val credentialManager = CredentialManager.create(context)
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(BuildKonfig.GOOGLE_CLIENT_ID)
            .setAutoSelectEnabled(true)
            .setNonce(generateNonce())
            .build()
        return GoogleAuthenticator(
            context = context,
            credentialManager = credentialManager,
            option = option
        )
    }

    private fun generateNonce(): String {
        val randomBytes = ByteArray(32)
        SecureRandom().nextBytes(randomBytes)
        return Base64.UrlSafe
            .withPadding(Base64.PaddingOption.ABSENT)
            .encode(randomBytes)
    }
}

private class GoogleAuthenticator(
    private val context: Context,
    private val credentialManager: CredentialManager,
    private val option: GetGoogleIdOption,
) : SocialAuthenticator<GoogleUser> {
    override suspend fun authenticate(): SocialLoginResult<GoogleUser> {
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()
        val credential = credentialManager.getCredential(
            context = context,
            request = request
        ).credential
        // TODO : 유저 취소, 아에 오류 타입 확인
        return try {
            val user = getGoogleUserFromCredential(credential)
            SocialLoginResult.Success(user!!)
        } catch (e: Exception) {
            SocialLoginResult.Failed
        }
    }

    private fun getGoogleUserFromCredential(
        credential: Credential,
    ): GoogleUser? {
        return when {
            credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                try {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    GoogleUser(
                        idToken = googleIdTokenCredential.idToken
                    )
                }catch (e : GoogleIdTokenParsingException) {
                    null
                }
            }

            else -> null
        }
    }
}