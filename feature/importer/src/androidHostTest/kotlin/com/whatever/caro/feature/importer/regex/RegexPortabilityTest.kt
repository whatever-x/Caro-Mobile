package com.whatever.caro.feature.importer.regex

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import java.io.File

// 기기 없이 Android ICU 호환을 지키는 가드다. 호스트 JVM 정규식은 이스케이프되지 않은 '}}'를 받아주지만 기기는 PatternSyntaxException을 던진다.
private val SCANNED_MODULES = listOf("feature/importer", "core/importparser")
private const val MIN_KNOWN_SITES = 20

private val KNOWN_REGEX_FILES =
    setOf(
        "ClozeTransformer.kt",
        "HtmlBlockStripper.kt",
        "HtmlCleaner.kt",
        "HtmlEntities.kt",
        "TextCheckTable.kt",
        "HtmlSignal.kt",
        "DecimalNumber.kt",
    )

private fun projectRoot(): File =
    generateSequence(File("").absoluteFile) { it.parentFile }
        .first { File(it, "feature/importer").isDirectory && File(it, "core/importparser").isDirectory }

class RegexPortabilityTest :
    FunSpec({
        val sites = RegexLiteralScanner.scan(projectRoot(), SCANNED_MODULES)

        test("수집기는 알려진 정규식 파일을 모두 찾는다") {
            sites.map { it.location.substringBefore(':').substringAfterLast('/') }.toSet() shouldContainAll KNOWN_REGEX_FILES
            sites.size shouldBeGreaterThanOrEqual MIN_KNOWN_SITES
        }

        test("importer와 파서의 모든 Regex 리터럴을 평가할 수 있다") {
            val failures = sites.filter { it.failure != null }.map { "${it.location} ${it.failure}" }

            withClue("평가하지 못한 Regex 리터럴") { failures.shouldBeEmpty() }
        }

        test("importer와 파서의 정규식은 Android ICU에서도 컴파일되는 문법만 쓴다") {
            val violations =
                sites
                    .filter { it.pattern != null }
                    .flatMap { site ->
                        IcuRegexChecker.violations(site.pattern.orEmpty(), site.options).map { "${site.location} ${site.pattern} -> $it" }
                    }

            withClue("ICU 비호환 정규식") { violations.shouldBeEmpty() }
        }
    })
