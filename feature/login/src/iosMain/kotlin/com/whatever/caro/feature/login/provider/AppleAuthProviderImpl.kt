package com.whatever.caro.feature.login.provider

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.uikit.LocalUIViewController
import com.whatever.caro.feature.login.model.AppleUser
import com.whatever.caro.feature.login.model.SocialAuthenticator
import com.whatever.caro.feature.login.model.SocialLoginResult
import io.github.aakira.napier.Napier
import kotlinx.cinterop.BetaInteropApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AuthenticationServices.ASAuthorization
import platform.AuthenticationServices.ASAuthorizationAppleIDCredential
import platform.AuthenticationServices.ASAuthorizationAppleIDProvider
import platform.AuthenticationServices.ASAuthorizationController
import platform.AuthenticationServices.ASAuthorizationControllerDelegateProtocol
import platform.AuthenticationServices.ASAuthorizationControllerPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASAuthorizationScopeEmail
import platform.AuthenticationServices.ASAuthorizationScopeFullName
import platform.Foundation.NSError
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.darwin.NSObject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume

internal class AppleAuthProviderImpl : AppleAuthProvider {
    @Composable
    override fun get(): SocialAuthenticator<AppleUser> {
        val viewController = LocalUIViewController.current
        return remember(viewController) {
            AppleAuthenticatorImpl(viewController = viewController)
        }
    }
}

/**
 * Swift에 정의된 인증 실패시 발생 되는 에러 코드
 * https://developer.apple.com/documentation/authenticationservices/asauthorizationerror-swift.struct/code
 */
enum class ASAuthorizationErrorCode(
    val code: Int,
) {
    FAILED(1000),
    CANCELED(1001),
    NOT_HANDLED(1002),
    INVALID_RESPONSE(1003),
    NOT_INTERACTIVE(1004),
    UNKNOWN(1005),
    CREDENTIAL_EXPORT(1006),
    CREDENTIAL_IMPORT(1007),
    ;

    companion object {
        fun fromCode(code: Int): ASAuthorizationErrorCode? = entries.firstOrNull { it.code == code }
    }
}

private class AppleAuthenticatorImpl(
    private val viewController: UIViewController,
) : SocialAuthenticator<AppleUser> {
    private var authorizationDelegate: ASAuthorizationControllerDelegateProtocol? = null
    private var presentationContextProvider: ASAuthorizationControllerPresentationContextProvidingProtocol? = null

    override suspend fun authenticate(): SocialLoginResult<AppleUser> =
        suspendCancellableCoroutine { continuation ->
            val provider = ASAuthorizationAppleIDProvider()
            val request =
                provider.createRequest().apply {
                    requestedScopes = listOf(ASAuthorizationScopeFullName, ASAuthorizationScopeEmail)
                }

            val controller = ASAuthorizationController(listOf(request))
            var isHandled = false

            fun cleanup() {
                authorizationDelegate = null
                presentationContextProvider = null
            }

            fun resumeOnce(result: SocialLoginResult<AppleUser>) {
                if (isHandled) return
                isHandled = true
                cleanup()
                if (continuation.isActive) {
                    continuation.resume(result)
                }
            }

            authorizationDelegate =
                object : NSObject(), ASAuthorizationControllerDelegateProtocol {
                    @OptIn(BetaInteropApi::class)
                    override fun authorizationController(
                        controller: ASAuthorizationController,
                        didCompleteWithAuthorization: ASAuthorization,
                    ) {
                        val credential =
                            didCompleteWithAuthorization.credential as? ASAuthorizationAppleIDCredential

                        val idToken = credential?.extractIdToken()

                        if (idToken.isNullOrBlank()) {
                            Napier.e { "apple login: identityToken is null or blank" }
                            resumeOnce(SocialLoginResult.Failed)
                            return
                        }

                        resumeOnce(
                            SocialLoginResult.Success(
                                AppleUser(idToken = idToken),
                            ),
                        )
                    }

                    override fun authorizationController(
                        controller: ASAuthorizationController,
                        didCompleteWithError: NSError,
                    ) {
                        when (ASAuthorizationErrorCode.fromCode(didCompleteWithError.code.toInt())) {
                            ASAuthorizationErrorCode.CANCELED -> {
                                resumeOnce(SocialLoginResult.UserCancelled)
                            }

                            else -> {
                                Napier.e {
                                    "apple login failed: code=${didCompleteWithError.code} " +
                                        "domain=${didCompleteWithError.domain} " +
                                        "message=${didCompleteWithError.localizedDescription}"
                                }
                                resumeOnce(SocialLoginResult.Failed)
                            }
                        }
                    }
                }

            presentationContextProvider =
                object : NSObject(), ASAuthorizationControllerPresentationContextProvidingProtocol {
                    override fun presentationAnchorForAuthorizationController(controller: ASAuthorizationController): UIWindow =
                        requireNotNull(viewController.view.window) {
                            "Apple Sign In presentation window is null."
                        }
                }

            continuation.invokeOnCancellation { cause ->
                cleanup()
                if (cause is CancellationException) {
                    isHandled = true
                }
            }

            controller.delegate = authorizationDelegate
            controller.presentationContextProvider = presentationContextProvider
            controller.performRequests()
        }

    @OptIn(BetaInteropApi::class)
    private fun ASAuthorizationAppleIDCredential.extractIdToken(): String? {
        val tokenData = identityToken ?: return null
        return NSString.create(tokenData, NSUTF8StringEncoding)?.toString()
    }
}
