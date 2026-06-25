package com.whatever.caro.feature.profile.edit

import com.whatever.caro.core.data.repository.profile.ProfileRepository
import com.whatever.caro.core.data.util.suspendRunCatching
import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.profile.NicknameValidationResult
import com.whatever.caro.feature.profile.NicknameValidator
import com.whatever.caro.feature.profile.edit.mvi.EditProfileIntent
import com.whatever.caro.feature.profile.edit.mvi.EditProfileSideEffect
import com.whatever.caro.feature.profile.edit.mvi.EditProfileState
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class EditProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val nicknameValidator: NicknameValidator,
    nickname: String,
    exceptionFilter: ExceptionFilter,
) : BaseViewModel<EditProfileState, EditProfileIntent, EditProfileSideEffect>(
        initialState = EditProfileState(nickname = nickname),
        exceptionFilter = exceptionFilter,
    ) {
    private var nicknameValidationJob: Job? = null

    override suspend fun handleIntent(intent: EditProfileIntent) {
        when (intent) {
            is EditProfileIntent.UpdateNickname -> handleUpdateNickname(intent.nickname)
            is EditProfileIntent.ClickRefresh -> fetchRandomNickname()
            is EditProfileIntent.ClickConfirm -> handleConfirm()
            is EditProfileIntent.ClickBack -> postSideEffect(EditProfileSideEffect.NavigateBack)
        }
    }

    private fun handleUpdateNickname(nickname: String) {
        val filtered = nicknameValidator.filterInput(nickname)
        val validation = nicknameValidator.validate(filtered)

        if (validation.isValid.not()) {
            nicknameValidationJob?.cancel()
            reduce { copy(nickname = filtered, validationResult = validation) }
            return
        }

        reduce { copy(nickname = filtered, validationResult = NicknameValidationResult.Checking) }

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
        reduce { copy(isRandomNicknameLoading = true) }
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
        reduce { copy(isLoading = true) }
        launch {
            suspendRunCatching {
                profileRepository.updateNickname(nickname = currentState.nickname)
            }.onSuccess {
                postSideEffect(EditProfileSideEffect.NavigateBack)
            }.onFailure { throwable ->
                Napier.e(throwable = throwable) { "handleConfirm failed" }
                reduce { copy(isLoading = false) }
            }
        }
    }

    companion object {
        private const val DEBOUNCE_MS = 350L
    }
}
