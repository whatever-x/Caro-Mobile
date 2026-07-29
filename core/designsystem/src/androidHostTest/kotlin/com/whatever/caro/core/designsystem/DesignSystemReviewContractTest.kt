package com.whatever.caro.core.designsystem

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeExactly
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

            borderColorBranch shouldContain "isFocused"
            borderColorBranch shouldNotContain "enabled"
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

private fun findProjectFile(relativePath: String): File =
    generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, relativePath) }
        .first { it.exists() }

private fun String.countOccurrences(value: String): Int = windowed(size = value.length).count { it == value }
