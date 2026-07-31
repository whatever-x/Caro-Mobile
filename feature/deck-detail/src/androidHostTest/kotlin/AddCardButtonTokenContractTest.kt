import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.File

class AddCardButtonTokenContractTest :
    FunSpec({
        test("카드 추가 버튼은 brand surface 토큰을 사용한다") {
            val source = findAddCardButtonItem().readText()

            source shouldContain "CaroTheme.color.surface.brand"
            source shouldNotContain "CaroTheme.color.background.brand"
        }
    })

private fun findAddCardButtonItem(): File {
    val relative =
        "feature/deck-detail/src/commonMain/kotlin/com/whatever/caro/feature/deck/detail/components/" +
            "lazycolumn/AddCardButtonItem.kt"

    return generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, relative) }
        .first { it.exists() }
}
