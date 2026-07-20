import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.File

class LearningCompletionLayoutContractTest :
    FunSpec({
        test("학습 완료 통계는 Figma 높이와 단일행 라벨 정책을 사용한다") {
            val source = findLearningComponents().readText()
            val completionStat =
                source
                    .substringAfter("private fun CompletionStat(")
                    .substringBefore("private val LearningTopBarHeight")

            completionStat shouldContain ".height(LearningCompletionStatHeight)"
            completionStat shouldNotContain ".padding(CaroTheme.spacing.m)"
            completionStat shouldContain "maxLines = 1"
            completionStat shouldContain "softWrap = false"
            completionStat shouldContain "overflow = TextOverflow.Ellipsis"
        }
    })

private fun findLearningComponents(): File {
    val relative =
        "feature/learning/src/commonMain/kotlin/com/whatever/caro/feature/learning/components/" +
            "LearningComponents.kt"

    return generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, relative) }
        .first { it.exists() }
}
