package com.whatever.caro.feature.importer.regex

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File
import java.nio.file.Files

private const val MODULE = "feature/sample"

private fun scanSource(source: String): List<RegexSite> {
    val root = Files.createTempDirectory("regex-scan").toFile()
    try {
        val dir = File(root, "$MODULE/src/commonMain/kotlin").apply { mkdirs() }
        File(dir, "Sample.kt").writeText(source)
        return RegexLiteralScanner.scan(root, listOf(MODULE))
    } finally {
        root.deleteRecursively()
    }
}

class RegexLiteralScannerTest :
    FunSpec({
        test("이스케이프된 일반 문자열 리터럴을 실제 패턴 문자열로 푼다") {
            val sites = scanSource("""val a = Regex("\\{\\{c\\d+::|}}")""")

            sites.single().pattern shouldBe """\{\{c\d+::|}}"""
        }

        test("raw 문자열은 이스케이프 없이 읽고 끝의 \$는 글자로 둔다") {
            val sites = scanSource("val a = Regex(\"\"\"^(\\d+)\$\"\"\")")

            sites.single().pattern shouldBe """^(\d+)$"""
        }

        test("상수 보간과 이어붙이기와 replace를 평가한다") {
            val sites =
                scanSource(
                    """
                    const val NAMES = "a|b" +
                        "|c"
                    const val END = "(?:</%s>|${'$'})"
                    val a = Regex("<(${'$'}NAMES)>" + END.replace("%s", "x"), RegexOption.IGNORE_CASE)
                    """.trimIndent(),
                )

            sites.single().pattern shouldBe "<(a|b|c)>(?:</x>|$)"
            sites.single().options shouldBe listOf("IGNORE_CASE")
        }

        test("블록 보간 \${NAME}과 점으로 이어진 상수 경로도 푼다") {
            val sites =
                scanSource(
                    """
                    object Names {
                        const val TAGS = "b|i"
                    }
                    val a = Regex("<(${'$'}{Names.TAGS})>")
                    """.trimIndent(),
                )

            sites.single().pattern shouldBe "<(b|i)>"
        }

        test("한 선언의 Regex 두 개를 줄 번호와 함께 따로 수집한다") {
            val sites = scanSource("val x = 1\nval a = listOf(Regex(\"a\"), Regex(\"b\"))")

            sites.map { it.location.substringAfter(':') } shouldBe listOf("2", "2")
            sites.map { it.pattern } shouldBe listOf("a", "b")
        }

        test("주석 줄의 Regex(는 수집하지 않는다") {
            val sites = scanSource("// Regex(\"a\")\n/**\n * Regex(\"b\")\n */\nval a = Regex(\"c\")")

            sites.map { it.pattern } shouldBe listOf("c")
        }

        test("함수 호출로 만든 패턴은 조용히 건너뛰지 않고 failure로 남긴다") {
            val sites = scanSource("val a = Regex(buildPattern())")

            sites.single().failure.orEmpty() shouldContain "buildPattern"
        }

        test("정의를 찾을 수 없는 상수 보간은 failure로 남긴다") {
            val sites = scanSource("val a = Regex(\"x\$MISSING\")")

            sites.single().failure.orEmpty() shouldContain "MISSING"
        }

        test("toRegex 확장 호출은 failure로 남긴다") {
            val sites = scanSource("val a = \"x\".toRegex()")

            sites.single().failure.orEmpty() shouldContain "toRegex"
        }

        test("Regex 호출이 없는 소스에서는 아무것도 수집하지 않는다") {
            scanSource("val a = RegexOption.IGNORE_CASE").shouldBeEmpty()
        }

        test("main 소스 세트가 아닌 폴더는 읽지 않는다") {
            val root = Files.createTempDirectory("regex-scan").toFile()
            try {
                File(root, "$MODULE/src/commonTest/kotlin").apply { mkdirs() }.resolve("T.kt").writeText("val a = Regex(\"a\")")

                RegexLiteralScanner.scan(root, listOf(MODULE)) shouldHaveSize 0
            } finally {
                root.deleteRecursively()
            }
        }
    })
