package com.whatever.caro.core.designsystem

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty

class ImportStringsContractTest : FunSpec() {
    init {
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

        test("값은 직선 따옴표 대신 둥근 따옴표를 쓴다") {
            val straightQuoted = localizedValues().filter { STRAIGHT_QUOTE.containsMatchIn(it.value) }

            straightQuoted.map { it.label }.shouldBeEmpty()
        }

        test("값에 가운뎃점 계열 문자가 없다") {
            val dotted =
                localizedValues().filter { localized ->
                    localized.value.any { it.code in MIDDLE_DOT_CODES } || MIDDLE_DOT_ESCAPE.containsMatchIn(localized.value)
                }

            dotted.map { it.label }.shouldBeEmpty()
        }
    }
}

private val KEY_KINDS =
    listOf(
        "title",
        "subtitle",
        "label",
        "hint",
        "chip",
        "tip",
        "reason",
        "button",
        "loading",
        "summary",
        "value",
        "warning",
        "banner",
        "tag",
        "dialog",
        "content_description",
    )

private val LIMIT_NUMBER = Regex("""(?<![\d,])(10,?000|5,?000|1,?000,?000)(?![\d,])""")
private val KEY_FORMAT = Regex("""import_(${KEY_KINDS.joinToString("|")})(_[a-z0-9]+)+""")
private val STRAIGHT_QUOTE = Regex("""['"]""")

// 금지 문자를 소스에 직접 쓰지 않도록 code point로 적는다.
private val MIDDLE_DOT_CODES = listOf(0x00B7, 0x2022, 0x2027, 0x2219, 0x22C5, 0x30FB, 0xFF65, 0x318D)

// XML에 역슬래시 u escape로 적어도 런타임에서는 같은 문자가 되므로 escape 표기도 잡는다.
private val MIDDLE_DOT_ESCAPE =
    Regex(
        """\\u(${MIDDLE_DOT_CODES.joinToString("|") { it.toString(16).padStart(4, '0') }})""",
        RegexOption.IGNORE_CASE,
    )
private val HANGUL = Regex("""[ᄀ-ᇿ㄰-㆏가-힣]""")

private data class LocalizedValue(
    val locale: StringsLocale,
    val key: String,
    val value: String,
) {
    val label: String
        get() = "${locale.name}:$key"
}

// 키를 하나도 읽지 못하면 모든 검사가 빈 목록으로 통과하므로 여기서 실패시킨다.
private fun expectedKeys(): List<String> =
    (ImportStringsXml.importEntries(StringsLocale.Ko).keys + ImportStringsXml.importEntries(StringsLocale.En).keys)
        .sorted()
        .also { keys -> check(keys.isNotEmpty()) { "strings.xml에서 import_* 키를 읽지 못했다" } }

private fun localizedValues(): List<LocalizedValue> =
    expectedKeys().flatMap { key ->
        StringsLocale.entries.map { locale -> LocalizedValue(locale, key, valueOf(locale, key)) }
    }

private fun valueOf(
    locale: StringsLocale,
    key: String,
): String = ImportStringsXml.valueOf(locale, key)
