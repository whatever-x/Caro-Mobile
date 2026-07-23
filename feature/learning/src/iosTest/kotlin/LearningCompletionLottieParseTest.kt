import caromobile.core.designsystem.generated.resources.Res
import io.github.alexzhirkevich.compottie.LottieComposition
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class LearningCompletionLottieParseTest :
    FunSpec({
        test("학습 완료 Lottie에는 불투명 배경 레이어가 없다") {
            runTest {
                val json = Res.readBytes("files/lottie_check_pop.json").decodeToString()
                val layerNames =
                    Json
                        .parseToJsonElement(json)
                        .jsonObject
                        .getValue("layers")
                        .jsonArray
                        .map {
                            it.jsonObject
                                .getValue("nm")
                                .jsonPrimitive.content
                        }

                layerNames shouldNotContain "background"
            }
        }

        test("학습 완료 Lottie 리소스를 Compottie로 파싱한다") {
            runTest {
                val json = Res.readBytes("files/lottie_check_pop.json").decodeToString()
                val composition = LottieComposition.parse(json)

                composition.durationFrames shouldBe 60f
                composition.width shouldBe 150f
                composition.height shouldBe 150f
            }
        }
    })
