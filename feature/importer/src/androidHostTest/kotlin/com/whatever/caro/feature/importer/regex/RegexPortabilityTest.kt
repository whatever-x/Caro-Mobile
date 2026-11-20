package com.whatever.caro.feature.importer.regex

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import java.io.File
import java.lang.reflect.Modifier
import java.util.regex.Pattern

// 호스트 JVM은 수량자를 닫지 않는 이스케이프 없는 `}`를 받아주지만 Android ICU는 PatternSyntaxException을 던져 object 초기화가 실패한다.
// 그래서 컴파일 성공이 아니라 패턴 글자를 검사한다.
private val REGEX_HOLDERS =
    listOf(
        "com.whatever.caro.feature.importer.transform.ClozeTransformer",
        "com.whatever.caro.feature.importer.transform.HtmlBlockStripper",
        "com.whatever.caro.feature.importer.transform.HtmlCleaner",
        "com.whatever.caro.feature.importer.transform.HtmlEntities",
        "com.whatever.caro.feature.importer.logic.TextCheckTableKt",
        "com.whatever.caro.core.importparser.HtmlSignal",
        "com.whatever.caro.core.importparser.xlsx.DecimalNumber",
    )
private val SCANNED_SOURCE_DIRS =
    listOf("feature/importer", "core/importparser").flatMap { module ->
        listOf("commonMain", "androidMain").map { "$module/src/$it" }
    }
private val REGEX_CALL = Regex("""\b(?:to)?Regex\(|\bPattern\.compile\(""")

// `\p{L}`, `\x{41}`처럼 중괄호를 받는 이스케이프는 통째로 건너뛴다. 문자 클래스 안의 `}`도 거부하며 `\}`로 쓰면 ICU와 JVM이 같게 읽는다.
private val ESCAPE_OR_QUANTIFIER = Regex("""\\[pPxN]\{[^\}]*\}|\\.|\{\d+(?:,\d*)?\}""")

private fun hasStrayClosingBrace(pattern: String): Boolean = ESCAPE_OR_QUANTIFIER.replace(pattern, "").contains('}')

private fun fieldValues(
    type: Class<*>,
    owner: Any?,
): List<Any?> =
    type.declaredFields
        .filter { Modifier.isStatic(it.modifiers) == (owner == null) }
        .map { field -> field.apply { isAccessible = true }.get(owner) }

private fun patternsIn(value: Any?): List<String> =
    when {
        value is Regex -> listOf(value.pattern)
        value is Pattern -> listOf(value.pattern())
        value is Iterable<*> -> value.flatMap(::patternsIn)
        value != null && value.javaClass.name.startsWith("com.whatever.caro.") -> fieldValues(value.javaClass, value).flatMap(::patternsIn)
        else -> emptyList()
    }

private fun projectRoot(): File =
    generateSequence(File("").absoluteFile) {
        it.parentFile
    }.first { File(it, "core/importparser").isDirectory }

class RegexPortabilityTest : FunSpec() {
    init {
        val patterns = REGEX_HOLDERS.flatMap { name -> fieldValues(Class.forName(name), null).flatMap(::patternsIn) }

        test("제품 코드에서 만드는 정규식은 모두 검사 대상에 모인다") {
            val sourceCount =
                SCANNED_SOURCE_DIRS
                    .flatMap { File(projectRoot(), it).walkTopDown().filter { file -> file.extension == "kt" } }
                    .sumOf { REGEX_CALL.findAll(it.readText()).count() }

            withClue("새 정규식은 object나 파일 수준 val로 두고 그 클래스를 REGEX_HOLDERS에 더한다") { patterns.size shouldBe sourceCount }
        }

        test("검사는 사고 당시 패턴의 이스케이프 없는 닫는 중괄호를 거부하고 ICU에서 유효한 중괄호는 통과시킨다") {
            hasStrayClosingBrace("""\{\{c\d+::|}}""") shouldBe true
            hasStrayClosingBrace("""\p{L}\P{Lu}\x{41}\N{DIGIT ONE}a{2}b{1,}c{1,3}\}""") shouldBe false
        }

        test("제품 코드의 정규식은 수량자 밖에서 이스케이프 없는 닫는 중괄호를 쓰지 않는다") {
            withClue("Android ICU에서 컴파일되지 않는 정규식") { patterns.filter(::hasStrayClosingBrace).shouldBeEmpty() }
        }
    }
}
