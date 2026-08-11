package com.whatever.caro.composeApp

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.File

class SystemBarBackgroundLayoutContractTest : FunSpec() {
    init {
        test("navigation bar 배경은 navigation bar inset 영역만 채운다") {
            val source =
                findProjectFile(
                    "composeApp/src/commonMain/kotlin/com/whatever/caro/composeApp/SystemBarBackground.kt",
                ).readText()
            val systemBarBackground =
                source
                    .substringAfter("internal fun CaroSystemBarBackground(")
                    .substringBefore("@Composable\nprivate fun SystemBarBackgroundRole.color()")
            val rootBox = systemBarBackground.substringAfter("Box(").substringBefore(") {")

            systemBarBackground shouldContain "windowInsetsBottomHeight(WindowInsets.navigationBars)"
            rootBox shouldNotContain ".background(navigationBarColor)"
        }
    }
}

private fun findProjectFile(relativePath: String): File =
    generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, relativePath) }
        .first { it.exists() }
