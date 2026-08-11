import com.whatever.caro.core.model.learning.StudyRating
import com.whatever.caro.feature.learning.runEvaluationTransition
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalCoroutinesApi::class)
class LearningEvaluationTransitionTest :
    FunSpec({
        test("애니메이션이 끝나지 않아도 제한 시간이 지나면 평가한다") {
            runTest {
                var evaluated: StudyRating? = null

                runEvaluationTransition(
                    rating = StudyRating.EASY,
                    animate = { awaitCancellation() },
                    onEvaluate = { evaluated = it },
                )

                evaluated shouldBe StudyRating.EASY
            }
        }

        test("애니메이션이 완료되면 한 번만 평가한다") {
            runTest {
                val evaluations = mutableListOf<StudyRating>()

                runEvaluationTransition(
                    rating = StudyRating.FAIR,
                    animate = {},
                    onEvaluate = evaluations::add,
                )

                evaluations shouldContainExactly listOf(StudyRating.FAIR)
            }
        }

        test("애니메이션만 취소되고 화면이 살아 있으면 한 번만 평가한다") {
            runTest {
                val evaluations = mutableListOf<StudyRating>()

                val job =
                    launch {
                        runEvaluationTransition(
                            rating = StudyRating.EASY,
                            animate = { throw CancellationException("animation interrupted") },
                            onEvaluate = evaluations::add,
                        )
                    }
                job.join()

                evaluations shouldContainExactly listOf(StudyRating.EASY)
            }
        }

        test("화면 코루틴이 취소되면 평가하지 않는다") {
            runTest {
                var evaluated: StudyRating? = null
                val job =
                    launch {
                        runEvaluationTransition(
                            rating = StudyRating.AGAIN,
                            animate = { awaitCancellation() },
                            onEvaluate = { evaluated = it },
                        )
                    }

                runCurrent()
                job.cancelAndJoin()

                evaluated shouldBe null
            }
        }
    })
