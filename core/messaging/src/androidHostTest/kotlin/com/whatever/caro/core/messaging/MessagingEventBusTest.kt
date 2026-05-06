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

        // MessagingEventBus는 process-wide singleton(object)이므로 테스트 간 replay 슬롯이 공유됨.
        // 각 테스트는 sentinel을 먼저 publish하여 이전 상태를 덮어쓴 뒤 검증.

        test("publishToken 후 새 구독자는 마지막 토큰을 replay 받는다") {
            runTest {
                val token = "token-${Random.nextLong()}"
                MessagingEventBus.publishToken(token)

                MessagingEventBus.tokenFlow.test {
                    awaitItem() shouldBe token
                    cancel()
                }
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

        test("tokenFlow는 가장 최신 토큰만 replay한다 (replay = 1)") {
            runTest {
                MessagingEventBus.publishToken("a")
                MessagingEventBus.publishToken("b")
                MessagingEventBus.publishToken("c")

                MessagingEventBus.tokenFlow.test {
                    awaitItem() shouldBe "c"
                    cancel()
                }
            }
        }

        test("publishMessage 후 새 구독자는 마지막 메시지를 replay 받는다") {
            runTest {
                val message = RemoteMessage(deckId = "deck-${Random.nextLong()}")
                MessagingEventBus.publishMessage(message)

                MessagingEventBus.messageFlow.test {
                    awaitItem() shouldBe message
                    cancel()
                }
            }
        }

        test("활성 구독자는 publishMessage 호출마다 새 메시지를 받는다") {
            runTest {
                val sentinel = RemoteMessage(deckId = "sentinel-${Random.nextLong()}")
                MessagingEventBus.publishMessage(sentinel)

                MessagingEventBus.messageFlow.test {
                    awaitItem() shouldBe sentinel

                    val next = RemoteMessage(deckId = "next-${Random.nextLong()}")
                    MessagingEventBus.publishMessage(next)
                    awaitItem() shouldBe next

                    cancel()
                }
            }
        }

        test("messageFlow는 buffer를 초과하는 burst에도 throw하지 않고 최신만 replay한다 (DROP_OLDEST)") {
            runTest {
                // extraBufferCapacity = 64. 구독자 없는 상태로 100건 publish해도 예외 없이 처리되어야 한다.
                repeat(100) { i ->
                    MessagingEventBus.publishMessage(RemoteMessage(deckId = "deck-$i"))
                }

                MessagingEventBus.messageFlow.test {
                    awaitItem() shouldBe RemoteMessage(deckId = "deck-99")
                    cancel()
                }
            }
        }

        test("deckId가 null인 RemoteMessage도 publish/replay 가능하다") {
            runTest {
                MessagingEventBus.publishMessage(RemoteMessage(deckId = null))

                MessagingEventBus.messageFlow.test {
                    awaitItem() shouldBe RemoteMessage(deckId = null)
                    cancel()
                }
            }
        }
    })
