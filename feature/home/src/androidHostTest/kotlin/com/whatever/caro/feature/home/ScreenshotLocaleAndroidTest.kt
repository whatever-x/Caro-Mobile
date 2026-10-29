package com.whatever.caro.feature.home

import android.os.LocaleList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ScreenshotLocaleAndroidTest {
    @Test
    fun restoresGlobalsAfterSuccessAndException() {
        val locale = Locale.getDefault()
        val locales = LocaleList.getDefault()
        val qualifiers = RuntimeEnvironment.getQualifiers()
        withAndroidScreenshotLocale("ko") {
            assertEquals("ko", Locale.getDefault().language)
            assertEquals("ko", LocaleList.getDefault()[0].language)
        }
        assertEquals(locale, Locale.getDefault())
        assertEquals(locales, LocaleList.getDefault())
        assertEquals(qualifiers, RuntimeEnvironment.getQualifiers())
        assertThrows(IllegalStateException::class.java) {
            withAndroidScreenshotLocale("en") {
                assertEquals("en", Locale.getDefault().language)
                error("render failed")
            }
        }
        assertEquals(locale, Locale.getDefault())
        assertEquals(locales, LocaleList.getDefault())
        assertEquals(qualifiers, RuntimeEnvironment.getQualifiers())
    }
}
