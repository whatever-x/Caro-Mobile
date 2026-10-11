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

internal class FcmTokenRepositoryImpl(
    private val notificationDataSource: NotificationDataSource,
    private val localAuthDataSource: LocalAuthDataSource,
) : FcmTokenRepository {
    override suspend fun syncTokens(tokens: Flow<String>) {
        combine(tokens, localAuthDataSource.observeRegisteredAccessToken()) { token, accessToken ->
            if (accessToken != null && token.isNotBlank()) token to accessToken else null
        }.distinctUntilChanged()
            .catch {
                if (it is CancellationException) throw it
                Napier.w { "FCM session observation failed; retry on next app start" }
            }.collect { registration ->
                if (registration != null) {
                    suspendRunCatching {
                        notificationDataSource.registerToken(
                            RegisterDeviceTokenRequest(token = registration.first, platform = notificationPlatform()),
                        )
                    }.onFailure {
                        Napier.w { "FCM token registration failed; retry on next app start or token change" }
                    }
                }
            }
    }
}
