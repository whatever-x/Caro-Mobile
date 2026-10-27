@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.whatever.caro.core.ads

import GoogleMobileAds.GADFullScreenContentDelegateProtocol
import GoogleMobileAds.GADFullScreenPresentingAdProtocol
import GoogleMobileAds.GADInterstitialAd
import GoogleMobileAds.GADMobileAds
import GoogleMobileAds.GADRequest
import GoogleUserMessagingPlatform.UMPConsentForm
import GoogleUserMessagingPlatform.UMPConsentInformation
import GoogleUserMessagingPlatform.UMPConsentStatusRequired
import GoogleUserMessagingPlatform.UMPPrivacyOptionsRequirementStatusRequired
import GoogleUserMessagingPlatform.UMPRequestParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.Foundation.NSBundle
import platform.Foundation.NSError
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationState
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.TimeMark
import kotlin.time.TimeSource

internal class IosInterstitialProvider : NativeInterstitialProvider {
    private val appId = NSBundle.mainBundle.objectForInfoDictionaryKey("GADApplicationIdentifier") as? String ?: ""
    private val unitId = NSBundle.mainBundle.objectForInfoDictionaryKey("CaroInterstitialAdUnitID") as? String ?: ""
    private val consent = UMPConsentInformation.sharedInstance
    private var active = false
    private var allowConsent = false
    private var initialized = false
    private var consentUpdated = false
    private var consentUpdating = false
    val consentFormShowing = MutableStateFlow(false)
    private var loading = false
    private var generation = 0
    private var loadedAt: TimeMark? = null
    private var ad: GADInterstitialAd? = null
    override val privacyOptionsRequired = MutableStateFlow(false)

    fun bindHost(
        active: Boolean,
        allowConsent: Boolean,
    ): Boolean {
        this.active = active
        this.allowConsent = allowConsent
        return allowConsent && active && (host() != null || consentFormShowing.value)
    }

    override suspend fun initialize() =
        withContext(Dispatchers.Main.immediate) {
            if (!configured()) return@withContext AdInitializationResult.DISABLED
            if (initialized) return@withContext AdInitializationResult.INITIALIZED
            if (!allowConsent || consentUpdating || consentFormShowing.value) return@withContext AdInitializationResult.DEFERRED
            val host = host() ?: return@withContext AdInitializationResult.DEFERRED
            consentUpdating = true
            try {
                if (!consentUpdated) {
                    suspendCancellableCoroutine { continuation ->
                        consent.requestConsentInfoUpdateWithParameters(UMPRequestParameters()) { error ->
                            consentUpdated = error == null
                            if (continuation.isActive) continuation.resume(Unit)
                        }
                    }
                }
                updatePrivacyOptions()
                if (!allowConsent || host() == null) return@withContext AdInitializationResult.DEFERRED
                if (consent.consentStatus == UMPConsentStatusRequired) {
                    suspendCancellableCoroutine { continuation ->
                        UMPConsentForm.loadWithCompletionHandler { form, _ ->
                            val currentHost = host()
                            if (!continuation.isActive) return@loadWithCompletionHandler
                            if (!allowConsent || currentHost == null || form == null) {
                                continuation.resume(Unit)
                            } else {
                                consentFormShowing.value = true
                                try {
                                    form.presentFromViewController(currentHost) {
                                        consentFormShowing.value = false
                                        if (continuation.isActive) continuation.resume(Unit)
                                    }
                                } catch (exception: Exception) {
                                    consentFormShowing.value = false
                                    if (continuation.isActive) continuation.resumeWithException(exception)
                                }
                            }
                        }
                    }
                }
                if (!allowConsent || host() == null) return@withContext AdInitializationResult.DEFERRED
                updatePrivacyOptions()
                if (consent.canRequestAds) {
                    suspendCancellableCoroutine { continuation ->
                        GADMobileAds.sharedInstance.startWithCompletionHandler {
                            if (continuation.isActive) continuation.resume(Unit)
                        }
                    }
                    initialized = true
                    preload()
                    AdInitializationResult.INITIALIZED
                } else {
                    AdInitializationResult.RETRYABLE_FAILURE
                }
            } finally {
                consentUpdating = false
            }
        }

