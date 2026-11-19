package com.whatever.caro.core.ads

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.whatever.caro.core.data.repository.ads.AdEligibilityMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.TimeMark
import kotlin.time.TimeSource

internal class AndroidInterstitialProvider(
    context: Context,
    private val eligibility: AdEligibilityMonitor,
) : NativeInterstitialProvider {
    private val callbacks = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val context = context.applicationContext
    private val metadata = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA).metaData
    private val appId = metadata?.getString("com.google.android.gms.ads.APPLICATION_ID").orEmpty()
    private val unitId = metadata?.getString("com.whatever.caro.ads.INTERSTITIAL_ID").orEmpty()
    private val consent = UserMessagingPlatform.getConsentInformation(context)
    private var activity = WeakReference<Activity>(null)
    private var active = false
    private var allowConsent = false
    private var initialized = false
    private var consentUpdated = false
    private var consentUpdating = false
    val consentFormShowing = MutableStateFlow(false)
    private var loading = false
    private var generation = 0
    private var accountGeneration: Long? = null
    private var loadedAt: TimeMark? = null
    private var ad: InterstitialAd? = null
    override val privacyOptionsRequired = MutableStateFlow(false)

    init {
        callbacks.launch {
            eligibility.state.collect { currentAccount() }
        }
    }

    fun bindHost(
        host: Activity?,
        active: Boolean,
        allowConsent: Boolean,
    ): Boolean {
        activity = WeakReference(host)
        this.active = active
        this.allowConsent = allowConsent
        return allowConsent && host() != null
    }

    override suspend fun initialize() =
        withContext(Dispatchers.Main.immediate) {
            val account = currentAccount() ?: return@withContext AdInitializationResult.DEFERRED
            if (!configured()) return@withContext AdInitializationResult.DISABLED
            if (initialized) return@withContext AdInitializationResult.INITIALIZED
            if (!allowConsent || consentUpdating || consentFormShowing.value) return@withContext AdInitializationResult.DEFERRED
            val host = host() ?: return@withContext AdInitializationResult.DEFERRED
            consentUpdating = true
            try {
                if (!consentUpdated) {
                    suspendCancellableCoroutine { continuation ->
                        consent.requestConsentInfoUpdate(host, ConsentRequestParameters.Builder().build(), {
                            if (isEligible(account)) consentUpdated = true
                            if (continuation.isActive) continuation.resume(Unit)
                        }, {
                            if (continuation.isActive) continuation.resume(Unit)
                        })
                    }
                }
                if (!isEligible(account)) return@withContext AdInitializationResult.DEFERRED
                updatePrivacyOptions()
                if (!allowConsent || host() == null) return@withContext AdInitializationResult.DEFERRED
                if (consent.consentStatus == ConsentInformation.ConsentStatus.REQUIRED) {
                    suspendCancellableCoroutine { continuation ->
                        UserMessagingPlatform.loadConsentForm(context, { form ->
                            callbacks.launch {
                                val currentHost = host()
                                if (!continuation.isActive) return@launch
                                if (!isEligible(account) || !allowConsent || currentHost == null) {
                                    continuation.resume(Unit)
                                } else {
                                    consentFormShowing.value = true
                                    try {
                                        form.show(currentHost) {
                                            consentFormShowing.value = false
                                            if (continuation.isActive) continuation.resume(Unit)
                                        }
                                    } catch (exception: Exception) {
                                        consentFormShowing.value = false
                                        if (continuation.isActive) continuation.resumeWithException(exception)
                                    }
                                }
                            }
                        }, { if (continuation.isActive) continuation.resume(Unit) })
                    }
                }
                if (!isEligible(account) || !allowConsent || host() == null) return@withContext AdInitializationResult.DEFERRED
                updatePrivacyOptions()
                if (consent.canRequestAds()) {
                    withContext(Dispatchers.IO) {
                        if (isEligible(account)) MobileAds.initialize(context, InitializationConfig.Builder(appId).build()) {}
                    }
                    if (!isEligible(account)) return@withContext AdInitializationResult.DEFERRED
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
        val account = currentAccount() ?: return
        if (!initialized || !consent.canRequestAds() || loading || ad != null) return
        loading = true
        val requestGeneration = generation
        InterstitialAd.load(
            AdRequest.Builder(unitId).build(),
            object : AdLoadCallback<InterstitialAd> {
                override fun onAdLoaded(ad: InterstitialAd) {
                    callbacks.launch {
                        if (requestGeneration == generation && isEligible(account)) {
                            loading = false
                            if (consent.canRequestAds()) {
                                this@AndroidInterstitialProvider.ad = ad
                                loadedAt = TimeSource.Monotonic.markNow()
                            }
                        }
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    callbacks.launch { if (requestGeneration == generation && isEligible(account)) loading = false }
                }
            },
        )
    }

    override fun takeReadyAd(): ReadyInterstitial? {
        val account = currentAccount() ?: return null
        if (!consent.canRequestAds() || host() == null) return null
        val ready = ad ?: return null
        ad = null
        if ((loadedAt?.elapsedNow()?.inWholeMinutes ?: 60) >= 60) return null
        return ReadyInterstitial { onShown ->
            withContext(Dispatchers.Main.immediate) {
                val host = host()
                if (!isEligible(account) || host == null || !consent.canRequestAds()) return@withContext false
                suspendCancellableCoroutine { continuation ->
                    var shown = false
                    ready.adEventCallback =
                        object : InterstitialAdEventCallback {
                            override fun onAdShowedFullScreenContent() {
                                callbacks.launch {
                                    shown = true
                                    if (isEligible(account)) onShown()
                                }
                            }

                            override fun onAdImpression() {
                                callbacks.launch {
                                    shown = true
                                    if (isEligible(account)) onShown()
                                }
                            }

                            override fun onAdDismissedFullScreenContent() {
                                callbacks.launch { if (continuation.isActive) continuation.resume(shown) }
                            }

                            override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                                callbacks.launch { if (continuation.isActive) continuation.resume(shown) }
                            }
                        }
                    ready.show(host)
                }
            }
        }
    }

    override suspend fun showPrivacyOptions() =
        withContext(Dispatchers.Main.immediate) {
            val account = currentAccount() ?: return@withContext
            val host = host() ?: return@withContext
            if (!allowConsent || !privacyOptionsRequired.value || consentFormShowing.value) return@withContext
            generation++
            ad = null
            loading = false
            suspendCancellableCoroutine { continuation ->
                consentFormShowing.value = true
                try {
                    UserMessagingPlatform.showPrivacyOptionsForm(host) {
                        consentFormShowing.value = false
                        if (isEligible(account)) updatePrivacyOptions()
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                } catch (exception: Exception) {
                    consentFormShowing.value = false
                    if (continuation.isActive) continuation.resumeWithException(exception)
                }
            }
            if (isEligible(account) && consent.canRequestAds()) preload()
        }

    private fun host() = activity.get()?.takeIf { active && !it.isFinishing && !it.isDestroyed }

    private fun isEligible(account: Long): Boolean = eligibility.state.value.let { it.allowed && it.generation == account }

    private fun currentAccount(): Long? {
        val state = eligibility.state.value
        if (accountGeneration != state.generation) {
            accountGeneration = state.generation
            generation++
            ad = null
            loading = false
            initialized = false
            consentUpdated = false
            privacyOptionsRequired.value = false
        }
        return state.generation.takeIf { state.allowed }
    }

    private fun configured() = appId.startsWith("ca-app-pub-") && unitId.startsWith("ca-app-pub-")

    private fun updatePrivacyOptions() {
        privacyOptionsRequired.value =
            consent.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }
}
