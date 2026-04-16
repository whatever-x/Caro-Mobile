package com.whatever.caro.core.test.module

import com.whatever.caro.core.remote.di.networkModule
import com.whatever.caro.core.remote.di.qualifier.NetworkClient
import io.kotest.core.spec.style.FunSpec
import io.kotest.koin.KoinExtension
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import org.koin.core.qualifier.named
import org.koin.test.KoinTest
import org.koin.test.get

class NetworkModuleTest :
    FunSpec(),
    KoinTest {
    init {
        extensions(KoinExtension(listOf(networkModule)))

        test("AUTH 클라이언트와 DEFAULT 클라이언트는 서로 다른 인스턴스를 가진다.") {
            val authClient: HttpClient = get(qualifier = named(NetworkClient.AUTH))
            val defaultClient: HttpClient = get(qualifier = named(NetworkClient.DEFAULT))

            (authClient === defaultClient) shouldBe false
        }
    }
}
