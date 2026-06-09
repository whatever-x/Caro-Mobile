import app.cash.turbine.test
import com.whatever.caro.core.data.repository.AuthRepository
import com.whatever.caro.core.data.repository.profile.ProfileRepository
import com.whatever.caro.core.model.auth.AuthSession
import com.whatever.caro.feature.profile.CreateProfileViewModel
import com.whatever.caro.feature.profile.NicknameValidationResult
import com.whatever.caro.feature.profile.NicknameValidator
import com.whatever.caro.feature.profile.mvi.CreateProfileIntent
import com.whatever.caro.feature.profile.mvi.CreateProfileSideEffect
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode.Companion.exactly
import dev.mokkery.verifySuspend
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class CreateProfileViewModelTest : FunSpec() {
    init {
        val dispatcher = StandardTestDispatcher()

        beforeTest {
            Dispatchers.setMain(dispatcher)
        }

        afterTest {
            Dispatchers.resetMain()
            dispatcher.cancel()
        }

        // init 의 fetchRandomNickname 이 항상 닉네임을 채우도록 기본 스텁을 준 VM 팩토리.
        fun createViewModel(
            randomNickname: String = "기본닉네임",
            isAvailable: Boolean = true,
        ): Triple<CreateProfileViewModel, AuthRepository, ProfileRepository> {
            val authRepository = mock<AuthRepository>()
            val profileRepository =
                mock<ProfileRepository> {
                    everySuspend { getRandomNickname() } returns randomNickname
                    everySuspend { isNicknameAvailable(any()) } returns isAvailable
                }
            val viewModel =
                CreateProfileViewModel(
                    authRepository = authRepository,
                    profileRepository = profileRepository,
                    nicknameValidator = NicknameValidator(),
                )
            return Triple(viewModel, authRepository, profileRepository)
        }

        test("init() 은 랜덤 닉네임을 받아 state 를 Valid 로 채운다") {
            runTest {
                val (viewModel, _, _) = createViewModel(randomNickname = "랜덤닉네임")

                advanceUntilIdle()

                viewModel.state.value.nickname shouldBe "랜덤닉네임"
                viewModel.state.value.validationResult shouldBe NicknameValidationResult.Valid
            }
        }

        test("최소 길이 미만 닉네임은 서버 확인 없이 TooShort 로 표시된다") {
            runTest {
                val (viewModel, _, profileRepository) = createViewModel()
                advanceUntilIdle()

                viewModel.intent(CreateProfileIntent.UpdateNickname("a"))
                advanceUntilIdle()

                viewModel.state.value.validationResult shouldBe NicknameValidationResult.TooShort
                // 형식 검증 실패 시 가용성 API 를 호출하지 않아야 한다.
                verifySuspend(exactly(0)) { profileRepository.isNicknameAvailable("a") }
            }
        }

        test("형식이 유효하고 중복이 아니면 Valid 가 된다") {
            runTest {
                val (viewModel, _, _) = createViewModel(isAvailable = true)
                advanceUntilIdle()

                viewModel.intent(CreateProfileIntent.UpdateNickname("거북이"))
                advanceUntilIdle()

                viewModel.state.value.validationResult shouldBe NicknameValidationResult.Valid
            }
        }

        test("형식이 유효하지만 이미 사용 중이면 Duplicate 가 된다") {
            runTest {
                val (viewModel, _, _) = createViewModel(isAvailable = false)
                advanceUntilIdle()

                viewModel.intent(CreateProfileIntent.UpdateNickname("거북이"))
                advanceUntilIdle()

                viewModel.state.value.validationResult shouldBe NicknameValidationResult.Duplicate
            }
        }

        test("확인 가능 상태에서 ClickConfirm 은 가입 완료 후 NavigateBack 을 emit 한다") {
            runTest {
                val (viewModel, authRepository, _) =
                    createViewModel(randomNickname = "랜덤닉네임")
                everySuspend {
                    authRepository.completeRegistration(any(), any())
                } returns AuthSession(accessToken = "access", refreshToken = "refresh")
                advanceUntilIdle()

                viewModel.sideEffect.test {
                    viewModel.intent(CreateProfileIntent.ClickConfirm)
                    advanceUntilIdle()

                    awaitItem() shouldBe CreateProfileSideEffect.NavigateBack
                }
                verifySuspend(exactly(1)) {
                    authRepository.completeRegistration(
                        nickname = "랜덤닉네임",
                        termsAgreed = true,
                    )
                }
            }
        }

        test("ClickBack 은 NavigateBack 을 emit 한다") {
            runTest {
                val (viewModel, _, _) = createViewModel()
                advanceUntilIdle()

                viewModel.sideEffect.test {
                    viewModel.intent(CreateProfileIntent.ClickBack)
                    advanceUntilIdle()

                    awaitItem() shouldBe CreateProfileSideEffect.NavigateBack
                }
            }
        }
    }
}
