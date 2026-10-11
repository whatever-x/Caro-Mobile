package com.whatever.caro.core.designsystem

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty

class ImportStringsContractTest :
    FunSpec({
        test("모든 import_* 키가 ko 파일에 있다") {
            val missing = expectedKeys() - ImportStringsXml.importEntries(StringsLocale.Ko).keys

            withClue("values-ko/strings.xml에 없는 키") { missing.shouldBeEmpty() }
        }

        test("모든 import_* 키가 en 파일에 있다") {
            val missing = expectedKeys() - ImportStringsXml.importEntries(StringsLocale.En).keys

            withClue("values/strings.xml에 없는 키") { missing.shouldBeEmpty() }
        }

        test("키마다 형식 인자 집합이 ko와 en에서 같다") {
            val mismatched =
                expectedKeys().filter { key ->
                    ImportStringsXml.formatArgs(valueOf(StringsLocale.Ko, key)) !=
                        ImportStringsXml.formatArgs(valueOf(StringsLocale.En, key))
                }

            mismatched.shouldBeEmpty()
        }

        test("형식 인자는 %N\$s나 %N\$d 위치 지정 방식만 쓴다") {
            val malformed = localizedValues().filter { '%' in ImportStringsXml.withoutFormatArgs(it.value) }

            malformed.map { it.label }.shouldBeEmpty()
        }

        test("형식 인자 번호는 1부터 빠짐없이 이어진다") {
            val gapped =
                localizedValues().filter { localized ->
                    val indexes =
                        ImportStringsXml
                            .formatArgs(localized.value)
                            .map { it.index }
                            .distinct()
                            .sorted()
                    indexes != (1..indexes.size).toList()
                }

            gapped.map { it.label }.shouldBeEmpty()
        }

        test("값에 실제 줄바꿈이 없다") {
            val multiline = localizedValues().filter { '\n' in it.value || '\r' in it.value }

            multiline.map { it.label }.shouldBeEmpty()
        }

        test("값에 장수나 글자 수 상한 숫자를 직접 쓰지 않는다") {
            val hardcoded = localizedValues().filter { LIMIT_NUMBER.containsMatchIn(it.value) }

            hardcoded.map { it.label }.shouldBeEmpty()
        }

        test("값은 비어 있지 않고 앞뒤 공백이 없다") {
            val untrimmed = localizedValues().filter { it.value.isBlank() || it.value != it.value.trim() }

            untrimmed.map { it.label }.shouldBeEmpty()
        }

        test("키는 import_<kind>_<name> 형식이다") {
            val badKeys = localizedValues().filterNot { KEY_FORMAT.matches(it.key) }

            badKeys.map { it.label }.shouldBeEmpty()
        }

        test("en 값에 한글이 없다") {
            val untranslated =
                localizedValues().filter { it.locale == StringsLocale.En && HANGUL.containsMatchIn(it.value) }

            untranslated.map { it.label }.shouldBeEmpty()
        }
    })

private val LIMIT_NUMBER = Regex("""(?<![\d,])(10,?000|5,?000|1,?000,?000)(?![\d,])""")
private val KEY_FORMAT = Regex("""import_[a-z0-9]+(_[a-z0-9]+)+""")
private val HANGUL = Regex("""[ᄀ-ᇿ㄰-㆏가-힣]""")

private data class LocalizedValue(
    val locale: StringsLocale,
    val key: String,
    val value: String,
) {
    val label: String
        get() = "${locale.name}:$key"
}

// 카탈로그 키도 대상에 넣어야 파일에 import_* 키가 하나도 없을 때 검사가 빈 집합으로 통과하지 않는다.
private fun expectedKeys(): List<String> =
    (
        importKoCopy.keys +
            ImportStringsXml.importEntries(StringsLocale.Ko).keys +
            ImportStringsXml.importEntries(StringsLocale.En).keys
    ).sorted()

private fun localizedValues(): List<LocalizedValue> =
    expectedKeys().flatMap { key ->
        StringsLocale.entries.map { locale -> LocalizedValue(locale, key, valueOf(locale, key)) }
    }

private fun valueOf(
    locale: StringsLocale,
    key: String,
): String = ImportStringsXml.valueOf(locale, key)
