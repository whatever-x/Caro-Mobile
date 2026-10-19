package com.whatever.caro.core.data.repository.fcm

import com.whatever.caro.core.data.util.suspendRunCatching
import com.whatever.caro.core.datastore.datasource.LocalAuthDataSource
import com.whatever.caro.core.datastore.datasource.LocalNotificationDataSource
import com.whatever.caro.core.remote.datasource.notification.NotificationDataSource
import com.whatever.caro.core.remote.dto.notification.request.RegisterDeviceTokenRequest
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

internal class FcmTokenRepositoryImpl(
    private val notificationDataSource: NotificationDataSource,
    private val localAuthDataSource: LocalAuthDataSource,
    private val localNotificationDataSource: LocalNotificationDataSource,
) : FcmTokenRepository {
    private val registrationMutex = Mutex()
    private val latestToken = MutableStateFlow("")
    private val permissionGranted = MutableStateFlow<Boolean?>(null)
    private val permissionRevision = MutableStateFlow(0L)
    private val confirmedEnabled = MutableStateFlow(false)
    private var registeredToken: String? = null
    private var successfulRegistration: Registration? = null
    private var registrationPaused = false

    override fun observeNotificationEnabled(): Flow<Boolean> =
        combine(localNotificationDataSource.observeNotificationEnabled(), confirmedEnabled, permissionGranted) { choice, enabled, granted ->
            (choice ?: enabled) && granted == true
        }.distinctUntilChanged()

    override fun updateNotificationPermission(granted: Boolean) {
        permissionGranted.value = granted
        // 같은 권한 상태로 복귀해도 직전 네트워크 실패를 재시도한다.
        permissionRevision.update { it + 1 }
    }

    override suspend fun resumeRegistration() {
        registrationMutex.withLock { registrationPaused = false }
    }

    override suspend fun unregisterToken() {
        registrationMutex.withLock {
            registrationPaused = true
            removeTokens()
            confirmedEnabled.value = false
        }
    }

    override suspend fun setNotificationEnabled(enabled: Boolean) {
        val session = checkNotNull(localAuthDataSource.observeRegisteredAccessToken().first()) { "No registered session" }
        val token =
            if (enabled) {
                withTimeoutOrNull(TOKEN_WAIT_MILLIS) { latestToken.first { it.isNotBlank() } }
                    ?: error("FCM token wait timed out")
            } else {
                null
            }
        registrationMutex.withLock {
            check(!registrationPaused && localAuthDataSource.fetchAccessToken() == session) { "Notification session changed" }
            if (enabled) {
                check(permissionGranted.value == true) { "Notification permission required" }
                register(checkNotNull(token), session, permissionRevision.value)
            } else {
                removeTokens()
            }
            check(localAuthDataSource.fetchAccessToken() == session) { "Notification session changed" }
            localNotificationDataSource.saveNotificationEnabled(enabled)
            confirmedEnabled.value = enabled
        }
    }

    override suspend fun syncTokens(tokens: Flow<String>) {
        successfulRegistration = null
        val observedTokens = tokens.onEach { if (it.isNotBlank()) latestToken.value = it }
        val sessions =
            localAuthDataSource.observeRegisteredAccessToken().onEach { accessToken ->
                if (accessToken == null) {
                    registrationMutex.withLock {
                        registrationPaused = false
                        confirmedEnabled.value = false
                        successfulRegistration = null
                    }
                }
            }
        combine(
            observedTokens,
            sessions,
            localNotificationDataSource.observeNotificationEnabled(),
            permissionGranted,
            permissionRevision,
        ) { token, session, choice, granted, revision ->
            Registration(token, session, choice != false && granted == true, revision)
        }.distinctUntilChanged()
            .catch {
                if (it is CancellationException) throw it
                Napier.w { "FCM session observation failed; retry on next app start" }
            }.collect { registration ->
                suspendRunCatching {
                    registrationMutex.withLock {
                        if (registration.revision != permissionRevision.value ||
                            localAuthDataSource.fetchAccessToken() != registration.session
                        ) {
                            return@withLock
                        }
                        val choice = localNotificationDataSource.observeNotificationEnabled().first()
                        if (choice == false) {
                            if (registeredToken != null || localNotificationDataSource.fetchRegisteredToken() != null) removeTokens()
                            confirmedEnabled.value = false
                            return@withLock
                        }
                        if (permissionGranted.value != true || registration.session == null) {
                            confirmedEnabled.value = false
                            return@withLock
                        }
                        if (registrationPaused || registration.token.isBlank()) return@withLock
                        if (successfulRegistration?.let { it.token == registration.token && it.session == registration.session } == true) {
                            confirmedEnabled.value = true
                            return@withLock
                        }
                        register(registration.token, registration.session, registration.revision)
                        confirmedEnabled.value = localAuthDataSource.fetchAccessToken() == registration.session
                    }
                }.onFailure {
                    Napier.w { "FCM token registration failed; retry on next app resume or token change" }
                }
            }
    }

    private suspend fun register(
        token: String,
        session: String,
        revision: Long,
    ) {
        notificationDataSource.registerToken(RegisterDeviceTokenRequest(token = token, platform = notificationPlatform()))
        registeredToken = token
        localNotificationDataSource.saveRegisteredToken(token)
        successfulRegistration = Registration(token, session, true, revision)
        Napier.d { "FCM token registration succeeded" }
    }

    private suspend fun removeTokens() {
        var failure: Throwable? = null
        listOfNotNull(registeredToken, localNotificationDataSource.fetchRegisteredToken(), latestToken.value.takeIf { it.isNotBlank() })
            .distinct()
            .forEach { token ->
                suspendRunCatching { notificationDataSource.unregisterToken(token) }
                    .onFailure { if (failure == null) failure = it }
            }
        failure?.let { throw it }
        localNotificationDataSource.saveRegisteredToken(null)
        registeredToken = null
        successfulRegistration = null
        Napier.d { "FCM token removal succeeded" }
    }

    private data class Registration(
        val token: String,
        val session: String?,
        val enabled: Boolean,
        val revision: Long,
    )

    private companion object {
        const val TOKEN_WAIT_MILLIS = 10_000L
    }
}
