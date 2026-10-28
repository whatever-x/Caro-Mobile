package com.whatever.caro.feature.profile.create

import com.whatever.caro.core.data.repository.auth.AuthRepository
import com.whatever.caro.core.data.repository.profile.ProfileRepository
import com.whatever.caro.core.data.util.suspendRunCatching
import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.profile.NicknameValidationResult
import com.whatever.caro.feature.profile.NicknameValidator
import com.whatever.caro.feature.profile.create.mvi.CreateProfileIntent
import com.whatever.caro.feature.profile.create.mvi.CreateProfileSideEffect
import com.whatever.caro.feature.profile.create.mvi.CreateProfileState
import com.whatever.caro.feature.profile.create.mvi.CreateProfileStep
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

class CreateProfileViewModel(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val nicknameValidator: NicknameValidator,
    exceptionFilter: ExceptionFilter,
) : BaseViewModel<CreateProfileState, CreateProfileIntent, CreateProfileSideEffect>(
        initialState = CreateProfileState(),
        exceptionFilter = exceptionFilter,
    ) {
    private var nicknameValidationJob: Job? = null
    private var randomNicknameJob: Job? = null

    init {
        fetchRandomNickname()
    }

    override suspend fun handleIntent(intent: CreateProfileIntent) {
        if (currentState.isLoading) return
        if (currentState.isReloginDialogVisible &&
            intent != CreateProfileIntent.ConfirmRelogin && intent != CreateProfileIntent.DismissRelogin
        ) {
            return
        }

        when (intent) {
            is CreateProfileIntent.UpdateNickname -> {
                handleUpdateNickname(intent.nickname)
            }

            is CreateProfileIntent.UpdateYear -> {
                reduce { copy(year = intent.value.filter { it in '0'..'9' }.take(4), registrationFailed = false) }
            }

            is CreateProfileIntent.UpdateMonth -> {
                reduce { copy(month = intent.value.filter { it in '0'..'9' }.take(2), registrationFailed = false) }
            }

            is CreateProfileIntent.UpdateDay -> {
                reduce { copy(day = intent.value.filter { it in '0'..'9' }.take(2), registrationFailed = false) }
            }

            is CreateProfileIntent.RetryNicknameCheck -> {
                if (currentState.validationResult == NicknameValidationResult.CheckFailed) {
                    handleUpdateNickname(currentState.nickname)
                }
            }

            is CreateProfileIntent.ClickRefresh -> {
                fetchRandomNickname()
            }

            is CreateProfileIntent.ClickConfirm -> {
                handleConfirm()
            }

            is CreateProfileIntent.ClickRelogin -> {
                if (currentState.registrationFailed && currentState.step == CreateProfileStep.BirthDate) {
                    reduce { copy(isReloginDialogVisible = true) }
                }
            }

            is CreateProfileIntent.DismissRelogin -> {
                reduce { copy(isReloginDialogVisible = false) }
            }

            is CreateProfileIntent.ConfirmRelogin -> {
                handleRelogin()
            }

            is CreateProfileIntent.ClickBack -> {
                reduce {
                    if (step == CreateProfileStep.BirthDate) {
                        copy(step = CreateProfileStep.Nickname, registrationFailed = false)
                    } else {
                        copy(isCancelDialogVisible = true)
                    }
                }
            }

            is CreateProfileIntent.ConfirmCancel -> {
                handleConfirmCancel()
            }

            is CreateProfileIntent.DismissCancelDialog -> {
                reduce { copy(isCancelDialogVisible = false) }
            }
        }
    }

    private fun handleUpdateNickname(nickname: String) {
        randomNicknameJob?.cancel()
        val filtered = nicknameValidator.filterInput(nickname)
        val validation = nicknameValidator.validate(filtered)

        if (validation.isValid.not()) {
            nicknameValidationJob?.cancel()
            reduce {
                copy(
                    nickname = filtered,
                    validationResult = validation,
                    isRandomNicknameLoading = false,
                )
            }
            return
        }

        reduce {
            copy(
                nickname = filtered,
                validationResult = NicknameValidationResult.Checking,
                isRandomNicknameLoading = false,
            )
        }

        checkNickname(filtered)
    }

    private fun checkNickname(filtered: String) {
        nicknameValidationJob?.cancel()
        nicknameValidationJob =
            launch {
                delay(DEBOUNCE_MS)
                val result =
                    suspendRunCatching {
                        if (profileRepository.isNicknameAvailable(filtered)) {
                            NicknameValidationResult.Valid
                        } else {
                            NicknameValidationResult.Duplicate
                        }
                    }.getOrElse { throwable ->
                        Napier.e(throwable = throwable) { "checkNicknameAvailability failed" }
                        NicknameValidationResult.CheckFailed
                    }
                coroutineContext.ensureActive()
                reduce { copy(validationResult = result) }
            }
    }

    private fun fetchRandomNickname() {
        nicknameValidationJob?.cancel()
        randomNicknameJob?.cancel()
        reduce { copy(isRandomNicknameLoading = true) }
        randomNicknameJob =
            launch {
                suspendRunCatching { profileRepository.getRandomNickname() }
                    .onSuccess { nickname ->
                        coroutineContext.ensureActive()
                        reduce {
                            copy(
                                nickname = nickname,
                                validationResult = NicknameValidationResult.Valid,
                            )
                        }
                    }.onFailure { throwable ->
                        coroutineContext.ensureActive()
                        Napier.e(throwable = throwable) { "getRandomNickname failed" }
                        if (currentState.validationResult == NicknameValidationResult.Checking) {
                            checkNickname(currentState.nickname)
                        }
                    }
                coroutineContext.ensureActive()
                reduce { copy(isRandomNicknameLoading = false) }
            }
    }

    /**
     * 닉네임 설정을 마치지 않은 세션은 앱을 다시 켜도 이 화면으로 돌아오므로,
     * 회원가입 취소는 저장된 토큰을 지우고 로그인 화면에서 다시 시작하게 한다.
     */
    private suspend fun handleConfirmCancel() {
        if (currentState.isCancelDialogVisible.not()) return
        nicknameValidationJob?.cancel()
        randomNicknameJob?.cancel()
        reduce { copy(isCancelDialogVisible = false, isLoading = true, isRandomNicknameLoading = false) }
        authRepository.logout()
        postSideEffect(CreateProfileSideEffect.NavigateLogin)
    }

    private fun handleRelogin() {
        if (!currentState.isReloginDialogVisible || !currentState.registrationFailed) return
        reduce { copy(isReloginDialogVisible = false, isLoading = true, reloginFailed = false) }
        launch {
            suspendRunCatching { authRepository.logout() }
                .onSuccess { postSideEffect(CreateProfileSideEffect.NavigateLogin) }
                .onFailure { throwable ->
                    Napier.e(throwable = throwable) { "logout for relogin failed" }
                    reduce { copy(isLoading = false, reloginFailed = true) }
                }
        }
    }

    private fun handleConfirm() {
        if (currentState.isConfirmEnabled.not()) return
        if (currentState.step == CreateProfileStep.Nickname) {
            nicknameValidationJob?.cancel()
            randomNicknameJob?.cancel()
            reduce { copy(step = CreateProfileStep.BirthDate, isRandomNicknameLoading = false) }
            return
        }
        val nickname = currentState.nickname
        val birthDate = currentState.birthDate ?: return
        nicknameValidationJob?.cancel()
        randomNicknameJob?.cancel()
        reduce { copy(isLoading = true, isRandomNicknameLoading = false, registrationFailed = false, reloginFailed = false) }
        launch {
            suspendRunCatching {
                // TODO: 약관 동의 화면 추가 시 termsAgreed 전달 값 변경
                authRepository.completeRegistration(
                    nickname = nickname,
                    termsAgreed = true,
                    birthDate = birthDate,
                )
            }.onSuccess {
                postSideEffect(CreateProfileSideEffect.NavigateHome)
            }.onFailure { throwable ->
                Napier.e(throwable = throwable) { "completeRegistration failed" }
                reduce { copy(isLoading = false, registrationFailed = true) }
            }
        }
    }

    companion object {
        private const val DEBOUNCE_MS = 350L
    }
}
