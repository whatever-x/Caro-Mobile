package com.whatever.caro.core.designsystem

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.File

class DesignSystemReviewContractTest :
    FunSpec({
        test("텍스트 영역 테두리는 focus 여부로만 분기한다") {
            val source =
                findProjectFile(
                    "core/designsystem/src/commonMain/kotlin/com/whatever/caro/core/designsystem/" +
                        "components/CaroTextArea.kt",
                ).readText()
            val borderColorBranch =
                source
                    .substringAfter("val borderColor =")
                    .substringBefore("val mergedTextStyle =")
            val borderArguments =
                source
                    .substringAfter(".border(")
                    .substringBefore(").padding(")

            borderColorBranch shouldContain "isFocused"
            borderColorBranch shouldNotContain "enabled"
            borderArguments shouldContain "color = borderColor"
        }

        test("텍스트 영역 minHeight 기본값은 90dp다") {
            val source = readTextAreaSource()

            source.dpDefaultOf("minHeight") shouldBe "90.dp"
        }

        test("텍스트 영역 maxHeight 기본값은 130dp다") {
            val source = readTextAreaSource()

            source.dpDefaultOf("maxHeight") shouldBe "130.dp"
        }

        test("텍스트 영역 fillHeight 기본값은 false다") {
            val source = readTextAreaSource()

            source.signatureLineOf("fillHeight") shouldBe "fillHeight: Boolean = false"
        }

        test("Pretendard 타이포그래피는 zero letter spacing 기본값을 공유한다") {
            val source =
                findProjectFile(
                    "core/designsystem/src/commonMain/kotlin/com/whatever/caro/core/designsystem/" +
                        "themes/CaroTypography.kt",
                ).readText()

            source.countOccurrences("letterSpacing = 0.em") shouldBeExactly 1
            source.countOccurrences("basePretendardStyle.copy(") shouldBeExactly 13
        }
    })

private fun readTextAreaSource(): String =
    findProjectFile(
        "core/designsystem/src/commonMain/kotlin/com/whatever/caro/core/designsystem/" +
            "components/CaroTextArea.kt",
    ).readText()

private fun String.signatureLineOf(parameter: String): String? =
    substringAfter("fun CaroTextArea(")
        .substringBefore("\n) {")
        .lineSequence()
        .map { it.trim().removeSuffix(",") }
        .firstOrNull { it.startsWith("$parameter:") }

// 기본값이 리터럴이든 같은 파일의 상수 참조든 최종 dp 값으로 비교한다.
private fun String.dpDefaultOf(parameter: String): String? {
    val expression = signatureLineOf(parameter)?.substringAfter("=", missingDelimiterValue = "")?.trim()
    if (expression.isNullOrEmpty()) return null
    val constantValue =
        Regex("""val\s+${Regex.escape(expression)}\s*=\s*(\S+)""")
            .find(this)
            ?.groupValues
            ?.get(1)
    return constantValue ?: expression
}

private fun findProjectFile(relativePath: String): File =
    generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, relativePath) }
        .first { it.exists() }

private fun String.countOccurrences(value: String): Int = windowed(size = value.length).count { it == value }
