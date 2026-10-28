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
import com.whatever.caro.feature.profile.create.mvi.CreateProfileState
import com.whatever.caro.feature.profile.create.mvi.CreateProfileStep
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
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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

        test("닉네임 확인 실패 시 다음을 막고 재시도로 같은 값을 확인한다") {
            runTest {
                val (viewModel, _, profileRepository) = createViewModel()
                var attempts = 0
                everySuspend { profileRepository.isNicknameAvailable("거북이") } calls {
                    attempts++
                    if (attempts == 1) error("network")
                    true
                }
                advanceUntilIdle()
                viewModel.intent(CreateProfileIntent.UpdateNickname("거북이"))
                runCurrent()
                viewModel.state.value.validationResult shouldBe NicknameValidationResult.Checking
                viewModel.state.value.isConfirmEnabled shouldBe false
                viewModel.intent(CreateProfileIntent.ClickConfirm)
                advanceUntilIdle()
                viewModel.state.value.step shouldBe CreateProfileStep.Nickname
                viewModel.state.value.validationResult shouldBe NicknameValidationResult.CheckFailed
                viewModel.state.value.isConfirmEnabled shouldBe false
                viewModel.intent(CreateProfileIntent.ClickConfirm)
                viewModel.intent(CreateProfileIntent.RetryNicknameCheck)
                runCurrent()
                viewModel.state.value.validationResult shouldBe NicknameValidationResult.Checking
                advanceUntilIdle()
                viewModel.state.value.validationResult shouldBe NicknameValidationResult.Valid
                viewModel.state.value.isConfirmEnabled shouldBe true
                attempts shouldBe 2
            }
        }

        test("취소된 이전 확인 응답은 최신 닉네임의 상태를 덮지 않는다") {
            runTest {
                val (viewModel, _, profileRepository) = createViewModel()
                var oldResponse: Continuation<Boolean>? = null
                val newResponse = CompletableDeferred<Unit>()
                everySuspend { profileRepository.isNicknameAvailable("오래된닉네임") } calls {
                    suspendCoroutine { oldResponse = it }
                }
                everySuspend { profileRepository.isNicknameAvailable("새로운닉네임") } calls {
                    newResponse.await()
                    true
                }
                advanceUntilIdle()
                viewModel.intent(CreateProfileIntent.UpdateNickname("오래된닉네임"))
                runCurrent()
                advanceTimeBy(350)
                runCurrent()
                viewModel.intent(CreateProfileIntent.UpdateNickname("새로운닉네임"))
                runCurrent()
                advanceTimeBy(350)
                runCurrent()
                checkNotNull(oldResponse).resume(false)
                runCurrent()
                viewModel.state.value.nickname shouldBe "새로운닉네임"
                viewModel.state.value.validationResult shouldBe NicknameValidationResult.Checking
                viewModel.state.value.isConfirmEnabled shouldBe false
                newResponse.complete(Unit)
                advanceUntilIdle()
                viewModel.state.value.validationResult shouldBe NicknameValidationResult.Valid
            }
        }

        test("확인 중 랜덤 생성이 실패하면 기존 닉네임 확인을 재개한다") {
            runTest {
                val (viewModel, _, profileRepository) = createViewModel()
                advanceUntilIdle()
                val firstCheck = CompletableDeferred<Unit>()
                var checks = 0
                everySuspend { profileRepository.isNicknameAvailable("거북이") } calls {
                    checks++
                    if (checks == 1) firstCheck.await()
                    false
                }
                everySuspend { profileRepository.getRandomNickname() } calls { error("network") }
                viewModel.intent(CreateProfileIntent.UpdateNickname("거북이"))
                runCurrent()
                advanceTimeBy(350)
                runCurrent()
                viewModel.state.value.validationResult shouldBe NicknameValidationResult.Checking
                viewModel.intent(CreateProfileIntent.ClickRefresh)
                advanceUntilIdle()
                viewModel.state.value.nickname shouldBe "거북이"
                viewModel.state.value.validationResult shouldBe NicknameValidationResult.Duplicate
                viewModel.state.value.isConfirmEnabled shouldBe false
                checks shouldBe 2
            }
        }

        test("확인 가능 상태에서 ClickConfirm 은 가입 완료 후 NavigateHome 을 emit 한다") {
            runTest {
                val (viewModel, authRepository, _) =
                    createViewModel(randomNickname = "랜덤닉네임")
                everySuspend {
                    authRepository.completeRegistration(any(), any(), any())
                } returns AuthSession(accessToken = "access", refreshToken = "refresh")
                advanceUntilIdle()

                viewModel.sideEffect.test {
                    viewModel.intent(CreateProfileIntent.ClickConfirm)
                    viewModel.intent(CreateProfileIntent.UpdateYear("1994"))
                    viewModel.intent(CreateProfileIntent.UpdateMonth("02"))
                    viewModel.intent(CreateProfileIntent.UpdateDay("15"))
                    viewModel.intent(CreateProfileIntent.ClickConfirm)
                    advanceUntilIdle()

                    awaitItem() shouldBe CreateProfileSideEffect.NavigateHome
                }
                verifySuspend(exactly(1)) {
                    authRepository.completeRegistration(
                        nickname = "랜덤닉네임",
                        termsAgreed = true,
                        birthDate = "1994-02-15",
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
                    authRepository.completeRegistration(any(), any(), any())
                } calls {
                    saveGate.await()
                    AuthSession(accessToken = "access", refreshToken = "refresh")
                }
                advanceUntilIdle()

                viewModel.sideEffect.test {
                    viewModel.intent(CreateProfileIntent.ClickConfirm)
                    viewModel.intent(CreateProfileIntent.UpdateYear("1994"))
                    viewModel.intent(CreateProfileIntent.UpdateMonth("02"))
                    viewModel.intent(CreateProfileIntent.UpdateDay("15"))
                    viewModel.intent(CreateProfileIntent.ClickConfirm)
                    viewModel.intent(CreateProfileIntent.UpdateNickname("변경된닉네임"))
                    runCurrent()

                    viewModel.state.value.isLoading shouldBe true
                    viewModel.state.value.nickname shouldBe "저장할닉네임"

                    viewModel.intent(CreateProfileIntent.ClickRefresh)
                    viewModel.intent(CreateProfileIntent.ClickConfirm)
                    viewModel.intent(CreateProfileIntent.ClickBack)
                    runCurrent()

                    viewModel.state.value.nickname shouldBe "저장할닉네임"
                    expectNoEvents()
                    verifySuspend(exactly(1)) {
                        profileRepository.getRandomNickname()
                    }
                    verifySuspend(exactly(1)) {
                        authRepository.completeRegistration(
                            nickname = "저장할닉네임",
                            termsAgreed = true,
                            birthDate = "1994-02-15",
                        )
                    }

                    saveGate.complete(Unit)
                    awaitItem() shouldBe CreateProfileSideEffect.NavigateHome
                }
            }
        }

        test("생년월일 입력은 유효한 날짜만 허용하고 뒤로 가도 유지된다") {
            runTest {
                val (viewModel, _, _) = createViewModel()
                advanceUntilIdle()
                viewModel.intent(CreateProfileIntent.ClickConfirm)
                viewModel.intent(CreateProfileIntent.UpdateYear("2023"))
                viewModel.intent(CreateProfileIntent.UpdateMonth("02"))
                viewModel.intent(CreateProfileIntent.UpdateDay("29"))
                advanceUntilIdle()
                viewModel.state.value.isBirthDateInvalid shouldBe true
                viewModel.state.value.isConfirmEnabled shouldBe false
                viewModel.intent(CreateProfileIntent.UpdateYear("2024"))
                advanceUntilIdle()
                viewModel.state.value.birthDate shouldBe "2024-02-29"
                viewModel.intent(CreateProfileIntent.ClickBack)
                advanceUntilIdle()
                viewModel.state.value.step shouldBe CreateProfileStep.Nickname
                viewModel.intent(CreateProfileIntent.ClickConfirm)
                advanceUntilIdle()
                viewModel.state.value.birthDate shouldBe "2024-02-29"
            }
        }

        test("생년월일은 기준 날짜의 오늘까지 허용한다") {
            val state = CreateProfileState(year = "2026", month = "10", day = "28")
            state.birthDateOn(LocalDate(2026, 10, 27)) shouldBe null
            state.birthDateOn(LocalDate(2026, 10, 28)) shouldBe "2026-10-28"
        }

        test("가입 실패 후 입력을 유지하고 다시 시도한다") {
            runTest {
                val (viewModel, authRepository, _) = createViewModel()
                var attempts = 0
                everySuspend { authRepository.completeRegistration(any(), any(), any()) } calls {
                    attempts++
                    if (attempts == 1) error("network")
                    AuthSession(accessToken = "access", refreshToken = "refresh")
                }
                advanceUntilIdle()
                viewModel.intent(CreateProfileIntent.ClickConfirm)
                viewModel.intent(CreateProfileIntent.UpdateYear("1994"))
                viewModel.intent(CreateProfileIntent.UpdateMonth("02"))
                viewModel.intent(CreateProfileIntent.UpdateDay("15"))
                viewModel.intent(CreateProfileIntent.ClickConfirm)
                advanceUntilIdle()
                viewModel.state.value.registrationFailed shouldBe true
                viewModel.state.value.birthDate shouldBe "1994-02-15"
                viewModel.sideEffect.test {
                    viewModel.intent(CreateProfileIntent.ClickConfirm)
                    advanceUntilIdle()
                    awaitItem() shouldBe CreateProfileSideEffect.NavigateHome
                }
                attempts shouldBe 2
            }
        }

        test("가입 실패 시 다시 로그인 취소는 입력을 보존하고 확인은 한 번만 로그아웃한다") {
            runTest {
                val (viewModel, authRepository, _) = createViewModel()
                everySuspend { authRepository.completeRegistration(any(), any(), any()) } calls { error("network") }
                val logoutGate = CompletableDeferred<Unit>()
                everySuspend { authRepository.logout() } calls { logoutGate.await() }
                advanceUntilIdle()
                viewModel.intent(CreateProfileIntent.ClickRelogin)
                viewModel.intent(CreateProfileIntent.ConfirmRelogin)
                runCurrent()
                verifySuspend(exactly(0)) { authRepository.logout() }
                viewModel.intent(CreateProfileIntent.ClickConfirm)
                viewModel.intent(CreateProfileIntent.UpdateYear("1994"))
                viewModel.intent(CreateProfileIntent.UpdateMonth("02"))
                viewModel.intent(CreateProfileIntent.UpdateDay("15"))
                viewModel.intent(CreateProfileIntent.ClickConfirm)
                advanceUntilIdle()
                viewModel.intent(CreateProfileIntent.ClickRelogin)
                runCurrent()
                viewModel.state.value.isReloginDialogVisible shouldBe true
                viewModel.intent(CreateProfileIntent.DismissRelogin)
                runCurrent()
                viewModel.state.value.isReloginDialogVisible shouldBe false
                viewModel.state.value.birthDate shouldBe "1994-02-15"
                verifySuspend(exactly(0)) { authRepository.logout() }
                viewModel.sideEffect.test {
                    viewModel.intent(CreateProfileIntent.ClickRelogin)
                    viewModel.intent(CreateProfileIntent.ConfirmRelogin)
                    viewModel.intent(CreateProfileIntent.ConfirmRelogin)
                    viewModel.intent(CreateProfileIntent.ClickConfirm)
                    runCurrent()
                    viewModel.state.value.isLoading shouldBe true
                    verifySuspend(exactly(1)) { authRepository.logout() }
                    expectNoEvents()
                    logoutGate.complete(Unit)
                    awaitItem() shouldBe CreateProfileSideEffect.NavigateLogin
                }
            }
        }

        test("다시 로그인 정리 실패는 화면에 남아 재시도할 수 있다") {
            runTest {
                val (viewModel, authRepository, _) = createViewModel()
                everySuspend { authRepository.completeRegistration(any(), any(), any()) } calls { error("network") }
                everySuspend { authRepository.logout() } calls { error("storage") }
                advanceUntilIdle()
                viewModel.intent(CreateProfileIntent.ClickConfirm)
                viewModel.intent(CreateProfileIntent.UpdateYear("1994"))
                viewModel.intent(CreateProfileIntent.UpdateMonth("02"))
                viewModel.intent(CreateProfileIntent.UpdateDay("15"))
                viewModel.intent(CreateProfileIntent.ClickConfirm)
                advanceUntilIdle()
                viewModel.sideEffect.test {
                    viewModel.intent(CreateProfileIntent.ClickRelogin)
                    viewModel.intent(CreateProfileIntent.ConfirmRelogin)
                    advanceUntilIdle()
                    viewModel.state.value.reloginFailed shouldBe true
                    viewModel.state.value.registrationFailed shouldBe true
                    viewModel.state.value.isLoading shouldBe false
                    viewModel.state.value.birthDate shouldBe "1994-02-15"
                    expectNoEvents()
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
