package com.whatever.caro.core.ads

import com.whatever.caro.core.data.repository.ads.InterstitialFrequencyRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

internal class DefaultInterstitialAdController(
    private val provider: NativeInterstitialProvider,
    private val frequency: InterstitialFrequencyRepository,
    private val today: () -> String = {
        Clock.System
            .now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .toString()
    },
) : InterstitialAdController {
    private val presentation = Mutex()
    override val privacyOptionsRequired = provider.privacyOptionsRequired

    override suspend fun initialize() = provider.initialize()

    override fun preload() = provider.preload()

    override suspend fun showIfEligible(evaluatedCardCount: Int) {
        if (evaluatedCardCount < 20 || !presentation.tryLock()) return
        try {
            val ad = provider.takeReadyAd() ?: return
            val reservedDate = today()
            val reservation = frequency.reserve(reservedDate) ?: return
            if (today() != reservedDate) {
                frequency.release(reservation)
                return
            }
            var shownOn: String? = null
            try {
                val presented = ad.show { if (shownOn == null) shownOn = today() }
                if (shownOn != null || presented) {
                    frequency.confirmShown(reservation, shownOn ?: today())
                } else {
                    frequency.release(reservation)
                }
            } catch (cancelled: CancellationException) {
                // An interrupted callback cannot prove the ad was never displayed.
                throw cancelled
            } catch (_: Exception) {
                // Retain an uncertain reservation to preserve the daily upper bound.
                if (shownOn != null) withContext(NonCancellable) { frequency.confirmShown(reservation, shownOn ?: today()) }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // A local storage or SDK error must not block learning completion.
        } finally {
            presentation.unlock()
        }
    }

    override suspend fun showPrivacyOptions() {
        if (!presentation.tryLock()) return
        try {
            provider.showPrivacyOptions()
        } finally {
            presentation.unlock()
        }
    }
}
