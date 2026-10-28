@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.whatever.caro.feature.setting.platform

import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UISceneDidActivateNotification
import platform.UIKit.UISceneDidEnterBackgroundNotification
import platform.UIKit.UISceneWillDeactivateNotification
import platform.darwin.NSObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InquiryPlatformIosTest {
    @Test
    fun `only the current scene background and activation are observed`() {
        val center = NSNotificationCenter()
        val host = NSObject()
        val other = NSObject()
        val events = mutableListOf<String>()
        var currentScene: Any? = null
        val dispose =
            observeInquirySceneLifecycle(
                currentScene = { currentScene },
                onBackground = { events += "background" },
                onForeground = { events += "foreground" },
                notificationCenter = center,
            )
        try {
            center.postNotificationName(UISceneDidEnterBackgroundNotification, host)
            currentScene = host
            center.postNotificationName(UISceneDidEnterBackgroundNotification, other)
            center.postNotificationName(UISceneDidActivateNotification, other)
            center.postNotificationName(UISceneWillDeactivateNotification, host)
            assertEquals(emptyList(), events)

            center.postNotificationName(UISceneDidEnterBackgroundNotification, host)
            center.postNotificationName(UISceneDidActivateNotification, host)
            assertEquals(listOf("background", "foreground"), events)
        } finally {
            dispose()
        }
        center.postNotificationName(UISceneDidEnterBackgroundNotification, host)
        center.postNotificationName(UISceneDidActivateNotification, host)
        assertEquals(listOf("background", "foreground"), events)
    }

    @Test
    fun `URL open waits for UIKit completion and returns failure`() =
        runTest {
            lateinit var complete: (Boolean) -> Unit
            var launchedUrl: String? = null
            val result =
                async {
                    openInquiryUrl("https://example.com/inquiry") { url, callback ->
                        launchedUrl = url.absoluteString
                        complete = callback
                    }
                }
            runCurrent()
            assertEquals("https://example.com/inquiry", launchedUrl)
            assertFalse(result.isCompleted)

            complete(false)
            runCurrent()

            assertFalse(result.await())
        }

    @Test
    fun `duplicate URL completion keeps the first successful result`() =
        runTest {
            lateinit var complete: (Boolean) -> Unit
            val result =
                async {
                    openInquiryUrl("https://example.com/inquiry") { _, callback -> complete = callback }
                }
            runCurrent()
            complete(true)
            complete(false)
            runCurrent()

            assertTrue(result.await())
        }

    @Test
    fun `late UIKit success cannot complete a cancelled open`() =
        runTest {
            lateinit var complete: (Boolean) -> Unit
            val result =
                async {
                    openInquiryUrl("https://example.com/inquiry") { _, callback -> complete = callback }
                }
            runCurrent()
            result.cancelAndJoin()
            complete(true)
            complete(true)
            runCurrent()

            assertTrue(result.isCancelled)
        }
}
