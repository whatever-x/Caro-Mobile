import app.cash.turbine.test
import com.whatever.caro.core.data.repository.profile.ProfileRepository
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.profile.NicknameValidationResult
import com.whatever.caro.feature.profile.NicknameValidator
import com.whatever.caro.feature.profile.edit.EditProfileViewModel
import com.whatever.caro.feature.profile.edit.mvi.EditProfileIntent
import com.whatever.caro.feature.profile.edit.mvi.EditProfileSideEffect
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode.Companion.exactly
import dev.mokkery.verifySuspend
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class EditProfileViewModelTest : FunSpec() {
    init {
        val dispatcher = StandardTestDispatcher()

        beforeTest {
            Dispatchers.setMain(dispatcher)
        }

        afterTest {
            Dispatchers.resetMain()
        }

        fun createViewModel(
            nickname: String = "기존닉네임",
            randomNickname: String = "랜덤닉네임",
            isAvailable: Boolean = true,
        ): Pair<EditProfileViewModel, ProfileRepository> {
            val profileRepository =
                mock<ProfileRepository> {
                    everySuspend { getRandomNickname() } returns randomNickname
                    everySuspend { isNicknameAvailable(any()) } returns isAvailable
                    everySuspend { updateNickname(any()) } returns Unit
                }
            val viewModel =
                EditProfileViewModel(
                    profileRepository = profileRepository,
                    nicknameValidator = NicknameValidator(),
                    nickname = nickname,
                    exceptionFilter = ExceptionFilter.None,
                )
            return viewModel to profileRepository
        }

        test("초기 nickname 으로 state 가 채워지고 기본 검증 결과는 Valid 다") {
            runTest {
                val (viewModel, _) = createViewModel(nickname = "기존닉네임")

                advanceUntilIdle()

                viewModel.state.value.nickname shouldBe "기존닉네임"
                viewModel.state.value.validationResult shouldBe NicknameValidationResult.Valid
            }
        }

        test("최소 길이 미만 닉네임은 서버 확인 없이 TooShort 로 표시된다") {
            runTest {
                val (viewModel, profileRepository) = createViewModel()
                advanceUntilIdle()

                viewModel.intent(EditProfileIntent.UpdateNickname("a"))
                advanceUntilIdle()

                viewModel.state.value.validationResult shouldBe NicknameValidationResult.TooShort
                verifySuspend(exactly(0)) { profileRepository.isNicknameAvailable("a") }
            }
        }

        test("형식이 유효하고 중복이 아니면 Valid 가 된다") {
            runTest {
                val (viewModel, _) = createViewModel(isAvailable = true)
                advanceUntilIdle()

                viewModel.intent(EditProfileIntent.UpdateNickname("거북이"))
                advanceUntilIdle()

                viewModel.state.value.validationResult shouldBe NicknameValidationResult.Valid
            }
        }

        test("형식이 유효하지만 이미 사용 중이면 Duplicate 가 된다") {
            runTest {
                val (viewModel, _) = createViewModel(isAvailable = false)
                advanceUntilIdle()

                viewModel.intent(EditProfileIntent.UpdateNickname("거북이"))
                advanceUntilIdle()

                viewModel.state.value.validationResult shouldBe NicknameValidationResult.Duplicate
            }
        }

        test("ClickRefresh 는 랜덤 닉네임을 받아 Valid 로 채우고 로딩을 해제한다") {
            runTest {
                val (viewModel, _) = createViewModel(randomNickname = "거북이123")
                advanceUntilIdle()

                viewModel.intent(EditProfileIntent.ClickRefresh)
                advanceUntilIdle()

                viewModel.state.value.nickname shouldBe "거북이123"
                viewModel.state.value.validationResult shouldBe NicknameValidationResult.Valid
                viewModel.state.value.isRandomNicknameLoading shouldBe false
            }
        }

        test("유효한 상태에서 ClickConfirm 은 updateNickname 후 NavigateBack 을 emit 한다") {
            runTest {
                val (viewModel, profileRepository) = createViewModel(nickname = "거북이")
                advanceUntilIdle()

                viewModel.sideEffect.test {
                    viewModel.intent(EditProfileIntent.ClickConfirm)
                    advanceUntilIdle()

                    awaitItem() shouldBe EditProfileSideEffect.NavigateBack
                }
                verifySuspend(exactly(1)) { profileRepository.updateNickname(nickname = "거북이") }
            }
        }

        test("프로필 저장 중 사용자 Intent 를 무시하고 저장 시작 시점의 닉네임을 전달한다") {
            runTest {
                val (viewModel, profileRepository) = createViewModel(nickname = "저장할닉네임")
                val saveGate = CompletableDeferred<Unit>()
                val randomNicknameGate = CompletableDeferred<Unit>()
                everySuspend {
                    profileRepository.updateNickname(any())
                } calls {
                    saveGate.await()
                }
                everySuspend {
                    profileRepository.getRandomNickname()
                } calls {
                    randomNicknameGate.await()
                    "뒤늦게도착한닉네임"
                }

                viewModel.sideEffect.test {
                    viewModel.intent(EditProfileIntent.ClickRefresh)
                    runCurrent()
                    viewModel.intent(EditProfileIntent.ClickConfirm)
                    viewModel.intent(EditProfileIntent.UpdateNickname("변경된닉네임"))
                    runCurrent()

                    viewModel.state.value.isLoading shouldBe true
                    viewModel.state.value.nickname shouldBe "저장할닉네임"

                    viewModel.intent(EditProfileIntent.ClickRefresh)
                    viewModel.intent(EditProfileIntent.ClickConfirm)
                    viewModel.intent(EditProfileIntent.ClickBack)
                    runCurrent()
                    randomNicknameGate.complete(Unit)
                    runCurrent()

                    viewModel.state.value.nickname shouldBe "저장할닉네임"
                    expectNoEvents()
                    verifySuspend(exactly(1)) {
                        profileRepository.getRandomNickname()
                    }
                    verifySuspend(exactly(1)) {
                        profileRepository.updateNickname(nickname = "저장할닉네임")
                    }

                    saveGate.complete(Unit)
                    awaitItem() shouldBe EditProfileSideEffect.NavigateBack
                }
            }
        }

        test("검증 결과가 유효하지 않으면 ClickConfirm 은 무시된다") {
            runTest {
                val (viewModel, profileRepository) = createViewModel()
                advanceUntilIdle()

                viewModel.intent(EditProfileIntent.UpdateNickname("a"))
                advanceUntilIdle()

                viewModel.sideEffect.test {
                    viewModel.intent(EditProfileIntent.ClickConfirm)
                    advanceUntilIdle()

                    expectNoEvents()
                }
                verifySuspend(exactly(0)) { profileRepository.updateNickname(any()) }
            }
        }

        test("ClickBack 은 NavigateBack 을 emit 한다") {
            runTest {
                val (viewModel, _) = createViewModel()
                advanceUntilIdle()

                viewModel.sideEffect.test {
                    viewModel.intent(EditProfileIntent.ClickBack)
                    advanceUntilIdle()

                    awaitItem() shouldBe EditProfileSideEffect.NavigateBack
                }
            }
        }
    }
}
