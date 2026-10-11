package com.whatever.caro.core.designsystem

import java.io.File

internal enum class StringsLocale(
    private val directory: String,
) {
    En("values"),
    Ko("values-ko"),
    ;

    val relativePath: String
        get() = "core/designsystem/src/commonMain/composeResources/$directory/strings.xml"
}

internal data class FormatArg(
    val index: Int,
    val type: Char,
)

/** 실제 줄바꿈 검사를 하려면 값을 가공하지 않고 읽어야 한다. */
internal object ImportStringsXml {
    private const val KEY_PREFIX = "import_"

    private val entryPattern = Regex("""<string\s+name="([^"]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
    private val argPattern = Regex("""%(\d+)\$([sd])""")

    fun importEntries(locale: StringsLocale): Map<String, String> =
        entryPattern
            .findAll(findProjectFile(locale.relativePath).readText())
            .associate { match -> match.groupValues[1] to match.groupValues[2] }
            .filterKeys { key -> key.startsWith(KEY_PREFIX) }

    fun valueOf(
        locale: StringsLocale,
        key: String,
    ): String = requireNotNull(importEntries(locale)[key]) { "${locale.relativePath} 에 $key 문구가 없다" }

    fun formatArgs(value: String): Set<FormatArg> =
        argPattern
            .findAll(value)
            .map { match -> FormatArg(match.groupValues[1].toInt(), match.groupValues[2].single()) }
            .toSet()

    /** 인자 자리 표시를 걷어 낸 나머지에 `%`가 남으면 위치 지정이 아닌 형식이 섞인 것이다. */
    fun withoutFormatArgs(value: String): String = argPattern.replace(value, "").replace("%%", "")

    /** 인자 종류(%s, %d)를 가리지 않고 문구 모양만 비교할 때 쓴다. */
    fun normalizeArgTypes(value: String): String = argPattern.replace(value) { match -> "%${match.groupValues[1]}\$s" }

    fun render(
        template: String,
        vararg args: String,
    ): String = argPattern.replace(template) { match -> args[match.groupValues[1].toInt() - 1] }

    private fun findProjectFile(relativePath: String): File =
        generateSequence(File("").absoluteFile) { it.parentFile }
            .map { File(it, relativePath) }
            .first { it.exists() }
}
