package com.whatever.caro.feature.setting.platform

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.ComponentActivity
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class InquiryUrlAndroidTest {
    @Test
    fun `successful URL handoff launches the exact external intent`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
            try {
                openInquiryUrl(controller.get(), "https://example.com/inquiry") shouldBe true
                val intent = shadowOf(controller.get()).nextStartedActivity
                intent.action shouldBe Intent.ACTION_VIEW
                intent.dataString shouldBe "https://example.com/inquiry"
            } finally {
                controller.pause().stop().destroy()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `unhandled URL returns failure without registering a successful open`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val controller = Robolectric.buildActivity(RejectingInquiryActivity::class.java).setup()
            try {
                openInquiryUrl(controller.get(), "https://example.com/inquiry") shouldBe false
            } finally {
                controller.pause().stop().destroy()
                Dispatchers.resetMain()
            }
        }
}

class RejectingInquiryActivity : ComponentActivity() {
    override fun startActivity(intent: Intent?): Unit = throw ActivityNotFoundException("No browser")
}
