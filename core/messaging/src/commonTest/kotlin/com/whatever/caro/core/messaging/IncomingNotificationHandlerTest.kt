package com.whatever.caro.core.messaging

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class IncomingNotificationHandlerTest :
    FunSpec({
        test("제목과 본문을 한 번 표시한다") {
            val presenter = RecordingPresenter()
            IncomingNotificationHandler(presenter).onReceived("Caro test", "FCM notification test")

            presenter.calls shouldBe listOf("Caro test" to "FCM notification test")
        }

        test("제목이 없어도 본문을 표시한다") {
            val presenter = RecordingPresenter()
            IncomingNotificationHandler(presenter).onReceived(null, "본문")

            presenter.calls shouldBe listOf("" to "본문")
        }

        test("제목과 본문이 공백이면 표시하지 않는다") {
            val presenter = RecordingPresenter()
            IncomingNotificationHandler(presenter).onReceived(" ", null)

            presenter.calls shouldBe emptyList()
        }
    })

private class RecordingPresenter : NotificationPresenter {
    val calls = mutableListOf<Pair<String, String>>()

    override fun show(
        title: String,
        body: String,
    ) {
        calls += title to body
    }
}
