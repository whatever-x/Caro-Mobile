package com.whatever.caro.feature.home

import platform.Foundation.NSUserDefaults
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ScreenshotLocaleIosTest {
    @Test
    fun restoresAppleLanguagesAfterSuccessAndException() {
        val defaults = NSUserDefaults.standardUserDefaults
        val domain = iosScreenshotDefaultsDomain()
        val previous = defaults.persistentDomainForName(domain)?.get("AppleLanguages")
        try {
            for (initial in listOf<List<String>?>(null, listOf("fr"))) {
                for (fails in listOf(false, true)) {
                    if (initial ==
                        null
                    ) {
                        defaults.removeObjectForKey("AppleLanguages")
                    } else {
                        defaults.setObject(initial, forKey = "AppleLanguages")
                    }
                    val storedBefore = defaults.persistentDomainForName(domain).orEmpty().toMap()
                    val effectiveBefore = defaults.objectForKey("AppleLanguages")
                    val language = if (fails) "en" else "ko"
                    val render = {
                        withIosScreenshotLocale(language) {
                            assertEquals(listOf(language), defaults.objectForKey("AppleLanguages"))
                            assertEquals(listOf(language), defaults.persistentDomainForName(domain)?.get("AppleLanguages"))
                            if (fails) error("render failed")
                        }
                    }
                    if (fails) assertFailsWith<IllegalStateException> { render() } else render()
                    assertEquals(initial, defaults.persistentDomainForName(domain)?.get("AppleLanguages"))
                    assertEquals(storedBefore, defaults.persistentDomainForName(domain).orEmpty())
                    assertEquals(effectiveBefore, defaults.objectForKey("AppleLanguages"))
                }
            }
        } finally {
            if (previous == null) defaults.removeObjectForKey("AppleLanguages") else defaults.setObject(previous, forKey = "AppleLanguages")
        }
    }
}
