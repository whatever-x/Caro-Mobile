package com.whatever.caro.feature.login.provider

import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.whatever.caro.feature.login.model.SocialLoginResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GoogleAuthenticatorTest {
    @Test
    fun `fallback cancellation is user cancellation`() =
        runTest {
            assertAuthentication(listOf(NoCredentialException(), GetCredentialCancellationException()), SocialLoginResult.UserCancelled)
        }

    @Test
    fun `fallback without credentials reports no credential`() =
        runTest {
            assertAuthentication(listOf(NoCredentialException(), NoCredentialException()), SocialLoginResult.NoCredential)
        }

    @Test
    fun `initial cancellation does not retry`() =
        runTest {
            assertAuthentication(listOf(GetCredentialCancellationException()), SocialLoginResult.UserCancelled)
        }

    private suspend fun assertAuthentication(
        errors: List<GetCredentialException>,
        expected: SocialLoginResult<Nothing>,
    ) {
        val requests = mutableListOf<GetCredentialRequest>()
        val authenticator =
            GoogleAuthenticator(RuntimeEnvironment.getApplication(), serverClientId = "test-client-id") { _, request ->
                requests.add(request)
                throw errors[requests.lastIndex]
            }

        assertEquals(expected, authenticator.authenticate())
        assertEquals(errors.size, requests.size)
        assertTrue((requests.first().credentialOptions.single() as GetGoogleIdOption).filterByAuthorizedAccounts)
        if (requests.size == 2) {
            assertFalse((requests.last().credentialOptions.single() as GetGoogleIdOption).filterByAuthorizedAccounts)
        }
    }
}
