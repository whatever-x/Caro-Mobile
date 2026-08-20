import com.whatever.caro.feature.profile.NicknameValidationResult
import com.whatever.caro.feature.profile.NicknameValidator
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class NicknameValidatorTest : FunSpec() {
    private val validator = NicknameValidator()

    init {
        test("한글 IME 조합 중 낱자는 입력 단계에서 유지된다") {
            validator.filterInput("ㅋ") shouldBe "ㅋ"
            validator.filterInput("카ㄹ") shouldBe "카ㄹ"
        }

        test("허용하지 않는 문자는 제거된다") {
            validator.filterInput("카로!@# 1") shouldBe "카로1"
        }

        test("입력은 최대 길이로 잘린다") {
            validator.filterInput("a".repeat(30)).length shouldBe NicknameValidator.MAX_LENGTH
        }

        test("완성되지 않은 낱자는 최종 검증에서 걸러진다") {
            validator.validate("카ㄹ") shouldBe NicknameValidationResult.InvalidCharacter
            validator.validate("카로") shouldBe NicknameValidationResult.Valid
        }
    }
}
