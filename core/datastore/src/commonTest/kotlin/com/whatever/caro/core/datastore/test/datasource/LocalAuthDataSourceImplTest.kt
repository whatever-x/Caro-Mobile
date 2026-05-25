package com.whatever.caro.core.datastore.test.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.whatever.caro.core.datastore.datasource.LocalAuthDataSourceImpl
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okio.IOException

class LocalAuthDataSourceImplTest : FunSpec() {
    init {
        val accessTokenKey = stringPreferencesKey("auth_access_token")
        val refreshTokenKey = stringPreferencesKey("auth_refresh_token")

        test("fetchAccessToken: 저장된 토큰이 있으면 반환한다") {
            runTest {
                val prefs =
                    mutablePreferencesOf(accessTokenKey to "ACCESS").toPreferences()
                val source = LocalAuthDataSourceImpl(FakeDataStore(data = flowOf(prefs)))

                source.fetchAccessToken() shouldBe "ACCESS"
            }
        }

        test("fetchAccessToken: 저장된 토큰이 없으면 null 을 반환한다") {
            runTest {
                val source =
                    LocalAuthDataSourceImpl(FakeDataStore(data = flowOf(emptyPreferences())))

                source.fetchAccessToken() shouldBe null
            }
        }

        test("fetchAccessToken: IOException 발생 시 null 을 반환한다") {
            runTest {
                val failingFlow = flow<Preferences> { throw IOException("disk failure") }
                val source = LocalAuthDataSourceImpl(FakeDataStore(data = failingFlow))

                source.fetchAccessToken() shouldBe null
            }
        }

        test("fetchAccessToken: IOException 이 아닌 예외는 전파한다") {
            runTest {
                val failingFlow = flow<Preferences> { throw IllegalStateException("not io") }
                val source = LocalAuthDataSourceImpl(FakeDataStore(data = failingFlow))

                shouldThrow<IllegalStateException> { source.fetchAccessToken() }
            }
        }

        test("fetchRefreshToken: 저장된 토큰이 있으면 반환한다") {
            runTest {
                val prefs =
                    mutablePreferencesOf(refreshTokenKey to "REFRESH").toPreferences()
                val source = LocalAuthDataSourceImpl(FakeDataStore(data = flowOf(prefs)))

                source.fetchRefreshToken() shouldBe "REFRESH"
            }
        }

        test("fetchRefreshToken: IOException 발생 시 null 을 반환한다") {
            runTest {
                val failingFlow = flow<Preferences> { throw IOException("disk failure") }
                val source = LocalAuthDataSourceImpl(FakeDataStore(data = failingFlow))

                source.fetchRefreshToken() shouldBe null
            }
        }

        test("saveTokens: 전달된 access/refresh 토큰을 함께 저장한다") {
            runTest {
                val fakeStore = FakeDataStore(data = flowOf(emptyPreferences()))
                val source = LocalAuthDataSourceImpl(fakeStore)

                source.saveTokens(accessToken = "A", refreshToken = "R")

                fakeStore.snapshot()[accessTokenKey] shouldBe "A"
                fakeStore.snapshot()[refreshTokenKey] shouldBe "R"
            }
        }

        test("saveTokens: 쓰기 중 IOException 이 발생하면 호출자에게 전파한다") {
            runTest {
                val source =
                    LocalAuthDataSourceImpl(
                        FakeDataStore(
                            data = flowOf(emptyPreferences()),
                            updateError = IOException("disk full"),
                        ),
                    )

                shouldThrow<IOException> {
                    source.saveTokens(accessToken = "A", refreshToken = "R")
                }
            }
        }

        test("clear: 저장된 access/refresh 토큰을 함께 제거한다") {
            runTest {
                val initial =
                    mutablePreferencesOf(
                        accessTokenKey to "A",
                        refreshTokenKey to "R",
                    ).toPreferences()
                val fakeStore = FakeDataStore(data = flowOf(initial), initial = initial)
                val source = LocalAuthDataSourceImpl(fakeStore)

                source.clear()

                fakeStore.snapshot().contains(accessTokenKey) shouldBe false
                fakeStore.snapshot().contains(refreshTokenKey) shouldBe false
            }
        }

        test("clear: 쓰기 중 IOException 이 발생하면 호출자에게 전파한다") {
            runTest {
                val source =
                    LocalAuthDataSourceImpl(
                        FakeDataStore(
                            data = flowOf(emptyPreferences()),
                            updateError = IOException("disk full"),
                        ),
                    )

                shouldThrow<IOException> { source.clear() }
            }
        }
    }
}

private class FakeDataStore(
    override val data: Flow<Preferences>,
    initial: Preferences = emptyPreferences(),
    private val updateError: Throwable? = null,
) : DataStore<Preferences> {
    private var current: Preferences = initial

    fun snapshot(): Preferences = current

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        updateError?.let { throw it }
        current = transform(current)
        return current
    }
}
