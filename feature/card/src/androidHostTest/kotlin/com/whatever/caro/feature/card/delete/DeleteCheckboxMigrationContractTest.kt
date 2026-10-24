package com.whatever.caro.feature.card.delete

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File

class DeleteCheckboxMigrationContractTest :
    FunSpec({
        test("카드 삭제 줄은 공용 CaroCheckbox를 쓴다") {
            val source = findProjectFile("$CARD_MAIN_DIR/delete/components/DeleteCardListItem.kt").readText()

            source shouldContain "CaroCheckbox("
        }

        test("feature card 소스에는 SelectCheckbox가 남지 않는다") {
            val remaining =
                findProjectFile(CARD_MAIN_DIR)
                    .walkTopDown()
                    .filter { it.isFile && it.extension == "kt" && it.readText().contains("SelectCheckbox") }
                    .map { it.name }
                    .toList()

            remaining shouldBe emptyList()
        }
    })

private const val CARD_MAIN_DIR = "feature/card/src/commonMain/kotlin/com/whatever/caro/feature/card"

private fun findProjectFile(relativePath: String): File =
    generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, relativePath) }
        .first { it.exists() }
