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
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

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

        when (intent) {
            is CreateProfileIntent.UpdateNickname -> handleUpdateNickname(intent.nickname)
            is CreateProfileIntent.ClickRefresh -> fetchRandomNickname()
            is CreateProfileIntent.ClickConfirm -> handleConfirm()
            is CreateProfileIntent.ClickBack -> postSideEffect(CreateProfileSideEffect.NavigateBack)
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
                        // TODO: 서버 에러 처리 UI 확정 시 폴백 제거
                        NicknameValidationResult.Valid
                    }
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
                        reduce {
                            copy(
                                nickname = nickname,
                                validationResult = NicknameValidationResult.Valid,
                            )
                        }
                    }.onFailure { throwable ->
                        Napier.e(throwable = throwable) { "getRandomNickname failed" }
                    }
                reduce { copy(isRandomNicknameLoading = false) }
            }
    }

    private fun handleConfirm() {
        if (currentState.isConfirmEnabled.not()) return
        val nickname = currentState.nickname
        nicknameValidationJob?.cancel()
        randomNicknameJob?.cancel()
        reduce { copy(isLoading = true, isRandomNicknameLoading = false) }
        launch {
            suspendRunCatching {
                // TODO: 약관 동의 화면 추가 시 termsAgreed 전달 값 변경
                authRepository.completeRegistration(
                    nickname = nickname,
                    termsAgreed = true,
                )
            }.onSuccess {
                postSideEffect(CreateProfileSideEffect.NavigateHome)
            }.onFailure { throwable ->
                Napier.e(throwable = throwable) { "completeRegistration failed" }
                reduce { copy(isLoading = false) }
            }
        }
    }

    companion object {
        private const val DEBOUNCE_MS = 350L
    }
}