    override fun preload() {
        if (!initialized || !consent.canRequestAds || loading || ad != null) return
        loading = true
        val requestGeneration = generation
        GADInterstitialAd.loadWithAdUnitID(unitId, GADRequest.request()) { loaded, _ ->
            if (requestGeneration == generation) {
                loading = false
                if (consent.canRequestAds) {
                    ad = loaded
                    loadedAt = TimeSource.Monotonic.markNow()
                }
            }
        }
    }

    override fun takeReadyAd(): ReadyInterstitial? {
        if (!consent.canRequestAds || host() == null) return null
        val ready = ad ?: return null
        ad = null
        if ((loadedAt?.elapsedNow()?.inWholeMinutes ?: 60) >= 60) return null
        return ReadyInterstitial { onShown ->
            withContext(Dispatchers.Main.immediate) {
                val host = host()
                if (host == null || !consent.canRequestAds) return@withContext false
                suspendCancellableCoroutine { continuation ->
                    val delegate =
                        object : NSObject(), GADFullScreenContentDelegateProtocol {
                            private var shown = false

                            override fun adWillPresentFullScreenContent(ad: GADFullScreenPresentingAdProtocol) {
                                shown = true
                                onShown()
                            }

                            override fun adDidRecordImpression(ad: GADFullScreenPresentingAdProtocol) {
                                shown = true
                                onShown()
                            }

                            override fun adDidDismissFullScreenContent(ad: GADFullScreenPresentingAdProtocol) {
                                ready.fullScreenContentDelegate = null
                                if (presentationDelegate === this) presentationDelegate = null
                                if (continuation.isActive) continuation.resume(shown)
                            }

                            override fun ad(
                                ad: GADFullScreenPresentingAdProtocol,
                                didFailToPresentFullScreenContentWithError: NSError,
                            ) {
                                ready.fullScreenContentDelegate = null
                                if (presentationDelegate === this) presentationDelegate = null
                                if (continuation.isActive) continuation.resume(shown)
                            }
                        }
                    ready.fullScreenContentDelegate = delegate
                    // The SDK delegate is weak; keep it alive for the presentation callbacks.
                    presentationDelegate = delegate
                    ready.presentFromRootViewController(host)
                }
            }.also { presentationDelegate = null }
        }
    }

    private var presentationDelegate: GADFullScreenContentDelegateProtocol? = null

    override suspend fun showPrivacyOptions() =
        withContext(Dispatchers.Main.immediate) {
            val host = host() ?: return@withContext
            if (!allowConsent || !privacyOptionsRequired.value || consentFormShowing.value) return@withContext
            generation++
            ad = null
            loading = false
            suspendCancellableCoroutine { continuation ->
                consentFormShowing.value = true
                try {
                    UMPConsentForm.presentPrivacyOptionsFormFromViewController(host) {
                        consentFormShowing.value = false
                        updatePrivacyOptions()
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                } catch (exception: Exception) {
                    consentFormShowing.value = false
                    if (continuation.isActive) continuation.resumeWithException(exception)
                }
            }
            if (consent.canRequestAds) preload()
        }

    private fun host(): UIViewController? {
        if (!active || UIApplication.sharedApplication.applicationState != UIApplicationState.UIApplicationStateActive) return null
        return UIApplication.sharedApplication.connectedScenes
            .filterIsInstance<UIWindowScene>()
            .firstOrNull { it.activationState == UISceneActivationStateForegroundActive }
            ?.windows
            ?.filterIsInstance<UIWindow>()
            ?.firstOrNull { it.isKeyWindow() }
            ?.rootViewController
            ?.takeIf { it.presentedViewController == null }
    }

    private fun configured() = appId.startsWith("ca-app-pub-") && unitId.startsWith("ca-app-pub-")

    private fun updatePrivacyOptions() {
        privacyOptionsRequired.value = consent.privacyOptionsRequirementStatus == UMPPrivacyOptionsRequirementStatusRequired
    }
}
