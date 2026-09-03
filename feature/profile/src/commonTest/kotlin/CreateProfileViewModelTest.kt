import app.cash.turbine.test
import com.whatever.caro.core.data.repository.auth.AuthRepository
import com.whatever.caro.core.data.repository.profile.ProfileRepository
import com.whatever.caro.core.model.auth.AuthSession
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.profile.NicknameValidationResult
import com.whatever.caro.feature.profile.NicknameValidator
import com.whatever.caro.feature.profile.create.CreateProfileViewModel
import com.whatever.caro.feature.profile.create.mvi.CreateProfileIntent
import com.whatever.caro.feature.profile.create.mvi.CreateProfileSideEffect
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
class CreateProfileViewModelTest : FunSpec() {
    init {
        val dispatcher = StandardTestDispatcher()

        beforeTest {
            Dispatchers.setMain(dispatcher)
        }

        afterTest {
            Dispatchers.resetMain()
        }

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
                    exceptionFilter = ExceptionFilter.None,
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

        test("확인 가능 상태에서 ClickConfirm 은 가입 완료 후 NavigateHome 을 emit 한다") {
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

                    awaitItem() shouldBe CreateProfileSideEffect.NavigateHome
                }
                verifySuspend(exactly(1)) {
                    authRepository.completeRegistration(
                        nickname = "랜덤닉네임",
                        termsAgreed = true,
                    )
                }
            }
        }

        test("가입 저장 중 사용자 Intent 를 무시하고 저장 시작 시점의 닉네임을 전달한다") {
            runTest {
                val (viewModel, authRepository, profileRepository) =
                    createViewModel(randomNickname = "저장할닉네임")
                val saveGate = CompletableDeferred<Unit>()
                everySuspend {
                    authRepository.completeRegistration(any(), any())
                } calls {
                    saveGate.await()
                    AuthSession(accessToken = "access", refreshToken = "refresh")
                }
                advanceUntilIdle()
                val randomNicknameGate = CompletableDeferred<Unit>()
                everySuspend {
                    profileRepository.getRandomNickname()
                } calls {
                    randomNicknameGate.await()
                    "뒤늦게도착한닉네임"
                }

                viewModel.sideEffect.test {
                    viewModel.intent(CreateProfileIntent.ClickRefresh)
                    runCurrent()
                    viewModel.intent(CreateProfileIntent.ClickConfirm)
                    viewModel.intent(CreateProfileIntent.UpdateNickname("변경된닉네임"))
                    runCurrent()

                    viewModel.state.value.isLoading shouldBe true
                    viewModel.state.value.nickname shouldBe "저장할닉네임"

                    viewModel.intent(CreateProfileIntent.ClickRefresh)
                    viewModel.intent(CreateProfileIntent.ClickConfirm)
                    viewModel.intent(CreateProfileIntent.ClickBack)
                    runCurrent()
                    randomNicknameGate.complete(Unit)
                    runCurrent()

                    viewModel.state.value.nickname shouldBe "저장할닉네임"
                    expectNoEvents()
                    verifySuspend(exactly(2)) {
                        profileRepository.getRandomNickname()
                    }
                    verifySuspend(exactly(1)) {
                        authRepository.completeRegistration(
                            nickname = "저장할닉네임",
                            termsAgreed = true,
                        )
                    }

                    saveGate.complete(Unit)
                    awaitItem() shouldBe CreateProfileSideEffect.NavigateHome
                }
            }
        }

        test("ClickBack 은 회원가입 취소 다이얼로그를 띄우고 즉시 이동하지 않는다") {
            runTest {
                val (viewModel, _, _) = createViewModel()
                advanceUntilIdle()

                viewModel.sideEffect.test {
                    viewModel.intent(CreateProfileIntent.ClickBack)
                    advanceUntilIdle()

                    viewModel.state.value.isCancelDialogVisible shouldBe true
                    expectNoEvents()
                }
            }
        }

        test("DismissCancelDialog 는 다이얼로그만 닫는다") {
            runTest {
                val (viewModel, _, _) = createViewModel()
                advanceUntilIdle()

                viewModel.sideEffect.test {
                    viewModel.intent(CreateProfileIntent.ClickBack)
                    viewModel.intent(CreateProfileIntent.DismissCancelDialog)
                    advanceUntilIdle()

                    viewModel.state.value.isCancelDialogVisible shouldBe false
                    expectNoEvents()
                }
            }
        }

        test("ConfirmCancel 은 세션을 정리하고 NavigateLogin 을 emit 한다") {
            runTest {
                val (viewModel, authRepository, _) = createViewModel()
                everySuspend { authRepository.logout() } returns Unit
                advanceUntilIdle()

                viewModel.sideEffect.test {
                    viewModel.intent(CreateProfileIntent.ClickBack)
                    viewModel.intent(CreateProfileIntent.ConfirmCancel)
                    advanceUntilIdle()

                    awaitItem() shouldBe CreateProfileSideEffect.NavigateLogin
                    viewModel.state.value.isCancelDialogVisible shouldBe false
                }
                verifySuspend(exactly(1)) { authRepository.logout() }
            }
        }
    }
}
