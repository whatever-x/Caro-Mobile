package com.whatever.caro.core.designsystem

import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.allStringResources
import io.kotest.matchers.collections.shouldBeEmpty
import kotlinx.coroutines.test.runTest
import org.jetbrains.compose.resources.ResourceEnvironment
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.getSystemResourceEnvironment
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

// \n이 화면에 어떻게 나오는지는 Compose 리소스 변환을 거친 뒤에만 알 수 있어 XML 원문이 아니라 getString으로 읽는다.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ImportStringsResourceTest {
    @Test
    fun `값의 역슬래시 n은 실제 줄바꿈으로 읽히고 역슬래시가 남지 않는다`() =
        runTest {
            val broken =
                RESOURCE_LOCALES.flatMap { (stringsLocale, locale) ->
                    val environment = environmentOf(locale)
                    ImportStringsXml.importEntries(stringsLocale).mapNotNull { (key, raw) ->
                        val args = Array(ImportStringsXml.formatArgs(raw).size) { "0" }
                        val rendered = getString(environment, Res.allStringResources.getValue(key), *args)
                        val escapeBroken = '\\' in rendered || rendered.count { it == '\n' } != raw.split("\\n").size - 1
                        "${stringsLocale.name}:$key".takeIf { escapeBroken }
                    }
                }

            broken.shouldBeEmpty()
        }
}

private val RESOURCE_LOCALES = listOf(StringsLocale.Ko to Locale.KOREAN, StringsLocale.En to Locale.ENGLISH)

// getSystemResourceEnvironment는 Locale 기본값으로 언어를 고르므로 잠깐 바꾸고 바로 되돌린다.
private fun environmentOf(locale: Locale): ResourceEnvironment {
    val previous = Locale.getDefault()
    return try {
        Locale.setDefault(locale)
        getSystemResourceEnvironment()
    } finally {
        Locale.setDefault(previous)
    }
}
