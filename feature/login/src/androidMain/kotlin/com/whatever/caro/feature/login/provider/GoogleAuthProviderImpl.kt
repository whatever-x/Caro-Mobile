package com.whatever.caro.feature.login.provider

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
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

        return GoogleAuthenticator(
            context = context,
            credentialManager = credentialManager,
        )
    }
}

private class GoogleAuthenticator(
    private val context: Context,
    private val credentialManager: CredentialManager,
) : SocialAuthenticator<GoogleUser> {
    override suspend fun authenticate(): SocialLoginResult<GoogleUser> {
        val option =
            GetGoogleIdOption
                .Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(BuildKonfig.GOOGLE_WEB_CLIENT_ID)
                .setAutoSelectEnabled(true)
                .setNonce(generateNonce())
                .build()

        val request =
            GetCredentialRequest
                .Builder()
                .addCredentialOption(option)
                .build()
        return try {
            val credential =
                credentialManager
                    .getCredential(
                        context = context,
                        request = request,
                    ).credential
            val user = getGoogleUserFromCredential(credential) ?: return SocialLoginResult.Failed
            SocialLoginResult.Success(user)
        } catch (_: GetCredentialCancellationException) {
            SocialLoginResult.UserCancelled
        } catch (_: NoCredentialException) {
            // 첫 로그인에 대한 fallback
            val fallbackOption =
                GetGoogleIdOption
                    .Builder()
                    .setServerClientId(BuildKonfig.GOOGLE_WEB_CLIENT_ID)
                    .setFilterByAuthorizedAccounts(false)
                    .setAutoSelectEnabled(false)
                    .build()

            val fallbackRequest =
                GetCredentialRequest
                    .Builder()
                    .addCredentialOption(fallbackOption)
                    .build()

            try {
                val credential =
                    credentialManager
                        .getCredential(
                            request = fallbackRequest,
                            context = context,
                        ).credential
                val user =
                    getGoogleUserFromCredential(credential) ?: return SocialLoginResult.Failed
                SocialLoginResult.Success(user)
            } catch (_: GetCredentialException) {
                SocialLoginResult.Failed
            }
        } catch (_: GetCredentialException) {
            SocialLoginResult.Failed
        }
    }

    private fun getGoogleUserFromCredential(credential: Credential): GoogleUser? =
        when {
            credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                try {
                    val googleIdTokenCredential =
                        GoogleIdTokenCredential.createFrom(credential.data)
                    GoogleUser(
                        idToken = googleIdTokenCredential.idToken,
                    )
                } catch (_: GoogleIdTokenParsingException) {
                    null
                }
            }

            else -> {
                null
            }
        }

    private fun generateNonce(): String {
        val randomBytes = ByteArray(32)
        SecureRandom().nextBytes(randomBytes)
        return Base64.UrlSafe
            .withPadding(Base64.PaddingOption.ABSENT)
            .encode(randomBytes)
    }
}
