package com.whatever.caro.core.messaging

import app.cash.turbine.test
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class MessagingEventBusTest :
    FunSpec({
        test("publishToken 후 tokenFlow.value는 마지막 토큰을 노출한다") {
            runTest {
                val token = "token-${Random.nextLong()}"
                MessagingEventBus.publishToken(token)

                MessagingEventBus.tokenFlow.value shouldBe token
            }
        }

        test("활성 구독자는 publishToken 호출마다 새 토큰을 받는다") {
            runTest {
                val sentinel = "sentinel-${Random.nextLong()}"
                MessagingEventBus.publishToken(sentinel)

                MessagingEventBus.tokenFlow.test {
                    awaitItem() shouldBe sentinel

                    val next = "next-${Random.nextLong()}"
                    MessagingEventBus.publishToken(next)
                    awaitItem() shouldBe next

                    cancel()
                }
            }
        }

        test("tokenFlow는 conflation으로 마지막 토큰만 노출한다") {
            runTest {
                MessagingEventBus.publishToken("a")
                MessagingEventBus.publishToken("b")
                MessagingEventBus.publishToken("c")

                MessagingEventBus.tokenFlow.value shouldBe "c"
            }
        }

        test("publishMessage 후 messages.receive()로 메시지를 받는다") {
            runTest {
                val message = CloudMessage(anyValue = "TestValue")
                MessagingEventBus.publishMessage(message)

                MessagingEventBus.messages.receive() shouldBe message
            }
        }

        test("publishMessage 호출마다 receive로 새 메시지를 받을 수 있다") {
            runTest {
                val first = CloudMessage(anyValue = "TestValue-1")
                MessagingEventBus.publishMessage(first)
                MessagingEventBus.messages.receive() shouldBe first

                val second = CloudMessage(anyValue = "TestValue-2")
                MessagingEventBus.publishMessage(second)
                MessagingEventBus.messages.receive() shouldBe second
            }
        }

        test("CONFLATED Channel은 burst에도 publishMessage가 실패하지 않고 최신만 보관한다") {
            runTest {
                repeat(100) { i ->
                    MessagingEventBus.publishMessage(CloudMessage(anyValue = "TestValue-$i"))
                }

                MessagingEventBus.messages.receive() shouldBe CloudMessage(anyValue = "TestValue-99")
            }
        }
    })
