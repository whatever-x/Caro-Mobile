package com.whatever.caro.core.data.repository.fcm

import com.whatever.caro.core.data.util.suspendRunCatching
import com.whatever.caro.core.datastore.datasource.LocalAuthDataSource
import com.whatever.caro.core.remote.datasource.notification.NotificationDataSource
import com.whatever.caro.core.remote.dto.notification.request.RegisterDeviceTokenRequest
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class FcmTokenRepositoryImpl(
    private val notificationDataSource: NotificationDataSource,
    private val localAuthDataSource: LocalAuthDataSource,
) : FcmTokenRepository {
    private val registrationMutex = Mutex()
    private var latestToken: String? = null
    private var registeredToken: String? = null
    private var registrationPaused = false

    override suspend fun unregisterToken() {
        registrationMutex.withLock {
            registrationPaused = true
            var failure: Throwable? = null
            listOfNotNull(registeredToken, latestToken).distinct().forEach { token ->
                suspendRunCatching { notificationDataSource.unregisterToken(token) }
                    .onFailure { if (failure == null) failure = it }
            }
            failure?.let { throw it }
            registeredToken = null
        }
    }

    override suspend fun syncTokens(tokens: Flow<String>) {
        val observedTokens =
            tokens.onEach { token ->
                registrationMutex.withLock {
                    if (token.isNotBlank()) latestToken = token
                }
            }
        val sessions =
            localAuthDataSource.observeRegisteredAccessToken().onEach { accessToken ->
                if (accessToken == null) registrationMutex.withLock { registrationPaused = false }
            }
        combine(observedTokens, sessions) { token, accessToken ->
            if (accessToken != null && token.isNotBlank()) token to accessToken else null
        }.distinctUntilChanged()
            .catch {
                if (it is CancellationException) throw it
                Napier.w { "FCM session observation failed; retry on next app start" }
            }.collect { registration ->
                if (registration != null) {
                    suspendRunCatching {
                        registrationMutex.withLock {
                            if (registrationPaused || localAuthDataSource.fetchAccessToken() != registration.second) {
                                return@withLock
                            }
                            notificationDataSource.registerToken(
                                RegisterDeviceTokenRequest(token = registration.first, platform = notificationPlatform()),
                            )
                            registeredToken = registration.first
                        }
                    }.onFailure {
                        Napier.w { "FCM token registration failed; retry on next app start or token change" }
                    }
                }
            }
    }
}
