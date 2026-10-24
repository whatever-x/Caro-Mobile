package com.whatever.caro.feature.deck

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.File

class DeckComponentRelocationContractTest :
    FunSpec({
        listOf("CtaButton.kt", "TextField.kt", "DeckTipSection.kt", "Size.kt").forEach { fileName ->
            test("덱 component 디렉터리에 $fileName 파일이 남아 있지 않다") {
                File(findProjectFile(DECK_MAIN_DIR), "component/$fileName").exists() shouldBe false
            }
        }

        listOf(
            "CtaButton",
            "RequiredFieldHeader",
            "FieldCounter",
            "DeckTipSection",
            "TipDotSize",
            "PageHorizontalPadding",
            "CtaButtonHeight",
        ).forEach { identifier ->
            test("덱 소스에 $identifier 식별자가 남아 있지 않다") {
                val pattern = Regex("""\b$identifier\b""")

                val matchedFiles =
                    findProjectFile(DECK_MAIN_DIR)
                        .walkTopDown()
                        .filter { it.extension == "kt" && pattern.containsMatchIn(it.readText()) }
                        .map { it.name }
                        .toList()

                matchedFiles shouldBe emptyList()
            }
        }
    })

private const val DECK_MAIN_DIR = "feature/deck/src/commonMain/kotlin/com/whatever/caro/feature/deck"

private fun findProjectFile(relativePath: String): File =
    generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, relativePath) }
        .first { it.exists() }
