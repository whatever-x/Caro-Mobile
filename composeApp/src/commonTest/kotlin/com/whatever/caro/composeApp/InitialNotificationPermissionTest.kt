package com.whatever.caro.composeApp

import com.whatever.caro.core.datastore.datasource.LocalNotificationDataSource
import com.whatever.caro.core.ui.notification.NotificationPermissionController
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest

class InitialNotificationPermissionTest : FunSpec() {
    init {
        test("최초 실행만 권한 요청을 실행한다") {
            runTest {
                var requested = false
                val source =
                    object : LocalNotificationDataSource {
                        override suspend fun consumeInitialPermissionRequest(): Boolean = (!requested).also { requested = true }
                    }
                val permission = RecordingNotificationPermission()

                requestInitialNotificationPermission(source, permission)
                requestInitialNotificationPermission(source, permission)

                permission.requestCount shouldBe 1
            }
        }

        test("플래그 저장 실패 시 OS 권한을 요청하지 않는다") {
            runTest {
                val permission = RecordingNotificationPermission()
                val source =
                    object : LocalNotificationDataSource {
                        override suspend fun consumeInitialPermissionRequest(): Boolean = error("save failed")
                    }

                shouldThrow<IllegalStateException> { requestInitialNotificationPermission(source, permission) }

                permission.requestCount shouldBe 0
            }
        }

        test("권한 요청 거절 후 재실행해도 자동 재요청하지 않는다") {
            runTest {
                var requested = false
                val source =
                    object : LocalNotificationDataSource {
                        override suspend fun consumeInitialPermissionRequest(): Boolean = (!requested).also { requested = true }
                    }
                val permission = RecordingNotificationPermission(granted = false)

                requestInitialNotificationPermission(source, permission)
                requestInitialNotificationPermission(source, permission)

                permission.requestCount shouldBe 1
            }
        }

        test("권한 요청 취소는 호출자에게 전파한다") {
            runTest {
                val source =
                    object : LocalNotificationDataSource {
                        override suspend fun consumeInitialPermissionRequest(): Boolean = true
                    }
                val permission = RecordingNotificationPermission(failure = CancellationException("cancel request"))

                shouldThrow<CancellationException> { requestInitialNotificationPermission(source, permission) }
            }
        }
    }
}

private class RecordingNotificationPermission(
    private val granted: Boolean = true,
    private val failure: Throwable? = null,
) : NotificationPermissionController {
    var requestCount = 0

    override suspend fun isGranted(): Boolean = granted

    override suspend fun request(): Boolean {
        requestCount++
        failure?.let { throw it }
        return granted
    }

    override fun openSettings() = error("unused")
}
