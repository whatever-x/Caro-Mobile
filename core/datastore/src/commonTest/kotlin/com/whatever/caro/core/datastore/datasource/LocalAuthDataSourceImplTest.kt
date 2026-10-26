package com.whatever.caro.core.datastore.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import okio.IOException

class LocalAuthDataSourceImplTest : FunSpec() {
    init {
        test("세션 파일 읽기 실패는 등록 불가능 상태로 처리한다") {
            runTest {
                val store =
                    object : DataStore<Preferences> {
                        override val data = flow<Preferences> { throw IOException("read failed") }

                        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences = error("unused")
                    }
                LocalAuthDataSourceImpl(store).observeRegisteredAccessToken().first() shouldBe null
            }
        }

        test("세션 관찰 취소는 흡수하지 않는다") {
            runTest {
                val store =
                    object : DataStore<Preferences> {
                        override val data = flow<Preferences> { throw CancellationException("cancel read") }

                        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences = error("unused")
                    }
                shouldThrow<CancellationException> {
                    LocalAuthDataSourceImpl(store).observeRegisteredAccessToken().first()
                }
            }
        }

        test("인증 토큰과 회원가입 완료 상태가 모두 있어야 등록 가능한 세션이다") {
            runTest {
                val source = LocalAuthDataSourceImpl(MemoryDataStore())
                source.observeRegisteredAccessToken().first() shouldBe null
                source.saveTokens("access", "refresh")
                source.observeRegisteredAccessToken().first() shouldBe null
                source.saveRegistrationComplete(false)
                source.observeRegisteredAccessToken().first() shouldBe null
                source.saveRegistrationComplete(true)
                source.observeRegisteredAccessToken().first() shouldBe "access"
                source.clear()
                source.observeRegisteredAccessToken().first() shouldBe null
            }
        }
    }
}

private class MemoryDataStore : DataStore<Preferences> {
    override val data = MutableStateFlow(emptyPreferences())

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val updated = transform(data.value)
        data.value = updated
        return updated
    }
}
